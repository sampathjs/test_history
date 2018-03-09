package com.customer.migr_refdata;

import com.olf.openjvs.Ask;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.openlink.esp.migration.persistence.Statics;
import com.openlink.esp.migration.persistence.Util;
import com.openlink.esp.migration.persistence.log.MigrLog;
import com.openlink.esp.migration.persistence.log.enums.MigrError;


public class Migr_RefData_Contacts_ParamPlugin implements IScript {

	
	 String COL_DEFINITION_ID = "Definition Id";
	 String COL_DEFINITION_NAME = "Definition Name";
	 String COL_TYPE = "Type";
	 String COL_STATUS = "Status";
	 String COL_TABLE_NAME = "Table";

	int selectId = 0;
	Table options = null;
	Table ask = null;

	
		
	@Override
	public void execute(IContainerContext context) throws OException {
		// TODO Auto-generated method stub

		Table argt = context.getArgumentsTable(); // do not destroy

		try {
			retrieveOptions();
			//getFileDefinitions();
			retrieveSelectionFromUser();
			addSelectionToArgt(argt);
			destroy();
		} catch (OException e) {
			MigrLog.error("MGR-PRM-001", MigrError.MGR_PRM_001, e.getMessage());
		}
	}
	protected void retrieveOptions() throws OException {

		Table definitions = getFileDefinitions();

		options = definitions.copyTable();
		options.setColName(1, COL_DEFINITION_ID);
		options.setColName(2, COL_DEFINITION_NAME);
		options.setColName(3, COL_TYPE);
		options.setColName(4, COL_STATUS);
		options.setColName(5, COL_TABLE_NAME);
		
		definitions.destroy();
	}
	
				protected Table getFileDefinitions() throws OException {
					int ret;
					String sql = null;
					Table options = Table.tableNew("fileDefinitions");
					sql = getSql();
					try{
						ret = DBaseTable.execISql(options, sql);
						if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
							throw new OException(DBUserTable.dbRetrieveErrorInfo(ret, "error exec sql"));
						}
					} catch (OException e) {
						options.destroy();
						throw e;
					}
					if (options == null || options.getNumRows() <= 0) {
						MigrLog.error("MGR-PRM-003", MigrError.MGR_PRM_003);
					}
					return options;
				}

				private String getSql() {
					String sql = null;
					sql = "select a.type_id as 'Definition Id', a.type_name  as 'Definition Name', b.data_type as 'Type', b.current_status  as 'Status', b.data_load_table as 'Table' " +
						"from  USER_csv_channel_type a, USER_migr_op_data_load b where a.type_id=b.type_id AND b.data_type='CONTACT_DATA' ORDER BY b.type_id";
					return sql;
				}

				private void retrieveSelectionFromUser() throws OException {
					int returnStatus = 0;
					Table returnValues = null; // do not destroy

					ask = Table.tableNew();

					options.sortCol(1);
					Ask.setAvsTable(ask, options, "Data Definition", 1, 1, 1);
					returnStatus = Ask.viewTable(ask, "Select Data For Processing.", "Select One Type:");

					if (returnStatus == 1) {
						returnValues = ask.getTable("return_value", 1);
						selectId = returnValues.getInt("return_value", 1);
					}
				}

				private void addSelectionToArgt(Table argt) throws OException {
					//Util.addColumn(argt, Statics.ASK_COL_SELECT_ID, COL_TYPE_ENUM.COL_INT);
					Util.addColumn(argt, COL_TABLE_NAME, COL_TYPE_ENUM.COL_STRING);
					if (argt.getNumRows() < 1) {
						argt.addRow();
					}
					int row= options.findInt(COL_DEFINITION_ID, selectId, SEARCH_ENUM.FIRST_IN_GROUP);
					String val= options.getString(COL_TABLE_NAME, row);
					//argt.setInt("type_id", 1, selectId);
					
					argt.setString("Table", 1,val);
					}

				private void destroy() throws OException {
					if (ask != null) {
						ask.destroy();
						ask = null;
					}
					if (options != null) {
						options.destroy();
						ask = null;
					}

				
	}

}
