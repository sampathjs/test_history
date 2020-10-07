/*$Header: /cvs/master/olf/plugins/standard/credit/STD_RSK_SumDelta.java,v 1.10.128.1 2015/07/29 12:51:17 chrish Exp $*/

/*
File Name:                      STD_RSK_SumDelta.java
 
Report Name:                    NONE
 
Output File Name:               NONE
 
Author:                         Guillaume Cortade / Alex Afraymovich
Creation Date:
 
Revision History:               Jan 29, 2013 - Donald Chan fixed incorrect Delta values for DTS 104186
								Mar 29, 2006 - Alex A. made this a standard script
                                Dec 12, 2003 - Guillaume C.
                                                
Script Type:                    Risk Online limit monitoring deal/update script

Recommended Script Category: Risk Limit

Main Script:                    
Parameter Script:               
Display Script: 
 
Description:                    Exposure is sum of delta sensitivity for each trade
                                Dependant on USER_RESULT_STD_FX_CONVERSION, PFOLIO_RESULT_TYPE.INDEX_RATE_RESULT, PFOLIO_RESULT_TYPE.TRAN_GPT_DELTA_SIDE_RESULT results
Assumptions:                    
 
Instructions:                                          
  
Uses EOD Results?
 
Which EOD Results are used?
 
When can the script be run?  
*/ 

package standard.credit;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import standard.include.JVS_INC_STD_Online;
import standard.include.JVS_INC_STD_Simulation;
import standard.include.JVS_INC_Standard;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_CREDIT_RISK)
public class STD_RSK_SumDelta implements IScript {
	private JVS_INC_STD_Online m_INCSTDOnline;
	private JVS_INC_STD_Simulation m_INCSTDSimulation;
	private JVS_INC_Standard m_INCStandard;
	private String error_log_file;

	public STD_RSK_SumDelta(){
		m_INCSTDOnline = new JVS_INC_STD_Online();
		m_INCSTDSimulation =new JVS_INC_STD_Simulation();
		m_INCStandard = new JVS_INC_Standard();

	}
	// *****************************************************************************
	public void execute(IContainerContext context) throws OException
	{

		String sFileName = "STD_RSK_SumDelta";
		error_log_file = Util.errorInitScriptErrorLog(Util.getEnv("AB_OUTDIR") + "\\error_logs\\" + sFileName);

		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();

		m_INCStandard.Print(error_log_file, "START","\n*** Script STD_RSK_SumDelta.java Starts ***\n");
		// Initialize the online script
		m_INCSTDOnline.ONL_Init(argt, returnt);

		// Call the virtual functions according to action type
		if(m_INCSTDOnline.gRunType == m_INCSTDOnline.cRunPreCheck
				|| m_INCSTDOnline.gRunType==m_INCSTDOnline.cRunDeal
				|| m_INCSTDOnline.gRunType==m_INCSTDOnline.cRunBatch)
		{    
			compute_gross(argt,returnt);
			m_INCSTDOnline.ONL_AssignBuckets(m_INCSTDOnline.cBucketUpToMaturity, argt, returnt);
			m_INCSTDOnline.ONL_AssignCollateral(returnt);
		}
		else if(m_INCSTDOnline.gRunType== m_INCSTDOnline.cRunBatchUpdate
				|| m_INCSTDOnline.gRunType== m_INCSTDOnline.cRunDealUpdate)
		{    
			compute_net(argt,returnt);
		}  

		else if(m_INCSTDOnline.gRunType==  m_INCSTDOnline.cRunAdhoc)
		{    
			compute_gross(argt,returnt);
			format_report(returnt);
		}  
		else
		{    
			m_INCSTDOnline.ONL_Fail("Incorrect operation code", returnt);
		}

		m_INCStandard.Print(error_log_file, "END","\n*** Script STD_RSK_SumDelta.java Ends ***\n");
		m_INCSTDOnline.ONL_Succeed(returnt);
	} // main/0.


	// *****************************************************************************
	void compute_gross(Table argt,Table returnt) throws OException
	{
		String sqlStr;
		Table tResList, tSimRes, tRes, tPX, tFXRes, idxMarket;
		SimResultType  userTypeSumDelta;
		int simId = 0, indexQid, x, numRows, market;
		
		// Get all the necessary simulation results
		tResList = Sim.createResultListForSim();
		SimResult.addResultForSim(tResList, SimResultType.create("PFOLIO_RESULT_TYPE.TRAN_GPT_DELTA_SIDE_RESULT"));
		SimResult.addResultForSim(tResList, SimResultType.create("PFOLIO_RESULT_TYPE.INDEX_RATE_RESULT"));
		SimResult.addResultForSim(tResList,SimResultType.create("USER_RESULT_STD_FX_CONVERSION"));

		tSimRes = m_INCSTDSimulation.SIM_ComputeResults(tResList,argt);
		tRes = m_INCSTDSimulation.SIM_GetResult(tSimRes, PFOLIO_RESULT_TYPE.TRAN_GPT_DELTA_SIDE_RESULT.toInt());
		userTypeSumDelta = SimResultType.create("USER_RESULT_STD_FX_CONVERSION");
		simId = userTypeSumDelta.getId();
		tFXRes = m_INCSTDSimulation.SIM_GetResult(tSimRes, simId);
		tPX =m_INCSTDSimulation.SIM_GetResult(tSimRes, PFOLIO_RESULT_TYPE.INDEX_RATE_RESULT.toInt());
		tSimRes.destroy();

		// create a query of index id to get the market for all the curves
		indexQid = Query.tableQueryInsert(tPX, "index_id", "query_result_plugin");
		sqlStr = "select index_id, market from idx_def, query_result_plugin where query_result_plugin.unique_id = " + indexQid +
				" and query_result_plugin.query_result = idx_def.index_id" +
				" and idx_def.db_status = 1";
		idxMarket = Table.tableNew();
		DBaseTable.execISql(idxMarket, sqlStr);
		tPX.select(idxMarket, "market", "index_id EQ $index_id");
		idxMarket.destroy();
		Query.clear(indexQid);
		
		// Convert sensitivity into dollar delta but only for commodity curves
		tRes.select( tPX, "output_value, market", "index_id EQ $index AND id EQ $gpt_id");
		numRows = tRes.getNumRows();
		for(x=1;x<=numRows;x++)
		{
			market = tRes.getInt("market", x);
			if (market != IDX_MARKET_ENUM.IDX_MARKET_COMMODITIES.toInt())
				continue;
			
			tRes.setDouble("delta", x, tRes.getDouble("delta", x) * tRes.getDouble("output_value", x));
		}

		// Convert to reporting currency
		tRes.select( tFXRes, "scen_fx_conv", "index EQ $index and deal_num EQ $deal_num AND deal_leg EQ $deal_leg");
		tRes.mathDivCol( "delta", "scen_fx_conv", "delta");
		tFXRes.destroy();

		// Use the sensitivity as exposure
		returnt.select( tRes, "SUM, delta(deal_exposure), delta", "deal_num EQ $deal_num");
		tPX.destroy();
		tRes.destroy();
		tResList.destroy();
	} // compute_gross/1.


	// *****************************************************************************
	void compute_net(Table argt,Table returnt) throws OException
	{
		Table    tClientData;
		Table    tTranInfo;

		// Get the client data and net the exposure
		tClientData = argt.getTable( "client_trade_data", 1);
		tClientData.addCol( "usage", COL_TYPE_ENUM.COL_DOUBLE);
		m_INCSTDOnline.ONL_NetExposure(tClientData, "delta");

		// Fill the return table with the netted exposures
		tTranInfo = Table.tableNew();
		tTranInfo.select( argt.getTable( "deal_table", 1), "deal_num, exp_line_id", "deal_num GT 0");
		tTranInfo.select( tClientData, "usage", "deal_num EQ $deal_num");
		tClientData.delCol( "usage");

		returnt.select( tTranInfo, "SUM, usage", "exp_line_id EQ $exp_line_id");
		tTranInfo.destroy();
	} // compute_net/0.


	// *****************************************************************************
	void format_report(Table returnt) throws OException
	{
		returnt.defaultFormat();
		returnt.viewTable();
	} // format_report/0.
}
