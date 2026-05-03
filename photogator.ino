// Declare an instance of this class on the stack within the main loop
// when accessing global variables also accessed by an interrupt handler
class CriticalSection
{
public:
	static void enter()
	{
		noInterrupts();	// disable interrupts while in critical section
	}

	static void exit()
	{
		interrupts();		// re-enable interrupts as critical section ends
	}

	CriticalSection()
	{
		enter();
	}

	~CriticalSection()
	{
		exit();
	}
};

const uint8_t k_startInterruptPin = 2;	// assign interrupt pin for start
const uint8_t k_stopInterruptPin = 3;		// assign interrupt pin for stop
const uint8_t k_groundPin = 4;				// ground pin for the interrupt filtering capacitor
const char*const k_pHeartbeatMsg = "HeartBeat";
const char*const k_pEventMsgPrefix = "BeamBroken:";
const char*const k_pSeparator = ",";
const uint8_t k_maxUnsignedLongStrLen = 10;	// # digits in 2^32
const uint8_t k_maxEventMsgLen =
	11										// # chars in k_pEventMsgPrefix
	+ 3 * k_maxUnsignedLongStrLen	// space for the numbers
	+ 2									// space for the comma separators
	+ 1;									// space for the null terminator

volatile bool g_newEvent = false;			// Have we seen a new interrupt event?
volatile uint8_t g_eventPin = 0;				// pin  of interrupt event
volatile unsigned long g_eventTime = 0;	// time of interrupt event
volatile unsigned long g_eventSeqNum = 0;	// sequence number of interrupt event
unsigned long g_lastHearbeat = 0;			// time last heartbeat message was sent

// Attaches interrupt handler to run on falling edge of input signal
static void attachInterruptFalling(uint8_t pin, void (*handler)()) {
	attachInterrupt(digitalPinToInterrupt(pin), handler, FALLING);
}

static void trigger(uint8_t pin) {
	g_newEvent = true;
	g_eventPin = pin;
	g_eventTime = millis();
	++g_eventSeqNum;
}

static void startTrigger() {
	trigger(k_startInterruptPin);
}

static void stopTrigger() {
	trigger(k_stopInterruptPin);
}

void setup() {
	Serial.begin(57600);

	// Initialize digital pin LED_BUILTIN as an output:
	pinMode(LED_BUILTIN, OUTPUT);

	// Set a ground for the interrupt filtering capacitor:
	pinMode(k_groundPin, OUTPUT);
	digitalWrite(k_groundPin, LOW);

	// Set up the interrupt handlers:
	pinMode(k_startInterruptPin, INPUT_PULLUP);
	attachInterruptFalling(k_startInterruptPin, startTrigger);
	pinMode(k_stopInterruptPin, INPUT_PULLUP);
	attachInterruptFalling(k_stopInterruptPin, stopTrigger);
}

static void sendEventMessage(uint8_t eventPin, unsigned long eventTime, unsigned long eventSeqNum)
{
	char msg[k_maxEventMsgLen];
	char* pEnd = msg;

	// Start the message with the prefix:
	strcpy(pEnd, k_pEventMsgPrefix);
	pEnd += strlen(pEnd);

	// Add the sequence number and separator:
	ultoa(eventSeqNum, pEnd, 10);
	pEnd += strlen(pEnd);
	strcpy(pEnd, k_pSeparator);
	pEnd += strlen(pEnd);

	// Add the sensor ID (pin number) and separator:
	ultoa(eventPin, pEnd, 10);
	pEnd += strlen(pEnd);
	strcpy(pEnd, k_pSeparator);
	pEnd += strlen(pEnd);

	// Add the time:
	ultoa(eventTime, pEnd, 10);

	// Send message on the serial port:
	Serial.write(msg, strlen(msg) + 1);
}

static uint8_t toggleBuiltinLedState()
{
	static uint8_t g_builtinLedState = LOW;
	g_builtinLedState = (g_builtinLedState == LOW) ? HIGH : LOW;
	return g_builtinLedState;
}

void loop() {
	CriticalSection::enter();
	auto newEvent = g_newEvent;
	auto eventPin = g_eventPin;
	auto eventTime = g_eventTime;
	auto eventSeqNum = g_eventSeqNum;
	g_newEvent = false;
	CriticalSection::exit();

	if (newEvent)
	{
		sendEventMessage(eventPin, eventTime, eventSeqNum);
	}

	if (millis() - g_lastHearbeat > 1000)
	{
		Serial.write(k_pHeartbeatMsg, strlen(k_pHeartbeatMsg) + 1);
		digitalWrite(LED_BUILTIN, toggleBuiltinLedState());
		g_lastHearbeat = millis();
	}
}
