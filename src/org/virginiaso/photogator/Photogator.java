package org.virginiaso.photogator;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import org.virginiaso.serialport.ArduinoEvent;
import org.virginiaso.serialport.HeartBeatEvent;
import org.virginiaso.serialport.SerialPortReader;

import jssc.SerialPortException;

public class Photogator extends JFrame {
	private static final long serialVersionUID = 1L;
	private static final String APP_NAME = "Photogator";
	private static final String SERIAL_PORT_PROP = "serial.port";
	private static final String SERIAL_PORT_ENV_VAR = "ARDUINO_SERIAL_PORT";
	private static final File PROPERTIES_FILE = new File("SerialPort.properties");
	private static final File LOG_FILE = new File(APP_NAME.toLowerCase() + ".log");
	private static final String NOT_CONNECTED_MSG = "Not connected";
	private static final String CONNECTED_MSG_FMT = "Connected to serial port %1$s";
	private static final String[] DIVISIONS = { "A", "B", "C" };
	private static final File SAVED_SESSION_DIR = new File("Saved" + APP_NAME + "Sessions");

	// These must match:
	private static final String SAVED_SESSION_FILENM_FMT = APP_NAME + "Session-%1$s%2$02d-%3$03d.txt";
	private static final Pattern SAVED_SESSION_FILENM_PARSER = Pattern.compile(
		APP_NAME + "Session-([ABC])([0-9]+)-([0-9]+)\\.txt", Pattern.CASE_INSENSITIVE);

	static final PrintStream ERR_LOG;

	private JToolBar toolBar;
	private JLabel divisionLbl;
	private JComboBox<String> divisionCombo;
	private JLabel teamNumLbl;
	private JSpinner teamNumSpinner;
	private JButton saveBtn;
	private JButton clearBtn;
	private JButton aboutBtn;
	private JTextArea log;
	private JScrollPane logScrollPane;
	private JToolBar statusBar;
	private JLabel connectedLbl;

	private BeamBrokenEvent lastEvent = null;
	private SerialPortReader portRdr = null;
	private boolean isLogDirty = false;

	static {
		PrintStream errLog = System.out;
		try {
			errLog = new PrintStream(LOG_FILE, StandardCharsets.UTF_8.name());
		} catch (IOException ex) {
			ex.printStackTrace(errLog);
		}
		ERR_LOG = errLog;
		ArduinoEvent.registerMsgType(HeartBeatEvent.class);
		ArduinoEvent.registerMsgType(BeamBrokenEvent.class);
	}

	public Photogator() {
		initComponents();
		setLogDirty(false);
	}

	private void initComponents() {
		setName(APP_NAME);
		setTitle(APP_NAME);
		ImageIcon appIcon = createIcon("app-icon24", APP_NAME);
		setIconImage(appIcon.getImage());
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent evt) {
				windowOpenAction();
			}

			@Override
			public void windowClosing(WindowEvent evt) {
				windowClosingAction();
			}
		});

		toolBar = new JToolBar(SwingConstants.HORIZONTAL);
		toolBar.setFloatable(false);
		toolBar.setRollover(true);

		divisionLbl = new JLabel("Division:");
		toolBar.add(divisionLbl);

		divisionCombo = new JComboBox<>(DIVISIONS);
		divisionCombo.setSelectedIndex(1);
		toolBar.add(divisionCombo);

		teamNumLbl = new JLabel("Team:");
		toolBar.add(teamNumLbl);

		// Args are initial value, min, max, and step:
		teamNumSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
		toolBar.add(teamNumSpinner);

		toolBar.addSeparator();

		saveBtn = createToolbarBtn("save-as24", "Save", "Save the log contents to a file", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				saveBtnAction(evt);
			}
		});
		toolBar.add(saveBtn);

		clearBtn = createToolbarBtn("delete24", "Clear", "Clear the log contents (cannot be undone)", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				clearBtnAction(evt);
			}
		});
		toolBar.add(clearBtn);

		toolBar.addSeparator();

		aboutBtn = createToolbarBtn("about24", "About", "About " + APP_NAME, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				aboutBtnAction(evt);
			}
		});
		//aboutBtn.setIcon(appIcon);
		toolBar.add(aboutBtn);

		log = new JTextArea(200, 50);
		log.setText("");
		log.setEditable(false);
		log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
		logScrollPane = new JScrollPane(log);

		statusBar = new JToolBar(SwingConstants.HORIZONTAL);
		statusBar.setFloatable(false);
		statusBar.setRollover(true);

		connectedLbl = new JLabel(NOT_CONNECTED_MSG);
		connectedLbl.setToolTipText("Shows the serial port to which the Arduino is connected");
		statusBar.add(connectedLbl);

		add(toolBar, BorderLayout.PAGE_START);
		add(logScrollPane, BorderLayout.CENTER);
		add(statusBar, BorderLayout.PAGE_END);
		pack();
	}

	private JButton createToolbarBtn(String imageName, String altText, String toolTipText, ActionListener listener) {
		JButton btn = new JButton();
		btn.setToolTipText(toolTipText);
		btn.addActionListener(listener);
		ImageIcon img = createIcon(imageName, altText);
		if (img == null) {
			btn.setText(altText);
		} else {
			btn.setIcon(img);
		}
		return btn;
	}

	private ImageIcon createIcon(String path, String description) {
		URL imageURL = (path == null || path.isEmpty())
			? null
			: getClass().getResource(String.format("images/%1$s.png", path));
		if (imageURL == null) {
			ERR_LOG.format("Unable to find resource '%1$s'%n", path);
			return null;
		} else {
			return new ImageIcon(imageURL, description);
		}
	}

	private boolean isLogDirty() {
		return isLogDirty;
	}

	private void setLogDirty(boolean newValue) {
		isLogDirty = newValue;
		saveBtn.setEnabled(isLogDirty);
	}

	private void windowOpenAction() {
		try {
			String serialPortName = getExplicitSerialPortSelection();
			if (serialPortName == null) {
				InitializationDialog initDlg = new InitializationDialog(this);
				if (initDlg.isASerialPortPresent()) {
					initDlg.setVisible(true);
					serialPortName = initDlg.getFoundSerialPort();
				} else {
					msgDlg(JOptionPane.ERROR_MESSAGE,
						"No serial ports are present.%nPerhaps the photogates are not connected to the computer.");
				}
				initDlg.dispose();
			}

			if (serialPortName == null) {
				// User pressed the Exit button in the initialization dialog.
				this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
			} else {
				portRdr = new SerialPortReader(serialPortName, this::serialPortRecieveAction, ERR_LOG);
				connectedLbl.setText(String.format(CONNECTED_MSG_FMT, serialPortName));
			}
		} catch (SerialPortException ex) {
			ex.printStackTrace(ERR_LOG);
			connectedLbl.setText(NOT_CONNECTED_MSG);
			msgDlg(JOptionPane.ERROR_MESSAGE,
				"Unable to open serial port.  Detailed error message:%n%1$s%n%2$s",
				ex.getClass().getName(), ex.getMessage());
		}
	}

	private void windowClosingAction() {
		try {
			if (portRdr != null) {
				portRdr.close();
			}
		} catch (SerialPortException ex) {
			ex.printStackTrace(ERR_LOG);
		}
	}

	private static String getExplicitSerialPortSelection() {
		String result = null;
		if (PROPERTIES_FILE.exists() && PROPERTIES_FILE.isFile()) {
			Properties props = new Properties();
			try (
				InputStream in = new FileInputStream(PROPERTIES_FILE);
				Reader rdr = new InputStreamReader(in, StandardCharsets.UTF_8);
			) {
				props.load(rdr);
				result = blankToNull(props.getProperty(SERIAL_PORT_PROP));
			} catch (IOException ex) {
				ex.printStackTrace(ERR_LOG);
			}
		}

		if (result != null) {
			ERR_LOG.format("Got serial port '%1$s' from properties file '%2$s'.%n",
				result, PROPERTIES_FILE.getAbsolutePath());
		} else {
			result = blankToNull(System.getProperty(SERIAL_PORT_PROP));
		}

		if (result != null) {
			ERR_LOG.format("Got serial port '%1$s' from system properties.%n", result);
		} else {
			result = blankToNull(System.getenv(SERIAL_PORT_ENV_VAR));
		}

		if (result != null) {
			ERR_LOG.format("Got serial port '%1$s' from environment.%n", result);
		}
		return result;
	}

	private static String blankToNull(String str) {
		if (str == null) {
			return null;
		} else {
			str = str.trim();
			return (str.isEmpty()) ? null : str;
		}
	}

	private void saveBtnAction(ActionEvent evt) {
		if (log.getText().trim().isEmpty()) {
			msgDlg(JOptionPane.INFORMATION_MESSAGE, "There is no text to save.");
		} else if (!isLogDirty()) {
			msgDlg(JOptionPane.INFORMATION_MESSAGE, "Already saved.");
		} else {
			int option = JOptionPane.showConfirmDialog(this,				// Parent window
				"Would you like to clear the display after saving it?",	// Message
				APP_NAME,																// Title
				JOptionPane.YES_NO_CANCEL_OPTION,								// Button choices
				JOptionPane.QUESTION_MESSAGE);									// Icon

			if (option == JOptionPane.YES_OPTION || option == JOptionPane.NO_OPTION) {
				try {
					saveDisplay();
					if (option == JOptionPane.YES_OPTION) {
						clearDisplay();
					}
				} catch (IOException ex) {
					ex.printStackTrace(ERR_LOG);
					msgDlg(JOptionPane.ERROR_MESSAGE,
						"Unable to save display.  Detailed error message:%n%1$s%n%2$s",
						ex.getClass().getName(), ex.getMessage());
				}
			}
		}
	}

	private void saveDisplay() throws IOException {
		if (ensureSessionDirExists()) {
			String division = (String) divisionCombo.getSelectedItem();
			int teamNum = (int) teamNumSpinner.getValue();
			int sessionNum = getNextSessionNumber(division, teamNum);
			File newSessionFile = new File(SAVED_SESSION_DIR,
				String.format(SAVED_SESSION_FILENM_FMT, division, teamNum, sessionNum));
			try (
				OutputStream out = new FileOutputStream(newSessionFile);
				Writer wtr = new OutputStreamWriter(out, StandardCharsets.UTF_8);
			) {
				wtr.write(log.getText());
			}
			setLogDirty(false);
		}
	}

	private static boolean ensureSessionDirExists() {
		boolean sessionDirExists = false;
		if (!SAVED_SESSION_DIR.exists()) {
			SAVED_SESSION_DIR.mkdirs();
			sessionDirExists = true;
		} else if (!SAVED_SESSION_DIR.isDirectory()) {
			String msg = String.format(""
				+ "Unable to create the directory for saved sessions,%n"
				+ "     %1$s%n"
				+ "because it already exists, but is not a directory",
				SAVED_SESSION_DIR.getAbsolutePath());
			JOptionPane.showMessageDialog(null, msg, APP_NAME, JOptionPane.ERROR_MESSAGE);
		} else {
			sessionDirExists = true;
		}
		return sessionDirExists;
	}

	private static int getNextSessionNumber(String division, int teamNum) {
		return 1 + Arrays.stream(SAVED_SESSION_DIR.listFiles())
			.filter(f -> f.isFile())
			.map(f -> SAVED_SESSION_FILENM_PARSER.matcher(f.getName()))
			.filter(m -> m.matches())
			.filter(m -> division.equalsIgnoreCase(m.group(1)))
			.filter(m -> teamNum == Integer.parseInt(m.group(2)))
			.map(m -> Integer.parseInt(m.group(3)))
			.max(Integer::compare)
			.orElse(-1);
	}

	private void clearBtnAction(ActionEvent evt) {
		int option = JOptionPane.NO_OPTION;
		if (isLogDirty()) {
			option = JOptionPane.showConfirmDialog(this,							// Parent window
				"Would you like to save the display before clearing it?",	// Message
				APP_NAME,																	// Title
				JOptionPane.YES_NO_CANCEL_OPTION,									// Button choices
				JOptionPane.QUESTION_MESSAGE);										// Icon
		}
		if (option == JOptionPane.YES_OPTION || option == JOptionPane.NO_OPTION) {
			try {
				if (option == JOptionPane.YES_OPTION) {
					saveDisplay();
				}
				clearDisplay();
			} catch (IOException ex) {
				ex.printStackTrace(ERR_LOG);
				msgDlg(JOptionPane.ERROR_MESSAGE,
					"Unable to save display.  Detailed error message:%n%1$s%n%2$s",
					ex.getClass().getName(), ex.getMessage());
			}
		}
	}

	private void clearDisplay() {
		log.setText("");
		log.setCaretPosition(0);
		setLogDirty(false);
	}

	private void aboutBtnAction(ActionEvent evt) {
		ImageIcon img = createIcon("app-icon", APP_NAME);
		msgDlg(JOptionPane.INFORMATION_MESSAGE, img, ""
			+ "%1$s%n"
			+ "Photo-gate timing software%n"
			+ "%n"
			+ "© 2017, Virginia Science Olympiad.%n"
			+ "All rights reserved.%n"
			+ "%n"
			+ "%2$s",
			APP_NAME, getPortNames());
	}

	private static String getPortNames() {
		String[] portNames = InitializationDialog.getSerialPortNames();
		if (portNames == null || portNames.length == 0) {
			return "There are no available serial ports.";
		} else {
			return Arrays.stream(portNames).collect(Collectors.joining(
				String.format("%n   "),
				String.format("Available serial ports:%n   "),
				""));
		}
	}

	private void serialPortRecieveAction(String msg) {
		ArduinoEvent evt = ArduinoEvent.parse(msg);
		if (evt == null) {
			log.append(String.format("Error:  Unrecognized message format:  \"%1$s\"%n", msg));
			log.setCaretPosition(log.getDocument().getLength());
			setLogDirty(true);
		} else if (evt instanceof HeartBeatEvent) {
			// Do nothing
		} else if (evt instanceof BeamBrokenEvent) {
			BeamBrokenEvent bbEvt = (BeamBrokenEvent) evt;
			log.append(bbEvt.format());
			if (bbEvt.follows(lastEvent)) {
				log.append(bbEvt.difference(lastEvent));
			}
			log.setCaretPosition(log.getDocument().getLength());
			setLogDirty(true);
			lastEvent = bbEvt;
		}
	}

	private void msgDlg(int msgType, String fmt, Object... args) {
		msgDlg(msgType, null, fmt, args);
	}

	private void msgDlg(int msgType, ImageIcon img, String fmt, Object... args) {
		String title = APP_NAME;
		String msg = String.format(fmt, args);
		if (img == null) {
			JOptionPane.showMessageDialog(this, msg, title, msgType);
		} else {
			JOptionPane.showMessageDialog(this, msg, title, msgType, img);
		}
	}

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				new Photogator().setVisible(true);
			}
		});
	}
}