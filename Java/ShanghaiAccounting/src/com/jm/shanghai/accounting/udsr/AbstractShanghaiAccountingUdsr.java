package com.jm.shanghai.accounting.udsr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.jm.shanghai.accounting.udsr.control.MappingTableFilterApplicator;
import com.jm.shanghai.accounting.udsr.control.OutputTableRetrievalApplicator;
import com.jm.shanghai.accounting.udsr.control.RuntimeTableRetrievalApplicator;
import com.jm.shanghai.accounting.udsr.model.fixed.ConfigurationItem;
import com.jm.shanghai.accounting.udsr.model.fixed.PartyInfoFields;
import com.jm.shanghai.accounting.udsr.model.mapping.MappingConfigurationColType;
import com.jm.shanghai.accounting.udsr.model.mapping.MappingTableCellConfiguration;
import com.jm.shanghai.accounting.udsr.model.mapping.MappingTableColumnConfiguration;
import com.jm.shanghai.accounting.udsr.model.mapping.MappingTableRowConfiguration;
import com.jm.shanghai.accounting.udsr.model.mapping.predicate.AbstractPredicate;
import com.jm.shanghai.accounting.udsr.model.mapping.predicate.PredicateParser;
import com.jm.shanghai.accounting.udsr.model.retrieval.ColumnSemantics;
import com.jm.shanghai.accounting.udsr.model.retrieval.JavaTable;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfiguration;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescription;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescriptionLoader;
import com.olf.embedded.simulation.AbstractSimulationResult2;
import com.olf.embedded.simulation.RevalResult;
import com.olf.openrisk.application.EnumDebugLevel;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.control.EnumSplitMethod;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.simulation.Configuration;
import com.olf.openrisk.simulation.ConfigurationField;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.Scenario;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.table.ColumnFormatterAsDateTime;
import com.olf.openrisk.table.ColumnFormatterAsRef;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumFormatDateTime;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableColumn;
import com.olf.openrisk.table.TableFactory;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumInsSub;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumResetFieldId;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.EnumValueStatus;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2018-11-14		V1.0		jwaechter		- Initial Version
 * 2019-02-11		V1.1		jwaechter		- Moved company ID to ReportBuilder report
 *                                              - Removing unused code
 *                                              - Refactoring
 *                                              - Comments
 * 2019-07-14		V1.2		jwaechter		- Added logic for cust/comp mapping table.
 * 2019-08-20		V1.3		jwaechter		- Added logic for party codes of UK, US, HK
 * 2019-09-15		V1.4		jwaechter		- The runtime table structure is created 
 *                                                in fewer steps now.
 * 2019-09-25		V1.5		jwaechter		- Added retrieval for internal party currency codes                                                
 */

/**
 * Main Plugin to generate the raw data for the accounting postings 
 * in Shanghai.
 * @author jwaechter
 * @version 1.5
 */
public abstract class AbstractShanghaiAccountingUdsr extends AbstractSimulationResult2 {
	
	/**
	 * The configuration items designating the type of the result (GL vs SL) that is also used
	 * as a filter during rule mapping.
	 * */
	private final ConfigurationItem typePrefix;
		
	/**
	 * Column name of the an artificially created id column for mapping tables.
	 * This column name may not be used in any mapping table.	
	 */
	public static final String ROW_ID = "row_id";
	
	private static RetrievalConfigurationColDescriptionLoader colLoader;
	
	protected AbstractShanghaiAccountingUdsr (final ConfigurationItem typePrefix) {
		super();
		this.typePrefix = typePrefix;
	}

	@Override
	public void calculate(final Session session, final Scenario scenario,
			final RevalResult revalResult, final Transactions transactions,
			final RevalResults prerequisites) {
		init(session);
		colLoader = new RetrievalConfigurationColDescriptionLoader(session);
		RuntimeAuditingData runtimeAuditingData = new RuntimeAuditingData();
		session.setClientData(runtimeAuditingData);
		try {
			long startTime = System.currentTimeMillis();
			PluginLog.info("Starting calculate of Raw Accounting Data UDSR");
			Map<String, String> parameters = generateSimParamTable(scenario);
			PluginLog.debug (parameters.toString());
			List<RetrievalConfiguration> retrievalConfig = convertRetrievalConfigTableToList(session);

			JavaTable eventDataTable = createEventDataTable(transactions, session.getTableFactory(), revalResult);
			finalizeTableStructure(eventDataTable, session, scenario, revalResult, transactions, prerequisites, parameters, retrievalConfig);
			PluginLog.info("Creation of event data table finished");

			PluginLog.info("Starting retrieval");
			 // using java table didn't bring an advantage over using an endur table
			long startRetrieval = System.currentTimeMillis();
			eventDataTable.mergeIntoEndurTable(revalResult.getTable());
			// apply all currently hard coded data retrieval by executing certain SQLs
			// and add the SQL results to the runtime table.
			retrieveAdditionalData (session, revalResult, transactions);
			runtimeAuditingData.setRetrievalConfig(retrievalConfig);
			// apply generic data retrieval according to the configuration in the retrieval table
			applyRetrievalToRuntimeTable(session, scenario, revalResult,
					transactions, prerequisites, parameters, retrievalConfig);
			addForwardRateForContangoBackwardation(session, revalResult, transactions);
			addForwardRateForContangoBackwardationCorrectingDeals(session, revalResult, transactions);
			calculateContangoBackwardation(session, revalResult);
			long endRetrieval = System.currentTimeMillis();
			PluginLog.info("Finished retrieval. Computation time (ms): " + (endRetrieval-startRetrieval));
			// Apply hard wired formatting to certain columns to ensure the mapping takes names
			// not IDs
			formatColumns (revalResult.getTable());
			// generate unique row ids for each existing row in the runtime table before applying mapping
			// this allows later removal of rows from the runtime table that do no match.
			generateUniqueRowIdForTable(revalResult.getTable(), false);
			showRuntimeDataTable(ConfigurationItem.VIEW_RUNTIME_DATA_TABLE_BEFORE_MAPPING, session, revalResult.getTable());
			// Apply mapping to account mapping table, tax code mapping table and material number mapping table
			applyAllMappings(session, revalResult, retrievalConfig);
			showRuntimeDataTable(ConfigurationItem.VIEW_RUNTIME_DATA_TABLE_AFTER_MAPPING, session, revalResult.getTable());
			// ensures updates based on changes in the const repo desktop are used
			ConfigurationItem.resetValues();
			long endTime = System.currentTimeMillis();
			PluginLog.info("Execution Time in ms: " + (endTime - startTime));
			PluginLog.info("Completed calculate of Raw Accounting Data UDSR");
		} catch (Throwable t) {
			PluginLog.info(t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				PluginLog.info(ste.toString());
			}
			throw t;
		}
	}

	private void finalizeTableStructure(JavaTable eventDataTable,
			final Session session,
			final Scenario scenario, final RevalResult revalResult,
			final Transactions transactions, final RevalResults prerequisites,
			Map<String, String> parameters,
			List<RetrievalConfiguration> retrievalConfig) {
		// add all new columns to the runtime table in advance in a single step
		// 1. all columns resulting from hard coded retrieval
		String colNames[] = { "int_bu_code", "int_bunit_id", "external_party_is_jm_group",
				"external_party_is_internal", "region_ext_bu", "region_int_bu", "party_code_uk", "party_code_us", "party_code_hk", "party_code_cn_debtor", "party_code_cn_creditor", "ext_bu_jm_group", "endur_doc_num", "endur_doc_status", "jde_doc_num", "jde_cancel_doc_num", "vat_invoice_doc_num", 
				"customer_code_usd", "customer_code_gbp", "customer_code_eur", "customer_code_zar",
				"server_date", "eod_date", "business_date", "processing_date", "trading_date", 
				"latest_fixing_date", "latest_date_unfixed", 
				"contango_settlement_value", "contango_spot_equiv_value", "contango_backwardation",
				"contango_settlement_value_correcting_deal", "contango_spot_equiv_value_correcting_deal",
				"near_start_date", "far_start_date", "form_near", "form_far", "loco_near", "loco_far", "swap_type",
				"country_code_ext_bu"};
		EnumColType colTypes[] = {EnumColType.String, EnumColType.Int, EnumColType.String, 
				EnumColType.Int, EnumColType.String, EnumColType.String, EnumColType.String, EnumColType.String, EnumColType.String, EnumColType.String, EnumColType.String, EnumColType.String, EnumColType.Int, EnumColType.Int, EnumColType.String, EnumColType.String, EnumColType.String, 
				EnumColType.String, EnumColType.String, EnumColType.String, EnumColType.String,
				EnumColType.Int, EnumColType.Int, EnumColType.Int, EnumColType.Int, EnumColType.Int,
				EnumColType.Int, EnumColType.Int, 
				EnumColType.Double, EnumColType.Double, EnumColType.String,
				EnumColType.Double, EnumColType.Double,
				EnumColType.DateTime, EnumColType.DateTime, EnumColType.String, EnumColType.String, EnumColType.String, EnumColType.String, EnumColType.String,
				EnumColType.String};
		for (int i=0; i < colNames.length; i++) {
			eventDataTable.addColumn(colNames[i], colTypes[i]);
		}
		// Unique ID column
		if (eventDataTable.isValidColumn(ROW_ID)) {
			String errorMessage = "The row '" + ROW_ID + " does already exist in the table."
					+ " This row is important to contain a unique row number. Can't proceed. "
					+ " Please ensure the table to not contain a row named '"
					+ ROW_ID + "'";
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		eventDataTable.addColumn(ROW_ID, EnumColType.Int);
		// retrieval configuration columns:
		Collections.sort(retrievalConfig); // ensure priority based execution
		for (RetrievalConfiguration rc : retrievalConfig) {
			RuntimeTableRetrievalApplicator retrievalApplicator = new RuntimeTableRetrievalApplicator(this, rc, colLoader);
			String colName = retrievalApplicator.getColNameRuntimeTable();
			EnumColType colType = retrievalApplicator.getColType(eventDataTable, session, scenario, prerequisites, transactions, parameters);
			eventDataTable.addColumn(colName, colType);
		}		
	}

	/**
	 * This method requires a column labelled "spot_equiv_price" of type double to be present.
	 * Currently it is assumed that this column is retrieved via the USER_jm_acc_retrieval_config
	 * table.
	 * 
	 * @param session
	 * @param revalResult
	 */
	private void calculateContangoBackwardation(Session session,
			RevalResult revalResult) {
		Table runtimeTable = revalResult.getTable();
		for (int rowId = runtimeTable.getRowCount()-1; rowId >= 0; rowId--) {
			String correctiveDeal = runtimeTable.getString("correcting_deal", rowId);
			String colNameSettlementValue = null;
			String colNameSpotEquivValue = null;
			if (correctiveDeal != null && !correctiveDeal.isEmpty()) {
				colNameSettlementValue = "contango_settlement_value_correcting_deal";
				colNameSpotEquivValue = "contango_spot_equiv_value_correcting_deal";				
			} else {
				colNameSettlementValue = "contango_settlement_value";
				colNameSpotEquivValue = "contango_spot_equiv_value";
			}
			double settlementValue = runtimeTable.getDouble(colNameSettlementValue, rowId);
			double spotEquivValue = runtimeTable.getDouble(colNameSpotEquivValue, rowId);
			if (spotEquivValue < settlementValue) {
				runtimeTable.setString("contango_backwardation", rowId, "C");
			} else {
				runtimeTable.setString("contango_backwardation", rowId, "B");				
			}
		}
	}

	private void retrieveAdditionalData(Session session,
			RevalResult revalResult, Transactions transactions) {
		Table runtimeTable = revalResult.getTable();
		
		if (runtimeTable.getRowCount() == 0) {
			return;
		}
		for (int row=runtimeTable.getRowCount()-1; row>=0;row--) {
			int dealTrackingNum = runtimeTable.getInt("deal_tracking_num", row);
			Transaction tran = transactions.getTransaction(dealTrackingNum);
			runtimeTable.setInt("int_bunit_id", row, tran.getValueAsInt(EnumTransactionFieldId.InternalBusinessUnit));
		}
		formatColumn (runtimeTable, "external_party_is_internal", EnumReferenceTable.InternalExternal);
		formatColumn (runtimeTable, "int_bunit_id", EnumReferenceTable.Party);
		Table partyInfoTable = createPartyInfoTable(session);
		joinPartyCodes(session, revalResult, transactions, partyInfoTable);
		int colNumExternalPartyIsInternal = runtimeTable.getColumnId("external_party_is_internal");
		int colNumExtBuJmGroup = runtimeTable.getColumnId("ext_bu_jm_group");		
		for (int row=runtimeTable.getRowCount()-1; row>=0;row--) {
			String externalPartyIsInternal = runtimeTable.getDisplayString(colNumExternalPartyIsInternal, row);
			String extBuJmGroup = runtimeTable.getDisplayString(colNumExtBuJmGroup, row);
			if (externalPartyIsInternal.equals("Internal") || extBuJmGroup.equals("Yes") ) {
				runtimeTable.setString("external_party_is_jm_group", row, "Yes");
			} else {				
				runtimeTable.setString("external_party_is_jm_group", row, "No");
			}
		}
		
		ConstTable documentInfoTable = createDocumentInfoTable (session, runtimeTable);
		joinDocumentInfo(session, revalResult, documentInfoTable);
		
		ConstTable swapInfoTable = createSwapInfoTable (session, runtimeTable, transactions);
		runtimeTable.select(swapInfoTable, "swap_type,near_start_date,far_start_date,form_near,form_far,loco_near,loco_far", "[In.tran_num] == [Out.tran_num]");

		addDates(session, revalResult, transactions);
		addCountryCodeExternalBu(session, runtimeTable, transactions);
	}

	private void addCountryCodeExternalBu(Session session,
			Table runtimeTable, Transactions transactions) {
		StringBuilder allTranNums = createTranNumList(runtimeTable, "tran_num");
		// assumption: there is only one invoice per deal
		String sql = 
				"\nSELECT DISTINCT ab.tran_num"
			+	"\n, c.iso_code AS country_code_ext_bu"
			+	"\nFROM ab_tran ab"
			+ 	"\nINNER JOIN party ext_bunit"
			+   "\n ON ext_bunit.party_id = ab.external_bunit"
			+   "\nINNER JOIN party_address main_address"
			+   "\n ON ext_bunit.party_id = main_address.party_id"
			+	"\n   AND main_address.address_type = 1" // 1 = main, Endur default, no dynamic retrieval necessary
			+   "\nINNER JOIN country c"
			+   "\n ON c.id_number = main_address.country"	
			+	"\nWHERE ab.tran_num IN (" + allTranNums.toString() + ")";
		session.getDebug().logLine(sql, EnumDebugLevel.High);
		Table countryCodeTable = session.getIOFactory().runSQL(sql);
		runtimeTable.select(countryCodeTable, "country_code_ext_bu", "[In.tran_num] == [Out.tran_num]");
	}

	private ConstTable createSwapInfoTable(Session session, Table runtimeTable,
			Transactions transactions) {
		StringBuilder allTranNums = createTranNumList(runtimeTable, "tran_num");
		// assumption: there is only one invoice per deal
		String sql = 
				"\nSELECT DISTINCT ab.tran_num"
			+	"\n,  ab_near.start_date AS near_start_date"
			+	"\n,  ab_far.start_date AS far_start_date"
			+ 	"\n,  ISNULL(form_near_info.value, form_info_type.default_value) AS form_near"
			+ 	"\n,  ISNULL(form_far_info.value, form_info_type.default_value) AS form_far"
			+ 	"\n,  ISNULL(loco_near_info.value, loco_info_type.default_value) AS loco_near"
			+ 	"\n,  ISNULL(loco_far_info.value, loco_info_type.default_value) AS loco_far"
			+	"\n,  '' AS swap_type"
			+	"\nFROM ab_tran ab"
			+	"\nINNER JOIN ab_tran ab_far"
			+	"\n ON ab_far.tran_group = ab.tran_group"
			+	"\n    AND ab_far.ins_sub_type = " + EnumInsSub.FxFarLeg.getValue()
			+	"\nINNER JOIN ab_tran ab_near"
			+	"\n ON ab_near.tran_group = ab.tran_group"
			+	"\n    AND ab_near.ins_sub_type = " + EnumInsSub.FxNearLeg.getValue()
			+   "\nINNER JOIN tran_info_types form_info_type"
			+	"\n ON form_info_type.type_name = 'Form'"
			+   "\nINNER JOIN tran_info_types loco_info_type"
			+	"\n ON loco_info_type.type_name = 'Loco'"
			+	"\nLEFT OUTER JOIN ab_tran_info form_near_info"
			+	"\n ON form_near_info.tran_num = ab_near.tran_num"
			+	"\n    AND form_near_info.type_id = form_info_type.type_id"
			+	"\nLEFT OUTER JOIN ab_tran_info form_far_info"
			+	"\n ON form_far_info.tran_num = ab_far.tran_num"
			+	"\n    AND form_far_info.type_id = form_info_type.type_id"
			+	"\nLEFT OUTER JOIN ab_tran_info loco_near_info"
			+	"\n ON loco_near_info.tran_num = ab_near.tran_num"
			+	"\n    AND loco_near_info.type_id = loco_info_type.type_id"
			+	"\nLEFT OUTER JOIN ab_tran_info loco_far_info"
			+	"\n ON loco_far_info.tran_num = ab_far.tran_num"
			+	"\n    AND loco_far_info.type_id = loco_info_type.type_id"
			+	"\nWHERE ab.tran_num IN (" + allTranNums.toString() + ")"
			;
		session.getDebug().logLine(sql, EnumDebugLevel.High);
		Table swapData = session.getIOFactory().runSQL(sql);
		
		for (int row=swapData.getRowCount()-1; row >= 0; row--) {
			Date nearDate = swapData.getDate("near_start_date", row);
			Date farDate = swapData.getDate("far_start_date", row);
			String formNear = swapData.getString("form_near", row);
			String formFar = swapData.getString("form_far", row);
			String locoNear = swapData.getString("loco_near", row);
			String locoFar = swapData.getString("loco_far", row);
			if (nearDate.before(farDate)) {
				swapData.setString("swap_type", row, "Repo");
			} else if (formNear != null && formFar != null && !formNear.equals(formFar)) {
				swapData.setString("swap_type", row, "Form");
			} else if (locoNear != null && locoFar != null && !locoNear.equals(locoFar)) {
				swapData.setString("swap_type", row, "Loco");
			}
		}
		return swapData;
	}

	private void addDates(Session session, RevalResult revalResult, Transactions transactions) {
		CalendarFactory cf = session.getCalendarFactory();
		Date serverDate = session.getServerTime();
		Date eodDate = session.getEodDate();
		Date businessDate = session.getBusinessDate();
		Date processingDate = session.getProcessingDate();
		Date tradingDate = session.getTradingDate();
		String colNames[] = {"server_date", "eod_date", "business_date", "processing_date", "trading_date",
				"latest_fixing_date", "latest_date_unfixed"};
		ColumnFormatterAsDateTime cfadt = revalResult.getTable().getFormatter().createColumnFormatterAsDateTime(EnumFormatDateTime.Date);
		for (String colName : colNames) {
			revalResult.getTable().getFormatter().setColumnFormatter(colName, cfadt);			
		}
		revalResult.getTable().setColumnValues("server_date", cf.getJulianDate(serverDate));
		revalResult.getTable().setColumnValues("eod_date", cf.getJulianDate(eodDate));
		revalResult.getTable().setColumnValues("business_date", cf.getJulianDate(businessDate));
		revalResult.getTable().setColumnValues("processing_date", cf.getJulianDate(processingDate));
		revalResult.getTable().setColumnValues("trading_date", cf.getJulianDate(tradingDate));
		addFixingDates (session, revalResult, transactions);
	}
	
	private void addForwardRateForContangoBackwardation(Session session,
			RevalResult revalResult, Transactions transactions) {
		Table runtimeTable = revalResult.getTable(); 
		StringBuilder allDealNums = createTranNumList(runtimeTable, "deal_tracking_num");
		StringBuilder sql = new StringBuilder();
		sql.append("\nSELECT settlement_value AS contango_settlement_value")
		   .append("\n ,deal_num AS deal_tracking_num")
		   .append("\n ,spot_equiv_value AS contango_spot_equiv_value")
		   .append("\nFROM USER_jm_jde_extract_data")
		   .append("\nWHERE deal_num IN (" + allDealNums.toString() + ")")
		   ;
		Table rateTable = session.getIOFactory().runSQL(sql.toString());
		runtimeTable.select(rateTable, "contango_settlement_value, contango_spot_equiv_value", "[In.deal_tracking_num] == [Out.deal_tracking_num]");	
	}
	
	private void addForwardRateForContangoBackwardationCorrectingDeals(Session session,
			RevalResult revalResult, Transactions transactions) {
		Table runtimeTable = revalResult.getTable(); 
		StringBuilder allDealNums = createTranNumListForStringColumn(runtimeTable, "correcting_deal");
		if (allDealNums.length() == 0) {
			return;
		}
		StringBuilder sql = new StringBuilder();
		sql.append("\nSELECT settlement_value AS contango_settlement_value_correcting_deal")
		   .append("\n ,CAST(deal_num AS VARCHAR(12)) AS correcting_deal")
		   .append("\n ,spot_equiv_value AS contango_spot_equiv_value_correcting_deal")
		   .append("\nFROM USER_jm_jde_extract_data")
		   .append("\nWHERE deal_num IN (" + allDealNums.toString() + ")")
		   ;
		Table rateTable = session.getIOFactory().runSQL(sql.toString());
		runtimeTable.select(rateTable, "contango_settlement_value_correcting_deal, contango_spot_equiv_value_correcting_deal", "[In.correcting_deal] == [Out.correcting_deal]");	
	}


	private void addFixingDates(Session session, RevalResult revalResult, Transactions transactions) {
		for (int row=revalResult.getTable().getRowCount()-1; row >= 0; row--) {
			int tranNum = revalResult.getTable().getInt("tran_num", row);
			Transaction tran = transactions.getTransactionById(tranNum);
			if (!tran.getValueAsString(EnumTransactionFieldId.InstrumentType).equals("METAL-SWAP")) {
				continue;
			}
			int latestFixingDate = getLatestFixingDate(tran);
			int latestDateUnfixed = getLatestDateUnfixed(tran);
			revalResult.getTable().setInt("latest_fixing_date", row, latestFixingDate);
			revalResult.getTable().setInt("latest_date_unfixed", row, latestDateUnfixed);
		}
	}


	private void joinDocumentInfo(Session session,
			RevalResult revalResult, ConstTable documentInfoTable) {
		Table runtimeTable = revalResult.getTable();
		runtimeTable.select(documentInfoTable, "endur_doc_num, endur_doc_status, jde_doc_num, jde_cancel_doc_num, vat_invoice_doc_num", 
				"[In.tran_num] == [Out.tran_num] AND [In.ins_para_seq_num] == [Out.ins_para_seq_num] AND [In.pymt_type] == [Out.pymt_type]");
	}

	private ConstTable createDocumentInfoTable(Session session, Table runtimeTable) {
		StringBuilder allTranNums = createTranNumList(runtimeTable, "tran_num");
		// assumption: there is only one invoice per deal
		int docTypeInvoiceId = session.getStaticDataFactory().getId(EnumReferenceTable.StldocDocumentType, "Invoice");
		String sql = 
				"\nSELECT DISTINCT h.document_num AS endur_doc_num"
			+ 	"	, h.doc_status AS endur_doc_status"
			+   "	, d.tran_num"
			+ 	" 	, d.ins_para_seq_num"
			+   "   , d.cflow_type AS pymt_type"
			+   "   , ISNULL(j.value, '') AS jde_doc_num"
			+   "   , ISNULL(k.value, '') AS jde_cancel_doc_num"
			+   "   , ISNULL(l.value, '') AS vat_invoice_doc_num"
			+	"\nFROM stldoc_details d"
			+	"\nINNER JOIN stldoc_header h"
			+	"\n ON d.document_num = h.document_num"
			+	"\n    AND d.doc_version = h.doc_version"
			+   "\nLEFT OUTER JOIN stldoc_info j "
			+ 	"\n	ON j.document_num = d.document_num and j.type_id = 20003" // invoices
			+	"\nLEFT OUTER JOIN stldoc_info k "
			// confirmation = cancellation of invoice for credit notes
			+ 	"\n	ON k.document_num = d.document_num and k.type_id = 20007" // confirmation )
			+   "\nLEFT OUTER JOIN stldoc_info l "
			+ 	"\n	ON l.document_num = d.document_num and l.type_id = 20005" // VAT Invoice Doc Num
			+	"\nWHERE d.tran_num IN (" + allTranNums.toString() + ")"
			+	"\n AND h.doc_type = " + docTypeInvoiceId 
			;
		Table docData = session.getIOFactory().runSQL(sql);
		return docData;
	}

	public StringBuilder createTranNumList(Table runtimeTable, 
			String colName) {
		Set<Integer> tranNums = new HashSet<Integer>(runtimeTable.getRowCount());
		for (int tranNum : runtimeTable.getColumnValuesAsInt(colName)) {
			tranNums.add(tranNum);
		}
		StringBuilder allTranNums = new StringBuilder();
		boolean first = true;
		for (int tranNum : tranNums) {
			if (!first) {
				allTranNums.append(",");
			}
			allTranNums.append(tranNum);
			first = false;
		}
		return allTranNums;
	}

	public StringBuilder createTranNumListForStringColumn(Table runtimeTable, 
			String colName) {
		Set<String> tranNums = new HashSet<String>(runtimeTable.getRowCount());
		for (String tranNum : runtimeTable.getColumnValuesAsString(colName)) {
			tranNums.add(tranNum);
		}
		StringBuilder allTranNums = new StringBuilder();
		boolean first = true;
		for (String tranNum : tranNums) {
			if (tranNum == null || tranNum.isEmpty()) {
				continue;
			}
			if (!first) {
				allTranNums.append(",");
			}
			allTranNums.append(tranNum);
			first = false;
		}
		return allTranNums;
	}

	
	private void joinPartyCodes(Session session,
			RevalResult revalResult, Transactions transactions, Table partyInfoTable) {
		Table runtimeTable = revalResult.getTable();
		runtimeTable.select(partyInfoTable, "int_ext->external_party_is_internal,region->region_ext_bu, party_code_uk, party_code_us, party_code_hk, party_code_cn_debtor, party_code_cn_creditor, customer_code_gbp, customer_code_usd, customer_code_eur, customer_code_zar", "[In.party_id] == [Out.ext_bunit_id]");
		runtimeTable.select(partyInfoTable, "region->region_int_bu, int_bu_code", "[In.party_id] == [Out.int_bunit_id]");
		runtimeTable.select(partyInfoTable, "ext_bu_jm_group", "[In.party_id] == [Out.ext_lentity_id]");
	}

	private void applyAllMappings(final Session session,
			final RevalResult revalResult,
			List<RetrievalConfiguration> retrievalConfig) {
		List<RetrievalConfigurationColDescription> mappingTables = retrieveSortedMappingTables();
		// the following lines apply the same mapping algorithm to different mapping tables.
		// all mapping tables are taking into account the current runtime table excluding the output
		// columns of the previous mapping. They are also using instances of RetrievalConfiguration.
		// The RetrievalConfiguration contains different methods retrieving a string denoting the column
		// name of a certain column for each type of mapping table. To ensure that for each mapping table
		// the right get column name method is used, a different instance of "ColNameProvider" is
		// used in a Java 8 Lambda like manner but with the prev Java 8 clumsy way of instancing
		// an anonymous interface.
		
		RuntimeAuditingData runtimeAuditingData = (RuntimeAuditingData) session.getClientData();
		runtimeAuditingData.setRetrievalConfig(retrievalConfig);

		Map<String, MappingTableColumnConfiguration> allColConfigs = new HashMap<>();

		for (final RetrievalConfigurationColDescription table : mappingTables) {
			MappingAuditingData mad = new MappingAuditingData();
			runtimeAuditingData.getMappingAuditingData().put(table, mad);

			Table beforeMapping = revalResult.getTable().cloneData();
			formatColumns(beforeMapping);
			mad.setRuntimeTableBeforeMapping(beforeMapping);
			
			PluginLog.info("Starting of mapping logic (" + table.getMappingTableName() + ")");
			long startMapping = System.currentTimeMillis();
			// In java 8 this should be just rc -> rc.getColNameCustCompTable()
			ColNameProvider colNameProvider = new ColNameProvider() {		
				@Override
				public String getColName(RetrievalConfiguration rc) {
					return rc.getColumnValue(table); 
				}
			};
			mad.setColNameProvider(colNameProvider);
			Map<String, MappingTableColumnConfiguration> tableColConfig = applyMapping(
					table, colNameProvider, 
					session, revalResult, retrievalConfig);
			allColConfigs.putAll(tableColConfig);
			
			Table afterMapping = revalResult.getTable().cloneData();
			formatColumns(afterMapping);
			mad.setRuntimeTableAfterMapping(afterMapping);
			
			long endMappingTime = System.currentTimeMillis();
			PluginLog.info("End of Mapping. computation time " + table.getMappingTableName() + " (ms):  " + (endMappingTime - startMapping));
		}
		
		createOutputTable(revalResult, revalResult.getTable(), retrievalConfig, allColConfigs);
	}

	public List<RetrievalConfigurationColDescription> retrieveSortedMappingTables() {
		List<RetrievalConfigurationColDescription> mappingTables = new ArrayList<>();
		for (RetrievalConfigurationColDescription tc : colLoader.getColDescriptions()) {
			if (tc.getUsageType() == ColumnSemantics.MAPPER_COLUMN) {
				mappingTables.add(tc);				
			}
		}
		Collections.sort(mappingTables, new Comparator<RetrievalConfigurationColDescription>() {
			@Override
			public int compare(RetrievalConfigurationColDescription left,
					RetrievalConfigurationColDescription right) {
				return left.getMappingTableEvaluationOrder() - right.getMappingTableEvaluationOrder();
			}
		});
		return mappingTables;
	}

	/**
	 * Executes the mapping for the provided mapping table.
	 * @param table Contains the name of the user table with the mappings to process
	 * @param colNameProvider The method used to retrieve the name of the column given an instance
	 * of RetrievalConfiguration. RetrievalConfiguration captures the column names for
	 * all mapping tables, so this parameter allows this method to be used in a generic
	 * way over all mapping tables.
	 * @param session
	 * @param revalResult Contains the runtime table.
	 * @param retrievalConfig The parsed retrieval table.
	 * @return
	 */
	private Map<String, MappingTableColumnConfiguration> applyMapping(
			RetrievalConfigurationColDescription table, ColNameProvider colNameProvider,
			final Session session, final RevalResult revalResult,
			List<RetrievalConfiguration> retrievalConfig) {
		Table mappingTable = null;
		mappingTable = retrieveMappingTable(session, table.getMappingTableName());
		Map<String, MappingTableColumnConfiguration> mappingTableColConfig = 
				confirmMappingTableStructure (table.getMappingTableName(), colNameProvider, 
						mappingTable, revalResult.getTable(), retrievalConfig);
		generateUniqueRowIdForTable(mappingTable, true);
		List<MappingTableRowConfiguration> mappingRows = 
				parseMappingTable (colNameProvider, mappingTable, revalResult.getTable(),
						retrievalConfig, mappingTableColConfig);
		createMappingOutputColumnsInRuntimeTable (revalResult.getTable(), mappingTable, mappingTableColConfig);
		executeMapping(colNameProvider, revalResult.getTable(), mappingTable, mappingTableColConfig,
				mappingRows, retrievalConfig);
		return mappingTableColConfig;
	}

	private void applyRetrievalToRuntimeTable(final Session session,
			final Scenario scenario, final RevalResult revalResult,
			final Transactions transactions, final RevalResults prerequisites,
			Map<String, String> parameters,
			List<RetrievalConfiguration> retrievalConfig) {
		Collections.sort(retrievalConfig); // ensure priority based execution
		for (RetrievalConfiguration rc : retrievalConfig) {
			RuntimeTableRetrievalApplicator retrievalApplicator = new RuntimeTableRetrievalApplicator(this, rc, colLoader);
			retrievalApplicator.apply (revalResult.getTable(), session, scenario, prerequisites, transactions, parameters);
		}
	}

	/**
	 * Executes a SQL to retrieve party info fields. Primary key of the
	 * table returned is "party_id". 
	 * Other columns (all of type string):
	 * <ul> 
	 *   <li> ext_bu_jm_group </li>
	 *   <li> party_code_cn_debtor </li>
	 *   <li> party_code_cn_creditor </li>  
	 * </ul>
	 * @param session
	 * @return
	 */
	private Table createPartyInfoTable(Session session) {
		//	TODO: Replace with generic retrieval operator
		String sql = 				
				   "\nSELECT p.party_id"
				+ "\n  ,p.int_ext"
				+ "\n  ,ISNULL(int_bu_code.value, 'undefined') AS int_bu_code"
				+ "\n  ,ISNULL(pi_jm_group.value, pit_jm_group.default_value) AS ext_bu_jm_group"
				+ "\n  ,ISNULL(pi_customer_code_usd_ext.value, ISNULL(pi_customer_code_usd_int.value, '')) AS customer_code_usd"
				+ "\n  ,ISNULL(pi_customer_code_gbp_ext.value, ISNULL(pi_customer_code_gbp_int.value, '')) AS customer_code_gbp"
				+ "\n  ,ISNULL(pi_customer_code_eur_ext.value, ISNULL(pi_customer_code_eur_int.value, '')) AS customer_code_eur"
				+ "\n  ,ISNULL(pi_customer_code_zar_ext.value, ISNULL(pi_customer_code_zar_int.value, '')) AS customer_code_zar"
				+ "\n  ,ISNULL(pi_region_int_bu.value, '') AS region_int_bu"
				+ "\n  ,ISNULL(pi_region_ext_bu.value, '') AS region_ext_bu"
				+ "\n  ,ISNULL(pi_region_ext_bu.value, pi_region_int_bu.value) AS region"
				+ "\n  ,ISNULL(pi_party_code_uk_e.value, pi_party_code_uk_i.value) AS party_code_uk"
				+ "\n  ,ISNULL(pi_party_code_hk_e.value, pi_party_code_hk_i.value) AS party_code_hk"
				+ "\n  ,ISNULL(pi_party_code_us_e.value, pi_party_code_us_i.value) AS party_code_us"
				+ "\n  ,ISNULL(pi_party_code_cn_debtor_e.value, ISNULL(pi_party_code_cn_debtor_i.value, '')) AS party_code_cn_debtor"
				+ "\n  ,ISNULL(pi_party_code_cn_creditor_e.value, ISNULL(pi_party_code_cn_creditor_i.value, '')) AS party_code_cn_creditor"
				+ "\nFROM party p"
				+ "\n  INNER JOIN party_info_types pit_jm_group"
				+ "\n    ON pit_jm_group.type_name = '" + PartyInfoFields.JM_GROUP.getName() + "'"
				
				+ "\n  LEFT OUTER JOIN party_info pi_jm_group"
				+ "\n    ON p.party_id = pi_jm_group.party_id AND pi_jm_group.type_id = pit_jm_group.type_id"
				+ "\n  LEFT OUTER JOIN party_info pi_region_int_bu"
				+ "\n    ON p.party_id = pi_region_int_bu.party_id AND pi_region_int_bu.type_id = "
					+ PartyInfoFields.REGION_INTERNAL.retrieveId(session)
				+ "\n  LEFT OUTER JOIN party_info pi_region_ext_bu"
				+ "\n    ON p.party_id = pi_region_ext_bu.party_id AND pi_region_ext_bu.type_id = "
					+ PartyInfoFields.REGION_EXTERNAL.retrieveId(session)
					
				+ "\n  LEFT OUTER JOIN party_info int_bu_code"
				+ "\n    ON p.party_id = int_bu_code.party_id AND int_bu_code.type_id = "
					+ PartyInfoFields.INT_BUSINESS_UNIT_CODE.retrieveId(session)

					
				+ "\n  LEFT OUTER JOIN party_info pi_party_code_uk_e"
				+ "\n    ON p.party_id = pi_party_code_uk_e.party_id AND pi_party_code_uk_e.type_id = "
					+ PartyInfoFields.PARTY_CODE_UK_EXTERNAL.retrieveId(session)
				+ "\n  LEFT OUTER JOIN party_info pi_party_code_us_e"
				+ "\n    ON p.party_id = pi_party_code_us_e.party_id AND pi_party_code_us_e.type_id = "
					+ PartyInfoFields.PARTY_CODE_US_EXTERNAL.retrieveId(session)
				+ "\n  LEFT OUTER JOIN party_info pi_party_code_hk_e"
				+ "\n    ON p.party_id = pi_party_code_hk_e.party_id AND pi_party_code_hk_e.type_id = "
					+ PartyInfoFields.PARTY_CODE_HK_EXTERNAL.retrieveId(session)					
				+ "\n  LEFT OUTER JOIN party_info pi_party_code_uk_i"
				+ "\n    ON p.party_id = pi_party_code_uk_i.party_id AND pi_party_code_uk_i.type_id = "
					+ PartyInfoFields.PARTY_CODE_UK_INTERNAL.retrieveId(session)
				+ "\n  LEFT OUTER JOIN party_info pi_party_code_us_i"
				+ "\n    ON p.party_id = pi_party_code_us_i.party_id AND pi_party_code_us_i.type_id = "
					+ PartyInfoFields.PARTY_CODE_US_INTERNAL.retrieveId(session)
				+ "\n  LEFT OUTER JOIN party_info pi_party_code_hk_i"
				+ "\n    ON p.party_id = pi_party_code_hk_i.party_id AND pi_party_code_hk_i.type_id = "
					+ PartyInfoFields.PARTY_CODE_HK_INTERNAL.retrieveId(session)
				+ "\n  LEFT OUTER JOIN party_info pi_party_code_cn_debtor_e"
				+ "\n    ON p.party_id = pi_party_code_cn_debtor_e.party_id AND pi_party_code_cn_debtor_e.type_id = "
					+ PartyInfoFields.PARTY_CODE_CN_DEBTOR_EXTERNAL.retrieveId(session)
				+ "\n  LEFT OUTER JOIN party_info pi_party_code_cn_creditor_e"
				+ "\n    ON p.party_id = pi_party_code_cn_creditor_e.party_id AND pi_party_code_cn_creditor_e.type_id = "
					+ PartyInfoFields.PARTY_CODE_CN_CREDITOR_EXTERNAL.retrieveId(session)
				+ "\n  LEFT OUTER JOIN party_info pi_party_code_cn_debtor_i"
				+ "\n    ON p.party_id = pi_party_code_cn_debtor_i.party_id AND pi_party_code_cn_debtor_i.type_id = "
					+ PartyInfoFields.PARTY_CODE_CN_DEBTOR_INTERNAL.retrieveId(session)
				+ "\n  LEFT OUTER JOIN party_info pi_party_code_cn_creditor_i"
				+ "\n    ON p.party_id = pi_party_code_cn_creditor_i.party_id AND pi_party_code_cn_creditor_i.type_id = "
					+ PartyInfoFields.PARTY_CODE_CN_CREDITOR_INTERNAL.retrieveId(session)

				+ "\n  LEFT OUTER JOIN party_info pi_customer_code_usd_ext"
				+ "\n    ON p.party_id = pi_customer_code_usd_ext.party_id AND pi_customer_code_usd_ext.type_id = "
					+ PartyInfoFields.CUSTOMER_CODE_USD_EXT.retrieveId(session)
				+ "\n  LEFT OUTER JOIN party_info pi_customer_code_usd_int"
				+ "\n    ON p.party_id = pi_customer_code_usd_int.party_id AND pi_customer_code_usd_int.type_id = "
					+ PartyInfoFields.CUSTOMER_CODE_USD_INT.retrieveId(session)
				+ "\n  LEFT OUTER JOIN party_info pi_customer_code_gbp_ext"
				+ "\n    ON p.party_id = pi_customer_code_gbp_ext.party_id AND pi_customer_code_gbp_ext.type_id = "
					+ PartyInfoFields.CUSTOMER_CODE_GBP_EXT.retrieveId(session)
				+ "\n  LEFT OUTER JOIN party_info pi_customer_code_gbp_int"
				+ "\n    ON p.party_id = pi_customer_code_gbp_int.party_id AND pi_customer_code_gbp_int.type_id = "
					+ PartyInfoFields.CUSTOMER_CODE_GBP_INT.retrieveId(session)
				+ "\n  LEFT OUTER JOIN party_info pi_customer_code_eur_ext"
				+ "\n    ON p.party_id = pi_customer_code_eur_ext.party_id AND pi_customer_code_eur_ext.type_id = "
					+ PartyInfoFields.CUSTOMER_CODE_EUR_EXT.retrieveId(session)
				+ "\n  LEFT OUTER JOIN party_info pi_customer_code_eur_int"
				+ "\n    ON p.party_id = pi_customer_code_eur_int.party_id AND pi_customer_code_eur_int.type_id = "
					+ PartyInfoFields.CUSTOMER_CODE_EUR_INT.retrieveId(session)
				+ "\n  LEFT OUTER JOIN party_info pi_customer_code_zar_ext"
				+ "\n    ON p.party_id = pi_customer_code_zar_ext.party_id AND pi_customer_code_zar_ext.type_id = "
					+ PartyInfoFields.CUSTOMER_CODE_ZAR_EXT.retrieveId(session)
				+ "\n  LEFT OUTER JOIN party_info pi_customer_code_zar_int"
				+ "\n    ON p.party_id = pi_customer_code_zar_int.party_id AND pi_customer_code_zar_int.type_id = "
					+ PartyInfoFields.CUSTOMER_CODE_ZAR_INT.retrieveId(session)
				;
		session.getDebug().logLine(sql, EnumDebugLevel.High);
		return session.getIOFactory().runSQL(sql);
	}

	/**
	 * Reads out the simulation parameters from this UDSR run. The keys
	 * of the resulting map are stored in a path like string having the pattern
	 * %configuration type%\\%selection%\\name
	 * @param scenario
	 * @return
	 */
	private Map<String, String> generateSimParamTable(final Scenario scenario) {
		Map<String, String> parameters = new HashMap<>();
		for (Configuration config : scenario.getConfigurations()) {
			String configType = config.getConfigurationType().getName();
			String selection = config.getSelection();
			for (ConfigurationField cf : config.getFields()) {
				String name = cf.getName();
				String displayString = cf.getDisplayString();
				String key = configType + "\\" + selection + "\\" + name;
				parameters.put(key, displayString);
			}
		}
		return parameters;
	}

	private void executeMapping(ColNameProvider colNameProvider, Table runtimeTable, Table mappingTable,
			Map<String, MappingTableColumnConfiguration> mappingTableColConfig,
			List<MappingTableRowConfiguration> mappingRows, List<RetrievalConfiguration> retrievalConfig) {
		PluginLog.info("Number of rows in runtime table before mapping: " + runtimeTable.getRowCount());

		Map<String, RetrievalConfiguration> rcByMappingColName = new HashMap<>(retrievalConfig.size()*3);
		for (RetrievalConfiguration rc : retrievalConfig) {
			if (colNameProvider.getColName(rc) != null && colNameProvider.getColName(rc).trim().length() > 0) {
				rcByMappingColName.put(colNameProvider.getColName(rc), rc);
			}
		}
		MappingTableFilterApplicator applicator = 
				new MappingTableFilterApplicator (rcByMappingColName, mappingTableColConfig, colLoader);
		mappingTable.sort(ROW_ID);
		Set<Integer> rowsToIgnore = new TreeSet<>();
		for (int i=runtimeTable.getRowCount()-1; i >= 0; i--) {
			TableRow runtimeTableRow = runtimeTable.getRow(i);
			if (rowsToIgnore.contains(runtimeTableRow.getNumber())) {
				continue; // ignore rows that have been created during manual join
			}
			
			Collection<MappingTableRowConfiguration> matchingRows = applicator.apply(runtimeTable, runtimeTableRow);
			if (matchingRows.size() == 0) {
				runtimeTable.removeRow(runtimeTableRow.getNumber());
				continue;
			}
			boolean first = true;
			for (MappingTableRowConfiguration matchingRow : matchingRows) {
				int rowNum = -1;
				if (!first) {
					TableRow newRow = runtimeTable.addRow();
					newRow.copyData(runtimeTableRow);
					rowNum = newRow.getNumber();
					rowsToIgnore.add(rowNum);
				} else {
					
					rowNum = runtimeTableRow.getNumber();
				}
				int rowMappingTable = 
						mappingTable.findSorted(mappingTable.getColumnId(ROW_ID), matchingRow.getUniqueRowId(), 0);
				copyMappingDataOutputToRuntimeTable (runtimeTable, rowNum, mappingTable, rowMappingTable, mappingTableColConfig);
				first=false;
			}
		}
		PluginLog.info("Number of rows in runtime table after mapping: " + runtimeTable.getRowCount());
	}

	private void copyMappingDataOutputToRuntimeTable(Table runtimeTable,
			int rowNumRuntimeTable, Table mappingTable, int rowNumMappingTable,
			Map<String, MappingTableColumnConfiguration> mappingTableColConfig) {
		for (Entry<String, MappingTableColumnConfiguration>  mtcc : mappingTableColConfig.entrySet()) {
			Object value = mappingTable.getValue(mtcc.getKey(), rowNumMappingTable);
			if (mtcc.getValue().getMappingColType() == MappingConfigurationColType.OUTPUT) {
				if (mtcc.getValue().getColType() == EnumColType.String) {
					String stringValue = (String) value;
					String[] tokens = stringValue.split("\\+");
					StringBuilder sb = new StringBuilder();
					for (String token : tokens) {
						token = token.trim();
						if (token.startsWith("%") &&
								token.endsWith("%")) {
								String srcColName = token.substring(1, token.length()-1);
								sb.append(runtimeTable.getDisplayString(runtimeTable.getColumnId(srcColName), rowNumRuntimeTable));
						} else {
							sb.append(token);
						}
					}
					value = sb.toString();
				}
				runtimeTable.setValue(mtcc.getKey().substring(2), 
					rowNumRuntimeTable, value);
			}
		}
	}

	private void createMappingOutputColumnsInRuntimeTable(
			Table runtimeTable,
			Table mappingTable,
			Map<String, MappingTableColumnConfiguration> mappingTableColConfig) {
		List<String> newColNames = new ArrayList<>();
		List<EnumColType> newColTypes = new ArrayList<>();
		for (Entry<String, MappingTableColumnConfiguration>  mtcc : mappingTableColConfig.entrySet()) {
			if (mtcc.getValue().getMappingColType() == MappingConfigurationColType.OUTPUT) {
				EnumColType colType = mappingTable.getColumnType(mappingTable.getColumnId(mtcc.getKey()));
				newColNames.add(mtcc.getKey().substring(2));
				newColTypes.add(colType);
			}
		}
		runtimeTable.addColumns(newColNames.toArray(new String[newColNames.size()]), 
				newColTypes.toArray(new EnumColType[newColTypes.size()]));
	}

	public static Table retrieveMappingTable(final Session session, String userTableName) {
		Table mappingTable;
		UserTable mappingUserTable = session.getIOFactory().getUserTable(userTableName, false);
		if (mappingUserTable == null) {
			String errorMessage = "The mandatory table " + userTableName + " can't be retrieved. "
					+ ". Please check database access rights, confirm the existence of the table in the database ";
			throw new RuntimeException (errorMessage);
		}
		mappingTable = mappingUserTable.retrieveTable();
		return mappingTable;
	}

	public static List<MappingTableRowConfiguration> parseMappingTable(
			ColNameProvider colNameProvider, Table mappingTable,
			Table runtimeTable, // runtime table is used to determine col types
			List<RetrievalConfiguration> retrievalConfig,
			Map<String, MappingTableColumnConfiguration> mappingTableColConfig) {
		Map<String, RetrievalConfiguration> retrievalConfigByMappingColName = 
				new HashMap<>();
		for (RetrievalConfiguration rc : retrievalConfig) {
			retrievalConfigByMappingColName.put(colNameProvider.getColName(rc), rc);
		}
		List<MappingTableRowConfiguration> parsedRows = new ArrayList<>(mappingTable.getRowCount());
		for (TableRow mappingRow : mappingTable.getRows()) {
			int uniqueRowId = mappingTable.getInt(ROW_ID, mappingRow.getNumber());
			List<MappingTableCellConfiguration> cellConfigurations = new ArrayList<>(mappingTable.getColumnCount());
			for (MappingTableColumnConfiguration colConfig : mappingTableColConfig.values()) {
				// look at columns containing actual mapping logic only:
				if (colConfig.getMappingColType() == MappingConfigurationColType.MAPPING_LOGIC) {
					RetrievalConfiguration rc = retrievalConfigByMappingColName.get(colConfig.getColName());
					String colNameRuntimeTable = rc.getColumnValue(colLoader.getRuntimeDataTable());
					EnumColType colType = runtimeTable.getColumnType(runtimeTable.getColumnId(colNameRuntimeTable));
					String unparsedPredicate = mappingTable.getString(colNameProvider.getColName(rc), mappingRow.getNumber());
					if (unparsedPredicate == null) {
						throw new RuntimeException ("The predicate in column '" + colConfig.getColName() + "' is "
								+ " null for row " + Helper.tableRowToString(mappingTable, mappingRow));
					}
					AbstractPredicate predicate = PredicateParser.parsePredicate (colType, unparsedPredicate);
					MappingTableCellConfiguration cellConfig = new MappingTableCellConfiguration(colConfig, predicate);
					cellConfigurations.add(cellConfig);
				}
			}
			MappingTableRowConfiguration rowConfig = new MappingTableRowConfiguration(colNameProvider,
					uniqueRowId, cellConfigurations, retrievalConfig, colLoader);
			for (MappingTableCellConfiguration cellConfig : cellConfigurations) {
				cellConfig.setRowConfig(rowConfig);
			}
			parsedRows.add(rowConfig);
		}
		return parsedRows;
	}

	public static Map<String, MappingTableColumnConfiguration> confirmMappingTableStructure(String mappingTableName, ColNameProvider colNameProvider, Table mappingTable,
			Table runtimeDataTable,
			List<RetrievalConfiguration> retrievalConfig) {
		StringBuilder errorMessage = new StringBuilder();

		Map<String, RetrievalConfiguration> retrievalConfigByMappingColName = 
				new HashMap<>();
		for (RetrievalConfiguration rc : retrievalConfig) {
			if (colNameProvider.getColName(rc) != null && !colNameProvider.getColName(rc).trim().isEmpty()) {
				retrievalConfigByMappingColName.put(colNameProvider.getColName(rc), rc);				
			}
		}
		Map<String, MappingTableColumnConfiguration> columns = new HashMap<>();
		for (TableColumn tc : mappingTable.getColumns()) {
			boolean startsWith_ = tc.getName().startsWith("o_");
			boolean colNameDefinedInRetrievalTable = retrievalConfigByMappingColName.containsKey(tc.getName());
			RetrievalConfiguration rc = retrievalConfigByMappingColName.get(tc.getName());
			MappingConfigurationColType colType=null;
			if (startsWith_ && !colNameDefinedInRetrievalTable) {
				colType = MappingConfigurationColType.OUTPUT;
			} else if (colNameDefinedInRetrievalTable && tc.getType() != EnumColType.String) {
				errorMessage.append("\n\nThe column " + tc.getName() + " in the mapping table '" 
						+ mappingTableName + "' is of type "
						+ tc.getType() + " but as this column is also defined in the retrieval table '" 
						+ ConfigurationItem.RETRIEVAL_CONFIG_TABLE_NAME.getValue() 
						+ "' so it has to be of type String");
			} else if (colNameDefinedInRetrievalTable) {
				colType = MappingConfigurationColType.MAPPING_LOGIC;
			} else {
				colType = MappingConfigurationColType.UNKNOWN;
			}
			EnumColType targetColType = colType == MappingConfigurationColType.MAPPING_LOGIC?runtimeDataTable.getColumn(rc.getColumnValue(colLoader.getRuntimeDataTable())).getType():null;
			if (colType == MappingConfigurationColType.OUTPUT) {
				targetColType = mappingTable.getColumnType(mappingTable.getColumnId(tc.getName()));
			}
			columns.put(tc.getName(), new MappingTableColumnConfiguration(colType, tc.getName(), targetColType));
		}
		for (RetrievalConfiguration rc : retrievalConfig) {
			if (colNameProvider.getColName(rc) != null && !colNameProvider.getColName(rc).trim().isEmpty()) {
				if (!columns.containsKey(colNameProvider.getColName(rc))) {
					errorMessage.append("\n\nThe column " + colNameProvider.getColName(rc) + " as defined"
							+ " in the retrieval configuration table "
							+ ConfigurationItem.RETRIEVAL_CONFIG_TABLE_NAME.getValue() 
							+ " does not exist in the mapping table "
							+ mappingTableName);
				}
			}
		}
		if (errorMessage.length() > 0) {
			throw new RuntimeException (errorMessage.toString());
		}
		return columns;
	}

	public static void generateUniqueRowIdForTable(Table table, boolean newColumn) {
		if (newColumn) { 
			addUniqueRowIdColumn(table);
		}
		int counter = 1;
		for (TableRow runtimeTableRow : table.getRows()) {
			table.setValue(ROW_ID, runtimeTableRow.getNumber(), counter++);
		}
	}

	private static void addUniqueRowIdColumn(Table table) {
		if (table.isValidColumn(ROW_ID)) {
			String errorMessage = "The row '" + ROW_ID + " does already exist in the table."
					+ " This row is important to contain a unique row number. Can't proceed. "
					+ " Please ensure the table to not contain a row named '"
					+ ROW_ID + "'";
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		table.addColumn(ROW_ID, EnumColType.Int);
	}

	private void createOutputTable(final RevalResult revalResult,
			Table runtimeTable, 
			List<RetrievalConfiguration> retrievalConfig,
			Map<String, MappingTableColumnConfiguration> mappingTableColConfig) {
		Map<String, String> columnsToRetain = new TreeMap<>();
		applyMandatoryColumnsOutputTableRetrieval(revalResult.getTable(), runtimeTable, columnsToRetain);
		StringBuilder columnNames = new StringBuilder();
		for (RetrievalConfiguration rc : retrievalConfig) {
			OutputTableRetrievalApplicator retrievalApplicator = new OutputTableRetrievalApplicator(rc, colLoader);
			retrievalApplicator.apply(revalResult.getTable(), runtimeTable, columnNames, columnsToRetain);
		}
		for (Entry<String, MappingTableColumnConfiguration>  mtcc : mappingTableColConfig.entrySet()) {
			if (mtcc.getValue().getMappingColType() == MappingConfigurationColType.OUTPUT) {
				columnsToRetain.put(mtcc.getKey().substring(2), mtcc.getKey().substring(2));
			}
		}
		for (int i=runtimeTable.getColumnCount()-1; i >= 0; i--) {
			TableColumn rc = runtimeTable.getColumn(i);
			if (columnsToRetain.containsKey(rc.getName())) {
				if (!columnsToRetain.get(rc.getName()).equals(rc.getName())) {
					runtimeTable.setColumnName(rc.getNumber(), columnsToRetain.get(rc.getName()));					
				}
			} else {
				runtimeTable.removeColumn(rc.getNumber());
			}
		}
		revalResult.setTable(runtimeTable);
	}

	private void applyMandatoryColumnsOutputTableRetrieval(Table resultTable,
			Table runtimeTable, Map<String, String> outputColNames) {
		outputColNames.put("event_num", "event_num");
		outputColNames.put("deal_tracking_num", "deal_tracking_num");
		outputColNames.put("tran_num", "tran_num");
		outputColNames.put("ins_num", "ins_num");
		outputColNames.put("ins_para_seq_num", "ins_para_seq_num");
		outputColNames.put("ins_seq_num", "ins_seq_num");
	}

	private List<RetrievalConfiguration> convertRetrievalConfigTableToList(
			final Session session) {
		Table configTable = session.getIOFactory().getUserTable(ConfigurationItem.RETRIEVAL_CONFIG_TABLE_NAME.getValue()).retrieveTable();
		List<RetrievalConfiguration> retrievalConfig=new ArrayList<>(configTable.getRowCount());
		for (TableRow configRow : configTable.getRows()) {
			int priority = configTable.getInt(colLoader.getPriority().getColName(), configRow.getNumber());
			String colNameReportOutput = configTable.getString(colLoader.getReportOutput().getColName(), configRow.getNumber()); // optional value, might be null
			String colNameRuntimeTable = configTable.getString(colLoader.getRuntimeDataTable().getColName(), configRow.getNumber());
			String colNameMappingTable = configTable.getString(colLoader.getMappingTable().getColName(), configRow.getNumber()); // optional value, might be null
			String colNameTaxTable = configTable.getString(colLoader.getTaxTable().getColName(), configRow.getNumber());  // optional value, might be null
			String colNameMaterialNumberTable = configTable.getString(colLoader.getMaterialNumberTable().getColName(), configRow.getNumber()); // optional value, might be null
			String colNameCustCompTable = configTable.getString(colLoader.getCustCompTable().getColName(), configRow.getNumber()); // optional value, might be null
			String retrievalLogic = configTable.getString(colLoader.getRetrievalLogic().getColName(), configRow.getNumber());
			RetrievalConfiguration rc = new RetrievalConfiguration(colLoader);
			rc.setPriority(priority);
			rc.setColumnValue(colLoader.getReportOutput(), colNameReportOutput);
			rc.setColumnValue(colLoader.getRuntimeDataTable(), colNameRuntimeTable);
			rc.setColumnValue(colLoader.getMappingTable(), colNameMappingTable);
			rc.setColumnValue(colLoader.getTaxTable(), colNameTaxTable);
			rc.setColumnValue(colLoader.getMaterialNumberTable(), colNameMaterialNumberTable);
			rc.setColumnValue(colLoader.getCustCompTable(), colNameCustCompTable);
			rc.setColumnValue(colLoader.getRetrievalLogic(), retrievalLogic);
			retrievalConfig.add(rc);
		}
		return retrievalConfig;
	}

	private void showRuntimeDataTable(final ConfigurationItem trigger,
			final Session session, final Table runtimeTable) {
		String showRuntimeDataTable = trigger.getValue();
		if (showRuntimeDataTable != null) {
			try {
				boolean showTable = Boolean.parseBoolean(showRuntimeDataTable);
				if (showTable) {
					session.getDebug().viewTable(runtimeTable.cloneData());					
				}
			} catch (RuntimeException ex) { }
		}
	}

	private JavaTable createEventDataTable(final Transactions transactions, TableFactory tableFactory, RevalResult revalResult) {
		PluginLog.info("Start createEventDataTable");
		JavaTable resultTable = new JavaTable();
		Map<Integer, Integer> tranNumToDealTrackingNum = new TreeMap<>();
		long totalTimeAddRowsAndColumns = 0;
		long totalTimeCreateEventTable = 0;
		boolean first = true;
		for (Transaction tran : transactions) {
			tranNumToDealTrackingNum.put(tran.getTransactionId(), tran.getDealTrackingId());
			long start = System.currentTimeMillis();
			Table dealEventTable = tran.getDealEvents().asTable();
			long end = System.currentTimeMillis();;
			totalTimeCreateEventTable += end - start;
//			revalResult.setTable(dealEventTable.cloneStructure());
			start = System.currentTimeMillis();
			resultTable.mergeAddEndurTable(dealEventTable);
			end = System.currentTimeMillis();
			totalTimeAddRowsAndColumns += end - start;
//			if (first) {
//				revalResult.setTable(dealEventTable.cloneStructure());
//			}
			first = false;
		}
		PluginLog.info("Total time in creating event table(ms): " + totalTimeCreateEventTable);
		PluginLog.info("Total time in add rows and columns(ms): " + totalTimeAddRowsAndColumns);
		resultTable.addColumn("deal_tracking_num", EnumColType.Int);
		for (int row = resultTable.getRowCount()-1; row >= 0; row--) {
			int tranNum = resultTable.getInt("tran_num", row);
			resultTable.setValue ("deal_tracking_num", row, tranNumToDealTrackingNum.get(tranNum));
		}
		PluginLog.info("createEventDataTable finished successfully");
		return resultTable;
	}

	public void finalizeCalculation(final Session session, final Scenario scenario,
			final EnumSplitMethod splitMethod, final RevalResult masterResult) {
		init(session);
		// Empty default implementation
	}

	@Override
	public void format(final Session session, final RevalResult revalResult) {
		init(session);
		PluginLog.info("Starting format of Raw Accounting Data UDSR");
		PluginLog.info("Completed format of Raw Accounting Data UDSR");
	}

	private void formatColumns(final Table result) {
//		revalResult.applyDefaultFormat();
		formatColumn(result, "uom", EnumReferenceTable.IdxUnit);
		formatColumn(result, "buy_sell", EnumReferenceTable.BuySell);
		formatColumn(result, "ins_type", EnumReferenceTable.InsType);
		formatColumn(result, "from_currency", EnumReferenceTable.Currency);
		formatColumn(result, "to_currency", EnumReferenceTable.Currency);
		formatColumn(result, "event_type", EnumReferenceTable.EventType);
		formatColumn(result, "nostro_flag", EnumReferenceTable.NostroFlag);
		formatColumn(result, "int_vostro_flag", EnumReferenceTable.NostroFlag);
		formatColumn(result, "settle_delivery_type", EnumReferenceTable.DeliveryType);
		formatColumn(result, "event_source", EnumReferenceTable.EventSource);
		formatColumn(result, "settle_currency_id", EnumReferenceTable.Currency);
		formatColumn(result, "settle_curr_unit", EnumReferenceTable.UnitDisplay);
		formatColumn(result, "pymt_type", EnumReferenceTable.CflowType);
		formatColumn(result, "internal_contact", EnumReferenceTable.Personnel);
		formatColumn(result, "external_contact", EnumReferenceTable.Personnel);
		formatColumn(result, "int_account_id", EnumReferenceTable.Account);
		formatColumn(result, "ext_account_id", EnumReferenceTable.Account);
		formatColumn(result, "int_settle_id", EnumReferenceTable.SettleInstructions);
		formatColumn(result, "ext_settle_id", EnumReferenceTable.SettleInstructions);
		formatColumn(result, "ext_bunit", EnumReferenceTable.Party);
		formatColumn(result, "ext_lentity_id", EnumReferenceTable.Party);
		formatColumn(result, "delivery_type", EnumReferenceTable.DeliveryType);
		formatColumn(result, "currency", EnumReferenceTable.Currency);
		formatColumn(result, "unit", EnumReferenceTable.UnitDisplay);
	}

	private void formatColumn(Table result, String colName, EnumReferenceTable refTable) {
		int colId = result.getColumnId(colName);
		if (result.isValidColumn(colId) && result.getColumnType(colId) == EnumColType.Int) {
			ColumnFormatterAsRef formatter = result.getFormatter().createColumnFormatterAsRef(refTable);
			if (result.getFormatter().getColumnFormatter(colId) != formatter) {
				result.getFormatter().setColumnFormatter(colId, formatter);				
			}
		}
	}

	public void init(Session session) {
		String abOutdir = session.getSystemSetting("AB_OUTDIR");
		String logLevel = ConfigurationItem.LOG_LEVEL.getValue();
		String logFile = ConfigurationItem.LOG_FILE.getValue();
		String logDir = ConfigurationItem.LOG_DIRECTORY.getValue();
		if (logDir.trim().equals("")) {
			logDir = abOutdir;
		}
		try {
			PluginLog.init(logLevel, logDir, logFile);
		} catch (Exception e) {
			throw new RuntimeException (e);
		}
		PluginLog.info("**********" + this.getClass().getName() + " started **********");
	}

	public ConfigurationItem getTypePrefix() {
		return typePrefix;
	}
	
	private int getLatestFixingDate (Transaction tran) {
		int legCount = tran.getLegCount();
		int latestFixingDate = 0;
		for (int legNo = 0; legNo < legCount; legNo++) {
			Leg leg = tran.getLeg(legNo);
			String fxFloat = leg.getDisplayString(EnumLegFieldId.FixFloat);
			if ("float".equalsIgnoreCase(fxFloat)) {
				int latestFixingDateOnLeg = getLatestFixingDate(leg);
				if (latestFixingDateOnLeg > latestFixingDate) {
					latestFixingDate = latestFixingDateOnLeg;
				}
			}
		}
		if (latestFixingDate == 0) {
			return tran.getValueAsInt(EnumTransactionFieldId.TradeDate);
		}
		
		return latestFixingDate;
	}
	
	private int getLatestFixingDate(Leg leg) {
		int numResets = leg.getResets().getCount();
		int latestFixingDateOnLeg = 0;
		for (int resetNo = 0; resetNo < numResets; resetNo++) {
			int resetValueStatus = leg.getResets().get(resetNo).getValueAsInt(EnumResetFieldId.ValueStatus);
			if (resetValueStatus != EnumValueStatus.Known.getValue()) {
				break;
			}
			latestFixingDateOnLeg = leg.getResets().get(resetNo).getValueAsInt(EnumResetFieldId.Date);
		}
		return latestFixingDateOnLeg;
	}
	
	private int getLatestDateUnfixed (Transaction tran) {
		int legCount = tran.getLegCount();
		int latestUnfixedDate = 0x7FFFFFFF;
		for (int legNo = 0; legNo < legCount; legNo++) {
			Leg leg = tran.getLeg(legNo);
			String fxFloat = leg.getDisplayString(EnumLegFieldId.FixFloat);
			if ("float".equalsIgnoreCase(fxFloat)) {
				int latestDateUnfixedOnLeg = getLatestDateUnfixed(leg);
				if (latestDateUnfixedOnLeg < latestUnfixedDate) {
					latestUnfixedDate = latestDateUnfixedOnLeg;
				}
			}
		}
		if (latestUnfixedDate == 0x7FFFFFFF) {
			return 0;
		}
		
		return latestUnfixedDate;
	}
	
	private int getLatestDateUnfixed(Leg leg) {
		int numResets = leg.getResets().getCount();
		int latestDateUnfixedOnLeg = 0x7FFFFFFF;
		for (int resetNo = numResets-1; resetNo >= 0; resetNo--) {
			int resetValueStatus = leg.getResets().get(resetNo).getValueAsInt(EnumResetFieldId.ValueStatus);
			if (resetValueStatus == EnumValueStatus.Known.getValue()) {
				break;
			}
			latestDateUnfixedOnLeg = leg.getResets().get(resetNo).getValueAsInt(EnumResetFieldId.Date);
		}
		return latestDateUnfixedOnLeg;
	}
}
