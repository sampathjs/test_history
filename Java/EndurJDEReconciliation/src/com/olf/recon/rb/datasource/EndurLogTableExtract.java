package com.olf.recon.rb.datasource;

import java.util.ArrayList;
import java.util.List;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;

/*
 * History:
 * 2019-11-18 	V1.0	jwaechter		- Initial Version
 */

public class EndurLogTableExtract implements IScript {

	private static final String USER_JM_JDE_INTERFACE_RUN_LOG = "USER_jm_jde_interface_run_log";

	@Override
	public void execute(IContainerContext context) throws OException {
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();
		
		int mode = argt.getInt("ModeFlag", 1);
		
		/* Meta data collection */
		if (mode == 0) 
		{
			setOutputFormat(returnt);
			
			return;
		}
		Table params = null;
		if (argt.getNumRows() > 0)
		{        	        	        	
			params = argt.getTable("PluginParameters", 1);
		} else {
			params = Table.tableNew();
			params.addCol("dummy", COL_TYPE_ENUM.COL_STRING);
		}
		runSql (returnt, params);
	}

	private void runSql(Table returnt, Table params) throws OException {
		List<String> availableParams = getAvailableParams (params);
		StringBuilder sql = new StringBuilder();
		sql.append("\nSELECT * ");
		sql.append("\nFROM ").append(USER_JM_JDE_INTERFACE_RUN_LOG);
		if (availableParams.size() > 0) {
			sql.append("\nWHERE ");
			boolean first = true;
			for (String param : availableParams) {
				String value = getStringParam (params, param);
				sql.append("\n	");
				if (!first) {
					sql.append(" AND ");
				} else {
					first=false;
					sql.append("    ");					
				}
				sql.append(param).append(" ");
				sql.append(value);
			}
		}
		try {
			int ret = DBaseTable.execISql(returnt, sql.toString());
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
				throw new RuntimeException ("Error executing SQL: " + sql.toString());
			}			
		} catch (Exception ex) {
			// ensure SQL is part of exception message
			throw new RuntimeException ("Error executing SQL: " + sql.toString());			
		}
	}

	private void setOutputFormat(Table output) throws OException {
		output.setTableName(USER_JM_JDE_INTERFACE_RUN_LOG);
		int ret = DBUserTable.structure(output);
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
			throw new RuntimeException ("Error retrieving structure of table " + USER_JM_JDE_INTERFACE_RUN_LOG);
		}
	}
	
	/**
	 * Find the param row  
	 * 
	 * @param customParam
	 * @return
	 * @throws OException
	 */
	private int getRow(Table params, String customParam) throws OException
	{
		int findRow = -1;
		
		findRow = params.unsortedFindString(1, customParam, SEARCH_CASE_ENUM.CASE_INSENSITIVE);        
		
		return findRow;
	}
	
	public String getStringParam(Table params, String paramName) throws OException
	{
		String paramValue = "";
		
		int findRow = getRow(params, paramName);
		if (findRow > 0)
		{
			paramValue = params.getString(2, findRow);
		}
		
		return paramValue;
	}
	
	public List<String> getAvailableParams(Table params) throws OException
	{
		List<String> availableParams = new ArrayList<>(params.getNumRows());
		Table runLogTable = Table.tableNew(USER_JM_JDE_INTERFACE_RUN_LOG);
		DBUserTable.structure(runLogTable);	
		for (int row=params.getNumRows(); row >= 1; row --) {
			String param = params.getString(1, row);
			if (runLogTable.getColNum(param) > 0) {
				availableParams.add(param);				
			}
		}
		runLogTable.destroy();
		
		return availableParams;
	}

}
