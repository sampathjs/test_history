   /*$Header: /cvs/master/olf/plugins/standard/credit/STD_RSK_Notional.java,v 1.4 2012/05/16 19:43:24 dzhu Exp $*/

/*
File Name:                      STD_RSK_Notional.java
 
Report Name:                    NONE
 
Output File Name:               NONE
 
Author:                         Guillaume Cortade / Alex Afraymovich
Creation Date:
 
Revision History:               Mar 29, 2006 - Alex A. made this a standard script
                                Dec 08, 2003 - Guillaume C.
                                                
Script Type:                    Credit Online limit monitoring deal/update script

Recommended Script Category: Risk Limit

Main Script:                    
Parameter Script:               
Display Script: 
 
Description:                    Exposure is netted Notional
                                Dependant on USER_RESULT_STD_FX_CONVERSION, PFOLIO_RESULT_TYPE.CURRENT_NOTIONAL_RESULT results
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
public class STD_RSK_Notional implements IScript {
private JVS_INC_STD_Online m_INCSTDOnline;
private JVS_INC_STD_Simulation m_INCSTDSimulation;
 public STD_RSK_Notional(){
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
         m_INCSTDOnline.ONL_AssignBuckets(m_INCSTDOnline.cBucketUpToMaturity,argt,returnt);
         m_INCSTDOnline.ONL_AssignCollateral(returnt);
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
void compute_gross(Table argt, Table returnt) throws OException
   {
   Table tResList, tSimRes, tRes, tFXRes;
   String   sTranCol, sNotnlCol;
   int      iCurrentTran, iLoop;
   double   dNotnl;
   SimResultType simType = SimResultType.create("USER_RESULT_STD_FX_CONVERSION");
   int simId = simType.getId();
   // Get the simulation results
   tResList = Sim.createResultListForSim();
   SimResult.addResultForSim(tResList, SimResultType.create("PFOLIO_RESULT_TYPE.CURRENT_NOTIONAL_RESULT"));
   //SimResult.addResultForSim(tResList, SimResultType.create("USER_RESULT_STD_FX_CONVERSION"));
   SimResult.addResultForSim(tResList, simType);

   tSimRes = m_INCSTDSimulation.SIM_ComputeResults(tResList,argt);
   tRes = SimResult.getTranResults(tSimRes);
   tFXRes = m_INCSTDSimulation.SIM_GetResult(tSimRes, simId);

   // Convert the Notional to reporting currency
   tRes.select( tFXRes, "*", "deal_num EQ $deal_num AND deal_leg EQ $deal_leg");
   sNotnlCol = SimResult.resultColName(PFOLIO_RESULT_TYPE.CURRENT_NOTIONAL_RESULT.toInt());
   tRes.addCols( "F(base_notnl) F(abs_notnl)");
   tFXRes.destroy();

   // Do the Notional math
   tRes.mathDivCol( sNotnlCol, "scen_fx_conv", "base_notnl");
   tRes.mathAddColConst( "base_notnl", 0.0, "abs_notnl");
   tRes.mathABSCol( "abs_notnl");
   tRes.mathRoundCol( "abs_notnl", 0);

   // Remove all but the highest absolute value notional for each deal
   tRes.group( "deal_num, abs_notnl, base_notnl");
   iCurrentTran = -42;
   for(iLoop = tRes.getNumRows(); iLoop > 0; iLoop--)
      {
      if(tRes.getInt( "deal_num", iLoop) != iCurrentTran)
         {
         iCurrentTran = tRes.getInt( "deal_num", iLoop);
         continue;
         } // if.

      tRes.setDouble( "base_notnl", iLoop, 0.0);
      } // for iLoop.

   // Use the Notional as exposure
   returnt.select( tRes, "SUM, base_notnl", "deal_num EQ $deal_num");
   returnt.mathAddColConst( "base_notnl", 0.0, "deal_exposure");
   tSimRes.destroy();
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
   m_INCSTDOnline.ONL_NetExposure(tClientData, "base_notnl");

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
   m_INCSTDOnline.ONL_NetExposure(returnt, "notnl");
   returnt.defaultFormat();

   returnt.viewTable();
   } // format_report/0.




}
