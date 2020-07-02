package com.jm.shanghai.accounting.udsr;

import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.concurrent.TimeUnit;

import com.jm.shanghai.accounting.udsr.control.MappingTableFilterApplicator;
import com.jm.shanghai.accounting.udsr.control.OutputTableRetrievalApplicator;
import com.jm.shanghai.accounting.udsr.control.RuntimeTableRetrievalApplicator;
import com.jm.shanghai.accounting.udsr.control.RuntimeTableRetrievalApplicator.RuntimeTableRetrievalApplicatorInput;
import com.jm.shanghai.accounting.udsr.gui.AccountingOperatorsGui;
import com.jm.shanghai.accounting.udsr.model.fixed.ConfigurationItem;
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
import com.olf.embedded.simulation.AbstractSimulationResult2;
import com.olf.embedded.simulation.RevalResult;
import com.olf.openrisk.application.EnumDebugLevel;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.control.EnumSplitMethod;
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
import com.olf.openrisk.trading.EnumInstrumentFieldId;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumResetFieldId;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.EnumValueStatus;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;
import com.olf.jm.logging.Logging;

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
 * 2019-10-04		V1.6		jwaechter		- Refactord party info retrieval to work via 
 * 												  retrival configuration      
 * 2019-10-07		V1.7		jwaechter		- Bugfix in document info handling    
 * 2019-10-29		V1.8		jwaechter		- Now retrieving document data from the *_hist tables
 * 2019-11-11		V1.9		jwaechter		- Moved control if auditing data is collected to 
 *                                                caller of UDSR. Caller now created the instance
 *                                                of RuntimeAuditingData and this class populates
 *                                                it if and only if it can be found in the sessions
 *                                                client data.
 * 2019-11-12		V1.10		jwaechter		- Added retrieval of spot equivalent price and trade price
 *                                                of corrective deal.
 * 2019-12-18		V1.11		jwaechter		- Now using centralized cache instance
 *                                              - removed item_currency field hard coded patch
 *                                                to wire the item_curency output column
 *                                                with an input column of another user table.
 * 2020-01-13		V1.12		jwaechter		- Added default value "Repo" (time swap) for 
 *                                                unclassified swaps
 * 2020-01-24		V1.13		jwaechter		- Added event num as join criteria for the document data                                                
 * 2020-03-05		V1.14		jwaechter		- Modified document retrieval to retrieve data for all
 *                                                document version relevant for JDE instead of the 
 *                                                latest document version only.                                       
 */

/**
 * Main plugin to generate the raw data for the accounting postings 
 * in Shanghai and now also in UK / US / HK.
 * 
 * For a description of the underlying logic refer to the specification.
 * 
 * Please note in addition to the logic to actually generate the accounting data, 
 * there is a another piece of logic to communicate runtime data with the 
 * {@link AccountingOperatorsGui}. The communication between the GUI and this business
 * logic is done the following way:
 * <ul>
 *   <li> The GUI is executing the UDSRs for SL and GL within it's own process  </li>
 *   <li> 
 *     The {@link AbstractShanghaiAccountingUdsr} is filling the sessions client data
 *     object with an instance of {@link RuntimeAuditingData}
 *   </li>
 *   <li>
 *     The GUI is picking up the instance of {@link RuntimeAuditingData} from the 
 *     sessions client data object - this is possible as the GUI runs 
 *     the UDSR internally and not via a remote.
 *   </li>
 * </ul>
 * The {@link RuntimeAuditingData} is basically a protocol containing snapshots of
 * data as seen between the different mapping steps and additional 
 * data used for computation to enable debugging. 
 *  
 * @author jwaechter
 * @version 1.14
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
		
	private static CacheManager cacheManager = new CacheManager();
	
	public static CacheManager getCacheManager() {
		return cacheManager;
	}

	
	protected AbstractShanghaiAccountingUdsr (final ConfigurationItem typePrefix) {
		super();
		this.typePrefix = typePrefix;
	}

	@Override
	public void calculate(final Session session, final Scenario scenario,
			final RevalResult revalResult, final Transactions transactions,
			final RevalResults prerequisites) {
		init(session);
		cacheManager.clearCache();
		cacheManager.init(session);
		// check if the calling plugin has stored an instance of runtimeAuditingData
		// within the sessions client data object. 
		// if no such instance is provided, don't save the runtime auditing data (e.g. in PROD)
				
		RuntimeAuditingData runtimeAuditingData = null;
		if (session.getClientData() != null && session.getClientData() instanceof RuntimeAuditingData) {
			runtimeAuditingData = (RuntimeAuditingData) session.getClientData();
		}
		try {
			long startTime = System.currentTimeMillis();
			Logging.info("Starting calculate of Raw Accounting Data UDSR");
			Map<String, String> parameters = generateSimParamTable(scenario);
			Logging.debug (parameters.toString());
			List<RetrievalConfiguration> retrievalConfig = convertRetrievalConfigTableToList(session);

			// using the JavaTable instead of the Endur memory table to build up the initial 
			// event data, as the event table are dynamic in table structure 
			// (event info fields might be event type specific)
			// to avoid long running memory copy processes.
			JavaTable eventDataTable = createEventDataTable(transactions, session.getTableFactory(), revalResult);
			if (runtimeAuditingData != null) {
				runtimeAuditingData.setRuntimeTable(eventDataTable);
			}
			// creating the final table structure containing all the columns used for 
			// input in the mapping tables as well as all output columns of the mapping table
			// the eventDataTable is used as a base to collect all the column data before actually
			// creating the Endur table.
			finalizeTableStructure(eventDataTable, session, scenario, revalResult, transactions, prerequisites, parameters, retrievalConfig);
			Logging.info("Creation of event data table finished");

			Logging.info("Starting retrieval");
			long startRetrieval = System.currentTimeMillis();
			eventDataTable.mergeIntoEndurTable(revalResult.getTable());
			// apply all currently hard coded data retrieval by executing certain SQLs
			// and add the SQL results to the runtime table.
			retrieveAdditionalData (session, revalResult, transactions);
			if (runtimeAuditingData != null) {
				runtimeAuditingData.setRetrievalConfig(retrievalConfig);				
			}
			// the party info table contains a mapping from 
			// (party, party info type) -> value of the party info field
			// It is used later on while applying the data retrieval
			// The mapping described above makes it very easy to apply the
			// PartyInfo(<info field name>, <join column>) operator.
			Table partyInfoTable = retrievePartyInfoTable (session);
			// apply generic data retrieval according to the configuration in the retrieval table
			applyRetrievalToRuntimeTable(session, scenario, revalResult,
					transactions, prerequisites, parameters, partyInfoTable, retrievalConfig);
			calculateExternalPartyIsJmGroup(session, revalResult, transactions,
					revalResult.getTable());
			addSpotEquivValueForContangoBackwardation(session, revalResult);
			addSpotEquivValueForContangoBackwardationCorrectingDeals(session, revalResult);
			calculateContangoBackwardation(session, revalResult);
			calculateMetalInterest(session, revalResult, transactions);
			long endRetrieval = System.currentTimeMillis();
			Logging.info("Finished retrieval. Computation time (ms): " + (endRetrieval-startRetrieval));
			// Apply hard wired formatting to certain columns to ensure the mapping takes names
			// not IDs
			formatColumns (revalResult.getTable());
			addMoneyDirection (revalResult);
			// generate unique row ids for each existing row in the runtime table before applying mapping
			// this allows later removal of rows from the runtime table that do no match.
			generateUniqueRowIdForTable(revalResult.getTable(), false);
			showRuntimeDataTable(ConfigurationItem.VIEW_RUNTIME_DATA_TABLE_BEFORE_MAPPING, session, revalResult.getTable());
			// Apply mapping to account mapping table, tax code mapping table and material number mapping table
			applyAllMappings(session, revalResult, retrievalConfig, eventDataTable);
			showRuntimeDataTable(ConfigurationItem.VIEW_RUNTIME_DATA_TABLE_AFTER_MAPPING, session, revalResult.getTable());
			// ensures updates based on changes in the const repo desktop are used
			ConfigurationItem.resetValues();
			long endTime = System.currentTimeMillis();
			Logging.info("Execution Time in ms: " + (endTime - startTime));
			Logging.info("Completed calculate of Raw Accounting Data UDSR");
		} catch (Throwable t) {
			Logging.info(t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				Logging.info(ste.toString());
			}
			throw t;
		}finally{
			Logging.close();
		}
	}





	private Table retrievePartyInfoTable(Session session) {
		String sql = 
				"\nSELECT party_id, type_name, value, type_id"
			+	"\nFROM party_info_view";
		session.getDebug().logLine(sql, EnumDebugLevel.High);
		Table countryCodeTable = session.getIOFactory().runSQL(sql);
		return countryCodeTable;
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
				"endur_doc_num", "endur_doc_status", "jde_doc_num", "jde_cancel_doc_num", "vat_invoice_doc_num", "event_present_on_document", "jde_cancel_vat_doc_num", "doc_issue_date",
				"external_party_is_internal", 
				"server_date", "eod_date", "business_date", "processing_date", "trading_date", 
				"latest_fixing_date", "latest_date_unfixed", 
				"contango_settlement_value", "contango_spot_equiv_value", "contango_backwardation", "contango_backwardation_correcting_deal",
				"contango_settlement_value_correcting_deal", "contango_spot_equiv_value_correcting_deal",
				"spot_equiv_price_correcting_deal", "trade_price_correcting_deal",				
				"near_start_date", "far_start_date", "form_near", "form_far", "loco_near", "loco_far", "swap_type",
				"country_code_ext_bu",
				"fx_near_leg_spot_equiv_value", "fx_far_leg_spot_equiv_value", "money_direction",
				// columns for Metal Interest
				"int_trade_type",
				"int_income_expense",
				"int_weight_toz", 
				"int_trade_price_toz",
				"int_accrual_this_month",
				"int_accrual_to_date",
				"int_spot_price_ccy/toz",
				"int_metal_usd_price",
				"int_fx_rate"
				};
		EnumColType colTypes[] = {EnumColType.String, EnumColType.Int, EnumColType.String, 
				EnumColType.Int, EnumColType.Int, EnumColType.String, EnumColType.String, EnumColType.String, EnumColType.String, EnumColType.String, EnumColType.DateTime,
				EnumColType.Int,
				EnumColType.Int, EnumColType.Int, EnumColType.Int, EnumColType.Int, EnumColType.Int,
				EnumColType.Int, EnumColType.Int, 
				EnumColType.Double, EnumColType.Double, EnumColType.String, EnumColType.String,
				EnumColType.Double, EnumColType.Double,
				EnumColType.Double, EnumColType.Double,
				EnumColType.DateTime, EnumColType.DateTime, EnumColType.String, EnumColType.String, EnumColType.String, EnumColType.String, EnumColType.String,
				EnumColType.String,
				EnumColType.Double, EnumColType.Double, EnumColType.String,
				// column types for Metal Interest
				EnumColType.String,
				EnumColType.String,
				EnumColType.Double,
				EnumColType.Double,
				EnumColType.Double,
				EnumColType.Double,
				EnumColType.Double,
				EnumColType.Double,
				EnumColType.Double,
				};
		for (int i=0; i < colNames.length; i++) {
			eventDataTable.addColumn(colNames[i], colTypes[i]);
		}
		// Unique ID column
		if (eventDataTable.isValidColumn(ROW_ID)) {
			String errorMessage = "The row '" + ROW_ID + " does already exist in the table."
					+ " This row is important to contain a unique row number. Can't proceed. "
					+ " Please ensure the table to not contain a row named '"
					+ ROW_ID + "'";
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		eventDataTable.addColumn(ROW_ID, EnumColType.Int);
		// retrieval configuration columns:
		Collections.sort(retrievalConfig); // ensure priority based execution
		for (RetrievalConfiguration rc : retrievalConfig) {
			RuntimeTableRetrievalApplicator retrievalApplicator = new RuntimeTableRetrievalApplicator(this, rc, cacheManager.getColLoader());
			String colName = retrievalApplicator.getColNameRuntimeTable();
			EnumColType colType = retrievalApplicator.getColType(eventDataTable, session, scenario, prerequisites, transactions, parameters, cacheManager);
			eventDataTable.addColumn(colName, colType);
		}
		List<RetrievalConfigurationColDescription> mappingTables = retrieveSortedMappingTables();
		for (final RetrievalConfigurationColDescription table : mappingTables) {
			String mappingTableName = table.getMappingTableName();
			ColNameProvider colNameProvider = new ColNameProvider() {		
				@Override
				public String getColName(RetrievalConfiguration rc) {
					return rc.getColumnValue(table); 
				}
			};
			Table mappingTable = cacheManager.retrieveMappingTable (session, mappingTableName);
			Map<String, MappingTableColumnConfiguration> mappingTableColConfig = confirmMappingTableStructure (mappingTableName,
					colNameProvider, mappingTable,  eventDataTable, retrievalConfig);
			for (Entry<String, MappingTableColumnConfiguration>  mtcc : mappingTableColConfig.entrySet()) {
				if (mtcc.getValue().getMappingColType() == MappingConfigurationColType.OUTPUT) {
					EnumColType colType = mappingTable.getColumnType(mappingTable.getColumnId(mtcc.getKey()));
					if (eventDataTable.isValidColumn(mtcc.getKey().substring(2)) 
							&& eventDataTable.getColumnType(mtcc.getKey().substring(2)) == colType) {
						continue;
					}
					eventDataTable.addColumn(mtcc.getKey().substring(2), colType);
				}
			}			
		}
	}

	private void calculateMetalInterest(Session session, RevalResult revalResult, Transactions transactions) {
		Table runtimeTable = revalResult.getTable();

		StringBuilder allDealNums = createTranNumList(runtimeTable, "deal_tracking_num");
		StringBuilder sql = new StringBuilder();
		sql.append("\nSELECT deal_num, deal_leg, spot_rate")
		   .append("\nFROM USER_jm_pnl_market_data")
		   .append("\nWHERE deal_num IN (" + allDealNums.toString() + ")");
		Table pnlTable = session.getIOFactory().runSQL(sql.toString());
		
		for (int rowId = runtimeTable.getRowCount()-1; rowId >= 0; rowId--) {
			int dealTrackingNum = runtimeTable.getInt("deal_tracking_num", rowId);
			Transaction tran = transactions.getTransaction(dealTrackingNum);

			int pymtType = runtimeTable.getInt("pymt_type", rowId);
			String tradeType = (pymtType == 37 || pymtType == 113 || pymtType == 114) ? "Swap" : "Trade";
			runtimeTable.setString("int_trade_type", rowId, tradeType);
			
			double settlementValue = Math.abs(runtimeTable.getDouble("contango_settlement_value", rowId));
			double spotEquivValue = Math.abs(runtimeTable.getDouble("contango_spot_equiv_value", rowId));
			int buySell = runtimeTable.getInt("buy_sell", rowId);
			String incomeExpense = (buySell == 0 && settlementValue <= spotEquivValue) || (buySell == 1 && settlementValue > spotEquivValue) ? "Income" : "Expense";
			runtimeTable.setString("int_income_expense", rowId, incomeExpense);
			
			double position = tran.getValueAsDouble(EnumTransactionFieldId.Position);
			String toolset = tran.getValueAsString(EnumTransactionFieldId.Toolset);
			double weight = toolset.equals("ComFut") ? position * tran.getLeg(0).getValueAsDouble(EnumLegFieldId.Notional) : position;
			runtimeTable.setDouble("int_weight_toz", rowId, weight);
			
			double price = tran.getValueAsDouble(EnumTransactionFieldId.Price);
			runtimeTable.setDouble("int_trade_price_toz", rowId, price);
			
			double metalUsdPrice = pnlTable.findRowId("deal_leg == 0 AND deal_num == " + dealTrackingNum, 0);
			double fxRate = pnlTable.findRowId("deal_leg == 1 AND deal_num == " + dealTrackingNum, 0);
			double spotPrice = metalUsdPrice / fxRate;
			runtimeTable.setDouble("int_metal_usd_price", rowId, metalUsdPrice);
			runtimeTable.setDouble("int_fx_rate", rowId, fxRate);
			runtimeTable.setDouble("int_spot_price_ccy/toz", rowId, spotPrice);
			
			double accrual = settlementValue - spotEquivValue;
			Date settleDate = tran.getValueAsDate(EnumTransactionFieldId.SettleDate);
			Date tradeDate = tran.getValueAsDate(EnumTransactionFieldId.TradeDate);
			Date lastDayOfPrevMonth = session.getCalendarFactory().createSymbolicDate("-1lom").evaluate();
			Date firstDayOfPrevMonth = session.getCalendarFactory().createSymbolicDate("-2fom>5cd").evaluate();
			double accrualToDate = accrual / dayDiff(tradeDate, settleDate) * 
					dayDiff(tradeDate, (settleDate.before(lastDayOfPrevMonth) ? settleDate : lastDayOfPrevMonth));
			runtimeTable.setDouble("int_accrual_to_date", rowId, accrualToDate);
			double accrualThisMonth = accrual / dayDiff(tradeDate, settleDate) * 
					dayDiff(tradeDate.after(lastDayOfPrevMonth) ? tradeDate : firstDayOfPrevMonth, 
							settleDate.before(lastDayOfPrevMonth) ? settleDate : lastDayOfPrevMonth);
			runtimeTable.setDouble("int_accrual_this_month", rowId, accrualThisMonth);
		}

		pnlTable.dispose();
	}
	
	private long dayDiff(Date startDate, Date endDate) {
	    return TimeUnit.DAYS.convert(endDate.getTime() - startDate.getTime(), TimeUnit.MILLISECONDS);
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
				double settlementValue = runtimeTable.getDouble(colNameSettlementValue, rowId);
				double spotEquivValue = runtimeTable.getDouble(colNameSpotEquivValue, rowId);
				if (spotEquivValue < settlementValue) {
					runtimeTable.setString("contango_backwardation_correcting_deal", rowId, "C");
				} else {
					runtimeTable.setString("contango_backwardation_correcting_deal", rowId, "B");
				}
			} 
			colNameSettlementValue = "contango_settlement_value";
			colNameSpotEquivValue = "contango_spot_equiv_value";
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
		if (typePrefix.getValue().equalsIgnoreCase("SL")) {
			ConstTable documentInfoTable = createDocumentInfoTable (session, runtimeTable);
			runtimeTable.select(documentInfoTable, "endur_doc_num, endur_doc_status, jde_doc_num, jde_cancel_doc_num, vat_invoice_doc_num, jde_cancel_vat_doc_num, doc_issue_date", 
					"[In.deal_tracking_num] == [Out.deal_tracking_num] AND [In.ins_para_seq_num] == [Out.ins_para_seq_num] AND [In.pymt_type] == [Out.pymt_type] AND [In.event_num] == [Out.event_num]");

			joinDocumentEventPresence(session, runtimeTable);			
		}
		
		ConstTable swapInfoTable = createSwapInfoTable (session, runtimeTable, transactions);
		runtimeTable.select(swapInfoTable, "swap_type,near_start_date,far_start_date,form_near,form_far,loco_near,loco_far", "[In.tran_num] == [Out.tran_num]");

		addDates(session, revalResult, transactions);
		addCountryCodeExternalBu(session, runtimeTable, transactions);
	}

	/**
	 * Calculates if a certain event is present on the document identified by its endur_doc_num
	 * and stores the result in column "event_present_on_document" ("Yes" or "No").
	 * @param session
	 * @param runtimeTable
	 */
	private void joinDocumentEventPresence(Session session,
			Table runtimeTable) {
		StringBuilder alleventNums = createEventNumList(runtimeTable, "event_num");
		// Retrieves if an event is present on the invoice
		int docTypeInvoiceId = session.getStaticDataFactory().getId(EnumReferenceTable.StldocDocumentType, "Invoice");
		int docStatusSentToCpId = session.getStaticDataFactory().getId(EnumReferenceTable.StldocDocumentStatus, "2 Sent to CP");
		int docStatusReceivedId = session.getStaticDataFactory().getId(EnumReferenceTable.StldocDocumentStatus, "2 Received");
		int docStatusCancelled = session.getStaticDataFactory().getId(EnumReferenceTable.StldocDocumentStatus, "Cancelled");
		runtimeTable.setColumnValues("event_present_on_document", "No");
		
		String sql = 
				"\nSELECT DISTINCT "
			+   "\n  d.event_num event_num"
			+   "\n, d.document_num AS endur_doc_num"
			+   "\n, 'Yes' AS event_present_on_document"
			+	"\nFROM stldoc_details_hist d"
			+	"\nINNER JOIN stldoc_header_hist h"
			+	"\n ON d.document_num = h.document_num"
			+	"\n    AND d.doc_version = h.doc_version"
			+	"\nWHERE d.event_num IN (" + alleventNums.toString() + ")"
			+	"\n AND h.doc_type = " + docTypeInvoiceId 
			+   "\n AND h.doc_status IN (" + docStatusCancelled + ", " + docStatusReceivedId + ", " + docStatusSentToCpId + ")"

			;
		
		session.getDebug().logLine(sql, EnumDebugLevel.High);
		Table eventPresenceTable = session.getIOFactory().runSQL(sql);
		runtimeTable.select(eventPresenceTable, "event_present_on_document", "[In.event_num] == [Out.event_num] AND [In.endur_doc_num] == [Out.endur_doc_num]");
	}

	
	
	/**
	 * Calculates the values of the column "external_party_is_jm_group" based on the values within the columns 
	 * "external_party_is_internal", "ext_bu_jm_group" and "int_bunit_id".
	 * @param session
	 * @param revalResult
	 * @param transactions
	 * @param runtimeTable
	 */
	private void calculateExternalPartyIsJmGroup(Session session,
			RevalResult revalResult, Transactions transactions,
			Table runtimeTable) {
		formatColumn (runtimeTable, "external_party_is_internal", EnumReferenceTable.InternalExternal);
		formatColumn (runtimeTable, "int_bunit_id", EnumReferenceTable.Party);
		Table additionalPartyData = retrieveAdditionalPartyData(session);
		runtimeTable.select(additionalPartyData, "int_ext->external_party_is_internal", "[In.party_id] == [Out.ext_bunit_id]");

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
	}

	private void addCountryCodeExternalBu(Session session,
			Table runtimeTable, Transactions transactions) {
		StringBuilder allTranNums = createTranNumList(runtimeTable, "tran_num");
		// Retrieves the country code of the main address of the external business
		// unit as designated in the transactions being processed.
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

	/**
	 * Retrieves the values of the columns "near_start_date", "far_start_date", "form_near",
	 * "form_far", "loco_near", "loco_far" and filling the value of column "swap_type" 
	 * based on the values of the columns mentioned before.
	 * The swap type determination logic is the following.
	 * <ol>
	 *   <li> 'Repo' is assigned if the near_start_date is before the far_start_date. </li>
	 *   <li> 'Form' is assigned if form_near and form_far are different and 'Repo' did not get assigned</li>
	 *   <li> 'Loco' is assigned if loco_near and loco_far are different and neither 'Repo' nor 'Form' got assigned </li>
	 *   <li> 'Repo' is assigned as a default value if neither one of the above are assigned. </li>
	 * </ol>
	 * @param session
	 * @param runtimeTable
	 * @param transactions
	 * @return
	 */
	private ConstTable createSwapInfoTable(Session session, Table runtimeTable,
			Transactions transactions) {
		StringBuilder allTranNums = createTranNumList(runtimeTable, "tran_num");
		// assumption: there is only one invoice per deal
		String sql = 
				"\nSELECT DISTINCT ab.tran_num"
			+	"\n,  fx_aux_data.spot_date AS near_start_date"
			+	"\n,  fx_aux_data.far_date AS far_start_date"
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
			+   "\nINNER JOIN fx_tran_aux_data fx_aux_data "
			+   "\n ON fx_aux_data.tran_num = ab_near.tran_num"
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
		swapData.convertColumns(
				Arrays.asList("near_start_date", "far_start_date").toArray(new String[1]), 
				Arrays.asList(EnumColType.DateTime, EnumColType.DateTime).toArray(new EnumColType[1]));
		
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
			} else { // default = time swap -> repo
				swapData.setString("swap_type", row, "Repo");				
			}
		}
		return swapData;
	}

	/**
	 * Fills the values for the columns "server_date", "eod_date", "business_date", "processing_date",
	 * "trading_date", "latest_fixing_date", and "latest_date_unfixed".
	 * @param session
	 * @param revalResult
	 * @param transactions
	 */
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
	
	/**
	 * This method requires the following columns to be retrieved as configured in the 
	 * USER_jm_acc_retrieval_config table:
	 * <ul>
	 *   <li> ins_sub_type </li>
	 *   <li> buy_sell </li>
	 *   <li> spot_equiv_value </li>
	 *   <li> spot_equiv_price </li>
	 *   <li> tran_group </li>s
	 * </ul>
	 * In addition, the following columns have to be retrieved via hard coded logic before
	 * running the method:
	 * <ul>
	 *   <li> fx_near_leg_spot_equiv_value </li>
	 *   <li> fx_far_leg_spot_equiv_value </li>
	 * </ul>
	 * The method is going to fill the "money_direction" column
	 * where appropriate.
	 * @param revalResult
	 */
	private void addMoneyDirection(RevalResult revalResult) {
		final Map<Integer, List<Integer>> fxFarRowsByTranGroup = new HashMap<>();
		final Map<Integer, List<Integer>> fxNearRowsByTranGroup = new HashMap<>();
		Table runtimeTable = revalResult.getTable();
		int insTypeColId = runtimeTable.getColumnId("ins_sub_type");
		int buySellColId = runtimeTable.getColumnId("buy_sell");
		
		for (int row=runtimeTable.getRowCount()-1; row >= 0; row--) {
			String insSubType = runtimeTable.getDisplayString(insTypeColId, row);
			if (insSubType.equalsIgnoreCase("FX-NEARLEG")) {
				String tranGroupAsString = runtimeTable.getString("tran_group", row);
				int tranGroup = Integer.parseInt(tranGroupAsString.trim());
				List<Integer> nearRows = fxNearRowsByTranGroup.get(tranGroup);
				if (nearRows == null) {
					nearRows = new ArrayList<Integer>();
					fxNearRowsByTranGroup.put(tranGroup, nearRows);
				}
				nearRows.add(row);
				double spotEquivValueNear = Math.abs(runtimeTable.getDouble("spot_equiv_value", row));
				runtimeTable.setDouble("fx_near_leg_spot_equiv_value", row, spotEquivValueNear);
				if (fxFarRowsByTranGroup.containsKey(tranGroup)) {
					for (int rowFarLeg : fxFarRowsByTranGroup.get(tranGroup)) {
						runtimeTable.setDouble("fx_near_leg_spot_equiv_value", rowFarLeg, spotEquivValueNear);
						double spotEquivValueFar = runtimeTable.getDouble("spot_equiv_value", rowFarLeg);
						runtimeTable.setDouble("fx_far_leg_spot_equiv_value", row, spotEquivValueFar);
						String buySell = runtimeTable.getDisplayString(buySellColId, rowFarLeg);
						if (buySell.equalsIgnoreCase("BUY")) {
							if (spotEquivValueFar <= spotEquivValueNear) {
								runtimeTable.setString("money_direction", row, "ITM");
								runtimeTable.setString("money_direction", rowFarLeg, "ITM");
							} else {
								runtimeTable.setString("money_direction", row, "OTM");
								runtimeTable.setString("money_direction", rowFarLeg, "OTM");							
							}
						} else {
							if (spotEquivValueFar > spotEquivValueNear) {
								runtimeTable.setString("money_direction", row, "OTM");
								runtimeTable.setString("money_direction", rowFarLeg, "OTM");
							} else {
								runtimeTable.setString("money_direction", row, "ITM");
								runtimeTable.setString("money_direction", rowFarLeg, "ITM");							
							}						
						}						
					}
				}
			} else if (insSubType.equalsIgnoreCase("FX-FARLEG")) {
				String tranGroupAsString = runtimeTable.getString("tran_group", row);
				int tranGroup = Integer.parseInt(tranGroupAsString.trim());
				List<Integer> farRows = fxFarRowsByTranGroup.get(tranGroup);
				if (farRows == null) {
					farRows = new ArrayList<Integer>();
					fxFarRowsByTranGroup.put(tranGroup, farRows);
				}
				farRows.add(row);
				double spotEquivValueFar = Math.abs(runtimeTable.getDouble("spot_equiv_value", row));
				runtimeTable.setDouble("fx_far_leg_spot_equiv_value", row, spotEquivValueFar);
				if (fxNearRowsByTranGroup.containsKey(tranGroup)) {
					for (int rowNearLeg : fxNearRowsByTranGroup.get(tranGroup)) {
						runtimeTable.setDouble("fx_far_leg_spot_equiv_value", rowNearLeg, spotEquivValueFar);
						double spotEquivValueNear = runtimeTable.getDouble("spot_equiv_value", rowNearLeg);
						runtimeTable.setDouble("fx_near_leg_spot_equiv_value", row, spotEquivValueNear);
						String buySell = runtimeTable.getDisplayString(buySellColId, row);
						if (buySell.equalsIgnoreCase("BUY")) {
							if (spotEquivValueFar <= spotEquivValueNear) {
								runtimeTable.setString("money_direction", row, "ITM");
								runtimeTable.setString("money_direction", rowNearLeg, "ITM");
							} else {
								runtimeTable.setString("money_direction", row, "OTM");
								runtimeTable.setString("money_direction", rowNearLeg, "OTM");							
							}
						} else {
							if (spotEquivValueFar > spotEquivValueNear) {
								runtimeTable.setString("money_direction", row, "OTM");
								runtimeTable.setString("money_direction", rowNearLeg, "OTM");
							} else {
								runtimeTable.setString("money_direction", row, "ITM");
								runtimeTable.setString("money_direction", rowNearLeg, "ITM");							
							}						
						}						
					}
				}
			}			
		}
	}
	
	/**
	 * Queries the USER_jm_jde_extract_data to retrieve data for the 
	 * deals identified by column "deal_tracking_num".
	 * @param session the session to use to query the user table.
	 * @param revalResult The reval result containing the runtime table.
	 */
	private void addSpotEquivValueForContangoBackwardation(Session session,
			RevalResult revalResult) {
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
	
	/**
	 * Queries the USER_jm_jde_extract_data to retrieve data for the 
	 * deals identified by column "correcting_deal".
	 * Requires the column "Correcting Deal" to be present and previously
	 * retrieved as configured in the USER_jm_acc_retrieval_config table.
	 * @param session the session to use to query the user table.
	 * @param revalResult The reval result containing the runtime table.
	 */
	private void addSpotEquivValueForContangoBackwardationCorrectingDeals(Session session,
			RevalResult revalResult) {
		Table runtimeTable = revalResult.getTable(); 
		StringBuilder allDealNums = createTranNumListForStringColumn(runtimeTable, "correcting_deal");
		if (allDealNums.length() == 0) {
			return;
		}
		StringBuilder sql = new StringBuilder();
		sql.append("\nSELECT settlement_value AS contango_settlement_value_correcting_deal")
		   .append("\n ,CAST(deal_num AS VARCHAR(12)) AS correcting_deal")
		   .append("\n ,spot_equiv_value AS contango_spot_equiv_value_correcting_deal")
		   .append("\n ,spot_equiv_price AS spot_equiv_price_correcting_deal")
		   .append("\n ,trade_price AS trade_price_correcting_deal")
		   .append("\nFROM USER_jm_jde_extract_data")
		   .append("\nWHERE deal_num IN (" + allDealNums.toString() + ")")
		   ;
		Table rateTable = session.getIOFactory().runSQL(sql.toString());
		runtimeTable.select(rateTable, "contango_settlement_value_correcting_deal, contango_spot_equiv_value_correcting_deal, spot_equiv_price_correcting_deal, trade_price_correcting_deal",
				"[In.correcting_deal] == [Out.correcting_deal]");	
	}

	/**
	 * Fills the values of the columns "latest_fixing_date" and "latest_date_unfixed".
	 * @param session
	 * @param revalResult
	 * @param transactions
	 */
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
	
	private ConstTable createDocumentInfoTable(Session session, Table runtimeTable) {
		StringBuilder allDealTrackingNums = createTranNumList(runtimeTable, "deal_tracking_num");
		// assumption: there is only one invoice per deal
		int docTypeInvoiceId = session.getStaticDataFactory().getId(EnumReferenceTable.StldocDocumentType, "Invoice");
		int docStatusSentToCpId = session.getStaticDataFactory().getId(EnumReferenceTable.StldocDocumentStatus, "2 Sent to CP");
		int docStatusReceivedId = session.getStaticDataFactory().getId(EnumReferenceTable.StldocDocumentStatus, "2 Received");
		int docStatusCancelled = session.getStaticDataFactory().getId(EnumReferenceTable.StldocDocumentStatus, "Cancelled");
		String sql = 
				"\nSELECT DISTINCT h.document_num AS endur_doc_num"
			+ 	"	, h.doc_status AS endur_doc_status"
			+   "	, d.deal_tracking_num"
			+ 	" 	, d.ins_para_seq_num"
			+   "   , d.cflow_type AS pymt_type"
			+   "   , d.event_num"
			+   "   , h.doc_issue_date"
			+   "   , ISNULL(j.value, '') AS jde_doc_num"
			+   "   , ISNULL(k.value, '') AS jde_cancel_doc_num"
			+   "   , ISNULL(l.value, '') AS vat_invoice_doc_num"
			+   "   , ISNULL(m.value, '') AS jde_cancel_vat_doc_num"
			+	"\nFROM stldoc_details_hist d"
			+	"\nINNER JOIN stldoc_header_hist h"
			+	"\n ON d.document_num = h.document_num"
			+	"\n    AND d.doc_version = h.doc_version"
			+   "\nLEFT OUTER JOIN stldoc_info_h j "
			+ 	"\n	ON j.document_num = d.document_num and j.type_id = 20003" // invoices
			+   "\n   AND h.stldoc_hdr_hist_id = j.stldoc_hdr_hist_id"
			+	"\nLEFT OUTER JOIN stldoc_info_h k "
			// confirmation = cancellation of invoice for credit notes
			+ 	"\n	ON k.document_num = d.document_num and k.type_id = 20007" // confirmation / cancellation of invoice
			+   "\n   AND h.stldoc_hdr_hist_id = k.stldoc_hdr_hist_id"
			+   "\nLEFT OUTER JOIN stldoc_info_h l "
			+ 	"\n	ON l.document_num = d.document_num and l.type_id = 20005" // VAT Invoice Doc Num
			+   "\n   AND h.stldoc_hdr_hist_id = l.stldoc_hdr_hist_id"
			+   "\nLEFT OUTER JOIN stldoc_info_h m "
			+ 	"\n	ON m.document_num = d.document_num and m.type_id = 20008" // VAT Cancel Doc Num
			+   "\n   AND h.stldoc_hdr_hist_id = m.stldoc_hdr_hist_id"
			+	"\nWHERE d.deal_tracking_num IN (" + allDealTrackingNums.toString() + ")"
			+	"\n AND h.doc_type = " + docTypeInvoiceId 
			+   "\n AND h.doc_status IN (" + docStatusCancelled + ", " + docStatusReceivedId + ", " + docStatusSentToCpId + ")"
			;
		Table docData = session.getIOFactory().runSQL(sql);
		return docData;
	}
	
	/**
	 * Creates a comma separated list of all event nums used within the runtime table.
	 * @param runtimeTable
	 * @param colName column name of the column containing the event num. Has to be of type Long
	 * @return
	 */
	private StringBuilder createEventNumList(Table runtimeTable, 
			String colName) {
		Set<Long> eventNums = new HashSet<Long>(runtimeTable.getRowCount());
		for (Long eventNum : runtimeTable.getColumnValuesAsLong(colName)) {
			eventNums.add(eventNum);
		}
		StringBuilder allEventNums = new StringBuilder();
		boolean first = true;
		for (Long tranNum : eventNums) {
			if (!first) {
				allEventNums.append(",");
			}
			allEventNums.append(tranNum);
			first = false;
		}
		return allEventNums;
	}
	
	/**
	 * Creates a comma separated list of all tran nums used within the runtime table.
	 * @param runtimeTable
	 * @param colName the column name containing the tran num. Has to be of type Int
	 * @return
	 */
	private StringBuilder createTranNumList(Table runtimeTable, 
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
	
	/**
	 * Creates a comma separated list of all tran nums used within the runtime table.
	 * @param runtimeTable
	 * @param colName the column name containing the tran num. Has to be of type String
	 * @return
	 */
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

	private void applyAllMappings(final Session session,
			final RevalResult revalResult,
			List<RetrievalConfiguration> retrievalConfig,
			JavaTable runtimeTable) {
		List<RetrievalConfigurationColDescription> mappingTables = retrieveSortedMappingTables();
		// the following lines apply the same mapping algorithm to different mapping tables.
		// all mapping tables are taking into account the current runtime table excluding the output
		// columns of the previous mapping. They are also using instances of RetrievalConfiguration.
		// The RetrievalConfiguration contains different methods retrieving a string denoting the column
		// name of a certain column for each type of mapping table. To ensure that for each mapping table
		// the right get column name method is used, a different instance of "ColNameProvider" is
		// used in a Java 8 Lambda like manner but with the prev Java 8 clumsy way of instancing
		// an anonymous interface.
		
		RuntimeAuditingData runtimeAuditingData = null;
		if (session.getClientData() != null && session.getClientData() instanceof RuntimeAuditingData) {
			runtimeAuditingData = (RuntimeAuditingData) session.getClientData();
		}
		if (runtimeAuditingData != null) {
			runtimeAuditingData.setRetrievalConfig(retrievalConfig);			
		}

		Map<String, MappingTableColumnConfiguration> allColConfigs = new HashMap<>();

		for (final RetrievalConfigurationColDescription table : mappingTables) {
			MappingAuditingData mad = new MappingAuditingData();
			if (runtimeAuditingData != null) {
				runtimeAuditingData.getMappingAuditingData().put(table, mad);
			}
			Table beforeMapping = revalResult.getTable().cloneData();
			formatColumns(beforeMapping);
			mad.setRuntimeTableBeforeMapping(beforeMapping);
			
			Logging.info("Starting of mapping logic (" + table.getMappingTableName() + ")");
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
					session, revalResult, retrievalConfig,
					runtimeTable);
			allColConfigs.putAll(tableColConfig);
			
			Table afterMapping = revalResult.getTable().cloneData();
			formatColumns(afterMapping);
			mad.setRuntimeTableAfterMapping(afterMapping);
			
			long endMappingTime = System.currentTimeMillis();
			Logging.info("End of Mapping. computation time " + table.getMappingTableName() + " (ms):  " + (endMappingTime - startMapping));
		}
		
		createOutputTable(revalResult, revalResult.getTable(), retrievalConfig, allColConfigs);
	}

	public List<RetrievalConfigurationColDescription> retrieveSortedMappingTables() {
		List<RetrievalConfigurationColDescription> mappingTables = new ArrayList<>();
		for (RetrievalConfigurationColDescription tc : cacheManager.getColLoader().getColDescriptions()) {
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
			List<RetrievalConfiguration> retrievalConfig,
			JavaTable runtimeTable) {
		Table mappingTable = null;
		mappingTable = cacheManager.retrieveMappingTable(session, table.getMappingTableName());
		Map<String, MappingTableColumnConfiguration> mappingTableColConfig = 
				confirmMappingTableStructure (table.getMappingTableName(), colNameProvider, 
						mappingTable, runtimeTable, retrievalConfig);
		generateUniqueRowIdForTable(mappingTable, true);
		List<MappingTableRowConfiguration> mappingRows = 
				parseMappingTable (colNameProvider, mappingTable, revalResult.getTable(),
						retrievalConfig, mappingTableColConfig);
		executeMapping(colNameProvider, revalResult.getTable(), mappingTable, mappingTableColConfig,
				mappingRows, retrievalConfig);
		return mappingTableColConfig;
	}

	private void applyRetrievalToRuntimeTable(final Session session,
			final Scenario scenario, final RevalResult revalResult,
			final Transactions transactions, final RevalResults prerequisites,
			final Map<String, String> parameters,
			final Table partyInfoTable,
			final List<RetrievalConfiguration> retrievalConfig) {
		Collections.sort(retrievalConfig); // ensure priority based execution
		RuntimeTableRetrievalApplicator.RuntimeTableRetrievalApplicatorInput retrievalInput = 
				new RuntimeTableRetrievalApplicatorInput(revalResult.getTable(), session, scenario,
						prerequisites, transactions, parameters, partyInfoTable, getCacheManager());
		for (RetrievalConfiguration rc : retrievalConfig) {
			RuntimeTableRetrievalApplicator retrievalApplicator = new RuntimeTableRetrievalApplicator(this, rc, cacheManager.getColLoader());
			retrievalApplicator.apply (retrievalInput);
			retrievalApplicator.applyDefaultFormatting(retrievalInput); 
		}

	}

	/**
	 * Executes a SQL to retrieve party info fields. Primary key of the
	 * table returned is "party_id". Currently the only data retrieved is the int_ext flag.
	 * @param session
	 * @return
	 */
	private Table retrieveAdditionalPartyData(Session session) {
		//	TODO: Replace with generic retrieval operator
		String sql = 				
				   "\nSELECT p.party_id"
				+ "\n  ,p.int_ext"
				+ "\nFROM party p"
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
		Logging.info("Number of rows in runtime table before mapping: " + runtimeTable.getRowCount());

		Map<String, RetrievalConfiguration> rcByMappingColName = new HashMap<>(retrievalConfig.size()*3);
		for (RetrievalConfiguration rc : retrievalConfig) {
			if (colNameProvider.getColName(rc) != null && colNameProvider.getColName(rc).trim().length() > 0) {
				rcByMappingColName.put(colNameProvider.getColName(rc), rc);
			}
		}
		MappingTableFilterApplicator applicator = 
				new MappingTableFilterApplicator (rcByMappingColName, mappingTableColConfig, cacheManager.getColLoader());
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
		Logging.info("Number of rows in runtime table after mapping: " + runtimeTable.getRowCount());
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
					String colNameRuntimeTable = rc.getColumnValue(cacheManager.getColLoader().getRuntimeDataTable());
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
					uniqueRowId, cellConfigurations, retrievalConfig, cacheManager.getColLoader());
			for (MappingTableCellConfiguration cellConfig : cellConfigurations) {
				cellConfig.setRowConfig(rowConfig);
			}
			parsedRows.add(rowConfig);
		}
		return parsedRows;
	}

	public static Map<String, MappingTableColumnConfiguration> confirmMappingTableStructure(String mappingTableName, ColNameProvider colNameProvider, Table mappingTable,
			JavaTable runtimeDataTable,
			List<RetrievalConfiguration> retrievalConfig) {
		Map<String, MappingTableColumnConfiguration> columns = cacheManager.getMappingTableDescription(mappingTableName);
		if (columns != null) {
			return columns;
		}		
		
		columns = new HashMap<>();
		StringBuilder errorMessage = new StringBuilder();
		Map<String, RetrievalConfiguration> retrievalConfigByMappingColName = 
				new HashMap<>();
		for (RetrievalConfiguration rc : retrievalConfig) {
			if (colNameProvider.getColName(rc) != null && !colNameProvider.getColName(rc).trim().isEmpty()) {
				retrievalConfigByMappingColName.put(colNameProvider.getColName(rc), rc);				
			}
		}
		
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
			EnumColType targetColType = colType == MappingConfigurationColType.MAPPING_LOGIC?runtimeDataTable.getColumnType(rc.getColumnValue(cacheManager.getColLoader().getRuntimeDataTable())):null;
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
			String warningMessage = "The row '" + ROW_ID + " does already exist in the table."
					+ " This row is important to contain a unique row number. Can't proceed. "
					+ " Please ensure the table to not contain a row named '"
					+ ROW_ID + "'";
			Logging.warn(warningMessage);
		} else {
			table.addColumn(ROW_ID, EnumColType.Int);			
		}
	}

	private void createOutputTable(final RevalResult revalResult,
			Table runtimeTable, 
			List<RetrievalConfiguration> retrievalConfig,
			Map<String, MappingTableColumnConfiguration> mappingTableColConfig) {
		Map<String, String> columnsToRetain = new TreeMap<>();
		applyMandatoryColumnsOutputTableRetrieval(revalResult.getTable(), runtimeTable, columnsToRetain);
		StringBuilder columnNames = new StringBuilder();
		for (RetrievalConfiguration rc : retrievalConfig) {
			OutputTableRetrievalApplicator retrievalApplicator = new OutputTableRetrievalApplicator(rc, cacheManager.getColLoader());
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
			int priority = configTable.getInt(cacheManager.getColLoader().getPriority().getColName(), configRow.getNumber());
			String colNameReportOutput = configTable.getString(cacheManager.getColLoader().getReportOutput().getColName(), configRow.getNumber()); // optional value, might be null
			String colNameRuntimeTable = configTable.getString(cacheManager.getColLoader().getRuntimeDataTable().getColName(), configRow.getNumber());
			String colNameMappingTable = configTable.getString(cacheManager.getColLoader().getMappingTable().getColName(), configRow.getNumber()); // optional value, might be null
			String colNameTaxTable = configTable.getString(cacheManager.getColLoader().getTaxTable().getColName(), configRow.getNumber());  // optional value, might be null
			String colNameMaterialNumberTable = configTable.getString(cacheManager.getColLoader().getMaterialNumberTable().getColName(), configRow.getNumber()); // optional value, might be null
			String colNameCustCompTable = configTable.getString(cacheManager.getColLoader().getCustCompTable().getColName(), configRow.getNumber()); // optional value, might be null
			String retrievalLogic = configTable.getString(cacheManager.getColLoader().getRetrievalLogic().getColName(), configRow.getNumber());
			RetrievalConfiguration rc = new RetrievalConfiguration(cacheManager.getColLoader());
			rc.setPriority(priority);
			rc.setColumnValue(cacheManager.getColLoader().getReportOutput(), colNameReportOutput);
			rc.setColumnValue(cacheManager.getColLoader().getRuntimeDataTable(), colNameRuntimeTable);
			rc.setColumnValue(cacheManager.getColLoader().getMappingTable(), colNameMappingTable);
			rc.setColumnValue(cacheManager.getColLoader().getTaxTable(), colNameTaxTable);
			rc.setColumnValue(cacheManager.getColLoader().getMaterialNumberTable(), colNameMaterialNumberTable);
			rc.setColumnValue(cacheManager.getColLoader().getCustCompTable(), colNameCustCompTable);
			rc.setColumnValue(cacheManager.getColLoader().getRetrievalLogic(), retrievalLogic);
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
		Logging.info("Start createEventDataTable");
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
		Logging.info("Total time in creating event table(ms): " + totalTimeCreateEventTable);
		Logging.info("Total time in add rows and columns(ms): " + totalTimeAddRowsAndColumns);
		resultTable.addColumn("deal_tracking_num", EnumColType.Int);
		for (int row = resultTable.getRowCount()-1; row >= 0; row--) {
			int tranNum = resultTable.getInt("tran_num", row);
			resultTable.setValue ("deal_tracking_num", row, tranNumToDealTrackingNum.get(tranNum));
		}
		Logging.info("createEventDataTable finished successfully");
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
		Logging.info("Starting format of Raw Accounting Data UDSR");
		Logging.info("Completed format of Raw Accounting Data UDSR");
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
		formatColumn(result, "ins_sub_type", EnumReferenceTable.InsSubType);
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
			Logging.init(this.getClass(), ConfigurationItem.CONST_REP_CONTEXT, ConfigurationItem.CONST_REP_SUBCONTEXT);

		} catch (Exception e) {
			throw new RuntimeException (e);
		}
		Logging.info("**********" + this.getClass().getName() + " started **********");
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
