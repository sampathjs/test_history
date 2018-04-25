package com.olf.recon.rb.datasource.endurinvoiceextract;

import java.util.HashSet;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.recon.enums.EndurCashflowType;
import com.olf.recon.enums.EndurDocumentStatus;
import com.olf.recon.enums.EndurEventInfoField;
import com.olf.recon.exception.ReconciliationRuntimeException;
import com.openlink.util.logging.PluginLog;

/**
 * Abstract class to hold base functionality for all invoices 
 */
public abstract class AbstractEndurInvoiceExtract
{
	/* 
	 * Start and end date to filter invoices on. This start/end date is either the invoice date (doc generation date) or the
	 * metal settlement date - it depends on the trade type.
	 */
	protected int windowStartDate;
	protected int windowEndDate;
	protected Table tblOutputStructure;

	public AbstractEndurInvoiceExtract(int windowStartDate, int windowEndDate, Table tblOutputStructure) throws OException
	{
		this.windowStartDate = windowStartDate;
		this.windowEndDate = windowEndDate;
		this.tblOutputStructure = tblOutputStructure;
		
		PluginLog.info("Abstract Invoice Extract, window_start_date: " + OCalendar.formatDateInt(windowStartDate));
		PluginLog.info("Abstract Invoice Extract, window_end_date: " + OCalendar.formatDateInt(windowEndDate));
		PluginLog.info("Abstract Invoice Extract, current_date for session: " + OCalendar.formatDateInt(OCalendar.today()));
		PluginLog.info("Abstract Invoice Extract, business_date for session: " + OCalendar.formatDateInt(Util.getBusinessDate()));
		PluginLog.info("Abstract Invoice Extract, trading_date for session: " + OCalendar.formatDateInt(Util.getTradingDate()));
	}

	/*
	 * This abstract class is designed to return all documents from the database, so these abstract functions
	 * should be implemented by subclasses to filter out accordingly. In this case, we have a subclass for handling invoices based on
	 * invoice date, and a separate class for invoices based on metal settlement date.
	 */
	protected abstract Table getCashInvoices() throws OException;
	protected abstract Table getTaxInvoices() throws OException;
	protected abstract Table generateMergedInvoiceData() throws OException;
	
	/**
	 * Merge all settlement and tax records into one table.
	 * 
	 * JM doc numbers can have both settlement and tax amounts in one document, so this function caters for this among
	 * other scenarios detailed below.
	 * 
	 * What complicates this is that invoice reporting is not based on the endur doc num, it is based on the JM doc num, and this
	 * is made up of 4 doc info fields;
	 * 1. Our Doc Num
	 * 2. Cancellation Doc Num
	 * 3. VAT Doc Num
	 * 4. Cancellation VAT Doc Num
	 */
	protected Table generateMergedInvoiceData(int jmVatDocInfoId) throws OException 
	{
		Table tblVATInvoices = null;
		Table tblCashInvoices = null;
		Table tblTaxInvoices = null;
		
		try
		{
			tblCashInvoices = getCashInvoices();
			tblTaxInvoices = getTaxInvoices();
			
			/* Main output table to merge both cash and tax invoices into */
			Table tblRet = tblOutputStructure.cloneTable();

			String selectDistinct = "DISTINCT, invoice_number, endur_doc_num, invoice_date, value_date, payment_date, currency, internal_lentity, external_bunit";
			
			String selectWhere = "invoice_number EQ $invoice_number AND endur_doc_num EQ $endur_doc_num "
					+ "AND invoice_date EQ $invoice_date AND value_date EQ $value_date AND payment_date EQ $payment_date AND currency EQ $currency "
					+ "AND internal_lentity EQ $internal_lentity AND external_bunit EQ $external_bunit";		

			if (tblCashInvoices.getNumRows() > 0)
			{
				tblRet.select(tblCashInvoices, selectDistinct, "invoice_number GT -1");
				tblRet.select(tblCashInvoices, "SUM, settle_amount(settlement_value_net)", selectWhere);
			}
			
			if (tblTaxInvoices.getNumRows() > 0)
			{
				/* 
				 * Scenario 1
				 * 
				 * Get tax invoices that are stamped against "VAT Invoice Doc Num" or "Cancellation VAT Num"
				 * 
				 * Merge these into "tblRet" as entirely new invoice records (new rows)
				 */
				tblVATInvoices = Table.tableNew("VAT Invoices");
				tblVATInvoices.select(tblTaxInvoices, "*", "type_id EQ " + jmVatDocInfoId);
				tblRet.select(tblVATInvoices, selectDistinct, "invoice_number GT -1");
				tblRet.select(tblVATInvoices, "SUM, settle_amount(tax_amount_in_tax_ccy)", selectWhere);
				
				/* Now remove the rows we have just merged in from scenario 1 (above) */
				tblTaxInvoices.deleteWhereValue("type_id", jmVatDocInfoId);
				
				/* 
				 * Scenario 2 
				 * 
				 * Merge tax amounts onto existing "Our Doc Num" invoices. These are documents where both the cash and tax amounts are on one paper document,
				 * because the currency that both the cash and tax settle in are the same!
				 */
				tblRet.select(tblTaxInvoices, "SUM, settle_amount(tax_amount_in_tax_ccy)", "invoice_number EQ $invoice_number");
				
				/* 
				 * Now remove the rows we have just merged in from scenario 2 (above) - we do this
				 * by checking the doc numbers that exist mutually between "tblRet" and "tblTaxInvoices" 
				 */
				removeRowsWhereExist(tblTaxInvoices, tblRet, "invoice_number");
				
				/* 
				 * Scenario 3
				 * 
				 * We are now left with invoices that are stamped against "Our Doc Num" or "Cancelled Doc Num" and are billed as regular
				 * cash invoices - but these need to be merged into "tblRet" as new Tax invoices (new rows) as
				 * they do not exist in "tblRet" at all
				 */
				tblRet.select(tblTaxInvoices, selectDistinct, "invoice_number GT -1");
				tblRet.select(tblTaxInvoices, "SUM, settle_amount(tax_amount_in_tax_ccy)", selectWhere);
			}
			
			return tblRet;
	
		}
		finally
		{
			if (tblVATInvoices != null)
			{
				tblVATInvoices.destroy();
			}
			
			if (tblCashInvoices != null)
			{
				tblCashInvoices.destroy();
			}
			
			if (tblTaxInvoices != null)
			{
				tblTaxInvoices.destroy();
			}
		}
	}
	
	/**
	 * Returns a table with the following sequence of events;
	 * 1. Given tblApplicableInvoices - a table of all invoices
	 * 2. Given tblJMDocumentNumbers - a table of JM doc numbers for a particular collection of doc info statues (cancelled or non cancelled)
	 * 3. Filter the applicable invoices for VAT events only
	 * 4. Merge the JM doc num into these cash events 
	 * 
	 * @param tblApplicableInvoices
	 * @param tblJMDocumentNumbers
	 * @param jmDocNumFieldId
	 * @return
	 * @throws OException
	 */
	protected Table getCashInvoices(Table tblApplicableInvoices, Table tblJMDocumentNumbers, int jmDocNumFieldId) throws OException 
	{
		Table tblZeroInvoiceNumbers = Table.tableNew("Zero invoice numbers");
		Table tblUniqueInvoiceNumbers = Table.tableNew("Unique invoice numbers");
		
		
		try
		{
			Table tblCashInvoices = tblApplicableInvoices.cloneTable();
			tblCashInvoices.setTableName("Cash Invoices");
		
			/* Only interested in NON VAT rows, so copy these across to "tblCashInvoices" */
			int numRows = tblApplicableInvoices.getNumRows();
			for (int row = 1; row <= numRows; row++)
			{
				int cflowType = tblApplicableInvoices.getInt("cflow_type", row);
				
				if (cflowType != EndurCashflowType.VAT.id() && cflowType != EndurCashflowType.MANUAL_VAT.id())
				{
					tblApplicableInvoices.copyRowAdd(row, tblCashInvoices);
				}
			}
		
			/* Merge "Our Doc Num" or "Cancellation Doc Num" into tblCashInvoices */
			tblCashInvoices.select(tblJMDocumentNumbers, "invoice_number", 
					"endur_doc_num EQ $endur_doc_num AND stldoc_hdr_hist_id EQ $stldoc_hdr_hist_id AND type_id EQ " + jmDocNumFieldId);
			
			/* There can potentially be rows with zero invoice numbers because they don't match on the stldoc hdr hist */
			tblZeroInvoiceNumbers.select(tblCashInvoices, "DISTINCT, endur_doc_num", "invoice_number EQ 0");
			
			/* We're not interested in the stldoc_hdr_hist_id for this bit so get a distinct list without historics */
			tblUniqueInvoiceNumbers.select(tblJMDocumentNumbers, "DISTINCT, endur_doc_num, invoice_number, type_id", "invoice_number GT 0");
			
			/* Select the invoice number over to theze zero based documents */
			tblZeroInvoiceNumbers.select(tblUniqueInvoiceNumbers, "invoice_number", "endur_doc_num EQ $endur_doc_num AND type_id EQ " + jmDocNumFieldId);
			
			/* Now re-select the correctly enriched invoice number back to the original tblCashInvoices to fix up the rows where invoice_number was zero */
			tblCashInvoices.select(tblZeroInvoiceNumbers, "invoice_number", "endur_doc_num EQ $endur_doc_num");
			
			return tblCashInvoices;		
		}
		finally
		{
			if (tblZeroInvoiceNumbers != null)
			{
				tblZeroInvoiceNumbers.destroy();
			}
			
			if (tblUniqueInvoiceNumbers != null)
			{
				tblUniqueInvoiceNumbers.destroy();
			}
		}
	}
	
	/**
	 * 
	 * @param tblApplicableInvoices
	 * @param tblJMDocumentNumbers
	 * @param jmDocNumFieldId
	 * @param jmVatDocNumFieldId
	 * @return
	 * @throws OException
	 */
	protected Table getTaxInvoices(Table tblApplicableInvoices, Table tblJMDocumentNumbers, int jmDocNumFieldId, int jmVatDocNumFieldId) throws OException
	{
		Table tblRemainingTaxInvoices = null;
		
		try
		{
			Table tblTaxInvoices = Table.tableNew("Tax Invoices");
			
			tblTaxInvoices.select(tblApplicableInvoices, "*", "cflow_type EQ " + EndurCashflowType.VAT.id());
			tblTaxInvoices.select(tblApplicableInvoices, "*", "cflow_type EQ " + EndurCashflowType.MANUAL_VAT.id());
			
			applyTaxFXConversion(tblTaxInvoices);
			updateTaxReportingCurrency(tblTaxInvoices);
			
			/* 
			 * Enrich with "VAT Invoice Doc Num" or "Cancelled VAT Invoice Num" - these are invoices that are billed in separate documents, 
			 * because the tax currency differs from the deals settle currency
			 */
			tblTaxInvoices.select(tblJMDocumentNumbers, 
					"invoice_number, type_id", 
					"endur_doc_num EQ $endur_doc_num AND stldoc_hdr_hist_id EQ $stldoc_hdr_hist_id AND type_id EQ " + jmVatDocNumFieldId);
			
			/* 
			 * For the records that don't have a "VAT Invoice Doc Num" or "Cancelled VAT Invoice Num" set (from the above select statement), 
			 * these will have an according "Our Doc Num" or "Cancellation Doc Num" - i.e, they are tax events billed as normal invoices 
			 */
			tblRemainingTaxInvoices = Table.tableNew();
			tblRemainingTaxInvoices.select(tblTaxInvoices, "*", "invoice_number EQ 0");
			
			/* 
			 * Remove any rows from the original tblTaxInvoices where invoice_number is zero. This means
			 * that this table (tblTaxInvoices) will be left with invoices that are stamped against the "VAT Invoice Doc Num" field or "Cancellation VAT Invoice Num"
			 * 
			 * For these records, update the reporting currency, and the settle amount, as the invoice is on a separate document
			 * due to the reporting currency for the Tax event being different from the reporting currency for the settlement
			 */
			tblTaxInvoices.deleteWhereValue("invoice_number", 0);
			
			/* 
			 * Select the "Our Doc Num" or "Cancellation Doc Num" info field into the remaining records that didn't have a "VAT/Cancellation Invoice Doc Num", 
			 * and then merge these rows back into "tblTaxInvoices" 
			 */ 
			tblRemainingTaxInvoices.select(tblJMDocumentNumbers, "invoice_number, type_id", 
					"endur_doc_num EQ $endur_doc_num AND stldoc_hdr_hist_id EQ $stldoc_hdr_hist_id AND type_id EQ " + jmDocNumFieldId);
			
			tblRemainingTaxInvoices.copyRowAddAll(tblTaxInvoices);
			
			return tblTaxInvoices;	
		}
		finally
		{
			if (tblRemainingTaxInvoices != null)
			{
				tblRemainingTaxInvoices.destroy();
			}
		}
	}
	
	/**
	 * Clean up - can be overridden 
	 * 
	 * @throws OException
	 */
	protected void cleanup() throws OException
	{
		if (tblApplicableInvoices != null)
		{
			tblApplicableInvoices.destroy();
			tblApplicableInvoices = null;
		}
	}
	
	/**
	 * Get all applicable invoices for the parameter dates, cache the data (should be destroyed after)
	 * 
	 * @return
	 * @throws OException
	 */
	private static Table tblApplicableInvoices = null;
	protected Table getAllApplicableInvoices() throws OException
	{
		if (tblApplicableInvoices == null)
		{
			tblApplicableInvoices = Table.tableNew("All invoices based on invoice date and metal value date");
			
			Table tblInvoicesBasedOnInvoiceDate = getInvoices(true, false);
			Table tblInvoicesBasedOnMetalValueDate = getInvoices(false, true);
			
			tblApplicableInvoices = tblInvoicesBasedOnInvoiceDate.cloneTable();
			tblInvoicesBasedOnInvoiceDate.copyRowAddAll(tblApplicableInvoices);
			
			removeRowsWhereExist(tblInvoicesBasedOnMetalValueDate, tblApplicableInvoices, "endur_doc_num");
			tblInvoicesBasedOnMetalValueDate.copyRowAddAll(tblApplicableInvoices);
		}
		
		return tblApplicableInvoices;
	}
	
	/**
	 * Return all Endur native invoice paper documents from the database
	 * 
	 * @param filterByInvoiceDate - filters invoices based on doc_issue_date
	 * @param filterByMetalSettlementDate - filters invoices based on deals metal value date
	 * @return 
	 * @throws OException
	 */
	protected Table getInvoices(boolean filterByInvoiceDate, boolean filterByMetalSettlementDate) throws OException
	{
		Table tblInvoices = Table.tableNew("Invoices on invoice date or metal value date");

		String applicableDocumentStatuses = EndurDocumentStatus.SENT_TO_COUNTERPARTY.id() + ", " + EndurDocumentStatus.CANCELLED.id();
		
		String sqlQuery = 
			"SELECT \n" +
				"0 AS invoice_number, \n" +
				"shh.document_num AS endur_doc_num, \n" + 
				"shh.doc_status AS endur_doc_status, \n" +
				"shh.stldoc_hdr_hist_id, \n" +
				"sdh.cflow_type, \n" +
				"sdh.deal_tracking_num AS deal_num, \n" +
				"ab.tran_status, \n" +
				"sdh.tran_num, \n" +
				"sdh.event_num, \n" +
				"ab.ins_type, \n" +
				"ab.internal_lentity, \n" + 
				"ab.external_bunit, \n" +
				"sdh.settle_amount, \n" +
				"sdh.settle_ccy AS currency, \n" + 
				"shh.doc_issue_date AS invoice_date, \n" + 
				"CAST(atei.value AS DATETIME) AS value_date, \n" +
				"atei2.value AS taxed_event_num, \n" +
				"CAST(atei3.value AS FLOAT) AS fx_rate, \n" +
				"CAST(atei5.value AS FLOAT) AS base_amount, \n" +
				"atei4.value AS base_currency, \n" +
				"ate.event_date AS payment_date, \n" + 
				"sdh.event_date as stldoc_details_event_date, \n" + 
				"ate.event_date \n" + 
		"FROM stldoc_details_hist sdh \n" + 
		"JOIN stldoc_header_hist shh ON sdh.document_num = shh.document_num AND sdh.doc_version = shh.doc_version \n" +
		"JOIN ab_tran ab ON sdh.tran_num = ab.tran_num \n" +
		"JOIN ab_tran_event ate ON sdh.event_num = ate.event_num \n" + 
		"LEFT JOIN ab_tran_event_info atei ON ate.event_num = atei.event_num AND atei.type_id = " + EndurEventInfoField.METAL_VALUE_DATE.toInt() + " \n" +
		"LEFT JOIN ab_tran_event_info atei2 ON ate.event_num = atei2.event_num AND atei2.type_id = " + EndurEventInfoField.TAXED_EVENT_NUM.toInt() + " \n" +
		"LEFT JOIN ab_tran_event_info atei3 ON ate.event_num = atei3.event_num AND atei3.type_id = " + EndurEventInfoField.FX_RATE.toInt() + " \n" +
		"LEFT JOIN ab_tran_event_info atei4 ON ate.event_num = atei4.event_num AND atei4.type_id = " + EndurEventInfoField.BASE_CURRENCY.toInt() + " \n" +
		"LEFT JOIN ab_tran_event_info atei5 ON ate.event_num = atei5.event_num AND atei5.type_id = " + EndurEventInfoField.BASE_AMOUNT.toInt() + " \n" +
		"JOIN \n" +
		"( \n" + // This clause is to only load the latest document version. MAX(doc_version) not used because various other historic id's needed to join tax events later
			"SELECT \n" +
			"shh.document_num, \n" +
			"shh.doc_status, \n" +
			"MAX(shh.doc_version) AS doc_version \n" +
			"FROM stldoc_header_hist shh \n" + 
			"WHERE shh.doc_status in (" + applicableDocumentStatuses + ") \n" +
			"GROUP BY shh.document_num, shh.doc_status \n" +
		") latest_doc_version ON shh.document_num = latest_doc_version.document_num AND shh.doc_status = latest_doc_version.doc_status AND shh.doc_version = latest_doc_version.doc_version \n" +
		"WHERE shh.doc_type = 1 -- Invoice \n" +
		"AND shh.stldoc_template_id IN (SELECT stldoc_template_id FROM stldoc_templates WHERE stldoc_template_name LIKE '%JM-Invoice%') -- 'JM Invoice' template \n" +  
		"AND shh.doc_status IN (" + applicableDocumentStatuses + ") \n" +
		"AND sdh.settle_amount != 0 \n" +
		"AND ate.currency NOT IN (SELECT id_number FROM currency WHERE precious_metal = 1)";

		if (filterByInvoiceDate)
		{
			sqlQuery += " \nAND shh.doc_issue_date >= '" + OCalendar.formatJdForDbAccess(windowStartDate) + "' \n";
			sqlQuery += "AND shh.doc_issue_date <= '" + OCalendar.formatJdForDbAccess(windowEndDate) + "'";
		}
		else if (filterByMetalSettlementDate)
		{
			sqlQuery += " \nAND CAST(atei.value AS DATETIME) >= '" + OCalendar.formatJdForDbAccess(windowStartDate) + "' \n";
			sqlQuery += "AND CAST(atei.value AS DATETIME) <= '" + OCalendar.formatJdForDbAccess(windowEndDate) + "' \n";
		}	
		
		int ret = DBaseTable.execISql(tblInvoices, sqlQuery);
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			throw new ReconciliationRuntimeException("Unable to run query: " + sqlQuery);
		}
		
		/* Makes debugging easier */
		tblInvoices.colConvertDateTimeToInt("invoice_date");
		tblInvoices.colConvertDateTimeToInt("payment_date");
		tblInvoices.colConvertDateTimeToInt("event_date");
		tblInvoices.colConvertDateTimeToInt("value_date");
		tblInvoices.setColFormatAsRef("ins_type", SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);
		tblInvoices.setColFormatAsRef("internal_lentity", SHM_USR_TABLES_ENUM.PARTY_TABLE);
		tblInvoices.setColFormatAsRef("external_bunit", SHM_USR_TABLES_ENUM.PARTY_TABLE);
		
		/* Some custom manipulation */
		int numRows = tblInvoices.getNumRows();
		for (int row = 1; row <= numRows; row++)
		{
			int paymentDate = tblInvoices.getInt("payment_date", row);
			int paymentDateStldocDetails = tblInvoices.getInt("stldoc_details_event_date", row);
			int valueDate = tblInvoices.getInt("value_date", row);
			int eventDate = tblInvoices.getInt("event_date", row);
			
			/* 
			 * Invoices are sometimes (not often) sent on Amended, and the deal is later Validated - 
			 * this checks if the event/payment date has changed since the last amendment and uses the payment date
			 * from stldoc_details if they differ
			 */
			if (paymentDate != paymentDateStldocDetails)
			{
				tblInvoices.setInt("payment_date", row, paymentDateStldocDetails);
			}
			
			/* Set the cash event date for metal currency trades that do not have the "Metal Value Date" event info filled in */
			if ((valueDate == 0 || valueDate == -1))
			{
				tblInvoices.setInt("value_date", row, eventDate);
			}
		}

		return tblInvoices; 
	}

	/**
	 * Filter out excluded bunits from data table
	 * 
	 * @param tblData
	 * @param excludedCounterparties
	 * @throws OException
	 */
	protected void filterCounterparties(Table tblData, HashSet<Integer> excludedCounterparties) throws OException
	{
		for (int row = tblData.getNumRows(); row >= 1; row--)
		{
			int bunit = tblData.getInt("external_bunit", row);
			
			if (excludedCounterparties.contains(bunit))
			{
				tblData.delRow(row);	
			}
		}	
	}
	
	/**
	 * Filter out included lentities from data table
	 * 
	 * @param tblData
	 * @param excludedCounterparties
	 * @throws OException
	 */
	protected void filterIncludedLentites(Table tblData, HashSet<Integer> includedLentites) throws OException
	{
		for (int row = tblData.getNumRows(); row >= 1; row--)
		{
			int internalLentity = tblData.getInt("internal_lentity", row);
			
			if (!includedLentites.contains(internalLentity))
			{
				tblData.delRow(row);	
			}
		}
	}
	
	/**
	 * Some tax events are invoiced in a different currency other then the actual tax event settle currency in Endur.
	 * 
	 * For these, take the value from the Base Currency event info and apply it for reporting purposes 
	 * 
	 * @param tblData
	 * @throws OException
	 */
	protected void updateTaxReportingCurrency(Table tblData) throws OException 
	{
		int numRows = tblData.getNumRows();
		for (int row = 1; row <= numRows; row++)
		{
			String baseCurrencyStr = tblData.getString("base_currency", row);
			
			if (baseCurrencyStr != null && !"".equalsIgnoreCase(baseCurrencyStr))
			{
				int baseCurrency = Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, baseCurrencyStr);
				
				tblData.setInt("currency", row, baseCurrency);
			}
		}
	}
	
	/**
	 * Some tax events are reported in a different currency then the tax currency itself. 
	 * 
	 * @param tblData
	 * @throws OException
	 */
	protected void applyTaxFXConversion(Table tblData) throws OException
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
				
				tblData.setDouble("settle_amount", row, baseAmount);
			}
			else
			{
				settleAmount *= fxRate;
				tblData.setDouble("settle_amount", row, settleAmount);				
			}
		}
	}
	
	/**
	 * Remove rows from "tblSource", which already exist in "tblDestination" - based on the key column.
	 * 
	 * @param tblSource
	 * @param tblDestination
	 * @param KeyColumn
	 * @throws OException
	 */
	protected void removeRowsWhereExist(Table tblSource, Table tblDestination, String keyColumn) throws OException
	{
		Table tblDistinct = null;
		
		try
		{
			if (tblSource.getNumRows() > 0)
			{
				/* Get a distinct list of records based on the key, and a flag = 1 against all of these rows */
				tblDistinct = Table.tableNew();
				tblDistinct.select(tblDestination, "DISTINCT, " + keyColumn, keyColumn + " GT -1");
				tblDistinct.addCol("flag", COL_TYPE_ENUM.COL_INT);
				tblDistinct.setColValInt("flag", 1);
				
				/* Select "flag" into "tblSource" based on key-match, and then remove rows where flag == 1 */
				tblSource.select(tblDistinct, "flag", keyColumn + " EQ $" + keyColumn);
				tblSource.deleteWhereValue("flag", 1);
				tblSource.delCol("flag");	
			}	
		}
		finally
		{
			if (tblDistinct != null)
			{
				tblDistinct.destroy();
			}
		}
	}
	
	/**
	 * Filter out rows in tblData where the endur_doc_status = "documentStatus"
	 * 
	 * @param tblData
	 * @param documentStatus
	 * @throws OException
	 */
	protected void filterDocumentStatus(Table tblData, int documentStatusId) throws OException
	{
		for (int row = tblData.getNumRows(); row >= 1; row--)
		{
			int docStatus = tblData.getInt("endur_doc_status", row);
			
			if (docStatus != documentStatusId)
			{
				tblData.delRow(row);
			}
		}
	}

	/**
	 * Retrieve historic invoice numbers from stldoc_info_h
	 * 
	 * @return
	 * @throws OException
	 */
	public static Table getHistoricDocumentNumbers(Table tblInvoices, int[] applicableDocInfos) throws OException
	{
		Table tblData = Table.tableNew("JM Historic Invoice Numbers");
		int queryId = 0;
		
		try
		{
			if (tblInvoices.getNumRows() == 0)
			{
				return tblData;
			}
			
			/* Build a csv list of doc info fields */
			StringBuilder builder = new StringBuilder();
			for (int docInfoFieldId : applicableDocInfos)
			{
				if (builder.toString().equalsIgnoreCase(""))
				{
					builder.append(docInfoFieldId);
				}
				else
				{
					builder.append(", ");
					builder.append(docInfoFieldId);
				}
			}
			
			queryId = Query.tableQueryInsert(tblInvoices, "endur_doc_num");
			
			if (queryId > 0)
			{
				String resultTable = Query.getResultTableForId(queryId);
				
				/* 
				 * We select the max invoice number because there are instances in the database where two doc numbers are generated 
				 * and logged in the historics for the same stldoc_hdr_hist_id. 
				 * 
				 * It's not clear why this occurs, but must be something to do with doc generation failing in the first place. The selection of the 
				 * max invoice number resolves this.
				 */
				String sqlQuery =
					"SELECT \n" +
						"data.endur_doc_num, \n" +
						"data.type_id, \n" +
						"MAX(data.invoice_number) as invoice_number, \n" +
						"data.stldoc_hdr_hist_id \n" +
						"FROM \n" +
						"( \n" +
							"SELECT \n" + 
								"sih.document_num AS endur_doc_num, \n" +
								"sih.type_id, \n" +	
								"CAST(sih.value AS INT) AS invoice_number, \n" +
								"sih.stldoc_hdr_hist_id \n" +
							"FROM \n" +
								resultTable + " qr \n" +
							"JOIN stldoc_info_h sih ON qr.query_result = sih.document_num \n" +
							"WHERE qr.unique_id = " + queryId + " \n" +
							"AND sih.type_id IN (" + builder.toString() + ") \n" +
						") data \n" +
					"GROUP BY data.endur_doc_num, data.type_id, data.stldoc_hdr_hist_id";
				
				int ret = DBaseTable.execISql(tblData, sqlQuery);
				if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				{
					throw new ReconciliationRuntimeException("Unable to run query: " + sqlQuery);
				}	
			}
		}
		finally
		{
			if (queryId > 0)
			{
				Query.clear(queryId);	
			}
		}
		
		return tblData;
	}

}
