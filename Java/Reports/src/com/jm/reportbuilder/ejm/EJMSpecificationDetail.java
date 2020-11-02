package com.jm.reportbuilder.ejm;

import static com.olf.openjvs.enums.OLF_RETURN_CODE.OLF_RETURN_SUCCEED;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.jm.logging.Logging;

public class EJMSpecificationDetail extends EJMReportDataSource {
	
	private static final String COL_DEAL_TRACKING_NUM = "deal_tracking_num";
	private static final String COL_REFERENCE = "reference";
	private static final String COL_DOC_TYPE = "doc_type";
	private static final String COL_DOC_SOURCE = "doc_source";
	private static final String COL_DOC_NAME = "doc_name";
	private static final String COL_DOC_PATH = "doc_path";

	@Override
	protected void setOutputColumns(Table output) {
		try{
			output.addCol(COL_DEAL_TRACKING_NUM, COL_TYPE_ENUM.COL_INT, COL_DEAL_TRACKING_NUM);
			output.addCol(COL_REFERENCE, COL_TYPE_ENUM.COL_STRING, COL_REFERENCE);
			output.addCol(COL_DOC_TYPE, COL_TYPE_ENUM.COL_STRING, COL_DOC_TYPE);
			output.addCol(COL_DOC_SOURCE, COL_TYPE_ENUM.COL_STRING, COL_DOC_SOURCE);
			output.addCol(COL_DOC_NAME, COL_TYPE_ENUM.COL_STRING, COL_DOC_NAME);
			output.addCol(COL_DOC_PATH, COL_TYPE_ENUM.COL_STRING, COL_DOC_PATH);
			
		} catch (Exception e) {
			Logging.error("Failed to add columns to output. An exception has occurred : " + e.getMessage());
			throw new EJMReportException(e);
		}
	}

	@Override
	protected void generateOutputData(Table output) {
		try {		
			
			String dealTrackingNum = reportParameter.getStringValue("tradeRef");
			String reference = reportParameter.getStringValue("reference"); 
			if (reference.isEmpty()) {
				reference = "Batch Spec";
			}
			
			Logging.info(String.format("Parameters [tradeRef:%s/reference:%s]",dealTrackingNum,reference));
			
			String sqlQuery = " SELECT ddl.deal_tracking_num, do.file_object_reference AS reference, ot.type_name AS doc_type, \n" +
							"		do.file_object_source AS doc_source, do.file_object_name as doc_name, file_object_source + file_object_name AS doc_path \n" +
							"	FROM deal_document_link ddl \n" +
							"		JOIN file_object do ON (do.node_id = ddl.saved_node_id) \n" +
							"		JOIN ab_tran ab ON ab.deal_tracking_num = ddl.deal_tracking_num \n" +
							"		JOIN file_object_type ot ON ot.type_id = do.file_object_type \n" +
							"	WHERE ab.current_flag = 1 \n" +
							"	AND ddl.deal_tracking_num = " + dealTrackingNum + "\n" +
							"	AND do.file_object_reference = '" + reference + "' \n";

			Logging.debug("Executing sql query : " + sqlQuery);
			int retVal  = DBaseTable.execISql(output, sqlQuery);
			
            if (retVal != OLF_RETURN_SUCCEED.toInt()) {
                Logging.error("Failed to execute sql query : " + sqlQuery);
                String error = DBUserTable.dbRetrieveErrorInfo(retVal, "");
                throw new EJMReportException(error);
            }
            
            Logging.info("Number of rows retrieved : " + output.getNumRows());
            
		} catch (Exception e) {
			Logging.error("Failed to generate output data. An exception has occurred : " + e.getMessage());
			throw new EJMReportException(e);
		} 
	}
}
