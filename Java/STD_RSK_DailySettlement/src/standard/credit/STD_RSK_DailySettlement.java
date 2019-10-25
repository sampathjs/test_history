/*$Header: /cvs/master/olf/plugins/standard/credit/STD_RSK_DailySettlement.java,v 1.5 2012/05/16 19:42:30 dzhu Exp $*/

/*
File Name:                      STD_RSK_DailySettlement.java

Report Name:                    NONE

Output File Name:               NONE

Author:                         Guillaume Cortade / Alex Afraymovich
Creation Date:

Revision History:               Mar 29, 2006 - Alex A. made this a standard script
                                Dec 02, 2003 - Guillaume C.

Script Type:                    Credit Online limit monitoring deal/update script

Recommended Script Category: Risk Limit

Main Script:                    
Parameter Script:               
Display Script: 

Description:                    Exposure is maximum daily value of settlements
                                Dependant on USER_RESULT_STD_FX_CONVERSION and PFOLIO_RESULT_TYPE.CFLOW_BY_DAY_RESULT results

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


@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_CREDIT_RISK)
public class STD_RSK_DailySettlement implements IScript {
	private JVS_INC_STD_Simulation m_INCSTDSimulation;
	private JVS_INC_STD_Online m_INCSTDOnline;
	public STD_RSK_DailySettlement(){
		m_INCSTDOnline = new JVS_INC_STD_Online();
		m_INCSTDSimulation = new JVS_INC_STD_Simulation();

	}



	// *****************************************************************************
	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();

		// Initialize the online script
		m_INCSTDOnline.ONL_Init(argt,returnt);

		// Call the virtual functions according to action type

		if(m_INCSTDOnline.gRunType == m_INCSTDOnline.cRunPreCheck 
				|| m_INCSTDOnline.gRunType == m_INCSTDOnline.cRunDeal
				|| m_INCSTDOnline.gRunType == m_INCSTDOnline.cRunBatch )
		{
			compute_gross(argt,returnt);
			m_INCSTDOnline.ONL_AssignBuckets(m_INCSTDOnline.cBucketExact,argt,returnt);
		}
		else
			if(m_INCSTDOnline.gRunType == m_INCSTDOnline.cRunBatchUpdate
					|| m_INCSTDOnline.gRunType == m_INCSTDOnline.cRunDealUpdate)
			{
				compute_net(argt,returnt);   
			}
			else
				if(m_INCSTDOnline.gRunType == m_INCSTDOnline.cRunAdhoc)
				{
					compute_gross(argt,returnt);
					format_report(returnt);
				}
				else
				{
					m_INCSTDOnline.ONL_Fail("Incorrect operation code",returnt);
				}

		m_INCSTDOnline.ONL_Succeed(returnt);
	} // main/0.


	// *****************************************************************************
	void compute_gross(Table argt,Table returnt) throws OException
	{
		Table tResList, tSimRes, tRes, tFX, tClient;
		int      iLoop, iDeal, iRow;
		int simId=0;
		SimResultType simType= SimResultType.create("USER_RESULT_STD_FX_CONVERSION");
		simId= simType.getId();
		// Get all the necessary simulation results
		tResList = Sim.createResultListForSim();
		SimResult.addResultForSim(tResList, SimResultType.create("PFOLIO_RESULT_TYPE.CFLOW_BY_DAY_RESULT"));
		//Sim.addResultForSim(tResList, USER_RESULT_STD_FX_CONVERSION);
		SimResult.addResultForSim(tResList, simType);


		tSimRes = m_INCSTDSimulation.SIM_ComputeResults(tResList,argt);
		tRes =  m_INCSTDSimulation.SIM_GetResult(tSimRes, PFOLIO_RESULT_TYPE.CFLOW_BY_DAY_RESULT.toInt());
		tFX =  m_INCSTDSimulation.SIM_GetResult(tSimRes, simId);

		// Convert the cash flow amounts to the reporting currency
		tRes.select( tFX, "*", "deal_num EQ $deal_num AND deal_leg EQ $deal_leg");
		tRes.mathDivCol( "cflow", "scen_fx_conv", "cflow");
		tFX.destroy();

		// Get the table of cash flows by day into the client data
		returnt.addCol( "cash_flows", COL_TYPE_ENUM.COL_TABLE);
		for(iLoop = returnt.getNumRows(); iLoop > 0; iLoop--)
		{
			// Create the delta table for the transaction
			iDeal = returnt.getInt( "deal_num", iLoop);
			tClient = Table.tableNew("ClientData" + iDeal);

			// Save the delta results
			tClient.select( tRes, "*", "deal_num EQ " + iDeal);
			returnt.setTable( "cash_flows", iLoop, tClient);
		} // for iLoop.

		// Use the highest cash flow as exposure
		tRes.group( "deal_num, cflow");
		for(iLoop = returnt.getNumRows(); iLoop > 0; iLoop--)
		{
			iDeal = returnt.getInt( "deal_num", iLoop);
			iRow = tRes.findInt( "deal_num", iDeal, SEARCH_ENUM.LAST_IN_GROUP);

			if(iRow > 0)
				returnt.setDouble( "deal_exposure", iLoop,
						tRes.getDouble( "cflow", iRow));
		} // for iLoop.

		// Clean up
		tRes.destroy();
		tSimRes.destroy();
		tResList.destroy();
	} // compute_gross/1.


	// *****************************************************************************
	void compute_net(Table argt,Table returnt) throws OException
	{
		Table    tClientData, tData, tDaily;
		int         iLoop, iLine, iRow;


		// Get and expand the client data
		tClientData = argt.getTable( "client_trade_data", 1);
		tData = Table.tableNew("FlatCashFlows");
		for(iLoop = tClientData.getNumRows(); iLoop > 0; iLoop--)
		{
			tData.select( tClientData.getTable( "cash_flows", iLoop), "*", "deal_num GT 0");
		} // for iLoop.

		tData.select( argt.getTable( "deal_table", 1), "exp_line_id", "deal_num EQ $deal_num");

		// Find the daily cash flows
		tDaily = Table.tableNew("DailyCflows");
		tDaily.select( tData, "DISTINCT, exp_line_id, cflow_date", "cflow_date NE -42");
		tDaily.select( tData, "SUM, cflow",
		"exp_line_id EQ $exp_line_id AND cflow_date EQ $cflow_date");

		// Loop on the exposure line ids to compute the highest daily cash flow
		tDaily.group( "exp_line_id, cflow");
		for(iLoop = returnt.getNumRows(); iLoop > 0; iLoop--)
		{
			iLine = returnt.getInt( "exp_line_id", iLoop);
			iRow = tDaily.findInt( "exp_line_id", iLine, SEARCH_ENUM.LAST_IN_GROUP);

			if(iRow > 0)
				returnt.setDouble( "usage", iLoop,
						tDaily.getDouble( "cflow", iRow));
		} // for iLoop.

		tDaily.destroy();
		tData.destroy();
	} // compute_net/0.


	// *****************************************************************************
	void format_report(Table returnt) throws OException
	{
		returnt.defaultFormat();
		returnt.viewTable();
	} // format_report/0.




}
