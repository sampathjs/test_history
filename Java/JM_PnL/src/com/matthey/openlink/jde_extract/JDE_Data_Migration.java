package com.matthey.openlink.jde_extract;

import com.matthey.openlink.pnl.ConfigurationItemPnl;
import com.olf.openjvs.DBase;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Str;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.openlink.util.logging.PluginLog;

/*
 *History: 
 * 201x-xx-xx	V1.0     	- initial version	
 * 2017-11-16	V1.1	lma	- execute more than constants repository variable minDealNum
 *
 */
public class JDE_Data_Migration implements IScript 
{

	@Override
	/**
	 * When run, this will, for all valid relevant trades, delete any existing entries in USER_jm_jde_extract_data, 
	 * and re-create them 
	 */
	public void execute(IContainerContext context) throws OException 
	{
		initPluginLog();
		
		Table tranNums = createTransactionList();

		JDE_Data_Manager dataManager = new JDE_Data_Manager();
		dataManager.processDeals(tranNums);
		
		tranNums.destroy();
	}

	/**
	 * Creates a list of relevant transaction numbers - all new and validated current FX, ComSwap and ComFut deals (Trading type only)
	 * @return
	 * @throws OException
	 */
	private Table createTransactionList() throws OException 
	{
		int minimalDealNum = 0;
		String strMinDealNum = ConfigurationItemPnl.MIN_DEAL_NUM.getValue();
		try {
			minimalDealNum = Str.strToInt(strMinDealNum);
		} catch (OException e1) {
			e1.printStackTrace();
		}
				
		Table tranNums = new Table("Tran Nums");
		String sql = "SELECT tran_num FROM ab_tran WHERE toolset in (9, 15, 17) AND tran_status in (2,3) AND tran_type = 0 AND current_flag = 1 "
				+ " AND deal_tracking_num >= " + minimalDealNum;
		
		DBase.runSqlFillTable(sql, tranNums);
		
		return tranNums;
	}

	/**
	 * Initialise standard Plugin log functionality
	 * @throws OException
	 */
	private void initPluginLog() throws OException 
	{	
		String abOutdir =  SystemUtil.getEnvVariable("AB_OUTDIR");
		String logLevel = ConfigurationItemPnl.LOG_LEVEL.getValue();
		String logFile = ConfigurationItemPnl.LOG_FILE.getValue();
		String logDir = ConfigurationItemPnl.LOG_DIR.getValue();
		if (logDir.trim().isEmpty()) 
		{
			logDir = abOutdir + "\\error_logs";
		}
		if (logFile.trim().isEmpty()) 
		{
			logFile = this.getClass().getName() + ".log";
		}
		try 
		{
			PluginLog.init(logLevel, logDir, logFile);
		} 
		catch (Exception e) 
		{
			throw new RuntimeException (e);
		}
		PluginLog.info("Plugin: " + this.getClass().getName() + " started.\r\n");
	}	
}
