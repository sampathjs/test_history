package com.jm.accountingfeed.rb.datasource.salesledger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.jm.accountingfeed.enums.EndurDocumentStatus;
import com.jm.accountingfeed.enums.EndurEventInfoField;
import com.jm.accountingfeed.enums.EndurPartyInfoExternalBunit;
import com.jm.accountingfeed.enums.EndurPartyInfoExternalLEntity;
import com.jm.accountingfeed.enums.JDEStatus;
import com.jm.accountingfeed.exception.AccountingFeedRuntimeException;
import com.jm.accountingfeed.util.Constants;
import com.jm.accountingfeed.util.Util;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.logging.PluginLog;

public class SalesLedgerSapExtract extends SalesLedgerExtract
{
	@Override
	protected void setOutputFormat(Table output) throws OException 
	{
		super.setOutputFormat(output);
		
		output.addCol("tran_status", COL_TYPE_ENUM.COL_INT);
		output.addCol("company_code", COL_TYPE_ENUM.COL_STRING); // legal entity
		output.addCol("business_unit", COL_TYPE_ENUM.COL_STRING); // business unit
	}
	
	@Override
	protected Table generateOutput(Table output) throws OException 
	{
		super.generateOutput(output);
		
		removeNonCoverageTrades(output);
		
		setSAPAttributes(output);
		
		return output;
	}

	/**
	 * Return the transaction status linked to the deals for each invoice
	 * 
	 * @param tblData
	 * @throws OException
	 */
	private void setSAPAttributes(Table tblData) throws OException 
	{
		if (tblData.getNumRows() == 0)
		{
			return;
		}
		
		int queryId = 0;
		Table tblTemp = null;
		
		try
		{
			queryId = Query.tableQueryInsert(tblData, "deal_num");	
			
			tblTemp = Table.tableNew("Tran Status Info");
			
			String sqlQuery = 
				"SELECT \n" +
					"ab.deal_tracking_num AS deal_num, \n" +
					"ab.tran_num, \n" +
					"ab.external_lentity, \n" +
					"ab.external_bunit, \n" +
					"pi1.value AS ext_legal_entity_code, \n" +
					"pi2.value AS ext_business_unit_code, \n" +
					"ab.tran_status \n" +
				"FROM " + Query.getResultTableForId(queryId) + " qr \n" + 
				"JOIN ab_tran ab ON qr.query_result = ab.deal_tracking_num \n" +
				"LEFT JOIN party_info pi1 ON ab.external_lentity = pi1.party_id AND pi1.type_id = " + EndurPartyInfoExternalLEntity.EXTERNAL_LEGAL_ENTITY_CODE.toInt() + "\n" +
				"LEFT JOIN party_info pi2 ON ab.external_bunit = pi2.party_id AND pi2.type_id = " + EndurPartyInfoExternalBunit.EXTERNAL_BUSINESS_UNIT_CODE.toInt() + " \n" +
				"WHERE qr.unique_id = " + queryId + " \n" + 
				"AND ab.current_flag = 1";
			
			int ret = DBaseTable.execISql(tblTemp, sqlQuery);
			
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				throw new RuntimeException("Unable to run query: " + sqlQuery);
			}
			
			if (tblTemp.getNumRows() > 0)
			{
				tblData.select(tblTemp, "tran_status, ext_legal_entity_code(company_code), ext_business_unit_code(business_unit)", "deal_num EQ $deal_num");	
			}
		}
		finally
		{
			if (queryId > 0)
			{
				Query.clear(queryId);			
			}
			
			if (tblTemp != null)
			{
				tblTemp.destroy();
			}
		}
	}

	/**
	 * Remove any trades that don't fall under "Coverage"
	 * 
	 * @param tblData
	 * @throws OException
	 */
	private void removeNonCoverageTrades(Table tblData) throws OException
	{
		for (int row = tblData.getNumRows(); row >= 1; row--)
		{
			String isCoverage = tblData.getString("is_coverage", row);
			
			if ("".equalsIgnoreCase(isCoverage) || "no".equalsIgnoreCase(isCoverage))
			{
				tblData.delRow(row);	
			}
		}	
	}
	
	@Override
	protected List<JDEStatus> getApplicableJdeStatus() 
	{
		List<JDEStatus> jdeStatus = new ArrayList<JDEStatus>();
		
        jdeStatus.add(JDEStatus.PENDING_SENT);
        jdeStatus.add(JDEStatus.PENDING_CANCELLED);
        jdeStatus.add(JDEStatus.NOT_SENT);
        
        return jdeStatus;
	}
	
	@Override
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
            "AND sdh.cflow_type not in (select id_number from cflow_type where name like 'Rentals%') \n" +
			"AND usdt.sap_status IN (" + applicableJDEStatuses + ")";
		
		PluginLog.debug("sqlQuery " + sqlQuery);
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
		PluginLog.info("Latest invoices num = " + numRows);
		for (int row = 1; row <= numRows; row++)
		{
			int dealNum = tblInvoices.getInt("deal_num", row);
			int paymentDate = tblInvoices.getInt("payment_date", row);
			int paymentDateStldocDetails = tblInvoices.getInt("stldoc_details_event_date", row);
			int metalValueDate = tblInvoices.getInt("value_date", row);
			int eventDate = tblInvoices.getInt("event_date", row);
			
			/* 
			 * Invoices are sometimes (not often) sent on Amended, and the deal is later Validated - 
			 * this checks if the event/payment date has changed since the last amendment and uses the payment date
			 * from stldoc_details if they differ
			 */
			if (paymentDate != paymentDateStldocDetails && paymentDateStldocDetails != 0)
			{
				PluginLog.warn("Payment date empty on deal: " + dealNum + ", please check underlying deal model!");
				tblInvoices.setInt("payment_date", row, paymentDateStldocDetails);
			}
			
			/* Set the cash event date for metal currency trades that do not have the "Metal Value Date" event info filled in */
			if ((metalValueDate == 0 || metalValueDate == -1))
			{
				tblInvoices.setInt("value_date", row, eventDate);
			}
		}
		
		PluginLog.debug( "Number of records in tblInvoices=" + tblInvoices.getNumRows() );

		return tblInvoices; 
	}
}