package org.virginiaso.photogator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.EventObject;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
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
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import org.virginiaso.serialport.ArduinoEvent;
import org.virginiaso.serialport.HeartBeatEvent;
import org.virginiaso.serialport.SerialPortReader;

import jssc.SerialPortException;

public class Photogator extends JFrame
{
	private static final long serialVersionUID = 1L;
	private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
	private static final boolean CORRECT_DEFAULT_FONT_SIZES = false;
	public static final String APP_NAME = "Photogator";
	private static final int TOOLBAR_IMAGE_SIZE = 24;
	private static final String SERIAL_PORT_PROP = "serial.port";
	private static final String SERIAL_PORT_ENV_VAR = "ARDUINO_SERIAL_PORT";
	private static final File PROPERTIES_FILE = new File("SerialPort.properties");
	private static final File LOG_FILE = new File(APP_NAME.toLowerCase() + ".log");
	private static final String NOT_CONNECTED_MSG = "Not connected";
	private static final String CONNECTED_MSG_FMT = "Connected to serial port %1$s";
	private static final String[] DIVISIONS = { "A", "B", "C" };
	private static final File SAVED_SESSION_DIR = new File("Saved" + APP_NAME + "Sessions");

	private static final SimpleDateFormat XSD_TIME_FMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	// These must match:
	private static final String SAVED_SESSION_FILENM_FMT = APP_NAME + "Session-%1$s%2$02d-%3$03d.txt";
	private static final Pattern SAVED_SESSION_FILENM_PARSER = Pattern
		.compile(APP_NAME + "Session-([ABC])([0-9]+)-([0-9]+)\\.txt", Pattern.CASE_INSENSITIVE);

	static final PrintStream ERR_LOG;

	private JToolBar toolBar;
	private JLabel divisionLbl;
	private JComboBox<String> divisionCombo;
	private JLabel teamNumLbl;
	private JSpinner teamNumSpinner;
	private JButton readyBtn;
	private JButton saveAndClearBtn;
	private JButton settingsBtn;
	private JButton aboutBtn;
	private JTextArea log;
	private JScrollPane logScrollPane;
	private JToolBar statusBar;
	private JLabel connectedLbl;

	private SerialPortReader portRdr = null;
	private ElapsedTimeComputeMethod computeMethod = ElapsedTimeComputeMethod.FIRST_START_AFTER_READY;
	private BeamBrokenEvent applicableStartEvent = null;
	private boolean isLogDirty = false;

	static
	{
		PrintStream errLog = System.out;
		try
		{
			errLog = new PrintStream(LOG_FILE, StandardCharsets.UTF_8.name());
		}
		catch (IOException ex)
		{
			ex.printStackTrace(errLog);
		}
		ERR_LOG = errLog;
		ArduinoEvent.registerMsgType(HeartBeatEvent.class);
		ArduinoEvent.registerMsgType(BeamBrokenEvent.class);
	}

	public Photogator()
	{
		initComponents();
		setLogDirty(false);
	}

	private void initComponents()
	{
		setName(APP_NAME);
		setTitle(APP_NAME);
		ImageIcon appIcon = getRsrcAsIcon("app-icon24", APP_NAME);
		setIconImage(appIcon.getImage());
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowOpened(WindowEvent evt)
			{
				windowOpenAction();
			}

			@Override
			public void windowClosing(WindowEvent evt)
			{
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

		readyBtn = createToolbarBtn(null, "Ready", "Ready for the next run", this::readyBtnAction);
		toolBar.add(readyBtn);

		saveAndClearBtn = createToolbarBtn("saveAndClear", "Save and Clear",
			"Save the log contents to a file and clear the log (cannot be undone)", this::saveBtnAction);
		toolBar.add(saveAndClearBtn);

		toolBar.addSeparator();

		settingsBtn = createToolbarBtn("settings", "Settings", APP_NAME + " settings", this::settingsBtnAction);
		toolBar.add(settingsBtn);

		aboutBtn = createToolbarBtn("about", "About", "About " + APP_NAME, this::aboutBtnAction);
		toolBar.add(aboutBtn);

		log = new JTextArea(200, 50);
		log.setText("");
		log.setEditable(false);
		log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, Math.round(13.0f * getFontScaleFactor())));
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

		setToolbarStateAccordingToSettings();
		setFrameSize();

		MacOSAdapter.setAboutMenuAction(this::aboutMenuAction);
		MacOSAdapter.setPreferencesMenuAction(this::settingsMenuAction);
	}

	private void setFrameSize()
	{
		GraphicsConfiguration grConfig = getGraphicsConfiguration();
		Rectangle screenBounds = grConfig.getBounds();
		Insets screenInsets = getToolkit().getScreenInsets(grConfig);
		Rectangle usableScreenSpace = new Rectangle(
			screenBounds.x + screenInsets.left,
			screenBounds.y + screenInsets.top,
			screenBounds.x + screenBounds.width - screenInsets.right - screenInsets.left,
			screenBounds.y + screenBounds.height - screenInsets.bottom - screenInsets.top);
		Rectangle currentBounds = getBounds();
		Rectangle newBounds = currentBounds.intersection(usableScreenSpace);
		setBounds(newBounds);
	}

	private static JButton createToolbarBtn(String imageName, String altText,
		String toolTipText, ActionListener listener)
	{
		JButton btn = new JButton();
		btn.setToolTipText(toolTipText);
		btn.addActionListener(listener);
		if (imageName == null || imageName.isEmpty())
		{
			btn.setIcon(createStrutIcon(altText));
			btn.setText(altText);
		}
		else
		{
			btn.setIcon(getRsrcAsIcon(imageName, altText));
		}
		return btn;
	}

	private static ImageIcon createStrutIcon(String description)
	{
		BufferedImage strut = new BufferedImage(1, TOOLBAR_IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = strut.createGraphics();
		g.setColor(new Color(0, 0, 0, 0));
		g.fillRect(0, 0, 1, TOOLBAR_IMAGE_SIZE);
		g.dispose();
		return new ImageIcon(strut, description);
	}

	private static ImageIcon getRsrcAsIcon(String rsrcName, String description)
	{
		ImageIcon result = null;

		Image img = loadImageRsrc(rsrcName);
		if (img != null)
		{
			img = img.getScaledInstance(TOOLBAR_IMAGE_SIZE, TOOLBAR_IMAGE_SIZE, Image.SCALE_SMOOTH);
			result = new ImageIcon(img, description);
		}
		return result;
	}

	public static Image loadImageRsrc(String rsrcName)
	{
		Image result = null;
		String rsrcPath = String.format("images/%1$s.png", rsrcName);
		URL imageURL = ImagePanel.class.getResource(rsrcPath);
		if (imageURL == null)
		{
			ERR_LOG.format("Unable to find resource '%1$s'%n", rsrcPath);
		}
		else
		{
			try
			{
				result = ImageIO.read(imageURL);
			}
			catch (IOException ex)
			{
				ex.printStackTrace(ERR_LOG);
			}
		}
		return result;
	}

	private boolean isLogDirty()
	{
		return isLogDirty;
	}

	private void setLogDirty(boolean newValue)
	{
		isLogDirty = newValue;
		saveAndClearBtn.setEnabled(isLogDirty);
	}

	void windowOpenAction()
	{
		try
		{
			String serialPortName = getExplicitSerialPortSelection();
			if (serialPortName == null)
			{
				InitializationDialog initDlg = new InitializationDialog(this);
				if (initDlg.isASerialPortPresent())
				{
					initDlg.setVisible(true);
					serialPortName = initDlg.getFoundSerialPort();
				}
				else
				{
					msgDlg(JOptionPane.ERROR_MESSAGE,
						"No serial ports are present.%nPerhaps the photogates are not connected to the computer.");
				}
			}

			if (serialPortName == null)
			{
				// User pressed the Exit button in the initialization dialog.
				dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
			}
			else
			{
				portRdr = new SerialPortReader(serialPortName, this::serialPortRecieveAction, ERR_LOG);
				connectedLbl.setText(String.format(CONNECTED_MSG_FMT, serialPortName));
			}
		}
		catch (SerialPortException ex)
		{
			ex.printStackTrace(ERR_LOG);
			connectedLbl.setText(NOT_CONNECTED_MSG);
			msgDlg(JOptionPane.ERROR_MESSAGE, "Unable to open serial port.  Detailed error message:%n%1$s%n%2$s",
				ex.getClass().getName(), ex.getMessage());
		}
	}

	void windowClosingAction()
	{
		try
		{
			if (portRdr != null)
			{
				portRdr.close();
			}
		}
		catch (SerialPortException ex)
		{
			ex.printStackTrace(ERR_LOG);
		}
	}

	private static String getExplicitSerialPortSelection()
	{
		String result = null;
		if (PROPERTIES_FILE.exists() && PROPERTIES_FILE.isFile())
		{
			Properties props = new Properties();
			try (InputStream in = new FileInputStream(PROPERTIES_FILE);
				Reader rdr = new InputStreamReader(in, StandardCharsets.UTF_8);)
			{
				props.load(rdr);
				result = blankToNull(props.getProperty(SERIAL_PORT_PROP));
			}
			catch (IOException ex)
			{
				ex.printStackTrace(ERR_LOG);
			}
		}

		if (result != null)
		{
			ERR_LOG.format("Got serial port '%1$s' from properties file '%2$s'.%n", result,
				PROPERTIES_FILE.getAbsolutePath());
		}
		else
		{
			result = blankToNull(System.getProperty(SERIAL_PORT_PROP));
		}

		if (result != null)
		{
			ERR_LOG.format("Got serial port '%1$s' from system properties.%n", result);
		}
		else
		{
			result = blankToNull(System.getenv(SERIAL_PORT_ENV_VAR));
		}

		if (result != null)
		{
			ERR_LOG.format("Got serial port '%1$s' from environment.%n", result);
		}
		return result;
	}

	private static String blankToNull(String str)
	{
		if (str == null)
		{
			return null;
		}
		else
		{
			String strTrimmed = str.trim();
			return (strTrimmed.isEmpty()) ? null : strTrimmed;
		}
	}

	private void readyBtnAction(@SuppressWarnings("unused") ActionEvent evt)
	{
		applicableStartEvent = null;
	}

	private void saveBtnAction(@SuppressWarnings("unused") ActionEvent evt)
	{
		if (log.getText().trim().isEmpty() || !isLogDirty())
		{
			msgDlg(JOptionPane.INFORMATION_MESSAGE, "Nothing to save.");
		}
		else
		{
			int option = confirmDlg(JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION,
				"Saving the display for team %1$s-%2$d and then clearing it", divisionCombo.getSelectedItem(),
				teamNumSpinner.getValue());

			if (option == JOptionPane.OK_OPTION)
			{
				try
				{
					saveDisplay();
					clearDisplay();
					applicableStartEvent = null;
				}
				catch (IOException ex)
				{
					ex.printStackTrace(ERR_LOG);
					msgDlg(JOptionPane.ERROR_MESSAGE, "Unable to save display.  Detailed error message:%n%1$s%n%2$s",
						ex.getClass().getName(), ex.getMessage());
				}
			}
		}
	}

	private void saveDisplay() throws IOException
	{
		if (ensureSessionDirExists())
		{
			String division = (String) divisionCombo.getSelectedItem();
			int teamNum = ((Integer) teamNumSpinner.getValue()).intValue();
			int sessionNum = getNextSessionNumber(division, teamNum);
			File newSessionFile = new File(SAVED_SESSION_DIR,
				String.format(SAVED_SESSION_FILENM_FMT, division, teamNum, sessionNum));
			try (BufferedWriter wtr = Files.newBufferedWriter(newSessionFile.toPath(), StandardCharsets.UTF_8,
				StandardOpenOption.CREATE_NEW))
			{
				wtr.write(log.getText());
			}
			setLogDirty(false);
		}
	}

	private static boolean ensureSessionDirExists()
	{
		boolean sessionDirExists = false;
		if (!SAVED_SESSION_DIR.exists())
		{
			SAVED_SESSION_DIR.mkdirs();
			sessionDirExists = true;
		}
		else if (!SAVED_SESSION_DIR.isDirectory())
		{
			msgDlg(
				null, JOptionPane.ERROR_MESSAGE, null, "" + "Unable to create the directory for saved sessions,%n"
					+ "     %1$s%n" + "because it already exists, but is not a directory",
				SAVED_SESSION_DIR.getAbsolutePath());
		}
		else
		{
			sessionDirExists = true;
		}
		return sessionDirExists;
	}

	private static int getNextSessionNumber(String division, int teamNum)
	{
		return 1 + Arrays.stream(SAVED_SESSION_DIR.listFiles()).filter(File::isFile)
			.map(f -> SAVED_SESSION_FILENM_PARSER.matcher(f.getName())).filter(Matcher::matches)
			.filter(m -> division.equalsIgnoreCase(m.group(1))).filter(m -> teamNum == Integer.parseInt(m.group(2)))
			.map(m -> Integer.parseInt(m.group(3))).max(Integer::compare).orElse(-1);
	}

	private void clearDisplay()
	{
		log.setText("");
		log.setCaretPosition(0);
		setLogDirty(false);
	}

	void settingsBtnAction(@SuppressWarnings("unused") ActionEvent evt)
	{
		SettingsDialog dlg = new SettingsDialog(this, computeMethod);
		computeMethod = dlg.getElapsedTimeComputeMethod();
		setToolbarStateAccordingToSettings();
	}

	void settingsMenuAction(@SuppressWarnings("unused") EventObject evt)
	{
		settingsBtnAction(null);
	}

	private void setToolbarStateAccordingToSettings()
	{
		readyBtn.setEnabled(computeMethod == ElapsedTimeComputeMethod.FIRST_START_AFTER_READY);
	}

	void aboutBtnAction(@SuppressWarnings("unused") ActionEvent evt)
	{
		@SuppressWarnings("unused")
		AboutDialog dlg = new AboutDialog(this);
	}

	void aboutMenuAction(@SuppressWarnings("unused") EventObject evt)
	{
		aboutBtnAction(null);
	}

	private void serialPortRecieveAction(String msg)
	{
		ArduinoEvent evt = ArduinoEvent.parse(msg);
		if (evt == null)
		{
			appendToLog(String.format("Error:  Unrecognized message format:  \"%1$s\"%n", msg));
		}
		else
		{
			appendToLog(evt.format());
			if (evt instanceof BeamBrokenEvent)
			{
				BeamBrokenEvent thisEvt = (BeamBrokenEvent) evt;
				if (computeMethod == ElapsedTimeComputeMethod.FIRST_START_AFTER_READY)
				{
					if (thisEvt.getSensorId() == SensorId.START && applicableStartEvent == null)
					{
						applicableStartEvent = thisEvt;
					}
					else if (thisEvt.getSensorId() == SensorId.FINISH && applicableStartEvent != null)
					{
						appendToLog(thisEvt.formatDifference(applicableStartEvent));
						applicableStartEvent = null;
					}
				}
				else if (computeMethod == ElapsedTimeComputeMethod.CONSECUTIVE_START_END_PAIR)
				{
					if (thisEvt.getSensorId() == SensorId.START)
					{
						applicableStartEvent = thisEvt;
					}
					else if (thisEvt.getSensorId() == SensorId.FINISH && applicableStartEvent != null)
					{
						appendToLog(thisEvt.formatDifference(applicableStartEvent));
						applicableStartEvent = null;
					}
				}
				else
				{
					throw new IllegalStateException(String.format("Unknown compute method '%1$s'", computeMethod));
				}
			}
		}
	}

	private void appendToLog(String msg)
	{
		if (msg != null && !msg.isEmpty())
		{
			log.append(msg);
			setLogDirty(true);
			log.setCaretPosition(log.getDocument().getLength());
		}
	}

	private void msgDlg(int msgType, String fmt, Object... args)
	{
		msgDlg(msgType, null, fmt, args);
	}

	private void msgDlg(int msgType, ImageIcon img, String fmt, Object... args)
	{
		msgDlg(this, msgType, img, fmt, args);
	}

	public static void msgDlg(JFrame parent, int msgType, ImageIcon img, String fmt, Object... args)
	{
		String msg = String.format(fmt, args);
		if (img == null)
		{
			JOptionPane.showMessageDialog(parent, msg, APP_NAME, msgType);
		}
		else
		{
			JOptionPane.showMessageDialog(parent, msg, APP_NAME, msgType, img);
		}
	}

	private int confirmDlg(int msgType, int btnChoices, String fmt, Object... args)
	{
		return JOptionPane.showConfirmDialog(this, // Parent window
			String.format(fmt, args), // Message
			APP_NAME, // Title
			btnChoices, // Option type determines button choices
			msgType); // Message type determines icon
	}

	public static void main(String[] args)
	{
		ERR_LOG.format("%1$s started at %2$s%n", APP_NAME, XSD_TIME_FMT.format(new Date()));

		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

			if (CORRECT_DEFAULT_FONT_SIZES)
			{
				magnifyAllDefaultFonts(getFontScaleFactor());
			}

			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					new Photogator().setVisible(true);
				}
			});
		}
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException
			| UnsupportedLookAndFeelException ex)
		{
			ex.printStackTrace(ERR_LOG);
			msgDlg(null, JOptionPane.ERROR_MESSAGE, null,
				"Unable to set look and feel.  Detailed error message:%n%1$s%n%2$s",
				ex.getClass().getName(), ex.getMessage());
		}
	}

	private static float getFontScaleFactor()
	{
		// On Windows, Swing seems to assume a screen resolution of 72 DPI:
		return isWindows() ? getScreenDPI() / 72.0f : 1.0f;
	}

	private static float getScreenDPI()
	{
		float screenDpi = Toolkit.getDefaultToolkit().getScreenResolution();

		ERR_LOG.format("Screen resolution, obtained from Toolkit:  %1$g dpi%n", screenDpi);

		//GraphicsDevice defaultScreenDevice = GraphicsEnvironment
		//	.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		//if (defaultScreenDevice instanceof CGraphicsDevice)	// True on Mac OS X
		//{
		//	CGraphicsDevice device = (CGraphicsDevice) defaultScreenDevice;
		//	screenDpi = (float) (device.getScaleFactor() * device.getYResolution());
		//
		//	ERR_LOG.format(""
		//		+ "Screen resolution, obtained from CGraphicsDevice:  %1$g dpi%n"
		//		+ "   Scale factor for retina display:                %2$d%n"
		//		+ "   True screen resolution:                         %3$g dpi%n",
		//		device.getYResolution(), device.getScaleFactor(), screenDpi);
		//}

		return screenDpi;
	}

	private static void magnifyAllDefaultFonts(float scaleFactor)
	{
		ERR_LOG.format("Magnifying default fonts by a factor of %1$g%n", scaleFactor);

		UIDefaults defaults = UIManager.getDefaults();
		Map<Object, Font> resizedFonts = defaults.keySet().stream()
			.filter(Objects::nonNull)
			.filter(key -> key.toString().toLowerCase().contains("font"))
			.filter(key -> defaults.getFont(key) != null)
			.collect(Collectors.toMap(
				Function.identity(),													// key mapper
				key -> magnifyDefaultFont(defaults, key, scaleFactor)));	// value mapper

		// Collect key-font pairs above, then put them to avoid concurrent mods:
		resizedFonts.forEach((key, font) -> UIManager.put(key, font));
	}

	private static Font magnifyDefaultFont(UIDefaults defaults, Object key, float scaleFactor)
	{
		Font oldFont = defaults.getFont(key);
		return oldFont.deriveFont(oldFont.getSize2D() * scaleFactor);
	}

	private static boolean isWindows() {
		ERR_LOG.format("OS name:  '%1$s'%n", OS_NAME);
		return OS_NAME.contains("win");
	}

	@SuppressWarnings("unused")
	private static boolean isMacOSX() {
		ERR_LOG.format("OS name:  '%1$s'%n", OS_NAME);
		return OS_NAME.contains("mac");
	}
}
