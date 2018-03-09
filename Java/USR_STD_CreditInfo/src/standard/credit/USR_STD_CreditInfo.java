   /*$Header: /cvs/master/olf/plugins/standard/credit/USR_STD_CreditInfo.java,v 1.9 2012/06/26 15:08:55 chrish Exp $*/

/*
File Name:                      USR_STD_CreditInfo.java

Report Name:                    NONE

Output File Name:               NONE

Author:                         Guillaume Cortade / Alex Afraymovich
Creation Date:

Revision History: 				Sep 28, 2011 - DTS86132 - BLAST:- V10_2_1_09202011 :- System could not compile and import the standard scripts in Credit folder
								Jan 21, 2011 - DTS 61577; Change locale on date format to DATE_LOCALE_DEFAULT
				Feb 23, 2010 - DTS 59135; Replace TRAN_Get, TRAN_Set with their TRANF_GetField/TRANF_SetField Counterparts
                                Mar 29, 2006 - Alex A. made this a standard script
                                Nov 07, 2003 - Guillaume C.

Script Type:                    User-defined simulation result

Recommended Script Category: Simulation Result

Main Script:
Parameter Script:
Display Script:

Description:                    Agreement, party and issuer information general result

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
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class USR_STD_CreditInfo implements IScript {
private JVS_INC_STD_UserSimRes m_INCSTDUserSimRes;
 public USR_STD_CreditInfo(){
	m_INCSTDUserSimRes = new JVS_INC_STD_UserSimRes();

 }



// *****************************************************************************
public void execute(IContainerContext context) throws OException
{
Table argt = context.getArgumentsTable();
Table returnt = context.getReturnTable();

   // Call the virtual functions according to action type
   int tmp = m_INCSTDUserSimRes.USR_RunMode(argt);
   if( tmp == USER_RESULT_OPERATIONS.USER_RES_OP_CALCULATE.toInt())
   {
         compute_result(argt, returnt);
   }
   else if(tmp == USER_RESULT_OPERATIONS.USER_RES_OP_FORMAT.toInt())
   {
         format_result(returnt);
   }
   else if(tmp == USER_RESULT_OPERATIONS.USER_RES_OP_DWEXTRACT.toInt())
   {
         //m_INCSTDUserSimRes.USR_DefaultExtract(argt, returnt);
		 dw_extract_result(argt, returnt);
   }
   else
   {
       m_INCSTDUserSimRes.USR_Fail("Incorrect operation code", argt);
    }

   return;
   } // main/0.


// *****************************************************************************
void compute_result(Table argt, Table returnt) throws OException
   {
   int            iLoop, iDeal, iRow, iVal, iMaturity, iLeg;
   Table       tTrans;
   Transaction   pTran;


   // Prepare for computation
   tTrans = m_INCSTDUserSimRes.USR_Transactions(argt);
   returnt.addCols( "I(tran_num) I(deal_num) I(counterparty) I(issuer)");
   returnt.addCols( "I(agreement) I(toolset) I(ins_type) I(maturity)");
   returnt.addCols( "I(asset_type)");

   // Loop on the transactions
   for(iLoop = tTrans.getNumRows(); iLoop > 0; iLoop--)
      {
      pTran = tTrans.getTran( "tran_ptr", iLoop);
      iDeal = tTrans.getInt( "deal_num", iLoop);

      // Save the result information
      iRow = returnt.addRow();
      returnt.setInt( "deal_num", iRow, iDeal);
      iDeal = tTrans.getInt( "tran_num", iLoop);
      returnt.setInt( "tran_num", iRow, iDeal);
      iVal = pTran.getFieldInt( TRANF_FIELD.TRANF_EXTERNAL_LENTITY.toInt(), 0, "");
      returnt.setInt( "counterparty", iRow, iVal);
      iVal = pTran.getFieldInt( TRANF_FIELD.TRANF_ISSUER_LE.toInt(), 0, "");
      returnt.setInt( "issuer", iRow, iVal);
      iVal = pTran.getFieldInt( TRANF_FIELD.TRANF_AGREEMENT.toInt(), 0, "");
      returnt.setInt( "agreement", iRow, iVal);
      iVal = pTran.getFieldInt( TRANF_FIELD.TRANF_TOOLSET_ID.toInt(), 0, "");
      returnt.setInt( "toolset", iRow, iVal);
      iVal = pTran.getFieldInt( TRANF_FIELD.TRANF_INS_TYPE.toInt(), 0, "");
      returnt.setInt( "ins_type", iRow, iVal);
      iVal = pTran.getFieldInt( TRANF_FIELD.TRANF_ASSET_TYPE.toInt(), 0, "");
      returnt.setInt( "asset_type", iRow, iVal);

      //DTS 59135
      //iMaturity = pTran.getMaturityDate();
      //for(iLeg = pTran.getNumParams() - 1; iLeg > -1; iLeg--)
      // Loop on the transaction legs for last end date
      iMaturity = pTran.getFieldInt( TRANF_FIELD.TRANF_MAT_DATE.toInt(), 0, "");
      for(iLeg = pTran.getNumParams() - 1; iLeg > 0; iLeg--)
         {
         iVal = pTran.getFieldInt( TRANF_FIELD.TRANF_MAT_DATE.toInt(), iLeg, "");
         if(iVal > iMaturity)
            iMaturity = iVal;
         } // for iLeg.

      // Save the last maturity date
      returnt.setInt( "maturity", iRow, iMaturity);
      } // for iLoop.

   tTrans.destroy();
   } // compute_result/0.


// *****************************************************************************
void format_result(Table returnt) throws OException
   {
   // Set viewer column titles
   returnt.setColTitle( "tran_num",     "Transaction\nNumber");
   returnt.setColTitle( "deal_num",     "Deal\nNumber");
   returnt.setColTitle( "counterparty", "Counterparty");
   returnt.setColTitle( "issuer",       "Issuer");
   returnt.setColTitle( "agreement",    "Netting\nAgreement");
   returnt.setColTitle( "toolset",      "Toolset");
   returnt.setColTitle( "ins_type",     "Instrument\nType");
   returnt.setColTitle( "maturity",     "Maturity\nDate");
   returnt.setColTitle( "asset_type",   "Asset\nType");

   // Format ids to names and sort
   returnt.setColFormatAsRef( "agreement",    SHM_USR_TABLES_ENUM.MASTER_AGREEMENT_TABLE);
   returnt.setColFormatAsRef( "counterparty", SHM_USR_TABLES_ENUM.PARTY_TABLE);
   returnt.setColFormatAsRef( "issuer",       SHM_USR_TABLES_ENUM.PARTY_TABLE);
   returnt.setColFormatAsRef( "toolset",      SHM_USR_TABLES_ENUM.TOOLSETS_TABLE);
   returnt.setColFormatAsRef( "ins_type",     SHM_USR_TABLES_ENUM.INS_TYPE_TABLE);
   returnt.setColFormatAsRef( "asset_type",   SHM_USR_TABLES_ENUM.ASSET_TYPE_TABLE);

   returnt.setColFormatAsDate( "maturity", DATE_FORMAT.DATE_FORMAT_IMM, DATE_LOCALE.DATE_LOCALE_DEFAULT);
   returnt.group( "counterparty, agreement, deal_num, tran_num");
   } // format_result/0.

void dw_extract_result(Table argt, Table returnt) throws OException
{
}


}
