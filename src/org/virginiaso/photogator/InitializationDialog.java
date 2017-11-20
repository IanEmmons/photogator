package org.virginiaso.photogator;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import org.virginiaso.serialport.ArduinoEvent;
import org.virginiaso.serialport.HeartBeatEvent;
import org.virginiaso.serialport.SerialPortReader;

import jssc.SerialPortException;
import jssc.SerialPortList;

public class InitializationDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	private static final String DIALOG_TITLE = "Connecting...";
	private static final String HEADING_TEXT = "Searching for photogates on serial port:          ";
	private static final String EXIT_BTN_TEXT = "Exit";
	private static final int TIMER_INITIAL_DELAY_MS = 500;
	private static final int TIMER_INTERVAL_MS = 100;
	private static final Object LOCK = new Object();

	/*
	 * Five seconds may seem excessive, but it takes the Arduino
	 * a considerable time (about 2.5 seconds) to "wake up" after
	 * the serial port connection is made.
	 */
	private static final long TIME_LIMIT_FOR_ARDUINO_HEARTBEAT_MS = 5000;

	private JLabel headingLbl;
	private JPanel serialPortBtnPnl;
	private List<JRadioButton> serialPortBtns;
	private ButtonGroup serialPortBtnGrp;
	private JProgressBar progBar;
	private JButton exitBtn;
	private JPanel progAndExitPnl;
	private Timer timer;
	private int currentRadioBtnIndex = -1;
	private long currentRadioBtnStartTime = System.currentTimeMillis() - 2 * TIME_LIMIT_FOR_ARDUINO_HEARTBEAT_MS;
	private SerialPortReader portRdr = null;
	private boolean arduinoDetected = false;
	private String foundSerialPort = null;

	public InitializationDialog(JFrame frame) {
		super(frame, true);	// make's this a modal dialog
		initComponents();
	}

	private void initComponents() {
		setTitle(DIALOG_TITLE);

		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
			}
		});

		headingLbl = new JLabel(HEADING_TEXT);

		serialPortBtnPnl = new JPanel();
		serialPortBtnPnl.setLayout(new BoxLayout(serialPortBtnPnl, BoxLayout.PAGE_AXIS));

		List<String> serialPorts = Arrays.stream(getSerialPortNames())
			.sorted()
			.distinct()
			.collect(Collectors.toList());

		serialPortBtns = new ArrayList<>();
		serialPortBtnGrp = new ButtonGroup();
		Font btnFont = new Font(Font.MONOSPACED, Font.PLAIN, headingLbl.getFont().getSize());
		for (String portName : serialPorts) {
			JRadioButton btn = new JRadioButton(portName);
			btn.setActionCommand(portName);
			btn.setEnabled(false);
			btn.setFont(btnFont);
			serialPortBtns.add(btn);
			serialPortBtnGrp.add(btn);
			serialPortBtnPnl.add(btn);
		}
		serialPortBtnPnl.add(new JLabel(" "));	// spacer

		progBar = new JProgressBar(SwingConstants.HORIZONTAL);
		progBar.setIndeterminate(true);

		exitBtn = new JButton(EXIT_BTN_TEXT);
		exitBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				onExitBtn();
			}
		});

		progAndExitPnl = new JPanel();
		progAndExitPnl.setLayout(new BoxLayout(progAndExitPnl, BoxLayout.LINE_AXIS));
		progAndExitPnl.add(progBar);
		progAndExitPnl.add(exitBtn);

		add(headingLbl, BorderLayout.PAGE_START);
		add(serialPortBtnPnl, BorderLayout.CENTER);
		add(progAndExitPnl, BorderLayout.PAGE_END);
		pack();

		timer = new Timer(TIMER_INTERVAL_MS, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				onTimerEvent();
			}
		});
		timer.setInitialDelay(TIMER_INITIAL_DELAY_MS);
		timer.start();
	}

	private void onTimerEvent() {
		if (isArduinoDetected()) {
			timer.stop();
			foundSerialPort = portRdr.getSerialPortName();
			disconnectFromSerialPort();
			setVisible(false);
		} else if (System.currentTimeMillis() - currentRadioBtnStartTime > TIME_LIMIT_FOR_ARDUINO_HEARTBEAT_MS) {
			// Terminate existing serial port connection, if any:
			disconnectFromSerialPort();

			// Move to the next serial port:
			currentRadioBtnStartTime = System.currentTimeMillis();
			++currentRadioBtnIndex;
			currentRadioBtnIndex %= serialPortBtns.size();
			JRadioButton currentBtn = serialPortBtns.get(currentRadioBtnIndex);
			currentBtn.setSelected(true);
			String portName = currentBtn.getActionCommand();

			//Connect to portName:
			try {
				portRdr = new SerialPortReader(portName, this::serialPortRecieveAction, Photogator.ERR_LOG);
			} catch (SerialPortException ex) {
				ex.printStackTrace(Photogator.ERR_LOG);
			}
		}
	}

	private void serialPortRecieveAction(String msg) {
		ArduinoEvent evt = ArduinoEvent.parse(msg);
		if (evt == null) {
			Photogator.ERR_LOG.format("Unrecognized message '%1$s'%n", msg);
		} else if (evt instanceof HeartBeatEvent || evt instanceof BeamBrokenEvent) {
			setArduinoDetected(true);
		}
	}

	private void disconnectFromSerialPort() {
		if (portRdr != null) {
			try {
				portRdr.close();
			} catch (SerialPortException ex) {
				ex.printStackTrace(Photogator.ERR_LOG);
			}
			portRdr = null;
		}
	}

	private void onExitBtn() {
		timer.stop();
		foundSerialPort = null;
		disconnectFromSerialPort();
		setVisible(false);
	}

	private boolean isArduinoDetected() {
		synchronized (LOCK) {
			return arduinoDetected;
		}
	}

	private void setArduinoDetected(boolean newValue) {
		synchronized (LOCK) {
			arduinoDetected = newValue;
		}
	}

	public boolean isASerialPortPresent() {
		return serialPortBtns.size() > 0;
	}

	public String getFoundSerialPort() {
		return foundSerialPort;
	}

	public static String[] getSerialPortNames() {
		return SerialPortList.getPortNames();
	}
}
