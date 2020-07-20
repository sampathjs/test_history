package com.jm.accountingfeed.rb.datasource.salesledger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.jm.accountingfeed.enums.EndurCashflowType;
import com.jm.accountingfeed.enums.EndurDocumentInfoField;
import com.jm.accountingfeed.enums.EndurDocumentStatus;
import com.jm.accountingfeed.enums.EndurEventInfoField;
import com.jm.accountingfeed.enums.EndurTaxRate;
import com.jm.accountingfeed.enums.JDEStatus;
import com.jm.accountingfeed.enums.PartyRegion;
import com.jm.accountingfeed.enums.ReportBuilderParameter;
import com.jm.accountingfeed.exception.AccountingFeedRuntimeException;
import com.jm.accountingfeed.rb.datasource.ReportEngine;
import com.jm.accountingfeed.util.Constants;
import com.jm.accountingfeed.util.SimUtil;
import com.jm.accountingfeed.util.Util;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SimResult;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.INS_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.jm.logging.Logging;

/**
 * SL extract class responsible for
 * 
 * 1. Retrieving all latest invoices from the system that are in a Pending state
 * 2. Filter cash payments (by cflow type) and enrich supplementary data for these rows
 * 3. Filter tax payments (by cflow type) and enrich supplementary data for these rows
 * 4. Merge tax data into cash rows (if on the same invoice), otherwise append tax rows as new rows to the output
 * 5. Filter output data for as per region the report is running for (UK or HK or US)
 * 
 * Version		Updated By			Date		Ticket#			Description
 * -----------------------------------------------------------------------------------
 * 	1.1			Paras Yadav		10-Jan-2020		 P1722			Removed check to see if column taxColNum exists in 
 *  								  							tblCashEvents table before accessing it 
 */

public class SalesLedgerExtract extends ReportEngine 
{
	private HashSet<Integer> includedLentities;
	private HashSet<Integer> excludedCounterparties;
	
	/**
	 * queryId of the query_result table containing tran_num of Trades whose Invoices are to be extracted.
	 * This shall remain 0 in production execution because Interfaces never works on specific trades.
	 * This can be populated for Diagnostic purposes, e.g. it is populated by SalesLedgerData UDSR.
	 */
	protected int tranTableQueryId = 0;
	
	@Override
	protected void setOutputFormat(Table output) throws OException 
	{
		output.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		output.addCol("endur_doc_num", COL_TYPE_ENUM.COL_INT);
		output.addCol("invoice_number", COL_TYPE_ENUM.COL_INT);
		output.addCol("sl_status", COL_TYPE_ENUM.COL_STRING);
		output.addCol("last_doc_update_time", COL_TYPE_ENUM.COL_INT);
		output.addCol("stldoc_hdr_hist_id", COL_TYPE_ENUM.COL_INT);
		output.addCol("desk_location", COL_TYPE_ENUM.COL_STRING);
		output.addCol("cflow_type", COL_TYPE_ENUM.COL_INT);
		output.addCol("endur_doc_status", COL_TYPE_ENUM.COL_INT);
		output.addCol("buy_sell", COL_TYPE_ENUM.COL_INT);
		output.addCol("trade_type", COL_TYPE_ENUM.COL_STRING);
		output.addCol("interest_rate_on_lease", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("reference", COL_TYPE_ENUM.COL_STRING);
		output.addCol("ins_type", COL_TYPE_ENUM.COL_INT); 
		output.addCol("internal_portfolio", COL_TYPE_ENUM.COL_INT);	
		output.addCol("internal_bunit", COL_TYPE_ENUM.COL_INT);
		output.addCol("external_bunit", COL_TYPE_ENUM.COL_INT);
		output.addCol("internal_lentity", COL_TYPE_ENUM.COL_INT);
		output.addCol("fixed_float", COL_TYPE_ENUM.COL_INT);
		output.addCol("payment_date", COL_TYPE_ENUM.COL_INT);
		output.addCol("invoice_date", COL_TYPE_ENUM.COL_INT);
		output.addCol("trading_location", COL_TYPE_ENUM.COL_STRING);
		output.addCol("trade_date", COL_TYPE_ENUM.COL_INT);
		output.addCol("from_currency", COL_TYPE_ENUM.COL_INT);
		
		output.addCol("value_date", COL_TYPE_ENUM.COL_INT);
		output.addCol("position_uom", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("position_toz", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("settle_amount", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("deal_exchange_rate", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("uom", COL_TYPE_ENUM.COL_INT);
		output.addCol("unit_price", COL_TYPE_ENUM.COL_DOUBLE);
		
		output.addCol("tax_in_deal_ccy", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("tax_in_tax_ccy", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("tax_code", COL_TYPE_ENUM.COL_STRING);

		output.addCol("spot_equivalent_price", COL_TYPE_ENUM.COL_DOUBLE);
		output.addCol("spot_equivalent_value", COL_TYPE_ENUM.COL_DOUBLE);
		
		output.addCol("is_coverage", COL_TYPE_ENUM.COL_STRING);
		output.addCol("coverage_text", COL_TYPE_ENUM.COL_STRING);
		output.addCol("pricing_type", COL_TYPE_ENUM.COL_STRING);
	}
	
	/**
	 * Add tax and currency columns to the output table if they don't exist
	 * 
	 * @param output
	 * @throws OException
	 */
	protected void setMissingOutputFormat(Table output) throws OException 
	{
	    if (output.getColNum("tax_ccy") <= 0)
	    {
	        output.addCol("tax_ccy", COL_TYPE_ENUM.COL_STRING);
	    }
	    
        if (output.getColNum("tax_exchange_rate") <= 0)
        {
            output.addCol("tax_exchange_rate", COL_TYPE_ENUM.COL_DOUBLE);
        }
        
        if (output.getColNum("to_currency") <= 0)
        {
            output.addCol("to_currency", COL_TYPE_ENUM.COL_INT);
        }
	}
	
	@Override
	protected Table generateOutput(Table output) throws OException 
	{
		Table tblAllInvoices = null;
		Table tblCashEvents = null;
		Table tblPnlMarketData = null;
		Table tblUniqueDeals = null;
		
		includedLentities = getIncludedInternalLentities();
		excludedCounterparties = getExcludedCounterparties();
		String region = (rp != null ? rp.getStringValue(ReportBuilderParameter.REGIONAL_SEGREGATION.toString()) : "");
				
		try
		{
			/* 
			 * Get all latest "Sent" and "Cancelled" invoices from the system that are in a "Pending" state 
			 * 
			 * These are invoices that are yet to be sent to JDE. They can be newly validated invoices, or cancelled invoices.
			 */
			tblAllInvoices = getLatestInvoices(getApplicableJdeStatus());
            Logging.info("Fetched latest invoices. Num = " + tblAllInvoices.getNumRows());
			if (tblAllInvoices.getNumRows() == 0) return output;
			
			/* Filter out rows which are not needed as per the "regional_segregation" parameter */
            Logging.info("Filtering by: IncludedLegalEntities = " + includedLentities + ", ExcludedCounterparties=" + excludedCounterparties);			
			filterRefData(tblAllInvoices);
            Logging.info("Invoices after filtering as per ref data. Num = " + tblAllInvoices.getNumRows());
			
			/*
			 * Filter out documents that are in a "Sent" doc status, but in a "Pending Cancelled" JDE status. Easier to do here 
			 * as the underlying info is only stored once per doc_num. Since SL is an expansion of invoice data (multiple line items per
			 * doc _num, including same day cancellations), just remove any already processed rows
			 */
			filterSentRecords(tblAllInvoices);
            Logging.info("Invoices after filtering Send document in 'Pending Cancelled' JDE status. Num = " + tblAllInvoices.getNumRows());
            Logging.debug("Printing record with non zero invoice number");
            for(int i=1;i<= tblAllInvoices.getNumRows();i++)
			{
				Logging.debug("Processed record with deal number=" + tblAllInvoices.getInt("deal_num", i)
						+ ", document number=" + tblAllInvoices.getInt("endur_doc_num", i));
			}
			
			/* Generate cash events. Cash events are the primary source of data for invoices */
			tblCashEvents = generateCashInvoices(tblAllInvoices);
			
			/* Generate tax events, these are recorded separately in native tables */
			Table tblTaxEvents = generateTaxInvoices(tblAllInvoices);
			
			Logging.debug("Number of records in tblTaxEvents="+tblTaxEvents.getNumRows());
			
			/* Merge tax info into main cash table. An invoice row in SL is thus made up of both cash and tax amounts */
            Logging.info("Merging tax info into main cash table");
			mergeTaxInfo(tblCashEvents, tblTaxEvents);
			
			/**
			 * Set default 'to_currency' as the settlement currency. 
			 * 'to_currency' will be overwritten for non-CommPhy trades as per the Simulation results data later.
			 */
			tblCashEvents.setColName("currency", "to_currency");
			
            setMissingOutputFormat(output);
            if (tblCashEvents.getNumRows() == 0) return output;

            /* Enrich sim data from "JM Tran Data" */
            Logging.info("Enriching sim data");
			enrichSimData(tblCashEvents);
			
			/* Get PNL market data for spot equivalent attributes */
            Logging.info("Enriching Market data");
			enrichMarketData(tblCashEvents);
			
			/* Set default value of tax fields (if tax applicable) */
			setDefaultTaxInfo(tblCashEvents);

            /* Check if we have entries that are not present in user_jm_sl_doc_tracking and flag a warning if not */
            int numRows = tblCashEvents.getNumRows();
			for (int row = 1; row <= numRows; row++)
			{
				String slStatus = tblCashEvents.getString("sl_status", row);				
                int endurDocNum = tblCashEvents.getInt("endur_doc_num", row);                   
				if ("".equalsIgnoreCase(slStatus))
				{
					Logging.warn("document_num: " + endurDocNum + " has no record in user_jm_sl_doc_tracking, needs investigating!");
				}				
			}
			
			/* Adjust signage for position and cash */
			Util.adjustSignageForJDE(tblCashEvents, "endur_doc_status");
			
			/* Adjust advanced priced/defered trades */
			if (PartyRegion.HK.toString().equalsIgnoreCase(region))
			{
				adjustAdvancedPricedTrades(tblCashEvents);	
			}
			
			/* Copy output */
			output.select(tblCashEvents, "*", "endur_doc_num GT 0");
			
			Logging.debug("Number of records in output = " + output.getNumRows());
		}
		catch (Exception e)
		{
		    Logging.error("Exception in Sales Ledger extract: " + e.getMessage());
		    Util.printStackTrace(e);
			throw new AccountingFeedRuntimeException(e.getMessage(), e);
		}
		finally
		{
			if (tblAllInvoices != null)
			{
				tblAllInvoices.destroy();	
			}
			
			if (tblCashEvents != null) 
			{
				tblCashEvents.destroy();
			}
			
			if (tblPnlMarketData != null)
			{
				tblPnlMarketData.destroy();	
			}

			if (tblUniqueDeals != null)
			{
				tblUniqueDeals.destroy();
			}
		}
		
		return output;
	}

	/**
	 * Get all cash events. These are events where the cflow_type != TAX
	 * 
	 * Aggregate cash events at invoice and deal granularity (summing up by cflow_type)
	 * 
	 * We ignore tax events here as these will be merged in later.
	 * 
	 * @param tblAllInvoices
	 * @return
	 * @throws OException
	 */
	private Table generateCashInvoices(Table tblAllInvoices) throws OException
	{
		Table tblCashEvents = tblAllInvoices.copyTable();
		
		try
		{
			if (tblAllInvoices.getNumRows() > 0)
			{
				/* Filter out cash events only, so remove tax rows */
				tblCashEvents.deleteWhereValue("cflow_type", EndurCashflowType.VAT.id());
				tblCashEvents.deleteWhereValue("cflow_type", EndurCashflowType.MANUAL_VAT.id());
			}
			
			Logging.debug("Number of records in tblCashEvents="+tblCashEvents.getNumRows());
			
			/* Set the JM custom doc numbers against each invoice */
			boolean useHistorics = true;
			enrichCustomInvoiceNumber(tblCashEvents, useHistorics);
			/* 
			 * Enrich invoice numbers that are not in historics. This is a very odd case as stldoc info audit should always
			 * be tracked in the _h [historic] tables at all times, so this snippet is to be extra  cautious and check
			 * the current tables just incase a JM doc num is there.
			 */
			useHistorics = false;
			enrichCustomInvoiceNumber(tblCashEvents, useHistorics);
			int nRows = tblCashEvents.getNumRows();
			Logging.debug("Number of records in tblCashEvents="+ nRows );
			for (int row = 1; row <= nRows; row++)
			{
				int invoiceNumber = tblCashEvents.getInt("invoice_number", row);
				int endurDocNum = tblCashEvents.getInt("endur_doc_num", row);
				if (invoiceNumber == 0)
				{
					Logging.warn("Invoice num is 0 for document_num "  + endurDocNum + ", removing from output");
				}
			}
			tblCashEvents.deleteWhereValue("invoice_number", 0);
			
			/* Start building out cash output table, copy structure of output table */
			Table tblAggregatedEvents = returnt.cloneTable();
			tblAggregatedEvents.setTableName("Aggregated cash events");
			
			/* Enrich distinct key columns first */
			String distinctKeys = "deal_num, endur_doc_num, endur_doc_status, invoice_number, sl_status, last_doc_update_time, " +
									"stldoc_hdr_hist_id, buy_sell, ins_type, ins_sub_type, cflow_type, value_date, payment_date, invoice_date, " +
									"currency, tax_code, fx_rate, base_currency";
			
			tblAggregatedEvents.select(tblCashEvents, "DISTINCT, " + distinctKeys, "deal_num GT -1");

			/*
			 * Aggregate settle amounts via the unique keys - requirement is a row per invoice per deal. 
			 * so if an invoice has 4 deals linked to it, the expectation is 4 rows for that invoice in the extract
			 * 
			 * Distinct keys includes the "JM Invoice Number" (i.e, invoice_number field)
			 */
			tblAggregatedEvents.select(tblCashEvents, "SUM, settle_amount", getTableWhereClause(distinctKeys));
			
			/* Change the core columns names to output col names for onward processing */
			tblAggregatedEvents.setColName("fx_rate", "tax_exchange_rate");
			tblAggregatedEvents.setColName("base_currency", "tax_ccy");
			
			/* 
			 * Where there is an empty tax code, null out the tax_exchange_rate and tax_ccy as these are not applicable.
			 * 
			 * These fields are tax applicable, but still seem to be filled in for cash events 
			 * as per an Openlink op service which stamps event info fields.
			 */
			int numRows = tblAggregatedEvents.getNumRows();
			Logging.debug("Number of records in tblAggregatedEvents="+numRows);			
			for (int row = 1; row <= numRows; row++)
			{
				String taxCode = tblAggregatedEvents.getString("tax_code", row);
				
				/* If the tax code is null, or empty - null out tax_ccy and tax_exchange_rate */
				if (taxCode == null || "".equalsIgnoreCase(taxCode))
				{
					tblAggregatedEvents.setString("tax_ccy", row, "");
					tblAggregatedEvents.setDouble("tax_exchange_rate", row, 0.0);	
				}
			}
			
			return tblAggregatedEvents;
		}
		finally
		{
			if (tblCashEvents != null)
			{
				tblCashEvents.destroy();
			}
		}
	}
	
	/**
	 * Get all tax events, and enrich data accordingly.
	 * 
	 * @param tblAllInvoices
	 * @return
	 * @throws OException
	 */
	private Table generateTaxInvoices(Table tblAllInvoices) throws OException
	{
		Table tblZeroData = null;
		
		try
		{
			Table tblTaxInvoices = Table.tableNew("Tax invoices");
			
			/* Filter out "VAT" and "Manual VAT" events only */
			tblTaxInvoices.select(tblAllInvoices, "*", "cflow_type EQ " + EndurCashflowType.VAT.id());
			tblTaxInvoices.select(tblAllInvoices, "*", "cflow_type EQ " + EndurCashflowType.MANUAL_VAT.id());
			
			if (tblTaxInvoices.getNumRows() == 0)
			{
				return tblTaxInvoices;
			}
			
			/* Set the JM custom doc numbers against each invoice */
			boolean useHistorics = true;
			enrichCustomInvoiceNumber(tblTaxInvoices, useHistorics);
			
			/* Enrich invoice numbers that are not in historics (odd case) */
			useHistorics = false;
			enrichCustomInvoiceNumber(tblTaxInvoices, useHistorics);
			
			/* Log a warning for any invoices that don't have a corresponding JM document number! */
			tblZeroData = Table.tableNew();
			tblZeroData.select(tblTaxInvoices, "DISTINCT, endur_doc_num, invoice_number", "invoice_number EQ 0"); 
			int numRows = tblZeroData.getNumRows();
			Logging.debug("Number of records in tblZeroData="+ numRows );
			for (int row = 1; row <= numRows; row++)
			{
				int invoiceNumber = tblZeroData.getInt("invoice_number", row);
				int endurDocNum = tblZeroData.getInt("endur_doc_num", row);
				
				if (invoiceNumber == 0)
				{
					Logging.warn("Invoice num is 0 for document_num "  + endurDocNum + ", removing from output");
				}
			}
			
			/* 
			 * For onward processing from here on, remove scenarios where no 
			 * invoice number exists (a warning for these would have been logged below 
			 */
			tblTaxInvoices.deleteWhereValue("invoice_number", 0);
			
			/* Copy "settle_amount" to "tax_in_deal_ccy" for later enrichment */
			tblTaxInvoices.addCol("tax_in_deal_ccy", COL_TYPE_ENUM.COL_DOUBLE);
			tblTaxInvoices.copyCol("settle_amount", tblTaxInvoices, "tax_in_deal_ccy");
			
			/* Apply FX conversion */
			tblTaxInvoices.addCol("tax_in_tax_ccy", COL_TYPE_ENUM.COL_DOUBLE);
			applyTaxReportingCurrency(tblTaxInvoices);
			
			/* Exclude these as these get totally ignored in the interface */
			tblTaxInvoices.deleteWhereString("tax_code", EndurTaxRate.REVERSE_CHARGE_GOLD_NEGATIVE.fullName());
			
			Logging.debug("Number of records in tblTaxInvoices="+ tblTaxInvoices.getNumRows() );
			
			return tblTaxInvoices;
		}
		finally
		{
			if (tblZeroData != null)
			{
				tblZeroData.destroy();
			}	
		}
	}
	
	/**
	 * Enrich tax info into cash events based on deal and endur/jm doc nums
	 * 
	 * @param tblCashEvents
	 * @param tblTaxEvents
	 * @throws OException
	 */
	private void mergeTaxInfo(Table tblCashEvents, Table tblTaxEvents) throws OException
	{
		if (tblTaxEvents.getNumRows() == 0)
		{
			return;
		}
		
		Table tblDistinctTaxRecords = null;
		Table tblFilter = null;
		
		try
		{
			tblDistinctTaxRecords = Table.tableNew("Unique tax records");
			
			String distinctKeys = "deal_num, endur_doc_num, endur_doc_status, invoice_number, stldoc_hdr_hist_id, ins_type, currency"; 
			String whereClause = getTableWhereClause(distinctKeys);

			/* Sum tax amounts by distinct keys */
			tblDistinctTaxRecords.select(tblTaxEvents, "DISTINCT, " + distinctKeys, "deal_num GT -1");
			tblDistinctTaxRecords.select(tblTaxEvents, "SUM, tax_in_deal_ccy, tax_in_tax_ccy", whereClause);
			
			/* Now merge tax amounts into the cash records */
			tblCashEvents.select(tblDistinctTaxRecords, "tax_in_deal_ccy, tax_in_tax_ccy", whereClause);
			Logging.debug("Number of records in tblDistinctTaxRecords="+tblDistinctTaxRecords.getNumRows());
			
			/* Copy remaining tax rows into the main table as regular cash invoices */
			tblFilter = Table.tableNew("Filter for tax invoices"); 
			tblFilter.select(tblCashEvents, "DISTINCT, " + distinctKeys, "deal_num GT -1");
			tblFilter.addCol("flag", COL_TYPE_ENUM.COL_INT);
			tblFilter.setColValInt("flag", 1);
			tblTaxEvents.select(tblFilter, "flag", whereClause);
			
			Logging.debug("Number of records in tblFilter="+tblFilter.getNumRows());
			
			tblTaxEvents.deleteWhereValue("flag", 1); // Remove rows that have already been merged into the main Cash table
			
			tblTaxEvents.copyRowAddAllByColName(tblCashEvents);
			Logging.debug("Number of records in tblCashEvents="+tblCashEvents.getNumRows());
		}
		finally
		{
			if (tblFilter != null)
			{
				tblFilter.destroy();	
			}
			
			if (tblDistinctTaxRecords != null)
			{
				tblDistinctTaxRecords.destroy();	
			}
		}
	}
	
	/**
	 * Some tax events are reported in a different currency then the tax currency itself. 
	 * 
	 * @param tblData
	 * @throws OException
	 */
	private void applyTaxReportingCurrency(Table tblData) throws OException
	{
		int numRows = tblData.getNumRows();
		for (int row = 1; row <= numRows; row++)
		{
			double fxRate = tblData.getDouble("fx_rate", row);
			fxRate = (fxRate == 0.0) ? 1.0 : fxRate;
			double baseAmount = Math.abs(tblData.getDouble("base_amount", row));
			double settleAmount = tblData.getDouble("settle_amount", row);
			
			/* 
			 * Check "base amount" event info field - this is set on tax events. If it is set, use it as 
			 * it seems to be more accurate in terms of rounding
			 */
			if (baseAmount != 0.0)
			{
				if (settleAmount < 0)
				{
					/* 
					 * Adjust signage on base amount (as it's always positive) so that 
					 * any double tax events can net each other out if need be 
					 */
					baseAmount *= -1;
				}
				
				tblData.setDouble("tax_in_tax_ccy", row, baseAmount);
			}
			else
			{
				settleAmount *= fxRate;
				tblData.setDouble("tax_in_tax_ccy", row, settleAmount);				
			}
		}
	}
	
	/**
	 * Set the default value of tax fields as per following rules -
	 * a)  If Base Currency and FX Rate are populated, use those for taxCurrency and taxExchRate
     * b)  If value of tax event is zero, ignore (no taxCurrency or taxExchRate)
     * c)  Otherwise, if there is a Base Currency for internal_lentity on the trade, use that as the taxCurrency and taxExchRate = 1
     * d)  Otherwise, taxCurrency = GBP and taxExchRate = 1
     * Set the tax_code to 'UK Std Tax' whereever cash-flow type is 'Manual VAT' (As suggested by Ian Compton based on implementation on JDE side)
	 * @param tblCashEvents
	 * @throws OException
	 */
	private void setDefaultTaxInfo(Table tblCashEvents) throws OException
	{
        int numRows = tblCashEvents.getNumRows();
        for (int row = 1; row <= numRows; row++)
        {
            int endurDocNum = tblCashEvents.getInt("endur_doc_num", row);  
            double taxAmount = 0.0;
            /* Populate the default value of 'taxCurrency' and 'taxExchRate' for the records where Tax is applicable */
            int taxColNum = tblCashEvents.getColNum("tax_in_deal_ccy");
            if(taxColNum > 0){
            	taxAmount = tblCashEvents.getDouble("tax_in_deal_ccy", row);	
            }
            String taxCurrency = tblCashEvents.getString("tax_ccy", row);
            int cashFlowType = tblCashEvents.getInt("cflow_type", row);
            if(taxAmount != 0.0 && (taxCurrency == null || taxCurrency.isEmpty()))
            {
                //fetch the base_currency of Internal Bunit
                taxCurrency = tblCashEvents.getString("base_currency_int_bu", row);
                if(taxCurrency == null || taxCurrency.isEmpty())
                {
                    //Default currency
                    taxCurrency = Constants.TAX_CCY_DEFAULT;
                }
                Logging.debug("Setting default value of tax currency for document num=" + endurDocNum + ", base currency=" + taxCurrency);
                tblCashEvents.setString("tax_ccy", row, taxCurrency);
                tblCashEvents.setDouble("tax_exchange_rate", row, 1.0);   
            }  
            if(EndurCashflowType.MANUAL_VAT.id() == cashFlowType)
            {
                tblCashEvents.setString("tax_code", row, Constants.TAX_CODE_FOR_MANUAL_VAT);   
            }
        }        
	}
	
	/**
	 * Enrich monetary values from "JM Tran Data" and merge into SL
	 * 
	 * @param tblAllInvoices
	 * @throws OException
	 */
	private void enrichSimData(Table tblAllInvoices) throws OException 
	{
		Table tblCommPhys = null;
		Table tblAllOtherInstruments = null;
		Table tblUniqueDeals = null;
		
		try
		{
			ArrayList<String> simResultEnums = new ArrayList<String>();
			simResultEnums.add(Constants.JM_TRAN_DATA_SIM_ENUM);
			
			tblUniqueDeals = Table.tableNew("Unique deals");
			
			/* Latest version of the deal is OK here */
			tblUniqueDeals.select(tblAllInvoices, "DISTINCT, deal_num", "deal_num GT -1");
			
			if (tblUniqueDeals.getNumRows() == 0) return;

			boolean useLatestDealVersion = true;
//			Table tblSimData = SimUtil.getSimData(tblUniqueDeals, simResultEnums, useLatestDealVersion);
//			Table tblGeneralResults = SimResult.getGenResults(tblSimData);
//			Table tblJmTranData = SimResult.findGenResultTable(tblGeneralResults, SimUtil.getResultId(Constants.JM_TRAN_DATA_SIM), -2, -2, -2);
			Table tblJmTranData = SimUtil.runTranDataSimResultInternal(tblUniqueDeals);
			/* Get ref data for COMM-PHYS only, as these will be reported per leg */
			tblCommPhys = Table.tableNew("COMM-PHYS trades");
			tblCommPhys.select(tblJmTranData, 
					"DISTINCT, deal_num, desk_location, trading_location, internal_portfolio, internal_bunit, external_bunit, internal_lentity, trade_date, reference, trade_type, reference, fixed_float, is_coverage, coverage_text",
					"ins_type_id EQ " + INS_TYPE_ENUM.comm_physical.toInt());
			
			Logging.debug("Number of records in tblCommPhys="+tblCommPhys.getNumRows());

			/* Remove COMM-PHYS as these are per leg */
			tblAllOtherInstruments = tblJmTranData.copyTable();
			tblAllOtherInstruments.deleteWhereValue("ins_type_id", INS_TYPE_ENUM.comm_physical.toInt());
			
			Logging.debug("Number of records in tblAllOtherInstruments="+tblAllOtherInstruments.getNumRows());

			int preEnrichment = tblAllInvoices.getNumRows();

			/* For COMM-PHYS, only need ref data attributes (not position/cash) */
			tblAllInvoices.select(tblCommPhys, 
					"desk_location, trading_location, internal_portfolio, internal_bunit, external_bunit, internal_lentity, trade_date, reference, trade_type, reference, fixed_float, is_coverage, coverage_text", 
					"deal_num EQ $deal_num");

			/* For every other instrument, include ref data and position/cash attributes too */
			tblAllInvoices.select(tblAllOtherInstruments, 
					"desk_location, trading_location, internal_portfolio, internal_bunit, external_bunit, internal_lentity, trade_date, reference, trade_type, reference, fixed_float, is_coverage, coverage_text, " +
							"from_currency, uom, to_currency, unit_price, position_uom, position_toz, base_currency_int_bu, ins_type_id,interest_rate(interest_rate_on_lease), pricing_type", 
					"deal_num EQ $deal_num");

			int postEnrichment = tblAllInvoices.getNumRows();
			if (preEnrichment != postEnrichment)
			{
				Logging.warn("Number of rows mismatch after Tran Data enrichment, before: " + preEnrichment + ", after: " + postEnrichment);
			}
		}
		catch (Exception e)
		{
			throw new AccountingFeedRuntimeException("Error encountered duruing enrichSimData()", e);
		}
		finally
		{
			if (tblCommPhys != null) 
			{
				tblCommPhys.destroy();
			}
			
			if (tblAllOtherInstruments != null) 
			{
				tblAllOtherInstruments.destroy();
			}

			if (tblUniqueDeals != null)
			{
				tblUniqueDeals.destroy();	
			}	
		}
	}

	/**
	 * Enrich market data from user_jm_pnl_market_data
	 * 
	 * @param tblCashEvents
	 * @throws OException 
	 */
	private void enrichMarketData(Table tblCashEvents) throws OException 
	{
		Table tblUniqueDeals = null;
		Table tblPnlMarketData = null;
		Table metalSwaps = null;
		
		try
		{
			tblUniqueDeals = Table.tableNew("Unique deals");
			tblUniqueDeals.select(tblCashEvents, "DISTINCT, deal_num", "deal_num GT -1");
			
			Logging.debug("Number of records in tblUniqueDeals="+tblUniqueDeals.getNumRows());
			
			tblPnlMarketData = Util.getPnlMarketData(tblUniqueDeals);
			tblCashEvents.select(tblPnlMarketData, 
					"spot_equivalent_price, " +
					"spot_equivalent_value, " +
					"settlement_value, " +
					"fwd_rate(deal_exchange_rate)",
	                "deal_num EQ $deal_num ");	
            Logging.debug("Market data populated");

			metalSwaps = Table.tableNew();
            metalSwaps.select(tblCashEvents, "DISTINCT, deal_num", "ins_type_id EQ " + INS_TYPE_ENUM.metal_swap.toInt());
            metalSwaps.select(tblPnlMarketData, "metal_volume_uom(position_uom), metal_volume_toz(position_toz), trade_price(unit_price)", "deal_num EQ $deal_num");
            
            tblCashEvents.select(metalSwaps, "position_uom,position_toz,unit_price", "deal_num EQ $deal_num");
            Logging.debug("data populated for metal swaps. Num rows=" + metalSwaps.getNumRows());
            
            Logging.debug("Number of records in tblCashEvents="+tblCashEvents.getNumRows());
		} 
		catch (AccountingFeedRuntimeException | OException e)
		{
			String message = "Error encountered during enrichMarketData: " + e.getMessage();
			
			Logging.error(message);
			
			throw new AccountingFeedRuntimeException("Error encountered duruing enrichMarketData()", e);
		}
		finally
		{
			if (tblUniqueDeals != null)
			{
				tblUniqueDeals.destroy();
			}
			
			if (tblPnlMarketData != null)
			{
				tblPnlMarketData.destroy();
			}
            if (metalSwaps != null)
            {
                metalSwaps.destroy();
            }
		}
	}
	
	/**
	 * Stamp "Our Doc Num" (doc info field) against documents that have been "Sent to Counterparty"
	 * 
	 * Stamp "Cancellation Doc Num" (doc info field) against documents that have been "Cancelled"
	 * 
	 * Uses stldoc_info_h for historics
	 * Uses stldoc_info for current invoices (but only where the invoice number doesn't exist in historics!)
	 * 
	 * @param output
	 * @param useHistorics - true or false to query historic/current table respectively
	 * @throws OException
	 */
	private void enrichCustomInvoiceNumber(Table output, boolean useHistorics) throws OException 
	{
		if (output.getNumRows() == 0) return;
		
		/* These are the doc info fields we are mainly interested in */
		int docInfos[] = new int[] { 
				EndurDocumentInfoField.OUR_DOC_NUM.toInt(),
				EndurDocumentInfoField.CANCELLATION_DOC_NUM.toInt()
		};

		/* This will store the custom JM doc numbers marked against the native Endur document number */
		Table tblJmCustomDocumentNumbers = null;
		
		/* This will store any data rows from the "output" table that has invoice_number = 0 */
		Table tblMissingInvoiceNumbers = null;
		
		/* These will store the custom doc numbers accordingly for cancellations and sent invoices */
		Table tblOurDocNum = null;
		Table tblCancellationDocNum = null;	
		
		try
		{	
			if (useHistorics)
			{
				/* Get JM doc numbers from "stldoc_info_h" */
				tblJmCustomDocumentNumbers = Util.getHistoricDocumentNumbers(output, docInfos);
			}
			else
			{
				/* 
				 * If non historic, only try enrich for document numbers that haven't already been enriched. So this
				 * is where the invoice_number is zero (as per table.select below)
				 */
				tblMissingInvoiceNumbers = Table.tableNew("Missing Invoice Numbers");
				tblMissingInvoiceNumbers.select(output, "*", "invoice_number EQ 0");
				
				if (tblMissingInvoiceNumbers.getNumRows() == 0) return;
				
				/* 
				 * Now delete these zero rows from the output table as we are going to end up enriching
				 * these in a different table, and then copying back to the output (to avoid any table.select discrepancies)
				 */
				output.deleteWhereValue("invoice_number", 0);
				
				/* 
				 * This uses "stldoc_info" as opposed to "stldoc_info_h"
				 * 
				 * Get a list of invoices where the JM Invoice Numbers are missing in the "output" table. This is a very odd scenario, 
				 * but during testing it was observed that some (1 or 2) invoices have their custom invoice numbers in the current table as 
				 * opposed to historics. This supplementary enrichment ensures that we enrich JM document numbers from the "current" version of the table. 
				 */
				tblJmCustomDocumentNumbers = Util.getCurrentDocumentNumbers(tblMissingInvoiceNumbers, docInfos);
			}
			
			Logging.debug( "Number of records in tblJmCustomDocumentNumbers=" + tblJmCustomDocumentNumbers.getNumRows() );
			
			/* 
			 * Add an "endur_doc_status" column to set to "Sent 2 CP" or "Cancelled" depending on what info field we are enriching. 
			 * This supplementary column is added to  
			 */
			tblJmCustomDocumentNumbers.addCol("endur_doc_status", COL_TYPE_ENUM.COL_INT);
			
			/* Get the invoices stamped against "Our Doc Num" and enrich those first - these are for invoices that have been "Sent 2 CP" */
			tblOurDocNum = Table.tableNew("Our Doc Num");
			tblOurDocNum.select(tblJmCustomDocumentNumbers, "*", "type_id EQ " + EndurDocumentInfoField.OUR_DOC_NUM.toInt());
			//tblOurDocNum.setColValInt("endur_doc_status", EndurDocumentStatus.SENT_TO_COUNTERPARTY.id());
			
			if (useHistorics)
			{
				/* For historics, use the stldoc_hdr_hist_id for completeness */
				output.select(tblOurDocNum, "invoice_number", 
						"endur_doc_num EQ $endur_doc_num AND stldoc_hdr_hist_id EQ $stldoc_hdr_hist_id");		
			}
			else
			{
				tblMissingInvoiceNumbers.select(tblOurDocNum, "invoice_number", "endur_doc_num EQ $endur_doc_num ");				
			}
			
			/* Get the invoices stamped against "Cancellation Doc Num" and enrich those next - these are for invoices that have been "Cancelled" */
			tblCancellationDocNum = Table.tableNew("Cancellation Doc Num");
			tblCancellationDocNum.select(tblJmCustomDocumentNumbers, "*", "type_id EQ " + EndurDocumentInfoField.CANCELLATION_DOC_NUM.toInt());
			tblCancellationDocNum.setColValInt("endur_doc_status", EndurDocumentStatus.CANCELLED.id());
			
			if (useHistorics)
			{
				output.select(tblCancellationDocNum, "invoice_number", "endur_doc_num EQ $endur_doc_num AND endur_doc_status EQ $endur_doc_status AND stldoc_hdr_hist_id EQ $stldoc_hdr_hist_id");		
			}
			else
			{
				tblMissingInvoiceNumbers.select(tblCancellationDocNum, "invoice_number", "endur_doc_num EQ $endur_doc_num AND endur_doc_status EQ $endur_doc_status");			
			}
			
			Logging.debug( "Number of records in output=" + output.getNumRows() );
			
			/* Merge any records with zero invoices numbers (fixed by now) back into the main table */
			if (tblMissingInvoiceNumbers != null && tblMissingInvoiceNumbers.getNumRows() > 0)
			{
				tblMissingInvoiceNumbers.copyRowAddAllByColName(output);
			}
		}
		catch (Exception e)
		{
			throw new AccountingFeedRuntimeException("Error encountered during enrichCustomInvoiceNumber()", e);
		}
		finally
		{
			if (tblJmCustomDocumentNumbers != null)
			{
				tblJmCustomDocumentNumbers.destroy();
			}
			
			if (tblOurDocNum != null)
			{
				tblOurDocNum.destroy();
			}
			
			if (tblCancellationDocNum != null)
			{
				tblCancellationDocNum.destroy();
			}
			
			if (tblMissingInvoiceNumbers != null) 
			{
				tblMissingInvoiceNumbers.destroy();
			}
		}
	}

	@Override
	protected void registerConversions(Table output) throws OException 
	{
		
	}

	@Override
	protected void formatOutputData(Table output) throws OException 
	{
		
	}

	@Override
	protected void groupOutputData(Table output) throws OException 
	{
		output.clearGroupBy();
		output.group("endur_doc_num, stldoc_hdr_hist_id");
		output.groupBy();
	}

	/**
	 * Get a list of excluded counterparties, if set
	 * 
	 * @return
	 * @throws OException
	 */
	public HashSet<Integer> getExcludedCounterparties() throws OException
	{
		/* Empty by default */
		HashSet<Integer> set = new HashSet<Integer>();
		
		String excludedCounterparties = rp.getStringValue(ReportBuilderParameter.EXCLUDE_COUNTERPARTIES.toString());
		
		if (excludedCounterparties != null && !excludedCounterparties.equalsIgnoreCase(""))
		{
			try
			{
				String split[] = excludedCounterparties.split(",");

				int bunits = split.length;
				for (int i = 0 ; i < bunits; i++)
				{
					String bunit = split[i].trim();

					int bunitId = Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, bunit);

					set.add(bunitId);
				}
			}
			catch (Exception e)
			{
				/* Fail-safe */
			}
		}
		
		return set;
	}
	
	/**
	 * Get a list of the included lentities, if set 
	 * 
	 * @return
	 * @throws OException
	 */
	public HashSet<Integer> getIncludedInternalLentities() throws OException
	{
		/* Empty by default */
		HashSet<Integer> set = new HashSet<Integer>();

		String includedLentities = rp.getStringValue(ReportBuilderParameter.INCLUDE_INTERNAL_LENTITIES.toString());

		if (includedLentities != null && !includedLentities.equalsIgnoreCase(""))
		{
			try
			{
				String split[] = includedLentities.split(",");

				int lentities = split.length;
				for (int i = 0 ; i < lentities; i++)
				{
					String lentity = split[i].trim();
					int lentityId = Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, lentity);

					set.add(lentityId);
				}
			}
			catch (Exception e)
			{
				/* Fail safe */
			}			
		}
		
		return set;
	}

	/**
	 * think about
	 *	query below fetches both sent and cancelled. if cancelled, check if it was generated on the same day?? if so, leave em both in. if NOT, 
	 *	remove the Sent row (as it should have been sent on a prior day)
	 *	
	 * @return
	 * @throws OException
	 */
	public Table getLatestInvoices(List<JDEStatus> applicableJdeStatus) throws OException
	{
		Table tblInvoices = Table.tableNew("Invoices sent or cancelled");

        String notCancelDocumentStatuses = EndurDocumentStatus.SENT_TO_COUNTERPARTY.id() + ", " + EndurDocumentStatus.SEL_FOR_PAYMENT.id() + ", " +
                                                EndurDocumentStatus.UNDO_PAYMENT.id()+ ", "+ EndurDocumentStatus.RECEIVE_PAYMENT.id() + ", " +
                                                EndurDocumentStatus.IGNORE_FOR_PAYMENT.id() ;
		String applicableDocumentStatuses = notCancelDocumentStatuses + ", " + EndurDocumentStatus.CANCELLED.id();
	
		String applicableJDEStatuses = "";
		for (JDEStatus jdeStatus : applicableJdeStatus)
		{
			if (applicableJDEStatuses == null || "".equalsIgnoreCase(applicableJDEStatuses))
			{
				applicableJDEStatuses = "'" + jdeStatus.toString() + "'";	
			}
			else
			{
				applicableJDEStatuses += ", '" + jdeStatus.toString() + "'";
			}
		}
		
		/**
		 * In Production there should be no invoices for Metal Transfer & Metal Settlement events.
		 * However, even if they are produced incorrectly (as were created in Test env). SL extract shall exclude.
		 * To exclude these events, select only those events where currency is financial currency (or not precious metal)
		 */
		HashSet<Integer> preciousMetalCurrencies = Util.getPreciousMetalCurrencies();
		StringBuilder metalCurrencyCsv = new StringBuilder();
		for(int metalCurrency : preciousMetalCurrencies)
		{
		    metalCurrencyCsv.append(metalCurrency).append(",");
		}
		int metalCcyLength = metalCurrencyCsv.length();
		String metalCcyCondition = "";
		if(metalCcyLength > 1)
		{
		    metalCurrencyCsv.deleteCharAt(metalCcyLength-1);
		    metalCcyCondition = "AND ate.currency not in (" + metalCurrencyCsv.toString() + ")" ;
		}
		
		String tranNumCondition = "";
		if(getTranTableQueryId() > 0)
		{
		    tranNumCondition = "JOIN ( select query_result from query_result where unique_id=" + getTranTableQueryId() + " ) q ON ab.tran_num=q.query_result \n";
		}
		
		String sqlQuery = 
			"SELECT \n" +
				"shh.document_num AS endur_doc_num, \n" + 
				"shh.doc_status AS endur_doc_status, \n" +
				"shh.last_update AS last_doc_update_time, \n" +
				"shh.stldoc_hdr_hist_id, \n" +
				"shh.doc_version, \n" +
				"sdh.cflow_type, \n" +
				"sdh.deal_tracking_num AS deal_num, \n" +
				"ate.ins_para_seq_num AS deal_leg, \n" +
				"ate.ins_seq_num, \n" +
				"sdh.tran_num, \n" +
				"sdh.event_num, \n" +
				"ab.ins_type, \n" +
				"ab.buy_sell, \n" +
				"ab.ins_sub_type, \n" +
				"ab.external_bunit, \n" +
				"ab.internal_lentity, \n" +
				"sdh.settle_amount, \n" +
				"sdh.settle_ccy AS currency, \n" + 
				"shh.doc_issue_date AS invoice_date, \n" + 
				"CAST(atei.value AS DATETIME) AS value_date, \n" +
				"atei2.value AS taxed_event_num, \n" +
				"CAST(atei3.value AS FLOAT) AS fx_rate, \n" +
				"CAST(atei5.value AS FLOAT) AS base_amount, \n" +
				"atei4.value AS base_currency, \n" +
				"atei6.value AS tax_code, \n" +
				"ate.event_date AS payment_date, \n" + 
				"sdh.event_date as stldoc_details_event_date, \n" + 
				"ate.event_date, \n" +
				"usdt.sl_status \n" + 
			"FROM stldoc_details_hist sdh \n" +  
			"JOIN stldoc_header_hist shh ON sdh.document_num = shh.document_num AND sdh.doc_version = shh.doc_version \n" +
			"JOIN ab_tran ab ON sdh.tran_num = ab.tran_num \n" +
			tranNumCondition +
			"JOIN ab_tran_event ate ON sdh.event_num = ate.event_num " + metalCcyCondition + "\n" + 
			"LEFT JOIN ab_tran_event_info atei ON ate.event_num = atei.event_num AND atei.type_id = " + EndurEventInfoField.METAL_VALUE_DATE.toInt() + " \n" +
			"LEFT JOIN ab_tran_event_info atei2 ON ate.event_num = atei2.event_num AND atei2.type_id = " + EndurEventInfoField.TAXED_EVENT_NUM.toInt() + " \n" +			
			"LEFT JOIN ab_tran_event_info atei3 ON ate.event_num = atei3.event_num AND atei3.type_id = " + EndurEventInfoField.FX_RATE.toInt() + " \n" +
			"LEFT JOIN ab_tran_event_info atei4 ON ate.event_num = atei4.event_num AND atei4.type_id = " + EndurEventInfoField.BASE_CURRENCY.toInt() + " \n" +
			"LEFT JOIN ab_tran_event_info atei5 ON ate.event_num = atei5.event_num AND atei5.type_id = " + EndurEventInfoField.BASE_AMOUNT.toInt() + " \n" +
			"LEFT JOIN ab_tran_event_info atei6 ON ate.event_num = atei6.event_num AND atei6.type_id = " + EndurEventInfoField.TAX_RATE_NAME.toInt() + " \n" +
			"JOIN " + Constants.USER_JM_SL_DOC_TRACKING + " usdt ON shh.document_num = usdt.document_num \n" +
			"JOIN \n" +
			"( \n" + // This clause is to only load the latest document version. MAX(doc_version) not used because various other historic id's needed to join tax events later
				"SELECT \n" +
				"shh.document_num, \n" +
				"MAX(shh.doc_version) AS doc_version \n" +
				"FROM stldoc_header_hist shh \n" + 
				"WHERE shh.doc_status in (" + notCancelDocumentStatuses + ") \n" +
				"GROUP BY shh.document_num \n" +
                "UNION SELECT \n" +
                "shh.document_num, \n" +
                "MAX(shh.doc_version) AS doc_version \n" +
                "FROM stldoc_header_hist shh \n" + 
                "WHERE shh.doc_status in (" + EndurDocumentStatus.CANCELLED.id() + ") \n" +
                "GROUP BY shh.document_num \n" +
			") latest_doc_version ON shh.document_num = latest_doc_version.document_num AND shh.doc_version = latest_doc_version.doc_version \n" +
			"WHERE shh.doc_type = 1 -- Invoice \n" +
			"AND shh.stldoc_template_id IN (SELECT stldoc_template_id FROM stldoc_templates WHERE stldoc_template_name LIKE '%JM-Invoice%') \n" + 
			"AND shh.doc_status IN (" + applicableDocumentStatuses + ") \n" +
			"AND sdh.settle_amount != 0 \n" +
			//there is a special OLF task executed on the first of each month to create the rental deals, generate the invoices and send them to JDE (but they do not get stamped). So, Rentals are excluded here.
            "AND sdh.cflow_type not in (select id_number from cflow_type where name like 'Metal Rentals%') \n" +
			"AND usdt.sl_status IN (" + applicableJDEStatuses + ")";
		
		Logging.debug("sqlQuery " + sqlQuery);
		int ret = DBaseTable.execISql(tblInvoices, sqlQuery);
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			throw new AccountingFeedRuntimeException("Unable to run query: " + sqlQuery);
		}
		
		/* Makes debugging easier */
		tblInvoices.colConvertDateTimeToInt("event_date");
		tblInvoices.colConvertDateTimeToInt("value_date");
		tblInvoices.colConvertDateTimeToInt("payment_date");
		tblInvoices.colConvertDateTimeToInt("invoice_date");
		tblInvoices.colConvertDateTimeToInt("last_doc_update_time");
		
		/* Some custom manipulation */
		int numRows = tblInvoices.getNumRows();
		Logging.info("Latest invoices num = " + numRows);
		
		/* Get the missing event info for amended deals */
		Map<Long,MissingEventInfo> missingEventInfo = getMissingEventInfo(tblInvoices);
		Logging.debug("Count of missing tran event info: " + missingEventInfo.size());
		
		for (int row = 1; row <= numRows; row++)
		{
			int dealNum = tblInvoices.getInt("deal_num", row);
			int paymentDate = tblInvoices.getInt("payment_date", row);
			int paymentDateStldocDetails = tblInvoices.getInt("stldoc_details_event_date", row);
			int metalValueDate = tblInvoices.getInt("value_date", row);
			int eventDate = tblInvoices.getInt("event_date", row);
			long eventNum = tblInvoices.getInt64("event_num", row);
			String taxCode = tblInvoices.getString("tax_code", row);
			
			/* 
			 * Invoices are sometimes (not often) sent on Amended, and the deal is later Validated - 
			 * this checks if the event/payment date has changed since the last amendment and uses the payment date
			 * from stldoc_details if they differ
			 */
			if (paymentDate != paymentDateStldocDetails && paymentDateStldocDetails != 0)
			{
				Logging.warn("Payment date empty on deal: " + dealNum + ", please check underlying deal model!");
				tblInvoices.setInt("payment_date", row, paymentDateStldocDetails);
			}
			
			/* Update the missing event info values "Metal Value Date" and "Tax Rate Name" where applicable */
			if (metalValueDate <= 0 && missingEventInfo.containsKey(eventNum) && missingEventInfo.get(eventNum).getValueDate()>0){
				metalValueDate = missingEventInfo.get(eventNum).getValueDate();
				tblInvoices.setInt("value_date", row, metalValueDate);
			} 
			if (taxCode.isEmpty() && missingEventInfo.containsKey(eventNum) && !missingEventInfo.get(eventNum).getTaxCode().isEmpty()){
				taxCode = missingEventInfo.get(eventNum).getTaxCode();
				tblInvoices.setString("tax_code", row, taxCode);   
			}
			
			/* Set the cash event date for metal currency trades that do not have the "Metal Value Date" event info filled in */
			if ((metalValueDate == 0 || metalValueDate == -1))
			{
				tblInvoices.setInt("value_date", row, eventDate);    
			}
		}
		
		Logging.debug( "Number of records in tblInvoices=" + tblInvoices.getNumRows() );

		return tblInvoices; 
	}
	
	/**
	 * Filter out specific ref data from the input table
	 * 
	 * @param tblData
	 * @param excludedCounterparties
	 * @throws OException
	 */
	protected void filterRefData(Table tblData) throws OException
	{
		for (int row = tblData.getNumRows(); row >= 1; row--)
		{
			int bunit = tblData.getInt("external_bunit", row);
			int internalLentity = tblData.getInt("internal_lentity", row);
			
			if (excludedCounterparties.contains(bunit) || !includedLentities.contains(internalLentity))
			{
				tblData.delRow(row);	
			}
		}	
		
		Logging.debug( "Number of records in tblData=" + tblData.getNumRows() );
	}
	
	/**
	 * Since we have multiple records linked to an invoice number, and user_jm_sl_doc_tracking only
	 * stores one record per invoice number, this function will remove any rows that may have already
	 * been processed prior.
	 * 
	 * @param tblData
	 * @throws OException
	 */
	protected void filterSentRecords(Table tblData) throws OException
	{
		for (int row = tblData.getNumRows(); row >= 1; row--)
		{
			int docStatus = tblData.getInt("endur_doc_status", row);
			String jdeSentstamp = tblData.getString("sl_status", row);
			
			/* 
			 * If the doc status != "CANCELLED" and the stamp is "Pending Cancelled" - it means this line item record was already sent
			 * so can remove it from processing
			 */
			if (docStatus != EndurDocumentStatus.CANCELLED.id()  
					&& jdeSentstamp.equalsIgnoreCase(JDEStatus.PENDING_CANCELLED.toString()))
			{
				tblData.delRow(row);	
			}
		}	
		Logging.debug( "Number of records in tblData=" + tblData.getNumRows() );
	}
	
	protected List<JDEStatus> getApplicableJdeStatus()
	{
        List<JDEStatus> jdeStatus = new ArrayList<JDEStatus>();
        jdeStatus.add(JDEStatus.PENDING_SENT);
        jdeStatus.add(JDEStatus.PENDING_CANCELLED);
        jdeStatus.add(JDEStatus.NOT_SENT);  
        return jdeStatus;
	}

	protected int getTranTableQueryId() 
    {
        return tranTableQueryId;
    }

	protected void setTranTableQueryId(int tranTableQueryId) 
    {
        this.tranTableQueryId = tranTableQueryId;
    }	

	/**
	 * For HK, adjust the position and spot equiv value for partial invoiced trades (advanced pricing) 
	 * 
	 * @param tblCashEvents
	 * @throws OException
	 */
	private void adjustAdvancedPricedTrades(Table tblCashEvents) throws OException 
	{
		int numRows = tblCashEvents.getNumRows();
		
		for (int row = 1; row <= numRows; row++)
		{
			String pricingType = tblCashEvents.getString("pricing_type", row);
			
			if (Constants.ADVANCED_PRICING_AP.equalsIgnoreCase(pricingType) ||
					Constants.ADVANCED_PRICING_DP.equalsIgnoreCase(pricingType))
			{
				double settlementValue = Math.abs(tblCashEvents.getDouble("settlement_value", row));
				double invoiceCashAmount = Math.abs(tblCashEvents.getDouble("settle_amount", row));
				
				double ratio = 1;
				if (invoiceCashAmount != settlementValue && invoiceCashAmount < settlementValue)
				{
					ratio = invoiceCashAmount / settlementValue;
				}
				
				double positionUom = tblCashEvents.getDouble("position_uom", row);
				double positionToz = tblCashEvents.getDouble("position_toz", row);
				double spotEquivValue = tblCashEvents.getDouble("spot_equivalent_value", row);
				
				positionUom *= ratio;
				positionToz *= ratio;
				spotEquivValue *= ratio;
				
				tblCashEvents.setDouble("position_uom", row, positionUom);
				tblCashEvents.setDouble("position_toz", row, positionToz);
				tblCashEvents.setDouble("spot_equivalent_value", row, spotEquivValue);
			}
		}
	}
	
	/**
	 * Problem 1189 - Cancelled Invoice sent to JDE with incorrect Value Date and Tax Code
	 * Issue occurs when the underlying Deal is Amended before the Cancellation is sent to JDE.
	 * Missing Event Info fields corresponding to old Event Number will be retrieved from history table.
	 *
	 * @param tblInvoices the table of latest invoices
	 * @return the collection of missing event info
	 * @throws OException
	 */   
	private Map<Long,MissingEventInfo> getMissingEventInfo(Table tblInvoices) throws OException{
		Map<Long,MissingEventInfo> missingEventInfo = new HashMap<Long,MissingEventInfo>();     
		int invoiceRowCount = tblInvoices.getNumRows();
		if (invoiceRowCount == 0) {
			return missingEventInfo;
		}

		Table tblMissingEvent = com.olf.openjvs.Util.NULL_TABLE;
		Table tblMissingEventInfo = com.olf.openjvs.Util.NULL_TABLE;
		int queryId = 0;
		try {
			tblMissingEvent = new Table("Missing Events");
			tblMissingEvent.addCol("event_num", COL_TYPE_ENUM.COL_INT64);

			/* Gather all event numbers where value date or tax code is missing */
			for (int rowNum = 1; rowNum <= invoiceRowCount; rowNum++){
				int valueDate = tblInvoices.getInt("value_date", rowNum);
				String taxCode = tblInvoices.getString("tax_code", rowNum);
				if (valueDate <= 0 || taxCode.isEmpty()) {
					int row = tblMissingEvent.addRow();
					long eventNum = tblInvoices.getInt64("event_num", rowNum);
					tblMissingEvent.setInt64(1, row, eventNum);
				}
			}
			Logging.debug("Events number count where Value Date or Tax Code is missing: " + tblMissingEvent.getNumRows());

			/* Store all the event numbers in a query result table */
			queryId = Query.tableQueryInsert(tblMissingEvent, "event_num");
			String resultTable = Query.getResultTableForId(queryId);
			Logging.debug("Stored missing event numbers in " + resultTable + " for unique id " + queryId);

			/* Query history table to fetch missing event info for amended deals*/
			String sqlQuery = 
					"SELECT \n" +
							"ate.event_num, \n" + 
							"cast(eih1.value AS DATETIME) AS value_date, \n" +
							"eih2.value AS tax_code \n" +
					"FROM ab_tran_event ate \n" +
					"INNER JOIN query_result qr ON ate.event_num = qr.query_result \n" +
					"LEFT JOIN (SELECT event_num,type_id,value, rank() OVER (PARTITION BY event_num ORDER BY last_update DESC) AS rowRank \n" +
								"FROM ab_tran_event_info_h where type_id = " + EndurEventInfoField.METAL_VALUE_DATE.toInt() + " ) eih1 \n" +
					"ON eih1.event_num = ate.event_num and eih1.rowRank = 1 \n" +
					"LEFT JOIN (SELECT event_num,type_id,value, rank() OVER (PARTITION BY event_num ORDER BY last_update DESC) AS rowRank \n" +
								"FROM ab_tran_event_info_h where type_id = " + EndurEventInfoField.TAX_RATE_NAME.toInt() + " ) eih2 \n" + 
					"ON eih2.event_num = ate.event_num and eih2.rowRank = 1 \n" +
					"WHERE EXISTS (SELECT 1 FROM ab_tran_history ath WHERE ath.tran_status = "+ TRAN_STATUS_ENUM.TRAN_STATUS_AMENDED.toInt() + " " + 
									"AND ath.tran_num = ate.tran_num) \n" +
					"AND (eih1.value IS NOT NULL OR eih2.value IS NOT NULL) \n" +
					"AND qr.unique_id = " + queryId;

			Logging.debug("Query for fetching missing Event Info: " + sqlQuery);

			tblMissingEventInfo = Table.tableNew("Missing Event Info");
			int ret = DBaseTable.execISql(tblMissingEventInfo, sqlQuery);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
				throw new AccountingFeedRuntimeException("Unable to run query: " + sqlQuery);
			}

			/* Add missing event info values into the map */
			int missingEventInfoRowCount =  tblMissingEventInfo.getNumRows();
			tblMissingEventInfo.colConvertDateTimeToInt("value_date");
			for (int rowNum = 1; rowNum <= missingEventInfoRowCount; rowNum++){
				long eventNum = tblMissingEventInfo.getInt64("event_num", rowNum);
				int valueDate = tblMissingEventInfo.getInt("value_date", rowNum);
				String taxCode = tblMissingEventInfo.getString("tax_code", rowNum);
				MissingEventInfo eventInfo = new MissingEventInfo.MissingEventInfoBuilder(eventNum)
												.ValueDate(valueDate)
												.TaxCode(taxCode)
												.build();
				missingEventInfo.put(eventNum, eventInfo);
				Logging.debug("Added missing event info to map: " + eventInfo.toString());
			}

		} catch (Exception e) {
			throw new AccountingFeedRuntimeException("An exception occured : " + e.getMessage());
		} finally {
			if (Table.isTableValid(tblMissingEvent) == 1){
				tblMissingEvent.destroy();
			}
			if (Table.isTableValid(tblMissingEventInfo) == 1){
				tblMissingEventInfo.destroy();
			}
			if (queryId != 0) {
				Query.clear(queryId);
			}
		}
		return missingEventInfo;
	}
}