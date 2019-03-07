package com.jm.reportbuilder.audit;
 

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * @author SonnyR01
 * 
 */

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_OUTPUT)
@com.olf.openjvs.PluginType(com.olf.openjvs.enums.SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class SupportChangeAuditOutput implements IScript
{
	ConstRepository constRep;

	public SupportChangeAuditOutput() throws OException {
		super();
	}

	ODateTime dt = ODateTime.getServerCurrentDateTime();


	@Override
	/**
	 * execute: Main Gateway into script from the AScript extender class
	 */
	public void execute(IContainerContext context) throws OException {

		//Constants Repository init
		constRep = new ConstRepository(SupportChangeAuditConstants.REPO_CONTEXT, SupportChangeAuditConstants.REPO_SUB_CONTEXT);
		SupportChangeAuditConstants.initPluginLog(constRep); //Plug in Log init

		
		try {
			// PluginLog.init("INFO");
			PluginLog.info("Started Report Output Script: " + this.getClass().getName());
			Table argt = context.getArgumentsTable();
			Table dataTable = argt.getTable("output_data", 1);



			//Table paramTable = argt.getTable("output_parameters", 1);


			if (dataTable.getNumRows() > 0) {
				PluginLog.info("Updating the user table Num Rows:" + dataTable.getNumRows());
				updateUserTable(dataTable);
			} else {
				PluginLog.info("Nows to add user table" );
			}

			updateLastModifiedDate(dataTable);

		} catch (OException e)		{
			PluginLog.error(e.getStackTrace() + ":" + e.getMessage());
			throw new OException(e.getMessage());
		} catch (Exception e) {
			String errMsg = "Failed to initialize logging module.";
			// Util.printStackTrace(e);
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}
		PluginLog.debug("Ended Report Output Script: " + this.getClass().getName());
	}



	/**
	 * Updating the user table USER_support_change_audit
	 * 
	 * @param dataTable
	 * @throws OException
	 */
	private void updateUserTable(Table dataTable) throws OException {

		Table mainTable = Table.tableNew();

		String strWhat;

		int retVal = 0;

		try {

			mainTable = createTableStructure();

			PluginLog.info("Updating the user table");
			if (dataTable.getNumRows() > 0) {
				mainTable.select(dataTable, "*", "id_number GT 0");
				
				int retval = DBUserTable.bcpInTempDb(mainTable);
	            if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
	            	PluginLog.error(DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.insert() failed"));
				}
	            //mainTable.destroy();
			}
		} catch (OException e) {
			mainTable.setColValString("error_desc", DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.insert() failed"));
			mainTable.setColValDateTime("last_update", dt);
			PluginLog.error("Couldn't update the table " + e.getMessage());
		} finally {
			if (Table.isTableValid(mainTable) == 1) {
				mainTable.destroy();
			}
		}
	}

	/**
	 * Creating the output table
	 * 
	 * @return
	 * @throws OException
	 */
	private Table createTableStructure() throws OException {

		Table output = Table.tableNew(SupportChangeAuditConstants.USER_SUPPORT_CHANGE_AUDIT);

		DBUserTable.structure(output);
		
		return output;
	}

	/**
	 * setting the modified time in the constant repository
	 * 
	 * @param currentTime
	 * @throws OException
	 */
	private void updateLastModifiedDate(Table dataTable) throws OException {

		PluginLog.info("Updating the constant repository with the latest time stamp");

		Table updateTime = Util.NULL_TABLE;
		int retVal = 0;
 		try {
 			updateTime =  Table.tableNew();
            int numRows = dataTable.getNumRows();
			updateTime.addCol("context", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("sub_context", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("name", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("string_value", COL_TYPE_ENUM.COL_STRING);
			updateTime.addCol("int_value", COL_TYPE_ENUM.COL_INT);
			updateTime.addCol("date_value", COL_TYPE_ENUM.COL_DATE_TIME);

			updateTime.addRow();

			updateTime.setColValString("context", SupportChangeAuditConstants.REPO_CONTEXT);
			updateTime.setColValString("sub_context", SupportChangeAuditConstants.REPO_SUB_CONTEXT);
			updateTime.setColValString("name", "LastRunTime");

			updateTime.setColValDateTime("date_value", dt);
			updateTime.setColValInt("int_value" , numRows );

			updateTime.setTableName("USER_const_repository");

			updateTime.group("context,sub_context,name");

			// Update database table
			retVal = DBUserTable.update(updateTime);
			if (retVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				PluginLog.error(DBUserTable.dbRetrieveErrorInfo(retVal, "DBUserTable.saveUserTable () failed"));
			}


		} catch (OException e) {

			PluginLog.error("Couldn't update the user table with the current time stamp " + e.getMessage());
			throw new OException(e.getMessage());
		} finally {
			if (Table.isTableValid(updateTime) == 1) {
				updateTime.destroy();
			}
		}

	}

}
