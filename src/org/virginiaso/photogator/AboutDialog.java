package org.virginiaso.photogator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.text.html.HTMLEditorKit;

import jssc.SerialPortList;

public class AboutDialog extends JDialog
{
	private static final long serialVersionUID = 1L;
	private static final boolean SHOW_STYLED_MSG_CONTENT = false;
	private static final String DIALOG_TITLE = "About %1$s";

	private ImagePanel imgPanel;
	private JTextArea msgText;
	private JEditorPane msgPane;
	private JScrollPane msgScrollPane;
	private Box contentBox;
	private JButton okBtn;
	private Box btnBox;

	public AboutDialog(JFrame owner)
	{
		super(owner, String.format(DIALOG_TITLE, Photogator.APP_NAME), true); // true for modal
		initComponents();
	}

	private void initComponents()
	{
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		imgPanel = new ImagePanel("app-icon");

		msgText = new JTextArea(String.format(""
			+ "%1$s%n"
			+ "Timing software for use with Virginia Science Olympiad’s photo gates.%n"
			+ "%n"
			+ "© 2017, Virginia Science Olympiad.%n"
			+ "All rights reserved.%n"
			+ "%n"
			+ "%2$s",
			Photogator.APP_NAME, getPortNames()));
		msgText.setEditable(false);

		//msgPane = new JEditorPane("text/html", createTextContent());
		msgPane = new JEditorPane();
		msgPane.setEditorKit(new HTMLEditorKit());
		msgPane.setContentType("text/html");
		msgPane.setText(createTextContent());
		msgPane.setEditable(false);
		if (SHOW_STYLED_MSG_CONTENT)
		{
			Photogator.msgDlg(null, JOptionPane.INFORMATION_MESSAGE, null, createTextContent());
		}

		msgScrollPane = new JScrollPane(msgPane);
		msgScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		msgScrollPane.setPreferredSize(imgPanel.getPreferredSize());
		msgScrollPane.setMinimumSize(new Dimension(10, 10));

		contentBox = Box.createHorizontalBox();
		contentBox.add(imgPanel);
		contentBox.add(Box.createHorizontalStrut(3));
		contentBox.add(msgText);
		if (SHOW_STYLED_MSG_CONTENT)
		{
			contentBox.add(Box.createHorizontalStrut(3));
			contentBox.add(msgScrollPane);
		}
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

	private static String createTextContent()
	{
		try
		(
			InputStream in = AboutDialog.class.getResourceAsStream("AboutMessage.html");
			InputStreamReader rdr = new InputStreamReader(in, StandardCharsets.UTF_8);
			BufferedReader brdr = new BufferedReader(rdr);
		)
		{
			String htmlFmt = brdr.lines().collect(Collectors.joining(System.lineSeparator()));
			return String.format(htmlFmt, Photogator.APP_NAME, getPortNamesHtml());
		}
		catch (IOException ex)
		{
			ex.printStackTrace(Photogator.ERR_LOG);
			return ex.toString();
		}
	}

	private void onOkBtn(@SuppressWarnings("unused") ActionEvent evt)
	{
		setVisible(false);
	}

	public static String getPortNames()
	{
		String[] portNames = SerialPortList.getPortNames();
		if (portNames == null || portNames.length == 0)
		{
			return "There are no available serial ports.";
		}
		else
		{
			return Arrays.stream(portNames).collect(Collectors.joining(
				String.format("%n   "),
				String.format("Available serial ports:%n   "),
				""));
		}
	}

	public static String getPortNamesHtml()
	{
		String[] portNames = SerialPortList.getPortNames();
		if (portNames == null || portNames.length == 0)
		{
			return "There are no available serial ports.";
		}
		else
		{
			return Arrays.stream(portNames).collect(Collectors.joining(
				String.format("</li>%n   <li>"),
				String.format("Available serial ports:%n   <li>"),
				String.format("</li>%n")));
		}
	}
}
