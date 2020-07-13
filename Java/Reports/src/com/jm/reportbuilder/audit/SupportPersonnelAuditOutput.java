package com.jm.reportbuilder.audit;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
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
 * 
 */

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_OUTPUT)
public class SupportPersonnelAuditOutput implements IScript {
	ConstRepository constRep;

	public SupportPersonnelAuditOutput() throws OException {
		super();
	}

	ODateTime dt = ODateTime.getServerCurrentDateTime();


	@Override
	/**
	 * execute: Main Gateway into script from the AScript extender class
	 */
	public void execute(IContainerContext context) throws OException {

		//Constants Repository init
		constRep = new ConstRepository(SupportPersonnelAuditConstants.REPO_CONTEXT, SupportPersonnelAuditConstants.REPO_SUB_CONTEXT);
		SupportPersonnelAuditConstants.initPluginLog(constRep); //Plug in Log init

		Table tblAllPersonnelAuditData = Table.tableNew();
		tblAllPersonnelAuditData = getLatestPersnnelAuditData();
		
		
		try  {
			// PluginLog.init("INFO");
			PluginLog.info("Started Report Output Script: " + this.getClass().getName());
			Table argt = context.getArgumentsTable();
			Table dataTable = argt.getTable("output_data", 1);

			dataTable.addCol("updated_column", COL_TYPE_ENUM.COL_INT);
			dataTable.setColValInt("updated_column", 1);
			
			//row_exists, 0 AS update_row\n" +  
			dataTable.select(tblAllPersonnelAuditData, "row_exists", SupportPersonnelAuditConstants.COL_PERSONNEL_ID + " EQ $" + SupportPersonnelAuditConstants.COL_PERSONNEL_ID + 
																	 " AND " + SupportPersonnelAnalysisConstants.COL_PER_PERSONNEL_CURRENT_VERSION + " EQ $" + SupportPersonnelAnalysisConstants.COL_PER_PERSONNEL_CURRENT_VERSION);
			dataTable.deleteWhereValue("row_exists", 1);
			dataTable.delCol("row_exists");
			
			if (dataTable.getNumRows()>0){
				tblAllPersonnelAuditData.select(dataTable, "updated_column", SupportPersonnelAuditConstants.COL_PERSONNEL_ID + " EQ $" + SupportPersonnelAuditConstants.COL_PERSONNEL_ID );
				tblAllPersonnelAuditData.deleteWhereValue("updated_column", 0);
				
				dataTable.delCol("updated_column");
				tblAllPersonnelAuditData.delCol("row_exists");
				tblAllPersonnelAuditData.delCol("update_row");
				tblAllPersonnelAuditData.delCol("updated_column");	
				if (tblAllPersonnelAuditData.getNumRows()>0){
					tblAllPersonnelAuditData.addCol(SupportPersonnelAuditConstants.COL_LATEST_VERSION,COL_TYPE_ENUM.COL_INT);
					tblAllPersonnelAuditData.setColValInt(SupportPersonnelAuditConstants.COL_LATEST_VERSION, 0);
					tblAllPersonnelAuditData.group(SupportPersonnelAuditConstants.COL_PERSONNEL_ID + "," + SupportPersonnelAnalysisConstants.COL_PER_PERSONNEL_CURRENT_VERSION );
					DBUserTable.update(tblAllPersonnelAuditData);
				}
			}
			if (dataTable.getNumRows() > 0) {
				PluginLog.info("Updating the user table Num Rows:" + dataTable.getNumRows());
				updateUserTable(dataTable);
			} else {
				PluginLog.info("Nows to add user table" );
			}
			tblAllPersonnelAuditData.destroy();
			
		} catch (OException e) {
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


	private Table getLatestPersnnelAuditData() throws OException {
		Table tblPersonnelData = Table.tableNew(SupportPersonnelAuditConstants.USER_SUPPORT_PERSONNEL_AUDIT);
		String sqlCommand = "SELECT uspa." + SupportPersonnelAuditConstants.COL_PERSONNEL_ID + ", uspa." + SupportPersonnelAnalysisConstants.COL_PER_PERSONNEL_CURRENT_VERSION + ",1 AS row_exists, 0 AS update_row\n" +  
							" FROM " + SupportPersonnelAuditConstants.USER_SUPPORT_PERSONNEL_AUDIT + " uspa\n" +
							" WHERE uspa." + SupportPersonnelAuditConstants.COL_LATEST_VERSION + " = 1";
		DBaseTable.execISql(tblPersonnelData, sqlCommand);
		return tblPersonnelData;
	}

	
	/**
	 * Updating the user table USER_jm_emir_log
	 * 
	 * @param dataTable
	 * @throws OException
	 */
	private void updateUserTable(Table dataTable) throws OException
	{

		Table mainTable = Table.tableNew();

		

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

		Table output = Table.tableNew(SupportPersonnelAuditConstants.USER_SUPPORT_PERSONNEL_AUDIT );

		DBUserTable.structure(output);
		
		return output;
	}


}
