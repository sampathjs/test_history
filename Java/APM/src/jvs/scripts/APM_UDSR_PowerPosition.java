/* Released with version 05-Feb-2020_V17_0_8 of APM */

package jvs.scripts;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

/* 
 * For V17 and up 
 * 
			 */

public class APM_UDSR_PowerPosition implements IScript {

	private Table argt;
	private Table returnt;
	private Boolean isDebug;
	static private final int POWER_POSITION = 1;	

	public APM_UDSR_PowerPosition() throws OException {
		isDebug = true;

		}

	public void execute(IContainerContext context) throws OException {
		argt = context.getArgumentsTable();
		returnt = context.getReturnTable();

		USER_RESULT_OPERATIONS operation;
		operation = USER_RESULT_OPERATIONS.fromInt(argt.getInt("operation", 1));

		switch (operation) {
		case USER_RES_OP_CALCULATE:
			Power.generatePwrPositions(argt, returnt, POWER_POSITION);
			break;
		case USER_RES_OP_FORMAT:
			formatResult();
			break;
		default:
			logDebugMessage("Incorrect operation code");
			argt.setString("error_msg", 1, "Incorrect operation code");
			Util.exitFail();
		}

		if (isDebug) {
			logDebugMessage("\n JVS script was used to generate this APM Power Pos\n\n");
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
		int iStartDateCol, iEndDateCol, iStartTimeCol, iEndTimeCol, iTranNumCol, iPriceBandCol, iParamSeqNumCol;
		// The Power.generateVolumetricPositions function does this already
		returnt.setColFormatAsDate("startdate", DATE_FORMAT.DATE_FORMAT_DEFAULT);
		returnt.setColFormatAsDate("enddate", DATE_FORMAT.DATE_FORMAT_DEFAULT);
		returnt.setColFormatAsRef("power_choice", SHM_USR_TABLES_ENUM.PWR_CHOICE_TABLE);
		returnt.setColFormatAsRef("control_area", SHM_USR_TABLES_ENUM.PWR_CTL_AREA_TABLE);
		returnt.setColFormatAsRef("power_initial_term", SHM_USR_TABLES_ENUM.INITIAL_TERM_TABLE);
		returnt.setColFormatAsRef("location", SHM_USR_TABLES_ENUM.PWR_LOCATION_TABLE);
		returnt.setColFormatAsRef("power_del_loc", SHM_USR_TABLES_ENUM.PWR_LOCATION_TABLE);
		returnt.setColFormatAsRef("power_rec_loc", SHM_USR_TABLES_ENUM.PWR_LOCATION_TABLE);
		returnt.setColFormatAsRef("product", SHM_USR_TABLES_ENUM.PWR_PRODUCT_TABLE);
		returnt.setColFormatAsRef("region", SHM_USR_TABLES_ENUM.PWR_REGION_TABLE);
		returnt.setColFormatAsRef("power_service_type", SHM_USR_TABLES_ENUM.SERVICE_TYPE_TABLE);
		returnt.setColFormatAsRef("reporting_timezone", SHM_USR_TABLES_ENUM.TIME_ZONE_TABLE);
		returnt.setColFormatAsRef("deal_timezone", SHM_USR_TABLES_ENUM.TIME_ZONE_TABLE);
		returnt.setColFormatAsRef("price_band", SHM_USR_TABLES_ENUM.PRICE_BAND_TABLE);
		returnt.setColFormatAsRef("volume_type", SHM_USR_TABLES_ENUM.VOLUME_TYPE_TABLE);
		returnt.setColFormatAsRef("reporting_valuation_product", SHM_USR_TABLES_ENUM.PRODUCT_FORMAT_TABLE);
		returnt.setColFormatAsRef("deal_valuation_product", SHM_USR_TABLES_ENUM.PRODUCT_FORMAT_TABLE);
		returnt.setColFormatAsRef("projection_index", SHM_USR_TABLES_ENUM.INDEX_TABLE);
		returnt.setColFormatAsRef("unit", SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);
		returnt.setColFormatAsTime("start_time");
		returnt.setColFormatAsTime("end_time");
		returnt.setColFormatAsRef("pwr_product_id", SHM_USR_TABLES_ENUM.PRODUCT_FORMAT_TABLE);
		returnt.setColFormatAsRef("bav_flag", SHM_USR_TABLES_ENUM.TRUE_FALSE_TABLE);
		
		iTranNumCol = returnt.getColNum("tran_num");
		iParamSeqNumCol = returnt.getColNum("param_seq_num");
		iPriceBandCol = returnt.getColNum("price_band");
		iStartDateCol = returnt.getColNum("startdate");
		iEndDateCol = returnt.getColNum("enddate");
		iStartTimeCol = returnt.getColNum("start_time");
		iEndTimeCol = returnt.getColNum("end_time");
		returnt.clearGroupBy();

		returnt.addGroupBy(iTranNumCol);
		returnt.addGroupBy(iParamSeqNumCol);
		returnt.addGroupBy(iStartDateCol);
		returnt.addGroupBy(iStartTimeCol);
		returnt.addGroupBy(iEndDateCol);
		returnt.addGroupBy(iEndTimeCol);
		returnt.addGroupBy(iPriceBandCol);
		returnt.groupBy();
	}

}

