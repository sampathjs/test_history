package com.olf.recon.rb.datasource.jdewrapper;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.recon.enums.JdeDealExtractFieldAr;
import com.olf.recon.exception.ReconciliationRuntimeException;
import com.olf.recon.rb.datasource.ReportEngine;
import com.olf.recon.utils.Constants;
import com.olf.recon.utils.JDEConnection;
import com.olf.jm.logging.Logging;

/**
 * Executes an external IBM db stored proc to fetch deal specific data from JDE
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class JDEDealExtractAR extends ReportEngine
{	
	@Override
	protected void setOutputFormat(Table output) throws OException 
	{
		output.addCol("id", COL_TYPE_ENUM.COL_INT);
		output.addCol("note", COL_TYPE_ENUM.COL_STRING);
		output.addCol("gl_date", COL_TYPE_ENUM.COL_INT);
		output.addCol("account", COL_TYPE_ENUM.COL_STRING);
		output.addCol("type", COL_TYPE_ENUM.COL_STRING);
		output.addCol("document_num", COL_TYPE_ENUM.COL_INT);
		output.addCol("value_date", COL_TYPE_ENUM.COL_INT);
		output.addCol("qty_toz", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("amount", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("currency", COL_TYPE_ENUM.COL_STRING);
		output.addCol("batch_num", COL_TYPE_ENUM.COL_STRING);
		output.addCol("tran_num", COL_TYPE_ENUM.COL_INT);
	}

	@Override
	protected Table generateOutput(Table output) throws OException
	{
		Logging.info("window_start_date: " + windowStartDateStr + ", window_end_date: " + windowEndDateStr);
		
		String serverName = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_SERVER_NAME);
		String databaseName = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_DATABASE_NAME);
		String userName = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_USERNAME);
		String password = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_PASSWORD);
		String storedProcNameDeals = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_STORED_PROC_NAME_DEALS_AR);
		String timeout = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_STORED_PROC_QUERY_TIMEOUT);
		int timeoutInt = (timeout != null && !"".equalsIgnoreCase(timeout)) ? Integer.parseInt(timeout) : 0;
			
		JDEConnection jdeConnection = new JDEConnection(serverName, databaseName, userName, password);
		
		CallableStatement callableStatement = null;
		ResultSet resultSet = null;
		
		Table tblSuppData = null;
		
		try 
		{	
			String jdeStartDate = getDateEndurToJDEFormat(windowStartDateStr);
			String jdeEndDate = getDateEndurToJDEFormat(windowEndDateStr);
			/* Executes a stored proc in JDE */
			Logging.info("Executing stored proc: " + storedProcNameDeals);
			jdeConnection.connect();
			callableStatement = jdeConnection.prepareCall(storedProcNameDeals, ledgerMode, jdeStartDate, jdeEndDate);
			if (timeoutInt > 0) callableStatement.setQueryTimeout(timeoutInt);
			resultSet = callableStatement.executeQuery();
			Logging.info("Returned from stored proc: " + storedProcNameDeals);
			
			while (resultSet.next()) 
			{				
				String account = resultSet.getString(JdeDealExtractFieldAr.ACCOUNT.toString());
				BigDecimal amount = resultSet.getBigDecimal(JdeDealExtractFieldAr.AMOUNT.toString()); 
				String batchNum = resultSet.getString(JdeDealExtractFieldAr.BATCH_NUM.toString()); 
				String currency = resultSet.getString(JdeDealExtractFieldAr.CURRENCY.toString());
				int documentNum = resultSet.getInt(JdeDealExtractFieldAr.DOC_NUM.toString());
				int glDate = resultSet.getInt(JdeDealExtractFieldAr.GL_DATE.toString());
				int id = resultSet.getInt(JdeDealExtractFieldAr.ID.toString());
				String note = resultSet.getString(JdeDealExtractFieldAr.NOTE.toString());
				BigDecimal qtyToz = resultSet.getBigDecimal(JdeDealExtractFieldAr.QTY_TOZ.toString());
				String type = resultSet.getString(JdeDealExtractFieldAr.TYPE.toString());
				int valueDate = resultSet.getInt(JdeDealExtractFieldAr.VALUE_DATE.toString());
				int tranNum = resultSet.getInt(JdeDealExtractFieldAr.TRAN_NUM.toString());
				
				int newRow = output.addRow();
				output.setInt("id", newRow, id);
				output.setString("note", newRow, note);
				output.setInt("gl_date", newRow, glDate);
				output.setString("account", newRow, account);
				output.setString("type", newRow, type);
				output.setInt("document_num", newRow, documentNum);
				output.setInt("value_date", newRow, valueDate);
				output.setDouble("qty_toz", newRow, qtyToz.doubleValue());
				output.setDouble("amount", newRow, amount.doubleValue());
				output.setString("currency", newRow, currency);
				output.setString("batch_num", newRow, batchNum);
				output.setInt("tran_num", newRow, tranNum);
			}
		} 
		catch (Exception e) 
		{
			throw new ReconciliationRuntimeException("Error whilst parsing result set!", e);
		}
		finally
		{
			try 
			{
				if (callableStatement != null) callableStatement.close();
				if (resultSet != null) resultSet.close();
				
				jdeConnection.disconnect();

				if (tblSuppData != null)
				{
					tblSuppData.destroy();
				}
			} 
			catch (SQLException e) 
			{
				throw new ReconciliationRuntimeException(e.getMessage());
			}
		}
	
		return output;
	}

	@Override
	protected void registerConversions(Table output) throws OException 
	{

	}

	@Override
	protected void groupOutputData(Table output) throws OException 
	{
		output.group("jde_deal_num");
		output.groupBy();
	}

	@Override
	protected void formatOutputData(Table output) throws OException 
	{	
	}
	
}
