package com.olf.jm.utilities.tablesplitter;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openjvs.Ask;
import com.olf.openjvs.OException;
import com.olf.openjvs.Util;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;
import  com.olf.jm.logging.Logging;

/*
 * History:
 * 2017-07-11	V1.0	lma	- Initial Version
 */

/**
 * Plugin to split tables according to user input or values retrieved from constants repository. <br/>
 * Logic: 
 * <ol>
 *   <li> 
 *   	Retrieve contents of user table provided by user input/ and or ConstRep entry 
 *   {@value #CONST_REPO_CONTEXT}\{@value #CONST_REPO_SUBCONTEXT}\srcTableName
 *   </li>
 *   <li>
 *     Sort the user table by column "row_id" if this row id exists 
 *   </li>
 *   <li>
 *   	Split contents of the source table into in total {@link #outputTableCount} tables.
 *      Table names are following the pattern {@link #srcTableName}_dd with dd being a zero padded number to
 *      two digits between 1 and {@link #outputTableCount}.
 *   </li>
 *   <li>
 *     If necessary delete existing tables having the same names as described above.
 *   <li>
 *   <li>
 *   	Copy chunks of consecutive rows from the source table to each of the destination tables.
 *   </li>
 *   <li>
 *      Save the destination tables to database.
 *   </li>
 * </ol>
 * @author jwaechter
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class TableSplitter_Mode2 extends AbstractGenericScript {
	public static final String CONST_REPO_CONTEXT = "Migration"; // context of constants repository
	public static final String CONST_REPO_SUBCONTEXT = "TableSplitter"; // sub context of constants repository

	private static String srcTableName = "";
	private static int outputTableCount = 6;
	
    public Table execute(final Session session, final ConstTable table) {

    	init(session);
    	Logging.info("Starting Table Splitter");
    	try {
        	Table srcTable = process(session); 
        	Logging.info("Table Splitter finished successfully");
        	return srcTable;    		
    	} catch (Throwable t) {
    		Logging.error(t.toString());
    		throw t;
    	}finally{
    		Logging.close();
    	}
   }

	private Table process(final Session session) {
		try {
    		if (Util.canAccessGui() == 1) {    			
    			Logging.info("Can access GUI");
    			srcTableName = Ask.getString("Source Table Name", srcTableName);
    			outputTableCount = Integer.parseInt(Ask.getString("Output table count", Integer.toString(outputTableCount)));
    		} else {
    			Logging.info("Can't access GUI");
    		}
		} catch (OException e) {
			throw new RuntimeException ("Error retrieving user input");
		}
    	
    	Table srcTable = session.getIOFactory().getUserTable(srcTableName).retrieveTable();
    	if (srcTable.getColumnId("row_id") >= 0) {
        	Logging.info("Column 'row_id' found - sorting");
    		srcTable.sort("row_id", false);
    	} else {
        	Logging.info("Column 'row_id' NOT found");    		
    	}
    	Table[] dstTables = new Table[outputTableCount];
    	
    	for (int i=0; i < outputTableCount; i++) {
    		dstTables[i] = srcTable.cloneStructure();
    		String tableName = String.format(srcTable.getName() + "_%02d", i+1);
    		dstTables[i].setName(tableName);
    		try {
    			Logging.info("Trying to clear data in user table " + tableName);   			
    			session.getIOFactory().getUserTable(tableName).clearRows();  			    			
    			Logging.info("Successfully clear all data in user table " + tableName);
    		} catch (RuntimeException ex) {
    			Logging.warn("Could not clear user table " + tableName);
    		}
    	}
    	for (int row=srcTable.getRowCount()-1; row >= 0; row--) {
    		int tableNum = (((srcTable.getRowCount() - row)*outputTableCount)/(srcTable.getRowCount()+1))%outputTableCount;
    		int dstRow = dstTables[tableNum].addRow().getNumber();
    		dstTables[tableNum].copyRowData(srcTable, row, dstRow);
    	}
    	for (int i=0; i < outputTableCount; i++) {
			Logging.info("Insert data into user table " + dstTables[i].getName());
			session.getIOFactory().getUserTable(dstTables[i].getName()).insertRows(dstTables[i]);
			Logging.info("Saving user table " + dstTables[i].getName() + " completed");
    	}
		return srcTable;
	}
    
	private void init(Session session) {
		String abOutdir = session.getSystemSetting("AB_OUTDIR");
		try {
			ConstRepository constRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			// retrieve constants repository entry "logLevel" using default value "info" in case if it's not present:
			String logLevel = constRepo.getStringValue("logLevel", "info"); 
			String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
			String logDir = constRepo.getStringValue("logDir", abOutdir);
			try {
				Logging.init(this.getClass(),CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			srcTableName = constRepo.getStringValue("srcTableName", srcTableName);
			outputTableCount = constRepo.getIntValue("outputTableCount", outputTableCount);
		} catch (OException e1) {
		}
	}
}
