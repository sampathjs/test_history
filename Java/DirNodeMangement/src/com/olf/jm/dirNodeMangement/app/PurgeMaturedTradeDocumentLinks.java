package com.olf.jm.dirNodeMangement.app;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.jm.dirNodeMangement.model.ActivityReport;
import com.olf.openjvs.OException;
import com.olf.openjvs.Transaction;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.io.Query;
import com.olf.openrisk.io.QueryResult;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.constrepository.ConstantNameException;
import com.openlink.util.constrepository.ConstantTypeException;
import com.olf.jm.logging.Logging;

@ScriptCategory({ EnumScriptCategory.Generic })
public class PurgeMaturedTradeDocumentLinks extends AbstractGenericScript {

	
	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "DMS";
	
	/** The Constant SUBCONTEXT used to identify entries in the const repository.. */
	public static final String SUBCONTEXT = "MaturedTradeRowCleanUp";
	
	public Context context;
	
	
	@Override
	public Table execute(Context context, EnumScriptCategory category,
			ConstTable table) {
		try {
			this.context = context;
			
			init();
			
			process();
			return context.getTableFactory().createTable();
		} catch (Exception e) {
			String errorMessage = "Error removing documents from matured deals. " + e.getMessage();
			Logging.error(errorMessage);
			ActivityReport.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}finally{
			Logging.close();
		}
		
		
	}

	private void process() {
		IOFactory iof = context.getIOFactory();
		
		ActivityReport.start();
		
		// Load trades to purge
		Table tradesToPurge = loadTradesToPurge();
		
		int tradeCount = tradesToPurge.getRowCount();
		Logging.debug("About to purge documents from " + tradeCount + " trades");
		for(int row = 0; row < tradeCount; row++) {
			int tranNum = tradesToPurge.getInt("query_result", row);
			Logging.debug("About to purge documents from tran num " + tranNum);

			Transaction tran = null;
			try {
				tran = Transaction.retrieve(tranNum);
				
				
				
				if(tran == null) {
					String message = "Error loading transaction for tran num " + tranNum;
				
					Logging.error(message);
					throw new RuntimeException(message);
				}
				
				// Force the loading of the deal document table to remove erros when deleting the doc.
				tran.getDealDocumentTable();

				
				try(Table documentsToPurge = loadRecordsToPurge(tranNum)) {
					
					Logging.debug("About to remove " + documentsToPurge.getRowCount() + " documents");
					for(int document = 0; document < documentsToPurge.getRowCount(); document++) {
			 			ActivityReport.purge(documentsToPurge.getInt("deal_tracking_num", document),
			 					documentsToPurge.getInt("doc_id", document),
			 					documentsToPurge.getInt("node_id", document), 
			 					documentsToPurge.getString("node_name", document), 
			 					documentsToPurge.getString("file_object_name", document), 
			 					documentsToPurge.getString("file_object_source", document));
			 			
			 			
						// Remove the deal documents
						tran.deleteDealDocument(documentsToPurge.getInt("doc_id", document));
					
						// Remove the dir_node entries	
						iof.runStoredProcedure("ol_delete_file_object", documentsToPurge.getInt("node_id", document));
					}
				}	
				
				tran.saveDealDocumentTable();
			} catch (OException e) {
				String message = "Error removing documents from  tran num " + tranNum + ". " + e.getMessage();
				
				Logging.error(message);
				throw new RuntimeException(message);
			} finally {
				if(tran != null) {
					try {
						tran.destroy();
					} catch (OException e) {
					}
					tran = null;
				}
			}
 
		}
		
	
		ActivityReport.finish();
	}
	
	
 	private Table loadRecordsToPurge(int tranNum) {
 		String sql =   " select doc_id, ab.deal_tracking_num, dn.node_id, dn.node_name, file_object_name,  file_object_source "
                     + " from deal_document_link ddl "
                     + " join dir_node dn on ddl.saved_node_id = dn.node_id "
                     + " join file_object fo on fo.node_id = dn.node_id "
                     + " join ab_tran ab on ddl.deal_tracking_num  = ab.deal_tracking_num   and tran_num = " + tranNum;
;

 		
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

	private Table loadTradesToPurge() {
		IOFactory iof = context.getIOFactory();
	
		Query query = iof.getQueries().getQuery(getQueryName());
		
		
		QueryResult results = query.execute();
		
		return results.asTable();
	}
	
	private String getQueryName() {
		String queryName = "PurgeMaturedTradeDocumentLinks";
		try {
			queryName = constRep.getStringValue("savedQueryName", queryName);
		} catch (ConstantTypeException | ConstantNameException | OException e) {
			throw new RuntimeException("Error loading the saved query name. " + e.getMessage());
		}
		
		Logging.debug("Using saved quesy " + queryName);
		return queryName;
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
