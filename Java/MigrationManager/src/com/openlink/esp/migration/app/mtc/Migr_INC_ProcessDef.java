package com.openlink.esp.migration.app.mtc;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

abstract class Migr_INC_ProcessDef implements IScript
{
	// static constants
	public static final String
		COL_IMPORT_DEF_ID                = "type_id",
		COL_SILENT_MODE                  = "silent_mode";
	public static final String
		COL_RUN_LOAD_CSV_FLAG            = "run_csv_load",
		COL_RUN_MAPPING_FLAG             = "run_mapping",
		COL_RUN_STRUCTURING_FLAG         = "run_structuring",
		COL_RUN_FORMATTING_FLAG          = "run_formatting",
		COL_RUN_INT_DEAL_HANDLING_FLAG   = "run_int_deal_handling",
		COL_RUN_BROKER_FEE_HANDLING_FLAG = "run_broker_fee_handling",
		COL_RUN_DEAL_BOOKING_FLAG        = "run_deal_booking",
		COL_RUN_DEAL_UPDATING_FLAG       = "run_deal_updating",
		COL_RUN_SCHED_UPLOAD_FLAG        = "run_sched_upload";

	// to be overwritten thru extending classes
	int
		_import_definition=-1;
	boolean
		_silent_mode             =false,
		_run_load_csv            =false,
		_run_mapping             =false,
		_run_structuring         =false,
		_run_formatting          =false,
		_run_int_deal_handling   =false,
		_run_broker_fee_handling =false,
		_run_deal_booking        =false,
		_run_deal_updating       =false,
		_run_schedule_upload     =false;

	// work variables/constants
	private Table
		argt = null;
	private final static COL_TYPE_ENUM
		COL_INT = COL_TYPE_ENUM.COL_INT;

	// entry point for IScript
	public void execute(IContainerContext context) throws OException
	{
		argt = context.getArgumentsTable();

		putToArgt(COL_IMPORT_DEF_ID, _import_definition);
		putToArgt(COL_SILENT_MODE, _silent_mode);

		putToArgt(COL_RUN_LOAD_CSV_FLAG, _run_load_csv);
		putToArgt(COL_RUN_MAPPING_FLAG, _run_mapping);
		putToArgt(COL_RUN_STRUCTURING_FLAG, _run_structuring);
		putToArgt(COL_RUN_FORMATTING_FLAG, _run_formatting);
		putToArgt(COL_RUN_INT_DEAL_HANDLING_FLAG, _run_int_deal_handling);
		putToArgt(COL_RUN_BROKER_FEE_HANDLING_FLAG, _run_broker_fee_handling);
		putToArgt(COL_RUN_DEAL_BOOKING_FLAG, _run_deal_booking);
		putToArgt(COL_RUN_DEAL_UPDATING_FLAG, _run_deal_updating);

		putToArgt(COL_RUN_SCHED_UPLOAD_FLAG, _run_schedule_upload);
	}

	// helper functions
	private void putToArgt(String col_name, int value) throws OException
	{
		if (argt.getColNum(col_name) < 1)
			argt.addCol(col_name, COL_INT);
		if (argt.getNumRows() < 1)
			argt.addRow();
		argt.setInt(col_name, 1, value);
	}
	private void putToArgt(String col_name, boolean value) throws OException
	{
		if (argt.getColNum(col_name) < 1)
			argt.addCol(col_name, COL_INT);
		if (argt.getNumRows() < 1)
			argt.addRow();
		argt.setInt(col_name, 1, value?1:0);
	}
}
