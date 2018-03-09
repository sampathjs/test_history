package com.openlink.esp.migration.app.mtc;

public class Migr_Def1_BookingOnly_P extends Migr_INC_ProcessDef
{
	public Migr_Def1_BookingOnly_P()
	{
		// --------------------------------------------------------------//
		// 1. ENTER a valid import definition id                         //
		// --------------------------------------------------------------//
		_import_definition=1;

		// --------------------------------------------------------------//
		// 2. ENABLE/DISABLE silent mode                                 //
		//                                                               //
		//    FALSE -> default show all process logs                     //
		//    TRUE - > do NOT show any process logs                      //
		// --------------------------------------------------------------//
		_silent_mode=false;

		// --------------------------------------------------------------//
		// 3. CONFIGURE modules to run in batch                          //
		//    TRUE  -> module runs in batch                              //
		//    FALSE -> module does not run in batch                      //
		//                                                               //
		// NOTE: Ensure that no module is skipped!                       //
		// If not all modules should run, the batch has to be configured //
		// to end with this module.                                      //
		// --------------------------------------------------------------//
		_run_load_csv            =false;
		_run_mapping             =false;
		_run_structuring         =false;
		_run_formatting          =false;
		_run_int_deal_handling   =false;
		_run_broker_fee_handling =false;
		_run_deal_booking        =true;

		_run_schedule_upload     =false;
	}
}
