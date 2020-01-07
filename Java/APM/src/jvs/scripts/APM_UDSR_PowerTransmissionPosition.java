/* Released with version 27-Feb-2019_V17_0_7 of APM */

package jvs.scripts;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

/*
 * For V14 and up
 */

public class APM_UDSR_PowerTransmissionPosition implements IScript {
	
	private static final int MAX_NUMBER_OF_DB_RETRIES = 10;
	
	static private final int POWER_TRANSMISSION_POSITION = 0;	

	private int iToday;
	private Table argt;
	private Table returnt;
	private Boolean isDebugMode;


	public APM_UDSR_PowerTransmissionPosition() throws OException {
		iToday = OCalendar.today();
		isDebugMode = true;
			}

	public void execute(IContainerContext context) throws OException {
		argt = context.getArgumentsTable();
		returnt = context.getReturnTable();

		USER_RESULT_OPERATIONS operation;
		operation = USER_RESULT_OPERATIONS.fromInt(argt.getInt("operation", 1));

		switch (operation) {
		case USER_RES_OP_CALCULATE:
			Power.generatePwrPositions(argt, returnt, POWER_TRANSMISSION_POSITION);
			break;
		case USER_RES_OP_FORMAT:
			formatResult();
			break;
		default:
			argt.setString("error_msg", 1, "Incorrect operation code");
			Util.exitFail();
		}

		if (isDebugMode) {
			logDebugMessage("\n JVS was used to generate this APM Power Transmission\n\n");
	}

		Util.exitSucceed();
		}

	public void logDebugMessage(String sProcessingMessage) throws OException {
		String msg;
		msg = OCalendar.formatDateInt(OCalendar.getServerDate(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH) + " " + Util.timeGetServerTimeHMS() + " : "
				+ sProcessingMessage + "\n";
		OConsole.oprint(msg);
	}

	// *****************************************************************************
	public void formatResult() throws OException {
		int iStartDateCol, iEndDateCol, iStartTimeCol, iEndTimeCol, iTranNumCol, iPriceBandCol;
		// The Power.generateTransmissionPositions function does this already
		returnt.setColFormatAsDate("startdate", DATE_FORMAT.DATE_FORMAT_DEFAULT);
		returnt.setColFormatAsDate("enddate", DATE_FORMAT.DATE_FORMAT_DEFAULT);
		returnt.setColFormatAsTime("start_time");
		returnt.setColFormatAsTime("end_time");
		returnt.setColFormatAsRef("buy_sell", SHM_USR_TABLES_ENUM.BUY_SELL_TABLE);
		returnt.setColFormatAsRef("ins_type", SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE);
		returnt.setColFormatAsRef("ins_sub_type", SHM_USR_TABLES_ENUM.INS_SUB_TYPE_TABLE);
		returnt.setColFormatAsRef("pay_receive", SHM_USR_TABLES_ENUM.REC_PAY_TABLE);
		returnt.setColFormatAsRef("valuation_product", SHM_USR_TABLES_ENUM.PRODUCT_FORMAT_TABLE); /* SHM_USR_TABLES_ENUM. *//* PowerPos pwr_product_id */
		returnt.setColFormatAsRef("product", SHM_USR_TABLES_ENUM.PWR_PRODUCT_TABLE);
		returnt.setColFormatAsRef("tsd_price_band", SHM_USR_TABLES_ENUM.PRICE_BAND_TABLE);
		returnt.setColFormatAsRef("volume_type", SHM_USR_TABLES_ENUM.VOLUME_TYPE_TABLE);
		returnt.setColFormatAsRef("bav_flag", SHM_USR_TABLES_ENUM.TRUE_FALSE_TABLE);
		returnt.setColFormatAsRef("por_location", SHM_USR_TABLES_ENUM.PWR_LOCATION_TABLE);
		returnt.setColFormatAsRef("pod_location", SHM_USR_TABLES_ENUM.PWR_LOCATION_TABLE);
		returnt.setColFormatAsRef("unit", SHM_USR_TABLES_ENUM.UNIT_DISPLAY_TABLE);// defined by spec. power pos uses IDX_UNIT_TABLE
		returnt.setColFormatAsRef("por_price_band", SHM_USR_TABLES_ENUM.PRICE_BAND_TABLE);
		returnt.setColFormatAsRef("pod_price_band", SHM_USR_TABLES_ENUM.PRICE_BAND_TABLE);
		returnt.setColFormatAsRef("por_proj_index", SHM_USR_TABLES_ENUM.INDEX_TABLE);
		returnt.setColFormatAsRef("pod_proj_index", SHM_USR_TABLES_ENUM.INDEX_TABLE);
		returnt.setColFormatAsRef("user_id", SHM_USR_TABLES_ENUM.PERSONNEL_TABLE);
		returnt.setColFormatAsRef("toolset", SHM_USR_TABLES_ENUM.TOOLSETS_TABLE);
		returnt.setColFormatAsRef("internal_lentity", SHM_USR_TABLES_ENUM.PARTY_TABLE);
		returnt.setColFormatAsRef("internal_bunit", SHM_USR_TABLES_ENUM.PARTY_TABLE);
		returnt.setColFormatAsRef("internal_portfolio", SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE);
		returnt.setColFormatAsRef("external_lentity", SHM_USR_TABLES_ENUM.PARTY_TABLE);
		returnt.setColFormatAsRef("external_bunit", SHM_USR_TABLES_ENUM.PARTY_TABLE);
		returnt.setColFormatAsRef("tran_status", SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE);
		returnt.setColFormatAsRef("internal_contact", SHM_USR_TABLES_ENUM.PERSONNEL_TABLE);
		returnt.setColFormatAsDate("trade_date", DATE_FORMAT.DATE_FORMAT_MDY_SLASH);
		returnt.setColFormatAsDate("start_date", DATE_FORMAT.DATE_FORMAT_MDY_SLASH); // reset start date
		returnt.setColFormatAsDate("end_date", DATE_FORMAT.DATE_FORMAT_MDY_SLASH); // reset end date
		returnt.setColFormatAsRef("price_unit", SHM_USR_TABLES_ENUM.UNIT_DISPLAY_TABLE);
		returnt.setColFormatAsRef("reporting_timezone", SHM_USR_TABLES_ENUM.TIME_ZONE_TABLE);
		returnt.setColFormatAsRef("reporting_valuation_product", SHM_USR_TABLES_ENUM.PRODUCT_FORMAT_TABLE);
		
		iTranNumCol = returnt.getColNum("tran_num");
		iStartDateCol = returnt.getColNum("startdate");
		iEndDateCol = returnt.getColNum("enddate");
		iStartTimeCol = returnt.getColNum("start_time");
		iEndTimeCol = returnt.getColNum("end_time");
		iPriceBandCol = returnt.getColNum("tsd_price_band");
		returnt.clearGroupBy();

		returnt.addGroupBy(iTranNumCol);
		returnt.addGroupBy(iStartDateCol);
		returnt.addGroupBy(iStartTimeCol);
		returnt.addGroupBy(iEndDateCol);
		returnt.addGroupBy(iEndTimeCol);
		returnt.addGroupBy(iPriceBandCol);
		returnt.groupBy();
	} // format_result

}
