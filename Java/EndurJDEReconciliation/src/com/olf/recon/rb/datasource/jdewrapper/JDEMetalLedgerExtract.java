package com.olf.recon.rb.datasource.jdewrapper;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.recon.enums.JdeDealExtractField;
import com.olf.recon.enums.JdeMetalLedgerExtractField;
import com.olf.recon.exception.ReconciliationRuntimeException;
import com.olf.recon.rb.datasource.ReportEngine;
import com.olf.recon.utils.Constants;
import com.olf.recon.utils.JDEConnection;
import com.openlink.util.logging.PluginLog;


/**
 * Executes an external IBM db stored proc to fetch metal ledger specific data from JDE
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class JDEMetalLedgerExtract extends ReportEngine {

	@Override
	protected void setOutputFormat(Table output) throws OException {
		output.addCol("jde_deal_num", COL_TYPE_ENUM.COL_INT);
		output.addCol("jde_ins_type", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_tran_status", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_internal_lentity_desc", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_external_bunit_desc", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_trade_date", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_value_date", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_trade_date_julian", COL_TYPE_ENUM.COL_INT);
		output.addCol("jde_value_date_julian", COL_TYPE_ENUM.COL_INT);
		output.addCol("jde_metal", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_position_toz", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("reconciliation_note", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_ml_batch_number", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_ml_transaction_number", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_reconciliation_source", COL_TYPE_ENUM.COL_STRING);
		//output.addCol("jde_location", COL_TYPE_ENUM.COL_STRING);

	}

	@Override
	protected Table generateOutput(Table output) throws OException {
		// TODO Auto-generated method stub
		PluginLog.info("window_start_date: " + windowStartDateStr + ", window_end_date: " + windowEndDateStr);
		
		String serverName = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_SERVER_NAME);
		String databaseName = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_DATABASE_NAME);
		String userName = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_USERNAME);
		String password = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_PASSWORD);
		String storedProcNameDeals = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_STORED_PROC_NAME_METAL_LEDGER);
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
			PluginLog.info("Executing stored proc: " + storedProcNameDeals);
			jdeConnection.connect();
			callableStatement = jdeConnection.prepareCall(storedProcNameDeals, jdeStartDate, jdeEndDate);
			if (timeoutInt > 0) callableStatement.setQueryTimeout(timeoutInt);
			resultSet = callableStatement.executeQuery();
			PluginLog.info("Returned from stored proc: " + storedProcNameDeals);
			
			while (resultSet.next()) 
			{
				String reconciliationSource = resultSet.getString(JdeMetalLedgerExtractField.RECONCILIATION_SOURCE.toString());
				reconciliationSource = (reconciliationSource != null) ? reconciliationSource.trim() : reconciliationSource;
				
				if ("journal".equalsIgnoreCase(reconciliationSource))
				{
					/* Not interested in Journal entries for purpose of reconciliation, these are custom postings in JDE which will not be in Endur */
					continue;
				}
				
				int dealNum = resultSet.getInt(JdeMetalLedgerExtractField.DEAL_NUM.toString());
				String ins_type = resultSet.getString(JdeMetalLedgerExtractField.INSTRUMENT_TYPE.toString());
				String tran_staus = resultSet.getString(JdeMetalLedgerExtractField.TRAN_STATUS.toString());
				String internalLentityDesc = resultSet.getString(JdeMetalLedgerExtractField.INTERNAL_LENTITY_DESC.toString()).trim();				
				String externalBunitDesc = resultSet.getString(JdeMetalLedgerExtractField.EXTERNAL_BUNIT_DESC.toString()).trim();
				int valueDate = resultSet.getInt(JdeMetalLedgerExtractField.VALUE_DATE.toString()); 
				int tradeDate = resultSet.getInt(JdeMetalLedgerExtractField.TRADE_DATE.toString()); 
				String metal = resultSet.getString(JdeMetalLedgerExtractField.METAL_CURRENCY.toString());
				BigDecimal position = resultSet.getBigDecimal(JdeMetalLedgerExtractField.POSITION_TOZ.toString());
				String mlBatchNumber = resultSet.getString(JdeMetalLedgerExtractField.ML_BATCH_NUMBER.toString());
				String mlTransactionNumber = resultSet.getString(JdeMetalLedgerExtractField.ML_TRANSACTION_NUMBER.toString());
				//String location = resultSet.getString(JdeMetalLedgerExtractField.LOCATION.toString());
				
				
				/* Date formatting for ease of rec */
				String valueDateStr = String.valueOf(valueDate);
				String tradeDateStr = String.valueOf(tradeDate);
				String tradeDateInEndurFormat = getDateJdeToEndurFormat(tradeDateStr);					
				String valueDateInEndurFormat = getDateJdeToEndurFormat(valueDateStr);
				int tradeDateInEndurFormatJulian = OCalendar.parseString(tradeDateInEndurFormat);
				int valueDateInEndurFormatJulian = OCalendar.parseString(valueDateInEndurFormat);

				int newRow = output.addRow();
				output.setInt("jde_deal_num", newRow, dealNum);	
				output.setString("jde_ins_type", newRow, ins_type);	
				output.setString("jde_tran_status", newRow, tran_staus);	
				output.setString("jde_internal_lentity_desc", newRow, internalLentityDesc);
				output.setString("jde_external_bunit_desc", newRow, externalBunitDesc);
				output.setString("jde_trade_date", newRow, tradeDateInEndurFormat);
				output.setString("jde_value_date", newRow, valueDateInEndurFormat);
				output.setInt("jde_trade_date_julian", newRow, tradeDateInEndurFormatJulian);
				output.setInt("jde_value_date_julian", newRow, valueDateInEndurFormatJulian);
				output.setString("jde_metal", newRow, metal);
				output.setDouble("jde_position_toz", newRow, position.doubleValue());
				output.setString("jde_ml_batch_number", newRow, mlBatchNumber);
				output.setString("jde_ml_transaction_number", newRow, mlTransactionNumber);
				output.setString("jde_reconciliation_source", newRow, reconciliationSource);
				//output.setString("jde_location", newRow, location);
				
			}
			
			/* Add reconciliation notes */
			addReconciliationNotes(output, Constants.USER_JM_METAL_LEDGER_REC_NOTES, "deal_num", "jde_deal_num");
			
		} 
		catch (Exception e) 
		{
			throw new ReconciliationRuntimeException("Error whilst parsing result set!", e);
		}
		finally
		{
			try 
			{
				if (resultSet != null) resultSet.close();
				if (callableStatement != null) callableStatement.close();
							
				jdeConnection.disconnect();

				if (tblSuppData != null)
				{
					tblSuppData.destroy();
				}
			} 
			catch (SQLException e) 
			{
				PluginLog.error(e.getMessage());
			}
		}
	
		return output;
	}

	@Override
	protected void registerConversions(Table output) throws OException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void formatOutputData(Table output) throws OException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void groupOutputData(Table output) throws OException {
		// TODO Auto-generated method stub
		output.group("jde_deal_num");
		output.groupBy();
	}
	
}
