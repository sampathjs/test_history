package com.jm.reportbuilder.prices;


import java.io.File;
import java.util.Date;

import com.olf.openjvs.Ask;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.EmailMessage;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.ReportBuilder;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openjvs.enums.ASK_TEXT_DATA_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.EMAIL_MESSAGE_TYPE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;

import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.misc.TableUtilities;

public class RunJMBasePriceAllRegionReport_P implements IScript{

	@Override
	public void execute(IContainerContext context) throws OException {

		Table tblArgt=context.getArgumentsTable();

		try {

			setupLog();
			
			PluginLog.info("Start Script");
			
			askUser(tblArgt); 
			PluginLog.info("End Script");
			
		} catch (Throwable ex) {
			OConsole.oprint(ex.toString());
			PluginLog.error(ex.toString());
			throw ex;
		} finally {
			
		}
	}

	

	private void askUser(Table tblArgt) throws OException{
		
		tblArgt.addCol("metal_selected", COL_TYPE_ENUM.COL_STRING);
		tblArgt.addCol("ref_src_selected", COL_TYPE_ENUM.COL_STRING);
		tblArgt.addCol("user_exit", COL_TYPE_ENUM.COL_INT);

		
		String strSQL;
		
		strSQL = "SELECT name FROM currency where precious_metal = 1";
		Table tblMetal = Table.tableNew();
		DBaseTable.execISql(tblMetal, strSQL);
		
		
		strSQL = "SELECT name FROM ref_source WHERE name in ('JM London Opening','JM HK Opening','JM HK Closing','JM NY Opening','LME AM','LME PM','LBMA AM','LBMA Silver')";
		Table tblRefSrc = Table.tableNew();
		DBaseTable.execISql(tblRefSrc, strSQL);
		

		try {
			
			Table tblAsk = Table.tableNew();
			
			Ask.setAvsTable(tblAsk, tblMetal,"Metal", 1, ASK_SELECT_TYPES.ASK_MULTI_SELECT.jvsValue(), 1);
			
			Ask.setAvsTable(tblAsk, tblRefSrc,"RefSrc", 1, ASK_SELECT_TYPES.ASK_MULTI_SELECT.jvsValue(), 1);
			
			int ret = Ask.viewTable(tblAsk, "JM Base Price Report", "");
			
			if (ret <= 0) {
				PluginLog.info("User pressed cancel. Aborting...\n");
				Ask.ok("You cancelled execution of the task.");

				tblArgt.setInt("user_exit", 0, 1);
				
				tblAsk.destroy();
				
			}	
			
			Table tblMetalSelected = tblAsk.getTable("return_value", 1);
			String strMetalSelected = "";
			for(int i = 1;i<=tblMetalSelected.getNumRows();i++){
				
				if(  i == 1){
					strMetalSelected += tblMetalSelected.getString("return_val",i);
				}else{
					strMetalSelected += "," + tblMetalSelected.getString("return_val",i);
				}
			}

			Table tblRefSrcSelected = tblAsk.getTable("return_value", 2);
			String strRefSrcSelected = "";
			for(int i = 1;i<=tblRefSrcSelected.getNumRows();i++){
				
				if( i == 1){
					strRefSrcSelected += tblRefSrcSelected.getString("return_val",i);
				}else{
					strRefSrcSelected += "," + tblRefSrcSelected.getString("return_val",i);
				}
			}

			tblArgt.addRow();
			
			tblArgt.setString("metal_selected", 1, strMetalSelected);
			tblArgt.setString("ref_src_selected", 1, strRefSrcSelected);
			
			tblMetal.destroy();
			tblRefSrc.destroy();

			tblAsk.destroy();
			
		} catch (OException e) {
			throw new RuntimeException (e);
		}
		
	}	

	

	protected void setupLog() throws OException
	{
		String abOutDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";
		String logDir = abOutDir;

		ConstRepository constRepo = new ConstRepository("Reports", "");
		String logLevel = constRepo.getStringValue("logLevel");

		try
		{

			if (logLevel == null || logLevel.isEmpty())
			{
				logLevel = "DEBUG";
			}
			String logFile = "RunAllRegionJMBasePriceReport_P.log";
			PluginLog.init(logLevel, logDir, logFile);

		}

		catch (Exception e)
		{
			String errMsg = this.getClass().getSimpleName() + ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}

		PluginLog.info("**********" + this.getClass().getName() + " started **********");
	}
	
}
