package org.virginiaso.photogator;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
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

public class InitializationDialog extends JDialog
{
	private static final long serialVersionUID = 1L;
	private static final String DIALOG_TITLE = "Connecting...";
	private static final String HEADING_TEXT = "Searching for photogates on serial port:          ";
	private static final String EXIT_BTN_TEXT = "Exit";
	private static final int TIMER_INITIAL_DELAY_MS = 500;
	private static final int TIMER_INTERVAL_MS = 100;
	private static final Object LOCK = new Object();

	/*
	 * Five seconds may seem excessive, but it takes the Arduino a considerable time
	 * (about 2.5 seconds) to "wake up" after the serial port connection is made.
	 */
	private static final long TIME_LIMIT_FOR_ARDUINO_HEARTBEAT_MS = 5000;

	private JLabel headingLbl;
	private ButtonGroup serialPortBtnGrp;
	private Box serialPortBtnBox;
	private List<JRadioButton> serialPortBtns;
	private JProgressBar progBar;
	private JButton exitBtn;
	private Box progAndExitBox;
	private Timer timer;
	private int currentRadioBtnIndex = -1;
	private long currentRadioBtnStartTime = System.currentTimeMillis() - 2 * TIME_LIMIT_FOR_ARDUINO_HEARTBEAT_MS;
	private SerialPortReader portRdr = null;
	private boolean arduinoDetected = false;
	private String foundSerialPort = null;

	public InitializationDialog(JFrame frame)
	{
		super(frame, true); // make's this a modal dialog
		initComponents();
	}

	private void initComponents()
	{
		setTitle(DIALOG_TITLE);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		headingLbl = new JLabel(HEADING_TEXT);

		serialPortBtnGrp = new ButtonGroup();
		serialPortBtnBox = Box.createVerticalBox();
		serialPortBtnBox.add(headingLbl);
		serialPortBtnBox.add(Box.createVerticalStrut(3));

		Font btnFont = new Font(Font.MONOSPACED, Font.PLAIN, headingLbl.getFont().getSize());
		serialPortBtns = Arrays.stream(getSerialPortNames())
			.sorted()
			.distinct()
			.map(portName -> {
				JRadioButton btn = new JRadioButton(portName);
				btn.setActionCommand(portName);
				btn.setEnabled(false);
				btn.setFont(btnFont);
				serialPortBtnGrp.add(btn);
				serialPortBtnBox.add(btn);
				return btn;
			})
			.collect(Collectors.toList());
		serialPortBtnBox.setBorder(BorderFactory.createEmptyBorder(15, 15, 0, 15));

		progBar = new JProgressBar(SwingConstants.HORIZONTAL);
		progBar.setIndeterminate(true);

		exitBtn = new JButton(EXIT_BTN_TEXT);
		exitBtn.addActionListener(this::onExitBtn);

		progAndExitBox = Box.createHorizontalBox();
		progAndExitBox.add(progBar);
		progAndExitBox.add(exitBtn);
		progAndExitBox.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));

		getContentPane().add(serialPortBtnBox, BorderLayout.CENTER);
		getContentPane().add(progAndExitBox, BorderLayout.PAGE_END);
		pack();
		setLocationRelativeTo(getOwner());

		timer = new Timer(TIMER_INTERVAL_MS, this::onTimerEvent);
		timer.setInitialDelay(TIMER_INITIAL_DELAY_MS);
		timer.start();
	}

	private void onTimerEvent(@SuppressWarnings("unused") ActionEvent evt)
	{
		if (!isASerialPortPresent())
		{
			// Do nothing
		}
		else if (isArduinoDetected())
		{
			timer.stop();
			foundSerialPort = portRdr.getSerialPortName();
			disconnectFromSerialPort();
			setVisible(false);
		}
		else if (System.currentTimeMillis() - currentRadioBtnStartTime > TIME_LIMIT_FOR_ARDUINO_HEARTBEAT_MS)
		{
			// Terminate existing serial port connection, if any:
			disconnectFromSerialPort();

			// Move to the next serial port:
			currentRadioBtnStartTime = System.currentTimeMillis();
			++currentRadioBtnIndex;
			currentRadioBtnIndex %= serialPortBtns.size();
			JRadioButton currentBtn = serialPortBtns.get(currentRadioBtnIndex);
			currentBtn.setSelected(true);
			String portName = currentBtn.getActionCommand();

			// Connect to portName:
			try
			{
				portRdr = new SerialPortReader(portName, this::serialPortRecieveAction, Photogator.ERR_LOG);
			}
			catch (SerialPortException ex)
			{
				ex.printStackTrace(Photogator.ERR_LOG);
			}
		}
	}

	private void serialPortRecieveAction(String msg)
	{
		ArduinoEvent evt = ArduinoEvent.parse(msg);
		if (evt == null)
		{
			Photogator.ERR_LOG.format("Unrecognized message '%1$s'%n", msg);
		}
		else if (evt instanceof HeartBeatEvent || evt instanceof BeamBrokenEvent)
		{
			setArduinoDetected(true);
		}
	}

	private void disconnectFromSerialPort()
	{
		if (portRdr != null)
		{
			try
			{
				portRdr.close();
			}
			catch (SerialPortException ex)
			{
				ex.printStackTrace(Photogator.ERR_LOG);
			}
			portRdr = null;
		}
	}

	private void onExitBtn(@SuppressWarnings("unused") ActionEvent evt)
	{
		timer.stop();
		foundSerialPort = null;
		disconnectFromSerialPort();
		setVisible(false);
	}

	private boolean isArduinoDetected()
	{
		synchronized (LOCK)
		{
			return arduinoDetected;
		}
	}

	private void setArduinoDetected(boolean newValue)
	{
		synchronized (LOCK)
		{
			arduinoDetected = newValue;
		}
	}

	public boolean isASerialPortPresent()
	{
		return serialPortBtns.size() > 0;
	}

	public String getFoundSerialPort()
	{
		return foundSerialPort;
	}

	public static String[] getSerialPortNames()
	{
		return SerialPortList.getPortNames();
	}
}
