package com.matthey.openlink.jde_extract;

import com.matthey.openlink.pnl.ConfigurationItemPnl;
import com.olf.openjvs.DBase;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Str;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.jm.logging.Logging;
import com.openlink.util.constrepository.*;

/*
 *History: 
 * 201x-xx-xx	V1.0     	- initial version	
 * 2017-11-16	V1.1	lma	- execute more than constants repository variable minDealNum
 * 2020-02-18   V1.2    agrawa01 - memory leaks & formatting changes
 */

public class JDE_Data_Migration implements IScript {

	/** The Constant CONST_REPOSITORY_CONTEXT. */
	private String CONST_REPOSITORY_CONTEXT = "PNL";

	/** The Constant CONST_REPOSITORY_SUBCONTEXT. */
	private String CONST_REPOSITORY_SUBCONTEXT = "PNL_Handle_Intrday_Fixings";

	
	@Override
	/**
	 * When run, this will, for all valid relevant trades, delete any existing entries in USER_jm_jde_extract_data, 
	 * and re-create them 
	 */
	public void execute(IContainerContext context) throws OException {
		initLogging();
		Table tranNums = Util.NULL_TABLE;
		
		try {
			tranNums = createTransactionList();
			JDE_Data_Manager dataManager = new JDE_Data_Manager();
			dataManager.processDeals(tranNums);
			
		} finally {
			Logging.close();
			if (Table.isTableValid(tranNums) == 1) {
				tranNums.destroy();
			}
		}
		
	}

	/**
	 * Creates a list of relevant transaction numbers - all new and validated current FX, ComSwap and ComFut deals (Trading type only)
	 * @return
	 * @throws OException
	 */
	private Table createTransactionList() throws OException {
		int minimalDealNum = 0;
		String strMinDealNum = ConfigurationItemPnl.MIN_DEAL_NUM.getValue();
		
		try {
			minimalDealNum = Str.strToInt(strMinDealNum);
		} catch (OException e1) {
			e1.printStackTrace();
		}
				
		Table tranNums = new Table("Tran Nums");
		
		// Changes for exclusion of base metals on 01Nov 18 and enhanced for exception handling
		ConstRepository constRep = new ConstRepository(CONST_REPOSITORY_CONTEXT, CONST_REPOSITORY_SUBCONTEXT);
		String excludedPortfolios = constRep.getStringValue("Excluded_Portfolios");
		
		String sql = "SELECT ab.tran_num FROM ab_tran ab "
				+ "JOIN portfolio p on p.id_number=ab.internal_portfolio and p.name not in("
				+ excludedPortfolios
				+ ")"
				+ "WHERE ab.toolset in(9,15,17) "
				+ " AND ab.tran_status in (2,3) "
				+ " AND ab.current_flag = 1"
				+ " AND ab.deal_tracking_num >= " + minimalDealNum;

				try {
					DBase.runSqlFillTable(sql, tranNums);

				}

				catch (OException e) {

					Logging.info("The sql statement failed to execute "
							+ e.getMessage());
					throw new OException("The sql statement failed to execute "
							+ e.getMessage());

				}
		
		
		
		
		
		try {
			DBase.runSqlFillTable(sql, tranNums);
		} catch (OException e) {
			Logging.error("The sql statement failed to execute "+ e.getMessage());
			throw new OException("The sql statement failed to execute "+ e.getMessage());
		}
		
		return tranNums;
	}

	/**
	 * Initialise standard Plugin log functionality
	 * @throws OException
	 */
	private void initLogging() throws OException 
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
			Logging.init( this.getClass(), ConfigurationItemPnl.CONST_REP_CONTEXT, ConfigurationItemPnl.CONST_REP_SUBCONTEXT);
		} 
		catch (Exception e) 
		{
			throw new RuntimeException (e);
		}
		Logging.info("Plugin: " + this.getClass().getName() + " started.\r\n");
	}	
}
