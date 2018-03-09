package com.olf.recon.rb.datasource.jdewrapper;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Query;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.recon.enums.JdeDealExtractField;
import com.olf.recon.exception.ReconciliationRuntimeException;
import com.olf.recon.rb.datasource.ReportEngine;
import com.olf.recon.utils.Constants;
import com.olf.recon.utils.JDEConnection;
import com.openlink.util.logging.PluginLog;

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class JDEDealExtract extends ReportEngine
{	
	@Override
	protected void setOutputFormat(Table output) throws OException 
	{
		output.addCol("jde_deal_num", COL_TYPE_ENUM.COL_INT);
		output.addCol("jde_ins_type", COL_TYPE_ENUM.COL_INT);
		output.addCol("jde_tran_status", COL_TYPE_ENUM.COL_INT);
		output.addCol("jde_trade_date", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_value_date", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_trade_date_julian", COL_TYPE_ENUM.COL_INT);
		output.addCol("jde_value_date_julian", COL_TYPE_ENUM.COL_INT);
		output.addCol("jde_metal", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_currency", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_position_toz", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("jde_settlement_value", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("jde_spot_equivalent_price", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("jde_spot_equivalent_value", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("jde_gl_batch_number", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_gl_transaction_number", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_reconciliation_source", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_int_nostro_account_number", COL_TYPE_ENUM.COL_INT);
		output.addCol("jde_vostro_account_number", COL_TYPE_ENUM.COL_INT);
		output.addCol("jde_internal_nostro_account_name", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_internal_bunit_code", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_internal_bunit_desc", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_internal_lentity_code", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_internal_lentity_desc", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_external_vostro_account_name", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_external_bunit_code", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_external_bunit_desc", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_external_lentity_code", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_external_lentity_desc", COL_TYPE_ENUM.COL_STRING);
		output.addCol("reconciliation_note", COL_TYPE_ENUM.COL_STRING);
	}

	@Override
	protected Table generateOutput(Table output) throws OException
	{
		PluginLog.info("window_start_date: " + windowStartDateStr + ", window_end_date: " + windowEndDateStr);
		
		String serverName = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_SERVER_NAME);
		String databaseName = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_DATABASE_NAME);
		String userName = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_USERNAME);
		String password = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_PASSWORD);
		String storedProcNameDeals = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_STORED_PROC_NAME_DEALS);
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
				String reconciliationSource = resultSet.getString(JdeDealExtractField.RECONCILIATION_SOURCE.toString());
				reconciliationSource = (reconciliationSource != null) ? reconciliationSource.trim() : reconciliationSource;
				
				if ("journal".equalsIgnoreCase(reconciliationSource.trim()))
				{
					/* Not interested in Journal entries for purpose of reconciliation, these are custom postings in JDE which will not be in Endur */
					continue;
				}
				
				int dealNum = resultSet.getInt(JdeDealExtractField.DEAL_NUM.toString());
				int valueDate = resultSet.getInt(JdeDealExtractField.METAL_SETTLEMENT_OR_TRADE_DATE.toString()); /* metal maturity/settlement date */
				int tradeDate = resultSet.getInt(JdeDealExtractField.TRADE_DATE_OR_FIXING_DATE.toString()); /* FX trade = trade date, Swap = fixing date of float leg */
				String glBatchNumber = resultSet.getString(JdeDealExtractField.GL_BATCH_NUMBER.toString());
				String glTransactionNumber = resultSet.getString(JdeDealExtractField.GL_TRANSACTION_NUMBER.toString());
				String metal = resultSet.getString(JdeDealExtractField.METAL_CURRENCY.toString());
				String currency = resultSet.getString(JdeDealExtractField.FINANCIAL_CURRENCY.toString());
				BigDecimal position = resultSet.getBigDecimal(JdeDealExtractField.POSITION.toString());
				BigDecimal settlementValue = resultSet.getBigDecimal(JdeDealExtractField.SETTLEMENT_VALUE.toString());
				BigDecimal spotEquivalentPrice = resultSet.getBigDecimal(JdeDealExtractField.SPOT_EQUIVALENT_PRICE.toString());
				BigDecimal spotEquivalentValue = resultSet.getBigDecimal(JdeDealExtractField.SPOT_EQUIVALENT_VALUE.toString());
				int internalAccountNumber = resultSet.getInt(JdeDealExtractField.INT_ACCOUNT_NUM.toString());
				int externalAccountNumber = resultSet.getInt(JdeDealExtractField.EXT_ACCOUNT_NUM.toString());
				String internalNostroAccountName = resultSet.getString(JdeDealExtractField.INTERNAL_NOSTRO_ACCOUNT_NAME.toString()).trim();
				String internalBunitCode = resultSet.getString(JdeDealExtractField.INTERNAL_BUNIT_CODE.toString()).trim();
				String internalBunitDesc = resultSet.getString(JdeDealExtractField.INTERNAL_BUNIT_DESC.toString()).trim();
				String internalLentityCode = resultSet.getString(JdeDealExtractField.INTERNAL_LENTITY_CODE.toString()).trim();
				String internalLentityDesc = resultSet.getString(JdeDealExtractField.INTERNAL_LENTITY_DESC.toString()).trim();				
				String externalVostroAccountName = resultSet.getString(JdeDealExtractField.EXTERNAL_VOSTRO_ACCOUNT_NAME.toString()).trim();
				String externalBunitCode = resultSet.getString(JdeDealExtractField.EXTERNAL_BUNIT_CODE.toString()).trim();
				String externalBunitDesc = resultSet.getString(JdeDealExtractField.EXTERNAL_BUNIT_DESC.toString()).trim();
				String externalLentityCode = resultSet.getString(JdeDealExtractField.EXTERNAL_LENTITY_CODE.toString()).trim();
				String externalLentityDesc = resultSet.getString(JdeDealExtractField.EXTERNAL_LENTITY_DESC.toString()).trim();
				
				/* Date formatting for ease of rec */
				String valueDateStr = String.valueOf(valueDate);
				String tradeDateStr = String.valueOf(tradeDate);
				String tradeDateInEndurFormat = getDateJdeToEndurFormat(tradeDateStr);					
				String valueDateInEndurFormat = getDateJdeToEndurFormat(valueDateStr);
				int tradeDateInEndurFormatJulian = OCalendar.parseString(tradeDateInEndurFormat);
				int valueDateInEndurFormatJulian = OCalendar.parseString(valueDateInEndurFormat);

				int newRow = output.addRow();
				output.setInt("jde_deal_num", newRow, dealNum);				
				output.setString("jde_trade_date", newRow, tradeDateInEndurFormat);
				output.setString("jde_value_date", newRow, valueDateInEndurFormat);
				output.setInt("jde_trade_date_julian", newRow, tradeDateInEndurFormatJulian);
				output.setInt("jde_value_date_julian", newRow, valueDateInEndurFormatJulian);
				output.setString("jde_metal", newRow, metal);
				output.setString("jde_currency", newRow, currency);
				output.setDouble("jde_position_toz", newRow, position.doubleValue());
				output.setDouble("jde_settlement_value", newRow, settlementValue.doubleValue());
				output.setDouble("jde_spot_equivalent_price", newRow, spotEquivalentPrice.doubleValue());
				output.setDouble("jde_spot_equivalent_value", newRow, spotEquivalentValue.doubleValue());
				output.setInt("jde_int_nostro_account_number", newRow, internalAccountNumber);
				output.setInt("jde_vostro_account_number", newRow, externalAccountNumber);
				output.setString("jde_gl_batch_number", newRow, glBatchNumber);
				output.setString("jde_gl_transaction_number", newRow, glTransactionNumber);
				output.setString("jde_reconciliation_source", newRow, reconciliationSource);
				output.setString("jde_internal_nostro_account_name", newRow, internalNostroAccountName);
				output.setString("jde_internal_bunit_code", newRow, internalBunitCode);
				output.setString("jde_internal_bunit_desc", newRow, internalBunitDesc);
				output.setString("jde_internal_lentity_code", newRow, internalLentityCode);
				output.setString("jde_internal_lentity_desc", newRow, internalLentityDesc);
				output.setString("jde_external_vostro_account_name", newRow, externalVostroAccountName);
				output.setString("jde_external_bunit_code", newRow, externalBunitCode);
				output.setString("jde_external_bunit_desc", newRow, externalBunitDesc);
				output.setString("jde_external_lentity_code", newRow, externalLentityCode);
				output.setString("jde_external_lentity_desc", newRow, externalLentityDesc);
			}

			if (output.getNumRows() > 0)
			{
				tblSuppData = getSupplementaryData(output, "jde_deal_num");
				
				output.select(tblSuppData, "ins_type(jde_ins_type), tran_status(jde_tran_status)", "deal_num EQ $jde_deal_num");
			}
			
			/* Add reconciliation notes */
			addReconciliationNotes(output, Constants.USER_JM_DEAL_REC_NOTES, "deal_num", "jde_deal_num");
			
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
		regRefConversion(output, "jde_ins_type", SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);
		regRefConversion(output, "jde_tran_status", SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE);
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
	
	/**
	 * Load supplementary info from the database for specific deals
	 * 
	 * @param tblOutput
	 * @throws OException
	 */
	protected Table getSupplementaryData(Table tblOutput, String dealNumColumn) throws OException
	{
		int queryId = 0;
		
		try
		{
			Table tblData = Table.tableNew("Supplementary data for deals");
			
			queryId = Query.tableQueryInsert(tblOutput, dealNumColumn);
			
			if (queryId > 0)
			{	
				String sqlQuery = 
					"SELECT \n" +
						"ab.deal_tracking_num AS deal_num, \n" +
						"ab.ins_type, \n" +
						"ab.tran_status, \n" +
						"ab.internal_lentity, \n" +
						"ab.internal_bunit, \n" +
						"ab.external_lentity, \n" +
						"ab.external_bunit AS counterparty \n" +
					"FROM \n" +
						"query_result qr, \n" +
						"ab_tran ab \n" +
					"WHERE qr.query_result = ab.deal_tracking_num \n " +
					"AND qr.unique_id = " + queryId + " \n" +
					"AND ab.current_flag = 1";
				
				int ret = DBaseTable.execISql(tblData, sqlQuery);
				
				if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				{
					String message = "Unable to load query: " + sqlQuery;
					throw new ReconciliationRuntimeException(message);
				}
				
				return tblData;
			}
			else
			{
				PluginLog.error("Unable to generate query id on table containing: " + tblOutput.getNumRows() + " rows!");
			}
			
			return tblData;	
		}
		finally
		{
			if (queryId > 0)
			{
				Query.clear(queryId);
			}
		}
	}
}
