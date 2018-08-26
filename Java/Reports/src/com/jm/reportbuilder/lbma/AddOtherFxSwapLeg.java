package com.jm.reportbuilder.lbma;


import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
/*
 * History:
 * 
 */

/**
 *  
 * @author 
 * @version 1.0
 */

public class AddOtherFxSwapLeg implements IScript {

	@Override
	public void execute(IContainerContext context) throws OException {
	
		Table tblFxOtherLeg = Table.tableNew();
		
		try
		{
			
			setupLog();
			
			PluginLog.info("Starts  " + getClass().getSimpleName() + " ...");
			
			Table tblArgt = context.getArgumentsTable();
			int intQid = tblArgt.getInt("QueryId", 1);
			
			String strSQL;
			
			strSQL = "select \n";
			strSQL += "ab_other.tran_num \n";
			strSQL += "from \n";
			strSQL += "query_result64_dxr qr \n";
			strSQL += "inner join ab_tran ab_this on ab_this.tran_num = qr.query_result and ab_this.cflow_type in (37,113,114) \n";
			strSQL += "inner join ab_tran ab_other on ab_this.tran_group = ab_other.tran_group and ab_this.tran_num != ab_other.tran_num \n";
			strSQL += "where \n";
			strSQL += "qr.unique_id = " + intQid;
			strSQL += "and ab_other.tran_num not in (select qr.query_result from query_result64_dxr qr where qr.unique_id = " + intQid + "\n)";


			DBaseTable.execISql(tblFxOtherLeg, strSQL);
			
			if(tblFxOtherLeg.getNumRows() > 0 ){

				PluginLog.info("Found " + tblFxOtherLeg.getNumRows() + " other fx legs");
				
				for(int i=1;i<=tblFxOtherLeg.getNumRows();i++){
					
					int intOtherFXLegTranNum = tblFxOtherLeg.getInt("tran_num",i);
					PluginLog.info("Adding other leg tran " +intOtherFXLegTranNum);

					Query.insert(intQid, intOtherFXLegTranNum);	
				}
			}
			
			
			PluginLog.info("End  " + getClass().getSimpleName() + " ...");
			
		}
		catch (Exception e)
		{
			String errMsg = e.toString();
			com.olf.openjvs.Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}
		finally{
			
			if(Table.isTableValid(tblFxOtherLeg)==1){tblFxOtherLeg.destroy();}
		}
	

		
	}	

	
	
	protected void setupLog() throws OException {
		String abOutDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\Logs\\";
		String logDir = abOutDir;
		
		try {
			ConstRepository constRepo = new ConstRepository("Reports", "LBMA");
			
			String logLevel = constRepo.getStringValue("logLevel", "DEBUG");
			String logFile = constRepo.getStringValue("logFile", "LBMA_Report.log");
			
			PluginLog.init(logLevel, logDir, logFile);
			
		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName()+ ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}

		PluginLog.info("**********" + this.getClass().getName() + " started **********");
	}

}
