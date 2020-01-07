/* Released with version 29-Oct-2015_V14_2_4 of APM */

package jvs.scripts;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

/* 
 * For V14 and up 
 * 
 * NOTE: This is a reconstructed "leg-for-leg" version JVS script for APM Power Position. Some 
 * comments from the AVS file as well as functionalities and logic are kept almost the same.
 * Please refer more notes in AVS file APM_UDSR_PowerPosition.mls, which was Released with version 25-Oct-2012_V3_7_1 of APM.
 * 
 * - Removed some redundant version check. Because this development is for V14 and up; 
 * - Changed some function names 
 * - Made some sub-function calls inside computeResult/compute_result; Logic is kept the same;
 * - Applied DTS107747
 * - Updated "power_timezone" query for PWR_STD_PRODUCT.
 */

public class APM_UDSR_PowerPosition implements IScript {
	
	private static final int MAX_NUMBER_OF_DB_RETRIES = 10;
	
	/*
	 * This flag controls whether volumes associated with exercised or expired option status are displayed by the simulation result. Default value is
	 * 1 to exclude such statuses.
	 */
	static private int iExcludeKnownOptionTypes = 1;
	static private int iRollupDailyAndLowerGranularity = 1;
	static private String SETTLEMENT_TYPE_COL_NAME = "settlement_type";
	static private String FIX_FLOAT_COL_NAME = "fix_float";

	static private final int NO_ELIGIBLE_TRANSACTION = -1;
	static private final int DEFAULT_SUB_HOURLY_GRANULARITY = 15;

	private int iToday;
	private Table argt;
	private Table returnt;
	private Boolean isDebug;
	
	private int iMonthlyDateSequence;
	private int iHistMonthlyStart;
	private int iHistDailyStart;
	private int iHistHourlyStart;
	private int iHistSubHourlyStart;

	private int tsdQuerySet;
	private int rowNumInQueryCriteria;

	private int iFwdStartDate;
	private int iSubHourlyEndPoint;
	private int iHourlyEndPoint;
	private int iDailyEndPoint;
	private int iMonthlyEndPoint;

	public APM_UDSR_PowerPosition() throws OException {
		iToday = OCalendar.today();
		isDebug = true;
		
		iMonthlyDateSequence = 0;
		iHistMonthlyStart = 0;
		iHistDailyStart = 0;
		iHistHourlyStart = 0;
		iHistSubHourlyStart = 0;

		tsdQuerySet = -1;

		iFwdStartDate = iToday;
		iSubHourlyEndPoint = 0;
		iHourlyEndPoint = 0;
		iDailyEndPoint = 0;
		iMonthlyEndPoint = 0;
	}
	
	public void execute(IContainerContext context) throws OException {
		argt = context.getArgumentsTable();
		returnt = context.getReturnTable();

		USER_RESULT_OPERATIONS operation;
		operation = USER_RESULT_OPERATIONS.fromInt(argt.getInt("operation", 1));

		switch (operation) {
		case USER_RES_OP_CALCULATE:
			computeResult(argt.getTable("sim_def", 1));
			break;
		case USER_RES_OP_FORMAT:
			formatResult();
			break;
		default:
			logDebugMessage("Incorrect operation code");
			argt.setString("error_msg", 1, "Incorrect operation code");
			Util.exitFail();
		}

		if (isDebug) {
			logDebugMessage("\n JVS script was used to generate this APM Power Pos\n\n");
		}

		Util.exitSucceed();
	}

	// Get Date value from Sim Result setting;
	public int getParamDateValue(Table tResConfig, String sParamName) throws OException {
		int iDate = 0;
		String value;

		value = getParamStrValue(tResConfig, sParamName);

		if (Str.len(value) > 0)
			iDate = OCalendar.parseString(value);

		return iDate;
	}

	// Get Int value from Sim Result setting;
	public int getParamIntValue(Table tResConfig, String sParamName) throws OException {
		return Str.strToInt(getParamStrValue(tResConfig, sParamName));
	}

	// Get String value from Sim Result setting; All values are get by string
	// and then converted to other types
	public String getParamStrValue(Table tResConfig, String sParamName) throws OException {
		int iRow;
		String sVal;

		iRow = tResConfig.findString("res_attr_name", sParamName, SEARCH_ENUM.FIRST_IN_GROUP);

		sVal = tResConfig.getString("value", iRow);

		return sVal;
	}

	// Check whether Sim Result setting has value set for the parameter
	public int paramHasValue(Table tResConfig, String sParamName) throws OException {
		String sValue;

		sValue = getParamStrValue(tResConfig, sParamName);

		if ((Str.len(sValue) > 0) && Str.isEmpty(sValue) == 0) {
			return 1;
		}

		return 0;
	}

	public void logDebugMessage(String sProcessingMessage) throws OException {
		String msg;
		msg = OCalendar.formatDateInt(OCalendar.getServerDate(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH) + " " + Util.timeGetServerTimeHMS() + " : "
				+ sProcessingMessage + "\n";
		OConsole.oprint(msg);
	}

	private int compareRows(Table table, int row1, int row2) throws OException {
		int col;
		int num_cols;

		// Returns 1 if they match
		// Only compares INT columns ... ignores the rest. This is sufficient for now
		num_cols = table.getNumCols();
		for (col = 1; col <= num_cols; col++) {
			COL_TYPE_ENUM colType = COL_TYPE_ENUM.fromInt(table.getColType(col));
			switch (colType) {
			case COL_INT:
				if (table.getInt(col, row1) != table.getInt(col, row2)){
					return 0;
				}
				break;
			default:
				break;
			}
		}
		return 1;
	}

	private void addRolledUpRow(int iNumCols, Table tPositions, double dVolumetricEnergy, double dPrice, int iBavFlag, int iPositionRange,
			int iLocStartingDate, int iLocStartingTime, int iLocStoppingDate, int iLocStoppingTime, int iPriceBand, Table tArrayPositions, int iLRow,
			int bRollUp) throws OException {
		int k;
		String sColName;
		int iNewRow;
		int iVolumetricEnergyCol = 0;
		double dPrevVolumetricEnergy;
		// Add row
		iNewRow = tPositions.addRow();
		if (iNewRow == 1){
			bRollUp = 0; // Can't roll up 1st row
		}
		// now cycle around all the cols
		for (k = 1; k <= iNumCols; k++) {
			sColName = tPositions.getColName(k);

			if (sColName.equals("volumetric_energy")) {
				iVolumetricEnergyCol = k;
			} else if (sColName.equals("price")) {
				tPositions.setDouble(k, iNewRow, dPrice);
			} else if (sColName.equals("bav_flag")) {
				tPositions.setInt(k, iNewRow, iBavFlag);
			} else if (sColName.equals("position_range")) {
				tPositions.setString(k, iNewRow, Str.intToStr(iPositionRange));
			} else if (sColName.equals("position_range_int")) {
				tPositions.setInt(k, iNewRow, iPositionRange);
			} else if (sColName.equals("startdate")) {
				tPositions.setInt(k, iNewRow, iLocStartingDate);
			} else if (sColName.equals("start_time")) {
				tPositions.setInt(k, iNewRow, iLocStartingTime);
			} else if (sColName.equals("enddate")) {
				tPositions.setInt(k, iNewRow, iLocStoppingDate);
			} else if (sColName.equals("end_time")) {
				tPositions.setInt(k, iNewRow, iLocStoppingTime);
			} else if (sColName.equals("price_band")) {
				tPositions.setInt(k, iNewRow, iPriceBand);
			}
			// fall through if its not an array type
			else {
				COL_TYPE_ENUM colType = COL_TYPE_ENUM.fromInt(tPositions.getColType(k));
				switch (colType) {
				case COL_INT:
					tPositions.setInt(k, iNewRow, tArrayPositions.getInt(sColName, iLRow));
					break;
				case COL_DOUBLE:
					tPositions.setDouble(k, iNewRow, tArrayPositions.getDouble(sColName, iLRow));
					break;
				case COL_DATE_TIME:
					tPositions.setDateTime(k, iNewRow, tArrayPositions.getDateTime(sColName, iLRow));
					break;
				default:
					break;
				}
			}
		}

		// If we're rolling up and this row matches the previous one
		if (bRollUp != 0)
			bRollUp = compareRows(tPositions, iNewRow, iNewRow - 1);

		if (bRollUp != 0) {
			dPrevVolumetricEnergy = tPositions.getDouble(iVolumetricEnergyCol, iNewRow - 1);
			tPositions.setDouble(iVolumetricEnergyCol, iNewRow - 1, dVolumetricEnergy + dPrevVolumetricEnergy);
			tPositions.delRow(iNewRow);
		} else {
			tPositions.setDouble(iVolumetricEnergyCol, iNewRow, dVolumetricEnergy);
		}
	}

	private Table expandPwrTable(Table tArrayPositions, int iCollapseDates, int iIncrementType) throws OException {
		Table tCols, tVolumetric_energy, tPrice, tBav_flag, tPosition_range, tPriceBand = null;
		Table tLoc_starting_period, tLoc_stopping_period;
		Table tAllPositions = Util.NULL_TABLE;
		Table tPositions = Util.NULL_TABLE;
		Table tOutputTableFormatCached = Util.NULL_TABLE;
		String colName;
		String strCachedTableName = "Power Template";
		COL_TYPE_ENUM colType;
		int i, j, row, numRows, numCols;
		int bPriceBandArray = 0;
		int iBavFlag, iPriceBand = 0, iLocStartingDate = 0, iLocStartingTime = 0, iLocStoppingDate = 0, iLocStoppingTime = 0, iPositionRange;
		int iExpandedCount, iNumLRows, iVolumetricEnergyCol;
		int iSettlementType, iFixFloat, bInterestedInThisRowsL, iSettlementTypeCol, iFixFloatCol;
		String errorMessage;
		int bCacheTableFormat;

		double dVolumetricEnergy, dPrice;
		ODateTime dtLocStartingPeriod, dtLocStoppingPeriod;

		if (tArrayPositions.getColNum("volumetric_energy") > 0 && tArrayPositions.getColType("volumetric_energy") == COL_TYPE_ENUM.COL_DOUBLE.toInt()){
			return tArrayPositions;
		}

		logDebugMessage("USR_APM_PowerPosition starting to expand L-Shaped data ...");

		// cycle around the array table and create a column for every non array based col
		// we don't want to do this for every col as we only use a subset so look in the apm_table_columns
		// for the ones we want (plus a few others !)

		tOutputTableFormatCached = Table.getCachedTable(strCachedTableName);
		if (Table.isTableValid(tOutputTableFormatCached) == 0) {
			tCols = Table.tableNew("Power Cols");
			//first retrieve the dimensions that are turned on sourced from this result
			if (APM_TABLE_LoadFromDbWithSQL(tCols, 	"distinct a.result_column_name", 
													"apm_table_columns a join apm_pkg_enrichment_config b on a.enrichment_name = b.enrichment_name", 
													"result_enum_name = 'USER_RESULT_APM_POWER_POSITION' and b.on_off_flag = 1") != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) 
			{
				// raise failure
				tCols.destroy();
				errorMessage = "APM_UDSR_PowerPosition ERROR: Failed to retrieve dimensions for result from apm_table_columns";
				logDebugMessage(errorMessage);
				argt.setString("error_msg", 1, errorMessage);
				return Util.NULL_TABLE;
			}
			else
			{
				// now append the non dimension based columns
				Table tCols2 = Table.tableNew("Power Cols");
				if (APM_TABLE_LoadFromDbWithSQL(tCols2, "result_column_name", 
														"apm_table_columns", 
														"result_enum_name = 'USER_RESULT_APM_POWER_POSITION' and (enrichment_name is null or enrichment_name =  '' or enrichment_name = ' ')") != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) 
				{
					// raise failure
					tCols2.destroy();
					errorMessage = "APM_UDSR_PowerPosition ERROR: Failed to retrieve columns for result from apm_table_columns";
					logDebugMessage(errorMessage);
					argt.setString("error_msg", 1, errorMessage);
					return Util.NULL_TABLE;
				}
				tCols2.copyRowAddAll(tCols);
				tCols2.destroy();
			}

			// I also know that these are some of the extra arrays we are interested in
			row = tCols.addRow();
			tCols.setString(1, row, "price");
			row = tCols.addRow();
			tCols.setString(1, row, "bav_flag");
			row = tCols.addRow();
			tCols.setString(1, row, "tran_num");

			tOutputTableFormatCached = Table.tableNew(strCachedTableName);
			bCacheTableFormat = 1;

			// cycle around the Array based table and check the cols exist
			for (i = 1; i <= tCols.getNumRows(); i++) {
				colName = tCols.getString(1, i);
				if (tArrayPositions.getColNum(colName) > 0) {
					colType = COL_TYPE_ENUM.fromInt(tArrayPositions.getColType(colName));
					switch (colType) {
					case COL_INT:
						tOutputTableFormatCached.addCol(colName, colType);
						tOutputTableFormatCached.addGroupBy(colName);
						break;
					case COL_DOUBLE:
						tOutputTableFormatCached.addCol(colName, colType);
						break;
					case COL_STRING:
						tOutputTableFormatCached.addCol(colName, colType);
						break;
					case COL_INT_ARRAY:
						if (Str.equal(colName, "position_range") == 1) {
							// special case - we need both int and str versions of this so that we can key
							// and also copy the right dates across for daily/monthly granularity
							// converting this id into a String for later conversion in Pserver
							tOutputTableFormatCached.addCol("position_range", COL_TYPE_ENUM.COL_STRING);
							tOutputTableFormatCached.addCol("position_range_int", COL_TYPE_ENUM.COL_INT);
							break;
						}
						tOutputTableFormatCached.addCol(colName, COL_TYPE_ENUM.COL_INT);
						tOutputTableFormatCached.addGroupBy(colName);
						break;
					case COL_DOUBLE_ARRAY:
						tOutputTableFormatCached.addCol(colName, COL_TYPE_ENUM.COL_DOUBLE);
						break;
					case COL_DATE_TIME_ARRAY:
						tOutputTableFormatCached.addCol(colName, COL_TYPE_ENUM.COL_DATE_TIME);
						break;
					}
				} else {
					if (tArrayPositions.getNumRows() == 0) {
						// no rows so ignore but don't cache the table
						// the format might be wrong due to a bad early exit from the power fn
						bCacheTableFormat = 0;
					} else {
						// log an error and fail the result
						// this means we are expecting a column - but it isn't there.
						// not good as the APM join will fail later on
						errorMessage = "APM_UDSR_PowerPosition ERROR: Failed to find expected column '" + colName
								+ "' in table returned by power function";
						logDebugMessage(errorMessage);
						argt.setString("error_msg", 1, errorMessage);
						return Util.NULL_TABLE;
					}
				}
			}

			// we now have our output table format - save it in memory so we don't have to do this every time
			if (bCacheTableFormat == 1){
				Table.cacheTable(strCachedTableName, tOutputTableFormatCached);
			}

			// destroy intermediate tables
			tCols.destroy();
		}

		// clone format and make sure this ptr isn't cleaned up
		tAllPositions = tOutputTableFormatCached.cloneTable(); // Holds sum of all deals
		tPositions = tOutputTableFormatCached.cloneTable(); // Handle's one deal at a time

		iVolumetricEnergyCol = tPositions.getColNum("volumetric_energy");

		numCols = tPositions.getNumCols();
		tOutputTableFormatCached = Util.NULL_TABLE; // don't destroy this table as it's actually the cached table

		// Have a guess at how many rows's we'll add to avoid mem fragmentation.
		// Assumptoin here is that we expand each L-shaped row to about 2 rows
		// This is hard to guess and depends on hourly vs daily vs monthly granularity as well as deal characteristics
		iNumLRows = tArrayPositions.getNumRows();

		tAllPositions.tuneGrowth(iNumLRows * 2);
		tPositions.tuneGrowth(100);

		if (tArrayPositions.getColType("price_band") == COL_TYPE_ENUM.COL_INT_ARRAY.toInt())
			bPriceBandArray = 1;
		// get any col nums we need before the loop
		iSettlementTypeCol = tArrayPositions.getColNum(SETTLEMENT_TYPE_COL_NAME);
		iFixFloatCol = tArrayPositions.getColNum(FIX_FLOAT_COL_NAME);

		// now cycle around the array based table
		// unpack the arrays and copy into the output format
		iExpandedCount = 0;
		for (i = 1; i <= iNumLRows; i++) {
			// we use this flag to allow skipping of certain rows
			bInterestedInThisRowsL = 1;

			// ///////////////////////////////////////////////////////
			//
			// To match the logic in the standard RTP_PwrPos.java RTPE script
			// we are skipping the fixed rate legs of cash settled deals
			//
			// ////////////////////////////////////////////////////////
			iSettlementType = tArrayPositions.getInt(iSettlementTypeCol, i);
			iFixFloat = tArrayPositions.getInt(iFixFloatCol, i);
			if ((iSettlementType == OPTION_SETTLEMENT_TYPE.SETTLEMENT_TYPE_CASH.toInt()) && (iFixFloat == FIXED_OR_FLOAT.FIXED_RATE.toInt()))
				bInterestedInThisRowsL = 0;

			// Add any future skipping logic here....

			// we don't necessarily take all l-shaped rows forward into our expanded data
			if (bInterestedInThisRowsL != 0) {
				// set up all the arrays
				tVolumetric_energy = tArrayPositions.createTableFromArray(tArrayPositions.getColNum("volumetric_energy"), i);
				tPrice = tArrayPositions.createTableFromArray(tArrayPositions.getColNum("price"), i);
				tBav_flag = tArrayPositions.createTableFromArray(tArrayPositions.getColNum("bav_flag"), i);
				tPosition_range = tArrayPositions.createTableFromArray(tArrayPositions.getColNum("position_range"), i);
				tLoc_starting_period = tArrayPositions.createTableFromArray(tArrayPositions.getColNum("loc_starting_period"), i);
				tLoc_stopping_period = tArrayPositions.createTableFromArray(tArrayPositions.getColNum("loc_stopping_period"), i);

				if (bPriceBandArray != 0)
					tPriceBand = tArrayPositions.createTableFromArray(tArrayPositions.getColNum("price_band"), i);
				else
					iPriceBand = tArrayPositions.getInt("price_band", i);

				numRows = tVolumetric_energy.getNumRows();
				iExpandedCount += numRows; // Keep count of the number of rows expanded from L shaped data

				// now for every row in the arrays we copy the data in & set all
				// the other fields - ugh
				for (j = 1; j <= numRows; j++) {
					iBavFlag = tBav_flag.getInt(1, j);
					iPositionRange = tPosition_range.getInt(1, j);
					if (bPriceBandArray != 0)
						iPriceBand = tPriceBand.getInt(1, j);

					// If we're collapsing dates, we're collapsing by position range. The dates for each row are not interesting.
					// The start/end datetimes will be enriched later from tTimeframes
					if (iCollapseDates == 0) {
						dtLocStoppingPeriod = tLoc_stopping_period.getDateTime(1, j);
						iLocStoppingDate = dtLocStoppingPeriod.getDate();
						iLocStoppingTime = dtLocStoppingPeriod.getTime();

						dtLocStartingPeriod = tLoc_starting_period.getDateTime(1, j);
						iLocStartingDate = dtLocStartingPeriod.getDate();
						iLocStartingTime = dtLocStartingPeriod.getTime();
					}

					dVolumetricEnergy = tVolumetric_energy.getDouble(1, j);
					dPrice = tPrice.getDouble(1, j);

					addRolledUpRow(numCols, tPositions, dVolumetricEnergy, dPrice, iBavFlag, iPositionRange, iLocStartingDate, iLocStartingTime,
							iLocStoppingDate, iLocStoppingTime, iPriceBand, tArrayPositions, i, iCollapseDates);

				} // end j for

				// cleanup array tables
				tVolumetric_energy.destroy();
				tPrice.destroy();
				tBav_flag.destroy();
				tPosition_range.destroy();
				tLoc_starting_period.destroy();
				tLoc_stopping_period.destroy();

				if (bPriceBandArray != 0)
					tPriceBand.destroy();

			} // end skip row if

			// Are we done with a tran or last row ...
			if ((i == iNumLRows) || (tArrayPositions.getInt("tran_num", i) != tArrayPositions.getInt("tran_num", i + 1))) {
				if (iCollapseDates != 0) {
					// Group and roll up again ...
					tPositions.groupBy();
					numRows = tPositions.getNumRows();
					// now for every row in the arrays we copy the data in & set all the other fields
					for (j = numRows; j > 1; j--) {
						if (compareRows(tPositions, j, j - 1) != 0) {
							tPositions.setDouble(iVolumetricEnergyCol, j - 1,
									tPositions.getDouble(iVolumetricEnergyCol, j - 1) + tPositions.getDouble(iVolumetricEnergyCol, j));
							tPositions.delRow(j);
						}
					}
				}
				tPositions.copyRowAddAll(tAllPositions);
				tPositions.clearRows();
			}
		} // end i for

		logDebugMessage("USR_APM_PowerPosition : Converted " + tArrayPositions.getNumRows() + " L-shaped rows (expanding to " + iExpandedCount
				+ " rows) to " + tAllPositions.getNumRows() + " rows of schedule details");

		tArrayPositions.destroy();
		tPositions.destroy();

		return tAllPositions;
	}

	private void printPowerFnParams(int iIncrementType, ODateTime dtStartingPeriod, ODateTime dtStoppingPeriod, int iDateSequence, int iTimeZone,
			Table tQueryCriteria) throws OException {
		int row;
		String sGranularity, sDateSequence, sShowAll;

		sDateSequence = "None";
		if (iIncrementType == PWR_INCREMENT_TYPE_ENUM.INCREMENT_TYPE_15_MINUTE.toInt()) {
			sGranularity = "15_Minute";
		} else if (iIncrementType == PWR_INCREMENT_TYPE_ENUM.INCREMENT_TYPE_60_MINUTE.toInt()) {
			sGranularity = "Hourly";
		} else if (iIncrementType == -1 && iDateSequence == ROLL_CONV_ENUM.RC_CALENDAR_DAY.toInt()) {
			sGranularity = "Daily";
		} else if (iIncrementType == -1 && iDateSequence == 2) {
			sGranularity = "Monthly";
		} else if (iIncrementType == -1 && iDateSequence > 2) {
			sGranularity = "Monthly";
			sDateSequence = Ref.getShortName(SHM_USR_TABLES_ENUM.DATE_SEQUENCE_TABLE, iDateSequence + 1000);
		} else {
			sGranularity = "Unknown";
		}

		sShowAll = "Yes";
		for (row = 1; row <= tQueryCriteria.getNumRows(); row++) {
			if (Str.findSubString(tQueryCriteria.getString("query_where_str", row), "bav_flag") != Util.NOT_FOUND) {
				sShowAll = "No";
				break;
			}
		}

		logDebugMessage("Calling Power.generateVolumetricPositions...Params..." + "Start Date:" + OCalendar.formatJd(dtStartingPeriod.getDate())
				+ ", End Date: " + OCalendar.formatJd(dtStoppingPeriod.getDate()) + ", Timezone: "
				+ Ref.getShortName(SHM_USR_TABLES_ENUM.TIME_ZONE_TABLE, iTimeZone) + ", Granularity: " + sGranularity + ", Date Sequence: "
				+ sDateSequence + ", Show All Statuses: " + sShowAll);
	}

	private Table getResults(int iQueryId, ODateTime dtStartingPeriod, ODateTime dtStoppingPeriod, int iTimeZone, int iIncrementType,
			int iDateSequence, int iValuationProduct, Table tQueryCriteria, int iDateFormat, int iDateLocale, int iTimeFormat, int iCollapseDates,
			Table tAllTimeFrames) throws OException {
		int iRow, iEndDate, iEndTime, bMsgLogged;
		Table tTimeframes, tPositions, tArrayPositions;
		Table tDateManipulationTable, tExistingTimeFrames;
		String errorMessage;
		ODateTime dtLocStartingPeriod;
		ODateTime dtLocStoppingPeriod;
		int iLocStartingPeriodCol;
		int iLocStoppingPeriodCol;
		int iStartDateCol, iEndDateCol, iStartTimeCol, iEndTimeCol;

		tTimeframes = Table.tableNew("timeframes");

		printPowerFnParams(iIncrementType, dtStartingPeriod, dtStoppingPeriod, iDateSequence, iTimeZone, tQueryCriteria);
		tArrayPositions = Power.generateVolumetricPositions(iQueryId, dtStartingPeriod, dtStoppingPeriod, iTimeZone, iIncrementType, iDateSequence,
				iValuationProduct, 0, tQueryCriteria, iDateFormat, iDateLocale, iTimeFormat, 1, tTimeframes, 1);
		logDebugMessage("USR_APM_PowerPosition ... Power.generateVolumetricPositions complete");
		if(Table.isTableValid(tArrayPositions)==0 || Table.isTableValid(tTimeframes)==0)
		{
			logDebugMessage("USR_APM_PowerPosition ... Error: No data returned by PWR_GenerateVolumetricPositions");
			errorMessage = argt.getString("error_msg", 1);
			argt.setString("error_msg", 1, errorMessage + ". No data returned by PWR_GenerateVolumetricPositions");
			Util.exitFail();
		}
		// APM metadata calls valuation_product pwr_product_id ...
		tArrayPositions.setColName(tArrayPositions.getColNum("valuation_product"), "pwr_product_id");

		// take the positions in array format and generate a table that is expanded (i.e. non array'd)
		tPositions = expandPwrTable(tArrayPositions, iCollapseDates, iIncrementType);

		if (Table.isTableValid(tPositions) == 0) {
			logDebugMessage("USR_APM_PowerPosition ... No data returned by the Power Position fn");
			errorMessage = argt.getString("error_msg", 1);
			argt.setString("error_msg", 1, errorMessage + ". No data returned by the Power Position fn");
			Util.exitFail();
		}

		// DEBUG MSG
		logDebugMessage("USR_APM_PowerPosition started post-processing");

		tPositions.mathMultCol("volumetric_energy", "price", "cost");
		tPositions.mathMultCol("volumetric_energy", "bav_flag", "bav_volume");
		tPositions.mathMultCol("cost", "bav_flag", "bav_cost");

		// Now sort out dates ...
		// For end time, we want to change 32767:0 to 32766:86400 for better bucketing on client
		// If we are collapsing dates, we are replacing the time period of delivery with its overall range's start and end points.
		// As such, we manipulate the dates in tTimeFrames and select across once done (daily and monthly). 
		// For 'natural' granularity we simply manipulate the dates on the data itself

		if (iCollapseDates != 0) {
			// Add start and end date columns for APM filtering and bucketing
			// purposes
			tTimeframes.addCol("startdate", COL_TYPE_ENUM.COL_INT);
			tTimeframes.addCol("enddate", COL_TYPE_ENUM.COL_INT);
			tTimeframes.addCol("start_time", COL_TYPE_ENUM.COL_INT);
			tTimeframes.addCol("end_time", COL_TYPE_ENUM.COL_INT);

			tDateManipulationTable = tTimeframes;
		} else {
			tDateManipulationTable = tPositions;
		}

		iLocStartingPeriodCol = tDateManipulationTable.getColNum("loc_starting_period");
		iLocStoppingPeriodCol = tDateManipulationTable.getColNum("loc_stopping_period");
		iStartDateCol = tDateManipulationTable.getColNum("startdate");
		iEndDateCol = tDateManipulationTable.getColNum("enddate");
		iStartTimeCol = tDateManipulationTable.getColNum("start_time");
		iEndTimeCol = tDateManipulationTable.getColNum("end_time");

		bMsgLogged = 0;
		for (iRow = 1; iRow <= tDateManipulationTable.getNumRows(); iRow++) {

			if (iCollapseDates != 0) {
				dtLocStartingPeriod = tDateManipulationTable.getDateTime(iLocStartingPeriodCol, iRow);
				tDateManipulationTable.setInt(iStartDateCol, iRow, dtLocStartingPeriod.getDate());
				tDateManipulationTable.setInt(iStartTimeCol, iRow, dtLocStartingPeriod.getTime());

				// For end time, we want to change 32767:0 to 32766:86400 for better bucketing on client
				dtLocStoppingPeriod = tDateManipulationTable.getDateTime(iLocStoppingPeriodCol, iRow);
				iEndDate = dtLocStoppingPeriod.getDate();
				iEndTime = dtLocStoppingPeriod.getTime();
			} else {
				iEndDate = tDateManipulationTable.getInt(iEndDateCol, iRow);
				iEndTime = tDateManipulationTable.getInt(iEndTimeCol, iRow);
			}

			/*
			 * Note that the datetime representing midnight can be stored as either "DATE=X, TIME=0" or "DATE=X-1, TIME=86400". Normally, the output
			 * for start of period will be in the former format, whilst the output for the end of period will be in the latter format, which is what
			 * APM client expects. However, in case the end point is returned as "DATE=X, TIME=0", change its format appropriately.
			 */
			if (iEndTime == 0) {
				iEndDate--;
				iEndTime = 86400;
				if (bMsgLogged == 0) {
					logDebugMessage("USR_APM_PowerPosition : Info : Fixed up zero end time");
					bMsgLogged = 1;
				}
			}

			tDateManipulationTable.setInt(iEndDateCol, iRow, iEndDate);
			tDateManipulationTable.setInt(iEndTimeCol, iRow, iEndTime);
		}

		if (iCollapseDates != 0) {
			tPositions.select(tTimeframes, "startdate, enddate, start_time, end_time", "offset EQ $position_range_int");
		}

		tExistingTimeFrames = tAllTimeFrames.getTable(1, 1);
		if (tExistingTimeFrames != Util.NULL_TABLE && Table.isTableValid(tExistingTimeFrames) == 1) {
			tExistingTimeFrames = tAllTimeFrames.getTable(1, 1);
			tTimeframes.copyRowAddAll(tExistingTimeFrames);
			tExistingTimeFrames.sortCol("offset");
		} else{
			tAllTimeFrames.setTable(1, 1, tTimeframes);
		}

		// DEBUG MSG
		logDebugMessage("USR_APM_PowerPosition getResults finished post-processing");

		return tPositions;
	}

	// Same as GetResults() but uses time chunks. TODO: It must be merge with the GetResults() (AVS comment)
	private void getResultsMonthly(int iQueryId, ODateTime dtStartingPeriod, ODateTime dtStoppingPeriod, int iTimeZone, int iIncrementType,
			int iDateSequence, int iValuationProduct, Table tQueryCriteria, int iDateFormat, int iDateLocale, int iTimeFormat, int iCollapseDates,
			String sMonthPeriod) throws OException {
		int iRow, iEndDate, iEndTime, bMsgLogged;
		Table tTimeframes = null, tPositions = null;
		Table tDateManipulationTable;
		String errorMessage;
		ODateTime dtLocStartingPeriod;
		ODateTime dtLocStoppingPeriod;
		int iLocStartingPeriodCol;
		int iLocStoppingPeriodCol;

		int iStartDateCol, iEndDateCol, iStartTimeCol, iEndTimeCol;

		int run, first_chunk;

		ODateTime chunkStart;
		ODateTime chunkEnd;

		logDebugMessage("Generating power position result starting from [" + Str.dtToString(dtStartingPeriod) + "] stopping["
				+ Str.dtToString(dtStoppingPeriod) + "]");

		chunkStart = ODateTime.dtNew();
		chunkStart.setDate(dtStartingPeriod.getDate());
		chunkStart.setTime(dtStartingPeriod.getTime());

		chunkEnd = ODateTime.dtNew();

		run = 1;
		first_chunk = 1;

		do // time chunk loop
		{
			Table tArrayPositionsChunk, tPositionsChunk, tTimeframesChunk;

			ODateTime temp;

			// set the end point for the current chunk
			chunkEnd.setTime(chunkStart.getTime());

			// ----------------------------------------------------------------------
			// Time chunk duration is either based on month end (to fit in with
			// profile periods), or
			// the specified monthly date sequence, if set.
			if (Str.isEmpty(sMonthPeriod) != 0) {
				chunkEnd.setDate(OCalendar.getEOM(chunkStart.getDate()) + 1);
			} else {
				chunkEnd.setDate(OCalendar.parseStringWithHolId("1cd>1" + sMonthPeriod, 0, chunkStart.getDate()));
			}
			// ----------------------------------------------------------------------

			// The current time chunk can't be beyond the stopping point for the full period
			if (chunkEnd.getDate() > dtStoppingPeriod.getDate()) {
				// if here this is the last time chunk we are doing
				chunkEnd.setDate(dtStoppingPeriod.getDate());
				chunkEnd.setTime(dtStoppingPeriod.getTime());
				run = 0;
			}

			logDebugMessage("Processing chunk starting[" + Str.dtToString(chunkStart) + "] stopping[" + Str.dtToString(chunkEnd) + "]");

			// ------------------------------------------------------

			tTimeframesChunk = Table.tableNew("timeframes");

			printPowerFnParams(iIncrementType, chunkStart, chunkEnd, iDateSequence, iTimeZone, tQueryCriteria);
			tArrayPositionsChunk = Power.generateVolumetricPositions(iQueryId, chunkStart, chunkEnd, iTimeZone, iIncrementType, iDateSequence,
					iValuationProduct, 0, tQueryCriteria, iDateFormat, iDateLocale, iTimeFormat, 1, tTimeframesChunk, 1);
			logDebugMessage("USR_APM_PowerPosition ... Power.generateVolumetricPositions complete");
			if(Table.isTableValid(tArrayPositionsChunk)==0 || Table.isTableValid(tTimeframesChunk)==0)
			{
				logDebugMessage("USR_APM_PowerPosition ... Error: No data returned by PWR_GenerateVolumetricPositions");
				errorMessage = argt.getString("error_msg", 1);
				argt.setString("error_msg", 1, errorMessage + ". No data returned by PWR_GenerateVolumetricPositions");
				Util.exitFail();
			}
			// APM metadata calls valuation_product pwr_product_id ...
			tArrayPositionsChunk.setColName(tArrayPositionsChunk.getColNum("valuation_product"), "pwr_product_id");

			// take the positions in array format and generate a table that is expanded (i.e. non array'd)
			tPositionsChunk = expandPwrTable(tArrayPositionsChunk, iCollapseDates, iIncrementType);
			if (Table.isTableValid(tPositionsChunk) == 0) {
				logDebugMessage("USR_APM_PowerPosition ... No data returned by the Power Position fn");
				errorMessage = argt.getString("error_msg", 1);
				argt.setString("error_msg", 1, errorMessage + ". No data returned by the Power Position fn");
				Util.exitFail();
			}

			// Combined power position results generated for the current chunk with the data created for the previous chunks.
			if (first_chunk != 0) {
				tTimeframes = tTimeframesChunk;
				tPositions = tPositionsChunk;
				first_chunk = 0;
			} else {
				tPositionsChunk.copyRowAddAll(tPositions);
				tTimeframesChunk.copyRowAddAll(tTimeframes);

				tTimeframesChunk.destroy();
				tPositionsChunk.destroy();
			}

			// the start of the next chunk is the end of the current
			temp = chunkStart;
			chunkStart = chunkEnd;

			chunkEnd = temp; // will be set at the begining of the loop

		} while (run == 1);

		chunkEnd.destroy();
		chunkStart.destroy();

		// Delete invalid timeframes rows
		// Power.generateVolumetricPositions sometimes returns the 0 duration timeframe ( e.g start and stop points are the same )
		// we must remove such rows as they screwed up the join below
		{
			int row;
			int col = tTimeframes.getColNum("total_hours");
			if (col <= 0) {
				logDebugMessage("Timeframes result table missing 'total_hours' column");
				argt.setString("error_msg", 1, "Timeframes result table missing 'total_hours' column");
				Util.exitFail();
			}

			for (row = tTimeframes.getNumRows(); row > 0; row--) {
				double hours = tTimeframes.getDouble(col, row);
				if (hours == 0) {
					tTimeframes.delRow(row);
				}
			}
		}

		// DEBUG MSG
		logDebugMessage("USR_APM_PowerPosition started post-processing");

		tPositions.mathMultCol("volumetric_energy", "price", "cost");
		tPositions.mathMultCol("volumetric_energy", "bav_flag", "bav_volume");
		tPositions.mathMultCol("cost", "bav_flag", "bav_cost");

		// Now sort out dates ...
		// For end time, we want to change 32767:0 to 32766:86400 for better bucketing on client
		// If we are collapsing dates, we are replacing the time period of delivery with its overall range's start and end points.
		// As such, we manipulate the dates in tTimeFrames and select across once done. 
		// For 'natural' granularity we simply manipulate the dates on the data itself

		if (iCollapseDates != 0) {
			// Add start and end date columns for APM filtering and bucketing purposes
			tTimeframes.addCol("startdate", COL_TYPE_ENUM.COL_INT);
			tTimeframes.addCol("enddate", COL_TYPE_ENUM.COL_INT);
			tTimeframes.addCol("start_time", COL_TYPE_ENUM.COL_INT);
			tTimeframes.addCol("end_time", COL_TYPE_ENUM.COL_INT);

			tDateManipulationTable = tTimeframes;
		} else {
			tDateManipulationTable = tPositions;
		}

		iLocStartingPeriodCol = tDateManipulationTable.getColNum("loc_starting_period");
		iLocStoppingPeriodCol = tDateManipulationTable.getColNum("loc_stopping_period");
		iStartDateCol = tDateManipulationTable.getColNum("startdate");
		iEndDateCol = tDateManipulationTable.getColNum("enddate");
		iStartTimeCol = tDateManipulationTable.getColNum("start_time");
		iEndTimeCol = tDateManipulationTable.getColNum("end_time");

		bMsgLogged = 0;
		for (iRow = 1; iRow <= tDateManipulationTable.getNumRows(); iRow++) {

			if (iCollapseDates != 0) {
				dtLocStartingPeriod = tDateManipulationTable.getDateTime(iLocStartingPeriodCol, iRow);
				tDateManipulationTable.setInt(iStartDateCol, iRow, dtLocStartingPeriod.getDate());
				tDateManipulationTable.setInt(iStartTimeCol, iRow, dtLocStartingPeriod.getTime());

				// For end time, we want to change 32767:0 to 32766:86400 for better bucketing on client
				dtLocStoppingPeriod = tDateManipulationTable.getDateTime(iLocStoppingPeriodCol, iRow);
				iEndDate = dtLocStoppingPeriod.getDate();
				iEndTime = dtLocStoppingPeriod.getTime();
			} else {
				iEndDate = tDateManipulationTable.getInt(iEndDateCol, iRow);
				iEndTime = tDateManipulationTable.getInt(iEndTimeCol, iRow);
			}

			/*
			 * Note that the datetime representing midnight can be stored as either "DATE=X, TIME=0" or "DATE=X-1, TIME=86400". Normally, the output
			 * for start of period will be in the former format, whilst the output for the end of period will be in the latter format, which is what
			 * APM client expects. However, in case the end point is returned as "DATE=X, TIME=0", change its format appropriately.
			 */
			if (iEndTime == 0) {
				iEndDate--;
				iEndTime = 86400;
				if (bMsgLogged == 0) {
					logDebugMessage("USR_APM_PowerPosition : Info : Fixed up zero end time");
					bMsgLogged = 1;
				}
			}

			tDateManipulationTable.setInt(iEndDateCol, iRow, iEndDate);
			tDateManipulationTable.setInt(iEndTimeCol, iRow, iEndTime);
		}

		if (iCollapseDates != 0)
			tPositions.select(tTimeframes, "startdate, enddate, start_time, end_time", "offset EQ $position_range_int");

		tTimeframes.destroy();

		// set the bucket key to zero if we want to bucket by date rather than ID
		if (iIncrementType == -1)
			tPositions.setColValString("position_range", "0");

		if (tPositions.getNumRows() > 0)
			returnt.tuneGrowth(tPositions.getNumRows());

		returnt.select(tPositions, "*", "tran_num GT 0");

		tPositions.destroy();

		// DEBUG MSG
		logDebugMessage("USR_APM_PowerPosition GetMonthlyResults finished post-processing");

		return;
	}

	// return the number of hours according to the timeframes table for a valuation productr/price band
	private double getHoursForValProductAndPriceBand(Table tTimeFrames, int iPosRange, int valuationProduct, int priceBand) throws OException {
		int timeFramesRow, timeFramesRowEnd, productRow, priceBandRow, i;
		Table tProductDetails, tPriceBands;

		timeFramesRow = tTimeFrames.findInt("offset", iPosRange, SEARCH_ENUM.FIRST_IN_GROUP);
		timeFramesRowEnd = tTimeFrames.findInt("offset", iPosRange, SEARCH_ENUM.LAST_IN_GROUP);

		if (timeFramesRow > 0) {
			for (i = timeFramesRow; i <= timeFramesRowEnd; i++) {
				tProductDetails = tTimeFrames.getTable("valuation_product", i);
				productRow = tProductDetails.unsortedFindInt("valuation_product", valuationProduct);
				if (productRow > 0) {
					tPriceBands = tProductDetails.getTable("price_band_table", productRow);
					priceBandRow = tPriceBands.unsortedFindInt("price_band", priceBand);
					if (priceBandRow > 0)
						return tPriceBands.getDouble("total_hours", priceBandRow);
				}
			}
		}

		return -1;
	}

	// ----------------------------------------------------------------------------------------
	// decides whether the previous row represents the same shape schedule but
	// for a different (contiguous) period
	// ----------------------------------------------------------------------------------------
	private int compareNaturalGranularityRows(Table table, int row, int previousRow, Table tTimeFrames, int iStartDateCol, int iEndDateCol,
			int iStartTimeCol, int iEndTimeCol, int iPosRangeCol, int iPosRangeColInt, int valuationProductCol, int priceBandCol,
			int iVolumetricEnergyCol, int iBAVCol, int iBAVCostCol, int iCostCol) throws OException {
		int col;
		int num_cols;
		int iCurrTimeSpan, iPreviousTimeSpan, tmpStartTime, tmpEndTime;
		double dPreviousAverage, dCurrAverage;
		int iPosRange, valuationProduct, priceBand;
		double prevTotalHours, totalHours;

		// Returns 1 if they match
		num_cols = table.getNumCols();
		for (col = 1; col <= num_cols; col++) {
			if (col == iPosRangeCol) {
				// if position_range != "0" then we're on a funky day (not natural granularity) and do NOT rollup
				if (Str.equal(table.getString(iPosRangeCol, row), "0") == 0 || Str.equal(table.getString(iPosRangeCol, previousRow), "0") == 0){
					return 0;
				}
			} else if (col == iStartDateCol) {
				// could be current row is 46212 and 0 (start today) versus previous row is 46211 and 86400 (end previous day)
				// in this instance we are ok as its still contiguous
				if (table.getInt(iStartTimeCol, row) == 0 && table.getInt(iEndTimeCol, previousRow) == 86400
						&& table.getInt(iStartDateCol, row) == (table.getInt(iEndDateCol, previousRow) + 1)){
					continue;
				}
				// start date has to be the same as the previous rows end date
				if (table.getInt(iStartDateCol, row) != table.getInt(iEndDateCol, previousRow)){
					return 0;
				}
			} else if (col == iStartTimeCol) {
				// could be current row is 46212 and 0 (start today) versus previous row is 46211 and 86400 (end previous day)
				// in this instance we are ok as its still contiguous
				if (table.getInt(iStartTimeCol, row) == 0 && table.getInt(iEndTimeCol, previousRow) == 86400
						&& table.getInt(iStartDateCol, row) == (table.getInt(iEndDateCol, previousRow) + 1))
					continue;

				// start time has to be the same as the previous rows end time
				if (table.getInt(iStartTimeCol, row) != table.getInt(iEndTimeCol, previousRow))
					return 0;
			} else if (col == iEndDateCol || col == iEndTimeCol || col == iPosRangeColInt) {
				// we don't want to compare these ones as the prior end datetime and curr startdatetime are the important ones
				// the pos range int col will be different as it seems to be a daily one - so skip that too....
			} else if (col == iVolumetricEnergyCol || col == iBAVCol || col == iBAVCostCol || col == iCostCol) {
				// we need to check the average over the time period matches, not the absolute value
				// if we do the absolute value the gradient of the delivery may change and therefore it is wrong to rollup
				// the lowest granularity Endur supports is 5 minutes - 
				// therefore compare on this basis - will help us to dodge tiny numbers going beyond precision
				tmpStartTime = (table.getInt(iStartDateCol, row) * 86400) + table.getInt(iStartTimeCol, row);
				tmpEndTime = (table.getInt(iEndDateCol, row) * 86400) + table.getInt(iEndTimeCol, row);
				iCurrTimeSpan = tmpEndTime - tmpStartTime;
				tmpStartTime = (table.getInt(iStartDateCol, previousRow) * 86400) + table.getInt(iStartTimeCol, previousRow);
				tmpEndTime = (table.getInt(iEndDateCol, previousRow) * 86400) + table.getInt(iEndTimeCol, previousRow);
				iPreviousTimeSpan = tmpEndTime - tmpStartTime;
				if (iCurrTimeSpan > 0) // should never be zero - but anyway check
					dCurrAverage = table.getDouble(col, row) / (iCurrTimeSpan / 5);
				else
					return 0;
				if (iPreviousTimeSpan > 0) // should never be zero - but anyway check
					dPreviousAverage = table.getDouble(col, previousRow) / (iPreviousTimeSpan / 5);
				else
					return 0;

				if (dPreviousAverage != dCurrAverage)
					return 0;
			} else {
				// Make sure all integer cols match as they will be ID's (e.g. price band)
				// also make sure that the data values (doubles) are the same (otherwise shape will be different)
				COL_TYPE_ENUM colType = COL_TYPE_ENUM.fromInt(table.getColType(col));
				switch (colType) {
				case COL_INT:
					if (table.getInt(col, row) != table.getInt(col, previousRow)){
						return 0;
					}
					break;
				case COL_DOUBLE:
					if (table.getDouble(col, row) != table.getDouble(col, previousRow)){
						return 0;
					}
					break;
				default:
					break;
				}
			}
		}

		// finally cross reference the valuation product to the timeframes table using the offset
		// make sure the number of hours reported by the price band table for the valuation product and for the
		// price band match for the previous_row and current_row - if they do not you can't roll up
		// - the schedule is different across the days (i.e. end of week flowing into weekend)
		// if they are the same then we are happy
		iPosRange = table.getInt(iPosRangeColInt, row);
		valuationProduct = table.getInt(valuationProductCol, row);
		priceBand = table.getInt(priceBandCol, row);
		totalHours = getHoursForValProductAndPriceBand(tTimeFrames, iPosRange, valuationProduct, priceBand);
		if (totalHours > -1) {
			iPosRange = table.getInt(iPosRangeColInt, previousRow);
			valuationProduct = table.getInt(valuationProductCol, previousRow);
			priceBand = table.getInt(priceBandCol, previousRow);
			prevTotalHours = getHoursForValProductAndPriceBand(tTimeFrames, iPosRange, valuationProduct, priceBand);
			if (prevTotalHours > -1 && prevTotalHours != totalHours){
				return 0;
			}
		} else {
			return 0;
		}

		return 1;
	}

	// ----------------------------------------------------------------------------------------
	// rolls up data that is contiguous by date/time
	// ----------------------------------------------------------------------------------------
	private int rollupContiguousData(Table tPositions, Table tAllTimeFrames) throws OException {
		int bRollUp, iRow, iCol;
		int iStartDateCol, iEndDateCol, iStartTimeCol, iEndTimeCol, iPosRangeCol, iPosRangeColInt;
		int iVolumetricEnergyCol, iBAVCol, iCostCol, iBAVCostCol, iValuationProductCol, iPriceBandCol;
		double dPrevVolumetricEnergy, dPrevBAV, dPrevCost, dPrevBAVCost;
		double dRunningVolEnergyTotal, dRunningBAVTotal, dRunningCostTotal, dRunningBAVCostTotal;
		int iEndDate, iEndTime, numRows;
		Table tTimeFrames;

		if (iRollupDailyAndLowerGranularity == 0)
			return 1;

		numRows = tPositions.getNumRows();
		if (numRows < 1)
			return 1;

		iPosRangeCol = tPositions.getColNum("position_range");
		iPosRangeColInt = tPositions.getColNum("position_range_int");
		iStartDateCol = tPositions.getColNum("startdate");
		iEndDateCol = tPositions.getColNum("enddate");
		iStartTimeCol = tPositions.getColNum("start_time");
		iEndTimeCol = tPositions.getColNum("end_time");
		iVolumetricEnergyCol = tPositions.getColNum("volumetric_energy");
		iBAVCol = tPositions.getColNum("bav_volume");
		iBAVCostCol = tPositions.getColNum("bav_cost");
		iCostCol = tPositions.getColNum("cost");
		iValuationProductCol = tPositions.getColNum("pwr_product_id");
		iPriceBandCol = tPositions.getColNum("price_band");

		if (iPosRangeCol < 1 || iStartDateCol < 1 || iEndDateCol < 1 || iStartTimeCol < 1 || iEndTimeCol < 1 || iVolumetricEnergyCol < 1
				|| iBAVCol < 1 || iPosRangeColInt < 1 || iBAVCostCol < 1 || iCostCol < 1 || iValuationProductCol < 1 || iPriceBandCol < 1) {
			logDebugMessage("Missing columns in position table for RollupContiguousData");
			return 0;
		}

		// this should always be populated
		tTimeFrames = tAllTimeFrames.getTable(1, 1);
		if (Table.isTableValid(tTimeFrames) == 0) {
			logDebugMessage("No valid timeframes table for RollupContiguousData");
			return 0;
		}

		logDebugMessage("USR_APM_PowerPosition starting natural granularity rollup");

		// reset the group by so the data will be contiguous
		// we want the sort to be all the ID cols (int's) and then the date cols
		tPositions.clearGroupBy();
		for (iCol = 1; iCol <= tPositions.getNumCols(); iCol++) {
			if (iStartDateCol == iCol || iEndDateCol == iCol || iStartTimeCol == iCol || iEndTimeCol == iCol)
				continue;

			if (tPositions.getColType(iCol) == COL_TYPE_ENUM.COL_INT.toInt())
				tPositions.addGroupBy(iCol);
		}
		tPositions.addGroupBy(iStartDateCol);
		tPositions.addGroupBy(iStartTimeCol);
		tPositions.addGroupBy(iEndDateCol);
		tPositions.addGroupBy(iEndTimeCol);
		tPositions.groupBy();

		// now rollup
		bRollUp = 0;
		dRunningVolEnergyTotal = tPositions.getDouble(iVolumetricEnergyCol, numRows);
		dRunningBAVTotal = tPositions.getDouble(iBAVCol, numRows);
		dRunningBAVCostTotal = tPositions.getDouble(iBAVCostCol, numRows);
		dRunningCostTotal = tPositions.getDouble(iCostCol, numRows);
		for (iRow = numRows; iRow > 1; iRow--) {
			bRollUp = compareNaturalGranularityRows(tPositions, iRow, iRow - 1, tTimeFrames, iStartDateCol, iEndDateCol, iStartTimeCol, iEndTimeCol,
					iPosRangeCol, iPosRangeColInt, iValuationProductCol, iPriceBandCol, iVolumetricEnergyCol, iBAVCol, iBAVCostCol, iCostCol);
			if (bRollUp != 0) {
				// set the volume (should actually just be twice the amount) - but lets be good
				dPrevVolumetricEnergy = tPositions.getDouble(iVolumetricEnergyCol, iRow - 1);
				dPrevBAV = tPositions.getDouble(iBAVCol, iRow - 1);
				dPrevBAVCost = tPositions.getDouble(iBAVCostCol, iRow - 1);
				dPrevCost = tPositions.getDouble(iCostCol, iRow - 1);

				dRunningVolEnergyTotal += dPrevVolumetricEnergy;
				dRunningBAVTotal += dPrevBAV;
				dRunningBAVCostTotal += dPrevBAVCost;
				dRunningCostTotal += dPrevCost;

				// previous row is before current row timewise but contiguous so....
				// take start date and start time from previous row - i.e. do nothing
				// take end date and end time from current row (as it is later)
				iEndDate = tPositions.getInt(iEndDateCol, iRow);
				iEndTime = tPositions.getInt(iEndTimeCol, iRow);
				tPositions.setInt(iEndDateCol, iRow - 1, iEndDate);
				tPositions.setInt(iEndTimeCol, iRow - 1, iEndTime);

				// set the data values too as otherwise the rollup will get the values wrong
				// and we won't get an optimal rollup
				tPositions.setDouble(iVolumetricEnergyCol, iRow - 1, dRunningVolEnergyTotal);
				tPositions.setDouble(iBAVCol, iRow - 1, dRunningBAVTotal);
				tPositions.setDouble(iBAVCostCol, iRow - 1, dRunningBAVCostTotal);
				tPositions.setDouble(iCostCol, iRow - 1, dRunningCostTotal);

				tPositions.delRow(iRow);
			} else {
				tPositions.setDouble(iVolumetricEnergyCol, iRow, dRunningVolEnergyTotal);
				dRunningVolEnergyTotal = tPositions.getDouble(iVolumetricEnergyCol, iRow - 1);

				tPositions.setDouble(iBAVCol, iRow, dRunningBAVTotal);
				dRunningBAVTotal = tPositions.getDouble(iBAVCol, iRow - 1);

				tPositions.setDouble(iBAVCostCol, iRow, dRunningBAVCostTotal);
				dRunningBAVCostTotal = tPositions.getDouble(iBAVCostCol, iRow - 1);

				tPositions.setDouble(iCostCol, iRow, dRunningCostTotal);
				dRunningCostTotal = tPositions.getDouble(iCostCol, iRow - 1);
			}

		}

		if (bRollUp == 1) {
			// need to update the first row as we have exited loop without
			// updating
			tPositions.setDouble(iVolumetricEnergyCol, 1, dRunningVolEnergyTotal);
			tPositions.setDouble(iBAVCol, 1, dRunningBAVTotal);
			tPositions.setDouble(iBAVCostCol, 1, dRunningBAVCostTotal);
			tPositions.setDouble(iCostCol, 1, dRunningCostTotal);
		}

		logDebugMessage("USR_APM_PowerPosition rolled up natural granularity data from " + numRows + " to " + tPositions.getNumRows() + " rows");

		return 1;
	}

	// ----------------------------------------------------------------------------------------
	// returns the results for a day and gets rid of the bucketKey if we want to bucket by date
	// ----------------------------------------------------------------------------------------
	private int getResultsForDay(int iQueryId, ODateTime dtStartingPeriod, ODateTime dtStoppingPeriod, int iTimeZone, int iIncrementType,
			int iDateSequence, int iValuationProduct, Table tQueryCriteria, int iDateFormat, int iDateLocale, int iTimeFormat, int iCollapseDates,
			Table tTimeFrames) throws OException {
		Table tPositions;

		if (tTimeFrames.getNumCols() < 1) {
			tTimeFrames.addCol("timeframes", COL_TYPE_ENUM.COL_TABLE);
			tTimeFrames.addRow();
		}
		tPositions = getResults(iQueryId, dtStartingPeriod, dtStoppingPeriod, iTimeZone, iIncrementType, iDateSequence, iValuationProduct,
				tQueryCriteria, iDateFormat, iDateLocale, iTimeFormat, iCollapseDates, tTimeFrames);

		// set the bucket key to zero if we want to bucket by date rather than ID (all days apart from funky days)
		if (iIncrementType == -1)
			tPositions.setColValString("position_range", "0");

		// rollup contiguous rows IF we are at natural granularity
		if (iIncrementType == -1) {
			if (rollupContiguousData(tPositions, tTimeFrames) == 0)
				return 0;
		}

		if (tPositions.getNumRows() > 0)
			returnt.tuneGrowth(tPositions.getNumRows());

		returnt.select(tPositions, "*", "tran_num GT 0");

		tPositions.destroy();

		return 1;
	}

	// ----------------------------------------------------------------------------------------
	// returns the results for a day and gets rid of the bucketKey if we want to bucket by date
	// ----------------------------------------------------------------------------------------
	private void getResultsDaily(int iQueryId, ODateTime dtStartingPeriod, ODateTime dtStoppingPeriod, int iTimeZone, int iIncrementType,
			int iDateSequence, int iValuationProduct, Table tQueryCriteria, int iDateFormat, int iDateLocale, int iTimeFormat, int iCollapseDates)
			throws OException {
		Table tTimeFrames = Table.tableNew();
		getResultsForDay(iQueryId, dtStartingPeriod, dtStoppingPeriod, iTimeZone, iIncrementType, iDateSequence, iValuationProduct, tQueryCriteria,
				iDateFormat, iDateLocale, iTimeFormat, iCollapseDates, tTimeFrames);
		tTimeFrames.destroy();
	}

	// ------------------------------------------------------------------------------------------------------------------------------
	// returns the results for sub daily granularity. Some data is bucket by ID, others by date (depending on presence of funky day)
	// ------------------------------------------------------------------------------------------------------------------------------
	private int getResultsSubDaily(int iQueryId, int iStartDate, int iStopDate, int iTimeZone, int iSubDailyIncrementType, int iDateSequence,
			int iValuationProduct, Table tQueryCriteria, int iDateFormat, int iDateLocale, int iTimeFormat, int iCollapseDates) throws OException {
		int iIncrementType, currDate, retVal;
		ODateTime dtStartingPeriod, dtStoppingPeriod;
		Table tTimeFrames = Table.tableNew();

		iIncrementType = iSubDailyIncrementType;

		dtStartingPeriod = ODateTime.dtNew();
		dtStoppingPeriod = ODateTime.dtNew();

		for (currDate = iStartDate; currDate < iStopDate; currDate++) {
			dtStartingPeriod.setDateTime(currDate, 0);
			dtStoppingPeriod.setDateTime(currDate, 86400);

			if (OCalendar.dateIsDaylightSavings(currDate, iTimeZone) == 1) {
				iIncrementType = iSubDailyIncrementType; // funky date involved so we get data at specified granularity
				logDebugMessage("USR_APM_PowerPosition started processing at specified granularity for funky date: " + OCalendar.formatJd(currDate));
			} else {
				iIncrementType = -1; // no funky date involved so we can get data at natural granularity
				logDebugMessage("USR_APM_PowerPosition started processing at natural granularity for day: " + OCalendar.formatJd(currDate));
			}
			getResultsForDay(iQueryId, dtStartingPeriod, dtStoppingPeriod, iTimeZone, iIncrementType, iDateSequence, iValuationProduct,
					tQueryCriteria, iDateFormat, iDateLocale, iTimeFormat, 0, tTimeFrames);
		}

		dtStartingPeriod.destroy();
		dtStoppingPeriod.destroy();

		// Do a final rollup of contiguous rows (goes across days)
		retVal = rollupContiguousData(returnt, tTimeFrames);

		tTimeFrames.destroy();

		return retVal;
	}

	private Table getResConfigTable(Table tListResultTypes) throws OException {

		int iResultRow = tListResultTypes.unsortedFindString("res_attr_grp_name", "APM Power Position", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
		int iResultID = tListResultTypes.getInt("res_attr_grp_id", iResultRow);

		Table tResConfig = SimResult.getResultConfig(iResultID);
		tResConfig.sortCol("res_attr_name");

		return tResConfig;
	}

	private Table prepareResultParamTable() throws OException {
		int retval = DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
		String sCachedTableName = "Pfolio Result Attrs";
		Table tListResultTypes = Table.getCachedTable(sCachedTableName);
		if (Table.isTableValid(tListResultTypes) == 0) {
			tListResultTypes = Table.tableNew();
			retval = DBase.runSql("select * from pfolio_result_attr_groups");
			if (retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
				OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(retval, "DBase.runSql() failed"));
			} else {
				retval = DBase.createTableOfQueryResults(tListResultTypes);
				if (retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
					OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(retval, "DBase.createTableOfQueryResults() failed"));
				} else {
					Table.cacheTable(sCachedTableName, tListResultTypes);
				}
			}
		}

		if (retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
			logDebugMessage("Problem when retrieving result attribute groups");
			argt.setString("error_msg", 1, "Problem when retrieving result attribute groups");
			Util.exitFail();
		}

		return tListResultTypes;
	}

	private Table prepareQueryCriteriaTable() throws OException {
		Table tQueryCriteria = Table.tableNew("Query Criteria Table");
		tQueryCriteria.addCol("query_type", COL_TYPE_ENUM.COL_INT);
		tQueryCriteria.addCol("query_what_str", COL_TYPE_ENUM.COL_STRING);
		tQueryCriteria.addCol("query_from_str", COL_TYPE_ENUM.COL_STRING);
		tQueryCriteria.addCol("query_where_str", COL_TYPE_ENUM.COL_STRING);

		rowNumInQueryCriteria = tQueryCriteria.addRow();
		tQueryCriteria.setInt(1 /* query_type */, rowNumInQueryCriteria, 0);

		String what_string = setQueryCriteriaWhatString();
		tQueryCriteria.setString(2 /* query_what_str */, rowNumInQueryCriteria, "," + what_string);// need a comma here

		return tQueryCriteria;
	}

	public String setQueryCriteriaWhatString() {
		return "(param_reset_header.proj_index) startdate, (param_reset_header.proj_index) enddate, "
				+ "(param_reset_header.proj_index) start_time, (param_reset_header.proj_index) end_time, "
				+ "(param_reset_header.rate_multiplier) cost, (param_reset_header.rate_multiplier) bav_volume, "
				+ "(param_reset_header.rate_multiplier) bav_cost, "
				+ "(ptad.phys_tran_type) power_initial_term, (ptad.service_type) power_service_type, "
				+ "(ptad.choice) power_choice, (ptad.product) power_product, "
				+ "(ptad.pt_of_receipt_loc) power_rec_loc, (ptad.pt_of_delivery_loc) power_del_loc";
	}

	private int getTimeZone(Table resConfigTable) throws OException {
		int timeZone = 1000; /* GMT */
		if (paramHasValue(resConfigTable, "APM Power Timezone") != 0) {
			timeZone = Ref.getValue(SHM_USR_TABLES_ENUM.TIME_ZONE_TABLE, Str.toUpper(getParamStrValue(resConfigTable, "APM Power Timezone")));
		}
		return timeZone;
	}

	private void showAllVolumeOrBav(Table resConfigTable, Table tQueryCriteria) throws OException {
		if ((paramHasValue(resConfigTable, "APM Power Show All Volume Statuses")) == 0
				|| (getParamIntValue(resConfigTable, "APM Power Show All Volume Statuses") == 0)) {
			rowNumInQueryCriteria = tQueryCriteria.addRow();
			tQueryCriteria.setInt(1 /* query_type */, rowNumInQueryCriteria, 1);

			// version V91R1 or later(9.1.x) using L-Shaped table;
			tQueryCriteria.setString(4 /* query_where_str */, rowNumInQueryCriteria, "and tsd.bav_flag = 1");

			tsdQuerySet = rowNumInQueryCriteria;
		}
	}

	private void excludeKnownOptions(Table tQueryCriteria) throws OException {
		if (iExcludeKnownOptionTypes == 1) {
			String whereCond = " and tsd.status not in ( " + VOLUME_TYPE.VOLUME_TYPE_EXERCISED.toInt() + ", "
					+ VOLUME_TYPE.VOLUME_TYPE_EXPIRED.toInt() + ") ";
			if (tsdQuerySet == -1) {
				rowNumInQueryCriteria = tQueryCriteria.addRow();
				tQueryCriteria.setInt(1 /* query_type */, rowNumInQueryCriteria, 1);
			} else {
				String where2Cond = tQueryCriteria.getString(4 /* query_where_str */, rowNumInQueryCriteria);
				whereCond = whereCond + where2Cond;
			}
			tQueryCriteria.setString(4 /* query_where_str */, rowNumInQueryCriteria, whereCond);
		}
	}

	private int setQueryID(Table sim_def) throws OException {
		int iQueryId = 0;

		// If query ID is provided as a parameter, use it!
		if (sim_def.getColNum("APM Single Deal Query") > 0)
			iQueryId = sim_def.getInt("APM Single Deal Query", 1);

		// If query ID was not set or left at zero, create a query ID from the list of transactions
		if (iQueryId == 0) {
			Transaction tTransaction;
			// tTrans = USR_Transactions();
			int iResultType = argt.getInt("result_type", 1);
			Table tAllTrans = argt.getTable("transactions", 1);
			Table tTrans = tAllTrans.cloneTable();

			// Loop on all transactions
			for (int iLoop = tAllTrans.getNumRows(); iLoop > 0; iLoop--) {
				tTransaction = tAllTrans.getTran("tran_ptr", iLoop);

				// Keep only valid ones
				if (SimResult.isResultAllowedForTran(iResultType, tTransaction) != 0) {
					tAllTrans.copyRowAdd(iLoop, tTrans);
				}
			}

			if (Table.isTableValid(tTrans) == 0 || tTrans.getNumRows() == 0) {
				logDebugMessage("No eligible transactions passed to APM Power sim result\n");
				return NO_ELIGIBLE_TRANSACTION;
			}

			iQueryId = Query.tableQueryInsert(tTrans, tTrans.getColNum("tran_num"));
			if (iQueryId <= 0) {
				logDebugMessage("Failed to create query Id in APM Power sim result\n");
				argt.setString("error_msg", 1, "Failed to create query Id in APM Power Position sim result\n");
				Util.exitFail();
			}

			if (Table.isTableValid(tTrans) == 1) {
				tTrans.destroy();
			}
		}

		return iQueryId;

	}

	private String getMonthlyDateSequence(Table resConfigTable) throws OException {
		// Use calendar months, or a user-defined monthly date sequence?
		String sMonthlyDateSequence;
		if (paramHasValue(resConfigTable, "APM Power Month Date Sequence") > 0) {
			sMonthlyDateSequence = getParamStrValue(resConfigTable, "APM Power Month Date Sequence");
			iMonthlyDateSequence = Ref.getValue(SHM_USR_TABLES_ENUM.DATE_SEQUENCE_TABLE, sMonthlyDateSequence);
			if (iMonthlyDateSequence >= 0) {
				// PWR_Generate... function will subtract 1000 from the custom date sequence ID passed in
				iMonthlyDateSequence += 1000;
			} else {
				sMonthlyDateSequence = "";
				iMonthlyDateSequence = 2;
				logDebugMessage("Unrecognized date sequence parameter: " + getParamStrValue(resConfigTable, "APM Power Month Date Sequence")
						+ ". Defaulting to calendar months.");
			}
		} else {
			sMonthlyDateSequence = "";
			iMonthlyDateSequence = 2;
		}

		return sMonthlyDateSequence;
	}

	private int getSubHourlyGranularity(Table resConfigTable) throws OException {
		if (paramHasValue(resConfigTable, "APM Power Sub-Hourly Granularity (mins)") > 0) {
			return getParamIntValue(resConfigTable, "APM Power Sub-Hourly Granularity (mins)");
		} else {
			return DEFAULT_SUB_HOURLY_GRANULARITY;
		}
	}

	private int setSubHourlyIncType(int iSubHourlyGranularity) throws OException {
		int iSubHourlyIncrementType = PWR_INCREMENT_TYPE_ENUM.INCREMENT_TYPE_15_MINUTE.toInt();

		switch (iSubHourlyGranularity) {
		case 5:
			iSubHourlyIncrementType = PWR_INCREMENT_TYPE_ENUM.INCREMENT_TYPE_05_MINUTE.toInt();
			break;
		case 10:
			iSubHourlyIncrementType = PWR_INCREMENT_TYPE_ENUM.INCREMENT_TYPE_10_MINUTE.toInt();
			break;
		case 15:
			iSubHourlyIncrementType = PWR_INCREMENT_TYPE_ENUM.INCREMENT_TYPE_15_MINUTE.toInt();
			break;
		case 20:
			iSubHourlyIncrementType = PWR_INCREMENT_TYPE_ENUM.INCREMENT_TYPE_20_MINUTE.toInt();
			break;
		case 30:
			iSubHourlyIncrementType = PWR_INCREMENT_TYPE_ENUM.INCREMENT_TYPE_30_MINUTE.toInt();
			break;
		case 60:
			logDebugMessage("Sub Hourly granularity of 60 minutes is not supported (pick 30, 20, 15, 10 or 5)\n");
			Util.exitFail();
		default:
			logDebugMessage("Specified Sub-Hourly granularity is not supported (pick 30, 20, 15, 10 or 5)\n");
			Util.exitFail();
		}
		return iSubHourlyIncrementType;
	}

	private int getHistSubHourlyStart(Table resConfigTable) throws OException {
		int iHistSubHourlyStart = 0;
		if (paramHasValue(resConfigTable, "APM Power Historical Sub-Hourly Start") > 0) {
			iHistSubHourlyStart = getParamDateValue(resConfigTable, "APM Power Historical Sub-Hourly Start");
		}
		return iHistSubHourlyStart;
	}

	private int getHistHourlyStart(Table resConfigTable) throws OException {
		int iHistHourlyStart = 0;
		if (paramHasValue(resConfigTable, "APM Power Historical Hourly Start") > 0) {
			iHistHourlyStart = getParamDateValue(resConfigTable, "APM Power Historical Hourly Start");
		}
		return iHistHourlyStart;
	}

	private int getHistDailyStart(Table resConfigTable) throws OException {
		int iHistDailyStart = 0;
		if (paramHasValue(resConfigTable, "APM Power Historical Daily Start") > 0) {
			iHistDailyStart = getParamDateValue(resConfigTable, "APM Power Historical Daily Start");
		}
		return iHistDailyStart;
	}

	private int getHistMonthlyStart(Table resConfigTable) throws OException {
		int iHistMonthlyStart = 0;
		if (paramHasValue(resConfigTable, "APM Power Historical Monthly Start") > 0) {
			iHistMonthlyStart = getParamDateValue(resConfigTable, "APM Power Historical Monthly Start");
		}
		return iHistMonthlyStart;
	}

	// process at monthly granularity
	private void processHistMonthly(int iTimeZone, int iQueryId, Table tQueryCriteria, String sMonthlyDateSequence, int iValuationProduct,
			int iDateFormat, int iDateLocale, int iTimeFormat) throws OException {
		// Process daily from the start of the monthly period to the first day of the following month.
		int iStartDate = 0, iStopDate, iAlignedDate;
		int iDateSequence;
		int iIncrementType;
		ODateTime dtStartingPeriod, dtStoppingPeriod;

		dtStartingPeriod = ODateTime.dtNew();
		dtStoppingPeriod = ODateTime.dtNew();

		if (iHistMonthlyStart > 0) {
			iStartDate = iHistMonthlyStart;
			iStopDate = iHistDailyStart;
			if (iStopDate <= 0){
				iStopDate = iHistHourlyStart;
			}
			if (iStopDate <= 0){
				iStopDate = iHistSubHourlyStart;
			}
			if (iStopDate <= 0){
				iStopDate = iToday;
			}

			iAlignedDate = OCalendar.getEOM(iStartDate) + 1;

			if (iAlignedDate < iStopDate && iStartDate != OCalendar.getSOM(iStartDate)) {
				// We have some dates to fill where the start date isn't the first of the month and the stop date is at least later than the end of
				// the month.
				iStopDate = iAlignedDate;
				dtStartingPeriod.setDateTime(iStartDate, 0);
				dtStoppingPeriod.setDateTime((iStopDate - 1), 86400);
				iDateSequence = ROLL_CONV_ENUM.RC_CALENDAR_DAY.toInt();
				iIncrementType = -1;

				logDebugMessage("USR_APM_PowerPosition started processing daily granularity for monthly period between historical monthly start "
						+ "and the start of the following month in the period from " + OCalendar.formatJd(iStartDate) + " to "
						+ OCalendar.formatJd(iStopDate - 1));
				getResultsDaily(iQueryId, dtStartingPeriod, dtStoppingPeriod, iTimeZone, iIncrementType, iDateSequence, iValuationProduct,
						tQueryCriteria, iDateFormat, iDateLocale, iTimeFormat, 1);

				iStartDate = iStopDate;
			}
		}

		// Process whole months at monthly granularity
		if (iHistMonthlyStart > 0) {
			iStopDate = iHistDailyStart;
			if (iStopDate <= 0)
				iStopDate = iHistHourlyStart;
			if (iStopDate <= 0)
				iStopDate = iHistSubHourlyStart;
			if (iStopDate <= 0)
				iStopDate = iToday;

			// The aligned start date
			iAlignedDate = OCalendar.getSOM(iStartDate);

			// The monthly end date should be on a month boundary.
			iStopDate = OCalendar.getSOM(iStopDate);

			if (iStartDate == iAlignedDate && iAlignedDate != iStopDate) {
				// We should have at least one whole month to calculate, since the start date is the first of a month,
				// and the start and stop date are not in the same month.
				dtStartingPeriod.setDateTime(iStartDate, 0);
				dtStoppingPeriod.setDateTime((iStopDate - 1), 86400);
				iDateSequence = iMonthlyDateSequence;
				iIncrementType = -1;

				logDebugMessage("USR_APM_PowerPosition started processing historical data at monthly granularity");
				getResultsMonthly(iQueryId, dtStartingPeriod, dtStoppingPeriod, iTimeZone, iIncrementType, iDateSequence, iValuationProduct,
						tQueryCriteria, iDateFormat, iDateLocale, iTimeFormat, 1, sMonthlyDateSequence);

				iStartDate = iStopDate;
			}
		}

		if (iHistMonthlyStart > 0 && (iHistDailyStart > 0 || iHistHourlyStart > 0 || iHistSubHourlyStart > 0 || iStartDate < iToday)) {
			// Fill from monthly to daily/sub-daily granularity...
			iStopDate = iHistDailyStart;
			if (iStopDate <= 0){
				iStopDate = iHistHourlyStart;
			}
			if (iStopDate <= 0){
				iStopDate = iHistSubHourlyStart;
			}
			if (iStopDate <= 0){
				iStopDate = iToday;
			}

			if (iStartDate < iStopDate) {
				// Fill the rest of the period from the last whole month to the next valid period.
				dtStartingPeriod.setDateTime(iStartDate, 0);
				dtStoppingPeriod.setDateTime((iStopDate - 1), 86400);
				iDateSequence = ROLL_CONV_ENUM.RC_CALENDAR_DAY.toInt();
				iIncrementType = -1;

				logDebugMessage("USR_APM_PowerPosition started processing historical data monthly to daily granularity changeover from "
						+ OCalendar.formatJd(iStartDate) + " to " + OCalendar.formatJd(iStopDate - 1));
				getResultsDaily(iQueryId, dtStartingPeriod, dtStoppingPeriod, iTimeZone, iIncrementType, iDateSequence, iValuationProduct,
						tQueryCriteria, iDateFormat, iDateLocale, iTimeFormat, 1);
			}
		}

		dtStartingPeriod.destroy();
		dtStoppingPeriod.destroy();
	}

	// Process at daily granularity
	private void processHistDaily(int iTimeZone, int iQueryId, Table tQueryCriteria, int iValuationProduct, int iDateFormat, int iDateLocale,
			int iTimeFormat) throws OException {
		int iStartDate, iStopDate;
		int iDateSequence;
		int iIncrementType;

		ODateTime dtStartingPeriod = ODateTime.dtNew();
		ODateTime dtStoppingPeriod = ODateTime.dtNew();

		if (iHistDailyStart > 0) {
			iStartDate = iHistDailyStart;
			iStopDate = iHistHourlyStart;
			if (iStopDate <= 0)
				iStopDate = iHistSubHourlyStart;
			if (iStopDate <= 0)
				iStopDate = iToday;
			dtStartingPeriod.setDateTime(iStartDate, 0);
			dtStoppingPeriod.setDateTime((iStopDate - 1), 86400);
			iDateSequence = ROLL_CONV_ENUM.RC_CALENDAR_DAY.toInt();
			iIncrementType = -1;

			logDebugMessage("USR_APM_PowerPosition started processing at daily granularity from " + OCalendar.formatJd(iStartDate) + " to "
					+ OCalendar.formatJd(iStopDate - 1));
			getResultsDaily(iQueryId, dtStartingPeriod, dtStoppingPeriod, iTimeZone, iIncrementType, iDateSequence, iValuationProduct,
					tQueryCriteria, iDateFormat, iDateLocale, iTimeFormat, 1);
		}

		dtStartingPeriod.destroy();
		dtStoppingPeriod.destroy();
	}

	// Process at hourly granularity
	private void processHistHourly(int iTimeZone, int iQueryId, Table tQueryCriteria, int iValuationProduct, int iDateFormat, int iDateLocale,
			int iTimeFormat) throws OException {
		int iStartDate, iStopDate;
		int iDateSequence;
		int iIncrementType;

		if (iHistHourlyStart > 0) {
			iIncrementType = PWR_INCREMENT_TYPE_ENUM.INCREMENT_TYPE_60_MINUTE.toInt();
			iStartDate = iHistHourlyStart;
			iStopDate = iHistSubHourlyStart;
			if (iStopDate <= 0) {
				iStopDate = iToday;
			}
			// We can no longer set to monthly as PWR_GenerateVolumetricPositions requires monthly date sequence to start and stop on a month
			// boundary.
			iDateSequence = ROLL_CONV_ENUM.RC_CALENDAR_DAY.toInt();
			logDebugMessage("USR_APM_PowerPosition started processing at HOURLY granularity");
			getResultsSubDaily(iQueryId, iStartDate, iStopDate, iTimeZone, iIncrementType, iDateSequence, iValuationProduct, tQueryCriteria,
					iDateFormat, iDateLocale, iTimeFormat, 0);
		}
	}

	// Process at sub hourly granularity
	private void processHistSubHourly(int iTimeZone, int iQueryId, Table tQueryCriteria, int iValuationProduct, int iDateFormat, int iDateLocale,
			int iTimeFormat, int iSubHourlyIncrementType, int iSubHourlyGranularity) throws OException {
		int iStartDate, iStopDate;
		int iDateSequence;
		int iIncrementType;

		if (iHistSubHourlyStart > 0) {
			iIncrementType = iSubHourlyIncrementType;
			iStartDate = iHistSubHourlyStart;
			iStopDate = iToday;
			// We can no longer set to monthly as PWR_GenerateVolumetricPositions requires monthly date sequence to start and stop on a month
			// boundary.
			iDateSequence = ROLL_CONV_ENUM.RC_CALENDAR_DAY.toInt();
			logDebugMessage("USR_APM_PowerPosition started processing at " + iSubHourlyGranularity + " Math.Math.min granularity");
			getResultsSubDaily(iQueryId, iStartDate, iStopDate, iTimeZone, iIncrementType, iDateSequence, iValuationProduct, tQueryCriteria,
					iDateFormat, iDateLocale, iTimeFormat, 0);
		}

	}

	// Check the parameters for the various forward end-points. If no parameters have been set, use defaults; Otherwise, use the settings.
	private void getForwardEndPoints(Table resConfigTable) throws OException {
		if ((!(paramHasValue(resConfigTable, "APM Power Hourly Endpoint") > 0)) && (!(paramHasValue(resConfigTable, "APM Power Daily Endpoint") > 0))
				&& (!(paramHasValue(resConfigTable, "APM Power Sub-Hourly Endpoint") > 0))
				&& (!(paramHasValue(resConfigTable, "APM Power Monthly Endpoint") > 0))) {
			// If no parameters have been set, switch to defaults
			iSubHourlyEndPoint = 0;
			iHourlyEndPoint = OCalendar.parseString("1cd");
			iDailyEndPoint = OCalendar.parseString("1w");
			iMonthlyEndPoint = OCalendar.parseString("3m");
		} else {
			// Find out the various end-points
			iSubHourlyEndPoint = getParamDateValue(resConfigTable, "APM Power Sub-Hourly Endpoint");
			iHourlyEndPoint = getParamDateValue(resConfigTable, "APM Power Hourly Endpoint");
			iDailyEndPoint = getParamDateValue(resConfigTable, "APM Power Daily Endpoint");
			iMonthlyEndPoint = getParamDateValue(resConfigTable, "APM Power Monthly Endpoint");
		}

		// add 1 day to the value as then we match the APM GUI where 1cd is today and tomorrow, rather than just today
		if (iSubHourlyEndPoint > 0){
			iSubHourlyEndPoint++;
		}
		if (iHourlyEndPoint > 0){
			iHourlyEndPoint++;
		}
		if (iDailyEndPoint > 0){
			iDailyEndPoint++;
		}
		if (iMonthlyEndPoint > 0){
			iMonthlyEndPoint++;
		}
	}

	// Process at sub hourly granularity
	private void processFwdSubHourly(int iQueryId, int iTimeZone, Table tQueryCriteria, int iSubHourlyIncrementType, int iSubHourlyGranularity,
			int iValuationProduct, int iDateFormat, int iDateLocale, int iTimeFormat) throws OException {
		int iIncrementType;
		int iDateSequence;
		int iStopDate;

		if (iSubHourlyEndPoint > iFwdStartDate) {
			iIncrementType = iSubHourlyIncrementType;
			iStopDate = iSubHourlyEndPoint;
			// We can no longer set to monthly as PWR_GenerateVolumetricPositions requires monthly date sequence to start and stop on a month
			// boundary.
			iDateSequence = ROLL_CONV_ENUM.RC_CALENDAR_DAY.toInt();

			logDebugMessage("USR_APM_PowerPosition started processing at " + iSubHourlyGranularity + " Math.Math.min granularity");
			getResultsSubDaily(iQueryId, iFwdStartDate, iStopDate, iTimeZone, iIncrementType, iDateSequence, iValuationProduct, tQueryCriteria,
					iDateFormat, iDateLocale, iTimeFormat, 0);

			iFwdStartDate = iSubHourlyEndPoint;
		}
	}

	private void processFwdHourly(int iQueryId, int iTimeZone, Table tQueryCriteria, int iValuationProduct, int iDateFormat, int iDateLocale,
			int iTimeFormat) throws OException {
		// Process at hourly granularity
		if (iHourlyEndPoint > iFwdStartDate) {
			int iIncrementType = PWR_INCREMENT_TYPE_ENUM.INCREMENT_TYPE_60_MINUTE.toInt();

			int iStopDate = iHourlyEndPoint;
			// We can no longer set to monthly as PWR_GenerateVolumetricPositions requires monthly date sequence to start and stop on a month
			// boundary.
			int iDateSequence = ROLL_CONV_ENUM.RC_CALENDAR_DAY.toInt();

			logDebugMessage("USR_APM_PowerPosition started processing at HOURLY granularity");
			getResultsSubDaily(iQueryId, iFwdStartDate, iStopDate, iTimeZone, iIncrementType, iDateSequence, iValuationProduct, tQueryCriteria,
					iDateFormat, iDateLocale, iTimeFormat, 0);

			iFwdStartDate = iHourlyEndPoint;
		}
	}

	private void processFwdDaily(int iQueryId, int iTimeZone, Table tQueryCriteria, int iValuationProduct, int iDateFormat, int iDateLocale,
			int iTimeFormat) throws OException {
		// Process at daily granularity
		if (iDailyEndPoint > iFwdStartDate) {
			int iStopDate = iDailyEndPoint;

			ODateTime dtStartingPeriod = ODateTime.dtNew();
			ODateTime dtStoppingPeriod = ODateTime.dtNew();

			dtStartingPeriod.setDateTime(iFwdStartDate, 0);
			dtStoppingPeriod.setDateTime((iStopDate - 1), 86400);
			int iDateSequence = ROLL_CONV_ENUM.RC_CALENDAR_DAY.toInt();
			int iIncrementType = -1;

			logDebugMessage("USR_APM_PowerPosition started processing at daily granularity from " + OCalendar.formatJd(iFwdStartDate) + " to "
					+ OCalendar.formatJd(iStopDate - 1));
			getResultsDaily(iQueryId, dtStartingPeriod, dtStoppingPeriod, iTimeZone, iIncrementType, iDateSequence, iValuationProduct,
					tQueryCriteria, iDateFormat, iDateLocale, iTimeFormat, 1);

			iFwdStartDate = iDailyEndPoint;
			dtStartingPeriod.destroy();
			dtStoppingPeriod.destroy();
		}
	}

	private void processFwdMonthly(int iQueryId, int iTimeZone, Table tQueryCriteria, int iValuationProduct, int iDateFormat, int iDateLocale,
			int iTimeFormat, String sMonthlyDateSequence) throws OException {
		int iStopDate;
		ODateTime dtStartingPeriod = ODateTime.dtNew();
		ODateTime dtStoppingPeriod = ODateTime.dtNew();

		int iDateSequence;
		int iIncrementType;
		// Generate volumes for the days between the start date and the start of the following month.
		if (iMonthlyEndPoint > iFwdStartDate) {
			int iAlignedDate = OCalendar.getSOM(iFwdStartDate);

			if (iAlignedDate != iFwdStartDate) {
				// Check to see whether the start date is on a monthly boundary... 
				// if not then from iFwdStartDate to EOD(OCalendar.getEOM(iFwdStartDate)) generate daily data. 
				// This is to fix PWR_GenerateMonthlyVolume requiring month aligned data for month sequence.

				iStopDate = OCalendar.getEOM(iFwdStartDate) + 1;

				if (iStopDate > iMonthlyEndPoint) {
					iStopDate = iMonthlyEndPoint;
				}

				dtStartingPeriod.setDateTime(iFwdStartDate, 0);
				dtStoppingPeriod.setDateTime(iStopDate - 1, 86400);
				iDateSequence = ROLL_CONV_ENUM.RC_CALENDAR_DAY.toInt();
				iIncrementType = -1;

				logDebugMessage("USR_APM_PowerPosition processing daily before running monthly granularity for whole months from "
						+ OCalendar.formatJd(iFwdStartDate) + " to " + OCalendar.formatJd(iStopDate - 1));
				getResultsDaily(iQueryId, dtStartingPeriod, dtStoppingPeriod, iTimeZone, iIncrementType, iDateSequence, iValuationProduct,
						tQueryCriteria, iDateFormat, iDateLocale, iTimeFormat, 1);

				iFwdStartDate = iStopDate;
			}
		}

		// Process at monthly granularity for all whole months in the period from the start date to the monthly end point.
		if (iMonthlyEndPoint > iFwdStartDate) {
			// Force the end point to the start date of the month...
			iStopDate = OCalendar.getSOM(iMonthlyEndPoint);

			if (iFwdStartDate < iStopDate) {
				dtStartingPeriod.setDateTime(iFwdStartDate, 0);
				dtStoppingPeriod.setDateTime((iStopDate - 1), 86400);

				iDateSequence = iMonthlyDateSequence;
				iIncrementType = -1;

				logDebugMessage("USR_APM_PowerPosition processing monthly granularity from " + OCalendar.formatJd(iFwdStartDate) + " to "
						+ OCalendar.formatJd(iStopDate - 1));
				getResultsMonthly(iQueryId, dtStartingPeriod, dtStoppingPeriod, iTimeZone, iIncrementType, iDateSequence, iValuationProduct,
						tQueryCriteria, iDateFormat, iDateLocale, iTimeFormat, 1, sMonthlyDateSequence);

				iFwdStartDate = iStopDate;
			}
		}

		// Process any remaining days in the period.
		if (iMonthlyEndPoint > iFwdStartDate) {
			// Force the end point to the start date of the month...
			iStopDate = iMonthlyEndPoint;

			if (iFwdStartDate < iStopDate) {
				dtStartingPeriod.setDateTime(iFwdStartDate, 0);
				dtStoppingPeriod.setDateTime((iStopDate - 1), 86400);
				iDateSequence = ROLL_CONV_ENUM.RC_CALENDAR_DAY.toInt();
				iIncrementType = -1;

				logDebugMessage("USR_APM_PowerPosition processing daily granularity for remaining days in the monthly period from "
						+ OCalendar.formatJd(iFwdStartDate) + " to " + OCalendar.formatJd(iStopDate - 1));
				getResultsDaily(iQueryId, dtStartingPeriod, dtStoppingPeriod, iTimeZone, iIncrementType, iDateSequence, iValuationProduct,
						tQueryCriteria, iDateFormat, iDateLocale, iTimeFormat, 1);

				iFwdStartDate = iStopDate;
			}
		}

		dtStartingPeriod.destroy();
		dtStoppingPeriod.destroy();
	}

	// *****************************************************************************
	public void computeResult(Table sim_def) throws OException {
		int iQueryId = 0, iTimeZone;
		int iDateFormat, iDateLocale, iTimeFormat, iValuationProduct;
		Table tQueryCriteria = null, tResConfigTable = null, tListResultTypes = null;
		String sMonthlyDateSequence;
		int iSubHourlyGranularity, iSubHourlyIncrementType = 0;

		iDateFormat = DATE_FORMAT.DATE_FORMAT_DEFAULT.toInt();
		iDateLocale = DATE_LOCALE.DATE_LOCALE_US.toInt();
		iTimeFormat = TIME_FORMAT.TIME_FORMAT_HM24.toInt();
		iValuationProduct = Util.NOT_FOUND;

		// --------- RESULT PARAMETERS -----------
		// Prepare the result parameters table - this is our data source for populating the return table
		tListResultTypes = prepareResultParamTable();

		tResConfigTable = getResConfigTable(tListResultTypes);
		// --------- QUERY CRITERIA -----------
		tQueryCriteria = prepareQueryCriteriaTable();

		// --------- TIMEZONE -----------
		// Set the time zone
		iTimeZone = getTimeZone(tResConfigTable);

		// --------- SHOW ALL VOLUME STATUSES -----------
		// If we don't have "show all volume statuses" set, only show BAV
		showAllVolumeOrBav(tResConfigTable, tQueryCriteria);// need tsdQueryset info

		// --------- EXCLUDE KNOWN OPTIONS - DEFAULT BEHAVIOUR -----------
		// exclude known option statues if flag set
		// Note that this code has to be immediately after BAV code above
		excludeKnownOptions(tQueryCriteria);// need tsdQueryset info

		// --------- QUERY ID -----------
		iQueryId = setQueryID(sim_def);
		if (iQueryId == NO_ELIGIBLE_TRANSACTION) {
			return;
		}

		// --------- Monthly Date Sequence --------
		sMonthlyDateSequence = getMonthlyDateSequence(tResConfigTable);

		// --------- GRANULARITY OF SUB HOURLY -----------
		// Granularity of sub hourly data - cannot be a lookup list due to differences in param implementation between Endur versions
		iSubHourlyGranularity = getSubHourlyGranularity(tResConfigTable);
		iSubHourlyIncrementType = setSubHourlyIncType(iSubHourlyGranularity);

		// --------- PROCESS HISTORICAL DATA-----------
		iHistSubHourlyStart = getHistSubHourlyStart(tResConfigTable);
		iHistHourlyStart = getHistHourlyStart(tResConfigTable);
		iHistDailyStart = getHistDailyStart(tResConfigTable);
		iHistMonthlyStart = getHistMonthlyStart(tResConfigTable);

		processHistMonthly(iTimeZone, iQueryId, tQueryCriteria, sMonthlyDateSequence, iValuationProduct, iDateFormat, iDateLocale, iTimeFormat);

		processHistDaily(iTimeZone, iQueryId, tQueryCriteria, iValuationProduct, iDateFormat, iDateLocale, iTimeFormat);

		processHistHourly(iTimeZone, iQueryId, tQueryCriteria, iValuationProduct, iDateFormat, iDateLocale, iTimeFormat);

		processHistSubHourly(iTimeZone, iQueryId, tQueryCriteria, iValuationProduct, iDateFormat, iDateLocale, iTimeFormat, iSubHourlyIncrementType,
				iSubHourlyGranularity);

		// --------- END POINTS FOR FORWARD DATA-----------
		getForwardEndPoints(tResConfigTable);

		// --------- START PROCESSING FORWARD DATA -----------
		processFwdSubHourly(iQueryId, iTimeZone, tQueryCriteria, iSubHourlyIncrementType, iSubHourlyGranularity, iValuationProduct, iDateFormat,
				iDateLocale, iTimeFormat);

		processFwdHourly(iQueryId, iTimeZone, tQueryCriteria, iValuationProduct, iDateFormat, iDateLocale, iTimeFormat);

		processFwdDaily(iQueryId, iTimeZone, tQueryCriteria, iValuationProduct, iDateFormat, iDateLocale, iTimeFormat);

		processFwdMonthly(iQueryId, iTimeZone, tQueryCriteria, iValuationProduct, iDateFormat, iDateLocale, iTimeFormat, sMonthlyDateSequence);

		addFinancialDealsLocations(iQueryId, returnt);

		// --------- CLEAN UP -----------
		if (Table.isTableValid(tQueryCriteria) == 1) {
			tQueryCriteria.destroy();
		}

		if (Table.isTableValid(tResConfigTable) == 1) {
			tResConfigTable.destroy();
		}
		//tListResultTypes is a cached table, don't destroy it. 
		
		// If query ID is provided as a parameter, somebody else should free it
		if ((sim_def.getColNum("APM Single Deal Query") <= 0) || (sim_def.getInt("APM Single Deal Query", 1) == 0)) {
			Query.clear(iQueryId);
		}

	} // computeResult Ends

	// *****************************************************************************
	public void formatResult() throws OException {
		int iStartDateCol, iEndDateCol, iStartTimeCol, iEndTimeCol, iCol;
		// The Power.generateVolumetricPositions function does this already
		returnt.setColFormatAsDate("startdate", DATE_FORMAT.DATE_FORMAT_DEFAULT);
		returnt.setColFormatAsDate("enddate", DATE_FORMAT.DATE_FORMAT_DEFAULT);
		returnt.setColFormatAsRef("power_choice", SHM_USR_TABLES_ENUM.PWR_CHOICE_TABLE);
		returnt.setColFormatAsRef("control_area", SHM_USR_TABLES_ENUM.PWR_CTL_AREA_TABLE);
		returnt.setColFormatAsRef("power_initial_term", SHM_USR_TABLES_ENUM.INITIAL_TERM_TABLE);
		returnt.setColFormatAsRef("location", SHM_USR_TABLES_ENUM.PWR_LOCATION_TABLE);
		returnt.setColFormatAsRef("power_del_loc", SHM_USR_TABLES_ENUM.PWR_LOCATION_TABLE);
		returnt.setColFormatAsRef("power_rec_loc", SHM_USR_TABLES_ENUM.PWR_LOCATION_TABLE);
		returnt.setColFormatAsRef("product", SHM_USR_TABLES_ENUM.PWR_PRODUCT_TABLE);
		returnt.setColFormatAsRef("region", SHM_USR_TABLES_ENUM.PWR_REGION_TABLE);
		returnt.setColFormatAsRef("power_service_type", SHM_USR_TABLES_ENUM.SERVICE_TYPE_TABLE);
		returnt.setColFormatAsRef("power_timezone", SHM_USR_TABLES_ENUM.TIME_ZONE_TABLE);
		returnt.setColFormatAsRef("price_band", SHM_USR_TABLES_ENUM.PRICE_BAND_TABLE);
		returnt.setColFormatAsRef("volume_type", SHM_USR_TABLES_ENUM.VOLUME_TYPE_TABLE);
		returnt.setColFormatAsRef("projection_index", SHM_USR_TABLES_ENUM.INDEX_TABLE);
		returnt.setColFormatAsRef("unit", SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);
		returnt.setColFormatAsTime("start_time");
		returnt.setColFormatAsTime("end_time");
		returnt.setColFormatAsRef("pwr_product_id", SHM_USR_TABLES_ENUM.PRODUCT_FORMAT_TABLE);
		returnt.setColFormatAsRef("bav_flag", SHM_USR_TABLES_ENUM.TRUE_FALSE_TABLE);
		
		iStartDateCol = returnt.getColNum("startdate");
		iEndDateCol = returnt.getColNum("enddate");
		iStartTimeCol = returnt.getColNum("start_time");
		iEndTimeCol = returnt.getColNum("end_time");
		returnt.clearGroupBy();
		for (iCol = 1; iCol <= returnt.getNumCols(); iCol++){
			if (iCol == iStartDateCol || iCol == iEndDateCol || iCol == iStartTimeCol || iCol == iEndTimeCol ){
				continue;
			}
			if (returnt.getColType(iCol) == COL_TYPE_ENUM.COL_INT.toInt()){
				returnt.addGroupBy(iCol);
			}
		}
		returnt.addGroupBy(iStartDateCol);
		returnt.addGroupBy(iEndDateCol);
		returnt.addGroupBy(iStartTimeCol);
		returnt.addGroupBy(iEndTimeCol);
		returnt.groupBy();
	}

	/*-------------------------------------------------------------------------------
	Name:          APM_TABLE_LoadFromDBWithSQL
	Description:   deadlock protected version of the fn
	Parameters:      As per TABLE_LoadFromDBWithSQL
	Return Values:   retval (success or failure)
	Effects:   <any *>
	-------------------------------------------------------------------------------*/
	private int APM_TABLE_LoadFromDbWithSQL(Table table, String what, String from, String where) throws OException {
        final int nAttempts = 10;

        int iRetVal = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();

        int numberOfRetriesThusFar = 0;
        do {
        	// for error reporting further down
        	String message = null;
        	
            try {
                // db call
            	iRetVal = DBaseTable.loadFromDbWithSQL(table, what, from, where);
            } catch (OException exception) {
                iRetVal = exception.getOlfReturnCode().toInt();
                
                message = exception.getMessage();
            } finally {
                if (iRetVal == OLF_RETURN_CODE.OLF_RETURN_RETRYABLE_ERROR.toInt()) {
                    numberOfRetriesThusFar++;
                    
                    if(message == null) {
                        message = String.format("Query execution retry %1$d of %2$d. Check the logs for possible deadlocks.", numberOfRetriesThusFar, MAX_NUMBER_OF_DB_RETRIES);
                    } else {
                        message = String.format("Query execution retry %1$d of %2$d [%3$s]. Check the logs for possible deadlocks.", numberOfRetriesThusFar, MAX_NUMBER_OF_DB_RETRIES, message);
                    }
                    
                    OConsole.oprint(message);

                    Debug.sleep(numberOfRetriesThusFar * 1000);
                } else {
                    // it's not a retryable error, so leave
                    break;
                }
            }
        } while (numberOfRetriesThusFar < nAttempts);

        if (iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
        	OConsole.oprint(DBUserTable.dbRetrieveErrorInfo(iRetVal, "DBaseTable.loadFromDbWithSQL failed "));

		return iRetVal;
	}

	private void addFinancialDealsLocations(int iQueryId, Table tData) throws OException {
		int i, locID;
		Table tFinDeals, tBlankLocDeals;
		String sFrom, sWhat, sWhere;

		tFinDeals = Table.tableNew("Fin Deals");
		tBlankLocDeals = Table.tableNew("Blank Loc Deals");

		// The order of columns matches the final returnt format
		sWhat = "ab_tran.deal_tracking_num dealnum, ab_tran.ins_num, pa.param_seq_num leg, pa.param_seq_num power_location, ii.info_value power_location_str,"
				+ "pwr_locations.location_ctl_area power_ctl_area, pwr_control_area.ctl_area_region power_region, ppr.pricing_product power_product, "
				+ "0 power_choice, 0 power_initial_term, 0 power_service_type, 0 power_timezone, 0 power_loc_rec, 0 power_loc_del";

		sFrom = "ab_tran, ins_parameter pa, param_reset_header prh, query_result, idx_info ii, idx_info_types iit, idx_def id, pwr_locations, pwr_control_area, pwr_phys_reset ppr";

		sWhere = "ab_tran.tran_num = query_result.query_result AND "
				+ "pa.ins_num = ab_tran.ins_num AND "
				+ "pa.ins_num = prh.ins_num AND "
				+ "prh.param_seq_num = pa.param_seq_num AND prh.param_reset_header_seq_num = 0 AND "
				+ "prh.proj_index = id.index_id AND id.db_status = 1 AND id.index_version_id = ii.index_id AND ii.type_id = iit.type_id AND iit.type_name='Default Power Location ID' AND "
				+ "pwr_locations.location_id = ii.info_value AND pwr_locations.location_ctl_area = pwr_control_area.ctl_area_id AND "
				+ "ppr.ins_num = ab_tran.ins_num AND ppr.param_seq_num = 0 AND " + "query_result.unique_id =" + iQueryId;

		APM_TABLE_LoadFromDbWithSQL(tFinDeals, sWhat, sFrom, sWhere);

		if (Table.isTableValid(tFinDeals) == 1 && (tFinDeals.getNumRows() > 0)) {
			// We retrieved location as an Index Info field value, which is a String - convert to int
			for (i = 1; i <= tFinDeals.getNumRows(); i++) {
				locID = Str.strToInt(tFinDeals.getString("power_location_str", i));
				tFinDeals.setInt("power_location", i, locID);
				tFinDeals.setInt("power_loc_rec", i, locID);
				tFinDeals.setInt("power_loc_del", i, locID);
			}

			tBlankLocDeals.select(tData, "DISTINCT, deal_tracking_num (dealnum), param_seq_num (leg)", "location EQ 0");

			tBlankLocDeals.select(tFinDeals,
							"power_location (location), power_region (region), power_ctl_area (control_area), power_loc_rec (power_rec_loc), power_loc_del (power_del_loc)",
							"dealnum EQ $dealnum AND leg EQ $leg");

			tData.select(tBlankLocDeals, "location, region, control_area, power_rec_loc, power_del_loc",
					"dealnum EQ $deal_tracking_num AND leg EQ $param_seq_num");

			tFinDeals.destroy();
		}
		
		tBlankLocDeals.destroy();
	}

}
