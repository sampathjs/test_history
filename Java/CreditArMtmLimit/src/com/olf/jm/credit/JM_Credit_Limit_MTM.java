package com.olf.jm.credit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.limits.AbstractExposureCalculator2;
import com.olf.embedded.limits.ExposureDefinition;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.limits.EnumRiskLimit;
import com.olf.openrisk.limits.ExposureLimit;
import com.olf.openrisk.limits.ExposureLine;
import com.olf.openrisk.simulation.EnumResultType;
import com.olf.openrisk.simulation.ResultTypes;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.RevalSession;
import com.olf.openrisk.simulation.SimulationFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.StaticTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumProfileFieldId;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.limits.Field;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Legs;
import com.olf.openrisk.trading.Profile;
import com.olf.openrisk.trading.Profiles;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;

/*********************************************************************************************************************
 * File Name:                  JM_Credit_Limit_AR_MTM.java
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
 * 24-Aug-2020  GanapP02       Initial version
 ********************************************************************************************************************/

@ScriptCategory({ EnumScriptCategory.CreditRisk })
public class JM_Credit_Limit_MTM extends AbstractExposureCalculator2<Table, Table> {
	
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
		Logging.init(session, this.getClass(), this.getClass().getSimpleName(), "");
		Logging.info("\ncalculateDealExposures():Processing for deal : " + dealNum);
		Table clientData = dealCache.createConstView("*", "[deal_num] == " + transaction.getDealTrackingId()).asTable();
		if (clientData != null && clientData.getRowCount() > 0) {
			clientData.setName("MTM Detailed WorkSheet");
			rowExposure = clientData.getDouble("base_mtm", 0);
		}

		Field[] fields = definition.getCriteriaFields(transaction);
		DealExposure dealExposure = definition.createDealExposure(rowExposure, transaction, fields);
		if (clientData != null)
			dealExposure.setClientData(clientData);
		
		Logging.close();
		return new DealExposure[] { dealExposure };
	}

	@Override
	public Table createDealCache(Session session, ExposureDefinition definition, Transactions transactions) {
		Logging.init(session, this.getClass(), this.getClass().getSimpleName(), "");
		Logging.info("\ncreateDealCache(): Start Create Deal Cache");

		// If it is a quick credit check for fx swap, then tran_num = 0, first one will be near leg
		if (transactions.getCount() == 2 && transactions.getTransactionIds()[0] == 0) {
			transactions.get(0).assignTemporaryIds();
		}

		int todayJD = session.getCalendarFactory().getJulianDate(session.getTradingDate());
		EnumTransactionFieldId[] fields = {
				EnumTransactionFieldId.InternalLegalEntity,
				EnumTransactionFieldId.InternalBusinessUnit,
				EnumTransactionFieldId.ExternalBusinessUnit,
				EnumTransactionFieldId.ExternalLegalEntity,
				EnumTransactionFieldId.InstrumentType,
				EnumTransactionFieldId.Toolset,
				EnumTransactionFieldId.SettleDate, 
				EnumTransactionFieldId.PartyAgreement };
		Table dealCache = transactions.asTable(fields);
		renameColumns(dealCache);
		dealCache.setName("Deal Cache");

		Transactions transactionsUnsettled = getUnsettledTransactions(session, transactions, todayJD);
		
		// Get Missing party Agreement
		addMissingPartyAgreement(session, dealCache);
		
//		// Get Netting criteria from Party Agreement
//		addPartyAreementNettingCriteria(session, dealCache);
//
//		addMissingNettingColumns(session, dealCache, transactionsUnsettled);
		
		// Set up the reval
		SimulationFactory sf = session.getSimulationFactory();
		RevalSession reval = sf.createRevalSession(transactionsUnsettled);
		// Set the base currency from the Exposure definition
		reval.setCurrency(definition.getCurrency());
		// Get MTM
//		EnumResultType result = EnumResultType.MtmDetail;
		EnumResultType result = EnumResultType.BaseMtm;
		ResultTypes resultTypes = sf.createResultTypes();
		resultTypes.add(result);
		RevalResults results = reval.calcResults(resultTypes);
		if (results.contains(result)) {
			Table simResult = results.getResultTable(result).asTable();
			String sql = "SELECT id_number AS currency_id, 1 AS metal_leg FROM currency WHERE precious_metal = 1";
			Table pmCurrency = session.getIOFactory().runSQL(sql);
			simResult.select(pmCurrency, "metal_leg", "[IN.currency_id] == [OUT.currency_id]");
//			Table baseMtm = session.getTableFactory().createTable("Base Mtm");
//			baseMtm.select(simResult, "*", "[IN.metal_leg] == 1 AND [IN.payment_date] >=" + todayJD);
//			dealCache.select(baseMtm, "deal_num, payment_date, pv->base_mtm", "[IN.deal_num] == [OUT.deal_num]");
			dealCache.select(simResult, "142->base_mtm", "[IN.deal_num] == [OUT.deal_num]", "SUM(base_mtm)");
			dealCache.select(simResult, "currency_id", "[IN.deal_num] == [OUT.deal_num] AND [IN.metal_leg] == 1");
//			baseMtm.dispose();
		} else {
			Logging.info("\ncreateDealCache(): No Sim Result Returned for Base Mtm");
			// If Base MTM does not return anything then the exposure will be 0
			dealCache.addColumn("base_mtm", EnumColType.Double);
			dealCache.setColumnValues("base_mtm", 0.0);
		}
		results.dispose();
		dealCache.sort("deal_num", true);
		Logging.info("\ncreateDealCache(): End Create Deal Cache");
		Logging.close();
		return dealCache;
	}

//	private void addMissingNettingColumns(Session session, Table dealCache, Transactions transactionsUnsettled) {
//		
//		Table temp = session.getTableFactory().createTable();
//		temp.selectDistinct(dealCache, "netting_criteria", "[IN.deal_num] >= 0]");
//		
//		List<EnumTransactionFieldId> fieldsList = new ArrayList<EnumTransactionFieldId>();
//		Map<String,String> stldocCriteriaColumns = getCrieriaColumnList();
//		String copyColumns = "";
//		for(String nettingCriteria : temp.getColumnValuesAsString("netting_criteria")) {
//			if(null == nettingCriteria || nettingCriteria.isEmpty()) {
//				continue;
//			}
//			String[] nettingColumns = nettingCriteria.split(", ");
//			for(String nettingColumn : nettingColumns) {
//				if(!dealCache.isValidColumn(nettingColumn)){
//					String value = stldocCriteriaColumns.get(nettingColumn);
//					if(null == value) {
//						Logging.error("Netting criteria column %s is not mapped. Excluding from netting", nettingColumn);
//						continue;
//					}
//					fieldsList.add( EnumTransactionFieldId.valueOf(value));
//					copyColumns += nettingColumn + ", ";
//				}
//			}
//		}
//		if(fieldsList.size() > 0 ) {
//			copyColumns = copyColumns.substring(0, copyColumns.length()-2);
//			EnumTransactionFieldId[] fieldArray = new EnumTransactionFieldId[fieldsList.size()];
//			fieldArray = fieldsList.toArray(fieldArray);
//			Table missingColumns = transactionsUnsettled.asTable(fieldArray);
//			renameColumns(missingColumns);
//			dealCache.select(missingColumns, copyColumns, "[IN.deal_num] == [OUT.deal_num]");
//		}
//	}

	private void renameColumns(Table table) {
		
		renameColumn(table, EnumTransactionFieldId.DealTrackingId.getName(), "deal_num");
		renameColumn(table, EnumTransactionFieldId.TransactionId.getName(), "tran_num");
		renameColumn(table, EnumTransactionFieldId.InternalBusinessUnit.getName(), "internal_bunit");
		renameColumn(table, EnumTransactionFieldId.InternalLegalEntity.getName(), "internal_lentity");
		renameColumn(table, EnumTransactionFieldId.ExternalBusinessUnit.getName(), "external_bunit");
		renameColumn(table, EnumTransactionFieldId.ExternalLegalEntity.getName(), "external_lentity");
		renameColumn(table, EnumTransactionFieldId.SettleDate.getName(), "settle_date");
		renameColumn(table, EnumTransactionFieldId.Toolset.getName(), "toolset");
		renameColumn(table, EnumTransactionFieldId.PartyAgreement.getName(), "party_agreement_id");
		renameColumn(table, EnumTransactionFieldId.InstrumentType.getName(), "ins_type");
		renameColumn(table, EnumTransactionFieldId.InstrumentSubType.getName(), "ins_sub_type");
		renameColumn(table, EnumTransactionFieldId.DealIndexGroup.getName(), "idx_group");
		renameColumn(table, EnumTransactionFieldId.DealIndexSubGroup.getName(), "idx_subgroup");
		renameColumn(table, EnumTransactionFieldId.InternalPortfolio.getName(), "portfolio_id");
		renameColumn(table, EnumTransactionFieldId.BuySell.getName(), "buy_sell");
		renameColumn(table, EnumTransactionFieldId.MasterNettingAgreement.getName(), "master_netting_agreement");
		renameColumn(table, EnumTransactionFieldId.InstrumentId.getName(), "ins_num");
		renameColumn(table, EnumTransactionFieldId.TransactionGroup.getName(), "tran_group");
		renameColumn(table, EnumTransactionFieldId.SettleCurrency.getName(), "settle_ccy");
		renameColumn(table, EnumTransactionFieldId.Ticker.getName(), "ticker");
		
	}

	private void renameColumn(Table table, String fromName, String toNmae) {
		
		if(table.isValidColumn(fromName)){
			table.setColumnName(table.getColumnId(fromName), toNmae);	
		}
	}

//	private Map<String,String> getCrieriaColumnList() {
//
//		Map<String,String> stldocCriteriaColumns = new HashMap<String,String>();
//		stldocCriteriaColumns.put("deal_tracking_num", EnumTransactionFieldId.DealTrackingId.name());
//		stldocCriteriaColumns.put("settle_ccy",  EnumTransactionFieldId.SettleCurrency.name());
//		stldocCriteriaColumns.put("external_bunit",  EnumTransactionFieldId.ExternalBusinessUnit.name());
//		stldocCriteriaColumns.put("external_lentity", EnumTransactionFieldId.ExternalLegalEntity.name());
//		stldocCriteriaColumns.put("party_agreement_id", EnumTransactionFieldId.PartyAgreement.name());
//		stldocCriteriaColumns.put("internal_bunit", EnumTransactionFieldId.InternalBusinessUnit.name());
//		stldocCriteriaColumns.put("internal_lentity", EnumTransactionFieldId.InternalLegalEntity.name());
//		stldocCriteriaColumns.put("toolset", EnumTransactionFieldId.Toolset.name());
//		stldocCriteriaColumns.put("ins_type", EnumTransactionFieldId.InstrumentType.name());
//		stldocCriteriaColumns.put("ins_sub_type", EnumTransactionFieldId.InstrumentSubType.name());
//		stldocCriteriaColumns.put("idx_group", EnumTransactionFieldId.DealIndexGroup.name());
//		stldocCriteriaColumns.put("idx_subgroup", EnumTransactionFieldId.DealIndexSubGroup.name());
//		stldocCriteriaColumns.put("portfolio_id", EnumTransactionFieldId.InternalPortfolio.name());
//		stldocCriteriaColumns.put("buy_sell", EnumTransactionFieldId.BuySell.name());
//		stldocCriteriaColumns.put("master_netting_agreement", EnumTransactionFieldId.MasterNettingAgreement.name());
//		stldocCriteriaColumns.put("ins_num", EnumTransactionFieldId.InstrumentId.name());
//		stldocCriteriaColumns.put("tran_group", EnumTransactionFieldId.TransactionGroup.name());
//		stldocCriteriaColumns.put("ticker", EnumTransactionFieldId.Ticker.name());
//		stldocCriteriaColumns.put("ext_bunit_id", EnumTransactionFieldId.ExternalBusinessUnit.name());
//		stldocCriteriaColumns.put("ext_lentity_id", EnumTransactionFieldId.ExternalLegalEntity.name());
//		
//		
////		stldocCriteriaColumns.put("delivery_type", EnumLegFieldId.DeliveryType.name()
//		
////		stldocCriteriaColumns.put("int_settle_id", "");
////		stldocCriteriaColumns.put("ext_settle_id", "");
////		stldocCriteriaColumns.put("event_num", "");
////		stldocCriteriaColumns.put("event_date", "");
////		stldocCriteriaColumns.put("event_type", "");
////		stldocCriteriaColumns.put("settle_unit", "");
////		stldocCriteriaColumns.put("settle_type", "");
////		stldocCriteriaColumns.put("event_source", "");
////		stldocCriteriaColumns.put("event_party_agreement_id", "");
////		
////		stldocCriteriaColumns.put("parcel_id", "");
////		stldocCriteriaColumns.put("tran_info_type_20015", "");
////		stldocCriteriaColumns.put("event_info_type_20006", "");
////		stldocCriteriaColumns.put("event_info_type_20002", "");
////		stldocCriteriaColumns.put("int_metal_account", "");
////		stldocCriteriaColumns.put("ext_metal_account", "");
////		stldocCriteriaColumns.put("tax_tran_subtype", "");
////		stldocCriteriaColumns.put("party_info_type_20048", "");
////		stldocCriteriaColumns.put("tran_info_type_20044", "");
////		stldocCriteriaColumns.put("payment_currency_Invoice", "");
////		stldocCriteriaColumns.put("deal_unit", "");
////		stldocCriteriaColumns.put("event_info_type_20005", "");
//
//		return stldocCriteriaColumns;
//	}

	private Transactions getUnsettledTransactions(Session session, Transactions transactions, int todayJD) {

		String sql = "SELECT id_number AS currency FROM currency WHERE precious_metal = 1";
		Table pmCurrency = session.getIOFactory().runSQL(sql);

		Transactions transactionsUnsettled = session.getTradingFactory().createTransactions();
		for (Transaction tran : transactions) {

			// FX Deals do not have profiles, get settle date from base/term settle date from transaction
			if (tran.getInstrumentTypeObject().getId() == EnumInsType.FxInstrument.getValue()) {
				int baseCurrency = tran.getField(EnumTransactionFieldId.FxBaseCurrency).getValueAsInt();
				int termCurrency = tran.getField(EnumTransactionFieldId.FxTermCurrency).getValueAsInt();
				int settleDate = 0;
				if (pmCurrency.find(0, baseCurrency, 0) >= 0) {
					settleDate = tran.getField(EnumTransactionFieldId.SettleDate).getValueAsInt();
				} else if (pmCurrency.find(0, termCurrency, 0) >= 0) {
					settleDate = tran.getField(EnumTransactionFieldId.FxTermSettleDate).getValueAsInt();
				}
				if (settleDate >= todayJD) {
					transactionsUnsettled.add(tran);
				}
			} else if(tran.getInstrumentTypeObject().getId() == EnumInsType.CallNoticeNostro.getValue()
					|| tran.getInstrumentTypeObject().getId() == EnumInsType.CallNoticeMultiLegNostro.getValue()) {
				// Always include Call Notice deals
				transactionsUnsettled.add(tran);
			} else 	{
				// For ComSwap and LoanDepo & Call Not
				boolean addTran = false;
				Legs legs = tran.getLegs();
				for (Leg leg : legs) {
					if (pmCurrency.find(0, leg.getField(EnumLegFieldId.Currency).getValueAsInt(), 0) < 0) {
						// skip if it is not metal leg
						continue;
					}
					Profiles profiles = leg.getProfiles();
					for (Profile profile : profiles) {
						if (profile.getField(EnumProfileFieldId.PaymentDate).getValueAsInt() < todayJD) {
							continue;
						}
						// if payment date > today add transaction to calculate reval
						addTran = true;
					} // profile
				} // leg
				if (addTran)
					transactionsUnsettled.add(tran);
			} // non FX instrument
		} // Transaction

		return transactionsUnsettled;
	}

	private void addMissingPartyAgreement(Session session, Table dealCache) {
		
		Table partyAgreementList = null;
		Table uniquePA = null;
		try {
			String sql = "SELECT intbu.party_id internal_bunit, extbu.party_id external_bunit, pa.party_agreement_id"
					+ "\n     , netting_flag, haircut_calculator_flag, haircut_method, ai.ins_type"
					+ "\n  FROM party_agreement pa"
					+ "\n  JOIN party_agreement_assignment intbu ON pa.party_agreement_id = intbu.party_agreement_id"
					+ "\n       AND intbu.internal_external_flag = 0"
					+ "\n  JOIN party_agreement_assignment extbu ON pa.party_agreement_id = extbu.party_agreement_id"
					+ "\n       AND extbu.internal_external_flag = 1"
					+ "\n  JOIN agreement_ins ai ON pa.agreement_id = ai.agreement_id"
					+ "\n WHERE pa.doc_status = 1";
			partyAgreementList = session.getIOFactory().runSQL(sql);
			
			uniquePA = session.getTableFactory().createTable();
			int rowCount = dealCache.getRowCount();
			dealCache.addColumn("netting_flag", EnumColType.Int);
			dealCache.addColumn("haircut_calculator_flag", EnumColType.Int);
			dealCache.addColumn("haircut_method", EnumColType.Int);
			for(int row = 0; row < rowCount; row++) {
				int intBU = dealCache.getValueAsInt("internal_bunit", row);
				int extBU = dealCache.getValueAsInt("external_bunit", row);
				int insType = dealCache.getValueAsInt("ins_type", row);
				int partyAgreement = dealCache.getValueAsInt("party_agreement_id", row);
				String where = "[IN.internal_bunit] == " + intBU + " AND [IN.external_bunit] == " + extBU
						+ " AND [IN.ins_type] == " + insType;
				if (partyAgreement != 0) {
					where += " AND [IN.party_agreement_id] == " + partyAgreement;
				}
				uniquePA.select(partyAgreementList, "*", where);
				if(uniquePA.getRowCount() == 1) {
					dealCache.setInt("party_agreement_id", row, uniquePA.getInt("party_agreement_id", 0));
					dealCache.setInt("netting_flag", row, uniquePA.getInt("netting_flag", 0));
					dealCache.setInt("haircut_calculator_flag", row, uniquePA.getInt("haircut_calculator_flag", 0));
					dealCache.setInt("haircut_method", row, uniquePA.getInt("haircut_method", 0));
				}
				uniquePA.clear();
			}
		} catch (Exception e) {
			String message = "Failed to load party agreement netting flag : " + e.getMessage(); 
			Logging.error(message);
			printStatement(message);
			throw new OpenRiskException(message);
		} finally {
			partyAgreementList.dispose();
			uniquePA.dispose();			
		}
	}

//	private void addPartyAreementNettingCriteria(Session session, Table dealCache) {
//		
//		String sql = "SELECT pasc.party_agreement_id, sc.stldoc_criteria_column netting_criteria"
//				+ "\n  FROM party_agreement_stl_criteria pasc"
//				+ "\n  JOIN stldoc_criteria sc ON pasc.stldoc_criteria_id = sc.stldoc_criteria_id";
//		Table paStlCriteria = session.getIOFactory().runSQL(sql);
//		
//		Table nettingCriteria = session.getTableFactory().createTable();
//		nettingCriteria.selectDistinct(paStlCriteria, "party_agreement_id", "[IN.party_agreement_id] >0");
//		nettingCriteria.addColumn("netting_criteria", EnumColType.String);
//		int rowCount = nettingCriteria.getRowCount();
//		for(int row = 0; row < rowCount; row++) {
//			int pa = nettingCriteria.getInt("party_agreement_id", row);
//			StaticTable view = paStlCriteria.createView("netting_criteria", "[party_agreement_id] == " + pa);
//			String criteria = "";
//			for(int viewRow =0; viewRow < view.getRowCount(); viewRow++) {
//				criteria +=view.getString("netting_criteria", viewRow) + ", ";
//			}
//			nettingCriteria.setString("netting_criteria", row, criteria.substring(0, criteria.length()-2));
//		}
//		dealCache.select(nettingCriteria, "netting_criteria", "[IN.party_agreement_id] == [OUT.party_agreement_id]");
//	}

	@Override
	public void disposeDealCache(Session session, Table dealCache) {
		dealCache.dispose();
	}

	@Override
	public double aggregateLineExposures(Session session, ExposureLine line, LineExposure[] exposures,
			Table exposureCache, boolean isInquiry) {

		double rawExposure = 0.0;
		Logging.init(session, this.getClass(), this.getClass().getSimpleName(), "");
		Logging.info("\naggregateLineExposures(): Start Aggegate Line Exposures");

		printStatement("\nLine: " + line.toString());
//		double limit = line.getLimit(EnumRiskLimit.Unsecured);
		double limit = 0;
		try {
			for (LineExposure exposure : exposures) {
				printStatement("\nExposure : " + exposure.toString());
				double lineExposure = exposure.getRawExposure();
				Logging.info("\naggregateLineExposures(): Exposure : " + exposure.toString());
				ConstTable clientData = exposure.getClientData();
				if (clientData == null || clientData.getRowCount() <= 0) {
					continue;
				}
				int toolset = clientData.getInt("toolset", 0);
				if (toolset == EnumToolset.CallNotice.getValue()) {
					if(clientData.getInt("haircut_calculator_flag", 0) ==1){
						lineExposure = applyHaircut(session, clientData, lineExposure);	
					}
					limit += lineExposure;
					continue;
				}
				boolean useNetting = clientData.getInt("netting_flag", 0) == 1;
//				String nettingCriteria = clientData.getString("netting_criteria", 0);
//				// If deal's Party Agreement has netting criteria or if deal has no netting criteria 
//				// and line exposure is positive
//				if ((nettingCriteria != null &&	 !"".equalsIgnoreCase(nettingCriteria)) || lineExposure > 0) {
				if (useNetting || lineExposure > 0) {
					rawExposure += lineExposure;
				}
			}
		} catch (Exception e) {
			Logging.error("Failed : " + e.getMessage());
			printStatement("\nFailed : " + e.getMessage());
		}
		printStatement("\nRaw Exposure : " + rawExposure);
		printStatement("\nCollateral Limit : " + limit);
		
		Logging.info("\naggregateLineExposures(): End Aggegate Line Exposures");
		Logging.close();
		return rawExposure - limit ;
	}

	private double applyHaircut(Session session, ConstTable clientData, double lineExposure) {
		
		int partyAgreement = clientData.getInt("party_agreement_id", 0);
		int insType =  clientData.getInt("ins_type", 0);
		int currency = clientData.getInt("currency_id", 0);;
		
		String sql = "SELECT haircut FROM party_agreement_haircut WHERE party_agreement_id = " + partyAgreement
				+ " AND instrument_type = " + insType + " AND currency_id = " + currency;
		Table paHaircut = session.getIOFactory().runSQL(sql);
		if(paHaircut.getRowCount()>0) {
			double haircut = paHaircut.getDouble("haircut", 0);
			int haircutMethod = clientData.getInt("haircut_method", 0);
			if(haircutMethod == 1 && haircut != 0.0000 ){
				lineExposure /= haircut;
			} else {
				lineExposure *= haircut;
			}
		}
		return lineExposure;
	}

	private void printStatement(String message) {
		Logging.info(message);
		try {
			OConsole.oprint(message);
		} catch (OException e1) {
			throw new RuntimeException(e1.getMessage());
		}
	}

}