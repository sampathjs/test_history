package com.matthey.pmm.toms.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.service.misc.ReportBuilderHelper;
import com.matthey.pmm.toms.transport.CounterPartyTickerRuleTo;
import com.matthey.pmm.toms.transport.ImmutableCounterPartyTickerRuleTo;
import com.matthey.pmm.toms.transport.ImmutableTickerFxRefSourceRuleTo;
import com.matthey.pmm.toms.transport.ImmutableTickerPortfolioRuleTo;
import com.matthey.pmm.toms.transport.ImmutableTickerRefSourceRuleTo;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.TickerFxRefSourceRuleTo;
import com.matthey.pmm.toms.transport.TickerPortfolioRuleTo;
import com.matthey.pmm.toms.transport.TickerRefSourceRuleTo;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;

public class ValidationRuleService {
	protected final Session session;
	
	public ValidationRuleService (final Session session) {
		this.session = session;
	}
	
	public List<CounterPartyTickerRuleTo> getCounterPartyTickerRules (List<ReferenceTo> references) {
		String reportName = ReportBuilderHelper.retrieveReportBuilderNameForSyncCategory(session.getIOFactory(), "RuleCounterPartyTicker");
		Table reportData = ReportBuilderHelper.runReport(session.getTableFactory(), reportName);
		List<CounterPartyTickerRuleTo> rules = new ArrayList<>(reportData.getRowCount());
		Map<String, Long> metalFormEndurToTomsIdMap = new HashMap<>();
		references.stream()
			.filter(x -> x.idType() == DefaultReferenceType.METAL_FORM.getEntity().id())
			.forEach(x -> metalFormEndurToTomsIdMap.put(x.name(), x.id()));

		Map<String, Long> metalLocationEndurToTomsIdMap = new HashMap<>();
		references.stream()
			.filter(x -> x.idType() == DefaultReferenceType.METAL_LOCATION.getEntity().id())
			.forEach(x -> metalLocationEndurToTomsIdMap.put(x.name(), x.id()));
		
		Map<String, Long> tickerEndurToTomsIdMap = new HashMap<>();
		references.stream()
			.filter(x -> x.idType() == DefaultReferenceType.TICKER.getEntity().id())
			.forEach(x -> metalFormEndurToTomsIdMap.put(x.name(), x.id()));    	

		
		for (int row = reportData.getRowCount()-1; row >= 0; row--) {
			long metalFormReferenceId = metalFormEndurToTomsIdMap.get(reportData.getString("form", row));
			long metalLocationId = metalLocationEndurToTomsIdMap.get(reportData.getString("loco", row));
			long tickerId = tickerEndurToTomsIdMap.get(reportData.getString("toms_product", row));
			
			CounterPartyTickerRuleTo rule = ImmutableCounterPartyTickerRuleTo.builder()
					.accountName(reportData.getString("account_name", row))
					.counterPartyDisplayName(reportData.getDisplayString(reportData.getColumnId("party_id"), row))
					.idCounterParty(reportData.getInt("party_id", row))
					.idMetalForm(metalFormReferenceId)
					.idMetalLocation(metalLocationId)
					.idTicker(tickerId)
					.metalFormDisplayString(reportData.getString("form", row))
					.metalLocationDisplayString(reportData.getString("loco", row))
					.tickerDisplayName(reportData.getString("toms_product", row))
					.build();
			rules.add(rule);
		}
		return rules;		
	}

	public List<TickerPortfolioRuleTo> getTickerPortfolioRules(List<ReferenceTo> references) {
		String reportName = ReportBuilderHelper.retrieveReportBuilderNameForSyncCategory(session.getIOFactory(), "RuleTickerPortfolio");
		Table reportData = ReportBuilderHelper.runReport(session.getTableFactory(), reportName);
		List<TickerPortfolioRuleTo> rules = new ArrayList<>(reportData.getRowCount());
		Map<String, Long> portfolioEndurToTomsIdMap = new HashMap<>();
		references.stream()
			.filter(x -> x.idType() == DefaultReferenceType.PORTFOLIO.getEntity().id())
			.forEach(x -> portfolioEndurToTomsIdMap.put(x.name(), x.id()));

		Map<String, Long> tickerEndurToTomsIdMap = new HashMap<>();
		references.stream()
			.filter(x -> x.idType() == DefaultReferenceType.TICKER.getEntity().id())
			.forEach(x -> tickerEndurToTomsIdMap.put(x.name(), x.id()));
		
		for (int row = reportData.getRowCount()-1; row >= 0; row--) {
			long portfolioId = portfolioEndurToTomsIdMap.get(reportData.getString("name", row));
			long tickerId = tickerEndurToTomsIdMap.get(reportData.getString("toms_product", row));
			
			TickerPortfolioRuleTo rule = ImmutableTickerPortfolioRuleTo.builder()
					.displayStringIndex(reportData.getString("index_name", row))
					.displayStringParty(reportData.getString("short_name", row))
					.displayStringPortfolio(reportData.getString("name", row))
					.displayStringTicker(reportData.getString("toms_product", row))
					.idIndex(reportData.getInt ("index_id", row))
					.idParty(reportData.getInt ("party_id", row))
					.idPortfolio(portfolioId)
					.idTicker(tickerId)
					.build();
			rules.add(rule);
		}
		return rules;				
	}

	public List<TickerRefSourceRuleTo> getTickerRefSourceRules(List<ReferenceTo> references) {
		String reportName = ReportBuilderHelper.retrieveReportBuilderNameForSyncCategory(session.getIOFactory(), "RuleTickerRefSource");
		Table reportData = ReportBuilderHelper.runReport(session.getTableFactory(), reportName);
		List<TickerRefSourceRuleTo> rules = new ArrayList<>(reportData.getRowCount());

		Map<String, Long> tickerEndurToTomsIdMap = new HashMap<>();
		references.stream()
			.filter(x -> x.idType() == DefaultReferenceType.TICKER.getEntity().id())
			.forEach(x -> tickerEndurToTomsIdMap.put(x.name(), x.id()));
		
		Map<String, Long> refSourceEndurToTomsIdMap = new HashMap<>();
		references.stream()
			.filter(x -> x.idType() == DefaultReferenceType.REF_SOURCE.getEntity().id())
			.forEach(x -> tickerEndurToTomsIdMap.put(x.name(), x.id()));		
		
		for (int row = reportData.getRowCount()-1; row >= 0; row--) {
			long tickerId = tickerEndurToTomsIdMap.get(reportData.getString("toms_product", row));
			long refSourceId = refSourceEndurToTomsIdMap.get(reportData.getString("ref_source_name", row));
			
			TickerRefSourceRuleTo rule = ImmutableTickerRefSourceRuleTo.builder()
					.displayStringIndex(reportData.getString("index_name", row))
					.displayStringTicker(reportData.getString("toms_product", row))
					.displayStringRefSource(reportData.getString("ref_source_name", row))
					.idIndex(reportData.getInt ("index_id", row))
					.idTicker(tickerId)
					.idRefSource(refSourceId)
					.build();
			rules.add(rule);
		}
		return rules;				
	}
	
	public List<TickerFxRefSourceRuleTo> getTickerFxRefSourceRules(List<ReferenceTo> references) {
		String reportName = ReportBuilderHelper.retrieveReportBuilderNameForSyncCategory(session.getIOFactory(), "RuleTickerFxRefSource");
		Table reportData = ReportBuilderHelper.runReport(session.getTableFactory(), reportName);
		List<TickerFxRefSourceRuleTo> rules = new ArrayList<>(reportData.getRowCount());

		Map<String, Long> tickerEndurToTomsIdMap = new HashMap<>();
		references.stream()
			.filter(x -> x.idType() == DefaultReferenceType.TICKER.getEntity().id())
			.forEach(x -> tickerEndurToTomsIdMap.put(x.name(), x.id()));
		
		Map<String, Long> refSourceEndurToTomsIdMap = new HashMap<>();
		references.stream()
			.filter(x -> x.idType() == DefaultReferenceType.REF_SOURCE.getEntity().id())
			.forEach(x -> tickerEndurToTomsIdMap.put(x.name(), x.id()));		

		Map<String, Long> termCurrencyEndurToTomsIdMap = new HashMap<>();
		references.stream()
			.filter(x -> x.idType() == DefaultReferenceType.CCY_CURRENCY.getEntity().id())
			.forEach(x -> termCurrencyEndurToTomsIdMap.put(x.name(), x.id()));		

		
		for (int row = reportData.getRowCount()-1; row >= 0; row--) {
			long tickerId = tickerEndurToTomsIdMap.get(reportData.getString("toms_product", row));
			long refSourceId = refSourceEndurToTomsIdMap.get(reportData.getString("ref_source_name", row));
			long termCurrencyId = termCurrencyEndurToTomsIdMap.get(reportData.getString("settle_currency_id", row));
			
			TickerFxRefSourceRuleTo rule = ImmutableTickerFxRefSourceRuleTo.builder()
					.displayStringIndex(reportData.getString("index_name", row))
					.displayStringTicker(reportData.getString("toms_product", row))
					.displayStringRefSource(reportData.getString("ref_source_name", row))
					.displayStringTermCurrency(reportData.getString("settle_currency", row))
					.idIndex(reportData.getInt ("index_id", row))
					.idTicker(tickerId)
					.idRefSource(refSourceId)
					.idTermCurrency(termCurrencyId)
					.build();
			rules.add(rule);
		}
		return rules;				
	}
}