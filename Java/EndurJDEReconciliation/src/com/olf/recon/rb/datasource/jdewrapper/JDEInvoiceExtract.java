package com.olf.recon.rb.datasource.jdewrapper;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.recon.enums.JdeInvoiceExtractField;
import com.olf.recon.exception.ReconciliationRuntimeException;
import com.olf.recon.rb.datasource.ReportEngine;
import com.olf.recon.utils.Constants;
import com.olf.recon.utils.JDEConnection;
import com.olf.jm.logging.Logging;

/**
 * Executes an external IBM db stored proc to fetch invoice specific data from JDE
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class JDEInvoiceExtract extends ReportEngine
{
	@Override
	protected void setOutputFormat(Table output) throws OException 
	{
		output.addCol("jde_invoice_number", COL_TYPE_ENUM.COL_INT);
		output.addCol("jde_invoice_date", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_invoice_date_julian", COL_TYPE_ENUM.COL_INT);
		output.addCol("jde_value_date", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_value_date_julian", COL_TYPE_ENUM.COL_INT);
		output.addCol("jde_settlement_value_net", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("jde_tax_value_in_tax_ccy", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("jde_settlement_value_gross", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("jde_spot_equiv_value", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("jde_payment_date", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_payment_date_julian", COL_TYPE_ENUM.COL_INT);		
		output.addCol("jde_financial_currency", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_int_nostro_account_number", COL_TYPE_ENUM.COL_INT);
		output.addCol("jde_vostro_account_number", COL_TYPE_ENUM.COL_INT);		
		output.addCol("jde_sl_batch_number", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_sl_transaction_number", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_sl_vat_tran_number", COL_TYPE_ENUM.COL_STRING);
		output.addCol("jde_reconciliation_source", COL_TYPE_ENUM.COL_STRING);
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
		output.addCol("exists_in_endur_database", COL_TYPE_ENUM.COL_INT);
		output.addCol("reconciliation_note", COL_TYPE_ENUM.COL_STRING);
	}

	@Override
	protected Table generateOutput(Table output) throws OException
	{
		Logging.info("window_start_date: " + windowStartDateStr + ", window_end_date: " + windowEndDateStr);
		
		String serverName = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_SERVER_NAME);
		String databaseName = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_DATABASE_NAME);
		String userName = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_USERNAME);
		String password = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_PASSWORD);
		String storedProcNameInvoices = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_STORED_PROC_NAME_INVOICES);
		String timeout = constRepoConfig.getValue(Constants.CONST_REPO_VARIABLE_STORED_PROC_QUERY_TIMEOUT);
		int timeoutInt = (timeout != null && !"".equalsIgnoreCase(timeout)) ? Integer.parseInt(timeout) : 0;
		
		JDEConnection jdeConnection = new JDEConnection(serverName, databaseName, userName, password);
		
		CallableStatement callableStatement = null;
		ResultSet resultSet = null;
		
		try 
		{	
			String jdeStartDate = getDateEndurToJDEFormat(windowStartDateStr);
			String jdeEndDate = getDateEndurToJDEFormat(windowEndDateStr);
			
			/* Executes a stored proc in JDE */
			Logging.info("Executing stored proc: " + storedProcNameInvoices);
			jdeConnection.connect();
			callableStatement = jdeConnection.prepareCall(storedProcNameInvoices, jdeStartDate, jdeEndDate);
			if (timeoutInt > 0) callableStatement.setQueryTimeout(timeoutInt);
			resultSet = callableStatement.executeQuery();
			Logging.info("Returned from stored proc: " + storedProcNameInvoices);
			
			while (resultSet.next()) 
			{	
				String reconciliationSource = resultSet.getString(JdeInvoiceExtractField.RECONCILIATION_SOURCE.toString());
				reconciliationSource = (reconciliationSource != null) ? reconciliationSource.trim() : reconciliationSource;
				
				if ("journal".equalsIgnoreCase(reconciliationSource.trim()))
				{
					continue;
				}
				
				int invoiceNumber = resultSet.getInt(JdeInvoiceExtractField.INVOICE_NUMBER.toString());
				int invoiceDate = resultSet.getInt(JdeInvoiceExtractField.INVOICE_DATE.toString()); 
				int valueDate = resultSet.getInt(JdeInvoiceExtractField.VALUE_DATE.toString()); 
				BigDecimal settlementValueNet = resultSet.getBigDecimal(JdeInvoiceExtractField.SETTLEMENT_VALUE_NET.toString());
				BigDecimal taxValueInTaxCcy = resultSet.getBigDecimal(JdeInvoiceExtractField.TAX_VALUE_IN_TAX_CCY.toString());
				BigDecimal settlementValueGross = resultSet.getBigDecimal(JdeInvoiceExtractField.SETTLEMENT_VALUE_GROSS.toString());
				BigDecimal spotEquivValue = resultSet.getBigDecimal(JdeInvoiceExtractField.SPOT_EQUIVALENT_VALUE.toString());
				int paymentDate = resultSet.getInt(JdeInvoiceExtractField.PAYMENT_DATE.toString()); 
				String financialCurrency = resultSet.getString(JdeInvoiceExtractField.FINANCIAL_CURRENCY.toString());
				int internalAccountNumber = resultSet.getInt(JdeInvoiceExtractField.INT_ACCOUNT_NUM.toString());
				int externalAccountNumber = resultSet.getInt(JdeInvoiceExtractField.EXT_ACCOUNT_NUM.toString());				
				String slBatchNumber = resultSet.getString(JdeInvoiceExtractField.SL_BATCH_NUMBER.toString());
				String slTransactionNumber = resultSet.getString(JdeInvoiceExtractField.SL_TRANSACTION_NUMBER.toString());
				String slVatTranNumber = resultSet.getString(JdeInvoiceExtractField.SL_VAT_TRAN_NUMBER.toString());
				String internalNostroAccountName = resultSet.getString(JdeInvoiceExtractField.INTERNAL_NOSTRO_ACCOUNT_NAME.toString()).trim();
				String internalBunitCode = resultSet.getString(JdeInvoiceExtractField.INTERNAL_BUNIT_CODE.toString()).trim();
				String internalBunitDesc = resultSet.getString(JdeInvoiceExtractField.INTERNAL_BUNIT_DESC.toString()).trim();
				String internalLentityCode = resultSet.getString(JdeInvoiceExtractField.INTERNAL_LENTITY_CODE.toString()).trim();
				String internalLentityDesc = resultSet.getString(JdeInvoiceExtractField.INTERNAL_LENTITY_DESC.toString()).trim();				
				String externalVostroAccountName = resultSet.getString(JdeInvoiceExtractField.EXTERNAL_VOSTRO_ACCOUNT_NAME.toString()).trim();
				String externalBunitCode = resultSet.getString(JdeInvoiceExtractField.EXTERNAL_BUNIT_CODE.toString()).trim();
				String externalBunitDesc = resultSet.getString(JdeInvoiceExtractField.EXTERNAL_BUNIT_DESC.toString()).trim();
				String externalLentityCode = resultSet.getString(JdeInvoiceExtractField.EXTERNAL_LENTITY_CODE.toString()).trim();
				String externalLentityDesc = resultSet.getString(JdeInvoiceExtractField.EXTERNAL_LENTITY_DESC.toString()).trim();

				/* Date formatting for ease of rec */
				String invoiceDateStr = String.valueOf(invoiceDate);
				String paymentDateStr = String.valueOf(paymentDate);
				String valueDateStr = String.valueOf(valueDate);
				String invoiceDateInEndurFormat = getDateJdeToEndurFormat(invoiceDateStr);					
				String paymentDateInEndurFormat = getDateJdeToEndurFormat(paymentDateStr);
				String valueDateInEndurFormat = getDateJdeToEndurFormat(valueDateStr);
				int invoiceDateInEndurFormatJulian = OCalendar.parseString(invoiceDateInEndurFormat);
				int valueDateInEndurFormatJulian = OCalendar.parseString(valueDateInEndurFormat);
				int paymentDateInEndurFormatJulian = OCalendar.parseString(paymentDateInEndurFormat);

				int newRow = output.addRow();
				output.setInt("jde_invoice_number", newRow, invoiceNumber);
				output.setString("jde_invoice_date", newRow, invoiceDateInEndurFormat);
				output.setInt("jde_invoice_date_julian", newRow, invoiceDateInEndurFormatJulian);
				output.setString("jde_value_date", newRow, valueDateInEndurFormat);
				output.setInt("jde_value_date_julian", newRow, valueDateInEndurFormatJulian);
				output.setDouble("jde_settlement_value_net", newRow, settlementValueNet.doubleValue());
				output.setDouble("jde_tax_value_in_tax_ccy", newRow, taxValueInTaxCcy.doubleValue());
				output.setDouble("jde_settlement_value_gross", newRow, settlementValueGross.doubleValue());
				output.setDouble("jde_spot_equiv_value", newRow, spotEquivValue.doubleValue());				
				output.setString("jde_payment_date", newRow, paymentDateInEndurFormat);
				output.setInt("jde_payment_date_julian", newRow, paymentDateInEndurFormatJulian);
				output.setString("jde_financial_currency", newRow, financialCurrency);
				output.setInt("jde_int_nostro_account_number", newRow, internalAccountNumber);
				output.setInt("jde_vostro_account_number", newRow, externalAccountNumber);
				output.setString("jde_sl_batch_number", newRow, slBatchNumber);
				output.setString("jde_sl_transaction_number", newRow, slTransactionNumber);
				output.setString("jde_sl_vat_tran_number", newRow, slVatTranNumber);
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
			} 
			catch (SQLException e) 
			{
				throw new ReconciliationRuntimeException(e.getMessage());
			}
		}
		
		checkIfDocumentsExistinEndur(output);
		
		/* Add invoice reconciliation notes */
		addReconciliationNotes(output, Constants.USER_JM_INVOICE_REC_NOTES, "invoice_number", "jde_invoice_number");
		
		return output;
	}

	/**
	 * Add a flag to the output table to indicate if the JM Doc Num actually exists in Endur's stldoc_info tables. 
	 * 
	 * This is solely for diagnostics. The need for this arised when JDE was outputing rows from PRD that didn't exist in the
	 * Endur test environment as the Endur test envrionment was a pre dated PRD refresh. 
	 * 
	 * This is to aid diagnostics
	 * 
	 * @param output
	 * @throws OException
	 */
	private void checkIfDocumentsExistinEndur(Table output) throws OException
	{
		Table tblTemp = null;
		
		try
		{
			output.setColValInt("exists_in_endur_database", 0);
			
			tblTemp = Table.tableNew();
			
			String sqlQuery = 
				"SELECT \n" + 
				"DISTINCT \n" +
				"CAST(si.value AS BIGINT) AS jde_invoice_number \n" +
				"FROM \n" +
				"stldoc_info si \n" +
				" JOIN  stldoc_header_hist shh ON (si.document_num = shh.document_num AND si.stldoc_hdr_hist_id = shh.stldoc_hdr_hist_id) " +
				" JOIN stldoc_templates st ON (st.stldoc_template_id = shh.stldoc_template_id AND st.stldoc_template_name NOT LIKE ('%CN%')) " + 
				"WHERE si.type_id IN (20003, 20005, 20006, 20007) AND si.value NOT LIKE '%-%' \n" +
					"UNION \n" +
				"SELECT \n" + 
				"DISTINCT \n" +
				"CAST(sih.value AS BIGINT) AS jde_invoice_number \n" +
				"FROM \n" +
				"stldoc_info_h sih \n" +
				" JOIN  stldoc_header_hist shh ON (sih.document_num = shh.document_num AND sih.stldoc_hdr_hist_id = shh.stldoc_hdr_hist_id) " +
				" JOIN stldoc_templates st ON (st.stldoc_template_id = shh.stldoc_template_id AND st.stldoc_template_name NOT LIKE ('%CN%')) " +
				"WHERE sih.type_id IN (20003, 20005, 20006, 20007) AND sih.value NOT LIKE '%-%' ";

			int ret = DBaseTable.execISql(tblTemp, sqlQuery);

			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				throw new ReconciliationRuntimeException("Unable to run query: " + sqlQuery);
			}

			tblTemp.addCol("exists_in_endur_database", COL_TYPE_ENUM.COL_INT);
			tblTemp.setColValInt("exists_in_endur_database", 1);

			output.select(tblTemp, "exists_in_endur_database", "jde_invoice_number EQ $jde_invoice_number");	
		}
		finally
		{
			if (tblTemp != null)
			{
				tblTemp.destroy();			
			}
		}
	}
	
	@Override
	protected void registerConversions(Table output) throws OException 
	{	
	}

	@Override
	protected void groupOutputData(Table output) throws OException 
	{
		output.group("jde_invoice_number");
		output.groupBy();
	}

	@Override
	protected void formatOutputData(Table output) throws OException 
	{	
	}
}
