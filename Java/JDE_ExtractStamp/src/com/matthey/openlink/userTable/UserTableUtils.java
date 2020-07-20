package com.matthey.openlink.userTable;

import com.matthey.openlink.enums.EnumUserJmSlDocTracking;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.EnumQueryResultTable;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.io.QueryResult;
import com.olf.openrisk.table.Table;
import com.olf.jm.logging.Logging;

public class UserTableUtils {
	
	public final static String trackingTableName = "USER_jm_sl_doc_tracking";

	public static Table getTrackingData(int[] documentIds, Session session) {
		
		IOFactory iof = session.getIOFactory();
		
		try(QueryResult qryResult = iof.createQueryResult(EnumQueryResultTable.Plugin)) {

			qryResult.add(documentIds);

			//String sql  = "select * from " + trackingTableName  + " where documnet_num = " + documentId;
			String sql =  " select sl.* from " + trackingTableName  + " sl " 
					+ " join " + qryResult.getDatabaseTableName()  
					+ " q on q.query_result = sl.document_num and q.unique_id = " + qryResult.getId()
					+ " order by sl.document_num";

			Logging.debug("About to run SQL. \n" + sql);	

			Table tableData = null;
			try {
				tableData = iof.runSQL(sql);
			} catch (Exception e) {
				String errorMessage = "Error executing SQL: " + sql + ". Error: " + e.getMessage();
				Logging.error(errorMessage);
				throw new RuntimeException(errorMessage);
			}

			if (tableData == null) {
				String errorMessage = "Error checking if document exsists in table " + trackingTableName;
				Logging.error(errorMessage);
				throw new RuntimeException(errorMessage);				
			}	
			
			return tableData;
		} catch (Exception e) {
			String errorMessage = "Error loading tracking details. Error: " + e.getMessage();
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
	}
	
	static public String getCurrentStatus(int documentId, Table trackingData) {
		
		int row = trackingData.findSorted(trackingData.getColumnId(EnumUserJmSlDocTracking.DOCUMENT_NUM.getColumnName()), documentId, 0);
		
		if(row >= 0) {
			return trackingData.getString(EnumUserJmSlDocTracking.SL_STATUS.getColumnName(), row);
		}
		
		return null;
	}
	
	/**
	 * Creates an empty table with the same structure as the user table the data is being inserted into.
	 *
	 * @return the table
	 */
	static public Table createTableStructure(Session session) {
		Table userTable = session.getIOFactory().getUserTable(UserTableUtils.trackingTableName).getTableStructure();
		
		if (userTable == null) {
			String errorMessage = "Error creating structure for user table " + UserTableUtils.trackingTableName;
				Logging.error(errorMessage);
				throw new RuntimeException(errorMessage);				
		}
		
		return userTable;
	}	
}
