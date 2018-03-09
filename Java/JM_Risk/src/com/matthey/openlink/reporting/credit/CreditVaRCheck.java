package com.matthey.openlink.reporting.credit;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.limits.AbstractExposureCalculator2;
import com.olf.embedded.limits.ExposureDefinition;
import com.olf.openrisk.application.EnumDebugLevel;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.limits.ExposureLine;
import com.olf.openrisk.market.VaRDefinition;
import com.olf.openrisk.simulation.EnumResultType;
import com.olf.openrisk.simulation.ResultType;
import com.olf.openrisk.simulation.ResultTypes;
import com.olf.openrisk.simulation.RevalResults;
import com.olf.openrisk.simulation.RevalSession;
import com.olf.openrisk.simulation.SimulationFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;

@ScriptCategory({ EnumScriptCategory.CreditRisk })
public class CreditVaRCheck extends AbstractExposureCalculator2<Table, Table> 
{	
	private final static String VAR_DEFINITION_TO_USE = "Metals_1A";
	private final static String JM_CREDIT_VAR_UDSR = "JM Credit VaR Data";
		
	@Override
	public Table createExposureCache(Session session, ExposureDefinition definition) 
	{
		return session.getTableFactory().createTable();
	}
	
	@Override
	public com.olf.embedded.limits.ExposureCalculator2.DealExposure[] calculateDealExposures(
			Session session, ExposureDefinition definition,
			Transaction transaction, Table dealCache) 
	{
		if (session.getDebug().atLeast(EnumDebugLevel.Medium))
		{
			session.getDebug().printLine("CVC: calculateDealExposures called for deal " + transaction.getDealTrackingId());
		}		
		
		double rawExposure = 0.0;
		
		Table clientData = dealCache.createConstView("*","[deal_num] == " + transaction.getDealTrackingId()).asTable();
		clientData.setName("JM CVaR Data Summary");
		
		if ((clientData.getRowCount() > 0) && clientData.isValidColumn("pos_shock_cvar") && clientData.isValidColumn("neg_shock_cvar"))
		{
			rawExposure = Math.max(clientData.getDouble("pos_shock_cvar", 0), clientData.getDouble("neg_shock_cvar", 0));
		}
		
		DealExposure dealExposure = definition.createDealExposure(rawExposure, transaction);
		dealExposure.setClientData(clientData.cloneData());
		
		if (session.getDebug().atLeast(EnumDebugLevel.Medium))
		{
			session.getDebug().printLine("CVC: calculateDealExposures finished for deal " + transaction.getDealTrackingId());
		}		
		
		return new DealExposure[] { dealExposure };		
	}

	@Override
	public Table createDealCache(Session session, ExposureDefinition definition, Transactions transactions) 
	{
		if (session.getDebug().atLeast(EnumDebugLevel.Low))
		{
			session.getDebug().printLine("CVC: createDealCache called for " + transactions.getCount() + " transactions");
		}		
		
		// If it is a quick credit check for fx swap, then tran_num = 0, first one will be near leg
		if (transactions.getCount() == 2 && transactions.getTransactionIds()[0] == 0)
		{
			transactions.get(0).assignTemporaryIds();
		}
				
		EnumTransactionFieldId[] fields = {EnumTransactionFieldId.ExternalBusinessUnit, EnumTransactionFieldId.Toolset};
		Table tblTrans = transactions.asTable(fields);
		tblTrans.setColumnName(tblTrans.getColumnId("Deal Tracking Id"), "deal_num");
		tblTrans.setColumnName(tblTrans.getColumnId("Transaction Id"), "tran_num");
		tblTrans.setColumnName(tblTrans.getColumnId("External Business Unit"), "external_bunit");
		tblTrans.setColumnName(tblTrans.getColumnId("Toolset"), "toolset");
		
		tblTrans.addColumn("pos_shock_cvar", EnumColType.Double);
		tblTrans.addColumn("neg_shock_cvar", EnumColType.Double);
		
		SimulationFactory sf = session.getSimulationFactory();
		RevalSession reval = sf.createRevalSession(transactions);
		
		// Get JM Credit VaR Data
		ResultType cVaRDataResult = sf.createResultType(JM_CREDIT_VAR_UDSR);
		ResultType varGptRawDataResult = sf.createResultType(EnumResultType.VaRGridPointRawData);
		ResultTypes resultTypes = sf.createResultTypes();
		resultTypes.add(cVaRDataResult);
		resultTypes.add(varGptRawDataResult);
		
		// Add "Metals_1A" VaR definition to the Simulation Run
		VaRDefinition varDef = session.getMarketFactory().retrieveVaRDefinition(VAR_DEFINITION_TO_USE);			
		reval.setVaRDefinition(varDef);
		RevalResults results = reval.calcResults(resultTypes);
		
		if (results.contains(cVaRDataResult)) 
		{
			Table cVaRData = results.getResultTable(cVaRDataResult).asTable();
			
			if (cVaRData.isValidColumn("pos_shock_cvar") && cVaRData.isValidColumn("neg_shock_cvar"))
			{
				tblTrans.select(cVaRData, "pos_shock_cvar, neg_shock_cvar", "[IN.deal_num] == [OUT.deal_num]",  "SUM(pos_shock_cvar), SUM(neg_shock_cvar)");
			}			
			else
			{
				//If JM_CREDIT_VAR_UDSR table is blank, then the exposure will be 0
				tblTrans.setColumnValues("pos_shock_cvar", 0.0);
				tblTrans.setColumnValues("neg_shock_cvar", 0.0);				
			}
		} 
		else 
		{
			//If JM_CREDIT_VAR_UDSR does not return anything then the exposure will be 0
			tblTrans.setColumnValues("pos_shock_cvar", 0.0);
			tblTrans.setColumnValues("neg_shock_cvar", 0.0);
		}		
		
		results.dispose();

		if (session.getDebug().atLeast(EnumDebugLevel.Low))
		{
			session.getDebug().printLine("CVC: createDealCache finished for " + transactions.getCount() + " transactions");
		}			
		
		return tblTrans;
	}

	@Override
	public void disposeDealCache(Session session, Table dealCache) 
	{
		dealCache.dispose();
	}

	@Override
	public double aggregateLineExposures(
			Session session,
			ExposureLine line,
			LineExposure[] exposures,
			Table exposureCache, boolean isInquiry) 
	{
		if (session.getDebug().atLeast(EnumDebugLevel.Low))
		{
			session.getDebug().printLine("CVC: aggregateLineExposures called for " + exposures.length + " exposures");
		}		
		
		double posShockVaR = 0.0, negShockVaR = 0.0;
		
		for (LineExposure exposure : exposures) 
		{
			if (session.getDebug().atLeast(EnumDebugLevel.Medium))
			{
				session.getDebug().printLine("CVC: processing deal " + exposure.getDealTrackingId());
			}
			
			ConstTable clientData = exposure.getClientData();
			
			if ((clientData.getRowCount() > 0) && clientData.isValidColumn("pos_shock_cvar") && clientData.isValidColumn("neg_shock_cvar"))
			{
				posShockVaR += clientData.getDouble("pos_shock_cvar", 0);
				negShockVaR += clientData.getDouble("neg_shock_cvar", 0);
			}
		}
		
		if (session.getDebug().atLeast(EnumDebugLevel.Low))
		{
			session.getDebug().printLine("CVC: final positive CVaR - " + posShockVaR + ", negative CVaR - " + negShockVaR);
		}		
		
		double rawExposure = Math.max(posShockVaR, negShockVaR);
		
		if (session.getDebug().atLeast(EnumDebugLevel.Low))
		{
			session.getDebug().printLine("CVC: aggregateLineExposures finished for " + exposures.length + " exposures");
		}		
		
		return rawExposure;
	}
}