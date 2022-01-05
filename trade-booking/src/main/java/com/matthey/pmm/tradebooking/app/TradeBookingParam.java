package com.matthey.pmm.tradebooking.app;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JRootPane;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.Display;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;

@ScriptCategory({ EnumScriptCategory.Generic })
public class TradeBookingParam extends AbstractGenericScript {

    /**
     * @param context
     *            the current script context
     * @param table
     *            the input parameters
     * @return a <code>Table</code> or null
     */
    public Table execute(final Context context, final ConstTable table) {
    	// setup return tables. Main table contains one row and two columns (succeeded (0/1) and a table with a list of files to process)
    	Table paramTaskReturn = context.getTableFactory().createTable("Trade Booking Param Return Table");
        paramTaskReturn.addColumn("Succeeded", EnumColType.Int);
        paramTaskReturn.addColumn("Files", EnumColType.Table);
        paramTaskReturn.addRow();
        Table fileList = context.getTableFactory().createTable("File List");
        fileList.addColumn("filename", EnumColType.String);        
        paramTaskReturn.setTable("Files", 0, fileList);
        
        // ask user to select a file.
        String selectedFile = displayDialog(context);
        if (selectedFile != null) {
        	fileList.addRow();
        	fileList.setString(0, 0, selectedFile);
        	paramTaskReturn.setValue("Succeeded", 0, 1);
        }        
    	return paramTaskReturn;
    }
    
	protected String displayDialog (Context context) {
		final Display display = context.getDisplay();
		String abOutdir = context.getSystemSetting("AB_OUTDIR");
		FileSelection dialog = new FileSelection("Select Input File for Trade Booking", abOutdir, display);
		dialog.setTitle("Trade Booking Tool File Selection");

		dialog.getRootPane().setWindowDecorationStyle(JRootPane.QUESTION_DIALOG);
		dialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		Dimension frameSize = dialog.getSize();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int top = (screenSize.height - frameSize.height) / 2;
		int left = (screenSize.width - frameSize.width) / 2;
		dialog.setLocation(left, top);
		dialog.setVisible(true);
		dialog.toFront();
		try {
			while (!dialog.isFinished()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					throw new RuntimeException (e);
				}			
			};			
		} catch (Throwable t) {
			dialog.dispose();
			throw t;
		}
		if (dialog.isOk()) {
			return dialog.getSelectedFile();
		} 
		return null;
	}
}
