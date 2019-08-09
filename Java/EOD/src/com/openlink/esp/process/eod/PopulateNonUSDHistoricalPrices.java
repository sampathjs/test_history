package com.openlink.esp.process.eod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.Index;
import com.olf.openjvs.OCalendar;
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
 * to idx_historical_prices table. It relies on USER_JM_SAP_AUTOPOP_HIST_PRICES
 * to decide which index and ref source should be used to calculate the prices
 * and which index the price would be saved against.
 * 
 * @author YadavP03
 * @version 1.0
 */
public class PopulateNonUSDHistoricalPrices implements IScript {

	private final String CONTEXT = "EOD";
	private final String SUBCONTEXT = "NonUSDHistoricalPrices";
	private String metalsForHalfRounding = "";

	@Override
	public void execute(IContainerContext context) throws OException {
		try {
			init(context);
			process(context);
		} catch (Exception ex) {
			PluginLog.error(this.getClass().getName() + " terminated abnormaly: \n" + ex.toString());
			Util.exitFail("PopulateNonUSDHistoricalPrices script failed " + ex.getMessage());
			throw new OException(ex.getCause());
		}
		PluginLog.info(this.getClass().getName() + " ended successfully");
	}

	private void process(IContainerContext context) throws OException {
		String SQL = "SELECT * " + " FROM USER_jm_sap_autopop_hist_prices";
		Table userHistPriceConfig = runSQL(SQL);
		// Table userHistPriceConfig = loadUserHistPriceConfigs();
		HashMap<String, ArrayList<String>> idxRefSrcConfigMap = populateUserConfigMap(userHistPriceConfig);
		Table idxHistPrices = loadIdxHistPrices(userHistPriceConfig);
		HashMap<String, IndexDetails> idxDetailsMap = filterRefSources(idxHistPrices, idxRefSrcConfigMap);
		userHistPriceConfig = calcPrice(idxDetailsMap, userHistPriceConfig);
		saveHistPriceToDB(userHistPriceConfig);
	}

	/**
	 * saveHistPriceToDB. This Method creates a table of historical prices witht
	 * he required details from the input table passed. The table created is
	 * used to save the prices to the DB.
	 * 
	 * @param userHistPriceConfig
	 *            the table containing the prices to be saved.
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
				double price = userHistPriceConfig.getDouble("target_curve_price", row);
				if (BigDecimal.valueOf(price).compareTo(BigDecimal.ZERO) != 0) {
					String targetIndex = userHistPriceConfig.getString("target_price_curve", row);
					int targetIndexId = Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE, targetIndex);
					String targetRefSource = userHistPriceConfig.getString("target_reference_source", row);
					int targetRefSourceId = Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, targetRefSource);

					ODateTime resetDate = userHistPriceConfig.getDateTime("target_curve_reset_date", row);
					ODateTime startDate = userHistPriceConfig.getDateTime("target_curve_start_date", row);
					int importRow = importTable.addRow();
					importTable.setInt("index_id", importRow, targetIndexId);
					importTable.setInt("date", importRow, resetDate.getDate());
					importTable.setInt("start_date", importRow, startDate.getDate());
					importTable.setInt("ref_source", importRow, targetRefSourceId);
					importTable.setDouble("price", importRow, price);
				}

			}
			Index.tableImportHistoricalPrices(importTable, errorLog);
			if (errorLog.getNumRows() > 0) {
				PluginLog.error("Error while saving to idx_historical_Prices table");
				throw new RuntimeException("Error while saving to idx_historical_Prices table ");
			}
		} catch (Exception exp) {
			PluginLog.error("\n Error while saving to idx_historical_prices table" + exp.getMessage());
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
	 * calcPrice. This Method calculates the historical prices using the base
	 * price and the fx rate. price = base price/ fx rate.
	 * 
	 * @param idxDetailsMap
	 *            the map containing all the indexes.
	 * @param userHistPriceConfig
	 *            the index and ref sources for which historical price needs to
	 *            be calculated.
	 * @throws OException
	 */

	private Table calcPrice(HashMap<String, IndexDetails> idxDetailsMap, Table userHistPriceConfig) throws OException {
		double fxRate = 0.0;
		int rowCount = userHistPriceConfig.getNumRows();
		userHistPriceConfig.addCol("target_curve_price", COL_TYPE_ENUM.COL_DOUBLE);
		userHistPriceConfig.addCol("target_curve_reset_date", COL_TYPE_ENUM.COL_DATE_TIME);
		userHistPriceConfig.addCol("target_curve_start_date", COL_TYPE_ENUM.COL_DATE_TIME);
		for (int row = 1; row <= rowCount; row++) {

			String baseCurve = userHistPriceConfig.getString("base_curve", row);
			String baseCurveRefSrc = userHistPriceConfig.getString("base_reference_source", row);

			String fxCurve = userHistPriceConfig.getString("fx_curve", row);
			String fxCurveRefSrc = userHistPriceConfig.getString("closing_dataset", row);

			String targetCurve = userHistPriceConfig.getString("target_price_curve", row);

			String targetRefSource = userHistPriceConfig.getString("target_reference_source", row);

			IndexDetails baseCurveDtls = idxDetailsMap.get(baseCurve + "-" + baseCurveRefSrc);
			IndexDetails fxCurveDtls = idxDetailsMap.get(fxCurve + "-" + fxCurveRefSrc);

			if (baseCurveDtls != null && fxCurveDtls != null) {

				ODateTime resetDate = baseCurveDtls.getResetDate();
				double price = baseCurveDtls.getPrice();

				fxRate = fxCurveDtls.getPrice();

				if (BigDecimal.valueOf(fxRate).compareTo(BigDecimal.ZERO) == 0) {
					PluginLog.error("\n FX Rate for curve " + fxCurve + " is found to be " + fxRate);
					continue;
				}

				double targetPrice = convAndRoundPrice(price, fxRate, targetCurve);

				if (!isPriceChanged(targetCurve, resetDate, targetPrice, targetRefSource)) {
					PluginLog.info(" Price has not changed for Curve - " + targetCurve + " Reference Source - " + targetRefSource + " Price " + targetPrice);
					continue;
				}

				ODateTime startDate = baseCurveDtls.getStartDate();
				userHistPriceConfig.setDouble("target_curve_price", row, targetPrice);
				userHistPriceConfig.setDateTime("target_curve_reset_date", row, resetDate);
				userHistPriceConfig.setDateTime("target_curve_start_date", row, startDate);

			}
		}
		return userHistPriceConfig;
	}

	/**
	 * This method will convert the USD price using fxRate and round result to
	 * the closest HALP UP value. If it is exactly in the middle between two
	 * values, it will round up to the higher value. It will round to HALF
	 * (factor = 2)or QUARTER (Factor = 4) based on the configuration in
	 * constant repository. If the Metal is configured in const repo it will
	 * round to half by multiplying by 2 before rounding it HALF UP and dividing
	 * by 2 after rounding. If the metal is not configured it will multiply by 4
	 * rounding it HALF UP and and divide by 4.
	 * 
	 * @param price
	 *            the USD price to use.
	 * @param fxRate
	 *            the FX rate to be used for converting the USD price.
	 * 
	 * @throws OException
	 **/
	private double convAndRoundPrice(double price, double fxRate, String targetCurve) {

		double factor = 2.0;

		String[] curve = targetCurve.split("\\.");
		String metal = curve[0];

		if (!metalsForHalfRounding.contains(metal)) {
			factor = 4.0;
		}
		PluginLog.info("Parameters used for conversion Factor: " + factor + " USD Price: " + price + " Conversion Rate: " + fxRate);
		double convprice = factor * (price / fxRate);
		BigDecimal convpriceBD = BigDecimal.valueOf(convprice);
		convpriceBD = convpriceBD.setScale(0, RoundingMode.HALF_UP);
		double roundedPrice = convpriceBD.divide(BigDecimal.valueOf(factor)).doubleValue();
		PluginLog.info("Price after conversion and rounding applied " + roundedPrice + " for Curve " + targetCurve);
		return roundedPrice;

	}
	
	
	/** This method checks if the current price is different than the 
	 * existing price for the passed curve, reset date and ref source
	 * 
	 * @param targetCurve
	 *            the curve for which price is to be checked.
	 * @param resetDate
	 *            the reset Date for which price is to be checked.
	 * @param price
	 * 			  the price to be checked.
	 * @param targetRefSource
	 * 			  the refsource for which price is to be checked.
	 * 
	 * @throws OException
	 **/
	private boolean isPriceChanged(String targetCurve, ODateTime resetDate, double price, String targetRefSource) throws OException {

		boolean isPriceChanged = true;

		double oldPrice = loadHistPrices(targetCurve, resetDate, targetRefSource);
		if (BigDecimal.valueOf(price).compareTo(BigDecimal.valueOf(oldPrice)) == 0) {
			PluginLog.info("Price has not changed for index - " + targetCurve);
			isPriceChanged = false;
		}
		return isPriceChanged;

	}

	private double loadHistPrices(String targetCurve, ODateTime resetDate, String targetRefSource) throws OException {
		double price = 0.0;
		String resetDt = resetDate.formatForDbAccess();
		int refSrcId = Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, targetRefSource);
		int idxID = Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE, targetCurve);
		String SQL = "SELECT * FROM IDX_HISTORICAL_PRICES ihp " + " WHERE ihp.index_id =" + idxID + " AND ihp.reset_date = '" + resetDt + "'"
				+ " AND ihp.ref_source = " + refSrcId + " ORDER BY last_update DESC";
		Table histPrices = runSQL(SQL);
		if (histPrices.getNumRows() > 0) {
			price = histPrices.getDouble("price", 1);
		}

		return price;

	}

	/**
	 * populateUserConfigMap. This Method creates a MAP of the index ref source
	 * combinations for all the base curves and fx curves contained in the
	 * userHistPriceConfig table.
	 * 
	 * @param userHistPriceConfig
	 *            the table containing index and ref sources combinations we
	 *            want to work on.
	 * @throws OException
	 */

	private HashMap<String, ArrayList<String>> populateUserConfigMap(Table userHistPriceConfig) throws OException {

		HashMap<String, ArrayList<String>> idxRefSrcConfigMap = new HashMap<String, ArrayList<String>>();

		int rowCount = userHistPriceConfig.getNumRows();
		for (int row = 1; row <= rowCount; row++) {

			String baseCurve = userHistPriceConfig.getString("base_curve", row);
			String baseRefSrc = userHistPriceConfig.getString("base_reference_source", row);

			addEntryToMap(baseCurve, baseRefSrc, idxRefSrcConfigMap);

			String fxCurve = userHistPriceConfig.getString("fx_curve", row);
			String fxRefSrc = userHistPriceConfig.getString("closing_dataset", row);

			addEntryToMap(fxCurve, fxRefSrc, idxRefSrcConfigMap);

		}
		return idxRefSrcConfigMap;

	}

	private void addEntryToMap(String curve, String refSource, HashMap<String, ArrayList<String>> idxRefSrcConfigMap) {
		ArrayList<String> refsourceList = idxRefSrcConfigMap.get(curve);
		if (refsourceList == null) {
			refsourceList = new ArrayList<String>();
			refsourceList.add(refSource);
			idxRefSrcConfigMap.put(curve, refsourceList);
		}
		if (!refsourceList.contains(refSource)) {
			refsourceList.add(refSource);
		}

	}

	/**
	 * filterRefSources. This Method filters the idxHistPrices based ont he
	 * curve ref source contained in idxRefSrcConfigMap.
	 * 
	 * @param idxHistPrices
	 *            the table all the indexes.
	 * @param idxRefSrcConfigMap
	 *            containg the index ref source combinations we want to work on.
	 * @throws OException
	 */
	private HashMap<String, IndexDetails> filterRefSources(Table idxHistPrices, HashMap<String, ArrayList<String>> idxRefSrcConfigMap) throws OException {

		IndexDetails idxDtls;
		HashMap<String, IndexDetails> idxDtlsMap = new HashMap<String, IndexDetails>();
		int rowCount = idxHistPrices.getNumRows();
		for (int row = 1; row <= rowCount; row++) {
			int idxId = idxHistPrices.getInt("index_id", row);
			int refSourceId = idxHistPrices.getInt("ref_source", row);
			String refSource = Ref.getName(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, refSourceId);
			String idx = Ref.getName(SHM_USR_TABLES_ENUM.INDEX_TABLE, idxId);

			ArrayList<String> configRefSourceList = idxRefSrcConfigMap.get(idx);
			if (configRefSourceList.contains(refSource)) {
				double price = idxHistPrices.getDouble("price", row);
				ODateTime startDate = idxHistPrices.getDateTime("start_date", row);
				ODateTime resetDate = idxHistPrices.getDateTime("reset_date", row);
				idxDtls = new IndexDetails(idx, refSource, price, startDate, resetDate);
				idxDtlsMap.put(idx + "-" + refSource, idxDtls);
			}

		}
		if (idxDtlsMap.isEmpty()) {
			PluginLog.error("\n No matching Data found in idx_historical_table and USER_JM_SAP_AUTOPOP_HIST_PRICES");
			throw new OException("\n No matching Data found in idx_historical_table and USER_JM_SAP_AUTOPOP_HIST_PRICES");
		}
		return idxDtlsMap;
	}

	/**
	 * loadIdxHistPrices. This Method loads all the records from
	 * idx_historical_prices table with the latest resetdate for the indexes
	 * defined in userHistPriceConfig
	 * 
	 * @param userHistPriceConfig
	 *            containg the indexes we want to work on.
	 * @return Table
	 * @throws OException
	 */

	private Table loadIdxHistPrices(Table userHistPriceConfig) throws OException {

		int queryId = -1;
		Table idxHistPrices = Util.NULL_TABLE;
		try {
			int businessDate = Util.getBusinessDate();
			String businessDt = OCalendar.formatJdForDbAccess(businessDate);
			Table index = prepareQryResultIdxTbl(userHistPriceConfig);
			idxHistPrices = Table.tableNew();
			queryId = Query.tableQueryInsert(index, "index_id");
			String queryResultTable = Query.getResultTableForId(queryId);
			String SQL = " SELECT * FROM IDX_HISTORICAL_PRICES ihp \n"
					+ "	JOIN (SELECT MAX(hp.last_update) AS last_update, MAX(hp.reset_date) AS reset_date, hp.index_id AS index_id,"
					+ " hp.ref_source AS ref_source FROM IDX_HISTORICAL_PRICES hp \n"
					+ " group by hp.index_id,  hp.ref_source, hp.reset_date) selected_reset \n" + " ON (selected_reset.reset_date = ihp.reset_date)\n"
					+ " AND (selected_reset.index_id = ihp.index_id)" + " AND (selected_reset.ref_source = ihp.ref_source) \n" + " JOIN " + queryResultTable
					+ " qr ON (ihp.index_id = qr.query_result) AND qr.unique_id = " + queryId + " AND ihp.reset_date = '" + businessDt + "'";

			PluginLog.info("\n About to run SQL - " + SQL);
			int ret = DBaseTable.execISql(idxHistPrices, SQL);
			if (ret < 1) {
				String message = DBUserTable.dbRetrieveErrorInfo(ret, "Error executing sql " + SQL);
				PluginLog.error(message);
				throw new OException(message);
			}

			PluginLog.info("\n Number of Rows returned " + idxHistPrices.getNumRows());
		} catch (Exception exp) {
			PluginLog.error("\n Error while loading idx_historical_price data" + exp.getMessage());
		} finally {
			Query.clear(queryId);

		}
		return idxHistPrices;
	}

	/**
	 * prepareQryResultIdxTbl. This Method creates aquery result table for all
	 * the indexes contained in userHistPriceConfig table. The indexes in
	 * userHistPriceConfig are in string format and has to be converted to Ids
	 * before inserting them to query table.
	 * 
	 * @param userHistPriceConfig
	 *            table containing the index names
	 * @return Table
	 * @throws OException
	 */
	private Table prepareQryResultIdxTbl(Table userHistPriceConfig) throws OException {

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

	private Table runSQL(String query) throws OException {
		Table resultTable = Table.tableNew();
		PluginLog.info("\n About to run SQL - " + query);
		int ret = DBaseTable.execISql(resultTable, query);
		if (ret < 1) {
			String message = DBUserTable.dbRetrieveErrorInfo(ret, "Error executing sql " + query);
			PluginLog.error(message);
			throw new OException(message);
		}
		PluginLog.info("\n Number of Rows returned from result Table " + resultTable.getNumRows());
		return resultTable;
	}

	private void init(IContainerContext context) throws OException {
		String abOutdir = Util.getEnv("AB_OUTDIR");
		ConstRepository constRepo = new ConstRepository(CONTEXT, SUBCONTEXT);
		String logLevel = constRepo.getStringValue("logLevel", "info");
		String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
		String logDir = constRepo.getStringValue("logDir", abOutdir);
		metalsForHalfRounding = constRepo.getStringValue("metalsForHalfRounding");
		try {
			PluginLog.init(logLevel, logDir, logFile);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		PluginLog.info(this.getClass().getName() + " started");
	}
}
