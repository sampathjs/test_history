package com.olf.jm.autosipopulation.gui;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;

import com.olf.embedded.application.Display;

/*
 * History:
 * 2015-06-24	V1.0	jwaechter	- initial version
 * 2015-08-26	V1.1	jwaechter	- Added space and enlarged window to capture more detailed message
 * 2015-09-09	V1.2	jwaechter	- Enlarged window
 * 2015-10-06	V1.3	jwaechter	- fixed thread synch issues
 */

/**
 * Class containing the confirmation dialog shown to the user in case 
 * there are either no external, no internal or no neither internal nor external
 * SIs.
 * The user can cancel deal processing. The dialog is initialised and positioned, but not shown.
 * The dialog has to be shown in the external class using the instance of this dialog. 
 * The external class has to check using "isFinished()" if the user has finished the input.
 * After finished, the methods isCancel() and isOk() can be used to check if the user has confirmed
 * or not.
 * @author jwaechter
 * @version 1.3
 */
public class ConfirmationDialog extends JDialog implements ActionListener {
	private final JPanel contentPanel = new JPanel();
	private final Display display;
	
	/**
	 * Indicates whether the user has pushed the ok button or not.
	 */
	private boolean ok=false;
	/**
	 * Indicates whether the user has pushed the cancel button or not.
	 */
	private boolean cancel=false;
	/**
	 * Indicates whether the user has finished the input and closed the dialog.
	 */
	private boolean finished=false;
	
	/**
	 * Contains the size of the window.
	 */
	private final Dimension size;

	/**
	 * Creates the dialog. 
	 * @param message the message shown to the user
	 * @param display the display object from the context.
	 */
	public ConfirmationDialog(String message, final Display display) {
		this.display = display;
		setBounds(100, 100, 650, 300);
		size = getBounds().getSize();
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);

		JLabel lblConfirmationText = new JLabel("<html><body>" + message + "</body></html>");
		lblConfirmationText.setBounds(10, 10, size.width-20, size.height-20);
		lblConfirmationText.setVerticalAlignment(SwingConstants.TOP);
		contentPanel.add(lblConfirmationText);
		{ 
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{ 
				JButton okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				okButton.addActionListener(this);
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);  
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(this);
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		String action = ae.getActionCommand();
		if (action.equals("OK")) {
			setOk(true);
			setCancel(false);
			this.dispose();
			setFinished(true);
//			if (display != null) {
//				display.unblock();				
//			}
		}
		if (action.equals("Cancel")) {
			setCancel (true);
			setOk(false);
			setFinished(true);
//			if (display != null) {
//				display.unblock();				
//			}
			this.dispose();
		}
	}

	public synchronized boolean isOk() {
		return ok;
	}

	public synchronized void setOk(boolean ok) {
		this.ok = ok;
	}

	public synchronized boolean isCancel() {
		return cancel;
	}

	public synchronized boolean isFinished() {
		return finished;
	}

	public synchronized void setFinished(boolean finished) {
		this.finished = finished;
	}
	
	public synchronized void setCancel(boolean cancel) {
		this.cancel = cancel;
	}

	public Dimension getSize() {
		return size;
	}
}
