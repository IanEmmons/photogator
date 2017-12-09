package org.virginiaso.photogator;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

public class SettingsDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = 1L;
	private static final String DIALOG_TITLE = "Settings";

	private ElapsedTimeComputeMethod etComputeMethod;
	private JLabel headingLbl;
	private JRadioButton consecutivePairsRadioBtn;
	private JRadioButton firstStartAfterReadyRadioBtn;
	private ButtonGroup radioBtnGrp;
	private Box radioBtnBox;
	private JButton cancelBtn;
	private JButton okBtn;
	private Box okCancelBox;

	public SettingsDialog(JFrame owner, ElapsedTimeComputeMethod elapsedTimeComputeMethod) {
		super(owner, true);	// make's this a modal dialog
		etComputeMethod = elapsedTimeComputeMethod;
		initComponents();
	}

	private void initComponents() {
		setTitle(DIALOG_TITLE);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		headingLbl = new JLabel("Elapsed time computation method:");
		consecutivePairsRadioBtn = new JRadioButton("Consecutive start-end pairs");
		firstStartAfterReadyRadioBtn = new JRadioButton("First start gate after ready");

		radioBtnGrp = new ButtonGroup();
		radioBtnGrp.add(consecutivePairsRadioBtn);
		radioBtnGrp.add(firstStartAfterReadyRadioBtn);

		cancelBtn = new JButton("Cancel");
		cancelBtn.setActionCommand(cancelBtn.getText());
		cancelBtn.addActionListener(this);

		radioBtnBox = Box.createVerticalBox();
		radioBtnBox.add(headingLbl);
		radioBtnBox.add(Box.createVerticalStrut(3));
		radioBtnBox.add(consecutivePairsRadioBtn);
		radioBtnBox.add(firstStartAfterReadyRadioBtn);
		radioBtnBox.setBorder(BorderFactory.createEmptyBorder(15, 15, 0, 15));

		okBtn = new JButton("OK");
		okBtn.setActionCommand(okBtn.getText());
		okBtn.addActionListener(this);
		okBtn.setDefaultCapable(true);

		okCancelBox = Box.createHorizontalBox();
		okCancelBox.add(Box.createHorizontalGlue());
		okCancelBox.add(okBtn);
		okCancelBox.add(cancelBtn);
		okCancelBox.add(Box.createHorizontalGlue());
		okCancelBox.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));

		getRootPane().setDefaultButton(okBtn);
		getRootPane().registerKeyboardAction(this,
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			JComponent.WHEN_IN_FOCUSED_WINDOW);

		getContentPane().add(radioBtnBox, BorderLayout.CENTER);
		getContentPane().add(okCancelBox, BorderLayout.PAGE_END);
		pack();
		setLocationRelativeTo(getOwner());
		
		switch (etComputeMethod) {
		case consecutiveStartEndPair:
			consecutivePairsRadioBtn.setSelected(true);
			break;
		case firstStartAfterReady:
			firstStartAfterReadyRadioBtn.setSelected(true);
			break;
		}
		
		setVisible(true);
	}

	public void actionPerformed(ActionEvent evt) {
		setVisible(false);
		if (okBtn.getActionCommand().equals(evt.getActionCommand())) {
			if (consecutivePairsRadioBtn.isSelected()) {
				etComputeMethod = ElapsedTimeComputeMethod.consecutiveStartEndPair;
			} else if (firstStartAfterReadyRadioBtn.isSelected()) {
				etComputeMethod = ElapsedTimeComputeMethod.firstStartAfterReady;
			}
		}
	}

	public ElapsedTimeComputeMethod getElapsedTimeComputeMethod() {
		return etComputeMethod;
	}
}
