package com.openlink.jm.bo.docoutput;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.logging.PluginLog;

/*
 * History:
*
* 2020-01-10	V1.0	-	Pramod Garg - Initial Version - To insert the erroneous entry in 
* 										  USER_jm_auto_doc_email_errors table,if failed to make connection to mail server
**/

public class UpdateErrorInUserTable  {

	public static void insertErrorRecord(Table tblProcessData, String dealNumbers, String errorMessage ) throws OException{
        Table errorData = Table.tableNew("USER_jm_auto_doc_email_errors");
       
        int retval = DBUserTable.structure(errorData);
        if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
              errorData.destroy();
              PluginLog.error(DBUserTable.dbRetrieveErrorInfo(retval, "DBUserTable.structure() failed"));
              return;
        }
        errorData.addRow();
       
        int documentNumber = tblProcessData.getInt("document_num", 1);
        errorData.setInt("endur_document_num", 1, documentNumber);
       
        Table userData = tblProcessData.getTable("user_data", 1);
        int row = userData.unsortedFindString("col_name", "olfStlDocInfo_OurDocNum", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
        if(row > 0 && row <= userData.getNumRows()) {
            String jmDocNumvber = userData.getString("col_data", row);
            errorData.setString("jm_document_num", 1, jmDocNumvber);       	
        }

        
        errorData.setString("deals", 1, dealNumbers);
       
        int intBU = tblProcessData.getInt("internal_bunit", 1);
        errorData.setString("int_bu", 1, Ref.getShortName(SHM_USR_TABLES_ENUM.PARTY_TABLE, intBU));
       
        int extBU = tblProcessData.getInt("external_bunit", 1);
        errorData.setString("ext_bu", 1, Ref.getShortName(SHM_USR_TABLES_ENUM.PARTY_TABLE, extBU));
       
        int template = tblProcessData.getInt("stldoc_template_id", 1);
        errorData.setString("template", 1, Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_TEMPLATES_TABLE,template));
        
        
        errorData.setString("error_message", 1, errorMessage);
       
        ODateTime datetime = ODateTime.getServerCurrentDateTime();
        errorData.setDateTime( "run_date", 1, datetime);     
       
        retval = DBUserTable.insert(errorData);
        
        if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
        	errorData.destroy();
        	PluginLog.error(DBUserTable.dbRetrieveErrorInfo(retval, "DBUserTable.insert() failed"));
        	return;
        }

        errorData.destroy();

	}

}
