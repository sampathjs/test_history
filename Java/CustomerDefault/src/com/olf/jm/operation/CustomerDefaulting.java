package com.olf.jm.operation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractFieldListener;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.staticdata.Currency;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumFeeFieldId;
import com.olf.openrisk.trading.EnumInsSub;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTranfField;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Fee;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.constrepository.ConstantNameException;
import com.openlink.util.constrepository.ConstantTypeException;
import com.openlink.util.logging.PluginLog;

@ScriptCategory({ EnumScriptCategory.OpsSvcTranfield })
public class CustomerDefaulting extends AbstractFieldListener {

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "OpsService";
	
	/** The Constant SUBCONTEXT used to identify entries in the const repository.. */
	public static final String SUBCONTEXT = "CustomerDefaulting";
	
	
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception the exception
	 */
	private void init() throws Exception {
		constRep = new ConstRepository(CONTEXT, SUBCONTEXT);

		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			if (logDir == null) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}

	}
	
	@Override
	public void postProcess(Session session, Field field, String oldValue, String newValue, Table clientData) {
		
		try {
			init();
		} catch (Exception e) {
			String errorMessage = "Error initialising the logging. " + e.getMessage();
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		
		Transaction tran = field.getTransaction();
		EnumToolset toolset = tran.getToolset();
		StaticDataFactory sdf = session.getStaticDataFactory();
		IOFactory iof = session.getIOFactory();

		//If the script is triggered by a transaction info field
    	if (field.isUserDefined()) {
    		if (toolset == EnumToolset.ComSwap) {
    			setDefaultSpread(iof, tran);
    		}
    	}
    	else if (field.getTranfId() == EnumTranfField.ProjIndex || 
    			 field.getTranfId() == EnumTranfField.Ticker    || 
    			 field.getTranfId() == EnumTranfField.CurrencyPair) {
			setDefaultFormAndLoco(session, tran);
			if (toolset == EnumToolset.ComSwap){
				setDefaultFixLegCcy(sdf, tran, field.getValueAsString());
			}
    		setDefaultEndUser(session, tran);
			setDefaultSettlementDates(session, tran);
		} 
		else if (field.getTranfId() == EnumTranfField.ExternalBunit){
			setDefaultFormAndLoco(session, tran);
    		setDefaultEndUser(session, tran);
			setDefaultSettlementDates(session, tran);
		}
		else if (field.getTranfId() == EnumTranfField.InternalBunit){
			setDefaultFormAndLoco(session, tran);
		}
		else if (field.getTranfId() == EnumTranfField.IdxSubgroup || 
				 field.getTranfId() == EnumTranfField.Location){
			if (toolset == EnumToolset.Commodity) {
				setDefaultCommPhysFields(iof, tran);
				setDefaultSettlementDates(session, tran);
    		}
		}
		else if (field.getId() == EnumLegFieldId.StartDate.getValue()){
			if (tran.getToolset() == EnumToolset.Commodity) {
				setDefaultEndDate(tran);
			}
		}
		else if (field.getTranfId() == EnumTranfField.FromAcct){
			setDefaultEndUser(session, tran);
		}
	}

	private void setDefaultEndDate(Transaction tran) {
		for (Leg leg : tran.getLegs()) {
			if (leg.isPhysicalCommodity()) {
				// Set End Date to Start Date
				leg.setValue(EnumLegFieldId.MaturityDate, leg.getValueAsDate(EnumLegFieldId.StartDate));
			}
		}
	}

	private void setDefaultSettlementDates(Session session, Transaction tran) {
		StaticDataFactory sdf = session.getStaticDataFactory();
		IOFactory iof = session.getIOFactory();
		EnumToolset toolset = tran.getToolset();
		
		// Get party infos
		int extBU = tran.getValueAsInt(EnumTransactionFieldId.ExternalBusinessUnit);
		String strSql = "SELECT type_name, value FROM party_info_view where party_id = " + extBU
		              + "   AND type_name IN ('Cash Payment Term', 'Metal Payment Term')";
		Table temp = iof.runSQL(strSql);
		
		if (toolset == EnumToolset.Commodity) {
			try {
				// Now set as part of the dispatch workflow
				//setCommoditySettleDates(tran, sdf, temp);
			} catch (RuntimeException e) {
				return;
			}
		} 
		else if (toolset == EnumToolset.Fx){
			try {
				setFxSettleDates(session, tran, temp);
			} catch (RuntimeException e) {
				return;
			}
		}
		else if (toolset == EnumToolset.ComSwap){
			Currency ccy = sdf.getReferenceObject(Currency.class, tran.getLeg(0).getValueAsString(EnumLegFieldId.Currency));
			try {
				setPymtOffsetComSwap(tran, ccy, temp, "Metal Payment Term");
				setPymtOffsetComSwap(tran, ccy, temp, "Cash Payment Term");
			} catch (RuntimeException e) {
				return;
			}
		}
		else if (toolset == EnumToolset.Cash){
			String insSubgroup = tran.getValueAsString(EnumTransactionFieldId.InstrumentSubType);
			try {
				if (insSubgroup.equalsIgnoreCase(EnumInsSub.CashTransfer.getName())) {
					setCashSettleDates(tran, temp, "Metal Payment Term");
				}
				else if (insSubgroup.equalsIgnoreCase(EnumInsSub.CashTransaction.getName())) {
					setCashSettleDates(tran, temp, "Cash Payment Term");
				}
			} catch (RuntimeException e) {
				return;
			}		
		}
		else if (toolset == EnumToolset.Loandep){
			try {
				setLoanDepSettleDates(tran, temp);
			} catch (Exception e) {
				return;
			}
		}
		
		temp.dispose();
	}

	private void setDefaultCommPhysFields(IOFactory iof, Transaction tran) {
		for (Leg leg : tran.getLegs()) {
			
		
			if (leg.isPhysicalCommodity()){
				// Each location has a physical leg and a financial leg, financial leg no = physical leg + 1
				int legId = leg.getLegNumber();
				if (legId == tran.getLegCount()){
					PluginLog.error("There is a physical leg with no relating financial leg. \n");
					return;
				}
				
				String idxGroup = leg.getValueAsString(EnumLegFieldId.IndexGroup);
				String idxSubGroup = leg.getValueAsString(EnumLegFieldId.CommoditySubGroup);
				
				if (!idxGroup.equalsIgnoreCase("Pre Metal") || idxSubGroup.equalsIgnoreCase("None")) {
					continue;
				}
				
				// Get currency name from currency description
				String strSql = "SELECT name FROM currency WHERE precious_metal = 1 and description = '" + idxSubGroup +"'";
				Table temp = iof.runSQL(strSql);
				if (temp.getRowCount() != 1) {
					PluginLog.error(idxSubGroup + " is not a precious metal defined in currency table. \n");
					temp.dispose();
					return;
				}
				
				String ccy = temp.getString(0, 0);
				temp.dispose();
				
				Leg financialLeg = tran.getLeg(legId + 1);
				// Set Financial payment currency
				try {
					financialLeg.getField(EnumLegFieldId.Currency).setValue(ccy);
				} catch (Exception e) {
					PluginLog.error("Can't set Financial Payment Currency to " + ccy + ". \n");
				}
				
				// Set Fxd Price/Float spd = 1
				try {
					financialLeg.getField(EnumLegFieldId.RateSpread).setValue(1.0);
				} catch (Exception e) {
					PluginLog.error("Can't set Rate Spread to 1.0. \n");
				}
				

			}
			
			
		}
		
		// Update all the applicable fees to USD
		//setCommPhysFeeJvs(tran);
		
		setCommPhysFeeOc( tran);
		

		
	}

	private void setCommPhysFeeOc(Transaction tran) {
		// Note when setting the currency the order of the fees is potentially
		// updated. Need to
		// make multiple passes to ensure all fees are correctly set.
		boolean feesToUpdate = true;

		String currencyToSet = "USD";

		while (feesToUpdate) {
			boolean feeUpdated = false;

			for (Leg leg : tran.getLegs()) {

				for (Fee fee : leg.getFees()) {

					PluginLog.debug("OC Fee Side " + leg.getLegNumber()
							+ " fee num " + fee.getFeeNumber() + " type "
							+ fee.getValueAsString(EnumFeeFieldId.Definition)
							+ " currency "
							+ fee.getValueAsString(EnumFeeFieldId.Currency));

					Field field = fee.getField(EnumFeeFieldId.Currency);

					if (!field.isReadOnly()
							&& !field.getValueAsString().equalsIgnoreCase(
									currencyToSet)) {

						field.setValue(currencyToSet);

						feeUpdated = true;
						break;
					}
				}

			}
			if (!feeUpdated) {
				feesToUpdate = false;
			}
		}
	}
	


	private void setDefaultEndUser(Session session, Transaction tran) {
		IOFactory iof = session.getIOFactory();
		
		Table temp = null;
		try {
			temp = iof.getUserTable("USER_jm_end_user").retrieveTable();
		} catch (Exception e) {
			PluginLog.error("USER_jm_end_user (Case Sensitive) not exist in the database. \n");
			return;
		}
		
		Field endUser = tran.getField("End User");
		if (endUser == null || !endUser.isApplicable()) {
			PluginLog.error("Tran Info: End User not created. \n");
			return;
		}
		
		String extBU = null;
		if (tran.getToolset() == EnumToolset.Cash){
			String insSubGroup = tran.getValueAsString(EnumTransactionFieldId.InstrumentSubType);
			if (insSubGroup.equalsIgnoreCase(EnumInsSub.CashTransfer.getName())){
				extBU = tran.getValueAsString(EnumTransactionFieldId.FromBusinessUnit);
			} 
			else {
				extBU = tran.getValueAsString(EnumTransactionFieldId.ExternalBusinessUnit);
			}
		} else {
			extBU = tran.getValueAsString(EnumTransactionFieldId.ExternalBusinessUnit);
		}
		
		int rowId = temp.find(0, extBU, 0);
		if (rowId < 0) {
			endUser.setValue(extBU);
		} else {
			endUser.setValue("");
		}
		temp.dispose();
	}

	private void setDefaultFixLegCcy(StaticDataFactory sdf, Transaction tran, String fieldValue) {
		Currency objCcy = getCcyFromIndexNameOrTicker(sdf, fieldValue);
	
		if (objCcy == null) {
			return;
		}
		
		if (objCcy.isPreciousMetal()) {
			try {
				tran.getLeg(0).setValue(EnumLegFieldId.Currency, objCcy.getName());
			} catch (Exception e) {
				PluginLog.error("Failed to set currency on Leg 0(Fixed Leg) to " + objCcy.getName());
			}
		}
		
	}

	private void setDefaultFormAndLoco(Session session, Transaction tran) {
		EnumToolset toolset = tran.getToolset();
		IOFactory iof = session.getIOFactory();
		
		Currency objCcy = getCurrency(session, tran);
	
		if (objCcy == null) {
			return;
		}
		
		if (objCcy.isPreciousMetal()) {
			// Get the default value from party info
			int extBU = tran.getValueAsInt(EnumTransactionFieldId.ExternalBusinessUnit);
			// Get party info values from the db table
			try(Table temp = loadLocoAndFormData( session, extBU)) {

				if (temp == null) {
					return;
				}
				
				int rowId = temp.find(0, "Form", 0);
				if (rowId >= 0) {
					tran.getField("Form").setValue(temp.getString(1, rowId));
					if (toolset == EnumToolset.ComSwap) {
						setDefaultSpread(iof, tran);
					}
				} else {
					tran.getField("Form").setValue("");
				}
				rowId = temp.find(0, "Loco", 0);
				if (rowId >= 0) {
					String loco = locationOverride( session, tran, 
							temp.getString(1, rowId),  new Integer(temp.getInt(2, rowId)).toString());
					tran.getField("Loco").setValue(loco);
				} else {
					tran.getField("Loco").setValue("");
				}
				
			} catch (Exception e) {
				PluginLog.error(e.getMessage());
			}
		} else {
			try {
				tran.getField("Form").setValue("None");
				tran.getField("Loco").setValue("None");
			} catch (Exception e) {
				PluginLog.error("Tran Info 'Form' or 'Loco' not Created for toolset " + toolset.getName() + "\n");
			}
		}
		
	}
	
	private String locationOverride(Session session, Transaction tran, String defaultLoco, String locoId) {
		List<String> overrideLocations = null;
		try {
			String overrideLocationData = constRep.getStringValue("Default Location Override", "10,14,1,16,2");
			
			overrideLocations = new ArrayList<String>( Arrays.asList(overrideLocationData.split(",")));
			
			if(overrideLocations == null || overrideLocations.size() == 0) {
				String errorMessage = "Error calcualting the override locations. Empty list returned" ;
				PluginLog.error(errorMessage);
				throw new RuntimeException(errorMessage);				
			}
		} catch (Exception e) {
			String errorMessage = "Error reading the location overrides from the const repo. " + e.getMessage();
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		
		String returnValue = defaultLoco;
		if(overrideLocations.contains(locoId)) {
			// Overwrite the external BU loco with internal BU loco
			int intBU = tran.getValueAsInt(EnumTransactionFieldId.InternalBusinessUnit);
			try(Table temp = loadLocoAndFormData( session, intBU)) {
				if(temp == null) {
					String errorMessage = "Error loading internal BU loco";
					PluginLog.error(errorMessage);
					throw new RuntimeException(errorMessage);					
				}
				int rowId = temp.find(0, "Loco", 0);
				if(rowId >= 0) {
					returnValue = temp.getString(1, rowId);
				}   else {
					String errorMessage = "Interna loco not loaded";
					PluginLog.error(errorMessage);					
				}
			}
		}
		
		return returnValue;
	}
	
	private Table loadLocoAndFormData(Session session, int bu) {
		IOFactory iof = session.getIOFactory();
		
		String strSql = "SELECT piv.type_name, piv.value, loco_id "
				+ " FROM party_info_view piv"
				+ " LEFT JOIN USER_jm_loco l on piv.value = l.loco_name "
				+ " WHERE piv.party_id = " + bu
				+ " AND piv.type_name IN ('Form', 'Loco')";
		
		PluginLog.debug("About to run SQL: " + strSql);
		Table temp = iof.runSQL(strSql);

		return temp;
		
		
	}

	private void setDefaultSpread(IOFactory iof, Transaction tran) {
		double spread = tran.getLeg(1).getValueAsDouble(EnumLegFieldId.RateSpread);
		int extBU = tran.getValueAsInt(EnumTransactionFieldId.ExternalBusinessUnit);
		String buySell = tran.getValueAsString(EnumTransactionFieldId.BuySell);
		String formValue = tran.getField("Form").getValueAsString();
		String formInfo = buySell + " Form(" + formValue + ")";
		String strSql = "SELECT type_name, value FROM party_info_view where party_id = " + extBU + " AND type_name = '" + formInfo + "'";
		Table temp = iof.runSQL(strSql);
	
		if (temp.getRowCount() == 0) {
			temp.dispose();
			try {
				setSpreadFromUserTable(iof, tran, spread, buySell, formValue);
				
			} catch (Exception e) {
				PluginLog.error("Error loading the default spread. " + e.getMessage());
				return;
			}
		}
		else {
			String value = temp.getString(1, 0);
			temp.dispose();
			//tran.getLeg(1).setValue(EnumLegFieldId.RateSpread, spread);
			try ( Field metalPriceSpread = tran.getField("Metal Price Spread") ) {
				spread = Double.valueOf(value);
				metalPriceSpread.setValue(spread);
			} catch (Exception e) {
				PluginLog.error("Error setting the default spread. " + e.getMessage());
				return;
			} 
			
		}
	}

	private void setLoanDepSettleDates(Transaction tran, Table temp) {
		int rowId = temp.find(0, "Cash Payment Term", 0);
		if (rowId >= 0) {
			try {
				tran.retrieveField(EnumTranfField.PymtDateOffset, 0).setValue(temp.getString(1, rowId));
			} catch (Exception e) {
				PluginLog.error("Symbolic date " + temp.getString(1, rowId) + " not Valid. \n");
				temp.dispose();
				throw new RuntimeException();
			}
		}
	}

	private void setCashSettleDates(Transaction tran, Table temp, String infoValue) {
		int rowId = temp.find(0, infoValue, 0);
		if (rowId >= 0) {
			try {
				tran.setValue(EnumTransactionFieldId.SettleDate, temp.getString(1, rowId));
			} catch (Exception e) {
				PluginLog.error("Symbolic date " + temp.getString(1, rowId) + " not Valid. \n");
				temp.dispose();
				throw new RuntimeException();
			}
		}
	}

	private void setCommoditySettleDates(Transaction tran, StaticDataFactory sdf, Table temp) {
		for (Leg leg : tran.getLegs()) {
			if (leg.isPhysicalCommodity()) {
				// Each location has a physical leg and a financial leg,
				// financial leg no = physical leg + 1
				int legId = leg.getLegNumber();
				if (legId == tran.getLegCount()) {
					PluginLog.error("There is a physical leg with no relating financial leg. \n");
					throw new RuntimeException();
				}

				Leg financialLeg = tran.getLeg(legId + 1);
				Currency ccy = sdf.getReferenceObject(Currency.class, financialLeg.getValueAsString(EnumLegFieldId.Currency));
				if (!ccy.isPreciousMetal()) {
					continue;
				}

				setPymtOffsetCommPhys(tran, temp, "Cash Payment Term", legId);
				setPymtOffsetCommPhys(tran, temp, "Metal Payment Term", legId + 1);
			}
		}
	}

	private void setPymtOffsetComSwap(Transaction tran, Currency ccy, Table temp, String infoValue) {
		
		String projIndex = tran.retrieveField(EnumTranfField.ProjIndex, 1).getValueAsString();
		
		String skipIndexs = "";
		try {
			skipIndexs = constRep.getStringValue("ComSwapSkipProjIndex", skipIndexs);
		} catch (Exception e1) {
			PluginLog.error("Error reading indexs to skip from the cons repo. \n");
			temp.dispose();
			throw new RuntimeException();
		}
		
		if(skipIndexs.contains(projIndex)) {
			PluginLog.info("Skipping defaulting for proj index " + projIndex);
			return;
		}
		int rowId = temp.find(0, infoValue, 0);
		if (rowId >= 0) {
			try {
				if (infoValue.equalsIgnoreCase("Metal Payment Term")) {
					if (ccy.isPreciousMetal()) {
						tran.retrieveField(EnumTranfField.PymtDateOffset, 0).setValue(temp.getString(1, rowId));
					}
				} 
				else if (infoValue.equalsIgnoreCase("Cash Payment Term")){
					if (ccy.isPreciousMetal()) {
						tran.retrieveField(EnumTranfField.PymtDateOffset, 1).setValue(temp.getString(1, rowId));
					}
					else {
						tran.retrieveField(EnumTranfField.PymtDateOffset, 0).setValue(temp.getString(1, rowId));
					}
				}
			} catch (Exception e) {
				PluginLog.error("Symbolic date " + temp.getString(1, rowId) + " not Valid. \n");
				temp.dispose();
				throw new RuntimeException();
			}
		}
		
	}

	private void setFxSettleDates(Session session, Transaction tran, Table temp) {
		Date metalSettle = createSettleDate(session, temp, "Metal Payment Term");
		Date cashSettle = createSettleDate(session, temp, "Cash Payment Term");

		String cFlowType = tran.getField(EnumTransactionFieldId.CashflowType).getValueAsString();
		String currencyPair = tran.getField(EnumTransactionFieldId.CurrencyPair).getValueAsString();
		String[] ccys = currencyPair.split("/");
		
		StaticDataFactory sdf = session.getStaticDataFactory();
		Currency currency1 = (Currency)sdf.getReferenceObject(EnumReferenceObject.Currency, currencyPair.substring(0,3));
		Currency currency2 = (Currency)sdf.getReferenceObject(EnumReferenceObject.Currency, ccys[1].substring(0,3));
		
		if (currency1.isPreciousMetal()) {
			if (metalSettle != null) {
				tran.setValue(EnumTransactionFieldId.SettleDate, metalSettle);
				if (cFlowType.contains("Swap")) {
					tran.setValue(EnumTransactionFieldId.FxFarBaseSettleDate, metalSettle);
				}
			}
			if (cashSettle != null){
				tran.setValue(EnumTransactionFieldId.FxTermSettleDate, cashSettle);
				if (cFlowType.contains("Swap")) {
					tran.setValue(EnumTransactionFieldId.FxFarTermSettleDate, cashSettle);
				}
			}
		}
		else if (currency2.isPreciousMetal()) {
			if (metalSettle != null) {
				tran.setValue(EnumTransactionFieldId.FxTermSettleDate, metalSettle);
				if (cFlowType.contains("Swap")) {
					tran.setValue(EnumTransactionFieldId.FxFarTermSettleDate, metalSettle);
				}
			}
			if (cashSettle != null){
				tran.setValue(EnumTransactionFieldId.SettleDate, cashSettle);
				if (cFlowType.contains("Swap")) {
					tran.setValue(EnumTransactionFieldId.FxFarBaseSettleDate, cashSettle);
				}
			}
		} else {
			if (cashSettle != null) {
				tran.setValue(EnumTransactionFieldId.SettleDate, cashSettle);
				tran.setValue(EnumTransactionFieldId.FxTermSettleDate, cashSettle);
				if (cFlowType.contains("Swap")) {
					tran.setValue(EnumTransactionFieldId.FxFarBaseSettleDate, cashSettle);
					tran.setValue(EnumTransactionFieldId.FxFarTermSettleDate, cashSettle);
				}
			}
		}
	}

	private Date createSettleDate(Session session, Table temp, String infoName) {
		CalendarFactory cf = session.getCalendarFactory();
		Date metalSettle = null;
		int rowId = temp.find(0, infoName, 0);
		if (rowId >= 0) {
			try {
				metalSettle = cf.createSymbolicDate(temp.getString(1, rowId)).evaluate();
			} catch (Exception e) {
				PluginLog.error("Symbolic date " + temp.getString(1, rowId) + " not Valid. \n");
			}
		}
		
		return metalSettle;
	}

	private void setPymtOffsetCommPhys(Transaction tran, Table src, String infoName, int legId) {
		int rowId = src.find(0, infoName, 0);
		if (rowId >= 0) {
			try {
				tran.retrieveField(EnumTranfField.PymtDateOffset, legId).setValue(src.getString(1, rowId));
			} catch (Exception e) {
				PluginLog.error("Symbolic date " + src.getString(1, rowId) + " not Valid. \n");
				src.dispose();
				throw new RuntimeException();
			}
		}
	}

	private void setSpreadFromUserTable(IOFactory iof, Transaction tran, double spread, String buySell, String formValue) {
		Table temp = null;
		// Pick up the price from USER_jm_spread_form_prices
		try {
			temp = iof.runSQL("SELECT * from USER_jm_spread_form_prices");
		} catch (Exception e) {
			PluginLog.error("USER_jm_spread_form_prices not exist in the database. \n");
			throw new RuntimeException();
		}
		int rowId = temp.find(0, formValue, 0);
		if (rowId >= 0) {
			if (buySell.equalsIgnoreCase("Buy")) {
				try {
					spread = temp.getDouble("buy_price", rowId);
				} catch (Exception e) {
					PluginLog.error("USER_jm_spread_form_prices does not has a column called 'buy_price'. \n");
					temp.dispose();
					throw new RuntimeException();
				}
			}
			else {
				try {
					spread = temp.getDouble("sell_price", rowId);
				} catch (Exception e) {
					PluginLog.error("USER_jm_spread_form_prices does not has a column called 'sell_price'. \n");
					temp.dispose();
					throw new RuntimeException();
				}
			}
			//tran.getLeg(1).setValue(EnumLegFieldId.RateSpread, spread);
			try ( Field metalPriceSpread = tran.getField("Metal Price Spread") ) {
				metalPriceSpread.setValue(spread);
			} catch (Exception e) {
				PluginLog.error("Error setting the default spread. " + e.getMessage());
				return;
			} 
		}
		else {
			PluginLog.error("Missing row for form '" + formValue + "' in USER_jm_spread_form_prices. \n");
		}
		temp.dispose();
	}

	private Currency getCurrency(Session session, Transaction tran) {
		EnumToolset toolset = tran.getToolset();
		StaticDataFactory sdf = session.getStaticDataFactory();
		
		Currency objCcy = null;
		if (toolset == EnumToolset.Fx) {
			objCcy = getCcyFromIndexNameOrTicker(sdf, tran.getValueAsString(EnumTransactionFieldId.Ticker));
		} 
		else if (toolset == EnumToolset.ComSwap) {
			objCcy = getCcyFromIndexNameOrTicker(sdf, tran.getLeg(1).getValueAsString(EnumLegFieldId.ProjectionIndex));
		}
		else if (toolset == EnumToolset.Loandep) {
			objCcy = sdf.getReferenceObject(Currency.class, tran.getLeg(1).getValueAsString(EnumLegFieldId.Currency));
		}
		 
		return objCcy;
	}

	private Currency getCcyFromIndexNameOrTicker(StaticDataFactory sdf, String name) {
		String temp = null;
		// If it is not a valid name, then return null
		if (name == null) {
			return null;
		}

		if (name.contains(".")) {
			// Get Currency name from Index Name
			temp = name.split("\\.")[0];
			if (temp.length() < 3) {
				return null;
			}
			temp = temp.substring(temp.length() - 3, temp.length());
		} else if (name.contains("/")) {
			// Get Currency name from Ticker
			temp = name.split("/")[0];
			if (temp.length() < 3) {
				return null;
			}
			temp = temp.substring(0, 3);
		}
		
		if (temp == null) {
			return null;
		}
		
		Currency objCcy = null;
		try {
			objCcy = sdf.getReferenceObject(Currency.class, temp);
		} catch (Exception e) {
			// In some cases, the string is not a valid currency. This is valid and not an error.
		}
		
		return objCcy;
	}

}