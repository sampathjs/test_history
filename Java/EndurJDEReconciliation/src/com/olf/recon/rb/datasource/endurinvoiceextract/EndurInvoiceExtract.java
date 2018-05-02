package com.olf.recon.rb.datasource.endurinvoiceextract;

import java.math.BigDecimal;
import java.util.HashMap;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.DATE_LOCALE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.recon.enums.EndurPartyInfoExternalBunit;
import com.olf.recon.exception.ReconciliationRuntimeException;
import com.olf.recon.rb.datasource.ReportEngine;
import com.olf.recon.utils.Constants;
import com.olf.recon.utils.Util;

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class EndurInvoiceExtract extends ReportEngine
{	
	@Override
	protected void setOutputFormat(Table output) throws OException 
	{
		output.addCol("invoice_number", COL_TYPE_ENUM.COL_INT);
		output.addCol("endur_doc_num", COL_TYPE_ENUM.COL_INT);
		output.addCol("endur_doc_status", COL_TYPE_ENUM.COL_INT);
		output.addCol("invoice_date", COL_TYPE_ENUM.COL_INT);
		output.addCol("invoice_date_str", COL_TYPE_ENUM.COL_STRING);
		output.addCol("value_date", COL_TYPE_ENUM.COL_INT);
		output.addCol("value_date_str", COL_TYPE_ENUM.COL_STRING);
		output.addCol("settlement_value_net", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("tax_amount_in_tax_ccy", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("fx_rate", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("settlement_value_gross", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("payment_date", COL_TYPE_ENUM.COL_INT);
		output.addCol("payment_date_str", COL_TYPE_ENUM.COL_STRING);
		output.addCol("currency", COL_TYPE_ENUM.COL_INT);
		output.addCol("currency_str", COL_TYPE_ENUM.COL_STRING);
		output.addCol("internal_lentity", COL_TYPE_ENUM.COL_INT);
		output.addCol("internal_lentity_str", COL_TYPE_ENUM.COL_STRING);
		output.addCol("external_bunit", COL_TYPE_ENUM.COL_INT);
		output.addCol("external_bunit_str", COL_TYPE_ENUM.COL_STRING);
		output.addCol("reconciliation_note", COL_TYPE_ENUM.COL_STRING);
	}
	
	@Override
	protected Table generateOutput(Table output) throws OException 
	{	
		SentToCounterpartyInvoices sentInvoices = new SentToCounterpartyInvoices(windowStartDate, windowEndDate, output);
		CancelledInvoices cancelledInvoices = new CancelledInvoices(windowStartDate, windowEndDate, output);
		
		Table tblSentInvoices = null;
		Table tblCancelledInvoices = null;
		
		try
		{
			tblSentInvoices = sentInvoices.generateMergedInvoiceData();
			tblSentInvoices.copyRowAddAllByColName(output);
		
			tblCancelledInvoices = cancelledInvoices.generateMergedInvoiceData();
			tblCancelledInvoices.copyRowAddAllByColName(output);
			
			/* Filter out rows which are not needed as per reference data param config in Report Builder */
			filterCounterparties(output, excludedCounterparties);
			filterIncludedLentites(output, includedLentites);
			
			output.mathAddCol("settlement_value_net", "tax_amount_in_tax_ccy", "settlement_value_gross");

			/* If ZAR, then tax is in non-USD so don't add settlement + tax, just display gross = net */
			adjustJmPreciousMetalsLtdAmounts(output);
			
			/* Add reconciliation notes */
			addReconciliationNotes(output, Constants.USER_JM_INVOICE_REC_NOTES, "invoice_number", "invoice_number");
			
			/* Apply rounding */
			round(output);
		}
		catch (Exception e)
		{
			throw new ReconciliationRuntimeException("Exception encountered during generation of invoices: " + e.getMessage(), e);
		}
		finally 
		{
			if (tblSentInvoices != null) 
			{
				tblSentInvoices.destroy();
			}
			
			if (tblCancelledInvoices != null) 
			{
				tblCancelledInvoices.destroy();
			}
			
			sentInvoices.cleanup();
			cancelledInvoices.cleanup();
		}
		
		return output;
	}

	@Override
	protected void registerConversions(Table output) throws OException 
	{
		regRefConversion(output, "endur_doc_status", SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE);
	}

	@Override
	protected void groupOutputData(Table output) throws OException 
	{
		output.clearGroupBy();
		output.group("invoice_number");
		output.groupBy();
	}

	@Override
	protected void formatOutputData(Table output) throws OException 
	{
		output.copyColFormatDate("invoice_date", "invoice_date_str", DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
		output.copyColFormatDate("value_date", "value_date_str", DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
		output.copyColFormatDate("payment_date", "payment_date_str", DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_DEFAULT);
		
		output.copyColFromRef("currency", "currency_str", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		
		/* Use Reg.getShortName as Ref.GetName only returns the first 30 chars - known OLF issue */
		int numRows = output.getNumRows();
		for (int row = 1; row <= numRows; row++)
		{
			int externalBunitId = output.getInt("external_bunit", row);
			int internalLentityId = output.getInt("internal_lentity", row);
			
			output.setString("external_bunit_str", row, Ref.getShortName(SHM_USR_TABLES_ENUM.PARTY_TABLE, externalBunitId));
			output.setString("internal_lentity_str", row, Ref.getShortName(SHM_USR_TABLES_ENUM.PARTY_TABLE, internalLentityId));
		}
	}
	
	/**
	 * Any invoice sent from the JM PRECIOUS METALS LTD group has tax amount in ZAR so if the
	 * reporting currency is USD, we don't want to be adding a USD settlement amount to a ZAR tax amount (which
	 * would result in the wrong gross amount) - for these scenarios, just settlement_value_gross = settlement_value_net 
	 * 
	 * @param output
	 * @throws OException
	 */
	private void adjustJmPreciousMetalsLtdAmounts(Table output) throws OException
	{
		int numRows = output.getNumRows();
		
		int jmPreciousMetalLtd = Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, Constants.JM_PRECIOUS_METALS_LTD);
		int jmPlc = Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, Constants.JM_PLC);
		int ZAR = Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, Constants.CURRENCY_ZAR);

		for (int row = 1; row <= numRows; row++)
		{
			int internalLentity = output.getInt("internal_lentity", row);
			int externalBunit = output.getInt("external_bunit", row);

			int currency = output.getInt("currency", row);
			double settlementValueNet = output.getDouble("settlement_value_net", row);

			if (internalLentity == jmPreciousMetalLtd && currency != ZAR)
			{
				output.setDouble("settlement_value_gross", row, settlementValueNet);
			}
			else if (internalLentity == jmPlc && currency != ZAR)
			{
				int preferredCurrency = getPreferredCurrency(externalBunit);

				if (preferredCurrency == ZAR)
				{
					/* 
					 * SA has been merged into JM PLC, so for any SA trades the preferred currency should
					 * indicate if the tax is paid in ZAR. If so, ensure gross and net amounts match 
					 */
					output.setDouble("settlement_value_gross", row, settlementValueNet);
				}		
			}
		}
	}
	
	/**
	 * Round cash amounts as specified
	 * 
	 * @param output
	 * @throws OException
	 */
	private void round(Table output) throws OException
	{
		int numRows = output.getNumRows();
		
		for (int row = 1; row <= numRows; row++)
		{
			double settlementValueNet = output.getDouble("settlement_value_net", row);
			double settlementValueGross = output.getDouble("settlement_value_gross", row);
			double taxAmountInTaxCurrency = output.getDouble("tax_amount_in_tax_ccy", row);
			
            BigDecimal settlementValueNetBD = Util.roundCashAmount(settlementValueNet);
            BigDecimal settlementValueGrossBD = Util.roundCashAmount(settlementValueGross);
            BigDecimal taxAmountInTaxCurrencyBD = Util.roundCashAmount(taxAmountInTaxCurrency);
            
			double roundedSettlementValueNet = settlementValueNetBD.doubleValue();
			double roundedSettlementValueGross = settlementValueGrossBD.doubleValue();
			double roundedTaxAmount = taxAmountInTaxCurrencyBD.doubleValue();
			
			output.setDouble("settlement_value_net", row, roundedSettlementValueNet);
			output.setDouble("settlement_value_gross", row, roundedSettlementValueGross);
			output.setDouble("tax_amount_in_tax_ccy", row, roundedTaxAmount);
		}
	}
	
	/**
	 * Fetch and store the "Preferred Currency" party info for all external bunits
	 * 
	 * @param externalBunitId
	 * @return
	 * @throws OException
	 */
	private static HashMap<Integer, Integer> preferredCurrencyMap = null;
	private Integer getPreferredCurrency(int externalBunitId) throws OException
	{	
		if (preferredCurrencyMap == null)
		{
			preferredCurrencyMap = new HashMap<Integer, Integer>();
			
			Table tblData = null;
			
			try
			{
				tblData = Table.tableNew();
				
				String sqlQuery = "select party_id, value from party_info where type_id = " + EndurPartyInfoExternalBunit.PREFERRED_CURRENCY.toInt();
				
				int ret = DBaseTable.execISql(tblData, sqlQuery);
				
				if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				{
					throw new ReconciliationRuntimeException("Unable to run query: " + sqlQuery);
				}
				
				int numRows = tblData.getNumRows();
				for (int row = 1; row <= numRows; row++)
				{
					int partyId = tblData.getInt("party_id", row);
					String currency = tblData.getString("value", row);
					int currencyId = Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, currency);
					
					preferredCurrencyMap.put(partyId, currencyId);
				}
			}
			finally
			{
				if (tblData != null)
				{
					tblData.destroy();
				}
			}
		}
		
		if (preferredCurrencyMap.containsKey(externalBunitId))
		{
			return preferredCurrencyMap.get(externalBunitId);	
		}
		else
		{
			return -1;
		}
	}
}	