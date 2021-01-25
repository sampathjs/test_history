package com.olf.result;
/*
 * This UDSR calculates basic exposure for Commodity curves. It excludes discounting
 * and non-Commodity curves. It also requires the Index Rate Result. If the user 
 * wants to configure the result. The following steps need to be taken.
 * 
 * A Result Attribute Group called 'Forward Exposure Attributes' has to be created 
 * and assigned to the UDSR. It will need to contain three attributes.
 * 
 * 'Num Days Fwd Exposure': Integer - The numbers of days of exposure to calculate. The
 * 							default value for this is five if the attribute	is not created.
 * 
 * 'Fwd Reval Type':    Pick List table - constructed from reval_type table in Pick List Configuration screen,
 * 						and set id_number of reval_type in Id Column Name. This UDSR will only run as BOD if it is
 *                      in APM mode.
 *
 * 'Fwd Exposure Unit': Pick List - idx_unit table - This specifies the destination unit.
 * 						The default destination units are barrels(BBL) and metric ton (MT), plus any additional unit 
 * 	                    from the pick list. If running in APM mode, only BBL and MT will be created. 
 * 						
 *  This is a sample sql definition for user table for this result.
 * create table USER_DW_FWD_EXPOSURE
   (
 	extraction_id int ,
 	deal_num int ,
 	deal_leg int ,
 	deal_pdc int ,
 	event_source int ,
 	ins_seq_num int ,
 	ins_source_id int ,
 	payment_date int ,
 	index_id int ,
 	currency int ,
 	gpt_id int ,
 	gpt_name varchar2(255) ,
 	gpt_end_date int ,
 	delta_shift double precision,
 	alt_unit int,
 	delta double precision ,
 	scenario_mod_date int 
   )
 * 
 */

import java.util.HashMap;
import com.olf.openjvs.*;
import com.olf.openjvs.Math;
import com.olf.openjvs.enums.*;
import com.olf.result.APMUtility.APMUtility;


@PluginCategory({SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT})
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
@ScriptAttributes(allowNativeExceptions=true)
public class APM_UDSR_ForwardExposure implements IScript
{
	// Default names for the result and its attributes
	private String FWD_EXPOSURE_RESULT_ENUM_NAME = "USER_RESULT_APM_FORWARD_EXPOSURE";
	private String REVAL_ATTRIBUTE_GROUP_NAME = "Forward Exposure Attributes";
	private String NUM_DAYS_REVAL_ATTRIBUTE = "Num Days Fwd Exposure";
	private String MASS_ATTRIBUTE_PARAM = "Mass Unit";
	private String VOLUME_ATTRIBUTE_PARAM = "Volume Unit";
	private String ENERGY_ATTRIBUTE_PARAM = "Energy Unit";
	
	// Default exposure units always included in result.
	private String DEFAULT_EXPOSURE_BBL = "BBL";
	private String DEFAULT_EXPOSURE_MT = "MT";
	
	private int BOD_EOD_FLAG = 0;  // BOD mode
	private Boolean APM_RUN_MODE = Boolean.FALSE;
	private int NUM_SCENARIOS = 5;
	private String DAY_OR_CD =  "d";
	private HashMap <Integer, ExposureUnits> EXPOSURES = new HashMap<Integer, ExposureUnits>();
	private double EPSILON = 0.0001;
	// Flag to indicate if debugging for simulation results is turned on
	private Boolean DEBUG_ENABLED = Boolean.FALSE;
	private Table mapTable = null;

	
	/*
	 * The is the entry point for the UDSR. The UDSR is called multiple times
	 * when used. This method makes sure the UDSR performs the required action.
	 */
	public void execute(IContainerContext context) throws OException 
	{
		USER_RESULT_OPERATIONS operation;
		Table argt, returnt;
		
		argt = context.getArgumentsTable();
		returnt = context.getReturnTable();

		if (Debug.isAtLeastLow(OLF_DEBUG_TYPE.DebugType_SIM_RST.toInt()))
			DEBUG_ENABLED = Boolean.TRUE;
		
		operation = USER_RESULT_OPERATIONS.fromInt(argt.getInt("operation", 1));
		switch(operation)
		{
			case  USER_RES_OP_CALCULATE:
				compute_result(argt, returnt);
				break;
			case USER_RES_OP_FORMAT:
				format_result(returnt);
				break;
			case USER_RES_OP_DWEXTRACT:
				dw_extract_result(argt, returnt);
				break;
			case USER_RES_OP_AGGREGATE:
				aggregate_results(argt);
				break;
			case USER_RES_OP_FINALIZE_AGGREGATE:
			default:
				break;
		}
		return;
	}

	/*
	 * This method handles creating the result table and filling
	 * it with the exposure based on the scenario arguments.
	 */
	private void compute_result(Table argt, Table resultTable) throws OException 
	{
		Reval rData;
		Table transactions, resultList, simResults;
		int x, scenarioDate = 0, today;
		String modDate;
		
		if (DEBUG_ENABLED)
			OConsole.oprint("=> Started Computing Forward Exposure Result at " + Util.timeGetServerTimeHMS() + "<=\n");

		// Make sure there are transactions in the reval
		transactions = argt.getTable("transactions", 1);
		if ((Table.isTableValid(transactions) == 0) || (transactions.getNumRows() <= 0))
			return;
		
		// get the current date
		today = OCalendar.today();

		try
		{
			getRevalParameters(argt);
		}
		catch (OException oe)
		{
			if (DEBUG_ENABLED)
				OConsole.oprint("Warning: No scenario parameters defined for the Forward Exposure result.\n" +
					"The defaults are being used.\n");
			setDefaultResultParameters();
		}
		
		// intialize the columns
		createReturntCols(resultTable);

		// Create the Tran Gpt Delta By Leg result
		resultList = Sim.createResultListForSim();
		SimResult.addResultForSim(resultList, SimResultType.create("PFOLIO_RESULT_TYPE.TRAN_GPT_DELTA_LEG_RESULT"));	

		// create a base reval object with the delta configurations
		// we will add the horizon date modifications later
		rData = createRevalWithConfigs(transactions);
		
		try {
			// loop for the number of scenarios
			for(x=0; x<NUM_SCENARIOS; x++)
			{
				// create a format date representation of the symbolic date
				scenarioDate = OCalendar.parseString(x+DAY_OR_CD,-1, today);
				modDate = OCalendar.formatDateInt(scenarioDate, DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH);
	
				if (DEBUG_ENABLED)
					OConsole.oprint("=> Start Exposure Calculation for " + modDate + " Result at " + Util.timeGetServerTimeHMS() + "<=\n");
				// compute the results for this scenario
				simResults = computeScenarioResults(rData, modDate, resultList);
					
				addDeltaResults(resultTable, simResults, x, scenarioDate);
				if (DEBUG_ENABLED)
					OConsole.oprint("=> Finished Exposure Calculation for " + modDate + " Result at " + Util.timeGetServerTimeHMS() + "<=\n");
			}
	
			// add grid point specific information to the result
			addIndexInfo(resultTable, argt);
			doUnitConversionAndCalDelta(transactions, resultTable, scenarioDate);
			fixGptName(resultTable);
		}
		catch(OException oe)
		{
			argt.setString("error_msg", 1, oe.getMessage());
			throw oe;
		}
		finally 
		{
			// clean up
			rData.destroy();
			resultList.destroy();
			if (mapTable != null)
				mapTable.destroy();
		}
		if (DEBUG_ENABLED)
			OConsole.oprint("=> Finished Computing Forward Exposure Result at " + Util.timeGetServerTimeHMS() + "<=\n");
		return;
	}
	
	/*
	 * Convert the delta to the delivery unit and any other unit defined in the
	 * result configuration.
	 */
	private void doUnitConversionAndCalDelta(Table transactions, Table resultTable, int lastScenarioDate) throws OException
	{
		String sqlStr;
		int qid = 0, x, numRows;
		double  convFactor, deltaShift, deltaValue, delta;
		Table indexInfo = null;
		boolean startNewCfl = true, gptChanged = false;
		double oldDeltaValue = 0;
		int prevScenarioId=0, prevGptId=-1, prevIndexId=-1,  prevEventSource=-1, prevDealLeg=-1, prevDealNum=-1;
		String prevScenarioGroup= "";
		int deltaShiftCol = resultTable.getColNum("delta_shift");
		int deltaColNum = resultTable.getColNum("delta");
		int scenarioIdColNum = resultTable.getColNum("scenario_id");
		int gptIdColNum = resultTable.getColNum("gpt_id");
		int dealNumCol = resultTable.getColNum("deal_num");
		int dealLegCol = resultTable.getColNum("deal_leg");
		int eventSourceCol = resultTable.getColNum("event_source");
		int indexIdCol = resultTable.getColNum("index_id");
		int scenarioDateCol = resultTable.getColNum("scenario_mod_date");
		int scenarioGroupCol = resultTable.getColNum("scenario_group");

				
		if (DEBUG_ENABLED)
			OConsole.oprint("=> Started Unit/Contract Conversions Forward Exposure Result at " + Util.timeGetServerTimeHMS() + "<=\n");
		
		numRows = resultTable.getNumRows(); 
		if (numRows <= 0)
			return;

		try
		{
			qid = Query.tableQueryInsert(resultTable, "index_id", "query_result_plugin");
			sqlStr = "select idx_def.index_id, idx_gpt_def.gpt_id, idx_def.alt_unit, idx_gpt_def.delta_shift " +
					" from idx_def, idx_gpt_def, query_result_plugin " +
					" where query_result_plugin.unique_id  = " + qid +
					" and query_result_plugin.query_result = idx_def.index_id" +
					" and idx_def.index_version_id = idx_gpt_def.index_version_id" +
					" and idx_def.db_status = 1";
			indexInfo = Table.tableNew();
			DBaseTable.execISql(indexInfo, sqlStr);
			
			resultTable.select(indexInfo, "alt_unit, delta_shift", "index_id EQ $index_id AND gpt_id EQ $gpt_id");
	
			resultTable.group("deal_num, deal_leg, ins_source_id, scenario_group, scenario_mod_date, gpt_id");
	
			getUnitConversionFactors(transactions, resultTable);
	
			for(x=numRows; x>=1; x--)
			{
				int myGptId = resultTable.getInt(gptIdColNum, x);
				int myScenarioId = resultTable.getInt(scenarioIdColNum, x);
				String myScenarioGroup = resultTable.getString(scenarioGroupCol, x);
				boolean setRolloff = true;
				double rolloff = 0;
				int myScenarioDate = resultTable.getInt(scenarioDateCol, x);
				
				if (prevScenarioId == 0 ||
					resultTable.getInt(indexIdCol, x) != prevIndexId ||
					resultTable.getInt(dealLegCol, x) != prevDealLeg ||
					resultTable.getInt(dealNumCol, x) != prevDealNum ) 
				{
					startNewCfl = true;
				}
				else
					startNewCfl = false;
	
				if (myGptId != prevGptId && 
					myScenarioDate != lastScenarioDate && myScenarioGroup.equals(prevScenarioGroup))
				{	// catch gpt change in scenario when it's not at end of scenario date. 
					gptChanged = true;
				}
				
				//   within the same cashflow set, if cash flows from forward balance to balance, there is no rolloff. 
				if (!startNewCfl && prevEventSource == EVENT_SOURCE.EVENT_SOURCE_FORWARD_BALANCE.toInt() &&
					resultTable.getInt(eventSourceCol, x) == EVENT_SOURCE.EVENT_SOURCE_BALANCE.toInt())
					setRolloff = false;
				
				if (startNewCfl)
				{
					prevIndexId = resultTable.getInt(indexIdCol, x);
					prevEventSource = resultTable.getInt(eventSourceCol, x);
					prevDealLeg = resultTable.getInt(dealLegCol, x);
					prevDealNum = resultTable.getInt(dealNumCol, x);
					prevScenarioGroup = resultTable.getString(scenarioGroupCol, x);
				}
				prevScenarioId = myScenarioId;
				prevGptId = myGptId;
					
				deltaShift = resultTable.getDouble(deltaShiftCol, x);
				delta = resultTable.getDouble(deltaColNum, x);
				deltaValue = delta/deltaShift;
				resultTable.setDouble(deltaColNum, x, deltaValue);
					
				if (startNewCfl || gptChanged)
				{
					if (myScenarioDate == lastScenarioDate) 
						oldDeltaValue = deltaValue;
					else
						oldDeltaValue = 0;
				}
				
				if (setRolloff)
					rolloff = oldDeltaValue - deltaValue;
				if (Math.abs(rolloff) < EPSILON)
					rolloff = 0.0;
				if (rolloff != 0.0 && !gptChanged)
					rolloff *= -1;
				resultTable.setDouble(deltaColNum+1, x, rolloff);				
				oldDeltaValue = deltaValue;
				
				for(int z : EXPOSURES.keySet())
				{
					ExposureUnits eu = EXPOSURES.get(z);
					int expUnitColNum = eu.getColNum();
					convFactor = resultTable.getDouble(expUnitColNum, x);
					double expDeltaValue = deltaValue * convFactor;
					double prevDeltaValue;
					resultTable.setDouble(expUnitColNum, x, expDeltaValue);
					if (startNewCfl || gptChanged)
					{
						if (myScenarioDate == lastScenarioDate) 
							prevDeltaValue = expDeltaValue;
						else
							prevDeltaValue = 0;
					}
					else 
					{
						prevDeltaValue = resultTable.getDouble(expUnitColNum, x+1);
					}
					
					if (setRolloff)
						rolloff = prevDeltaValue - expDeltaValue;
					if (Math.abs(rolloff) < EPSILON)
						rolloff = 0.0;
					if (rolloff != 0.0 && !gptChanged)
						rolloff *= -1;
					resultTable.setDouble(expUnitColNum+1, x, rolloff);	
				}
	
				 /* we'll add a row to show delta drop off over night when gpt changes. 
				    and set delta value from next day as rolloff on new row, which will offsets today's rolloff. */
				if (gptChanged) 
				{				 
					int row = x +1;
					resultTable.copyRowAddAfter(row, resultTable, x);
					resultTable.setDouble(deltaColNum, row, 0);
					resultTable.setInt(scenarioDateCol, row, myScenarioDate);
					resultTable.setDouble(deltaColNum+1, row, resultTable.getDouble(deltaColNum, row+1));  
	
					for(int z: EXPOSURES.keySet())
					{
						ExposureUnits eu = EXPOSURES.get(z);
						int euDeltaColNum = eu.getColNum(); 
						resultTable.setDouble(euDeltaColNum, row, 0);
						resultTable.setDouble(euDeltaColNum+1, row, resultTable.getDouble(euDeltaColNum, row+1));
					}
					gptChanged = false;
				}
	
			}
			
			resultTable.deleteWhereValue(scenarioDateCol, lastScenarioDate);
			resultTable.delCol(scenarioGroupCol); 
			resultTable.delCol(scenarioIdColNum);
			
			if (DEBUG_ENABLED)
				OConsole.oprint("=> Finished Unit/Contract Conversions Forward Exposure Result at " + Util.timeGetServerTimeHMS() + "<=\n");
		}
		finally
		{
			indexInfo.destroy();
			Query.clear(qid);
		}
		return;
	}
	
	/*
	 * This method adds the index information to the result. This includes
	 * the grid point label and the coverage end date of the grid point.
	 */
	private void addIndexInfo(Table resultTable, Table argt) throws OException
	{
		int x, irrStart, irrEnd;
		Table scenResults, genResults, irr;
		
		scenResults = argt.getTable("sim_results", 1);
		if (Table.isTableValid(scenResults) == 0)
		{
			argt.setString("error_msg", 1, "Unable to access Index Rate result. Please confirm UDSR configuration.");
			return;
		}
		
		genResults = scenResults.getTable("result_class", 4);
		if (Table.isTableValid(genResults) == 0)
		{
			argt.setString("error_msg", 1, "Unable to access Index Rate result. Please confirm UDSR configuration.");
			return;
		}
		
		irrStart = genResults.findInt("result_type", PFOLIO_RESULT_TYPE.INDEX_RATE_RESULT.toInt(), SEARCH_ENUM.FIRST_IN_GROUP);
		if (irrStart <= 0)
		{
			argt.setString("error_msg", 1, "Unable to access Index Rate result. Please confirm UDSR configuration.");
			return;
		}

		irrEnd = genResults.findInt("result_type", PFOLIO_RESULT_TYPE.INDEX_RATE_RESULT.toInt(), SEARCH_ENUM.LAST_IN_GROUP);
		for(x=irrStart;x<=irrEnd;x++)
		{
			irr = genResults.getTable("result", x);
			if (Table.isTableValid(irr) == 0)
				continue;
			
			irr.addCol("index_id", COL_TYPE_ENUM.COL_INT);
			irr.setColValInt("index_id", genResults.getInt("disc_idx", x));
			resultTable.select(irr, "label(gpt_name), end_date(gpt_start_date), coverage_end_date(gpt_end_date)", "index_id EQ $index_id AND id EQ $gpt_id");
			irr.delCol("index_id");
		}
		
		return;
	}

	/*
	 * This method fills in the Tran Gpt Delta By Leg for each scenario. 
	 */
	private void addDeltaResults(Table resultTable, Table revalResult, int scenarioOffset, int scenarioDate) throws OException
	{
		int tranGptDeltaRow;
		String whatStr;
		Table genResult, delta;

		// Make sure the general results were calculated
		genResult = revalResult.getTable("result_class", 4);
		if (Table.isTableValid(genResult) == 0)
			return;
		
		// make sure the Tran Gpt Delta By Leg was calculated
		tranGptDeltaRow = SimResult.findGenResultRow(genResult, PFOLIO_RESULT_TYPE.TRAN_GPT_DELTA_LEG_RESULT.toInt(), -2, -2, -2);	
		if (tranGptDeltaRow <= 0)
			return;
		
		// make sure there wasn't any issues with the Tran Gpt Delta By Leg Result
		delta = genResult.getTable("result", tranGptDeltaRow);
		if (Table.isTableValid(delta) == 0)
			return;
		
		if (delta.getNumRows() <= 0)
			return;
		
		whatStr = "deal_num, deal_leg, deal_pdc, event_source, ins_seq_num, ins_source_id, index(index_id), " +
				  "gpt_id, payment_date, currency, delta, scenario_mod_date, scenario_id, scenario_group";
		delta.addCol("scenario_mod_date", COL_TYPE_ENUM.COL_INT);
		delta.addCol("scenario_id", COL_TYPE_ENUM.COL_INT);
		delta.addCol("scenario_group", COL_TYPE_ENUM.COL_STRING);
		delta.setColValInt("scenario_mod_date", scenarioDate);
		delta.setColValInt("scenario_id", scenarioOffset);
		
		setGroupSeq(delta, scenarioOffset);
		resultTable.select(delta, whatStr, "deal_num GT 0"); 

		return;
	}
	
	// setting the scenario group id on the result table.
	private void setGroupSeq(Table delta, int scenarioOffset) throws OException
	{
		int seqNum = 0, dealNum =-1, dealLeg =-1, idx = -1, gptId;
		
		if(0 == scenarioOffset)
		{		
			mapTable = delta.copyTable();	
			mapTable.addCol("seq_num", COL_TYPE_ENUM.COL_INT);
			mapTable.group("deal_num, deal_leg, index, gpt_id");
			
			for (int i = 1; i <= mapTable.getNumRows(); i++)
			{
				mapTable.setInt("seq_num", i, i);
			}
		}
		int dealNumCol = mapTable.getColNum("deal_num");
		for (int r = 1; r <= delta.getNumRows(); r++)
		{
			dealNum = delta.getInt("deal_num", r);
			dealLeg = delta.getInt("deal_leg", r);
			idx = delta.getInt("index", r);
			gptId = delta.getInt("gpt_id", r);
			
			int rFirst = mapTable.findInt(dealNumCol, dealNum, SEARCH_ENUM.FIRST_IN_GROUP);
			int rLast = mapTable.findInt(dealNumCol, dealNum, SEARCH_ENUM.LAST_IN_GROUP);
			boolean match = false;
			int row = 0;
			for (int i = rFirst; i <= rLast; i++)      // find record matches side and index.  
			{
				if (mapTable.getInt("deal_leg", i) == dealLeg && mapTable.getInt("index", i) == idx )
				{
					row = i;
					if (mapTable.getInt("gpt_id", i) == gptId)
					{
						match = true;
						break;
					}
				}
			}
			if (match)
				seqNum = mapTable.getInt("seq_num", row);			
			else
			{
				if (row > 0)   // find index on side, but gpt changed.
				{			
					mapTable.setInt("gpt_id", row, gptId);	// set new gpt on map.
					seqNum = mapTable.getInt("seq_num", row);
				}
				else
				{      // in case nothing exists, add the new record.
					int nr = mapTable.addRow();
					seqNum = mapTable.getNumRows()+1;
					mapTable.setInt("deal_num", nr, dealNum);
					mapTable.setInt("deal_leg", nr, dealLeg);
					mapTable.setInt("index", nr, idx);
					mapTable.setInt("gpt_id", nr, gptId);
					mapTable.setInt("seq_num", nr, seqNum);
				}
			}
			delta.setString("scenario_group", r, idx + "-" + seqNum);
		}
		
	}
	
	/*
	 * The method builds the scenario and runs the results for it.
	 */
	private Table computeScenarioResults(Reval rData, String modDate, Table resultList) throws OException
	{
		Table revalResult;
		
		/* Set the horizon date modification, Roll Market Data Flag
		 *  and EOD/BOD flag for the scenario run.
		 */
		rData.setHorizonRollMarketData(0);
		rData.setHorizonDate(modDate);
		
		//rData.setHorizonEODBODFlag(APM_RUN_MODE == Boolean.TRUE ? 0 : BOD_EOD_FLAG);  // always in BOD for now.
		rData.setHorizonEODBODFlag(BOD_EOD_FLAG);
		if (DEBUG_ENABLED)
			OConsole.oprint("=> Runnig Forward Exposure in (" + (BOD_EOD_FLAG == 0?"BOD":"EOD") + ") mode.\n");

		// run the results and then clear the horizon date modification
		revalResult = rData.computeResults(resultList);
		rData.clearHorizonDate();
		
		// check if everything ran successfully
		if (Table.isTableValid(revalResult) == 0)
			throw new OException("Failed to compute results for " + modDate + ".");
		
		return revalResult;
	}
	
	/*
	 *  This method creates the Reval object, sets the Scenario configurations for 
	 *  Tran Gpt Delta By Leg and adds transactions to the reval.
	 */
	
	private Reval createRevalWithConfigs(Table transactions) throws OException
	{
		int exRow, x, y, numParam, cutoffDate, matDate, numTrans, toolsetId;
		Table expandAllDeltaConfig;
		Transaction tran;
		Reval rData;
		
		rData = Reval.create();
		if(rData == Util.NULL_REVAL_DATA)
			throw new OException("Unable to create Reval object.");

		/* Set the scenario configurations */
		expandAllDeltaConfig = Sim.createConfigSubTableForType(SCENARIO_TARGET_TYPE.SCEN_RESULT,  RESULT_ATTRIBUTE_GROUPS.RES_ATTR_GRP_DELTA_GAMMA.toInt());

		/* Turn off the exposure for all non-commidity indexes */
		exRow = expandAllDeltaConfig.findInt("param_id", RESULT_ATTRIBUTE_TYPES.RES_ATTR_TRAN_GPT_DELTA_INDEX_MARKET_CONFIG.toInt(), SEARCH_ENUM.FIRST_IN_GROUP);
		expandAllDeltaConfig.setString("param_value", exRow, "Currencies,Equities,Fixed Income,Recovery,Spot Price,UOM");

		/* set all discount factors to 1.0 */
		exRow = expandAllDeltaConfig.findInt("param_id", RESULT_ATTRIBUTE_TYPES.RES_ATTR_TRAN_GPT_DELTA_ZERO_INTEREST_RATES.toInt(), SEARCH_ENUM.FIRST_IN_GROUP);
		expandAllDeltaConfig.setString("param_value", exRow, "Set Discount Factors to 1.0"); 
		
		// Return all Delta values (Required in order to get Event source as a column)
		exRow = expandAllDeltaConfig.findInt("param_id", RESULT_ATTRIBUTE_TYPES.RES_ATTR_TRAN_GPT_DELTA_FULL_DISPLAY.toInt(), SEARCH_ENUM.FIRST_IN_GROUP);
		expandAllDeltaConfig.setString("param_value", exRow, "Expand all Delta values");
		
		rData.addConfiguration(SCENARIO_TARGET_TYPE.SCEN_RESULT, RESULT_ATTRIBUTE_GROUPS.RES_ATTR_GRP_DELTA_GAMMA.toInt(), expandAllDeltaConfig);

		// calculate a cut-off date for deals. If the maturity of the deal
		// is prior to that date, then exclude it from the reval.
		cutoffDate = OCalendar.getSOM(OCalendar.getSOM(OCalendar.today()) - 1);
		
		// add the transactions to the reval leaving out non-commodity toolsets
		numTrans = transactions.getNumRows();
		for(x=1;x<=numTrans;x++)
		{
			tran = transactions.getTran("tran_ptr", x);
			
			/*
			 * If this is not one of the commodity type toolsets, then
			 * exclude it from the simulation. Commodity, ComFut, ComSwap
			 * and similar toolsets are kept. The Swap, Option, LoanDep and 
			 * various other toolsets are excluded.
			 */
			toolsetId = tran.getFieldInt(TRANF_FIELD.TRANF_TOOLSET_ID, 0);
			if (!isCommodityToolset(toolsetId))
				continue;
			
			/*
			 *  This is code to exclude deals. This section only allows transactions 
			 *  where the maturity date is later than the start of the prior month.
			 *  For example, if the current date is September 15th and deal with
			 *  with a maturity prior to August 1st will be excluded. This
			 *  is purposely a little long to allow for delayed payments, etc. 
			 */
			numParam = tran.getFieldInt(TRANF_FIELD.TRANF_NUM_PARAM_REC, 0);
			for(y=0;y<numParam;y++)
			{
				matDate = tran.getFieldInt(TRANF_FIELD.TRANF_MAT_DATE, y);
				if (matDate < cutoffDate)
					continue;
				
				rData.addTransaction(tran);
			}
		}

		return rData;
	}

	/*
	 * Is this a commodity oriented toolset
	 */
	private Boolean isCommodityToolset(int toolsetId)
	{
		TOOLSET_ENUM toolset;
		
		toolset = TOOLSET_ENUM.fromInt(toolsetId);
		switch(toolset)
		{
			case COMMODITY_TOOLSET:
			case COM_SWAP_TOOLSET:
			case COM_OPT_TOOLSET:
			case COM_FUT_TOOLSET:
			case COM_OPT_FUT_TOOLSET:
			case COM_SWAPTION_TOOLSET:
			case COM_FWD_TOOLSET:
			case ENERGY_LTP_TOOLSET:
			case ENERGY_PHYS_TOOLSET:
			case ENERGY_SWAPTION_TOOLSET:
			case ENERGY_TS_TOOLSET:
			case COM_HOURLY_TOOLSET:
			case COM_MTL_OPT_TOOLSET:
			case COM_MTL_PHYS_TOOLSET:
			case COM_MTL_SWAP_TOOLSET:
			case POWER_TOOLSET:
				return Boolean.TRUE;
			default:
				return Boolean.FALSE;
		}
	}
	
	private void fixGptName(Table resultTable) throws OException
	{
		if (!Table.isValidTable(resultTable))
			return;
		
		if(resultTable.getNumRows() < 1)
			return;
		
		int gptNameCol = resultTable.getColNum("gpt_name");
		for (int i = resultTable.getNumRows(); i > 0; i--)
		{
			String gptName = resultTable.getString(gptNameCol, i);
			if (gptName != null && gptName.contains("/"))
			{
				gptName = gptName.substring(0, gptName.indexOf("/"));
				resultTable.setString(gptNameCol, i, gptName);
			}
		}
	}
	
	private void getUnitConversionFactors(Table transactions, Table resultTable) throws OException
	{
		Transaction tran = Util.NULL_TRAN;
		int toolset = 0;
		int altUnitColNum = resultTable.getColNum("alt_unit");
		Table cflowTable = Util.NULL_TABLE;
		
		int numRows = resultTable.getNumRows(); 
		int prevDealNum = Util.NOT_FOUND;

		for (int i=1; i <= numRows; i++)
		{
			int curveUnit = resultTable.getInt(altUnitColNum, i);
			int dealNum = resultTable.getInt("deal_num", i);
			int side = resultTable.getInt("deal_leg", i);
			int dealProfile = resultTable.getInt("deal_pdc", i);
			
			if (prevDealNum != dealNum)
			{
				try 
				{
					int row = transactions.unsortedFindInt("deal_num", dealNum);
					tran = transactions.getTran("tran_ptr", row);
					toolset = tran.getFieldInt(TRANF_FIELD.TRANF_TOOLSET_ID.toInt());
					prevDealNum = dealNum;
					if (Table.isValidTable(cflowTable))
						cflowTable.destroy();
					cflowTable = tran.getCashflowDetails(Util.NULL_TABLE);
				}
				catch (OException e) 
				{
					String errMsg = "\nERROR:  Deal "+dealNum +" failed to get cashflow table. \nCore message: '" + e.getMessage()+"'\n";
					OConsole.oprint(errMsg);
					throw (new OException(errMsg));
				}
			}
			int parcelID = resultTable.getInt("ins_seq_num", i); 		
			
			if (toolset == TOOLSET_ENUM.COMMODITY_TOOLSET.toInt() && Table.isValidTable(cflowTable))
			{
				parcelID = getPricingParcel(cflowTable, resultTable, i);
			}
			
			for(int z: EXPOSURES.keySet())
			{
				double convFactor = 0.0;
				ExposureUnits eu = EXPOSURES.get(z);
				int exposureUnit = eu.getUnitID();
				int expCol = eu.getColNum();
				
				convFactor = APMUtility.GetUnitConversionFactorByParcel(curveUnit, exposureUnit, tran, side, dealProfile, parcelID);
				resultTable.setDouble(expCol, i, convFactor);
				resultTable.setInt(expCol -1, i, exposureUnit);
			}
		}
		if (Table.isValidTable(cflowTable))
			cflowTable.destroy();
	}
	
	private int getPricingParcel(Table cflowTable, Table resultTable, int row) throws OException
	{
		int pricingParcelId = -1;
		Table cflowRec = Util.NULL_TABLE;
		try 
		{
			if (Table.isValidTable(cflowTable) && cflowTable.getNumRows() > 0)
			{			
				int dealNum = resultTable.getInt("deal_num", row);
				int parcelID = resultTable.getInt("ins_seq_num", row);
				int side = resultTable.getInt("deal_leg", row);
				int insSourceId = resultTable.getInt("ins_source_id", row);  // cflow ID
				int eventSource = resultTable.getInt("event_source", row);
				
				if (eventSource == EVENT_SOURCE.EVENT_SOURCE_PARCEL_COMMODITY.toInt() || 
					eventSource == EVENT_SOURCE.EVENT_SOURCE_PARCEL.toInt() )
					pricingParcelId = parcelID;
				else if (eventSource == EVENT_SOURCE.EVENT_SOURCE_FORWARD_BALANCE.toInt())					
				{
					cflowRec = Table.tableNew();
					String wStr = "deal_tracking_num EQ " + dealNum + " AND param_seq_num EQ " + side + " AND cflow_id EQ " + insSourceId;
				
					cflowRec.select(cflowTable, "parcel_group", wStr);
					if (cflowRec.getNumRows() == 1)
					{
						pricingParcelId = cflowRec.getInt(1, 1);
					}
				}
			}	
		} 
		finally
		{
			cflowRec.destroy();
		}
				
		return pricingParcelId;
	}
	
	/*
	 * Add formatting to the result table. This method is called
	 * each time the result is displayed in the viewer.
	 */
	private void format_result(Table returnt) throws OException 
	{
		int deltaColNum, scenarioModDateCol;
		
		if (DEBUG_ENABLED)
			OConsole.oprint("=> Started Formatting Forward Exposure Result at " + Util.timeGetServerTimeHMS() + "<=\n");

		returnt.setColTitle("deal_num", "Deal\nNum");
		returnt.setColTitle("index_id", "Index\nName");
		returnt.setColTitle("gpt_id", "Gpt\nID");
		returnt.setColTitle("gpt_name", "Gpt\nName");
		returnt.setColTitle("gpt_end_date", "Gpt\nEnd\nDate");
		returnt.setColTitle("deal_leg", "Param\nSeq\nNum");
		returnt.setColTitle("deal_pdc", "Profile\nSeq\nNum");
		returnt.setColTitle("event_source", "Event\nSource");
		returnt.setColTitle("ins_source_id", "Instrument\nSource\nId");
		returnt.setColTitle("ins_seq_num", "Instrument\nSeq\nNum");
		returnt.setColTitle("payment_date", "Payment\nDate");
		returnt.setColTitle("currency", " \nCurrency");
		returnt.setColTitle("delta_shift", "Delta\nShift");
		returnt.setColTitle("alt_unit", "Delivery\nUnit");
		returnt.setColFormatAsNotnlAcct("delta_shift", Util.NOTNL_WIDTH, Util.RATE_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		returnt.setColFormatAsRef("index_id", SHM_USR_TABLES_ENUM.INDEX_TABLE);
		returnt.setColFormatAsRef("currency", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		returnt.setColFormatAsRef("event_source", SHM_USR_TABLES_ENUM.EVENT_SOURCE_TABLE);
		returnt.setColFormatAsRef("alt_unit", SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);
		returnt.setColFormatAsDate("payment_date");
		returnt.setColFormatAsDate("gpt_end_date");
		
		deltaColNum = returnt.getColNum("delta");
		returnt.setColTitle(deltaColNum, "Delta\nDelivery\nUnit");
		returnt.setColFormatAsNotnlAcct(deltaColNum, Util.NOTNL_WIDTH, Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		returnt.setColTitle("delta_rolloff", "Delta\nRoll Off");
		
		returnt.setColTitle("mass_unit", " Mass\nUnit");
		returnt.setColFormatAsRef("mass_unit", SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);
		returnt.setColTitle("mass_delta", " Delta\nMass");
		returnt.setColFormatAsNotnlAcct("mass_delta", Util.NOTNL_WIDTH, Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		returnt.setColTitle("mass_rolloff", "Roll Off \nMass");

		returnt.setColTitle("volume_unit", " Volume\nUnit");
		returnt.setColFormatAsRef("volume_unit", SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);
		returnt.setColTitle("volume_delta", " Delta\nVolume");
		returnt.setColFormatAsNotnlAcct("volume_delta", Util.NOTNL_WIDTH, Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		returnt.setColTitle("volume_rolloff", "Roll Off \nVolume");

		returnt.setColTitle("energy_unit", " Energy\nUnit");
		returnt.setColFormatAsRef("energy_unit", SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);
		returnt.setColTitle("energy_delta", " Delta\nEnergy");
		returnt.setColFormatAsNotnlAcct("energy_delta", Util.NOTNL_WIDTH, Util.NOTNL_PREC, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		returnt.setColTitle("energy_rolloff", "Roll Off \nEnergy");

		scenarioModDateCol = returnt.getColNum("scenario_mod_date");
		returnt.setColTitle(scenarioModDateCol, "Scenario\nDate");
		returnt.setColFormatAsDate(scenarioModDateCol);
		returnt.group("deal_num, deal_leg, deal_pdc, event_source, ins_seq_num, ins_source_id, index_id, gpt_id, scenario_mod_date");
		returnt.colHide("gpt_start_date");
		
		if (DEBUG_ENABLED)
			OConsole.oprint("=> Finished Formatting Forward Exposure Result at " + Util.timeGetServerTimeHMS() + "<=\n");
		return;
	}

	/* 
	 * Create the columns in the result table. The number of columns
	 * will be different if pivots are on/off.
	 */
	private void createReturntCols(Table resultTable) throws OException 
	{

		resultTable.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		resultTable.addCol("deal_leg", COL_TYPE_ENUM.COL_INT);
		resultTable.addCol("deal_pdc", COL_TYPE_ENUM.COL_INT);
		resultTable.addCol("event_source", COL_TYPE_ENUM.COL_INT);
		resultTable.addCol("ins_seq_num", COL_TYPE_ENUM.COL_INT);
		resultTable.addCol("ins_source_id", COL_TYPE_ENUM.COL_INT);
		resultTable.addCol("payment_date", COL_TYPE_ENUM.COL_INT);
		resultTable.addCol("index_id", COL_TYPE_ENUM.COL_INT);
		resultTable.addCol("currency", COL_TYPE_ENUM.COL_INT);
		resultTable.addCol("gpt_id", COL_TYPE_ENUM.COL_INT);
		resultTable.addCol("gpt_name", COL_TYPE_ENUM.COL_STRING);
		resultTable.addCol("gpt_start_date", COL_TYPE_ENUM.COL_INT);
		resultTable.addCol("gpt_end_date", COL_TYPE_ENUM.COL_INT);
		resultTable.addCol("delta_shift", COL_TYPE_ENUM.COL_DOUBLE);
		resultTable.addCol("alt_unit", COL_TYPE_ENUM.COL_INT);
		
		resultTable.addCol("delta", COL_TYPE_ENUM.COL_DOUBLE);
		resultTable.addCol("delta_rolloff", COL_TYPE_ENUM.COL_DOUBLE);
		for(int k : EXPOSURES.keySet())
		{
			ExposureUnits eu = EXPOSURES.get(k);			
			resultTable.addCol(eu.getUnitType()+"_unit", COL_TYPE_ENUM.COL_INT);
			resultTable.addCol(eu.getUnitType()+"_delta", COL_TYPE_ENUM.COL_DOUBLE);
			resultTable.addCol(eu.getUnitType()+"_rolloff", COL_TYPE_ENUM.COL_DOUBLE);
			int colNum = resultTable.getColNum(eu.getUnitType()+"_delta");
			eu.setColNum(colNum);
		}
		resultTable.addCol("scenario_mod_date", COL_TYPE_ENUM.COL_INT);
		resultTable.addCol("scenario_id", COL_TYPE_ENUM.COL_INT);
		
		return;
	}

	/*
	 * If the Simulation doesn't contain a result configuration, go to the database
	 * and check to see if there is a saved configuration for this result attribute 
	 * group and use those parameters. If it doesn't find those, the defaults
	 * defined in the plugin are used.
	 */
	private void setDefaultResultParameters() throws OException
	{
		String sqlStr, value;
		int numRows, x;
		Table resParam;
		
		int attribGroupId = Ref.getValue(SHM_USR_TABLES_ENUM.RES_ATTR_GRP_TABLE, REVAL_ATTRIBUTE_GROUP_NAME);
		final int numDaysAttribId = Ref.getValue(SHM_USR_TABLES_ENUM.PFOLIO_RESULT_ATTR_TYPES_TABLE, NUM_DAYS_REVAL_ATTRIBUTE);
		
		int unitMassbId = Ref.getValue(SHM_USR_TABLES_ENUM.PFOLIO_RESULT_ATTR_TYPES_TABLE, MASS_ATTRIBUTE_PARAM);
		int unitVolumeId = Ref.getValue(SHM_USR_TABLES_ENUM.PFOLIO_RESULT_ATTR_TYPES_TABLE, VOLUME_ATTRIBUTE_PARAM);
		int unitEnergyId = Ref.getValue(SHM_USR_TABLES_ENUM.PFOLIO_RESULT_ATTR_TYPES_TABLE, ENERGY_ATTRIBUTE_PARAM);

		sqlStr = "select pfolio_result_attr_grp_items.res_attr_id, pfolio_result_attr_grp_items.value " +
				" from pfolio_result_attr_grp_items " +
				" where pfolio_result_attr_grp_items.res_attr_grp_id = " + attribGroupId;
		resParam = Table.tableNew();
		DBaseTable.execISql(resParam, sqlStr);

		numRows = resParam.getNumRows();
		
		try
		{
			// use whatever is set in the database.
			for(x=1; x<=numRows; x++)
			{
				int attribId = resParam.getInt("res_attr_id", x);
				value = resParam.getString("value", x);
				if ((value == null) || (value.length() <= 0))
					continue;
	
				if (attribId == numDaysAttribId)
					NUM_SCENARIOS = Integer.parseInt(value) +1;
				
				if (attribId == unitMassbId)
					EXPOSURES.put(unitMassbId, new ExposureUnits(value, "mass"));
				if (attribId == unitVolumeId)
					EXPOSURES.put(unitVolumeId, new ExposureUnits(value, "volume"));
				if (attribId == unitEnergyId)
					EXPOSURES.put(unitEnergyId, new ExposureUnits(value, "energy"));
			}
		}
		finally 
		{
			resParam.dispose();
		}
		return;
	}
	/*
	 * Parse through the simulation definition for this
	 * scenario and get the settings for the number of days
	 * and pivot flag.
	 */
	private void getRevalParameters(Table argt) throws OException
	{
		int scenId, x, numScenarios;
		Table simDef, scenarioDef;
		
		simDef = argt.getTable("sim_def", 1);
		if ((Table.isTableValid(simDef) == 0) || (simDef.getNumRows() <= 0))
			throw new OException("No simulation definition.");
		
		scenarioDef = simDef.getTable("scenario_def", 1);
		if ((Table.isTableValid(scenarioDef) == 0) || (scenarioDef.getNumRows() <= 0))
			throw new OException("No scenario definition.");
		
		if(simDef.getColNum("APM Run Mode") > 1)
		{
			APM_RUN_MODE = Boolean.TRUE;
			//BOD_EOD_FLAG = 0;
		}
				
		simDef.getString("name", 1);
		scenId = argt.getInt("scen_id", 1);

		numScenarios = scenarioDef.getNumRows();
		for(x=1;x<=numScenarios;x++)
		{
			if (scenarioDef.getInt("scenario_id", x) != scenId)
				continue;
			
			scenarioDef.getString("scenario_name", x);
			break;
		}
		
		// Find the result configuration for the defined attribute group
		 Table tblAttributeGroups, tblConfig = null; 
		 int iAttributeGroup;
		 String strVal;
		 
	      tblAttributeGroups = SimResult.getAttrGroupsForResultType(argt.getInt("result_type", 1));

	      if(tblAttributeGroups.getNumRows() > 0)
	      {
	    	  iAttributeGroup = tblAttributeGroups.getInt("result_config_group", 1);
	    	  tblConfig = SimResult.getResultConfig(iAttributeGroup);
	    	  tblConfig.sortCol("res_attr_name");
	      }

	      strVal = APMUtility.GetParamStrValue(tblConfig, NUM_DAYS_REVAL_ATTRIBUTE);
	      if (!strVal.isEmpty())
	    	  NUM_SCENARIOS = Integer.parseInt(strVal) +1;
	      
	      strVal = APMUtility.GetParamStrValue(tblConfig, MASS_ATTRIBUTE_PARAM);
	      if (strVal.isEmpty())
	    	  EXPOSURES.put(Ref.getValue(SHM_USR_TABLES_ENUM.PFOLIO_RESULT_ATTR_TYPES_TABLE, MASS_ATTRIBUTE_PARAM), 
	    			  new ExposureUnits(DEFAULT_EXPOSURE_MT, "mass"));
	      else
	    	  EXPOSURES.put(Ref.getValue(SHM_USR_TABLES_ENUM.PFOLIO_RESULT_ATTR_TYPES_TABLE, MASS_ATTRIBUTE_PARAM),
	    			  new ExposureUnits(strVal, "mass"));
	      
	      strVal = APMUtility.GetParamStrValue(tblConfig, VOLUME_ATTRIBUTE_PARAM);
	      if (strVal.isEmpty())
	    	  EXPOSURES.put(Ref.getValue(SHM_USR_TABLES_ENUM.PFOLIO_RESULT_ATTR_TYPES_TABLE, VOLUME_ATTRIBUTE_PARAM), 
	    			  new ExposureUnits(DEFAULT_EXPOSURE_BBL, "volume"));
	      else
	    	  EXPOSURES.put(Ref.getValue(SHM_USR_TABLES_ENUM.PFOLIO_RESULT_ATTR_TYPES_TABLE, VOLUME_ATTRIBUTE_PARAM),
	    			  new ExposureUnits(strVal, "volume"));

	      strVal = APMUtility.GetParamStrValue(tblConfig, ENERGY_ATTRIBUTE_PARAM);
	      if (!strVal.isEmpty())
	      {
	    	  EXPOSURES.put(Ref.getValue(SHM_USR_TABLES_ENUM.PFOLIO_RESULT_ATTR_TYPES_TABLE, ENERGY_ATTRIBUTE_PARAM),
	    			  new ExposureUnits(strVal, "energy"));
	      }
	      	     
		return;
	}
	
	/*
	 * This method is called when the result is run on
	 * the grid. This appends the results to each other.
	 */
	private void aggregate_results(Table argt) throws OException
	{
		Table masterDelta, currentDelta;
		
		if (DEBUG_ENABLED)
			OConsole.oprint("=> Started Aggregating Forward Exposure Result at " + Util.timeGetServerTimeHMS() + "<=\n");

		masterDelta = argt.getTable("master_results", 1);
		currentDelta = argt.getTable("current_results", 1);
		if((Table.isTableValid(masterDelta) == 1) && (Table.isTableValid(currentDelta) == 1))
			currentDelta.copyRowAddAll(masterDelta);

		if (DEBUG_ENABLED)
			OConsole.oprint("=> Finished Aggregating Forward Exposure Result at " + Util.timeGetServerTimeHMS() + "<=\n");

		return;
	}
	
	/*
	 * This method is called when there is an extraction to the Endur/Findur
	 * data warehouse staging tables. This may need to be updated depending 
	 * on how the user models the output and if pivot is enabled.
	 */
	public void dw_extract_result(Table argt, Table returnt) throws OException
	{
		SimResultType fwdExposureResult;
		Table stblSimResults, stblGenResults, stblResults;
		String strDWTable;
		int simId;

		if (DEBUG_ENABLED)
			OConsole.oprint("=> Started Extracting Forward Exposure Result at " + Util.timeGetServerTimeHMS() + "<=\n");
		try 
		{
			fwdExposureResult = SimResultType.create(FWD_EXPOSURE_RESULT_ENUM_NAME);
		}
		catch(OException e)
		{
			argt.setString("error_msg", 1, "Can't create a SimResultType object for the Forward Exposure Result.");
			throw new OException("Can't create a SimResultType object for the Forward Exposure Result.");
		}

		simId = fwdExposureResult.getId();
		stblSimResults = argt.getTable("sim_results", 1);
		if (Table.isTableValid(stblSimResults) == 0)
		{
			argt.setString("error_msg", 1, "Could not find the Forward Exposure Result in the General Result table");
			throw new OException( "Could not find the Forward Exposure Result General Result table");
		}

		stblGenResults = stblSimResults.getTable(1, 4);
		if (Table.isTableValid(stblGenResults) == 0)
		{
			argt.setString("error_msg", 1, "Could not find the Forward Exposure Result in the General Result table");
			throw new OException( "Could not find the Forward Exposure Result General Result table");
		}

		// access user result using defined enumeration
		stblResults = SimResult.findGenResultTable(stblGenResults, simId, -2, -2, -2);  
		if(Table.isTableValid(stblResults) == 0)
		{
			argt.setString("error_msg", 1, "Could not find the Forward Exposure Result in the General Result table");
			throw new OException( "Could not find the Forward Exposure Result General Result table");
		}

		strDWTable = argt.getString("dw_user_table_name", 1);
		if ((strDWTable == null) || (strDWTable.length() <= 0))
		{
			argt.setString("error_msg", 1, "User table not configured in the Forward Exposure Result definition.");
			throw new OException( "User table not configured in the Forward Exposure Result definition.");
		}
		returnt.setTableName(strDWTable);

		// build data warehouse extraction table
		returnt.addCol("extraction_id", COL_TYPE_ENUM.COL_INT);
		returnt.select(stblResults, "*", "deal_num GT 0");

		if (DEBUG_ENABLED)
			OConsole.oprint("=> Finished Extracting Forward Exposure Result at " + Util.timeGetServerTimeHMS() + "<=\n");
		return;
	}
	
	public class ExposureUnits
	{
		private int unitID;
		private int colNum;
		private String unitType;
		
		public ExposureUnits() {}
		
		public ExposureUnits(String strVal, String uType) throws OException {
			unitID = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, strVal);
			colNum = -1;
			unitType = uType;
		}
		
		int getUnitID()
		{
			return unitID;
		}
		
		void setColNum(int n)
		{
			colNum = n;			
		}
		int getColNum()
		{
			return colNum;
		}
		
		String getUnitType()
		{
			return unitType;
		}
	}
}
