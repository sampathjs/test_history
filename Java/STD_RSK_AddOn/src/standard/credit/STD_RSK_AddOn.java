   /*$Header: /cvs/master/olf/plugins/standard/credit/STD_RSK_AddOn.java,v 1.4 2012/05/16 19:42:09 dzhu Exp $*/

/*
File Name:                      STD_RSK_AddOn.java
 
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
 
Description:                    Exposure is MtM + AddOn * Notional
                                AddOn depending on deal type and residual maturity
				Dependant on USER_RESULT_STD_CREDIT_ADDON and PFOLIO_RESULT_TYPE.CURRENT_NOTIONAL_RESULT

				USER_STD_Credit_Addons USER table is needed for the add-ons
				see USR_STD_CreditAddOn script for format

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
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class STD_RSK_AddOn implements IScript {
private JVS_INC_STD_Online m_INCSTDOnline;
private JVS_INC_STD_Simulation STD_RSK_AddOn;
 public STD_RSK_AddOn(){
	m_INCSTDOnline = new JVS_INC_STD_Online();
        STD_RSK_AddOn= new JVS_INC_STD_Simulation();
 }


// *****************************************************************************
public void execute(IContainerContext context) throws OException
{
Table argt = context.getArgumentsTable();
Table returnt = context.getReturnTable();

   // Initialize the online script
   m_INCSTDOnline.ONL_Init(argt,returnt);
   
   // Call the virtual functions according to action type
   if(m_INCSTDOnline.gRunType==m_INCSTDOnline.cRunPreCheck || m_INCSTDOnline.gRunType==m_INCSTDOnline.cRunDeal || m_INCSTDOnline.gRunType==m_INCSTDOnline.cRunBatch)
   {
         compute_gross(argt,returnt);
         m_INCSTDOnline.ONL_AssignBuckets(m_INCSTDOnline.cBucketUpToMaturity,argt,returnt);
         m_INCSTDOnline.ONL_AssignCollateral(returnt);
   }//if.
   else if(m_INCSTDOnline.gRunType==m_INCSTDOnline.cRunBatchUpdate||m_INCSTDOnline.gRunType==m_INCSTDOnline.cRunDealUpdate)
   {
       compute_net(argt,returnt);
   }//if.
   else if(m_INCSTDOnline.gRunType==m_INCSTDOnline.cRunAdhoc)
   {
         compute_gross(argt,returnt);
         format_report(returnt);
   }//if.
   else{
       m_INCSTDOnline.ONL_Fail("Incorrect operation code",returnt);
      }
   m_INCSTDOnline.ONL_Succeed(returnt);
   } // main/0.


// *****************************************************************************
void compute_gross(Table argt, Table returnt) throws OException
   {
   Table tResList;
   Table tSimRes, tRes;
   SimResultType  userTypeAddOn;
   int simId = 0;
   
   // Get the simulation results
   tResList = Sim.createResultListForSim();

   userTypeAddOn = SimResultType.create("USER_RESULT_STD_CREDIT_ADDON");
   SimResult.addResultForSim(tResList,SimResultType.create("PFOLIO_RESULT_TYPE.CURRENT_NOTIONAL_RESULT"));
   SimResult.addResultForSim(tResList, userTypeAddOn);
   tSimRes = STD_RSK_AddOn.SIM_ComputeResults(tResList,argt);
   
   simId = userTypeAddOn.getId();
   tRes = STD_RSK_AddOn.SIM_GetResult(tSimRes, simId);

   // Use the Mark to Market as exposure
   returnt.select( tRes, "gross_exp", "deal_num EQ $deal_num");
   returnt.mathAddColConst( "gross_exp", 0.0, "deal_exposure");

   tRes.destroy();
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
   m_INCSTDOnline.ONL_NetExposure(tClientData, "gross_exp");

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
   m_INCSTDOnline.ONL_NetExposure(returnt, "gross_exp");
   m_INCSTDOnline.ONL_AssignCollateral(returnt);

   returnt.defaultFormat();
   returnt.viewTable();
   } // format_report/0.




}
