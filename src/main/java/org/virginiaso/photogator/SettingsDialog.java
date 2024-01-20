package org.virginiaso.photogator;

import java.awt.BorderLayout;
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

public final class SettingsDialog extends JDialog
{
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

	public SettingsDialog(JFrame owner, ElapsedTimeComputeMethod elapsedTimeComputeMethod)
	{
		super(owner, DIALOG_TITLE, true); // true makes this a modal dialog
		etComputeMethod = elapsedTimeComputeMethod;
		initComponents();
	}

	private void initComponents()
	{
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		headingLbl = new JLabel("Elapsed time computation method:");
		consecutivePairsRadioBtn = new JRadioButton("Consecutive start-end pairs");
		firstStartAfterReadyRadioBtn = new JRadioButton("First start gate after ready");

		radioBtnGrp = new ButtonGroup();
		radioBtnGrp.add(consecutivePairsRadioBtn);
		radioBtnGrp.add(firstStartAfterReadyRadioBtn);

		cancelBtn = new JButton("Cancel");
		cancelBtn.setActionCommand(cancelBtn.getText());
		cancelBtn.addActionListener(evt -> onCancelBtn());

		radioBtnBox = Box.createVerticalBox();
		radioBtnBox.add(headingLbl);
		radioBtnBox.add(Box.createVerticalStrut(3));
		radioBtnBox.add(consecutivePairsRadioBtn);
		radioBtnBox.add(firstStartAfterReadyRadioBtn);
		radioBtnBox.setBorder(BorderFactory.createEmptyBorder(15, 15, 0, 15));

		okBtn = new JButton("OK");
		okBtn.setActionCommand(okBtn.getText());
		okBtn.addActionListener(evt -> onOkBtn());
		okBtn.setDefaultCapable(true);

		okCancelBox = Box.createHorizontalBox();
		okCancelBox.add(Box.createHorizontalGlue());
		okCancelBox.add(okBtn);
		okCancelBox.add(cancelBtn);
		okCancelBox.add(Box.createHorizontalGlue());
		okCancelBox.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));

		getRootPane().setDefaultButton(okBtn);
		getRootPane().registerKeyboardAction(evt -> onCancelBtn(),
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			JComponent.WHEN_IN_FOCUSED_WINDOW);

		getContentPane().add(radioBtnBox, BorderLayout.CENTER);
		getContentPane().add(okCancelBox, BorderLayout.PAGE_END);
		pack();
		setLocationRelativeTo(getOwner());

		JRadioButton rb = switch (etComputeMethod) {
		case CONSECUTIVE_START_END_PAIR -> consecutivePairsRadioBtn;
		case FIRST_START_AFTER_READY -> firstStartAfterReadyRadioBtn;
		};
		rb.setSelected(true);

		setVisible(true);
	}

	private void onOkBtn()
	{
		setVisible(false);
		if (consecutivePairsRadioBtn.isSelected())
		{
			etComputeMethod = ElapsedTimeComputeMethod.CONSECUTIVE_START_END_PAIR;
		}
		else if (firstStartAfterReadyRadioBtn.isSelected())
		{
			etComputeMethod = ElapsedTimeComputeMethod.FIRST_START_AFTER_READY;
		}
	}

	private void onCancelBtn()
	{
		setVisible(false);
	}

	public ElapsedTimeComputeMethod getElapsedTimeComputeMethod()
	{
		return etComputeMethod;
	}
}
