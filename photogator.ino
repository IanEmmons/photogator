// Declaring an instance of this class on the stack
// within an interrupt handler takes care of housekeeping
class InterruptFrame {
public:
	InterruptFrame() {
		noInterrupts();	// disable interrupts while handling current interrupt
	}

	~InterruptFrame() {
		EIFR = (1 << INTF1) | (1 << INTF0);	// clear interrupt flags in case
														// interrupt was triggered while
														// writing to serial port
		interrupts();								// re-enable interrupts
	}
};

const unsigned k_startInterruptPin = 2;	// assign interrupt pin for start
const unsigned k_stopInterruptPin = 3;		// assign interrupt pin for stop
const unsigned k_groundPin = 4;				// ground pin for the interrupt filtering capacitor
const char*const k_pHeartbeatMsg = "HeartBeat";
const char*const k_pEventMsgPrefix = "BeamBroken:";
const char*const k_pSeparator = ",";
const unsigned k_maxUnsignedLongStrLen = 10;	// # digits in 2^32
const unsigned k_maxEventMsgLen =
	11										// # chars in k_pEventMsgPrefix
	+ 3 * k_maxUnsignedLongStrLen	// space for the numbers
	+ 2									// space for the comma separators
	+ 1;									// space for the null terminator

volatile unsigned long seqNum = 0;	// sequence number of interrupt event

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

void loop() {
	// Send periodic heartbeat message:
	delay(900);
	digitalWrite(LED_BUILTIN, HIGH);
	Serial.write(k_pHeartbeatMsg, strlen(k_pHeartbeatMsg) + 1);
	delay(100);
	digitalWrite(LED_BUILTIN, LOW);
}

// Attaches interrupt handler to run on falling edge of input signal
static void attachInterruptFalling(unsigned pin, void (*handler)()) {
	attachInterrupt(digitalPinToInterrupt(pin), handler, FALLING);
}

static void startTrigger() {
	trigger(k_startInterruptPin);
}

static void stopTrigger() {
	trigger(k_stopInterruptPin);
}

static void trigger(unsigned pin) {
	InterruptFrame iFrame;
	char msg[k_maxEventMsgLen];
	char* pEnd = msg;

	unsigned long t = millis();  // get the number of milliseconds since the Arduino was reset
	++seqNum; // increment the event sequence number

	// Start the message with the prefix:
	strcpy(pEnd, k_pEventMsgPrefix);
	pEnd += strlen(pEnd);

	// Add the sequence number and separator:
	ultoa(seqNum, pEnd, 10);
	pEnd += strlen(pEnd);
	strcpy(pEnd, k_pSeparator);
	pEnd += strlen(pEnd);

	// Add the sensor ID (pin number) and separator:
	ultoa(pin, pEnd, 10);
	pEnd += strlen(pEnd);
	strcpy(pEnd, k_pSeparator);
	pEnd += strlen(pEnd);

	// Add the time:
	ultoa(t, pEnd, 10);

	// Send message on the serial port:
	Serial.write(msg, strlen(msg) + 1);
}
