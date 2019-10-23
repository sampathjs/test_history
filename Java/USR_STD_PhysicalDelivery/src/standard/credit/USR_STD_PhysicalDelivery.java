   /*$Header: /cvs/master/olf/plugins/standard/credit/USR_STD_PhysicalDelivery.java,v 1.6.296.1 2015/07/29 12:51:17 chrish Exp $*/

/*
File Name:                      USR_STD_PhysicalDelivery.java
 
Report Name:                    NONE
 
Output File Name:               NONE
 
Author:                         Guillaume Cortade / Alex Afraymovich
Creation Date:
 
Revision History:               Mar 29, 2006 - Alex A. made this a standard script
                                Nov 19, 2003 - Guillaume C.
                                                
Script Type:                    User-defined simulation result

Recommended Script Category: Simulation Result

Main Script:                    
Parameter Script:               
Display Script: 
 
Description:                    Flowed but unpaid physical amount 'by leg' result
                                Dependant on USER_RESULT_STD_FX_CONVERSION and the by leg results
				PERIOD_START_DATE, PAYMENT_DATE, PRICE_PRIOR and VOLUME_PRIOR
Assumptions:                    
 
Instructions:                                          
  
Uses EOD Results?
 
Which EOD Results are used?
 
When can the script be run?  
*/ 

package standard.credit;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.JVS_INC_STD_UserSimRes;
@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
public class USR_STD_PhysicalDelivery implements IScript {
private JVS_INC_STD_UserSimRes m_INCSTDUserSimRes;
 public USR_STD_PhysicalDelivery(){
	m_INCSTDUserSimRes = new JVS_INC_STD_UserSimRes();

 }



// *****************************************************************************
public void execute(IContainerContext context) throws OException
{
Table argt = context.getArgumentsTable();
Table returnt = context.getReturnTable();

   if(m_INCSTDUserSimRes.USR_RunMode(argt) == USER_RESULT_OPERATIONS.USER_RES_OP_CALCULATE.toInt())
      compute_result(argt, returnt);

   return;
   } // main/0.


// *****************************************************************************
void compute_result(Table argt, Table returnt) throws OException
   {
   Table tBaseFX, tLegs;
   String   sPx, sVol, sStart, sPay;
   int      iToday, iLoop, iDeal, iLeg, iPdc, iBegin, iEnd;


   // Get the pre-computed results
   tBaseFX = m_INCSTDUserSimRes.USR_GetRes(SimResultType.create("USER_RESULT_STD_FX_CONVERSION").getId(), argt);
   tLegs   = m_INCSTDUserSimRes.USR_GetRes(PFOLIO_RESULT_TYPE.VOLUME_PRIOR_BY_LEG_RESULT.toInt(), argt);
   sPx    = SimResult.resultColName(PFOLIO_RESULT_TYPE.PRICE_PRIOR_BY_LEG_RESULT.toInt());
   sVol   = SimResult.resultColName(PFOLIO_RESULT_TYPE.VOLUME_PRIOR_BY_LEG_RESULT.toInt());
   sStart = SimResult.resultColName(PFOLIO_RESULT_TYPE.PERIOD_START_DATE_BY_LEG_RESULT.toInt());
   sPay   = SimResult.resultColName(PFOLIO_RESULT_TYPE.PAYMENT_DATE_BY_LEG_RESULT.toInt());

   // Convert price to reporting currency and full volume
   tLegs.select( tBaseFX, "scen_fx_conv",
                "deal_num EQ $deal_num AND deal_leg EQ $deal_leg");
   tLegs.mathDivCol( sPx, "scen_fx_conv", sPx);
   tLegs.mathMultCol( sPx, sVol, sPx);

   // Loop on all deal, legs and periods to save results
   iToday = OCalendar.today();
   for(iLoop = tLegs.getNumRows(); iLoop > 0; iLoop--)
      {
      iDeal  = tLegs.getInt( "deal_num", iLoop);
      iLeg   = tLegs.getInt( "deal_leg", iLoop);
      iPdc   = tLegs.getInt( "deal_pdc", iLoop);
      iBegin = com.olf.openjvs.Math.doubleToInt(tLegs.getDouble( sStart, iLoop));
      iEnd   = com.olf.openjvs.Math.doubleToInt(tLegs.getDouble( sPay, iLoop));

      // Save only the volumes delivered or partly delivered but unpaid
      if(iBegin < iToday && iEnd >= iToday)
         m_INCSTDUserSimRes.USR_SetValue(tLegs.getDouble( sPx, iLoop), iDeal, iLeg, iPdc, argt, returnt);
      else
         m_INCSTDUserSimRes.USR_SetValue(0.0, iDeal, iLeg, iPdc, argt, returnt);
      } // for iLoop.

   tLegs.destroy();
   tBaseFX.destroy();
   } // compute_result/0.




}
