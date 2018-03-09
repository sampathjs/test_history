package com.olf.jm.credit;
import java.util.Date;

import com.matthey.webservice.consumer.FinancialService;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.limits.AbstractExposureCalculator2;
import com.olf.embedded.limits.ExposureDefinition;
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
import com.openlink.endur.utilities.logger.LogCategory;
import com.openlink.endur.utilities.logger.LogLevel;
import com.openlink.endur.utilities.logger.Logger;

@ScriptCategory({ EnumScriptCategory.CreditRisk })
public class JM_Credit_Limit_AR_MTM extends AbstractExposureCalculator2<Table, Table> {
	

	
	String TRAN_INFO_SENT_TO_JDE = "General Ledger";
	String FIRST_MATURITY_BUCKET = "Month 01";
	//Cache the maturity bucket and only load once
	Table maturityBuckets = null;
	
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
		double rowExposure;
		Date today = session.getTradingDate();
		Table clientData = dealCache.createConstView("*","[deal_num] == " + transaction.getDealTrackingId()).asTable();
		clientData.setName("MTM Detailed WorkSheet");
		
		Date metalSettleDate = clientData.getTable("metal_settlement_table", 0).getDate("metal_settlement_date", 0);
		String send2JDE = clientData.getString("send_to_jde", 0);
		
		// If status = 'Sent' and metal settlement date <= today
		if (send2JDE.equalsIgnoreCase("SENT") && metalSettleDate.compareTo(today) <= 0){
			rowExposure = 0;
		}
		else{
			rowExposure = clientData.getDouble("base_mtm", 0);
		}

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
        	
			DealExposure dealExposure = definition.createDealExposure(rowExposure, transaction, fields);
			clientData.dispose();
			
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
    		dealExposure.setClientData(clientData);
    		return new DealExposure[] {dealExposure};
        }
		
	}

	@Override
	public Table createDealCache(Session session,
			ExposureDefinition definition, Transactions transactions) 
	{
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
		EnumTransactionFieldId[] fields = {EnumTransactionFieldId.ExternalBusinessUnit, EnumTransactionFieldId.Toolset, EnumTransactionFieldId.InstrumentType};
		Table tblTrans = transactions.asTable(fields);
		
		tblTrans.setColumnName(tblTrans.getColumnId("Deal Tracking Id"), "deal_num");
		tblTrans.setColumnName(tblTrans.getColumnId("Transaction Id"), "tran_num");
		tblTrans.setColumnName(tblTrans.getColumnId("External Business Unit"), "external_bunit");
		tblTrans.setColumnName(tblTrans.getColumnId("Toolset"), "toolset");
		tblTrans.setColumnName(tblTrans.getColumnId("Instrument Type"), "ins_type");
		
		Transactions copyTrans = session.getTradingFactory().createTransactions();
		boolean commPhysDetected = false;
		// Remove any COMM-PHYS deals from the simulation
		for (int tranCount=0; tranCount<transactions.getCount(); tranCount++) {
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
			Logger.log/*PluginLog.error(*/(LogLevel.ERROR, LogCategory.Trading, this,"No Sim Result Returned for Cash Flow By Day \n");
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
		results.dispose();
		tbl.dispose();
		copyTrans.dispose();
		
		tblTrans.addColumn("send_to_jde", EnumColType.String);
		tblTrans.addColumn("metal_settlement_table", EnumColType.Table);
		
		for (int row = 0; row < tblTrans.getRowCount(); row++) {
			Transaction tran = transactions.getTransaction(tblTrans.getInt("deal_num", row));
			tblTrans.setString("send_to_jde", row, tran.getField(TRAN_INFO_SENT_TO_JDE).getValueAsString());
			tblTrans.setTable("metal_settlement_table", row, GetMetalSettleDateFromTran(session, tran));
		}
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
		return rawExposure;
	}

	private double aggregateMTM(ExposureLine line, LineExposure[] exposures) {
		double d = 0.0;
		boolean useNetting = line.getFacility().getDefinition().useNetting();
		for (LineExposure exposure : exposures) {
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
		int extBunit = 0;
		StaticDataFactory sdf = session.getStaticDataFactory();
		ConstField[] fields = line.getCriteriaFields();
		for (ConstField field : fields) {
			if (field.getCriteriaType().getId() == EnumRiskCriteria.ExtBunit.getValue()) {
				extBunit = sdf.getId(EnumReferenceTable.Party, field.getValueAsString());
				break;
			} else if (field.getCriteriaType().getId() == EnumRiskCriteria.ExtLentity.getValue()) {
				String extLentity = field.getValueAsString();
				LegalEntity le = (LegalEntity)sdf.getReferenceObject(EnumReferenceObject.LegalEntity, extLentity);
				extBunit = le.getBusinessUnits(true)[0].getId();
				break;
			}
		}
		// Get party info fields to find out account numbers
		String sql = "SELECT " + extBunit + " external_bunit, uk.value party_code_uk, us.value party_code_us, hk.value party_code_hk \n"
		   + "FROM \n"
		   + "(SELECT * FROM party_info_view where type_name = 'Party Code UK' AND party_id = " + extBunit + ") uk \n"
		   + "FULL OUTER JOIN \n"
		   + "(SELECT * FROM party_info_view where type_name = 'Party Code US' AND party_id = " + extBunit + ") us ON uk.party_id = us.party_id \n"
		   + "FULL OUTER JOIN \n"
		   + "(select * FROM party_info_view where type_name = 'Party Code HK' AND party_id = " + extBunit + ") hk ON uk.party_id = hk.party_id \n";
		Table buList = session.getIOFactory().runSQL(sql);
		
		if (buList.getRowCount() > 0) {
			Table arDetails = /*session.getTableFactory().createTable("ar_details");
			arDetails.addColumns("String[currency],Double[amount],Double[fx_spot_rate],Double[base_amount]")*/FinancialService.getOpenItems(session, "roy",null);
			// Get Party Info
			String partyCodeUK = buList.getString("party_code_uk", 0);
			String partyCodeUS = buList.getString("party_code_us", 0);
			String partyCodeHK = buList.getString("party_code_hk", 0);
			if (!partyCodeUK.isEmpty()) {
				Table arUK = FinancialService.getOpenItems(session, "roy", partyCodeUK);
				arDetails.appendRows(arUK);
				arUK.dispose();
			}
			if (!partyCodeUS.isEmpty()) {
				Table arUS = FinancialService.getOpenItems(session, "vfc", partyCodeUS);
				arDetails.appendRows(arUS);
				arUS.dispose();
			}
			if (!partyCodeHK.isEmpty()) {
				Table arHK = FinancialService.getOpenItems(session, "hgk", partyCodeHK);
				arDetails.appendRows(arHK);
				arHK.dispose();
			}
			// Get FX rates
			Currency usd = (Currency) sdf.getReferenceObject(EnumReferenceObject.Currency, "USD");
			for (int i = 0; i < arDetails.getRowCount(); i++) {
				Currency ccy = (Currency) sdf.getReferenceObject(EnumReferenceObject.Currency, arDetails.getString(FinancialService.CURRENCY, i));
				try {
					double rate = session.getMarket().getFXRate(ccy, usd, session.getTradingDate());
					arDetails.setDouble(FinancialService.SPOT_RATE, i, rate);
					arDetails.setDouble(FinancialService.BASE_VALUE, i, rate * arDetails.getDouble(FinancialService.VALUE, i));
				} catch (Exception e) {
					Logger.log(LogLevel.ERROR, LogCategory.Trading, this, "FX rate for currency " + ccy.getName() + "-> USD not set. \n", e);
				}
			}

			arAmount = arDetails.calcAsDouble(arDetails.getColumnId(FinancialService.BASE_VALUE), EnumColumnOperation.Sum);
		}
		
		buList.dispose();
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

}
