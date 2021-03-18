package com.jm.tpm.support.util;

import java.io.File;

import com.jm.eod.common.Utils;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.constrepository.ConstRepository;
import  com.olf.jm.logging.Logging;


public class TPMExecuteQueryChecks implements IScript {
	
	private static final String CONTEXT = "Support";
	private static final String SUBCONTEXT = "TPM_Maintenance_Wflow";
	private ConstRepository repository = null;
	
	@Override
	public void execute(IContainerContext arg0) throws OException {
		
		try{
			Logging.init(this.getClass(),CONTEXT, SUBCONTEXT);
    	}catch(Error ex){
    		throw new RuntimeException("Failed to initialise log file:"+ ex.getMessage());
    	}
		
		Table queryList = Util.NULL_TABLE;
		Table tOutput = Util.NULL_TABLE; 
		Table priorityList = Util.NULL_TABLE; 
        
        try {
        	repository = new ConstRepository(CONTEXT, SUBCONTEXT);
        	
        	Logging.info("Script execution starts...");
        	priorityList = fetchPriorityList();
        	int rows = priorityList.getNumRows();
        	for (int row = 1; row <= rows; row++) { 
        		queryList= fetchQueryList(priorityList.getInt("priority", row));
        		processQueries(queryList);
        	}
        	 
            Logging.info("Script execution ends...");
            
        } catch (OException oe) {
        	Logging.error(oe.getMessage());
        	Util.exitFail(oe.getMessage());
        	
        } finally {
        	Logging.close();
        	if (Table.isTableValid(queryList) == 1) {
        		queryList.destroy();
        	}
        }
	}
	
	private void processQueries(Table queryList) throws OException { 
		Table tOutput = Util.NULL_TABLE;       
		 int rows = queryList.getNumRows();
         if (rows == 0) {
         	Logging.info("No rows retrieved from USER_generic_wflow_query_list table");
         	return;
         }
		 Logging.info(String.format("%d rows retrieved from USER_generic_wflow_query_list table", rows));
         for (int row = 1; row <= rows; row++) {
         	String query = queryList.getString("query", row);
         	String queryName = queryList.getString("query_name", row);
         	int exactRows = queryList.getInt("exact_expected_rows", row);
         	int maxRows = queryList.getInt("max_expected_rows", row);
         	Logging.info(String.format("Running loop for query name - %s, query - %s, exact_expected_rows - %d, max_expected_rows - %d", queryName, query, exactRows, maxRows));
         	
         	try {
         		tOutput = executeQuery(query, queryName);
 				if (Table.isTableValid(tOutput) != 1) {
 					Logging.info(String.format("Invalid table retrieved for query (%s), moving to next query", queryName));
 					continue;
 				}
 				
 				int outputRows = tOutput.getNumRows();
 				Logging.info(String.format("Non-zero rows (%d) retrieved for query (%s)", outputRows, queryName));
 				
 				/**
 				 * If Exact_Rows = 0 AND Max_Rows = 0, send email if Output_Rows > 0
 				 * If Exact_Rows > 0, send email if Output_Rows != Exact_Rows 
 				 * If Exact_Rows = 0 AND Max_Rows > 0, send email if Output_Rows > Max_Rows
 				 */
 				if ((exactRows == 0 && maxRows == 0 && outputRows > 0)
 						|| (exactRows > 0 && outputRows > 0 && outputRows != exactRows)
 						|| (exactRows == 0 && maxRows > 0 && outputRows > maxRows)) {
 					Logging.info(String.format("Output rows (%d) doesn't satisfy the criteria (for exact_rows & max_rows) setup in user table", outputRows));
 					Logging.info(String.format("Sending email to Support team for query (%s)", queryName));
 					sendEmail(tOutput, queryName);
 					
 				} else {
 					Logging.info(String.format("Output rows (%d) satisfy the criteria (for exact_rows & max_rows) setup in user table, moving to next query", outputRows));
 				}
 				
         	} finally {
         		if (Table.isTableValid(tOutput) == 1) {
         			tOutput.destroy();
             	}
         	}
         }
		
	}

	private Table fetchPriorityList() throws OException {
		Table priorityList = Util.NULL_TABLE; 
        int retval;
        
        try {
        	priorityList= Table.tableNew();
        	String sqlQuery = "SELECT DISTINCT(u.priority) priority FROM USER_generic_wflow_query_list u WHERE u.active = 1 ORDER BY u.priority ASC ";
        	Logging.info(String.format("Executing SQL query - %s", sqlQuery));
        	retval = DBaseTable.execISql(priorityList, sqlQuery);
        	 
            if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
            	throw new OException(String.format("Error in executing query - %s", sqlQuery));
            } 
            
        } catch (OException oe) {
        	throw new OException(String.format("Error in executing query. Error Message - %s", oe.getMessage()));        	
		} 
        
        return priorityList;
	}



	private Table fetchQueryList(int priority) throws OException {
		Table queryList = Util.NULL_TABLE;
		Table tQueries = Util.NULL_TABLE;
        int retval;
        
        try {
        	queryList= Table.tableNew();
        	String sqlQuery = "SELECT u.* FROM USER_generic_wflow_query_list u WHERE u.active = 1 AND u.priority ="+priority+"  ORDER BY u.query_name,sequence";
        	Logging.info(String.format("Executing SQL query - %s", sqlQuery));
        	retval = DBaseTable.execISql(queryList, sqlQuery);
        	
            if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
            	throw new OException(String.format("Error in executing query - %s", sqlQuery));
            }
            
            tQueries = queryList.cloneTable();
            tQueries.delCol("active");
            tQueries.delCol("sequence");
            tQueries.delCol("id");
            
            String queryName = null;
            String fullQuery = "";
            
            int rows = queryList.getNumRows();
            for (int row = 1; row <= rows; row++) {
            	String currQueryName = queryList.getString("query_name", row);
            	String partQuery = queryList.getString("query", row);
            	
            	if (queryName != null && !queryName.equals(currQueryName)) {
            		int rowNum = tQueries.addRow();
            		tQueries.setInt("exact_expected_rows", rowNum, queryList.getInt("exact_expected_rows", (row - 1)));
            		tQueries.setInt("max_expected_rows", rowNum, queryList.getInt("max_expected_rows", (row - 1)));
            		tQueries.setString("query", rowNum, fullQuery);
            		tQueries.setString("query_name", rowNum, queryName);
            		
            		queryName = currQueryName;
            		fullQuery = partQuery + " ";
            		
            	} else {
            		fullQuery += partQuery + " ";
            		queryName = currQueryName;
            	}
            	
            	if (row == rows) {
            		int rowNum = tQueries.addRow();
            		tQueries.setInt("exact_expected_rows", rowNum, queryList.getInt("exact_expected_rows", row));
            		tQueries.setInt("max_expected_rows", rowNum, queryList.getInt("max_expected_rows", row));
            		tQueries.setString("query", rowNum, fullQuery);
            		tQueries.setString("query_name", rowNum, queryName);
            	}
            }
            
        } catch (OException oe) {
        	throw new OException(String.format("Error in executing query. Error Message - %s", oe.getMessage()));
        	
		} finally {
        	if (Table.isTableValid(queryList) == 1) {
        		queryList.destroy();
        	}
        }
        
        return tQueries;
	}

	/**
	 * Method to fetch all active queries present in USER_generic_wflow_query_list table.
	 * @return queryList
	 * @throws OException
	 */
	private Table fetchQueryList() throws OException {
		Table queryList = Util.NULL_TABLE;
		Table tQueries = Util.NULL_TABLE;
        int retval;
        
        try {
        	queryList= Table.tableNew();
        	String sqlQuery = "SELECT u.* FROM USER_generic_wflow_query_list u WHERE u.active = 1 ORDER BY u.query_name,sequence";
        	Logging.info(String.format("Executing SQL query - %s", sqlQuery));
        	retval = DBaseTable.execISql(queryList, sqlQuery);
        	
            if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
            	throw new OException(String.format("Error in executing query - %s", sqlQuery));
            }
            
            tQueries = queryList.cloneTable();
            tQueries.delCol("active");
            tQueries.delCol("sequence");
            tQueries.delCol("id");
            
            String queryName = null;
            String fullQuery = "";
            
            int rows = queryList.getNumRows();
            for (int row = 1; row <= rows; row++) {
            	String currQueryName = queryList.getString("query_name", row);
            	String partQuery = queryList.getString("query", row);
            	
            	if (queryName != null && !queryName.equals(currQueryName)) {
            		int rowNum = tQueries.addRow();
            		tQueries.setInt("exact_expected_rows", rowNum, queryList.getInt("exact_expected_rows", (row - 1)));
            		tQueries.setInt("max_expected_rows", rowNum, queryList.getInt("max_expected_rows", (row - 1)));
            		tQueries.setString("query", rowNum, fullQuery);
            		tQueries.setString("query_name", rowNum, queryName);
            		
            		queryName = currQueryName;
            		fullQuery = partQuery + " ";
            		
            	} else {
            		fullQuery += partQuery + " ";
            		queryName = currQueryName;
            	}
            	
            	if (row == rows) {
            		int rowNum = tQueries.addRow();
            		tQueries.setInt("exact_expected_rows", rowNum, queryList.getInt("exact_expected_rows", row));
            		tQueries.setInt("max_expected_rows", rowNum, queryList.getInt("max_expected_rows", row));
            		tQueries.setString("query", rowNum, fullQuery);
            		tQueries.setString("query_name", rowNum, queryName);
            	}
            }
            
        } catch (OException oe) {
        	throw new OException(String.format("Error in executing query. Error Message - %s", oe.getMessage()));
        	
		} finally {
        	if (Table.isTableValid(queryList) == 1) {
        		queryList.destroy();
        	}
        }
        
        return tQueries;
	}
	
	/**
	 * Method to execute a query retrieved from the user table.
	 * @param query
	 * @param queryName
	 * @return tOutput
	 * @throws OException
	 */
	private Table executeQuery(String query, String queryName) throws OException {
		Table tOutput = Util.NULL_TABLE;
		
		try {
			tOutput = Table.tableNew();
			DBaseTable.execISql(tOutput, query);
			
		} catch (OException oe) {
			Logging.error(String.format("Error in executing query (%s). Error Message - %s", queryName,
        			oe.getMessage()));
		}
		
		return tOutput;
	}
	
	/**
	 * Method to send an email (with attachment) - if there are non-zero rows retrieved for the query.
	 * @param tOutput
	 * @param queryName
	 * @throws OException
	 */
	private void sendEmail(Table tOutput, String queryName) throws OException {
		Logging.info("Attempting to send email (using configured Mail Service)..");
		Table envInfo = Util.NULL_TABLE;

		try {
			String recipients = this.repository.getStringValue("email_recipients");
			if (recipients == null || "".equals(recipients)) {
				throw new OException(String.format("email_recipients property not configured in USER_const_repository for Context-%s, SubContext-%s", CONTEXT, SUBCONTEXT));
			}
			
			recipients = com.matthey.utilities.Utils.convertUserNamesToEmailList(recipients);
			EmailMessage mymessage = EmailMessage.create();
			mymessage.addSubject(String.format("\"%s\" query returned non-zero rows - Require SUPPORT team attention", queryName));
			mymessage.addRecipients(recipients);

			StringBuilder builder = new StringBuilder();
			envInfo = com.olf.openjvs.Ref.getInfo();
			if (envInfo != null) {
				builder.append("This information has been generated from database: " + envInfo.getString("database", 1));
				builder.append(", on server: " + envInfo.getString("server", 1));
				builder.append("\n\n");
			}

			builder.append("Endur trading date: " + OCalendar.formatDateInt(Util.getTradingDate()));
			builder.append(", business date: " + OCalendar.formatDateInt(Util.getBusinessDate()));
			builder.append("\n\n");

			mymessage.addBodyText(builder.toString(), EMAIL_MESSAGE_TYPE.EMAIL_MESSAGE_TYPE_PLAIN_TEXT);

			String strFilename;
			StringBuilder fileName = new StringBuilder();
			String[] serverDateTime = ODateTime.getServerCurrentDateTime().toString().split(" ");
			String currentTime = serverDateTime[1].replaceAll(":", "-") + "-" + serverDateTime[2];

			fileName.append(Util.reportGetDirForToday()).append("\\");
			fileName.append(OCalendar.formatDateInt(OCalendar.today()));
			fileName.append("_");
			fileName.append(currentTime);
			fileName.append(".csv");

			strFilename = fileName.toString();
			tOutput.printTableDumpToFile(strFilename);

			if (new File(strFilename).exists()) {
				Logging.info("File attachment found: " + strFilename + ", attempting to attach to email..");
				mymessage.addAttachments(strFilename, 0, null);
			} else {
				Logging.info("File attachment not found: " + strFilename);
			}

			mymessage.send("Mail");
			mymessage.dispose();

			Logging.info(String.format("Email successfully sent to %s", recipients));
			
		} catch (Exception e) {
			Logging.error(String.format("Unable to send output email. Error - %s", e.getMessage()));
			
		} finally {
			if (Table.isTableValid(envInfo) == 1) {
				envInfo.destroy();
			}
		}
	}

}
