  /*$Header: /cvs/master/olf/plugins/standard/credit/USR_STD_CreditAddOn.java,v 1.11 2012/06/26 15:08:55 chrish Exp $*/
/*
File Name:                      USR_STD_CreditAddOn.java

Report Name:                    NONE

Output File Name:               NONE

Author:                         Guillaume Cortade / Alex Afraymovich
Creation Date:

Revision History:				Sep 28, 2011 - DTS86132 - BLAST:- V10_2_1_09202011 :- System could not compile and import the standard scripts in Credit folder
								Jan 21, 2011 - DTS 61577; Change locale on date format to DATE_LOCALE_DEFAULT
				Dec 06, 2010 - Replaced DBaseTable.loadFromDb* with DBaseTable.execISql
                                Mar 29, 2006 - Alex A. made this a standard script
                                Nov 12, 2003 - Guillaume C.

Script Type:                    User-defined simulation result

Recommended Script Category: Simulation Result

Main Script:
Parameter Script:
Display Script:

Description:                    General result simple add-on computation
                                Dependant on USER_RESULT_STD_FX_CONVERSION, PFOLIO_RESULT_TYPE.PV_RESULT,
				PFOLIO_RESULT_TYPE.CURRENT_NOTIONAL_RESULT, USER_RESULT_STD_CREDIT_INFO, PFOLIO_RESULT_TYPE.TRAN_LISTING_RESULT

				USER_STD_Credit_Addons USER table is needed for the add-ons:
				"S(toolset_name) S(date_from) S(date_to) F(addon)"
				Can have "S(ins_name)", rows with ins_name = "" are default for toolset

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
import standard.include.JVS_INC_STD_CreditRisk;
import standard.include.JVS_INC_STD_System;
import standard.include.JVS_INC_Standard;
@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
public class USR_STD_CreditAddOn implements IScript {
private JVS_INC_STD_System m_INCSTDSystem;
private JVS_INC_STD_CreditRisk m_JVS_INC_STD_CreditRisk;
private JVS_INC_STD_UserSimRes m_JVS_INC_STD_UserSimRes;
private JVS_INC_Standard m_INCStandard;
private String error_log_file;

 public USR_STD_CreditAddOn(){
        m_JVS_INC_STD_CreditRisk = new JVS_INC_STD_CreditRisk();
        m_JVS_INC_STD_UserSimRes = new JVS_INC_STD_UserSimRes();
        m_INCSTDSystem = new JVS_INC_STD_System();
        m_INCStandard = new JVS_INC_Standard();
 }



// *****************************************************************************
public void execute(IContainerContext context) throws OException
{

	String sFileName = "USR_STD_CreditAddOn";
	error_log_file = Util.errorInitScriptErrorLog(Util.getEnv("AB_OUTDIR") + "\\error_logs\\" + sFileName);

Table argt = context.getArgumentsTable();
Table returnt = context.getReturnTable();

   // Call the virtual functions according to action type
   if(m_JVS_INC_STD_UserSimRes.USR_RunMode(argt) == USER_RESULT_OPERATIONS.USER_RES_OP_CALCULATE.toInt())
   {
         compute_result(argt, returnt);
   }
   else
   if(m_JVS_INC_STD_UserSimRes.USR_RunMode(argt) == USER_RESULT_OPERATIONS.USER_RES_OP_FORMAT.toInt())
   {
         format_result(returnt);
   }
   else if(m_JVS_INC_STD_UserSimRes.USR_RunMode(argt) ==USER_RESULT_OPERATIONS.USER_RES_OP_DWEXTRACT.toInt())
   {
	     //m_JVS_INC_STD_UserSimRes.USR_DefaultExtract(argt,returnt);
		 dw_extract_result(argt, returnt);
   }
   else
   {
         m_JVS_INC_STD_UserSimRes.USR_Fail("Incorrect operation code", argt);
   }

   return;
   } // main/0.


// *****************************************************************************
void compute_result(Table argt, Table returnt) throws OException
   {
   Table    tTranRes, tFXRes, tCreditRes, tAddOns;
   String      sTranCol, sMtMCol, sNotnlCol, sWhere;
   int         iCurrentTran, iLoop, iParty, iStartRow, iAgreement, iUpdate;
   double      dNotnl = 0, dExposure, dGross, dCurrent;


   // Get the precomputed results
   tTranRes   = m_JVS_INC_STD_UserSimRes.USR_GetRes(PFOLIO_RESULT_TYPE.TRAN_LISTING_RESULT.toInt(), argt);
   tFXRes     = m_JVS_INC_STD_UserSimRes.USR_GetRes(SimResultType.create("USER_RESULT_STD_FX_CONVERSION").getId(), argt);
   tCreditRes = m_JVS_INC_STD_UserSimRes.USR_GetRes(SimResultType.create("USER_RESULT_STD_CREDIT_INFO").getId(), argt);

   // Transform the transaction result to integer numbers
   sTranCol  = SimResult.resultColName(PFOLIO_RESULT_TYPE.TRAN_LISTING_RESULT.toInt());
   tTranRes.addCols( "I(tran_num) F(base_pv) F(base_notnl)");
   tTranRes.mathAddColConst( sTranCol, 0.0, "tran_num");

   // Convert the MtM and Notional to reporting currency
   tTranRes.select( tFXRes, "*",
                "tran_num EQ $tran_num AND deal_leg EQ $deal_leg");
   sMtMCol   = SimResult.resultColName(PFOLIO_RESULT_TYPE.PV_RESULT.toInt());
   sNotnlCol = SimResult.resultColName(PFOLIO_RESULT_TYPE.CURRENT_NOTIONAL_RESULT.toInt());
   tTranRes.mathDivCol( sMtMCol,   "scen_fx_conv", "base_pv");
   tTranRes.mathDivCol( sNotnlCol, "scen_fx_conv", "base_notnl");

   // Keep only the highest absolute value notional for each deal
   tTranRes.mathABSCol( "base_notnl");
   iCurrentTran = -42;
   for(iLoop = tTranRes.getNumRows(); iLoop > 0; iLoop--)
      {
      if(tTranRes.getInt( "tran_num", iLoop) != iCurrentTran)
         {
         iCurrentTran = tTranRes.getInt( "tran_num", iLoop);
         dNotnl = tTranRes.getDouble( "base_notnl", iLoop);
         }
      else
         {
         tTranRes.setDouble( "base_notnl", iLoop + 1, 0.0);

         if(tTranRes.getDouble( "base_notnl", iLoop) < dNotnl)
            tTranRes.setDouble( "base_notnl", iLoop, dNotnl);
         else
            dNotnl = tTranRes.getDouble( "base_notnl", iLoop);
         } // if.
      } // for iLoop.

   // Gather the results together
   returnt.select( tCreditRes, "*", "tran_num NE -42");
   returnt.select( tTranRes, "SUM, base_pv, base_notnl",
                "tran_num EQ $tran_num");

   // Set the parameters
   tAddOns = GetAddOnTable();
   sWhere = " AND start LT $maturity AND end GE $maturity";
   returnt.select( tAddOns, "addon",
                "toolset EQ $toolset AND ins_type EQ 0" + sWhere);
   returnt.select( tAddOns, "addon",
                "ins_type EQ $ins_type" + sWhere);

   // Compute gross and net exposure
   returnt.addCols( "F(gross_exp) F(net_exp)");
   returnt.mathMultCol( "base_notnl", "addon", "gross_exp");
   returnt.mathAddCol( "base_pv", "gross_exp", "gross_exp");
   m_JVS_INC_STD_CreditRisk.CRD_NetExposure(returnt, "counterparty", "agreement", "gross_exp", "net_exp");

   // Clean up
   tAddOns.destroy();
   tCreditRes.destroy();
   tFXRes.destroy();
   tTranRes.destroy();
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
   returnt.setColTitle( "base_pv",      "Base\nMtM");
   returnt.setColTitle( "base_notnl",   "Base\nNotional");
   returnt.setColTitle( "addon",        "Add On\nFactor");
   returnt.setColTitle( "gross_exp",    "Gross\nExposure");
   returnt.setColTitle( "net_exp",      "Net\nExposure");

   // Format ids to names and sort
   returnt.setColFormatAsRef( "agreement",    SHM_USR_TABLES_ENUM.MASTER_AGREEMENT_TABLE);
   returnt.setColFormatAsRef( "counterparty", SHM_USR_TABLES_ENUM.PARTY_TABLE);
   returnt.setColFormatAsRef( "issuer",       SHM_USR_TABLES_ENUM.PARTY_TABLE);
   returnt.setColFormatAsRef( "toolset",      SHM_USR_TABLES_ENUM.TOOLSETS_TABLE);
   returnt.setColFormatAsRef( "ins_type",     SHM_USR_TABLES_ENUM.INS_TYPE_TABLE);
   returnt.setColFormatAsDate( "maturity", DATE_FORMAT.DATE_FORMAT_MINIMAL, DATE_LOCALE.DATE_LOCALE_DEFAULT);
   returnt.setColFormatAsNotnl( "base_pv",    12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
   returnt.setColFormatAsNotnl( "base_notnl", 12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
   returnt.setColFormatAsNotnl( "addon",      12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
   returnt.setColFormatAsNotnl( "gross_exp",  12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
   returnt.setColFormatAsNotnl( "net_exp",    12, 2, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());

   returnt.group( "counterparty, agreement, deal_num, tran_num");
   } // format_result/0.


// *****************************************************************************
Table GetAddOnTable() throws OException
   {
   Table tOutput, tRef;


   // User table needs "S(toolset_name) S(date_from) S(date_to) F(addon)"
   // Can have "S(ins_name)", rows with ins_name = "" are default for toolset
   tOutput = Table.tableNew("AddOns");
   try{
   	DBaseTable.execISql( tOutput, " WHERE * FROM USER_STD_Credit_Addons WHERE addon > -42 " );
   }
   catch( OException oex ){
   	m_INCStandard.Print(error_log_file, "ERROR", "\nOException, unsuccessful database query, " + oex.getMessage() );
   }

   m_INCSTDSystem.SYS_CheckTableWithAction(tOutput, "S(ins_name) I(toolset) I(ins_type) I(start) I(end) S(tmp)",
			    m_INCSTDSystem.cActionCreateMessage);

   // Transform the names into ids
   tRef = Table.tableNew();
   Ref.loadFromRef(tRef, SHM_USR_TABLES_ENUM.TOOLSETS_TABLE);
   tOutput.select( tRef, "id(toolset)", "label EQ $toolset_name");
   tRef.destroy();
   tRef = Table.tableNew();
   Ref.loadFromRef(tRef, SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);
   tOutput.select( tRef, "id(ins_type)", "label EQ $ins_name");
   tRef.destroy();

   // Parse the dates
   OCalendar.parseSymbolicDates(tOutput, "date_from", "tmp", "start");
   OCalendar.parseSymbolicDates(tOutput, "date_to", "tmp", "end");

   return tOutput;
   } // GetAddOnTable/0.

void dw_extract_result(Table argt, Table returnt) throws OException
{
	int iResType= 0;
	String strWhat;
	Table    tRes;

	// access user result using defined enumeration
	SimResultType resType = SimResultType.create("USER_RESULT_STD_CREDIT_ADDON");

	iResType = resType.getId();
	tRes = m_JVS_INC_STD_UserSimRes.USR_DefaultExtract(argt, returnt, iResType);

	if(tRes.getColNum(SimResult.resultColName(iResType)) > 0)
	{
		strWhat = SimResult.resultColName(iResType) + "(creditaddon)";
		returnt.select(tRes, strWhat, "deal_num EQ $deal_num AND deal_leg EQ $deal_leg");
	}

	tRes.destroy();
}

}
