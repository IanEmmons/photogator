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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;

import jssc.SerialPortList;

public class AboutDialog extends JDialog
{
	private static final long serialVersionUID = 1L;
	private static final String DIALOG_TITLE = "About %1$s";
	private static final String ABOUT_MSG_CONTENT_RSRC = "AboutMessage.html";

	private ImagePanel imgPanel;
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

		msgPane = new JEditorPane();
		msgPane.setEditable(false);
		setMsgContent();

		msgScrollPane = new JScrollPane(msgPane);
		msgScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		Dimension prefSize = new Dimension(
			(int) Math.round(1.618 * imgPanel.getPreferredSize().getWidth()),
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

			Path tmpPath = Files.createTempFile("PhotogatorAboutMessage", ".html");
			try (PrintWriter pwtr = new PrintWriter(tmpPath.toFile(), StandardCharsets.UTF_8.name()))
			{
				pwtr.format(content, Photogator.APP_NAME, getPortNamesHtml());
			}

			msgPane.setPage(tmpPath.toUri().toURL());
		}
		catch (IOException ex)
		{
			Photogator.ERR_LOG.format("Unable to load message content:  %1%s%n", ex.getMessage());
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

	private void onOkBtn(@SuppressWarnings("unused") ActionEvent evt)
	{
		setVisible(false);
	}
}
