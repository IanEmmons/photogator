package org.virginiaso.photogator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;

import jssc.SerialPortList;

public final class AboutDialog extends JDialog
{
	private static final long serialVersionUID = 1L;
	private static final String DIALOG_TITLE = "About %1$s";
	private static final String ABOUT_MSG_CONTENT_RSRC = "AboutMessage.html";

	private Photogator photogator;
	private ImagePanel imgPanel;
	private JEditorPane msgPane;
	private JScrollPane msgScrollPane;
	private Box contentBox;
	private JButton okBtn;
	private Box btnBox;

	public AboutDialog(Photogator photogator)
	{
		super(photogator, DIALOG_TITLE.formatted(Photogator.APP_NAME), true); // true for modal
		this.photogator = photogator;
		initComponents();
	}

	private void initComponents()
	{
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		imgPanel = new ImagePanel("app-icon");

		msgPane = new JEditorPane();
		msgPane.setEditable(false);
		setMsgContent();

		msgScrollPane = new JScrollPane(msgPane);
		msgScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		Dimension prefSize = new Dimension(
			(int) Math.round(1.8 * imgPanel.getPreferredSize().getWidth()),
			(int) Math.round(imgPanel.getPreferredSize().getHeight()));
		msgScrollPane.setPreferredSize(prefSize);
		msgScrollPane.setMinimumSize(new Dimension(10, 10));

		contentBox = Box.createHorizontalBox();
		contentBox.add(imgPanel);
		contentBox.add(Box.createHorizontalStrut(3));
		contentBox.add(msgScrollPane);
		contentBox.setBorder(BorderFactory.createEmptyBorder(15, 15, 0, 15));

		okBtn = new JButton("OK");
		okBtn.setActionCommand(okBtn.getText());
		okBtn.addActionListener(this::onOkBtn);
		okBtn.setDefaultCapable(true);

		btnBox = Box.createHorizontalBox();
		btnBox.add(Box.createHorizontalGlue());
		btnBox.add(okBtn);
		btnBox.add(Box.createHorizontalGlue());
		btnBox.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));

		getRootPane().setDefaultButton(okBtn);
		getRootPane().registerKeyboardAction(this::onOkBtn,
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			JComponent.WHEN_IN_FOCUSED_WINDOW);

		getContentPane().add(contentBox, BorderLayout.CENTER);
		getContentPane().add(btnBox, BorderLayout.PAGE_END);
		pack();
		setLocationRelativeTo(getOwner());

		setVisible(true);
	}

	private void setMsgContent()
	{
		try
		{
			String content;
			try
			(
				InputStream in = AboutDialog.class.getResourceAsStream(ABOUT_MSG_CONTENT_RSRC);
				InputStreamReader rdr = new InputStreamReader(in, StandardCharsets.UTF_8);
				BufferedReader brdr = new BufferedReader(rdr);
			)
			{
				content = brdr.lines().collect(Collectors.joining(System.lineSeparator()));
			}

			var vendor = getClass().getPackage().getImplementationVendor();
			var version = getClass().getPackage().getImplementationVersion();

			var tmpPath = Files.createTempFile("PhotogatorAboutMessage", ".html");
			try (var pwtr = new PrintWriter(tmpPath.toFile(), StandardCharsets.UTF_8))
			{
				pwtr.format(content, Photogator.APP_NAME, version, vendor,
					getPortNamesHtml(photogator.getSerialPortName()));
			}

			msgPane.setPage(tmpPath.toUri().toURL());
		}
		catch (IOException ex)
		{
			ex.printStackTrace(Photogator.ERR_LOG);
		}
	}

	public static String getPortNamesHtml(String connectedPort)
	{
		String[] portNames = SerialPortList.getPortNames();
		if (portNames == null || portNames.length == 0)
		{
			return "<p>There are no available serial ports.</p>";
		}
		else
		{
			return Stream.of(portNames)
				.map(port -> port.equals(connectedPort)
					? "<b>%1$s (connected)</b>".formatted(port)
					: port)
				.collect(Collectors.joining(
					"</li>\n   <li>",
					"<p>Available serial ports:</p>\n<ul>\n   <li>",
					"</li>\n</ul>\n"));
		}
	}

	private void onOkBtn(ActionEvent evt)
	{
		setVisible(false);
	}
}
