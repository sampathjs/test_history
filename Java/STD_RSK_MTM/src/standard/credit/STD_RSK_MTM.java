   /*$Header: /cvs/master/olf/plugins/standard/credit/STD_RSK_MTM.java,v 1.5 2012/05/16 19:43:16 dzhu Exp $*/

/*
File Name:                      STD_RSK_MTM.java
 
Report Name:                    NONE
 
Output File Name:               NONE
 
Author:                         Guillaume Cortade / Alex Afraymovich
Creation Date:
 
Revision History:               Dec 02, 2010 - Replaced calls to the OpenJVS String library with calls to the Java String library
                                Mar 29, 2006 - Alex A. made this a standard script
                                Dec 02, 2003 - Guillaume C.
                                                
Script Type:                    Credit Online limit monitoring deal/update script

Recommended Script Category: Risk Limit

Main Script:                    
Parameter Script:               
Display Script: 
 
Description:                    Exposure is netted MtM
                                Dependant on PFOLIO_RESULT_TYPE.BASE_PV_RESULT result
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

public class STD_RSK_MTM implements IScript {
private JVS_INC_STD_Online m_INCSTDOnline;
private JVS_INC_STD_Simulation m_INCSTDSimulation;
 public STD_RSK_MTM(){
	m_INCSTDOnline = new JVS_INC_STD_Online();
        m_INCSTDSimulation=new JVS_INC_STD_Simulation();
 }




// *****************************************************************************

  
 public void execute(IContainerContext context) throws OException
{
Table argt = context.getArgumentsTable();
Table returnt = context.getReturnTable();

   // Initialize the online script
   m_INCSTDOnline.ONL_Init(argt,  returnt);

   // Call the virtual functions according to action type
   if((m_INCSTDOnline.gRunType==m_INCSTDOnline.cRunPreCheck) ||(m_INCSTDOnline.gRunType==m_INCSTDOnline.cRunDeal)||
     m_INCSTDOnline.gRunType==m_INCSTDOnline.cRunBatch)
   {
         compute_gross(argt,returnt);
         m_INCSTDOnline.ONL_AssignBuckets(m_INCSTDOnline.cBucketExact, argt,returnt);
//         m_INCSTDOnline.ONL_AssignBuckets(m_INCSTDOnline.cBucketUpToMaturity);
         m_INCSTDOnline.ONL_AssignCollateral(returnt);
    
   }else if (m_INCSTDOnline.gRunType==m_INCSTDOnline.cRunBatchUpdate|| m_INCSTDOnline.gRunType==m_INCSTDOnline.cRunDealUpdate)
   {
         compute_net(argt,returnt);
    
   }   else if(m_INCSTDOnline.gRunType==m_INCSTDOnline.cRunAdhoc)   
   {
         compute_gross(argt,returnt);
         format_report(returnt);
         
   }else {
      
         m_INCSTDOnline.ONL_Fail("Incorrect operation code",returnt);
      } // switch.

   m_INCSTDOnline.ONL_Succeed(returnt);
   } // main/0.


// *****************************************************************************
void compute_gross(Table argt,Table returnt) throws OException
   {
   Table tResList, tSimRes=Util.NULL_TABLE, tRes;

   // Get the simulation results
   tResList = Sim.createResultListForSim();
   SimResult.addResultForSim(tResList, SimResultType.create("PFOLIO_RESULT_TYPE.BASE_PV_RESULT"));

  
   //tSimRes = SIM_ComputeResults(tResList);
   tSimRes = m_INCSTDSimulation.SIM_ComputeResults(tResList,argt);
   tRes = SimResult.getTranResults(tSimRes);

   // Use the Mark to Market as exposure
   returnt.select( tRes, "SUM, " + PFOLIO_RESULT_TYPE.BASE_PV_RESULT.toInt() + "(pv)", "deal_num EQ $deal_num");
   returnt.mathAddColConst( "pv", 0.0, "deal_exposure");
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
   m_INCSTDOnline.ONL_NetExposure(tClientData, "pv");

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
    
   m_INCSTDOnline.ONL_NetExposure(returnt, "pv");
   returnt.defaultFormat();

   returnt.viewTable();
   } // format_report/0.

}
