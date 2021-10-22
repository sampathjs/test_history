package com.olf.jm.metalstransfer.report;

/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author             | Description                                                                  |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 18-Aug-2021 |               | Rohit Tomar        | Initial version.                      									   |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */

import com.olf.openjvs.Ask;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openjvs.enums.ASK_TEXT_DATA_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;

/**
 * This is the param class for the metal transfer validation report
 * Its gives the pop up to user for selection according to which the data is fetched from the database.
 * 
 * @author TomarR01
 *
 */
public class MetalTransferValidationParam implements IScript {

	@Override
	public void execute(IContainerContext context) throws OException {

		int ret_val = 1;
		Table ask_tbl;
		Table tblDate;
		Table tblYesNo;
		Table tblStatus;
		Table party;

		Table argt = context.getArgumentsTable();
		argt.addCol(MetalTransferValidationHelper.PARTY, COL_TYPE_ENUM.COL_STRING);
		argt.addCol(MetalTransferValidationHelper.DATE, COL_TYPE_ENUM.COL_STRING);
		argt.addCol(MetalTransferValidationHelper.START_DATE, COL_TYPE_ENUM.COL_STRING);
		argt.addCol(MetalTransferValidationHelper.END_DATE, COL_TYPE_ENUM.COL_STRING);
		argt.addCol(MetalTransferValidationHelper.STATUS, COL_TYPE_ENUM.COL_STRING);
		argt.addCol(MetalTransferValidationHelper.EMAIL_FLAG, COL_TYPE_ENUM.COL_STRING);

		ask_tbl = Table.tableNew();
		tblDate = getDateSelection();
		tblYesNo = yesNo();
		tblStatus = getStatus();
		party = getInternalParty();

		if (Util.canAccessGui() == 1) {

			Ask.setAvsTable(ask_tbl, party, "Select Party", 1, ASK_SELECT_TYPES.ASK_MULTI_SELECT.toInt());

			Ask.setAvsTable(ask_tbl, tblDate, "Select Settle/Trade date", 1, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(),1, tblDate);

			Ask.setTextEdit(ask_tbl, "Start Date", OCalendar.formatDateInt(OCalendar.getSOM(Util.getBusinessDate())),ASK_TEXT_DATA_TYPES.ASK_DATE);

			Ask.setTextEdit(ask_tbl, "End Date", OCalendar.formatDateInt(Util.getBusinessDate()),ASK_TEXT_DATA_TYPES.ASK_DATE);

			Ask.setAvsTable(ask_tbl, tblStatus, "Select Status", 1, ASK_SELECT_TYPES.ASK_MULTI_SELECT.toInt(), 1,tblStatus);

			Ask.setAvsTable(ask_tbl, tblYesNo, "Email", 1, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(), 1, tblYesNo);

			ret_val = Ask.viewTable(ask_tbl, "Metal Transfer Validation Report","Please select Input parameters for the report");

			if (ret_val <= 0) {

				Ask.ok("\nUser pressed cancel. Aborting...");
				Util.exitFail("\nUser pressed cancel. Aborting...");
				return;
			}

			argt.addRow();

			argt.setString(MetalTransferValidationHelper.PARTY, 1, ask_tbl.getTable(2, 1).getString("ted_str_value", 1));
			argt.setString(MetalTransferValidationHelper.DATE, 1, ask_tbl.getTable(2, 2).getString("ted_str_value", 1));
			argt.setString(MetalTransferValidationHelper.START_DATE, 1, ask_tbl.getTable(2, 3).getString("ted_str_value", 1));
			argt.setString(MetalTransferValidationHelper.END_DATE, 1, ask_tbl.getTable(2, 4).getString("ted_str_value", 1));
			argt.setString(MetalTransferValidationHelper.STATUS, 1, ask_tbl.getTable(2, 5).getString("ted_str_value", 1));
			argt.setString(MetalTransferValidationHelper.EMAIL_FLAG, 1,	ask_tbl.getTable(2, 6).getString("ted_str_value", 1));

		} else {
			argt.addRow();

			argt.setString(MetalTransferValidationHelper.PARTY, 1,"JM PM LTD , JM PMM CN , JM PMM HK , JM PMM UK , JM PMM US");
			argt.setString(MetalTransferValidationHelper.DATE, 1, MetalTransferValidationHelper.SETTLEDATE);
			argt.setString(MetalTransferValidationHelper.START_DATE, 1,OCalendar.formatDateInt(OCalendar.getSOM(Util.getBusinessDate())));
			argt.setString(MetalTransferValidationHelper.END_DATE, 1, OCalendar.formatDateInt(Util.getBusinessDate()));
			argt.setString(MetalTransferValidationHelper.STATUS, 1, MetalTransferValidationHelper.VALIDATED);
			argt.setString(MetalTransferValidationHelper.EMAIL_FLAG, 1, MetalTransferValidationHelper.NO);
		}
	}

	/**
	 * @return
	 */
	private Table getInternalParty() {

		Table tblData = null;
		StringBuilder sql = null;

		try {
			tblData = Table.tableNew();

			sql = new StringBuilder();

			sql.append("SELECT short_name AS party ").append("\n FROM party ")
					.append("\n WHERE int_ext = 0 AND party_class = 1 AND party_status = 1");

			DBaseTable.execISql(tblData, sql.toString());

		} catch (OException e) {

		}
		return tblData;
	}

	/**
	 * @return
	 */
	private Table getStatus() {

		Table tblData = null;

		try {
			tblData = Table.tableNew();
			tblData.addCol(MetalTransferValidationHelper.STATUS, COL_TYPE_ENUM.COL_STRING);		
			
			tblData.addRowsWithValues("("+MetalTransferValidationHelper.VALIDATED+"),"
									+ "("+MetalTransferValidationHelper.NEW+"),"
									+ "("+MetalTransferValidationHelper.DELETED+"),"
									+ "("+MetalTransferValidationHelper.CANCELLED+")");

		} catch (OException e) {

		}
		return tblData;

	}
	
	/**
	 * @return
	 */
	private Table yesNo() {

		Table tblData = null;

		try {
			tblData = Table.tableNew();
			tblData.addCol(MetalTransferValidationHelper.FLAG, COL_TYPE_ENUM.COL_STRING);		
			
			tblData.addRowsWithValues("("+MetalTransferValidationHelper.NO+"),"
									+ "("+MetalTransferValidationHelper.YES+")");

		} catch (OException e) {

		}
		return tblData;

	}
	
	
	/**
	 * @return
	 */
	private Table getDateSelection() {

		Table tblData = null;

		try {
			tblData = Table.tableNew();
			tblData.addCol(MetalTransferValidationHelper.DATE, COL_TYPE_ENUM.COL_STRING);		
			
			tblData.addRowsWithValues("("+MetalTransferValidationHelper.SETTLEDATE+"),"
									+ "("+MetalTransferValidationHelper.TRADEDATE+")");

		} catch (OException e) {

		}
		return tblData;

	}

}
