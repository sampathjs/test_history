package com.olf.jm.autosipopulation.gui;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.olf.embedded.application.Display;
import com.olf.jm.autosipopulation.model.Pair;

/*
 * History:
 * 2015-06-24	V1.0	jwaechter	- initial version
 * 2015-08-26	V1.1	jwaechter	- Added space and enlarged window to capture more detailed message
 * 2015-09-09	V1.2	jwaechter	- Enlarged window
 * 2015-10-06   V1.3	jwaechter	- fixed thread synch issues.
 */

/**
 * Contains a dialog to select the internal settlement instruction,
 * the external settlement instruction or both. <br/>
 * Dialog components sizes are static, no layout manager used. <br/>
 * The user might cancel the deal processing. <br/>
 * The intended usage is the following: <br/>
 * <ol>
 *   <li> The external class is creating an instance of this class </li>
 *   <li> This instance sets up the dialog, but does not show it</li>
 *   <li> The external class shows the dialog until like any other JDialog instance </li>
 *   <li> The external class polls if the user has finished input (use isFinished() withing a loop of Thread.sleep(long)) </li>
 *   <li> The external class retrieves the user input using isOk(), isCancel() and getSelectedIntSI()/getSelectedExtSI() </li>
 * </ol>
 * @author jwaechter
 * @version 1.3
 */
public class SelectDialog extends JDialog implements ActionListener {
	/**
	 * Indicates whether this dialog is used to ask the user to select
	 * the internal, external or both settlement instructions.
	 */
	public static enum ListMode { INT, EXT, INTEXT  }; 
	
	private final JPanel contentPanel = new JPanel();
	
	private final Display display;
	
	/**
	 * true, of the user has pressed the ok button, false if not.
	 */
	private boolean ok=false;
	/**
	 * true, of the user has pressed the cancel button, false if not.
	 */
	private boolean cancel=false;
	/**
	 * true, of the user has closed the dialog, false if not
	 */
	private boolean finished=false;
	
	/**
	 * The size of the dialog.
	 */
	private final Dimension size;
	
	/**
	 * If the user has pushed the ok button, this contains the
	 * selected internal si (ID, Name)
	 */
	private Pair<Integer, String> selectedIntSI;
	/**
	 * If the user has pushed the ok button, this contains the
	 * selected internal si (ID, Name)
	 */
	private Pair<Integer, String> selectedExtSI;

	/** 
	  * @see ListMode
	  */
	private final ListMode mode;

	/**
	 * The label to show messages to the user as reaction to user input.
	 * E.g. "Please select an internal SI" in case the user pushes the ok
	 * button without selecting a settlement instruction.
	 */
	private JLabel messageLabel;

	/**
	 * Creates the dialog. 
	 * @param message The message shown to the user
	 * @param display the display from the context to be used.
	 * @param mode The mode controls whether to show input lists for internal, external or both SI. 
	 * @param intSettleIds list of possible internal settlement instructions
	 * @param extSettleIds list of possible external settlement instructions
	 * @param preSelIntSI the settlement instruction to be pre selected for the internal settlement instruction
	 * @param preSelExtSI the settlement instruction to be pre selected for the external settlement instruction
	 */
	public SelectDialog(final String message, final Display display, 
			final ListMode mode,
			final List<Pair<Integer, String>> intSettleIds, 
			final List<Pair<Integer, String>> extSettleIds,
			final Pair<Integer, String> preSelIntSI,
			final Pair<Integer, String> preSelExtSI) {
		this.display = display;
		this.mode = mode;
		selectedIntSI = null;
		selectedExtSI = null;
		setBounds(100, 100, 650, 350);
		size = getBounds().getSize();
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);
		{
			JLabel lblMessageToUser = new JLabel("<html><body>" + message + "</body></html>");
			lblMessageToUser.setBounds(10, 10, size.width-20, size.height-20);
			lblMessageToUser.setVerticalAlignment(SwingConstants.TOP);
			contentPanel.add(lblMessageToUser);
		}

		if (mode == ListMode.INT || mode == ListMode.INTEXT) {
			JLabel lblSelectInternalSettlement = new JLabel("Select Internal Settlement Instruction");
			lblSelectInternalSettlement.setBounds(10, 175, 197, 14);
			contentPanel.add(lblSelectInternalSettlement);
			
			JComboBox<Pair<Integer,String>> internalSIList = new JComboBox<>(new Vector<Pair<Integer, String>>(intSettleIds));
			internalSIList.setEditable(false);
			internalSIList.addActionListener(this);
			internalSIList.setActionCommand("InternalSI");

			internalSIList.setBounds(222, 175, 300, 20);
			if (preSelIntSI != null) {
				internalSIList.setSelectedItem(preSelIntSI);
			} else {
				internalSIList.setSelectedIndex(-1);
			}
			selectedIntSI = (Pair<Integer,String>)internalSIList.getSelectedItem();
			contentPanel.add(internalSIList);
		}

		if (mode == ListMode.EXT || mode == ListMode.INTEXT) {
			JLabel lblSelectExternalSettlement = new JLabel("Select External Settlement Instruction");
			lblSelectExternalSettlement.setBounds(10, 200, 197, 14);
			contentPanel.add(lblSelectExternalSettlement);

			JComboBox<Pair<Integer,String>> externalSIList = new JComboBox<>(new Vector<Pair<Integer, String>>(extSettleIds));
			externalSIList.setEditable(false);
			externalSIList.addActionListener(this);
			externalSIList.setActionCommand("ExternalSI");
			
			externalSIList.setBounds(222, 200, 300, 20);
			if (preSelExtSI != null) {
				externalSIList.setSelectedItem(preSelExtSI);
			} else {
				externalSIList.setSelectedIndex(-1);
			}
			selectedExtSI = (Pair<Integer,String>)externalSIList.getSelectedItem();
			contentPanel.add(externalSIList);
		}
		
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, 	BorderLayout.SOUTH);
			messageLabel = new JLabel ();
			buttonPane.add(messageLabel);
			{
				JButton okButton = new JButton("OK");
				okButton.setActionCommand("OK");
				okButton.addActionListener(this);
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setActionCommand("Cancel");
				cancelButton.addActionListener(this);
				buttonPane.add(cancelButton);
			}
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent ae) {
		String action = ae.getActionCommand();
		if (action.equals("OK")) {
			if ((mode == ListMode.EXT || mode == ListMode.INTEXT) && selectedExtSI == null) {
				messageLabel.setText("Please select a valid external settlement instruction");
			} else if ((mode == ListMode.INT || mode == ListMode.INTEXT) && selectedIntSI == null) {
				messageLabel.setText("Please select a valid internal settlement instruction");				
			} else {
				setOk(true);
				setCancel (false);
				setFinished(true);
				this.dispose();
//				if (display != null) {
//					display.unblock();						
//				}
			}
		}
		if (action.equals("Cancel")) {
			setCancel(true);
			setOk(false);
			this.dispose();
			setFinished(true);
//			if (display != null) {
//				display.unblock();				
//			}
		}
		if (action.equals("ExternalSI")) {
			JComboBox<Pair<Integer, String>> source = (JComboBox) ae.getSource();
			selectedExtSI = (Pair<Integer,String>)source.getSelectedItem();			
		}
		
		if (action.equals("InternalSI")) {
			JComboBox<Pair<Integer, String>> source = (JComboBox) ae.getSource();
			selectedIntSI = (Pair<Integer,String>)source.getSelectedItem();
		}
	}

	/**
	 * Checks whether the ok button was pushed by the user to cancel the dialog or not.
	 */
	public synchronized boolean isOk() {
		return ok;
	}

	/**
	 * Checks whether the cancel button was pushed by the user to cancel the dialog or not.
	 */
	public synchronized boolean isCancel() {
		return cancel;
	}
	/**
	 * Retrieves the internal settlement instruction selected by the user.
	 */
	public synchronized Pair<Integer, String> getSelectedIntSI() {
		return selectedIntSI;
	}

	/**
	 * Retrieves the external settlement instruction selected by the user.
	 */
	public synchronized Pair<Integer, String> getSelectedExtSI() {
		return selectedExtSI;
	}

	/**
	 * Retrieves the size of the dialog.
	 */
	public Dimension getSize() {
		return size;
	}

	/**
	 * Checks whether user has finished the input(dialog window closed) or not.
	 */	
	public synchronized boolean isFinished() {
		return finished;
	}

	public synchronized void setOk(boolean ok) {
		this.ok = ok;
	}

	public synchronized void setCancel(boolean cancel) {
		this.cancel = cancel;
	}

	public synchronized void setFinished(boolean finished) {
		this.finished = finished;
	}
	
	
}
