package com.olf.jm.dirNodeMangement.app;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.jm.dirNodeMangement.model.ActivityReport;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

@ScriptCategory({ EnumScriptCategory.Generic })
public class OrphanedEntryCleanUp extends AbstractGenericScript {

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "DMS";
	
	/** The Constant SUBCONTEXT used to identify entries in the const repository.. */
	public static final String SUBCONTEXT = "OrphanedRowCleanUp";
	
	public Context context;
	
	@Override
	public Table execute(Context context, EnumScriptCategory category,
			ConstTable table) {
		try {
			this.context = context;
			
			init();
			
			process(context);
			return context.getTableFactory().createTable();
		} catch (Exception e) {
			String errorMessage = "Error clearing down orphaned document link records. " + e.getMessage();
			Logging.error(errorMessage);
			ActivityReport.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}finally{
			Logging.close();
		}
		
		
	}

 	private void process(Context context) {
 		ActivityReport.start();
 		
 		Table recordsToPurge = loadRecordsToPurge();
 		
 		ActivityReport.recordsToPurge(recordsToPurge.getRowCount());
 		
 		IOFactory iof = context.getIOFactory();
 		for(int row = 0; row < recordsToPurge.getRowCount(); row++) {

 			iof.runStoredProcedure("ol_delete_file_object", recordsToPurge.getInt("node_id", row));
 			ActivityReport.purge(recordsToPurge.getInt("node_id", row), 
 					recordsToPurge.getString("node_name", row), 
 					recordsToPurge.getString("file_object_name", row), 
 					recordsToPurge.getString("file_object_source", row));
 			
 		}
 		
 		ActivityReport.finish();
	 }
 	
 	private Table loadRecordsToPurge() {
 		String sql =   " select dn.node_id, dn.node_name, file_object_name,  file_object_source " 
 					 + " from dir_node dn "
 					 + " join file_object fo on fo.node_id = dn.node_id "
 					 + " where node_name like 'Tran Document %' and node_type = 9 and category= 4 and dn.node_id not in (select saved_node_id from deal_document_link) ";

 		
        IOFactory iof = context.getIOFactory();
        
        Logging.debug("About to run SQL. \n" + sql);
        
        
        Table recordsToPurge = null;
        try {
        	recordsToPurge = iof.runSQL(sql);
        } catch (Exception e) {
            String errorMessage = "Error executing SQL: " + sql + ". Error: " + e.getMessage();
            Logging.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        
        
        return recordsToPurge; 
 	}
 	
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception the exception
	 */	private void init() throws Exception {
		constRep = new ConstRepository(CONTEXT, SUBCONTEXT);

		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			Logging.init(this.getClass(), CONTEXT, SUBCONTEXT);
			
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}
	}

}
