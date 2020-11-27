package com.olf.jm.credit;

/*********************************************************************************************************************
 * File Name:                  JM_Credit_Limit_AR.java
 *  
 * Author:                     Prashanth Ganapathi
 * 
 * Date Of Last Revision:
 * 
 * Script Type:                Main - Process
 * Parameter Script:           None
 * Display Script:             None
 * 
 * Toolsets script applies:    FX, ComSwap, LoanDep
 * 
 * Type of Script:             Credit batch, deal, update or ad-hoc report
 * 
 * History
 * 18-Aug-2020  GanapP02	EPI-1497	Initial version
 ********************************************************************************************************************/

import com.matthey.webservice.consumer.FinancialService;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.limits.AbstractExposureCalculator2;
import com.olf.embedded.limits.ExposureDefinition;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.limits.ConstField;
import com.olf.openrisk.limits.EnumRiskCriteria;
import com.olf.openrisk.limits.ExposureLine;
import com.olf.openrisk.simulation.EnumResultType;
import com.olf.openrisk.simulation.ResultTypes;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.RevalSession;
import com.olf.openrisk.simulation.SimulationFactory;
import com.olf.openrisk.staticdata.Currency;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.LegalEntity;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumColumnOperation;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;

@ScriptCategory({ EnumScriptCategory.CreditRisk })
public class JM_Credit_Limit_AR extends AbstractExposureCalculator2<Table, Table> {

	private final String PartyInfoPartyCodeUK = "Party Code UK";
	private final String PartyInfoPartyCodeUS = "Party Code US";
	private final String PartyInfoPartyCodeHK = "Party Code HK";
	private final String PartyInfoPartyCodeCN = "Party Code CN - Debtor";
	private String legalName = "";

	// Cache the maturity bucket and only load once
	Table maturityBuckets = null;
	String error_log_file = null;

	@Override
	public Table createExposureCache(Session session, ExposureDefinition definition) {

		// Get Available Exposure type from the definition
		String sql = "SELECT available_exposure_type FROM rsk_exposure_defn " 
				+ "WHERE exp_defn_id = " + definition.getId();
		return session.getIOFactory().runSQL(sql);
	}

	@Override
	public com.olf.embedded.limits.ExposureCalculator2.DealExposure[] calculateDealExposures(Session session,
			ExposureDefinition definition, Transaction transaction, Table dealCache) {

		double rowExposure = 0.0;
		int dealNum = transaction.getDealTrackingId();
		Table clientData = dealCache.createConstView("*", "[deal_num] == " + transaction.getDealTrackingId()).asTable();
		if (clientData != null && clientData.getRowCount() > 0) {
			clientData.setName("MTM Detailed WorkSheet");
			rowExposure = clientData.getDouble("base_mtm", 0);
		}

		// remove any temporary ID at this stage
		if (dealNum < 0)
			transaction.clearTemporaryIds();

		DealExposure dealExposure = definition.createDealExposure(rowExposure, transaction);
		if (clientData != null)
			dealExposure.setClientData(clientData);
		return new DealExposure[] { dealExposure };
	}

	@Override
	public Table createDealCache(Session session, ExposureDefinition definition, Transactions transactions)	{
		Logging.init(session, this.getClass(), this.getClass().getSimpleName(), "");
		Logging.info( "Start Create Deal Cache");
		
		// If it is a quick credit check for fx swap, then tran_num = 0, first one will be near leg
		if (transactions.getCount() == 2 && transactions.getTransactionIds()[0] == 0) {
			transactions.get(0).assignTemporaryIds();
		}

		int todayJD = session.getCalendarFactory().getJulianDate(session.getTradingDate());
		EnumTransactionFieldId[] fields = { EnumTransactionFieldId.ExternalBusinessUnit, EnumTransactionFieldId.Toolset,
				EnumTransactionFieldId.InstrumentType, EnumTransactionFieldId.MaturityDate };
		Table dealCache = transactions.asTable(fields);
		dealCache.setName("Deal Cache");
		dealCache.setColumnName(dealCache.getColumnId("Deal Tracking Id"), "deal_num");
		dealCache.setColumnName(dealCache.getColumnId("Transaction Id"), "tran_num");
		dealCache.setColumnName(dealCache.getColumnId("External Business Unit"), "external_bunit");
		dealCache.setColumnName(dealCache.getColumnId("Toolset"), "toolset");
		dealCache.setColumnName(dealCache.getColumnId("Instrument Type"), "ins_type");
		dealCache.setColumnName(dealCache.getColumnId("Maturity Date"), "maturity_date");

		Transactions copyTrans = session.getTradingFactory().createTransactions();
		for (int tranCount = 0; tranCount < dealCache.getRowCount(); tranCount++) {
			Transaction tran = transactions.getTransaction(dealCache.getInt("deal_num", tranCount));
			String insType = tran.getInstrumentTypeObject().getName().toString();
			// if not comm-phys, safe to add to reval
			if (!insType.isEmpty() && insType.equalsIgnoreCase("COMM-PHYS") == false) {
				copyTrans.add(tran);
			}
		}
		
		// Set up the reval
		SimulationFactory sf = session.getSimulationFactory();
		RevalSession reval = sf.createRevalSession(copyTrans);
		// Set the base currency from the Exposure definition
		reval.setCurrency(definition.getCurrency());

		// Get MTM
		ResultTypes resultTypes = sf.createResultTypes();
		resultTypes.add(EnumResultType.CashflowByDay);
		RevalResults results = reval.calcResults(resultTypes);
		if (results.contains(EnumResultType.CashflowByDay)) {
			Table cflowByDay = results.getResultTable(EnumResultType.CashflowByDay).asTable();
			String sql = "SELECT id_number AS currency, 1 AS metal_leg FROM currency WHERE precious_metal = 1";
			Table pmCurrency = session.getIOFactory().runSQL(sql);
			cflowByDay.select(pmCurrency, "metal_leg", "[IN.currency] == [OUT.currency]");
			
			Table baseMTM = session.getTableFactory().createTable();
			baseMTM.setName("BASE MTM");
			baseMTM.selectDistinct(cflowByDay, "deal_num", "[IN.metal_leg] == 1 AND [IN.cflow_date] < " + todayJD);
			baseMTM.select(cflowByDay, "base_cflow->base_mtm", "[IN.deal_num] == [OUT.deal_num] AND [IN.metal_leg] == 0 "
					+ "AND [IN.cflow_date] >= " + todayJD, "SUM(base_mtm)");
			//base_mtm is mtm of currency leg
			dealCache.select(baseMTM, "base_mtm", "[IN.deal_num] == [OUT.deal_num] AND [IN.base_mtm] >0.0000");
			//deal_base_mtm is mtm of deal
			dealCache.select(cflowByDay, "base_cflow->deal_base_mtm", "[IN.deal_num] == [OUT.deal_num]", "SUM(deal_base_mtm)");
		} else {
			Logging.info("No Sim Result Returned for Cash Flow By Day \n");
			// If cash flow by day does not return anything then the exposure will be 0
			dealCache.addColumn("base_mtm", EnumColType.Double);
			dealCache.setColumnValues("base_mtm", 0.0);
		}
		
		dealCache.removeColumn("ins_type");
		dealCache.removeColumn("maturity_date");
		results.dispose();
		copyTrans.dispose();
		
		Logging.info("End Create Deal Cache");
		Logging.close();
		return dealCache;
	}

	@Override
	public void disposeDealCache(Session session, Table dealCache) {
		dealCache.dispose();
	}

	@Override
	public double aggregateLineExposures(Session session, ExposureLine line, LineExposure[] exposures,
			Table exposureCache, boolean isInquiry) {

		double rawExposure = 0.0;
		try {
			Logging.init(session, this.getClass(), this.getClass().getSimpleName(), "");
			Logging.info("Start Aggegate Line Exposures");

			rawExposure = getARAmount(session, line);
			for (LineExposure exposure : exposures) {
				double lineExposure = exposure.getRawExposure();
				if (lineExposure > 0) {
					rawExposure += lineExposure;
				}
			}

			Logging.info("End Aggegate Line Exposures");
			Logging.close();
		} catch (Exception e) {
			Logging.info("Failed to get Aggregate Line Exposure: " + e.getMessage());
		}
		return rawExposure;
	}

	private double getARAmount(Session session, ExposureLine line) {
		double arAmount = 0.0;	
		String partyCodeUK = "", partyCodeUS = "", partyCodeHK = "", partyCodeCN = "";
		try {
			ConstField[] fields = line.getCriteriaFields();
			Table partyCodes = getPartyCodes(session, fields);
			
			// get the definition reporting currency
			Currency reportingCurrency = line.getFacility().getDefinition().getCurrency();					

			for(TableRow partyCodeRow : partyCodes.getRows()){
				Table arDetails = FinancialService.getOpenItems(session, "roy",null);
				// Get Party Info
				String partyName = partyCodeRow.getString("short_name");
				partyCodeUK = partyCodeRow.getString("party_code_uk");
				partyCodeUS = partyCodeRow.getString("party_code_us");
				partyCodeHK = partyCodeRow.getString("party_code_hk");
				partyCodeCN = partyCodeRow.getString("party_code_cn");
				Logging.info("Getting AR Balance for (" + partyName + "): UK:" + partyCodeUK + "/US:" + partyCodeUS + "/HK:" + partyCodeHK + "/CN:" + partyCodeCN);
				
				
				getOpenItemsForAccount(session, arDetails, partyName, partyCodeUK, "UK", "roy");
				getOpenItemsForAccount(session, arDetails, partyName, partyCodeUS, "US", "vfc");
				getOpenItemsForAccount(session, arDetails, partyName, partyCodeHK, "HK", "hgk");
				getOpenItemsForAccount(session, arDetails, partyName, partyCodeCN, "CN", "Shanghai");
				
				// Get FX rates to reporting currency of Defintion
				for(TableRow row : arDetails.getRows()) {
					row.getCell(FinancialService.BASE_VALUE).setDouble(0.0);
					Double balance = row.getDouble(FinancialService.VALUE);
					// only include +ve balances
					if (balance > 0.0) {
						String ccyName = row.getString(FinancialService.CURRENCY);
						Currency ccy = (Currency) session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.Currency, ccyName.equals("RMB") ? "CNY" : ccyName);
						try {
							// Get fx conversion rate to reporting currency
							double rate = session.getMarket().getFXRate(ccy, reportingCurrency, session.getTradingDate());
							row.getCell(FinancialService.SPOT_RATE).setDouble(rate);
							row.getCell(FinancialService.BASE_VALUE).setDouble(rate * balance);
						} catch (Exception e) {
							Logging.error( "FX rate for currency " + ccy.getName() + "-> " + reportingCurrency.getName() + " not set. \n", e);
						}
					}
				}
	
				double bunitTotal = arDetails.calcAsDouble(arDetails.getColumnId(FinancialService.BASE_VALUE), EnumColumnOperation.Sum);
				Logging.info("Final " + reportingCurrency.getName() + " AR Balance for (" + partyName + ") = %20.0f",bunitTotal);
				arAmount += bunitTotal;
			}
			Logging.info("Final " + reportingCurrency.getName() + " AR Balance for (" + legalName + ") = %20.0f", arAmount);
			
			partyCodes.dispose();
		}
		catch (Exception e)	{
			Logging.error("Error getting AR Balance for (" + legalName + "): UK:" + partyCodeUK + "/US:" + partyCodeUS + "/HK:" + partyCodeHK, e);
		}
		
		return arAmount >0 ? arAmount : 0.0;
	}

	private Table getPartyCodes(Session session, ConstField[] fields) {

		StaticDataFactory sdf = session.getStaticDataFactory();
		int extBunit = 0;
		String buSql = null;
		for (ConstField field : fields) {
			if (field.getCriteriaType().getId() == EnumRiskCriteria.ExtBunit.getValue()) {
				legalName = field.getValueAsString();
				extBunit = sdf.getId(EnumReferenceTable.Party, legalName);
				buSql = String.valueOf(extBunit);
				break;
			} else if (field.getCriteriaType().getId() == EnumRiskCriteria.ExtLentity.getValue()) {
				String extLentity = field.getValueAsString();
				LegalEntity le = (LegalEntity) sdf.getReferenceObject(EnumReferenceObject.LegalEntity, extLentity);
				legalName = le.getName();
				buSql = "(SELECT business_unit_id FROM party_relationship WHERE legal_entity_id = " + le.getId() + ")";
				break;
			}
		}

		// Get party info fields to find out account numbers
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT p.short_name, p.party_id, uk.value party_code_uk, us.value party_code_us");
		sql.append("\n     , hk.value party_code_hk, cn.value party_code_cn");
		sql.append("\nFROM party p");
		sql.append("\nLEFT OUTER JOIN party_info_view uk ON uk.party_id = p.party_id");
		sql.append("       AND uk.type_name = '").append(PartyInfoPartyCodeUK).append("'");
		sql.append("\nLEFT OUTER JOIN party_info_view us ON us.party_id = p.party_id");
		sql.append("        AND us.type_name = '").append(PartyInfoPartyCodeUS).append("'");
		sql.append("\nLEFT OUTER JOIN party_info_view hk ON hk.party_id = p.party_id");
		sql.append("        AND hk.type_name = '").append(PartyInfoPartyCodeHK).append("'");
		sql.append("\nLEFT OUTER JOIN party_info_view cn ON cn.party_id = p.party_id");
		sql.append("        AND cn.type_name = '").append(PartyInfoPartyCodeCN).append("'");
		sql.append("\nWHERE p.party_id IN ").append(buSql);
		Table partyCodes = session.getIOFactory().runSQL(sql.toString());
		return partyCodes;
	}

	private void getOpenItemsForAccount(Session session, Table arDetails, String partyName, String account,
			String country, String location) {
		Table ar = null;
		try {
			if (!account.isEmpty()) {
				ar = FinancialService.getOpenItems(session, location, account);
				arDetails.appendRows(ar);
				for (int i = 0; i < ar.getRowCount(); i++)
					Logging.info("AR Balance for (" + partyName + "): " + country + ":" + account + " [" + i 
							+ "] = %20.0f", ar.getDouble(FinancialService.VALUE, i));
			}
		} catch (Exception e) {
			Logging.error("Error getting AR Balance for (" + partyName + "): " + country + ":" + account, e);
		} finally {
			if (ar != null) {
				ar.dispose();
			}
		}
	}
}