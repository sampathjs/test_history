package com.olf.jm.credit;
import java.util.Date;

/*File Name:                    JM_Credit_Limit_AR_MTM.java

Author:                         Guillaume Cortade

Date Of Last Revision:  

Script Type:                    Main - Process
Parameter Script:               None 
Display Script:                 None

Toolsets script applies:        All?

Type of Script:                 Credit batch, deal, update or ad-hoc report

History
7-Jan-2019  G Evenson   Updates to script for:
						- Trap exception thrown by AR Web Service and Log
						- Modify deal cache processing to exclude matured FX/Cash
						- Modify deal processing to allow null entry in deal cache
						- Exclude zero AR Balances						
						
23-Jan-2019 G Evenson   Add support for Shanghai based entities trading in CNY

*/


import com.matthey.webservice.consumer.FinancialService;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.limits.AbstractExposureCalculator2;
import com.olf.embedded.limits.ExposureDefinition;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.Str;
import com.olf.openjvs.Util;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.limits.ConstField;
import com.olf.openrisk.limits.EnumRiskCriteria;
import com.olf.openrisk.limits.ExposureLine;
import com.olf.openrisk.limits.Field;
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
import com.olf.openrisk.trading.EnumCashflowType;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumProfileFieldId;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Profile;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;


@ScriptCategory({ EnumScriptCategory.CreditRisk })
public class JM_Credit_Limit_AR_MTM extends AbstractExposureCalculator2<Table, Table> {
	

	
	String TRAN_INFO_SENT_TO_JDE = "General Ledger";
	String FIRST_MATURITY_BUCKET = "Month 01";
	private final String PartyInfoPartyCodeUK = "Party Code UK";
	private final String PartyInfoPartyCodeUS = "Party Code US";
	private final String PartyInfoPartyCodeHK = "Party Code HK";
	private final String PartyInfoPartyCodeCN = "Party Code CN - Debtor";
	
	//Cache the maturity bucket and only load once
	Table maturityBuckets = null;
	String error_log_file = null;
	
	@Override
	public Table createExposureCache(Session session,
			ExposureDefinition definition) {	
		IOFactory iof = session.getIOFactory();
		// Get Available Exposure type from the definition
		Table temp = iof.runSQL("SELECT available_exposure_type from rsk_exposure_defn where exp_defn_id = " + definition.getId());
		temp.getInt("available_exposure_type", 0);
		return temp;
//		return Integer.valueOf(temp.getInt("available_exposure_type", 0));
	}
	
	@Override
	public com.olf.embedded.limits.ExposureCalculator2.DealExposure[] calculateDealExposures(
			Session session, ExposureDefinition definition,
			Transaction transaction, Table dealCache) {
		double rowExposure = 0.0;
		//Date today = session.getTradingDate();
		int dealNum = transaction.getDealTrackingId();		
		Table clientData = dealCache.createConstView("*","[deal_num] == " + transaction.getDealTrackingId()).asTable();
		if (clientData != null && clientData.getRowCount() > 0)
		{
			clientData.setName("MTM Detailed WorkSheet");
			
			//Date metalSettleDate = clientData.getTable("metal_settlement_table", 0).getDate("metal_settlement_date", 0);
			//String send2JDE = clientData.getString("send_to_jde", 0);
			
			// If status = 'Sent' and metal settlement date <= today
			//if (send2JDE.equalsIgnoreCase("SENT") && metalSettleDate.compareTo(today) <= 0){
			//	rowExposure = 0;
			//}
			//else{			
				rowExposure = clientData.getDouble("base_mtm", 0);
			//
		}
		
		// remove any temporary ID at this stage
		if (dealNum < 0)
			transaction.clearTemporaryIds();
		
        if (definition.hasCriteriaField(EnumRiskCriteria.MaturityBucket)) {
        	Field[] fields = definition.getCriteriaFields(transaction);
        	String origBucket = "";
        	int fieldIndex = 0;
        	
        	// Find the original bucket value
			for (int i = 0; i < fields.length; i++) {
				if (fields[i].getCriteriaType().getId() == EnumRiskCriteria.MaturityBucket.getValue()) {
					fieldIndex = i;
					origBucket = fields[i].getValueAsString();
				}
			}
			
        	// Workaround for a core system bug: ComSwap, LoanDep maturity date remains the date on the template in quick credit check
			if (transaction.getToolset() == EnumToolset.ComSwap || transaction.getToolset() == EnumToolset.Loandep) {
				Date maturityDate = transaction.getLeg(0).getValueAsDate(EnumLegFieldId.MaturityDate);
				String bucketName = getBucketName(maturityDate);
				if (!bucketName.equalsIgnoreCase(origBucket)) {
					fields[fieldIndex].setValue(bucketName);
				}
			} 
        	
			if (clientData != null)
				clientData.dispose();
			
			DealExposure dealExposure = definition.createDealExposure(rowExposure, transaction, fields);
			
            // Put the exposure on the last bucket, but always create a exposure on the first bucket
			if (!fields[fieldIndex].getValueAsString().equalsIgnoreCase(FIRST_MATURITY_BUCKET)) {
				// Create a dummy deal exposure on the first maturity bucket
				fields[fieldIndex].setValue(FIRST_MATURITY_BUCKET);
				DealExposure dealExposureMonth1 = definition.createDealExposure(0.0, transaction, fields);
				return new DealExposure[] { dealExposure, dealExposureMonth1};
			} else {
				return new DealExposure[] { dealExposure };
			}
        } else {
    		DealExposure dealExposure = definition.createDealExposure(rowExposure, transaction);
    		if (clientData != null)
    			dealExposure.setClientData(clientData);
    		return new DealExposure[] {dealExposure};
        }
		
	}

	@Override
	public Table createDealCache(Session session,
			ExposureDefinition definition, Transactions transactions) 
	{
		Logging.init(session, this.getClass(), "JM_Credit_Limit_AR_MTM", "");
		Logging.info( "Start Create Deal Cache");
		
		// If it is a quick credit check for fx swap, then tran_num = 0, first one will be near leg
		if (transactions.getCount() == 2 && transactions.getTransactionIds()[0] == 0){
			transactions.get(0).assignTemporaryIds();
		}
		
		// Get available buckets
		if (maturityBuckets == null) {
			IOFactory iof = session.getIOFactory();
			CalendarFactory cf = session.getCalendarFactory();
			maturityBuckets = iof.runSQL("select * from rsk_maturity_buckets");
			maturityBuckets.addColumns("start_date", EnumColType.Date);
			maturityBuckets.addColumns("end_date", EnumColType.Date);
			for (TableRow row : maturityBuckets.getRows()) {
				row.getCell("start_date").setDate(cf.createSymbolicDate(row.getString("start_datestr")).evaluate());
				row.getCell("end_date").setDate(cf.createSymbolicDate(row.getString("end_datestr")).evaluate());
			}
		}
		
		Date today = session.getTradingDate();
		EnumTransactionFieldId[] fields = {EnumTransactionFieldId.ExternalBusinessUnit, EnumTransactionFieldId.Toolset, EnumTransactionFieldId.InstrumentType, EnumTransactionFieldId.MaturityDate};
		Table tblTrans = transactions.asTable(fields);
		
		tblTrans.setColumnName(tblTrans.getColumnId("Deal Tracking Id"), "deal_num");
		tblTrans.setColumnName(tblTrans.getColumnId("Transaction Id"), "tran_num");
		tblTrans.setColumnName(tblTrans.getColumnId("External Business Unit"), "external_bunit");
		tblTrans.setColumnName(tblTrans.getColumnId("Toolset"), "toolset");
		tblTrans.setColumnName(tblTrans.getColumnId("Instrument Type"), "ins_type");
		tblTrans.setColumnName(tblTrans.getColumnId("Maturity Date"), "maturity_date");
		
		// remove all rows for FX and Cash Deals with a maturity date < today to improve memory usage
		for (int i=tblTrans.getRowCount()-1; i>=0; i--)
		{
			if (tblTrans.getInt("deal_num", i) > 0)
			{
				int toolset = tblTrans.getInt("toolset", i);
				Date matDate = tblTrans.getDate("maturity_date", i);
				if (matDate != null && matDate.compareTo(today) < 0 && (toolset == EnumToolset.Fx.getValue() || toolset == EnumToolset.Cash.getValue()))
					tblTrans.removeRow(i);
			}
		}
		
		//session.getDebug().viewTable(tblTrans);
		
		Transactions copyTrans = session.getTradingFactory().createTransactions();
		boolean commPhysDetected = false;
		// Remove any COMM-PHYS deals from the simulation
		for (int tranCount=0; tranCount<tblTrans.getRowCount(); tranCount++) {
			Transaction tran = transactions.getTransaction(tblTrans.getInt("deal_num", tranCount));
			String insType = tran.getInstrumentTypeObject().getName().toString();	
			// if not comm-phys, safe to add to reval
			if (!insType.isEmpty() && insType.equalsIgnoreCase("COMM-PHYS") == false) {
				copyTrans.add(tran);							
			} else if (!insType.isEmpty() && insType.equalsIgnoreCase("COMM-PHYS") == true) {
				commPhysDetected = true;	
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
		Table tbl = session.getTableFactory().createTable("Base MTM Table");
		if (results.contains(EnumResultType.CashflowByDay)) {
			Table cflowByDay = results.getResultTable(EnumResultType.CashflowByDay).asTable();
			cflowByDay.select(tblTrans, "toolset", "[IN.deal_num] == [OUT.deal_num]");
			tbl.selectDistinct(cflowByDay, "deal_num", "cflow_date >= 0");
			String strCondition = "[IN.deal_num] == [OUT.deal_num] AND [IN.toolset] != " 
			                    + EnumToolset.Loandep.getValue() 
			                    + " AND [IN.cflow_date] >= " + session.getCalendarFactory().getJulianDate(today);
			tbl.select(cflowByDay, "deal_num, base_discounted_cflow", strCondition, "SUM(base_discounted_cflow)");
			tbl.setColumnName(1, "base_mtm");
			strCondition = "[IN.deal_num] == [OUT.deal_num] AND [IN.cflow_type] == " 
						 + EnumCashflowType.FinalPrincipal.getValue() + " AND [In.toolset] == " + EnumToolset.Loandep.getValue();
			tbl.select(cflowByDay, "deal_num, base_cflow->base_mtm", strCondition);
			tblTrans.select(tbl, "base_mtm", "[IN.deal_num] == [OUT.deal_num]");
		} else {
			Logging.info("No Sim Result Returned for Cash Flow By Day \n");
			//If cash flow by day does not return anything then the exposure will be 0
			tblTrans.addColumn("base_mtm", EnumColType.Double);				
			tblTrans.setColumnValues("base_mtm", 0.0);
		}
		
		// This a fudge to ensure the MTMM is adjusted for COMM-PHYS trades ONLY. 
		// The increment is needed else the internal credit check will not fire as the MTM change is 0.
		if (commPhysDetected == true) {
			Table commPhysTable = session.getTableFactory().createTable();
			tblTrans.copyTo(commPhysTable);
			commPhysTable.setColumnValues("base_mtm", 0.001);
			String condition = "[OUT.deal_num] == [IN.deal_num] AND [IN.ins_type] == 48010";
			tblTrans.select(commPhysTable, "base_mtm", condition);
			commPhysTable.dispose();
		}
		tblTrans.removeColumn("ins_type");
		tblTrans.removeColumn("maturity_date");
		results.dispose();
		tbl.dispose();
		copyTrans.dispose();
		
		tblTrans.addColumn("send_to_jde", EnumColType.String);
		tblTrans.addColumn("metal_settlement_table", EnumColType.Table);
		for (int row = 0; row < tblTrans.getRowCount(); row++) {
			Transaction tran = transactions.getTransaction(tblTrans.getInt("deal_num", row));
			tblTrans.setString("send_to_jde", row, tran.getField(TRAN_INFO_SENT_TO_JDE).getValueAsString());
			// include CASH deals causes out of memory error
			if (tran.getToolset() != EnumToolset.Cash)
			{
				tblTrans.setTable("metal_settlement_table", row, GetMetalSettleDateFromTran(session, tran));
			}
		}
		
		Logging.info( "End Create Deal Cache");
		Logging.close();
		return tblTrans;
	}

	@Override
	public void disposeDealCache(Session session, Table dealCache) {
		dealCache.dispose();
	}

	@Override
	public double aggregateLineExposures(
			Session session,
			ExposureLine line,
			LineExposure[] exposures,
			Table exposureCache, boolean isInquiry) {
		double arAmount = 0.0;

		InitLogFile();
		Logging.init(session, this.getClass(), "JM_Credit_Limit_AR_MTM", "");
		Logging.info( "Start Aggegate Line Exposures");
		
		//If maturity buckets are defined, only set AR amount if it is the first month
		boolean hasMaturityBuckets = false;
		for (ConstField field : line.getCriteriaFields()){
			if (field.getCriteriaType().getId() == EnumRiskCriteria.MaturityBucket.getValue()){
				hasMaturityBuckets = true;
				if (field.getValueAsString().equalsIgnoreCase(FIRST_MATURITY_BUCKET)) {
					arAmount = getARAmount(session, line);
					break;
				}
			}
		}
		//If no maturity buckets defined, then take the AR amount
		if(!hasMaturityBuckets){
			arAmount = getARAmount(session, line);
		}
		
		double mtm = aggregateMTM(line, exposures);
		double rawExposure = 0.0;
		if (arAmount > 0) {
			rawExposure = mtm + arAmount;
		} else {
			rawExposure = mtm;
		}
		
//		if (exposureCache.intValue() == 0 && rawExposure < 0){
		if (exposureCache.getInt(0, 0) == 0 && rawExposure < 0){
			rawExposure = 0;
		}
		
		Logging.info( "End Aggegate Line Exposures");
		Logging.close();
		return rawExposure;
	}

	private double aggregateMTM(ExposureLine line, LineExposure[] exposures) {
		double d = 0.0;
		boolean useNetting = line.getFacility().getDefinition().useNetting();
		for (LineExposure exposure : exposures) {
			//PrintLog("aggregateMTM", "Aggregate MTM: " + exposure.getDealTrackingId() + " = " + exposure.getRawExposure());
			double rawExposure = exposure.getRawExposure();
			if (useNetting) {
				d += rawExposure;
			} else {
				if (rawExposure > 0) {
					d += rawExposure;
				}
			}
		}
		return d;
	}

	private double getARAmount(Session session, ExposureLine line) {
		double arAmount = 0.0;		
		String legalName = "";
		String partyCodeUK = "", partyCodeUS = "", partyCodeHK = "", partyCodeCN = "";
		try
		{
			String sql = "";
			StaticDataFactory sdf = session.getStaticDataFactory();
			ConstField[] fields = line.getCriteriaFields();
			for (ConstField field : fields) {
				if (field.getCriteriaType().getId() == EnumRiskCriteria.ExtBunit.getValue()) {
					legalName = field.getValueAsString();
					int extBunit = sdf.getId(EnumReferenceTable.Party, field.getValueAsString());
					// Get party info fields to find out account numbers
				    sql = "SELECT p.short_name, p.party_id, uk.value party_code_uk, us.value party_code_us, hk.value party_code_hk, cn.value party_code_cn\n"
					   + "FROM party p \n"
					   + "LEFT OUTER JOIN \n"
					   + "(SELECT * FROM party_info_view where type_name = '" + PartyInfoPartyCodeUK + "' AND party_id = " + extBunit + ") uk ON uk.party_id = p.party_id \n"
					   + "LEFT OUTER JOIN \n"
					   + "(SELECT * FROM party_info_view where type_name = '" + PartyInfoPartyCodeUS + "' AND party_id = " + extBunit + ") us ON us.party_id = p.party_id \n"
					   + "LEFT OUTER JOIN \n"
					   + "(select * FROM party_info_view where type_name = '" + PartyInfoPartyCodeHK + "' AND party_id = " + extBunit + ") hk ON hk.party_id = p.party_id \n"
					   + "LEFT OUTER JOIN \n"
					   + "(select * FROM party_info_view where type_name = '" + PartyInfoPartyCodeCN + "' AND party_id = " + extBunit + ") cn ON cn.party_id = p.party_id \n"
					   + "WHERE p.party_id = " + extBunit;
					break;
				} else if (field.getCriteriaType().getId() == EnumRiskCriteria.ExtLentity.getValue()) {
					String extLentity = field.getValueAsString();
					LegalEntity le = (LegalEntity)sdf.getReferenceObject(EnumReferenceObject.LegalEntity, extLentity);
					legalName = le.getName();
				    sql = "SELECT p.short_name, p.party_id, uk.value party_code_uk, us.value party_code_us, hk.value party_code_hk, cn.value party_code_cn\n"
					   + "FROM party_relationship r, party p \n"
					   + "LEFT OUTER JOIN \n"
					   + "(SELECT * FROM party_info_view where type_name = '" + PartyInfoPartyCodeUK + "') uk ON uk.party_id = p.party_id \n"
					   + "LEFT OUTER JOIN \n"
					   + "(SELECT * FROM party_info_view where type_name = '" + PartyInfoPartyCodeUS + "') us ON us.party_id = p.party_id \n"
					   + "LEFT OUTER JOIN \n"
					   + "(select * FROM party_info_view where type_name = '" + PartyInfoPartyCodeHK + "') hk ON hk.party_id = p.party_id \n"
					   + "LEFT OUTER JOIN \n"
					   + "(select * FROM party_info_view where type_name = '" + PartyInfoPartyCodeCN + "') cn ON cn.party_id = p.party_id \n"					   
					   + "WHERE p.party_id = r.business_unit_id\n" 
					   + "AND r.legal_entity_id = " + le.getId();
					break;
				}
			}
			
			Table buList = session.getIOFactory().runSQL(sql);
			// get the definition reporting currency
			Currency reportingCurrency = line.getFacility().getDefinition().getCurrency();					

			for (int row=0; row<buList.getRowCount(); row++) {				
				Table arDetails = /*session.getTableFactory().createTable("ar_details");
				arDetails.addColumns("String[currency],Double[amount],Double[fx_spot_rate],Double[base_amount]")*/FinancialService.getOpenItems(session, "roy",null);
				// Get Party Info
				String partyName = buList.getString("short_name", row);
				partyCodeUK = buList.getString("party_code_uk", row);
				partyCodeUS = buList.getString("party_code_us", row);
				partyCodeHK = buList.getString("party_code_hk", row);
				partyCodeCN = buList.getString("party_code_cn", row);
				PrintLog("aggregateLineExposures", "Getting AR Balance for (" + partyName + "): UK:" + partyCodeUK + "/US:" + partyCodeUS + "/HK:" + partyCodeHK + "/CN:" + partyCodeCN);
				
				try
				{
					if (!partyCodeUK.isEmpty()) {
						Table arUK = FinancialService.getOpenItems(session, "roy", partyCodeUK);
						arDetails.appendRows(arUK);
						for (int i=0; i<arUK.getRowCount(); i++)
							PrintLog("aggregateLineExposures", "AR Balance for (" + partyName + "): UK:" + partyCodeUK + " [" + i + "] = " + Str.formatAsNotnl(arUK.getDouble(FinancialService.VALUE, i), 20, 0));
						arUK.dispose();
					}
				}
				catch (Exception e)
				{
					PrintLog("aggregateLineExposures", "Error getting AR Balance for (" + partyName + "): UK:" + partyCodeUK + " : " + e.getMessage());
					Logging.error("Error getting AR Balance for (" + partyName + "): UK:" + partyCodeUK, e);
				}
				
				try
				{
					if (!partyCodeUS.isEmpty()) {
						Table arUS = FinancialService.getOpenItems(session, "vfc", partyCodeUS);
						arDetails.appendRows(arUS);
						for (int i=0; i<arUS.getRowCount(); i++)
							PrintLog("aggregateLineExposures", "AR Balance for (" + partyName + "): US:" + partyCodeUS + " [" + i + "] = " + Str.formatAsNotnl(arUS.getDouble(FinancialService.VALUE, i), 20, 0));
						arUS.dispose();
					}
				}
				catch (Exception e)
				{
					PrintLog("aggregateLineExposures", "Error getting AR Balance for (" + partyName + "): US:" + partyCodeUS + " : " + e.getMessage());
					Logging.error( "Error getting AR Balance for (" + partyName + "): US:" + partyCodeUS, e);
				}
				
				try
				{
					if (!partyCodeHK.isEmpty()) {
						Table arHK = FinancialService.getOpenItems(session, "hgk", partyCodeHK);
						arDetails.appendRows(arHK);
						for (int i=0; i<arHK.getRowCount(); i++)
							PrintLog("aggregateLineExposures", "AR Balance for (" + partyName + "): HK:" + partyCodeHK + " [" + i + "] = " + Str.formatAsNotnl(arHK.getDouble(FinancialService.VALUE, i), 20, 0));
						arHK.dispose();
					}
				}
				catch (Exception e)
				{
					PrintLog("aggregateLineExposures", "Error getting AR Balance for (" + partyName + "): HK:" + partyCodeHK + " : " + e.getMessage());
					Logging.error("Error getting AR Balance for (" + partyName + "): HK:" + partyCodeHK, e);
				}
				
				try
				{
					if (!partyCodeCN.isEmpty()) {
						Table arCN = FinancialService.getOpenItems(session, "Shanghai", partyCodeCN);
						arDetails.appendRows(arCN);
						for (int i=0; i<arCN.getRowCount(); i++)
							PrintLog("aggregateLineExposures", "AR Balance for (" + partyName + "): CN:" + partyCodeCN + " [" + i + "] = " + Str.formatAsNotnl(arCN.getDouble(FinancialService.VALUE, i), 20, 0));
						arCN.dispose();
					}
				}
				catch (Exception e)
				{
					PrintLog("aggregateLineExposures", "Error getting AR Balance for (" + partyName + "): HK:" + partyCodeHK + " : " + e.getMessage());
					Logging.error("Error getting AR Balance for (" + partyName + "): HK:" + partyCodeHK, e);
				}
				
				// Get FX rates to reporting currency of Defintion
				//Currency usd = (Currency) sdf.getReferenceObject(EnumReferenceObject.Currency, "USD");
				for (int i = 0; i < arDetails.getRowCount(); i++) {
					arDetails.setDouble(FinancialService.BASE_VALUE, i, 0.0);
					Double balance = arDetails.getDouble(FinancialService.VALUE, i);
					// only include +ve balances
					if (balance > 0.0)
					{
						String ccyName = arDetails.getString(FinancialService.CURRENCY, i);
						Currency ccy = (Currency) sdf.getReferenceObject(EnumReferenceObject.Currency, ccyName.equals("RMB") ? "CNY" : ccyName);
						try {
							// Get fx conversion rate to reporting currency
							double rate = session.getMarket().getFXRate(ccy, reportingCurrency, session.getTradingDate());
							arDetails.setDouble(FinancialService.SPOT_RATE, i, rate);
							arDetails.setDouble(FinancialService.BASE_VALUE, i, rate * balance);
						} catch (Exception e) {
							Logging.error( "FX rate for currency " + ccy.getName() + "-> " + reportingCurrency.getName() + " not set. \n", e);
							PrintLog("aggregateLineExposures", "FX rate for currency " + ccy.getName() + "-> " + reportingCurrency.getName() + " not set");
						}
					}
				}
	
				double bunitTotal = arDetails.calcAsDouble(arDetails.getColumnId(FinancialService.BASE_VALUE), EnumColumnOperation.Sum);
				PrintLog("aggregateLineExposures", "Final " + reportingCurrency.getName() + " AR Balance for (" + partyName + ") = " + Str.formatAsNotnl(bunitTotal, 20, 0));
				arAmount += bunitTotal;
			}
			
			PrintLog("aggregateLineExposures", "Final " + reportingCurrency.getName() + " AR Balance for (" + legalName + ") = " + Str.formatAsNotnl(arAmount, 20, 0));
			
			buList.dispose();
		}
		catch (Exception e)
		{
			PrintLog("aggregateLineExposures", "Error getting AR Balance for (" + legalName + "): UK:" + partyCodeUK + "/US:" + partyCodeUS + "/HK:" + partyCodeHK + " : " + e.getMessage());
			Logging.error("Error getting AR Balance for (" + legalName + "): UK:" + partyCodeUK + "/US:" + partyCodeUS + "/HK:" + partyCodeHK, e);
		}
		
		return arAmount;
	}
	
	
	
	private String getBucketName(Date maturityDate) {
		if (maturityBuckets == null) {
			return FIRST_MATURITY_BUCKET;
		}
		
		for (TableRow row : maturityBuckets.getRows()) {
			if (row.getDate("start_date").compareTo(maturityDate) <= 0 && row.getDate("end_date").compareTo(maturityDate) >= 0) {
				return row.getString("name");
			}
		}
		return FIRST_MATURITY_BUCKET;
	}
	
	private Table GetMetalSettleDateFromTran(Session session, Transaction tran) {
		StaticDataFactory sdf = session.getStaticDataFactory();
		Table metalSettlements = session.getTableFactory().createTable("Metal Settlement Table");
		metalSettlements.addColumn("metal_settlement_date", EnumColType.Date);
	    Date settleDate = session.getCalendarFactory().createDate(1970, 0, 1);
		if (tran.getToolset() == EnumToolset.Fx) {
			String cFlowType = tran.getField(EnumTransactionFieldId.CashflowType).getValueAsString();
			String currencyPair = tran.getField(EnumTransactionFieldId.CurrencyPair).getValueAsString();
			String[] ccys = currencyPair.split("/");
			Currency currency1 = (Currency)sdf.getReferenceObject(EnumReferenceObject.Currency, currencyPair.substring(0,3));
			Currency currency2 = (Currency)sdf.getReferenceObject(EnumReferenceObject.Currency, ccys[1].substring(0,3));
			
			if (cFlowType.contains("Swap")) {
				settleDate = tran.getField(EnumTransactionFieldId.SettleDate).getValueAsDate();
			}
		    else {
				if (currency1.isPreciousMetal()){
					settleDate = tran.getField(EnumTransactionFieldId.SettleDate).getValueAsDate();
				} else if (currency2.isPreciousMetal()){
					settleDate = tran.getField(EnumTransactionFieldId.FxTermSettleDate).getValueAsDate();
				}
			}
		} else if (tran.getToolset() == EnumToolset.ComSwap){
			for (Leg leg : tran.getLegs()) {
				String ccy = leg.getField(EnumLegFieldId.Currency).getValueAsString();
				Currency currency = (Currency)sdf.getReferenceObject(EnumReferenceObject.Currency, ccy);
				if (currency.isPreciousMetal()){
					for (Profile p : leg.getProfiles()){
						metalSettlements.addRow();
						settleDate = p.getField(EnumProfileFieldId.PaymentDate).getValueAsDate();
						metalSettlements.setDate("metal_settlement_date", metalSettlements.getRowCount() - 1, settleDate);
					}
					break;
				}
			}
		} else {
			settleDate = tran.getField(EnumTransactionFieldId.MaturityDate).getValueAsDate();
		}
		
		if (metalSettlements.getRowCount() == 0) {
			metalSettlements.addRow();
			metalSettlements.setDate("metal_settlement_date", 0, settleDate);
		}
		
		return metalSettlements;
	}
	
	private void InitLogFile() 
	{
		String sFileName = "JM_Credit_Limit_AR_MTM";
		try
		{
			if (error_log_file == null)
				error_log_file = Util.errorInitScriptErrorLog(sFileName);
		}
		catch (Exception e)
		{
			Logging.error( "Error initialising script log:" + e.getMessage(),e);
		}
	}
	
	private void PrintLog(String sKeyword, String sMessage)
	{
		try
		{
			if (error_log_file != null)
			{
				OConsole.oprint("\n" + sMessage);
				Util.errorLogMessage(error_log_file, sKeyword, sMessage);
				Util.scriptPostStatus(sMessage);
			}
		}
		catch (Exception e)
		{
			Logging.error("Error initialising script log:" + e.getMessage(),e);
		}	
	}
	
}
