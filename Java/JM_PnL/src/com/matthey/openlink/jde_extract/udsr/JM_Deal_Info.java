package com.matthey.openlink.udsr;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import com.olf.openjvs.DBase;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.BUY_SELL_ENUM;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.USER_RESULT_OPERATIONS;
//import com.openlink.util.logging.PluginLog;
//import com.matthey.openlink.pnl.ConfigurationItemPnl;

/*
 * History:
 * 2018-02-28	V1.0	mtsteglov	- Initial Version
 */

/**
 * Main Plugin for JM Deal Info Result
 * @author msteglov
 * @version 1.0
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
public class JM_Deal_Info implements IScript 
{	

	public void execute(IContainerContext context) throws OException 
	{
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();

		// initPluginLog();

		USER_RESULT_OPERATIONS op = USER_RESULT_OPERATIONS.fromInt(argt.getInt("operation", 1));
		try 
		{
			switch (op) 
			{
			case USER_RES_OP_CALCULATE:
				calculate(argt, returnt);
				break;
			case USER_RES_OP_FORMAT:
				format(argt, returnt);				
				break;
			}
			// PluginLog.info("Plugin " + this.getClass().getName() + " finished successfully.\n");
		} 
		catch (Exception e) 
		{
			// PluginLog.error(e.toString());
			for (StackTraceElement ste : e.getStackTrace()) {
				// PluginLog.error(ste.toString());
			}
			OConsole.message(e.toString() + "\r\n");
			// PluginLog.error("Plugin " + this.getClass().getName() + " failed.\n");
			// throw e;
		} 
	}
	
	// Store deal-level information about location and form of goods
	static class DealInfo
	{
		int m_dealNum = -1;
		int m_buySell = -1;
		int m_tranGroup = -1;
		String m_loco;
		String m_form;
	}
	
	// store tran group-level information about "from" and "to" movement accounts
	static class TranGroupInfo
	{
		int m_tranGroup = -1;
		int m_fromAccount = -1;
		int m_toAccount = -1;
		
		public TranGroupInfo(int tranGroup)
		{
			m_tranGroup = tranGroup;
		}
	}
	
	static class AccountInfo
	{
		int m_accountID = -1;
		String m_accountName;
		String m_loco;
		String m_form;		
		
		public AccountInfo(int accountID, String accountName, String loco, String form)
		{
			m_accountID = accountID;
			m_accountName = accountName;
			m_loco = loco;
			m_form = form;
		}
	}


	/**
	 * Calculates the UDSR output for a set of transctions given
	 * @param argt
	 * @param returnt
	 * @throws OException
	 */
	protected void calculate(Table argt, Table returnt) throws OException 
	{
		setOutputFormat(returnt);
		
		Table transactions = argt.getTable("transactions", 1);
		
		Vector<DealInfo> dealData = new Vector<DealInfo>();
		Vector<DealInfo> cashDealData = new Vector<DealInfo>();
		
		// For each deal, create a DealInfo entry - some are processed immediately, 
		// others (Cash toolset) get processed in bulk later
		for (int row = 1; row <= transactions.getNumRows(); row++)
		{
			DealInfo dealInfo = new DealInfo();
			
			int dealNum = transactions.getInt("deal_num", row);
			Transaction trn = transactions.getTran("tran_ptr", row);					
			
			int tranGroup = trn.getFieldInt(TRANF_FIELD.TRANF_TRAN_GROUP.toInt());
			int toolset = trn.getFieldInt(TRANF_FIELD.TRANF_TOOLSET_ID.toInt());
			int buySell = trn.getFieldInt(TRANF_FIELD.TRANF_BUY_SELL.toInt());
			
			dealInfo.m_dealNum = dealNum;
			dealInfo.m_tranGroup = tranGroup;		
			dealInfo.m_buySell = buySell;
			
			if (toolset == TOOLSET_ENUM.CALL_NOTICE_TOOLSET.toInt())
			{
				setDealInfoForCallNotice(dealInfo, trn);
				dealData.add(dealInfo);
			}
			else if (toolset == TOOLSET_ENUM.CASH_TOOLSET.toInt())
			{
				cashDealData.add(dealInfo);
			}
			else
			{
				setDealInfoFromTranInfoFields(dealInfo, trn);
				dealData.add(dealInfo);
			}			
		}
		
		// Now that all Cash deals have been identified, process in bulk
		processCashDeals(cashDealData);
		
		// Add populated deal info arrays to output table
		addDealInfoListToTable(dealData, returnt);
		addDealInfoListToTable(cashDealData, returnt);
		
		// Set default values for 
		setDefaultValues(returnt);
	}
	
	private final static String S_LOCO_DEFAULT_VALUE = "None";
	private final static String S_FORM_DEFAULT_VALUE = "None";
	
	private void setDefaultValues(Table data) throws OException 
	{
		for (int row = 1; row <= data.getNumRows(); row++)
		{
			String loco = data.getString("loco", row);
			if (loco.trim().length() == 0)
			{
				data.setString("loco", row, S_LOCO_DEFAULT_VALUE);
			}
			
			String form = data.getString("form", row);
			if (form.trim().length() == 0)
			{
				data.setString("form", row, S_FORM_DEFAULT_VALUE);
			}
		}		
	}

	/**
	 * Given a set of Cash deals, get their Tran Grop Info, and set DealInfo accordingly
	 * @param cashDealData
	 * @throws OException
	 */
	private void processCashDeals(Vector<DealInfo> cashDealData) throws OException
	{
		// Skip processing if no cash deals present
		if (cashDealData.size() < 1)
			return;
		
		int queryID = getTranGroupQueryID(cashDealData);
		
		HashMap<Integer, TranGroupInfo> tranGroupData = getTranGroupData(queryID);
		
		setDealInfoFromTranGroupInfo(cashDealData, tranGroupData);
		
		Query.clear(queryID);		
	}
	
	/**
	 * For each DealInfo, identify its relevant tran group's From or To account, and populate Loco and Form
	 * from that account's properties
	 * @param cashDealData
	 * @param tranGroupData
	 * @throws OException
	 */
	private void setDealInfoFromTranGroupInfo(Vector<DealInfo> cashDealData, HashMap<Integer, TranGroupInfo> tranGroupData) throws OException 
	{
		for (DealInfo dealInfo : cashDealData)
		{
			// Retrieve corresponding TranGroupInfo for this deal
			if (!tranGroupData.containsKey(dealInfo.m_tranGroup))
				continue;
			
			TranGroupInfo tgInfo = tranGroupData.get(dealInfo.m_tranGroup);
			
			// Retrieve relevant account, based on whether the deal is a buy or a sell
			int accountID = (dealInfo.m_buySell == BUY_SELL_ENUM.BUY.toInt()) ? tgInfo.m_toAccount : tgInfo.m_fromAccount;			
			String accountName = Ref.getName(SHM_USR_TABLES_ENUM.ACCOUNT_TABLE, accountID);
			
			// Set relevant properties based on account name
			setDealInfoFromAccountInfo(dealInfo, accountName);
		}		
	}

	/**
	 * Given a query ID of tran groups, retrieve a map of TranGroupInfo, with an entry per each tran group of interest
	 * @param queryID
	 * @return
	 * @throws OException 
	 */
	private HashMap<Integer, TranGroupInfo> getTranGroupData(int queryID) throws OException 
	{
		HashMap<Integer, TranGroupInfo> output = new HashMap<Integer, TranGroupInfo>();
		Table tranGroupData = new Table("tranGroupData");
		
		String sql = 
			"select ab.deal_tracking_num deal_num, ab.tran_group, c.xfer_component_type type, c.account_id " +
			"from ab_tran ab, cash_xfer_component_data c, query_result qr " +
			"where " +
				"ab.current_flag = 1 and ab.tran_num = c.tran_num and " +
				"ab.tran_group = qr.query_result and qr.unique_id = " + queryID;
		
		DBase.runSqlFillTable(sql, tranGroupData);
		
		for (int row = 1; row <= tranGroupData.getNumRows(); row++)
		{
			int tranGroup = tranGroupData.getInt("tran_group", row);
			int type = tranGroupData.getInt("type", row);
			int accountID = tranGroupData.getInt("account_id", row);
			
			if (!output.containsKey(tranGroup))
			{
				output.put(tranGroup, new TranGroupInfo(tranGroup));
			}
			
			TranGroupInfo tgInfo = output.get(tranGroup);
			
			if ((type == 0) || (type == 2))
			{
				// 0 and 2 are FROM and FROM_LINKED account types
				tgInfo.m_fromAccount = accountID;
			}
			else if ((type == 1) || (type == 3))
			{
				// 1 and 3 are TO and TO_LINKED account types
				tgInfo.m_toAccount = accountID;
			}
		}		
		
		tranGroupData.destroy();
		return output;
	}
	
	/**
	 * From a set of deals, get a query ID with their unique tran group ID's
	 * @param cashDealData
	 * @return
	 * @throws OException
	 */
	private int getTranGroupQueryID(Vector<DealInfo> cashDealData) throws OException
	{
		HashSet<Integer> tranGroupList = new HashSet<Integer>();
		for (DealInfo dealInfo : cashDealData)
		{
			tranGroupList.add(dealInfo.m_tranGroup);
		}
		
		Table tranGroupTable = new Table("tranGroupTable");
		tranGroupTable.addCol("tran_group", COL_TYPE_ENUM.COL_INT);
		for (int tranGroup : tranGroupList)
		{
			tranGroupTable.addRow();
			int row = tranGroupTable.getNumRows();
			tranGroupTable.setInt("tran_group", row, tranGroup);
		}
		
		int queryID = Query.tableQueryInsert(tranGroupTable, "tran_group");
		
		tranGroupTable.destroy();
		
		return queryID;
	}

	// Store mapping of account names to AccountInfo data
	private static HashMap<String, AccountInfo> s_accountInfoMap = new HashMap<String, AccountInfo>();
	
	/**
	 * Pre-loads the static account information (Loco and Form)
	 * @throws OException
	 */
	private static void loadAccountInfo() throws OException
	{
		Table output = new Table("Account Info");
		
		// Retrieve via SQL for all accounts
		String sql = "select a.account_name, a.account_id, loco.loco, form.form from account a " + 
				"left outer join " +
					"(select ai.account_id, ai.info_value loco from account_info ai, account_info_type ait " +
						"where ai.info_type_id = ait.type_id and ait.type_name = 'Loco') loco " +
					"on a.account_id = loco.account_id " +
				"left outer join " +
					"(select ai.account_id, ai.info_value form from account_info ai, account_info_type ait " +
						"where ai.info_type_id = ait.type_id and ait.type_name = 'Form') form " +
					"on a.account_id = form.account_id ";

		DBase.runSqlFillTable(sql, output);
		
		// Parse table into a HashMap structure of name to "account info"
		for (int row = 1; row <= output.getNumRows(); row++)
		{			
			int accountID = output.getInt("account_id", row);
			String accountName = output.getString("account_name", row);
			String loco = output.getString("loco", row);
			String form = output.getString("form", row);
			
			s_accountInfoMap.put(accountName, new AccountInfo(accountID, accountName, loco, form));
		}
		
		output.destroy();
	}
	
	/**
	 * Given a deal info object and account name, populates that deal's Loco and Form from the account's values
	 * @param dealInfo
	 * @param account
	 * @throws OException
	 */
	private void setDealInfoFromAccountInfo(DealInfo dealInfo, String account) throws OException 
	{
		if (s_accountInfoMap.isEmpty())
		{
			loadAccountInfo();
		}
		
		if (s_accountInfoMap.containsKey(account))
		{
			AccountInfo accountInfo = s_accountInfoMap.get(account);
			
			dealInfo.m_loco = accountInfo.m_loco;
			dealInfo.m_form = accountInfo.m_form;
		}
	}

	/**
	 * For a Call Notice deal, retrieves its account name, and sets DealInfo's Loco / Form from it
	 * @param dealInfo
	 * @param trn
	 * @throws OException
	 */
	private void setDealInfoForCallNotice(DealInfo dealInfo, Transaction trn) throws OException 
	{
		String account = trn.getField(TRANF_FIELD.TRANF_BOOK.toInt());
		
		setDealInfoFromAccountInfo(dealInfo, account);
	}

	/**
	 * Given a DealInfo object, adds a row to output table with its data
	 * @param dealData
	 * @param output
	 * @throws OException
	 */
	private void addDealInfoListToTable(Vector<DealInfo> dealData, Table output) throws OException 
	{
		for (DealInfo dealInfo: dealData)
		{
			output.addRow();
			int row = output.getNumRows();
			
			output.setInt("deal_num", row, dealInfo.m_dealNum);
			output.setString("loco", row, dealInfo.m_loco);
			output.setString("form", row, dealInfo.m_form);
		}
	}

	/**
	 * Sets the deal info fields "Loco" and "Form" from the appropriate Tran Info fields on the deal
	 * @param dealInfo
	 * @param trn
	 * @throws OException
	 */
	private void setDealInfoFromTranInfoFields(DealInfo dealInfo, Transaction trn) throws OException 
	{
		String form = trn.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Form");		
		if (form != null)
		{
			dealInfo.m_form = form;
		}
		
		String loco = trn.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Loco");
		if (loco != null)
		{
			dealInfo.m_loco = loco;
		}
	}

	/**
	 * Format the table for GUI consumption
	 * @param argt
	 * @param output
	 * @throws OException
	 */
	protected void format(Table argt, Table output) throws OException 
	{	
		output.setColTitle("deal_num", "Deal Number");
		output.setColTitle("loco", "Location");
		output.setColTitle("form", "Form");
	}	

	/**
	 * Sets the expected output (returnt) table format
	 * @param output
	 * @throws OException
	 */
	protected void setOutputFormat(Table output) throws OException
	{
		output.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		output.addCol("loco", COL_TYPE_ENUM.COL_STRING);
		output.addCol("form", COL_TYPE_ENUM.COL_STRING);
	}
	
	/*
	private void initPluginLog() throws OException {	
		String abOutdir =  SystemUtil.getEnvVariable("AB_OUTDIR");
		String logLevel = ConfigurationItemPnl.LOG_LEVEL.getValue();
		String logFile = ConfigurationItemPnl.LOG_FILE.getValue();
		String logDir = ConfigurationItemPnl.LOG_DIR.getValue();
		
		if (logDir.trim().equals("")) 
		{
			logDir = abOutdir + "\\error_logs";
		}
		
		if (logFile.trim().equals("")) 
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
		
		PluginLog.info("**********" + this.getClass().getName() + " started **********.\n");
	}
*/
}

