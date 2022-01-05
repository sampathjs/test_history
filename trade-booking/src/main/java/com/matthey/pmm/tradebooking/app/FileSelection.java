package com.matthey.pmm.tradebooking.app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;

import com.olf.embedded.application.Display;

/*
 * History:
 * 2022-09-30	V1.0	jwaechter	- initial version
 */

/**
 * Class used to retrieve the input file from the user
 * @author jwaechter
 * @version 1.0
 */
public class FileSelection extends JDialog implements ActionListener {
	private final JPanel contentPanel = new JPanel();
	
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
	 * Textfield containing the selected file
	 */
	private JTextField selectedFile;

	/**
	 * Creates the dialog. 
	 * @param message the message shown to the user
	 * @param display the display object from the context.
	 */
	public FileSelection(String message, String abOutdir, final Display display) {
		setBounds(100, 100, 650, 300);
		size = getBounds().getSize();
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);

		selectedFile = new JTextField(abOutdir);
		selectedFile.setBounds(10, 80, size.width-20, 20);
		selectedFile.setEnabled(true);
		selectedFile.setEditable(false);
		
		JLabel lblConfirmationText = new JLabel("<html><body>" + message + "</body></html>");
		lblConfirmationText.setBounds(10, 10, size.width-20, 20);
		lblConfirmationText.setVerticalAlignment(SwingConstants.TOP);
		contentPanel.add(lblConfirmationText);
		JButton fileChooserOpener = new JButton ("Select File");
		fileChooserOpener.setBounds(10, 40, size.width-20, 20);
		fileChooserOpener.setVerticalAlignment(SwingConstants.TOP);
		fileChooserOpener.addActionListener(x -> {
			JFileChooser fc = new JFileChooser(selectedFile.getText());
			int returnVal = fc.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
	            File file = fc.getSelectedFile();
	            selectedFile.setText(file.getPath());
	        } else {
	        	// cancelled
	        }
		});
		contentPanel.add(fileChooserOpener);
		contentPanel.add(selectedFile);

		JLabel filler = new JLabel("<html><body></body></html>");
		filler.setBounds(10, 120, size.width-80, 20);
		filler.setVerticalAlignment(SwingConstants.TOP);
		contentPanel.add(fileChooserOpener);
		
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);
		JButton okButton = new JButton("OK");
		okButton.setActionCommand("OK");
		okButton.addActionListener(this);
		buttonPane.add(okButton);
		getRootPane().setDefaultButton(okButton);  
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		cancelButton.setActionCommand("Cancel");
		buttonPane.add(cancelButton);
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		String action = ae.getActionCommand();
		if (action.equals("OK")) {
			setOk(true);
			setCancel(false);
			this.dispose();
			setFinished(true);
		}
		if (action.equals("Cancel")) {
			setCancel (true);
			setOk(false);
			setFinished(true);
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
	
	public synchronized String getSelectedFile () {
		return selectedFile.getText();
	}

	public Dimension getSize() {
		return size;
	}
}
