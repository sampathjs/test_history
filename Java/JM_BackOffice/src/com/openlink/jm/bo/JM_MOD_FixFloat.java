/* $Header: /cvs/master/olf/conf/BackOfficeStubs/src/standard/back_office_module/STD_MOD_FixFloat.java,v 1.4 2013/09/27 13:46:15 dchan Exp $*/

package com.openlink.jm.bo;
import java.text.DecimalFormat;

import standard.back_office_module.include.JVS_INC_STD_DocMsg;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.ScriptAttributes;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.DATE_LOCALE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;

/*
 *	History:
 * 	2016-09-12	V1.0	jwaechter	- Initial Version created as copy of STD_MOD_FixFloat.
 * 									- changed retrieval of olfNotnl field to capture all decimals
 *  2017-04-13	V1.1	jwaechter	- Added unit conversion to olfNotnl in case 
 *  								  the TRANF_UNIT field is applicable (). 
 *                                    Assuming olfNotnl to be in TOz always.
 *  2017-12-12 V1.2     sma         - fix for olfLastPymtDateS2, pymt date on the second leg SIDE_2
 *  2020-03-25 V1.3	    YadavP03    - memory leaks, formatting changes                                
 */

/**
 * Customisation of STD_MOD_FixFloat created for JM.
 * @author jwaechter
 * @version 1.2
 */
@ScriptAttributes(allowNativeExceptions=false)
public class JM_MOD_FixFloat implements IScript {
	DecimalFormat decFormat = new DecimalFormat("############.########"); // 12 digits, 8 decimals
	
	public void execute(IContainerContext context) throws OException 	{
		Table argt = context.getArgumentsTable();
		String scriptName = "STD_MOD_FixFloat";
		JVS_INC_STD_DocMsg.printMessage("Processing " + scriptName);

		if (argt.getInt( "GetItemList", 1) == 1) {
			// if mode 1
			ITEMLIST_createItemsForSelection( argt.getTable( "ItemList", 1) );
			//argt.viewTable();
		} else  { 
			//if mode 2
			GENDATA_getStandardGenerationData(argt);
			JVS_INC_STD_DocMsg.setXmlData(argt, scriptName);
		}

		JVS_INC_STD_DocMsg.printMessage("Completed Processing " + scriptName);
	}

	void ITEMLIST_createItemsForSelection(Table itemlist_tbl) throws OException {
		String group_name; 

		//LEG/SIDE 1
		group_name = "Swap Base Trade Info, Details, Leg 1";  
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Notional Amount",                           "olfNotnl",           1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Notional Currency",                         "olfNotnlCcy",        1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Payment Currency",                          "olfPymtCcy",         1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "First Payment Date",                        "olfFirstPymtDate",   1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Payment Category",                          "olfPymtCategory",    1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Payment Period",                            "olfPymtPeriod",      1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Last Payment Date",                         "olfLastPymtDate",    1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Fixed/Flating Rate",                        "olfRate",            1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Yield Basis",                               "olfYldBasis",        1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Payment Holiday Schedule",                  "olfHolSchd",         1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Business Day Convention",                   "olfPymtConv",        1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Floating Rate Reference",                   "olfRefSource",       1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Floating Rate Index",                       "olfProjIndex",       1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Index Tenor",                               "olfIndexTenor",      1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Floating Spread",                           "olfFloatSpd",        1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Compounding Period:",                       "olfCompPeriod",      1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Averaging Period",                          "olfAvgPeriod",       1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Method of Averaging",                       "olfAvgType",         1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Reset Holiday Schedule",                    "olfRstHolSchd",      1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Reset Dates",                               "olfRstDate",         1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Total Rows In Amorting Table",              "olfTotalAmort",      1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Fix/Float",                                 "olfFxFlt",           1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Pay/Receive",                               "olfPayRec",          1);

		//LEG/SIDE 2
		group_name = "Swap Base Trade Info, Details, Leg 2";   
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "First Payment Date",                        "olfFirstPymtDateS2", 1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Payment Category",                          "olfPymtCategoryS2",  1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Payment Period",                            "olfPymtPeriodS2",    1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Last Payment Date",                         "olfLastPymtDateS2",  1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Fixed/Flating Rate",                        "olfRateS2",          1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Yield Basis",                               "olfYldBasisS2",      1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Payment Holiday Schedule",                  "olfHolSchdS2",       1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Business Day Convention",                   "olfPymtConvS2",      1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Floating Rate Reference",                   "olfRefSourceS2",     1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Floating Rate Index",                       "olfProjIndexS2",     1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Index Tenor",                               "olfIndexTenorS2",    1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Floating Spread",                           "olfFloatSpdS2",      1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Compounding Period:",                       "olfCompPeriodS2",    1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Averaging Period",                          "olfAvgPeriodS2",     1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Method of Averaging",                       "olfAvgTypeS2",       1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Reset Holiday Schedule",                    "olfRstHolSchdS2",    1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Reset Dates",                               "olfRstDateS2",       1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Total Rows In Amorting Table",              "olfTotalAmortS2",    1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Notional Amount",                           "olfNotnlS2",         1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Notional Currency",                         "olfNotnlCcyS2",      1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Payment Currency",                          "olfPymtCcyS2",       1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Fix/Float",                                 "olfFxFltS2",         1);
		JVS_INC_STD_DocMsg.ItemList.add(itemlist_tbl, group_name, "Pay/Receive",                               "olfPayRecS2",        1);
	}

	void GENDATA_getStandardGenerationData(Table argt) throws OException { 
		Table itemlist_tbl  = argt.getTable( "ItemList",       1);
		Table gendata_tbl   = argt.getTable( "GenData",        1);
		Table event_tbl     = argt.getTable( "EventData",      1);

		Table side_one_tbl = Util.NULL_TABLE;
		Table side_two_tbl = Util.NULL_TABLE;
		Table profile_pymtdate_tbl  = Util.NULL_TABLE;
		Table profile_pymtdate2_tbl = Util.NULL_TABLE;

		Transaction tran_ptr = Util.NULL_TRAN;

		int num_rows = 0;
		int curr_row = 0;
		int ins_para_seq_num = event_tbl.getInt( "ins_para_seq_num",  1);
		int ins_seq_num      = event_tbl.getInt( "ins_seq_num",       1);
		int toolset_id       = event_tbl.getInt( "toolset",           1);
		int tran_num         = event_tbl.getInt( "tran_num",          1);
		int ins_num          = event_tbl.getInt( "ins_num",           1);

		String internal_field_name = null;
		String output_field_name   = null;
		String fixfloat_str = null;
		String fixfloat_side_1 = null;
		String fixfloat_side_2 = null;
		String ins_type_ref_name = null;

		//enumeration
		int SIDE_1 = 0;
		int SIDE_2 = 1;
		int internal_field_name_col_num = 0;
		int output_field_name_col_num   = 0;

		//initialize gendata_tbl
		if (gendata_tbl.getNumRows() <= 0) {
			gendata_tbl.addRow();
		}

		num_rows = itemlist_tbl.getNumRows();
		tran_ptr = JVS_INC_STD_DocMsg.retrieveTransactionObjectFromArgt(tran_num);
		ins_type_ref_name = JVS_INC_STD_DocMsg.GenData.getField("olfInsShortName");

		internal_field_name_col_num = itemlist_tbl.getColNum( "internal_field_name"); //will be removed when new module is ready
		output_field_name_col_num   = itemlist_tbl.getColNum( "output_field_name"); //will be removed when new module is ready

		fixfloat_side_1 = tran_ptr.getField( TRANF_FIELD.TRANF_FX_FLT.toInt(), SIDE_1, null);
		fixfloat_side_2 = tran_ptr.getField( TRANF_FIELD.TRANF_FX_FLT.toInt(), SIDE_2, null);   

		for (curr_row=1; curr_row<=num_rows; curr_row++) {
			
			internal_field_name = itemlist_tbl.getString( internal_field_name_col_num, curr_row);
			output_field_name   = itemlist_tbl.getString( output_field_name_col_num, curr_row);

			if (internal_field_name.equals("olfFxFlt")
					|| internal_field_name.equals("olfPayRec")
					|| internal_field_name.equals("olfNotnl")
					|| internal_field_name.equals("olfNotnlCcy")
					|| internal_field_name.equals("olfPymtCcy")
					|| internal_field_name.equals("olfRate")
					|| internal_field_name.equals("olfYldBasis")
					|| internal_field_name.equals("olfPymtCategory")
					|| internal_field_name.equals("olfPymtPeriod")
					|| internal_field_name.equals("olfHolSchd")
					|| internal_field_name.equals("olfPymtConv")
					|| internal_field_name.equals("olfRefSource")
					|| internal_field_name.equals("olfProjIndex")
					|| internal_field_name.equals("olfAvgType")
					|| internal_field_name.equals("olfAvgPeriod")
					|| internal_field_name.equals("olfRstHolSchd")
					|| internal_field_name.equals("olfFloatSpd")
					|| internal_field_name.equals("olfCompPeriod")
					|| internal_field_name.equals("olfIndexTenor")) {
				
				if (side_one_tbl == Util.NULL_TABLE) {
					if (Str.iEqual(fixfloat_side_1, "fixed") != 0) {
						side_one_tbl = getDealFixedLegTable(tran_ptr, SIDE_1, argt);
					} else {
						side_one_tbl = getDealFloatLegTable(toolset_id, tran_ptr, SIDE_1, ins_seq_num, ins_para_seq_num);
					}
				}

				JVS_INC_STD_DocMsg.GenData.setField(gendata_tbl, output_field_name, side_one_tbl, internal_field_name, 1);

			} else if(internal_field_name.equals("olfFxFltS2")
					|| internal_field_name.equals("olfPayRecS2")
					|| internal_field_name.equals("olfNotnlS2")
					|| internal_field_name.equals("olfNotnlCcyS2")
					|| internal_field_name.equals("olfPymtCcyS2")
					|| internal_field_name.equals("olfRateS2")
					|| internal_field_name.equals("olfYldBasisS2")
					|| internal_field_name.equals("olfPymtCategoryS2")
					|| internal_field_name.equals("olfPymtPeriodS2")
					|| internal_field_name.equals("olfHolSchdS2")
					|| internal_field_name.equals("olfPymtConvS2")
					|| internal_field_name.equals("olfRefSourceS2")
					|| internal_field_name.equals("olfProjIndexS2")
					|| internal_field_name.equals("olfAvgTypeS2")
					|| internal_field_name.equals("olfAvgPeriodS2")
					|| internal_field_name.equals("olfRstHolSchdS2")
					|| internal_field_name.equals("olfFloatSpdS2")
					|| internal_field_name.equals("olfCompPeriodS2")
					|| internal_field_name.equals("olfIndexTenorS2")) {

				if (side_two_tbl == Util.NULL_TABLE) {
					if (Str.iEqual(fixfloat_side_2, "fixed") != 0) {
						side_two_tbl = getDealFixedLegTable(tran_ptr, SIDE_2, argt);
					} else {
						side_two_tbl = getDealFloatLegTable(toolset_id, tran_ptr, SIDE_2, ins_seq_num, ins_para_seq_num);
					}

					JVS_INC_STD_DocMsg.addPostfixToAllCols(side_two_tbl, "S2");
				}
				
				if (side_two_tbl.getColNum(internal_field_name) > -1) {
					String formatVal = side_two_tbl.getString(internal_field_name, 1);
					JVS_INC_STD_DocMsg.GenData.setField(gendata_tbl, output_field_name,formatVal);
				}
				//StdDocMsg.GenData.setField(gendata_tbl, output_field_name, side_two_tbl, internal_field_name, 1);

			} else if( internal_field_name.equals("olfFirstPymtDate")
					|| internal_field_name.equals("olfLastPymtDate")) {
				
				if (profile_pymtdate_tbl == Util.NULL_TABLE) {
					//retrieve first and last payment dates for side 1
					JVS_INC_STD_DocMsg.printMessage("Retrieve profile table for LEG 1");
					profile_pymtdate_tbl = getProfilePymtDateTable(ins_num, SIDE_1);         
				}
				
				if (profile_pymtdate_tbl.getNumRows() > 0) {
					String formatVal = profile_pymtdate_tbl.getString(internal_field_name, 1);
					JVS_INC_STD_DocMsg.GenData.setField(gendata_tbl, output_field_name, formatVal);
				}
				//StdDocMsg.GenData.setField(gendata_tbl, output_field_name, profile_pymtdate_tbl, internal_field_name, 1);

			} else if(internal_field_name.equals("olfFirstPymtDateS2") 
					|| internal_field_name.equals("olfLastPymtDateS2")) { 
				
				if (profile_pymtdate2_tbl == Util.NULL_TABLE) {
					//retrieve first and last payment dates for side 1
					JVS_INC_STD_DocMsg.printMessage("Retrieve profile table for LEG 2");
					profile_pymtdate2_tbl = getProfilePymtDateTable(ins_num, SIDE_2); //V1.2 fix using SIDE_2
//						JVS_INC_STD_DocMsg.addPostfixToAllCols(profile_pymtdate2_tbl, "S2"); //V1.2 fix remove because of duplication               
				}
				
				if (profile_pymtdate2_tbl.getNumRows() > 0) {
					String formatVal = profile_pymtdate2_tbl.getString(internal_field_name, 1);
					JVS_INC_STD_DocMsg.GenData.setField(gendata_tbl, output_field_name,formatVal);
				}
				//StdDocMsg.GenData.setField(gendata_tbl, output_field_name,  profile_pymtdate2_tbl, internal_field_name, 1);
			}
		}//end of for loop
		
		JVS_INC_STD_DocMsg.destroyTable(side_one_tbl);
		JVS_INC_STD_DocMsg.destroyTable(side_two_tbl);
		JVS_INC_STD_DocMsg.destroyTable(profile_pymtdate_tbl);
		JVS_INC_STD_DocMsg.destroyTable(profile_pymtdate2_tbl);
	}

	/*************************************************************************
	 * Name:        getDealFixedLegTable()
	 * Description: returns a table in String columne format
	 *                          with fixed leg data.
	 **************************************************************************/
	Table getDealFixedLegTable(Transaction trans_ptr, int SIDE, Table argt) throws OException {
		Table output_tbl = Util.NULL_TABLE;

		String result_str        = null;
		String period_str        = null;
		String pymt_conv_str     = null;
		String pay_recd_str      = null;
		String ins_type_ref_name = JVS_INC_STD_DocMsg.GenData.getField("olfInsShortName");

		output_tbl = Table.tableNew("Fixed Leg Table");
		output_tbl.addCol( "olfFxFlt",        COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfHolSchd",      COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfNotnl",        COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfNotnlCcy",     COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfPayRec",       COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfPymtCategory", COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfPymtCcy",      COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfPymtConv",     COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfPymtPeriod",   COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfRate",         COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfYldBasis",     COL_TYPE_ENUM.COL_STRING);

		output_tbl.addRow();

		//if( Transaction.isNull(trans_ptr) == 1 || SIDE < 0 )
		if (trans_ptr == Util.NULL_TRAN  || SIDE < 0)
			return output_tbl;

		//"olfPayRec"
		pay_recd_str = trans_ptr.getField( TRANF_FIELD.TRANF_PAY_REC.toInt(), SIDE, null);
		pay_recd_str = Str.toLower(pay_recd_str);

		if (Str.iEqual(pay_recd_str, "recd") != 0) {
			pay_recd_str = "receive";
		}

		if (trans_ptr.getFieldInt(TRANF_FIELD.TRANF_TOOLSET_ID.toInt()) != TOOLSET_ENUM.SWAP_TOOLSET.toInt() ){
			if (!(Str.iEqual(ins_type_ref_name, "Equity-Swap") != 0 )&& !(Str.iEqual(ins_type_ref_name, "Equity-CFD") != 0)) {
				if (SIDE == 1 ) {
					//if Side 2
					if (Str.iEqual(pay_recd_str, "pay") != 0){
						pay_recd_str = "receive";
					} else if (Str.iEqual(pay_recd_str, "receive") != 0){
						pay_recd_str = "pay";
					}
				}
			}
		}
		output_tbl.setString( "olfPayRec", 1, pay_recd_str);

		//"olfFxFlt"
		result_str = trans_ptr.getField( TRANF_FIELD.TRANF_FX_FLT.toInt(), SIDE, null);
		output_tbl.setString( "olfFxFlt", 1, result_str);

		//"olfPymtCcy"
		result_str = trans_ptr.getField( TRANF_FIELD.TRANF_CURRENCY.toInt(), SIDE, null);
		output_tbl.setString( "olfPymtCcy", 1, result_str);

		//"olfNotnlCcy"
		result_str = trans_ptr.getField( TRANF_FIELD.TRANF_NOTNL_CURRENCY.toInt(), SIDE, null);
		output_tbl.setString( "olfNotnlCcy", 1, result_str);

		//"olfNotnl"
		double olfNotnl =  trans_ptr.getFieldDouble( TRANF_FIELD.TRANF_NOTNL.toInt(), SIDE, null);
		// add unit conversion if the TRANF_UNIT field is applicable
		if (trans_ptr.isFieldNotAppl(TRANF_FIELD.TRANF_UNIT, SIDE, null) == 0 && 
			trans_ptr.getFieldInt(TRANF_FIELD.TRANF_TOOLSET_ID.toInt()) == TOOLSET_ENUM.LOANDEP_TOOLSET.toInt()) {
			int unit = trans_ptr.getFieldInt( TRANF_FIELD.TRANF_UNIT.toInt(), SIDE, null);
			int unitTOz = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "TOz");
			if (unit != unitTOz) {
				double unitConversionFactor = Transaction.getUnitConversionFactor(unitTOz, unit);
				olfNotnl = olfNotnl * unitConversionFactor;
			}			
		}
		output_tbl.setString( "olfNotnl", 1, decFormat.format(olfNotnl));

		//"olfRate"
		result_str = trans_ptr.getField( TRANF_FIELD.TRANF_RATE.toInt(), SIDE, null);
		output_tbl.setString( "olfRate", 1, result_str);

		//"olfYldBasis"
		result_str = trans_ptr.getField( TRANF_FIELD.TRANF_YIELD_BASIS.toInt(), SIDE, null);
		if (Str.isEmpty(result_str) != 0) {
			result_str = "N/A";
		}
		output_tbl.setString( "olfYldBasis", 1, result_str);

		//"olfPymtPeriod"
		period_str = trans_ptr.getField( TRANF_FIELD.TRANF_PYMT_PERIOD.toInt(), SIDE, null);
		result_str = JVS_INC_STD_DocMsg.getPeriodLongName( period_str );
		output_tbl.setString( "olfPymtPeriod", 1, result_str);

		//olfPymtCategory
		result_str = JVS_INC_STD_DocMsg.getPeriodCategory( period_str );
		output_tbl.setString( "olfPymtCategory", 1, result_str);

		//"olfHolSchd"
		result_str = trans_ptr.getField( TRANF_FIELD.TRANF_HOL_LIST.toInt(), SIDE, null);
		output_tbl.setString( "olfHolSchd", 1, result_str);

		//"olfPymtConv"
		pymt_conv_str = trans_ptr.getField( TRANF_FIELD.TRANF_PAYMENT_CONV.toInt(), SIDE, null);
		result_str = JVS_INC_STD_DocMsg.getPaymentConvertionFullName( pymt_conv_str );
		output_tbl.setString( "olfPymtConv", 1, result_str);

		return output_tbl;
	}

	/******************************************************************************
	 * Name:        getDealFloatLegTable()
	 * Description: returns a table in String columne format with floating leg data.
	 *
	 * Dates:       6/23/2002
	 * Revision:    extend to support Option toolset ( 10/29/2002 )
	 *              change olfCtpBuySell to our unit olfBuySell logic for Option.
	 ******************************************************************************/
	Table getDealFloatLegTable(int toolset_id, Transaction trans_ptr, int SIDE, int ins_seq_num, int ins_para_seq_num) throws OException {
		Table output_tbl  = Util.NULL_TABLE;

		String result_str      = null;
		String period_str      = null;
		String pymt_conv_str   = null;
		String pay_recd_str    = null;
		String comp_period_str = null;
		String avg_period_str  = null;
		double prem_dbl = 0.0;

		output_tbl = Table.tableNew("Floating Leg Table");
		output_tbl.addCol( "olfAvgPeriod",     COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfAvgType",       COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfCompPeriod",    COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfFloatSpd",      COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfFxFlt",         COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfHolSchd",       COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfIndexTenor",    COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfNotnl",         COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfNotnlCcy",      COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfPayRec",        COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfProjIndex",     COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfPymtCategory",  COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfPymtCcy",       COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfPymtConv",      COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfPymtPeriod",    COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfRate",          COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfRefSource",     COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfRstHolSchd",    COL_TYPE_ENUM.COL_STRING);
		output_tbl.addCol( "olfYldBasis",      COL_TYPE_ENUM.COL_STRING);

		//add 1 row
		output_tbl.addRow();

		if (trans_ptr == Util.NULL_TRAN || SIDE < 0)
			return output_tbl;

		//Check if it the option only have 1 side

		if (toolset_id == TOOLSET_ENUM.OPTION_TOOLSET.toInt() && trans_ptr.getNumParams() ==1 && SIDE >0)
			return output_tbl;

		//"olfPymtCcy"
		result_str = trans_ptr.getField( TRANF_FIELD.TRANF_CURRENCY.toInt(), SIDE, null);
		output_tbl.setString( "olfPymtCcy", 1, result_str);

		//"olfNotnl"
		double olfNotional = trans_ptr.getFieldDouble( TRANF_FIELD.TRANF_NOTNL.toInt(), SIDE, null);
		output_tbl.setString( "olfNotnl", 1, decFormat.format(olfNotional));

		//"olfYldBasis"
		result_str = trans_ptr.getField( TRANF_FIELD.TRANF_YIELD_BASIS.toInt(), SIDE, null);
		output_tbl.setString( "olfYldBasis", 1, result_str);

		//"olfPymtPeriod"
		period_str = trans_ptr.getField( TRANF_FIELD.TRANF_PYMT_PERIOD.toInt(), SIDE, null);
		result_str = JVS_INC_STD_DocMsg.getPeriodLongName( period_str );
		output_tbl.setString( "olfPymtPeriod", 1, result_str);

		//olfPymtCategory
		result_str = JVS_INC_STD_DocMsg.getPeriodCategory( period_str );
		output_tbl.setString( "olfPymtCategory", 1, result_str);

		//"olfHolSchd"
		result_str = trans_ptr.getField( TRANF_FIELD.TRANF_HOL_LIST.toInt(), SIDE, null);
		output_tbl.setString( "olfHolSchd", 1, result_str);

		//"olfPymtConv"
		pymt_conv_str = trans_ptr.getField( TRANF_FIELD.TRANF_PAYMENT_CONV.toInt(), SIDE, null);
		result_str = JVS_INC_STD_DocMsg.getPaymentConvertionFullName( pymt_conv_str );
		output_tbl.setString( "olfPymtConv", 1, result_str);

		//"olfRefSource"
		result_str = trans_ptr.getField( TRANF_FIELD.TRANF_REF_SOURCE.toInt(), SIDE, null);
		output_tbl.setString( "olfRefSource", 1, result_str);

		//olfProjIndex
		result_str = trans_ptr.getField( TRANF_FIELD.TRANF_PROJ_INDEX.toInt(), SIDE, null);
		output_tbl.setString( "olfProjIndex", 1, result_str);

		//"olfAvgPeriod"
		avg_period_str = trans_ptr.getField( TRANF_FIELD.TRANF_AVG_PERIOD.toInt(), SIDE, null);
		output_tbl.setString( "olfAvgPeriod", 1, avg_period_str);

		//"olfAvgType"
		if (Str.iEqual( avg_period_str, "n/a") != 0 || Str.isEmpty( avg_period_str) != 0) {
			result_str = "N/A";
		} else {
			result_str = trans_ptr.getField( TRANF_FIELD.TRANF_AVG_TYPE.toInt(), SIDE, null);
		}
		output_tbl.setString( "olfAvgType", 1, result_str);

		//"olfRstHolSchd"
		result_str = trans_ptr.getField( TRANF_FIELD.TRANF_RESET_HOL_LIST.toInt(), SIDE, null);
		output_tbl.setString( "olfRstHolSchd", 1, result_str);

		//"olfFloatSpd"
		result_str = trans_ptr.getField( TRANF_FIELD.TRANF_FLOAT_SPD.toInt(), SIDE, null);
		output_tbl.setString( "olfFloatSpd", 1, result_str);

		//"olfCompPeriod"
		comp_period_str =  trans_ptr.getField( TRANF_FIELD.TRANF_COMP_PERIOD.toInt(), SIDE, null);
		result_str = JVS_INC_STD_DocMsg.getPeriodLongName( comp_period_str );
		output_tbl.setString( "olfCompPeriod", 1, result_str);

		//"olfRate"
		//"olfRate" --- get from 1st reset rate of reset table
		//result_str = trans_ptr.getField( TRANF_FIELD.TRANF_RATE, SIDE, null); //BUG
		//For swap (floating side only): If status is "known" then display the value of initial
		//flaoting rate. else display Unknown. Requested changes by Joseph Henry on Sep 8, 2004.
		String reset_value_status_str = trans_ptr.getField( TRANF_FIELD.TRANF_RESET_VALUE_STATUS.toInt(), SIDE, null, ins_seq_num, ins_para_seq_num);
		if (toolset_id == TOOLSET_ENUM.SWAP_TOOLSET.toInt() && Str.iEqual(reset_value_status_str, "Unknown") == 1) {
			output_tbl.setString( "olfRate", 1, "UNKNOWN");
		} else {
			result_str = JVS_INC_STD_DocMsg.getFloatSideFirstResetRate(trans_ptr);
			output_tbl.setString( "olfRate", 1, result_str);
		}

		//"olfNotnlCcy"
		result_str = trans_ptr.getField( TRANF_FIELD.TRANF_NOTNL_CURRENCY.toInt(), SIDE, null);
		output_tbl.setString( "olfNotnlCcy", 1, result_str);

		//"olfPayRec"
		pay_recd_str = trans_ptr.getField( TRANF_FIELD.TRANF_PAY_REC.toInt(), SIDE, null);
		pay_recd_str = Str.toLower(pay_recd_str);

		if (Str.iEqual(pay_recd_str, "recd") != 0) {
			pay_recd_str = "receive";
		}

		//Bug Fixed for DTS#14308
		//if( SIDE == 1 )//if Side 2
		//{
		//   if(Str.iEqual(pay_recd_str, "pay") != 0)
		//       pay_recd_str = "receive";
		//   else if(Str.iEqual(pay_recd_str, "receive") != 0)
		//       pay_recd_str = "pay";
		//}
		output_tbl.setString( "olfPayRec", 1, pay_recd_str);

		//"olfFxFlt"
		result_str = trans_ptr.getField( TRANF_FIELD.TRANF_FX_FLT.toInt(), SIDE, null);
		output_tbl.setString( "olfFxFlt", 1, result_str);

		//"olfIndexTenor"
		result_str = trans_ptr.getField( TRANF_FIELD.TRANF_PROJ_INDEX_TENOR.toInt(), SIDE, null);
		result_str = JVS_INC_STD_DocMsg.getPeriodLongName(result_str);
		output_tbl.setString( "olfIndexTenor", 1, result_str);

		return output_tbl;
	}

	Table getProfilePymtDateTable(int ins_num, int SIDE) throws OException {
		Table output_tbl = Util.NULL_TABLE;

		String last_pymt_date_str = null;
		String what = "param_seq_num, " + "profile_seq_num, " + "pymt_date";
		String from = "profile";
		String where = "ins_num = " + ins_num + " and param_seq_num = " + SIDE;

		output_tbl = Table.tableNew(from);
		JVS_INC_STD_DocMsg.loadDB(output_tbl, what, from, where);

		if (output_tbl.getNumCols() <= 0) {
			output_tbl.addRow();
		}

		output_tbl.group( "param_seq_num, profile_seq_num");
		output_tbl.setColFormatAsDate( "pymt_date", DATE_FORMAT.DATE_FORMAT_DMY_NOSLASH, DATE_LOCALE.DATE_LOCALE_US);

		JVS_INC_STD_DocMsg.convertColumnToStringN(output_tbl, "pymt_date");
		JVS_INC_STD_DocMsg.renameColumn(output_tbl, "olfFirstPymtDate", "pymt_date");

		output_tbl.addCol( "olfLastPymtDate", COL_TYPE_ENUM.COL_STRING);
		int rows = output_tbl.getNumRows();
		if (rows > 0) {
			last_pymt_date_str = output_tbl.getString( "olfFirstPymtDate", rows);
			output_tbl.setString( "olfLastPymtDate", 1, last_pymt_date_str);			
		}

		if (SIDE == 1) {
			JVS_INC_STD_DocMsg.addPostfixToAllCols(output_tbl, "S2");
		}

		return output_tbl;
	}

}
