  /*$Header: /cvs/master/olf/plugins/standard/credit/STD_RSK_VAR.java,v 1.6 2012/05/16 19:43:50 dzhu Exp $*/

/*
File Name:                      STD_RSK_VAR.java
 
Report Name:                    NONE
 
Output File Name:               NONE
 
Author:                         Guillaume Cortade / Alex Afraymovich
Creation Date:
 
Revision History:               Mar 29, 2006 - Alex A. made this a standard script
                                Dec 08, 2003 - Guillaume C.
                                                
Script Type:                    Risk Online limit monitoring deal/update script

Recommended Script Category: Risk Limit

Main Script:                    
Parameter Script:               
Display Script: 
 
Description:                    Exposure is gross Value at Risk (no netting)
                                Dependant on PFOLIO_RESULT_TYPE.VAR_CORRELATION_MATRIX_RESULT, PFOLIO_RESULT_TYPE.VAR_RAW_GPT_INFO_RESULT, 
				PFOLIO_RESULT_TYPE.TRAN_GPT_DELTA_RESULT, PFOLIO_RESULT_TYPE.VAR_BY_TRAN_RESULT

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

public class STD_RSK_VAR implements IScript {
private JVS_INC_STD_Online m_INCSTDOnline;
private JVS_INC_STD_Simulation m_JVS_INC_STD_Simulation;
 public STD_RSK_VAR(){
	m_INCSTDOnline = new JVS_INC_STD_Online();
        m_JVS_INC_STD_Simulation = new JVS_INC_STD_Simulation();

 }
 


// *****************************************************************************
// EDIT THIS CONSTANT WITH VALID SAVED VAR DEFINITION NAME
String      gVaRDefinition = "VaRDef_Test";
// *****************************************************************************


// *****************************************************************************
public void execute(IContainerContext context) throws OException
{
Table argt = context.getArgumentsTable();
Table returnt = context.getReturnTable();

   // Initialize the online script
   m_INCSTDOnline.ONL_Init(argt, returnt);

   // Call the virtual functions according to action type
   if(m_INCSTDOnline.gRunType == m_INCSTDOnline.cRunPreCheck 
           || m_INCSTDOnline.gRunType == m_INCSTDOnline.cRunDeal
           || m_INCSTDOnline.gRunType == m_INCSTDOnline.cRunBatch)
   {
         compute_gross(argt, returnt);
         m_INCSTDOnline.ONL_AssignBuckets(m_INCSTDOnline.cBucketUpToMaturity, argt, returnt);
         m_INCSTDOnline.ONL_AssignCollateral(returnt);
   }
   else if(m_INCSTDOnline.gRunType == m_INCSTDOnline.cRunBatchUpdate
           || m_INCSTDOnline.gRunType == m_INCSTDOnline.cRunDealUpdate)
   {
       compute_net(argt, returnt);
   }
   else if(m_INCSTDOnline.gRunType == m_INCSTDOnline.cRunAdhoc)
   {
         compute_gross(argt, returnt);
         format_report(returnt);
   }
   else
   {
       m_INCSTDOnline.ONL_Fail("Incorrect operation code", returnt);
   }
   

   m_INCSTDOnline.ONL_Succeed(returnt);
   } // main/0.


// *****************************************************************************
void compute_gross(Table argt, Table returnt) throws OException
   {
   Table tSimDef, tSimRes, tRes, tClient;
   int      iLoop, iDeal;

   // Create and run the VaR simulation
   tSimDef = m_JVS_INC_STD_Simulation.SIM_NewVaRSim(gVaRDefinition, m_JVS_INC_STD_Simulation.cCornFishDG, 0.95);
//   tSimDef = SIM_NewVaRSim(gVaRDefinition, cMCDeltaApp, 0.95);
   tSimRes =  m_JVS_INC_STD_Simulation.SIM_RunSim(argt,tSimDef);
   tSimDef.destroy();

   // Store the parameters on batch runs
   if(m_INCSTDOnline.gRunType == m_INCSTDOnline.cRunBatch)
      {
      tRes = m_JVS_INC_STD_Simulation.SIM_GetResult(tSimRes, PFOLIO_RESULT_TYPE.VAR_CORRELATION_MATRIX_RESULT.toInt());
      m_INCSTDOnline.ONL_SetUserData(tRes, PFOLIO_RESULT_TYPE.VAR_CORRELATION_MATRIX_RESULT.toInt(), argt);
      tRes.destroy();

      tRes = m_JVS_INC_STD_Simulation.SIM_GetResult(tSimRes, PFOLIO_RESULT_TYPE.VAR_RAW_GPT_INFO_RESULT.toInt());
      m_INCSTDOnline.ONL_SetUserData(tRes, PFOLIO_RESULT_TYPE.VAR_RAW_GPT_INFO_RESULT.toInt(), argt);
      tRes.destroy();
      } // if.

   // Add the tran gpt delta as client data
   returnt.addCol( "deal_delta", COL_TYPE_ENUM.COL_TABLE);
   tRes = m_JVS_INC_STD_Simulation.SIM_GetResult(tSimRes, PFOLIO_RESULT_TYPE.TRAN_GPT_DELTA_RESULT.toInt());
//   tRes.select( returnt, "exp_line_id", "deal_num EQ $deal_num");
   for(iLoop = returnt.getNumRows(); iLoop > 0; iLoop--)
      {
      // Create the delta table for the transaction
      iDeal = returnt.getInt( "deal_num", iLoop);
      tClient = Table.tableNew("ClientData" + iDeal);

      // Save the delta results
      tClient.select( tRes, "*", "deal_num EQ " + iDeal);
      returnt.setTable( "deal_delta", iLoop, tClient);
      } // for iLoop.
   tRes.destroy();

   // Use the VaR by trans as exposure
   tRes = m_JVS_INC_STD_Simulation.SIM_GetResult(tSimRes, PFOLIO_RESULT_TYPE.VAR_BY_TRAN_RESULT.toInt());
   returnt.select( tRes, "result(var)", "id EQ $deal_num");
   returnt.mathAddColConst( "var", 0.0, "deal_exposure");

   tRes.destroy();
   tSimRes.destroy();
   } // compute_gross/1.


// *****************************************************************************
void compute_net(Table argt, Table returnt) throws OException
   {
   Table    tClientData, tData, tDelta, tRaw, tCor;
   int         iLoop, iLine;
   double      dVaR;


   // Get and expand the client data
   tClientData = argt.getTable( "client_trade_data", 1);
   tData = Table.tableNew("FlatDelta");
   for(iLoop = tClientData.getNumRows(); iLoop > 0; iLoop--)
      {
      tDelta = tClientData.getTable( "deal_delta", iLoop);
      tData.select( tDelta, "*", "deal_num GT 0");
      } // for iLoop.


   // Get the updated summary and the VaR parameters
   tData.select( argt.getTable( "deal_table", 1), "exp_line_id", "deal_num EQ $deal_num");
   tDelta = m_INCSTDOnline.ONL_Summarize(tData, "index, gpt_id, exp_line_id", "delta, gamma", argt);
   tRaw = m_INCSTDOnline.ONL_GetUserData(PFOLIO_RESULT_TYPE.VAR_RAW_GPT_INFO_RESULT.toInt(), argt);
   tCor = m_INCSTDOnline.ONL_GetUserData(PFOLIO_RESULT_TYPE.VAR_CORRELATION_MATRIX_RESULT.toInt(), argt);

   // Loop on the exposure line ids to compute the VaR
   for(iLoop = returnt.getNumRows(); iLoop > 0; iLoop--)
      {
      iLine = returnt.getInt( "exp_line_id", iLoop);

      // Update the deltas with summary values
      tRaw.setColValDouble( "gpt_delta", 0.0);
      tRaw.setColValDouble( "gpt_gamma", 0.0);
      tRaw.select( tDelta, "SUM, delta(gpt_delta), gamma(gpt_gamma)",
                   "index EQ $index_id AND gpt_id EQ $gpt_id AND exp_line_id EQ " + iLine);

      dVaR = ValueAtRisk.computeVaR(tCor, tRaw, 1.0, 1.0, VAR_METHODS.VAR_MTHD_CF_DELTA_GAMMA.toInt(), .95);
      returnt.setDouble( "usage", iLoop, dVaR);
      } // for iLoop.

   tCor.destroy();
   tRaw.destroy();
   tDelta.destroy();
   tData.destroy();
   } // compute_net/0.


// *****************************************************************************
void format_report(Table returnt) throws OException
   {
   returnt.defaultFormat();

   returnt.viewTable();
   } // format_report/0.




}
