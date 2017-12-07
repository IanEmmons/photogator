package org.virginiaso.photogator;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.WindowConstants;

public class SettingsDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	private static final String DIALOG_TITLE = "Settings";

	private JLabel headingLbl;
	private JPanel radioBtnPnl;
	private ButtonGroup radioBtnGrp;
	private JRadioButton consecutivePairsRadioBtn;
	private JRadioButton firstStartAfterReadyRadioBtn;
	private JButton okBtn;
	private JButton cancelBtn;
	private JPanel okCancelPnl;

	public SettingsDialog(JFrame owner) {
		super(owner, true);
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

		headingLbl = new JLabel("Set how Photogator computes time intervals:");

		radioBtnPnl = new JPanel();
		radioBtnGrp = new ButtonGroup();
		radioBtnPnl.setLayout(new BoxLayout(radioBtnPnl, BoxLayout.PAGE_AXIS));

		consecutivePairsRadioBtn = new JRadioButton("Consecutive start-end pairs");
		consecutivePairsRadioBtn.setActionCommand("consecutive-start-end");
		//consecutivePairsRadioBtn.setEnabled(true);
		radioBtnGrp.add(consecutivePairsRadioBtn);
		radioBtnPnl.add(consecutivePairsRadioBtn);

		firstStartAfterReadyRadioBtn = new JRadioButton("First start gate after ready");
		firstStartAfterReadyRadioBtn.setActionCommand("first-start-after-ready");
		//firstStartAfterReadyRadioBtn.setEnabled(true);
		radioBtnGrp.add(firstStartAfterReadyRadioBtn);
		radioBtnPnl.add(firstStartAfterReadyRadioBtn);

		radioBtnPnl.add(new JLabel(" "));	// spacer

		okBtn = new JButton("OK");
		okBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				onOkBtn();
			}
		});

		cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				onCancelBtn();
			}
		});

		okCancelPnl = new JPanel();
		okCancelPnl.setLayout(new BoxLayout(okCancelPnl, BoxLayout.LINE_AXIS));
		okCancelPnl.add(okBtn);
		okCancelPnl.add(cancelBtn);

		add(headingLbl, BorderLayout.PAGE_START);
		add(radioBtnPnl, BorderLayout.CENTER);
		add(okCancelPnl, BorderLayout.PAGE_END);
		pack();
	}

	private void onOkBtn() {
	}

	private void onCancelBtn() {
	}
}
