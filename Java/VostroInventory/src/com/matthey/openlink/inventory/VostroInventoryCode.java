package com.matthey.openlink.inventory;


import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import com.matthey.openlink.utilities.DataAccess;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Util;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.EnumTableJoin;
import com.olf.openrisk.table.JoinSpecification;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.openlink.endur.utilities.logger.LogCategory;
import com.openlink.endur.utilities.logger.LogLevel;
import com.openlink.endur.utilities.logger.Logger;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.constrepository.*;

/*
 * History:
 * 2015-MM-DD	V1.0	pwallace	- Initial Version
 * 2016-09-27	V1.1	jwaechter	- Added PluginLog
 */

/**
 * D2250.1 Vostro Inventory
 * This mimics the EoD Inventory processing on custom tables that shadow the system implementation of nostros for Account Balances
 * 
 * @version $Revision:  $ 
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class VostroInventoryCode extends AbstractGenericScript {

	
	private static final String ACCOUNT_POSITION_HISTORY = "nostro_account_position_hist";
	private static final int ERR_CONFIG = 7899;
	private static final int DEPENDENTS_NOT_RUN = 7891;
	private ConstRepository repository = null;
    private static final String CONTEXT = "EOD";
	private static final String SUBCONTEXT = "VostroInventory";

	@Override
	public Table execute(Session session, ConstTable table) {
    	initPluginLog();
    	
//		SkipCheck=true;
		long startTime = System.nanoTime();
		try {
			if (!hasEoDInventoryRun(session)) {
				throw new VostroInventoryExceptionCode("EoD sequence", DEPENDENTS_NOT_RUN,
						"This process MUST only be run after the Nostro EoD Inventory has completed");
			}
			
			Table positionAdjustment = getVostroAdjustedPosition(session);
			//session.getDebug().viewTable(vap);
			Table vostroHistoricalInventoryPosition = getVostroHistory(session);
			//session.getDebug().viewTable(vHistory);
			
			// merge sources
			vostroHistoricalInventoryPosition.addColumn("adjBalance", EnumColType.Double);
			
			Table vostroAdjustedPostion= session.getTableFactory().createTable("positive ACCOUNTS");
			positionAdjustment.addColumn("adjBalance", EnumColType.Double);
			// update user table
			JoinSpecification vostroJoin = session.getTableFactory().createJoinSpecification("[LEFT.account_id] == [RIGHT.account_id] " +
					"AND [LEFT.currency_id] == [RIGHT.currency_id] " +
					"AND [LEFT.delivery_type] == [RIGHT.delivery_type] " +
					"AND [LEFT.portfolio_id] == [RIGHT.portfolio_id] " +
					"AND [LEFT.unit] == [RIGHT.unit] ", EnumTableJoin.LeftOuter);

			//session.getDebug().viewTable(vostroAdjustedPostion)
			//session.getDebug().viewTable(vostroHistoricalInventoryPosition)
			vostroJoin.fill(positionAdjustment, "unit,adjBalance", vostroHistoricalInventoryPosition, "unit,position");
			for (TableRow row : positionAdjustment.getRows()) {
				if (0!=Double.compare(com.olf.openrisk.utility.Numbers.DOUBLE_BLANK, row.getDouble("adjBalance"))) {
					row.getCell("account_id").setInt(0);
				} else {
					row.getCell("adjBalance").setDouble(row.getDouble("position"));
					row.getCell("position").setDouble(0d);
				}
			}
			vostroAdjustedPostion.select(positionAdjustment, "*", "[In.account_id] >0");
			vostroAdjustedPostion.setName("ADJ TODO");
			vostroAdjustedPostion.removeColumn("report_date");
			vostroAdjustedPostion.removeColumn("position");
		//	positionAdjustment.dispose();
			vostroAdjustedPostion.sort(new String[] {"account_id", "portfolio_id", "currency_id", "delivery_type", "unit"}, true);

			vostroHistoricalInventoryPosition.appendUnmatchedRows(vostroAdjustedPostion, "account_id, portfolio_id, currency_id, delivery_type, unit");
			//session.getDebug().viewTable(vostroAdjustedPostion);
			vostroAdjustedPostion.dispose();

			Logger.log(LogLevel.DEBUG, LogCategory.Trading, this,"\ncalculate balance...");
			vostroHistoricalInventoryPosition.calcColumn("position", "adjBalance + position"); 
			//session.getDebug().viewTable(vostroHistoricalInventoryPosition)
			vostroHistoricalInventoryPosition.calculate();
			Logger.log(LogLevel.DEBUG, LogCategory.Trading, this,"\nCALCULATED balance...");
			
			vostroHistoricalInventoryPosition.setName("COMPLETED History");
			//session.getDebug().viewTable(vHistory);
			vostroHistoricalInventoryPosition.removeColumn("adjBalance");
			vostroHistoricalInventoryPosition.addColumn("report_date", EnumColType.DateTime);
			vostroHistoricalInventoryPosition.setColumnValues("report_date", session.getProcessingDate());
			vostroHistoricalInventoryPosition.addColumn("last_update", EnumColType.DateTime);
			vostroHistoricalInventoryPosition.setColumnValues("last_update", Calendar.getInstance().getTime());
			vostroHistoricalInventoryPosition.addColumn("applied_amount", EnumColType.Double);
			vostroHistoricalInventoryPosition.setColumnValues("applied_amount", 0.0d);
			
			//history.clearRows();
			applyHistoricalBalances(session, vostroHistoricalInventoryPosition);
//			if (!SkipCheck) //System.out.println("DEBUG placeholder");
			
			Table historicalOpeningBalances = getVostroHistoricalOpeningTrades(session);
			if (historicalOpeningBalances!=null && historicalOpeningBalances.getRowCount()>0) {

				Table netAdjustedPostion= session.getTableFactory().createTable("netPostions4Today");
				historicalOpeningBalances.addColumn("alreadyExists", EnumColType.Double);
				// update user table
				JoinSpecification netVostro = session.getTableFactory().createJoinSpecification("[LEFT.account_id] == [RIGHT.account_id] " +
						"AND [LEFT.currency_id] == [RIGHT.currency_id] " +
						"AND [LEFT.delivery_type] == [RIGHT.delivery_type] " +
						"AND [LEFT.portfolio_id] == [RIGHT.portfolio_id] " +
						"AND [LEFT.unit] == [RIGHT.unit] ", EnumTableJoin.LeftOuter);
				
				netVostro.fill(historicalOpeningBalances, "unit,alreadyExists", positionAdjustment, "unit,position");
				for (TableRow row : historicalOpeningBalances.getRows()) {
					if (0!=Double.compare(com.olf.openrisk.utility.Numbers.DOUBLE_BLANK, row.getDouble("alreadyExists"))) {
						row.getCell("alreadyExists").setDouble(1.0d);
						row.getCell("account_id").setInt(0);
					} else {
						row.getCell("alreadyExists").setDouble(0.0d);
					}
				}
				netAdjustedPostion.select(historicalOpeningBalances, "*", "[In.account_id] >0");
//				session.getDebug().viewTable(vostroHistoricalInventoryPosition);
//				session.getDebug().viewTable(positionAdjustment);
//				session.getDebug().viewTable(netAdjustedPostion);
				netAdjustedPostion.removeColumn("alreadyExists");
				//removeExistingEoDEntries(netAdjustedPostion);
				if (netAdjustedPostion.getRowCount()>0)				
					applyHistoricalBalances(session, netAdjustedPostion);
			}
			PluginLog.info ("Vostro Inventory Generation Succeeded");
		} catch (VostroInventoryExceptionCode vie) {
			String message = String.format("ERR: Unexpected problem detected: %s", vie.getLocalizedMessage());
			Logger.log(LogLevel.INFO, LogCategory.Trading, this,
					message);
			PluginLog.error (message);
			for (StackTraceElement ste : vie.getStackTrace()) {
				PluginLog.error (ste.toString());
			}
			throw vie;
			
			
		} catch (OpenRiskException ore) {
			String message = String.format("System Error detected: %s", ore.getLocalizedMessage());
			Logger.log(LogLevel.INFO, LogCategory.Trading, this, message);
			for (StackTraceElement ste : ore.getStackTrace()) {
				PluginLog.error (ste.toString());
			}
			throw ore;
			
		} finally {
			Logger.log(LogLevel.INFO, LogCategory.Trading, this, 
					String.format("\n\n ALL done in %d secs",
					TimeUnit.SECONDS.convert(System.nanoTime() - startTime,
							TimeUnit.NANOSECONDS)
					));
		}
		return null;
	}

	private void applyHistoricalBalances(Session session, Table historical) {

		try {
			UserTable history = session.getIOFactory().getUserTable("USER_jm_vostro_acct_pos_hist", true);

			//session.getDebug().viewTable(historical);

			history.insertRows(historical);
			Logger.log(LogLevel.DEBUG, LogCategory.Trading, this, "\nAPPLIED historical balance...");

		} catch (OpenRiskException ore) {

			if (ore.getLocalizedMessage().contains("duplicate key value"))
				throw new VostroInventoryExceptionCode("Already Run", 9978,
						String.format("EoD Data exists for %s", session.getProcessingDate().toString()));

			Logger.log(LogLevel.INFO, LogCategory.Trading, this,
					String.format("System Error detected: %s",ore.getLocalizedMessage()));
			throw ore;

		}
	}

	/**
	 * Return a table of Vostro accounts where postings have been made outside the actual date to which they are effective.
	 * 
	 * This means transactions posted after and EoD which affect a date in the past will contains entries in this result set.
	 * The entries will have a start_date and end_date that the position needs to be applied to so the effective balance(position) of an account can be reported.
	 */
	private Table getVostroAdjustedPosition(Session session) {
			final String todaysVostroEntries = "SELECT  ates.ext_account_id as account_id" 
					+ ", nadv.currency_id, nadv.delivery_type"
					+ ", nadv.portfolio_id "
					+ ",-nadv.ohd_position ohd_position "
					+ ", nadv.unit "
//					+ ", nadv.event_date as report_date "
					+ ", sd.processing_date as report_date"
	/*				+ " FROM nostro_account_detail_view nadv " 
					+ " INNER JOIN nostro_flag nf ON  nadv.nostro_flag=nf.nostro_flag_id AND nf.nostro_flag_name='Settled' "
					+ " INNER JOIN system_dates sd ON (nadv.settle_date<=sd.processing_date ) " 
					+ " INNER JOIN ab_tran_event_settle ates ON (nadv.event_num=ates.event_num) " 
					+ " INNER JOIN ab_tran ab ON (nadv.deal_tracking_num=ab.deal_tracking_num AND ab.current_flag=1 AND ab.offset_tran_num=0) " 
					+ " INNER JOIN trans_status abs ON (abs.trans_status_id=ab.tran_status AND abs.name='Validated')";
	*/		
		    + " FROM nostro_account_detail_view nadv "
//			+ " INNER JOIN nostro_flag nf ON  nadv.nostro_flag=nf.nostro_flag_id AND nf.nostro_flag_name IN ('Settled') "
		    + " INNER JOIN system_dates sd ON (nadv.event_date<=sd.processing_date ) "
			+ " INNER JOIN ab_tran_event_settle ates ON (nadv.event_num=ates.event_num) " 
		    + " INNER JOIN ab_tran ab ON (nadv.tran_num=ab.tran_num AND ab.current_flag=1 AND ab.offset_tran_num=0) "
			+ " INNER JOIN trans_status abs ON (abs.trans_status_id=ab.tran_status AND abs.name IN ('Validated', 'Matured', 'Closeout')) "
			+ " WHERE nadv.nostro_flag = 1 "  // this is not the same as the ates.nostro_flag
			+ " AND nadv.event_num in ( " 
			+ "			SELECT  ate.event_num "
			+ "				FROM ab_tran_event_settle ates " 
			+ " 			JOIN ab_tran_event ate on ates.event_num=ate.event_num AND ates.delivery_type IN (select id_number from delivery_type where name ='Cash') "
			+ "				JOIN ab_tran ab ON (ate.tran_num=ab.tran_num  AND ab.current_flag=1 " 
			+ "					AND ab.tran_status IN (SELECT trans_status_id FROM trans_status WHERE name in ('Validated', 'Matured', 'Closeout'))) "  
			+ " 		WHERE ate.event_date<=(SELECT processing_date FROM system_dates) "
			+ "				AND ates.nostro_flag IN (SELECT nostro_flag_id FROM  nostro_flag WHERE nostro_flag_name in ('Un-Settled', 'Settled')))"; 

		
		
		
		
		final String todaysVostroAdjustments = "SELECT "
				+ "vapa.account_id "
				+ ",vapa.currency_id "
				+ ",vapa.delivery_type "
				+ ",vapa.portfolio_id "
				+ ",-vapa.position ohd_position "
				+ ",vapa.unit "
				+ ",vapa.official_system_date as report_date "
				+ "	FROM user_jm_vostro_account_pos_adj vapa "
				+ " INNER JOIN account ac ON vapa.account_id=ac.account_id "
				+ " INNER JOIN account_type act ON ac.account_type=act.id_number AND act.name='Vostro' "
				+ " INNER JOIN system_dates sd ON (vapa.official_system_date=sd.processing_date)";
		

		
		Table vostroAggregate = DataAccess.getDataFromTable(session, 
			 String.format("SELECT " 
								+ "tbl.account_id " 
								+ ",tbl.currency_id "
								+ ",tbl.delivery_type" 
								+ ",tbl.portfolio_id"
								+ ",sum(tbl.ohd_position) as position"
								+ ",tbl.unit" 
								+ ",tbl.report_date"
								+ " FROM (" + "%s " 
//								+ " UNION ALL " 
//								+ " %s " 
								+ ") as tbl" 
								+ " GROUP BY tbl.account_id"
								+ " ,tbl.currency_id"
								+ " ,tbl.delivery_type"
								+ " ,tbl.portfolio_id"
								+ " ,tbl.unit"
								+ " ,tbl.report_date",
							todaysVostroEntries/*,
							todaysVostroAdjustments*/));		
		
		if (null == vostroAggregate ) {
			//vostroAggregate.dispose();
			throw new VostroInventoryExceptionCode("Configuration data", ERR_CONFIG,
					"No data was obtainable from Vostro tables");
		}
		vostroAggregate.setName("VostroAdjustedPosition");
		return vostroAggregate;
		
	}
	
	
	
	/**
	 * identify all accounts with trades that precede the first EoD for that account 
	 * this can result in no matching data which is acceptable 
	 */
	private Table getVostroHistoricalOpeningTrades(Session session) {
		
		final String openingHistoricalTrades = "SELECT DISTINCT ates.ext_account_id account_id"  
				+ "  ,naph.currency_id, naph.delivery_type, naph.portfolio_id"
				+ "  ,naph.position, naph.report_date , naph.unit"  
				+ "\nFROM nostro_account_position_hist naph" 
				+ "\nINNER JOIN ab_tran_event_settle ates on (ates.int_account_id=naph.account_id)"  
				+ "\nWHERE naph.position=0.000 "
				+ " AND naph.last_update >=(SELECT MAX(last_update) FROM nostro_account_position_hist)";
		

		
		Table vostroHistoricalEntries = DataAccess.getDataFromTable(session, 
				openingHistoricalTrades);	
		
		
		if (vostroHistoricalEntries != null
				&& vostroHistoricalEntries.getRowCount()>0) {
			vostroHistoricalEntries.addColumn("last_update", EnumColType.DateTime);
			vostroHistoricalEntries.setColumnValues("last_update", Calendar.getInstance().getTime());
			vostroHistoricalEntries.addColumn("applied_amount", EnumColType.Double);
			vostroHistoricalEntries.setColumnValues("applied_amount", 0.0d);
		}
		
		vostroHistoricalEntries.setName("VostroOpeningHistoricalEntries");
		return vostroHistoricalEntries;
	}
	
	/**
	 * return the most recent Vostro account historical entries based on the {@value #ACCOUNT_POSITION_HISTORY} table
	 * 
	 * 
	 */
	private Table getVostroHistory(Session session) {

			   Table vostroHistory = DataAccess.getDataFromTable(session, 
						"SELECT naph.account_id"
								+ ",naph.currency_id "
								+ ",naph.delivery_type "
								+ ",naph.portfolio_id "
								+ ",naph.unit "
								+ ",naph.position "
								+ " FROM " + ACCOUNT_POSITION_HISTORY + " naph "
								+ " INNER JOIN ( "
								+ " SELECT "
								+ " hist.account_id   "
								+ " ,hist.currency_id  "
								+ " ,hist.delivery_type"
								+ " ,hist.portfolio_id "
								+ " ,hist.unit "
								+ " ,max(hist.report_date) as report_date "
								+ " FROM  " + ACCOUNT_POSITION_HISTORY + " hist"
								+ " INNER JOIN account ac on hist.account_id=ac.account_id "								//06/08/2015
								+ " INNER JOIN account_type act on ac.account_type=act.id_number AND act.name='Vostro'"		//06/08/2015
								+ " GROUP BY hist.account_id, hist.currency_id, hist.delivery_type, hist.portfolio_id, hist.unit "
								+ " ) as last ON naph.account_id=last.account_id AND "
								+ "   naph.currency_id = last.currency_id AND "
								+ "   naph.delivery_type = last.delivery_type AND "
								+ "   naph.portfolio_id = last.portfolio_id AND "
								+ "   naph.unit = last.unit AND "
								+ "   naph.report_date = last.report_date");
			   
				if (null == vostroHistory) {
					//vostroHistory.dispose();
					throw new VostroInventoryExceptionCode("Configuration data", ERR_CONFIG,
							"There is an problem getting the historical data");
				}
				vostroHistory.setName("VostroHistory");
		return vostroHistory;
	}
	
	/**
	 * if the Nostro history table({@value #ACCOUNT_POSITION_HISTORY}) has entries for active EoD date we can continue otherwise <b>fail</b>!
	 * <br>The nostro EoD inventory populates entries into the history position based on the effective system EoD, this used to ensure that dependent process has been completed before undertaking this customisation for the Vostro position...
	 */
	private boolean hasEoDInventoryRun(Session session) {
//		if (SkipCheck)
//			return SkipCheck;
		String hasNostroEoDInventoryRun = "IF EXISTS(SELECT report_date " +
				"\n FROM " + ACCOUNT_POSITION_HISTORY + " naph " +
				"\n INNER JOIN system_dates sd ON (naph.report_date = sd.processing_date))" +
				"\n BEGIN" +
				" SELECT 'True'" +
				" END" +
				"\n ELSE" +
				"\n BEGIN" +
				"  SELECT 'False'" +
				"END";
				
		Table nostroEoD = DataAccess.getDataFromTable(session, hasNostroEoDInventoryRun);
		if (null == nostroEoD ) {
			//nostroEoD.dispose();
			throw new VostroInventoryExceptionCode("Configuration data", DEPENDENTS_NOT_RUN,
					"This process MUST only be run after the Nostro EoD Inventory has completed");
		}
		return 0 == "True".compareToIgnoreCase(nostroEoD.getString(0, 0));
	}

	private void removeExistingEoDEntries(Table entries) {
		
	}

	/**
	 * @description	Initialises logging
 */
	private void initPluginLog () {   
		try {
	    	repository = new ConstRepository(CONTEXT, SUBCONTEXT);
			String abOutdir = Util.getEnv("AB_OUTDIR");
			String logLevel = repository.getStringValue ("logLevel", "Error");
	        String logFile = repository.getStringValue ("logFile", "VostroInventoryGeneration.log");
	        String logDir = repository.getStringValue ("logDir", abOutdir + "\\error_logs");        
	        try {
	            if (logDir.trim().equals (""))
	                PluginLog.init(logLevel);
	            else
	                PluginLog.init(logLevel, logDir, logFile);
	        }
	        catch (Exception ex) {
	            String strMessage = getClass().getSimpleName () + " - Failed to initialize log.";
	            OConsole.oprint(strMessage + "\n");
	            Util.exitFail();
	        }			
		} catch (OException ex) {
			throw new RuntimeException (ex);
		}
    }

}
