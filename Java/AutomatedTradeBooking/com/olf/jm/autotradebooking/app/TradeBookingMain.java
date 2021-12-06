package com.olf.jm.autotradebooking.app;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.autotradebooking.logic.FileProcessor;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.openrisk.table.EnumColType;


@ScriptCategory({ EnumScriptCategory.Generic })
public class TradeBookingMain extends AbstractGenericScript {
	public static final String CONST_REPO_CONTEXT = "DealBooking";
	public static final String CONST_REPO_SUBCONTEXT = "MainScript";	
	
	private ConstRepository constRepo;
	
    /**
     * @param context
     *            the current script context
     * @param table
     *            the input parameters
     * @return a <code>Table</code> or null
     */
	@Override
    public Table execute(final Session session, final ConstTable table) {
    	init(session);
    	try {
        	if (!checkParamTable(table)) {
        		return null;
        	}
        	FileProcessor fileProcessor = new FileProcessor(session, constRepo);
        	Table fileTable = table.getTable("Files", 0);
        	for (int row = 0; row < table.getRowCount(); row++) {
        		String fileNameToProces = fileTable.getString("filename", row);
        		Logging.info("Now processing file' " + fileNameToProces + "'");
        		fileProcessor.processFile(fileNameToProces);
        		Logging.info("Processing of ' " + fileNameToProces + "' finished successfully");
        	}
        	return null;    		
    	} catch (Exception ex) {
    		Logging.error("Deal Booking Process Failed: " + ex.toString() + "\n " + ex.getMessage());
    		for (StackTraceElement ste : ex.getStackTrace()) {
    			Logging.error(ste.toString());
    		}
    		throw ex;
    	}
    }
    
	private boolean checkParamTable(ConstTable table) {
		if (table.getRowCount() != 1) {
			Logging.error("Row count on parameter table is " + table.getRowCount() + " expected is exactly 1");
			return false;
		}
		if (table.getColumnCount() != 2) {
			Logging.error("Column count on parameter table is " + table.getColumnCount() + " expected is exactly 2");
			return false;
		}
		if (!table.isValidColumn("Succeeded")) {
			Logging.error("Column 'Succeeded' on parameter table is not valid");
			return false;
		}
		
		if (!table.isValidColumn("Files")) {
			Logging.error("Column 'Files' on parameter table is not valid");
			return false;
		}
		
		if (table.getColumn("Succeeded").getType() != EnumColType.Int) {
			Logging.error("Column 'Succeeded' on parameter table is not of type Int");			
			return false;
		}

		if (table.getColumn("Files").getType() != EnumColType.Table) {
			Logging.error("Column 'Files' on parameter table is not of type Table");			
			return false;
		}
		
		if (table.getInt("Succeeded", 0) != 1) {
			Logging.info("Nothing to process as param script did not succeed or was cancelled");
			return false;
		}

		if (!table.getTable("Files", 0).isValidColumn("filename")) {
			Logging.error("Column 'filename' on subtable 'Files' on the parameter table is not valid");			
			return false;
		}

		if (table.getTable("Files", 0).getColumn("filename").getType() != EnumColType.String) {
			Logging.error("Column 'filename' on subtable 'Files' on the parameter table is not of type String");			
			return false;
		}
		
		if (table.getTable("Files", 0).getRowCount() == 0) {
			Logging.info("Nothing to process as no files to process have been specifiedin the param script");			
			return false;
		}
		Logging.info("Check of parameter table succeeded without issues found");
		return true;
	}

	/**
	 * Inits plugin log by retrieving logging settings from constants repository.
	 * @param context
	 */
	private void init(Session session) {
		try {
			constRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			
			try {
				Logging.init(this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}			
		} catch (OException e) {
			throw new RuntimeException (e);
		}		
		Logging.info("\n\n********************* Start of new run ***************************");

		try {
			UIManager.setLookAndFeel( // for dialogs that are used in pre process runs
					UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException (e);
		} catch (InstantiationException e) {
			throw new RuntimeException (e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException (e);
		} catch (UnsupportedLookAndFeelException e) {
			throw new RuntimeException (e);
		}
	}
    

}
