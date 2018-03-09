package com.openlink.esp.migration.app.mtc;



public class Migr_ProcessDef_107_P extends Migr_INC_ProcessDef
{
	public Migr_ProcessDef_107_P()
	{
		// --------------------------------------------------------------//
		// 1. ENTER a valid import definition id                         //
		// --------------------------------------------------------------//
		_import_definition=107;

		// --------------------------------------------------------------//
		// 2. ENABLE/DISABLE silent mode                                 //
		//                                                               //
		//    FALSE -> default show all process logs                     //
		//    TRUE - > do NOT show any process logs                      //
		// --------------------------------------------------------------//
		_silent_mode=true;

		// --------------------------------------------------------------//
		// 3. CONFIGURE modules to run in batch                          //
		//    TRUE  -> module runs in batch                              //
		//    FALSE -> module does not run in batch                      //
		//                                                               //
		// NOTE: Ensure that no module is skipped!                       //
		// If not all modules should run, the batch has to be configured //
		// to end with this module.                                      //
		// --------------------------------------------------------------//
		_run_mapping             =false;
		_run_deal_booking        =true;
	}
}
