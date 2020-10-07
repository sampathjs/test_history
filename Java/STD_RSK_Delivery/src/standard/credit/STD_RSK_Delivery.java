/*$Header: /cvs/master/olf/plugins/standard/credit/STD_RSK_Delivery.java,v 1.7 2012/05/16 19:43:04 dzhu Exp $*/

/*
File Name:                      STD_RSK_Delivery.java

Report Name:                    NONE

Output File Name:               NONE

Author:                         Guillaume Cortade / Alex Afraymovich
Creation Date:

Revision History:               Mar 29, 2006 - Alex A. made this a standard script
                                Dec 12, 2003 - Guillaume C.

Script Type:                    Credit Online limit monitoring deal/update script

Recommended Script Category: Risk Limit

Main Script:                    
Parameter Script:               
Display Script: 

Description:                    Exposure is physically delivered commodity value
                                Dependant on USER_RESULT_STD_PHYSICAL_DELIVERY result

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
public class STD_RSK_Delivery implements IScript {
	private JVS_INC_STD_Online m_INCSTDOnline;
	private JVS_INC_STD_Simulation m_INCSTDSimulation;
	
	private JVS_INC_Standard m_INCStandard;
	private String error_log_file;
	
	public STD_RSK_Delivery(){
		m_INCSTDOnline = new JVS_INC_STD_Online();
		m_INCSTDSimulation = new JVS_INC_STD_Simulation();
		m_INCStandard = new JVS_INC_Standard();
	}

	// *****************************************************************************
	public void execute(IContainerContext context) throws OException
	{
		
		String sFileName = "STD_RSK_Delivery";
		error_log_file = Util.errorInitScriptErrorLog(Util.getEnv("AB_OUTDIR") + "\\error_logs\\" + sFileName);
		
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();

		m_INCStandard.Print(error_log_file, "START","\n*** Script STD_RSK_Delivery.java Starts ***\n");
		// Initialize the online script
		m_INCSTDOnline.ONL_Init(argt, returnt);

		// Call the virtual functions according to action type
		if (m_INCSTDOnline.gRunType == m_INCSTDOnline.cRunPreCheck
				|| m_INCSTDOnline.gRunType == m_INCSTDOnline.cRunDeal
				|| m_INCSTDOnline.gRunType == m_INCSTDOnline.cRunBatch)
		{
			compute_gross(returnt, argt);
			m_INCSTDOnline.ONL_AssignBuckets(m_INCSTDOnline.cBucketUpToMaturity, argt, returnt);
		}
		else if (m_INCSTDOnline.gRunType == m_INCSTDOnline.cRunBatchUpdate
				|| m_INCSTDOnline.gRunType == m_INCSTDOnline.cRunDealUpdate)
		{
			compute_net(argt, returnt);
		}
		else if(m_INCSTDOnline.gRunType == m_INCSTDOnline.cRunAdhoc)
		{
			compute_gross(returnt, argt);
			format_report(returnt); 
		}
		else
		{
			m_INCSTDOnline.ONL_Fail("Incorrect operation code", returnt);

		}

		m_INCStandard.Print(error_log_file, "END","\n*** Script STD_RSK_Delivery.java Ends ***\n");

		m_INCSTDOnline.ONL_Succeed(returnt);
	} // main/0.


	// *****************************************************************************
	void compute_gross(Table returnt, Table argt) throws OException
	{
		Table tResList, tSimRes, tRes;

		try
		{
			// Get the simulation results
			tResList = Sim.createResultListForSim();
			SimResult.addResultForSim(tResList, SimResultType.create("USER_RESULT_STD_PHYSICAL_DELIVERY"));
			tSimRes = m_INCSTDSimulation.SIM_ComputeResults(tResList, argt);
			tRes = SimResult.getTranLegResults(tSimRes);

			// Use the Mark to Market as exposure
			returnt.select( tRes, "SUM, #USER_RESULT_STD_PHYSICAL_DELIVERY(phys_val)",
			"deal_num EQ $deal_num");
			returnt.mathAddColConst( "phys_val", 0.0, "deal_exposure");

			tSimRes.destroy();
			tResList.destroy();
		}catch(OException oe)
		{
			oe.printStackTrace();
		}
	} // compute_gross/1.


	// *****************************************************************************
	void compute_net(Table argt,Table returnt) throws OException
	{
		Table    tClientData;
		Table    tTranInfo;

		// Get the client data and net the exposure
		tClientData = argt.getTable( "client_trade_data", 1);
		tClientData.addCol( "usage", COL_TYPE_ENUM.COL_DOUBLE);
		m_INCSTDOnline.ONL_NetExposure(tClientData, "phys_val");

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
		m_INCSTDOnline.ONL_NetExposure(returnt, "phys_val");

		returnt.defaultFormat();
		returnt.viewTable();
	} // format_report/0.
}
