package com.openlink.esp.migration.app.mtc;

public class Migr_ProcessDef_52_P extends Migr_INC_ProcessDef
{
	public Migr_ProcessDef_52_P()
	{
		// --------------------------------------------------------------//
		// 1. ENTER a valid import definition id                         //
		// --------------------------------------------------------------//
		_import_definition=52;

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
		_run_load_csv            =true;
		_run_mapping             =true;
		_run_structuring         =false;
		_run_formatting          =false;
		_run_int_deal_handling   =false;
		_run_broker_fee_handling =false;
		_run_deal_booking        =false;
		_run_deal_updating       =false;

		_run_schedule_upload     =true;
	}
}
