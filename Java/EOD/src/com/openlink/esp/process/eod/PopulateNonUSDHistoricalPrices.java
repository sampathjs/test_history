package com.openlink.esp.process.eod;

import java.math.BigDecimal;
import java.util.HashMap;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.Index;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.sc.bo.docproc.BO_CommonLogic.Query;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * This class calculates the metal prices for non USD currencies and saves them
 * to idx_historical_prices table.
 * It relies on USER_JM_SAP_AUTOPOP_HIST_PRICES to decide which index and ref source
 * should be used to calculate the prices and which index the price would be saved against.
 * 
 * @author YadavP03
 * @version 1.0
 */
public class PopulateNonUSDHistoricalPrices implements IScript {

private final String CONTEXT = "EOD";


	@Override
	public void execute(IContainerContext context) throws OException {
		try {
			init(context);
			process(context);
		} catch (Exception ex) {
			PluginLog.error(this.getClass().getName()
					+ " terminated abnormaly: \n" + ex.toString());
			Util.exitFail("PopulateNonUSDHistoricalPrices script failed " + ex.getMessage());
			throw new OException (ex.getCause());
		}
		PluginLog.info(this.getClass().getName() + " ended successfully");
	}

	private void process(IContainerContext context) throws OException {
		Table userHistPriceConfig = loadUserHistPriceConfigs();
		HashMap<String, String> idxRefSrcConfigMap  = populateUserConfigMap(userHistPriceConfig);
		Table idxHistPrices = loadIdxHistPrices(userHistPriceConfig);
		HashMap<String, IndexDetails> idxDetailsMap = filterRefSources(idxHistPrices, idxRefSrcConfigMap);
		userHistPriceConfig = calcPrice(idxDetailsMap, userHistPriceConfig);
		saveHistPriceToDB(userHistPriceConfig);
	}
	
	/**
	 * saveHistPriceToDB.
	 * This Method creates a table of historical prices witht he required details
	 * from the input table passed. The table created is used to save the prices to the DB.
	 * 
	 * @param userHistPriceConfig the table containing the prices to be saved.
	 * @throws OException 
	 */

	private void saveHistPriceToDB(Table userHistPriceConfig) throws OException {

		Table importTable = Util.NULL_TABLE;
		try {
			int rowCount = userHistPriceConfig.getNumRows();
			importTable = Table.tableNew("idx_historical_price_tbl");
			Table errorLog = Table.tableNew();
			importTable.addCol("index_id", COL_TYPE_ENUM.COL_INT);
			importTable.addCol("date", COL_TYPE_ENUM.COL_INT);
			importTable.addCol("start_date", COL_TYPE_ENUM.COL_INT);
			importTable.addCol("end_date", COL_TYPE_ENUM.COL_INT);
			importTable.addCol("yield_basis", COL_TYPE_ENUM.COL_INT);
			importTable.addCol("ref_source", COL_TYPE_ENUM.COL_INT);
			importTable.addCol("index_location", COL_TYPE_ENUM.COL_INT);
			importTable.addCol("price", COL_TYPE_ENUM.COL_DOUBLE);
			for (int row = 1; row <= rowCount; row++) {
				String targetIndex = userHistPriceConfig.getString("target_price_curve", row);
				int targetIndexId = Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE, targetIndex);
				String targetRefSource = userHistPriceConfig.getString("target_reference_source", row);
				int targetRefSourceId = Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, targetRefSource);
				double price = userHistPriceConfig.getDouble("target_curve_price", row);
				ODateTime resetDate = userHistPriceConfig.getDateTime("target_curve_reset_date", row);
				ODateTime startDate = userHistPriceConfig.getDateTime("target_curve_start_date", row);
				int importRow = importTable.addRow();
				importTable.setInt("index_id", importRow, targetIndexId);
				importTable.setInt("date", importRow, resetDate.getDate());
				importTable.setInt("start_date", importRow, startDate.getDate());
				importTable.setInt("ref_source", importRow, targetRefSourceId);
				importTable.setDouble("price", importRow, price);
			}
			Index.tableImportHistoricalPrices(importTable, errorLog);
			if(errorLog.getNumRows()>0){
				PluginLog.error("Error while saving to idx_historical_Prices table");
				throw new RuntimeException("Error while saving to idx_historical_Prices table ");
			}
		} catch (Exception exp) {
			PluginLog
					.error("\n Error while saving to idx_historical_prices table"
							+ exp.getMessage());
			throw new RuntimeException(exp.getMessage(), exp.getCause());

		} finally {
			if (Table.isTableValid(importTable) == 1) {
				importTable.destroy();
			}

			if (Table.isTableValid(userHistPriceConfig) == 1) {
				userHistPriceConfig.destroy();
			}

		}

	}

	/**
	 * calcPrice.
	 * This Method calculates the historical prices using the base price and the fx rate.
	 * price = base price/ fx rate.
	 * 
	 * @param idxDetailsMap the map containing all the indexes.
	 * @param userHistPriceConfig the index and ref sources for which 
	 * historical price needs to be calculated.
	 * @throws OException 
	 */
	
	private Table calcPrice(HashMap<String, IndexDetails> idxDetailsMap,
			Table userHistPriceConfig) throws OException {
		double fxRate = 0.0;
		int rowCount = userHistPriceConfig.getNumRows();
		userHistPriceConfig.addCol("target_curve_price", COL_TYPE_ENUM.COL_DOUBLE);
		userHistPriceConfig.addCol("target_curve_reset_date", COL_TYPE_ENUM.COL_DATE_TIME);
		userHistPriceConfig.addCol("target_curve_start_date", COL_TYPE_ENUM.COL_DATE_TIME);
		for (int row = 1; row <= rowCount; row++) {
			String baseCurve = userHistPriceConfig.getString("base_curve", row);
			String fxCurve = userHistPriceConfig.getString("fx_curve", row);
			if (idxDetailsMap.containsKey(baseCurve)
					&& idxDetailsMap.containsKey(fxCurve)) {
				double price = idxDetailsMap.get(baseCurve).getPrice();
				fxRate = idxDetailsMap.get(fxCurve).getPrice();
				if (BigDecimal.valueOf(fxRate).compareTo(BigDecimal.ZERO) == 0) {
					PluginLog.error("\n FX Rate for curve " + fxCurve
							+ " is found to be " + fxRate);
					continue;
				}
				double targetPrice = price / fxRate;
				ODateTime resetDate = idxDetailsMap.get(userHistPriceConfig.getString("base_curve", row)).getResetDate();
				ODateTime startDate = idxDetailsMap.get(userHistPriceConfig.getString("base_curve", row)).getStartDate();
				userHistPriceConfig.setDouble("target_curve_price", row, targetPrice);
				userHistPriceConfig.setDateTime("target_curve_reset_date", row, resetDate);
				userHistPriceConfig.setDateTime("target_curve_start_date", row, startDate);
			}
		}
		return userHistPriceConfig;
	}


	/**
	 * populateUserConfigMap.
	 * This Method creates a MAP of the index ref source combinations for 
	 * all the base curves and fx curves contained in the userHistPriceConfig table.
	 * 
	 * @param userHistPriceConfig the table containing index and ref sources combinations we want to work on.
	 * @throws OException 
	 */
	
	private HashMap<String, String> populateUserConfigMap(Table userHistPriceConfig)
			throws OException {

		HashMap<String, String> idxRefSrcConfigMap = new HashMap<String, String>();
		Table index = Table.tableNew();
		index.addCol("index_id", COL_TYPE_ENUM.COL_INT);

		int rowCount = userHistPriceConfig.getNumRows();
		for (int row = 1; row <= rowCount; row++) {
			String baseCurve = userHistPriceConfig.getString("base_curve", row);
			String fxCurve = userHistPriceConfig.getString("fx_curve", row);
			idxRefSrcConfigMap
					.put(baseCurve, userHistPriceConfig.getString(
							"base_reference_source", row));
			idxRefSrcConfigMap.put(fxCurve,
					userHistPriceConfig.getString("closing_dataset", row));
			int baseIndexId = Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE,
					baseCurve);
			int currRow = index.addRow();
			index.setInt("index_id", currRow, baseIndexId);
			int fxCurveId = Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE,
					fxCurve);
			currRow = index.addRow();
			index.setInt("index_id", currRow, fxCurveId);
		}
		return idxRefSrcConfigMap;

	}

	/**
	 * filterRefSources.
	 * This Method filters the idxHistPrices based ont he curve ref source contained in idxRefSrcConfigMap.
	 * 
	 * @param idxHistPrices the table all the indexes.
	 * @param idxRefSrcConfigMap containg the index ref source combinations we want to work on.
	 * @throws OException 
	 */
	private HashMap<String, IndexDetails> filterRefSources(Table idxHistPrices, HashMap<String, String> idxRefSrcConfigMap)
			throws OException {

		IndexDetails idxDtls;
		HashMap<String, IndexDetails> idxDtlsMap = new HashMap<String, IndexDetails>();
		int rowCount = idxHistPrices.getNumRows();
		for (int row = 1; row <= rowCount; row++) {
			int idxId = idxHistPrices.getInt("index_id", row);
			int refSourceId = idxHistPrices.getInt("ref_source", row);
			String refSource = Ref.getName(
					SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, refSourceId);
			String idx = Ref.getName(SHM_USR_TABLES_ENUM.INDEX_TABLE, idxId);

			String configRefSource = idxRefSrcConfigMap.get(idx);
			if (refSource.equalsIgnoreCase(configRefSource)) {
				double price = idxHistPrices.getDouble("price", row);
				ODateTime startDate = idxHistPrices.getDateTime("start_date",
						row);
				ODateTime resetDate = idxHistPrices.getDateTime("reset_date",
						row);
				idxDtls = new IndexDetails(idx, refSource, price, startDate,
						resetDate);
				idxDtlsMap.put(idx, idxDtls);
			}

		}
		if (idxDtlsMap.isEmpty()) {
			PluginLog
					.error("\n No matching Data found in idx_historical_table and USER_JM_SAP_AUTOPOP_HIST_PRICES");
			throw new OException(
					"\n No matching Data found in idx_historical_table and USER_JM_SAP_AUTOPOP_HIST_PRICES");
		}
		return idxDtlsMap;
	}

	/**
	 * loadIdxHistPrices.
	 * This Method loads all the records from idx_historical_prices table with the latest resetdate 
	 * for the indexes defined in userHistPriceConfig
	 * 
	 * @param userHistPriceConfig containg the indexes we want to work on.
	 * @return Table 
	 * @throws OException 
	 */
	
	private Table loadIdxHistPrices(Table userHistPriceConfig) throws OException {

		int queryId = -1;
		Table idxHistPrices = Util.NULL_TABLE;
		try {
			Table index = prepareQryResultIdxTbl(userHistPriceConfig);
			idxHistPrices = Table.tableNew();
			queryId = Query.tableQueryInsert(index, "index_id");
			String queryResultTable = Query.getResultTableForId(queryId);
			String SQL = "select * from idx_historical_prices ihp"
					+ " JOIN (SELECT max(hp.reset_date) AS reset_date, hp.index_id AS index_id,  hp.ref_source AS ref_source FROM idx_historical_prices hp group by hp.index_id,  hp.ref_source) selected_reset"
					+ "  ON selected_reset.reset_date = ihp.reset_date AND selected_reset.index_id = ihp.index_id AND selected_reset.ref_source = ihp.ref_source"
					+ " JOIN " + queryResultTable + " qr ON ihp.index_id = qr.query_result and qr.unique_id = " + queryId;


			PluginLog.info("\n About to run SQL - " + SQL);
			int ret = DBaseTable.execISql(idxHistPrices, SQL);
			if (ret < 1) {
				String message = DBUserTable.dbRetrieveErrorInfo(ret,
						"Error executing sql " + SQL);
				PluginLog.error(message);
				throw new OException(message);
			}

			PluginLog.info("\n Number of Rows returned "
					+ idxHistPrices.getNumRows());
		} catch (Exception exp) {
			PluginLog.error("\n Error while loading idx_historical_price data"
					+ exp.getMessage());
		} finally {
			Query.clear(queryId);
			
		}
		return idxHistPrices;
	}

	/**
	 * prepareQryResultIdxTbl.
	 * This Method creates aquery result table for all the indexes contained in 
	 * userHistPriceConfig table. The indexes in userHistPriceConfig are in string format
	 * and has to be converted to Ids before inserting them to query table. 
	 * 
	 * @param userHistPriceConfig table containing the index names
	 * @return Table
	 * @throws OException 
	 */
	private Table prepareQryResultIdxTbl(Table userHistPriceConfig) throws OException{
		
		Table index = Table.tableNew();
		index.addCol("index_id", COL_TYPE_ENUM.COL_INT);
		int rowCount = userHistPriceConfig.getNumRows();
		for (int row = 1; row <= rowCount; row++) {
			String baseCurve = userHistPriceConfig.getString("base_curve", row);
			String fxCurve = userHistPriceConfig.getString("fx_curve", row);
			int baseIndexId = Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE, baseCurve);
			int currRow = index.addRow();
			index.setInt("index_id", currRow, baseIndexId);
			int fxCurveId = Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE, fxCurve);
			currRow = index.addRow();
			index.setInt("index_id", currRow, fxCurveId);
		}
		return index;
	}

	/**
	 * loadUserHistPriceConfigs.
	 * This Method loads all the records from USER_JM_SAP_AUTOPOP_HIST_PRICES table.
	 * 
	 * @return Table
	 * @throws OException 
	 */
	private Table loadUserHistPriceConfigs() throws OException {

		String SQL = "SELECT * " + " FROM USER_jm_sap_autopop_hist_prices";
		Table userHistPriceConfig = Table.tableNew();
		PluginLog.info("\n About to run SQL - " + SQL);
		int ret = DBaseTable.execISql(userHistPriceConfig, SQL);
		if (ret < 1) {
			String message = DBUserTable.dbRetrieveErrorInfo(ret,
					"Error executing sql " + SQL);
			PluginLog.error(message);
			throw new OException(message);
		}
		PluginLog
				.info("\n Number of Rows returned from USER_JM_SAP_AUTOPOP_HIST_PRICES Table "
						+ userHistPriceConfig.getNumRows());
		return userHistPriceConfig;

	}

	private void init(IContainerContext context) throws OException {
		String abOutdir = Util.getEnv("AB_OUTDIR");
		ConstRepository constRepo = new ConstRepository(CONTEXT);
		String logLevel = constRepo.getStringValue("logLevel", "info");
		String logFile = constRepo.getStringValue("logFile", this.getClass()
				.getSimpleName() + ".log");
		String logDir = constRepo.getStringValue("logDir", abOutdir);
		try {
			PluginLog.init(logLevel, logDir, logFile);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		PluginLog.info(this.getClass().getName() + " started");
	}
}
