   /*$Header: /cvs/master/olf/plugins/standard/credit/USR_STD_FXConversion.java,v 1.8.296.1 2015/07/29 12:51:17 chrish Exp $*/

/*
File Name:                      USR_STD_FXConversion.java

Report Name:                    NONE

Output File Name:               NONE

Author:                         Guillaume Cortade / Alex Afraymovich
Creation Date:

Revision History:				Sep 28, 2011 - DTS86132 - BLAST:- V10_2_1_09202011 :- System could not compile and import the standard scripts in Credit folder
								Mar 29, 2006 - Alex A. made this a standard script
                                Nov 06, 2003 - Guillaume C.

Script Type:                    User-defined simulation result

Recommended Script Category: Simulation Result

Main Script:
Parameter Script:
Display Script:

Description:                    Conversion to base/scenario currency general result
                                Dependant on PFOLIO_RESULT_TYPE.FX_RESULT

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

public class USR_STD_FXConversion implements IScript {
	private JVS_INC_STD_UserSimRes m_INCSTDUserSimRes;
	public USR_STD_FXConversion(){
		m_INCSTDUserSimRes = new JVS_INC_STD_UserSimRes();

	}



	// *****************************************************************************
	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();

		if( m_INCSTDUserSimRes.USR_RunMode(argt) == USER_RESULT_OPERATIONS.USER_RES_OP_CALCULATE.toInt())
		{
			compute_result(argt, returnt);
		}
		else if(m_INCSTDUserSimRes.USR_RunMode(argt) == USER_RESULT_OPERATIONS.USER_RES_OP_FORMAT.toInt())
		{
			format_result(returnt);
		}
		else if(m_INCSTDUserSimRes.USR_RunMode(argt) == USER_RESULT_OPERATIONS.USER_RES_OP_DWEXTRACT.toInt())
		{
			//m_INCSTDUserSimRes.USR_DefaultExtract(argt, returnt);
			dw_extract_result(argt, returnt);
		}
		else
		{
			m_INCSTDUserSimRes.USR_Fail("Incorrect operation code", argt);
		}
		// Call the virtual functions according to action type

		return;
	} // main/0.


	// *****************************************************************************
	void compute_result(Table argt, Table returnt) throws OException
	{
		int            iBase, iReport, iLoop, iTran, iDeal, iLeg, iCcy, iDiscIndex, iRow, toolset;
		Table       tTmp;
		Transaction  pTran;
		double         dConv;

		returnt.addCols( "I(tran_num) I(deal_num) I(deal_leg) I(index) I(currency) I(base_ccy) I(scen_ccy) F(base_fx_conv) F(scen_fx_conv)");

		// Retrieve the deal and leg currencies and indexes
		tTmp = m_INCSTDUserSimRes.USR_Transactions(argt);
		for(iLoop = tTmp.getNumRows(); iLoop > 0; iLoop--)
		{
			pTran = tTmp.getTran( "tran_ptr", iLoop);
			iTran = tTmp.getInt( "tran_num", iLoop);
			iDeal = tTmp.getInt( "deal_num", iLoop);

			// Loop on the legs
			for(iLeg = pTran.getNumParams() - 1; iLeg > -1; iLeg--)
			{
				// Get the discounting index for each parameter record
				iDiscIndex = pTran.getFieldInt( TRANF_FIELD.TRANF_DISC_INDEX.toInt(), iLeg);
				if (iDiscIndex == 0)
					continue;
				
				// Add a return row and set the output values
				iRow = returnt.addRow();
				returnt.setInt( "tran_num", iRow, iTran);
				returnt.setInt( "deal_num", iRow, iDeal);
				returnt.setInt( "deal_leg", iRow, iLeg);
				returnt.setInt( "index",    iRow, iDiscIndex);

				toolset = pTran.getFieldInt(TRANF_FIELD.TRANF_TOOLSET_ID.toInt(), 0);
				if (toolset == TOOLSET_ENUM.FX_TOOLSET.toInt())
				{
					Transaction holdingTran;
					holdingTran = pTran.getHoldingTran();
					if (holdingTran == null)
						continue;
					
					// The FX curve is missing from this result. That curve is found on the projection
					// index for FX Instruments
					iDiscIndex = holdingTran.getFieldInt(TRANF_FIELD.TRANF_PROJ_INDEX.toInt(), iLeg);
					if (iDiscIndex == 0)
						continue;

					iRow = returnt.addRow();
					returnt.setInt( "tran_num", iRow, iTran);
					returnt.setInt( "deal_num", iRow, iDeal);
					returnt.setInt( "deal_leg", iRow, iLeg);
					returnt.setInt( "index",    iRow, iDiscIndex);
				}
			} // for iLeg.
		} // for iLoop.
		tTmp.destroy();

		// get the currency for each index
		Index.tableColConvertIndexToCurrency(returnt, "index", "currency");
		
		// Get the currencies to convert to
		iBase = Ref.getLocalCurrency();
		tTmp = m_INCSTDUserSimRes.USR_GetScenDef(argt);
		iReport = tTmp.getInt( "scenario_currency", 1);
		tTmp.destroy();

		// Add the base and reporting currency information
		returnt.setColValInt( "base_ccy", iBase);
		returnt.setColValInt( "scen_ccy", iReport);

		// Get the FX result
		tTmp = m_INCSTDUserSimRes.USR_GetRes(PFOLIO_RESULT_TYPE.FX_RESULT.toInt(), argt);
		if(Table.isTableValid(tTmp) == 0)
			m_INCSTDUserSimRes.USR_Fail("USR_FXConv requires PFOLIO_RESULT_TYPE.FX_RESULT", argt);

		// Convert to base currency
		returnt.setColValDouble( "base_fx_conv", 1.0);
		returnt.select( tTmp, "result(base_fx_conv)", "id EQ $currency");

		// Get conversion from scenario to base
		tTmp.sortCol( "id");
		iCcy = tTmp.findInt( "id", iReport, SEARCH_ENUM.FIRST_IN_GROUP);
		if(iCcy < 1)
			m_INCSTDUserSimRes.USR_Fail("Scenario currency has no FX conversion", argt);
		dConv = tTmp.getDouble( "result", iCcy);
		tTmp.destroy();

		// Convert to scenario currency
		if(dConv < 0.00000000001)
			m_INCSTDUserSimRes.USR_Fail("FX conversion from scenario to base invalid", argt);
		returnt.mathMultColConst( "base_fx_conv", 1.0 / dConv, "scen_fx_conv");
	} // compute_result/0.


	// *****************************************************************************
	void format_result(Table returnt) throws OException
	{
		// Set viewer column titles
		returnt.setColTitle( "tran_num",     "Transaction\nNumber");
		returnt.setColTitle( "deal_num",     "Deal\nNumber");
		returnt.setColTitle( "deal_leg",     "Deal\nLeg");
		returnt.setColTitle( "index",        "Index\nName");
		returnt.setColTitle( "currency",     "Original\nCurrency");
		returnt.setColTitle( "base_ccy",     "Base\nCurrency");
		returnt.setColTitle( "scen_ccy",     "Scenario\nCurrency");
		returnt.setColTitle( "base_fx_conv", "Conversion\nTo Base");
		returnt.setColTitle( "scen_fx_conv", "Conversion\nTo Scen.");

		// Format ids to names and sort
		returnt.defaultFormat();
		returnt.setColFormatAsRef( "base_ccy", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		returnt.setColFormatAsRef( "scen_ccy", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
		returnt.setColFormatAsNotnl( "base_fx_conv", 12, 5, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		returnt.setColFormatAsNotnl( "scen_fx_conv", 12, 5, COL_FORMAT_BASE_ENUM.BASE_NONE.toInt());
		returnt.group( "tran_num, deal_leg, index");
	} // format_result/0.

	void dw_extract_result(Table argt, Table returnt) throws OException
	{
	}

}
