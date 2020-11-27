package com.jm.reportbuilder.accountdata;

/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 24-Feb-2020 |               |GuptaN02         | Initial version. This report gives account activity between start date and end date
 * 															with initial open position at the open date. 
 * | 002 | 30-June-2020|               |GuptaN02         | Added changes for showing from and to account of strategy. EPI 1307 								   									   |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */


import com.matthey.utilities.ExceptionUtil;
import com.matthey.utilities.enums.EndurTranInfoField;
import com.olf.jm.reportbuilder.ReportBuilderDatasource;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.INS_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.jm.logging.Logging;

public class AccountSummaryWithOpenPosition extends ReportBuilderDatasource {
	final private String TRAN_STATUS = TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt()+","+TRAN_STATUS_ENUM.TRAN_STATUS_CLOSEOUT.toInt()+","+TRAN_STATUS_ENUM.TRAN_STATUS_MATURED.toInt();
	final private String EXCLUDED_INSTRUMENTS= INS_TYPE_ENUM.call_notice_nostro.toInt()+","+INS_TYPE_ENUM.call_notice_multi_leg_nostro.toInt();
	
	/**
	 * Gets the open position.
	 *
	 * @param accountId the account id
	 * @param startDate the start date
	 * @param accountType the account type
	 * @param accountClass the account class
	 * @return the open position
	 * @throws OException the o exception
	 */
	protected Table getOpenPosition(String accountId, String startDate, String accountType,String accountClass) throws OException
	{
		Table data = null;
		String position = accountType.equalsIgnoreCase("ext_account_id")? "(ates.settle_amount*-1)":"(ates.settle_amount)";
		try{
			String sql = "SELECT ates."+accountType+" AS account_id,acc.account_name, ates.currency_id,"
					     + " ates.delivery_type,sum("+position+") AS position \n"
						 + " FROM ab_tran_event_settle ates "
						 + " INNER JOIN ab_Tran_event abe ON ates.event_num=abe.event_num AND abe.event_date < '"+ startDate+ "' \n"
						 + " INNER JOIN account acc ON ates."+accountType+"=acc.account_id AND acc.account_id IN ( " +accountId + ") "
					     + " INNER JOIN  ab_Tran ab ON ab.tran_num=abe.tran_num \n"
					     + " AND ab.tran_status IN ("+TRAN_STATUS+") AND ab.current_flag=1  "
					     + " AND ab.ins_type NOT IN ("+EXCLUDED_INSTRUMENTS+")AND acc.account_type IN ("+accountClass+") \n"
					     + " GROUP BY acc.account_id, ates.currency_id,ates.delivery_type,acc.account_name,ates."+accountType;
				
					
			Logging.info("Running SQL: "+sql);

			data = Table.tableNew();
			DBaseTable.execISql(data, sql);
			Logging.info("SQL Executed successfully");
			return data;
		}
		catch(OException e)
		{
			ExceptionUtil.logException(e, 0);
			Logging.error("Error took place while getting open position for account: "+accountType+" upto: "+startDate+". Error is "+e.getMessage());
			throw new OException("Error took place while getting open position for account: "+accountType+" upto: "+startDate+". Error is "+e.getMessage());
		}


	}

	/**
	 * Gets the event data between given dates
	 * @param accountId
	 * @param startDate
	 * @param endDate
	 * @param accountType
	 * @param accountClass
	 * @return
	 * @throws OException
	 */
	protected Table getEventData(String accountId, String startDate,String endDate,  String accountType,String accountClass) throws OException
	{
		Table data = null;
		String position = accountType.equalsIgnoreCase("ext_account_id")? "(ates.settle_amount*-1)":"(ates.settle_amount)";
		try{
			String sql= "SELECT  ates."+accountType+" AS account_id, acc.account_name ,ates.currency_id,"
					+ " ates.delivery_type,"+position+" AS position, \n"
					+ " ab.external_bunit AS party_id,abe.event_num, abe.tran_num, abe.event_date,"
					+ " ab.ins_type, ati.value AS strategy_num,ab.reference,ati1.value as from_Acc, ati2.value as to_Acc"
					+ " FROM ab_tran_event_settle ates \n"
					+ " INNER JOIN ab_Tran_event abe ON ates.event_num=abe.event_num and abe.event_date >= '"+startDate+"' "
					+ " and abe.event_date<='"+endDate+"' INNER JOIN account acc ON ates."+accountType+"= \n"
					+ " acc.account_id AND acc.account_id IN ( " +accountId + ")"
					+ " INNER JOIN ab_Tran ab ON ab.tran_num=abe.tran_num AND ab.tran_status IN ("+TRAN_STATUS+")\n"
					+ " AND ab.current_flag=1 AND ab.ins_type NOT IN ("+EXCLUDED_INSTRUMENTS+") "
					+ " LEFT JOIN ab_tran_info ati ON ab.tran_num=ati.tran_num AND ati.type_id = "+EndurTranInfoField.STRATEGY_NUM.toInt()
					+ " AND acc.account_type IN ("+accountClass+") "
					+ " LEFT JOIN ab_tran_info ati1 on ati.value=ati1.tran_num and ati1.type_id = "+EndurTranInfoField.FROM_ACC.toInt()
					+ " LEFT JOIN ab_tran_info ati2 on ati.value=ati2.tran_num and ati2.type_id = "+EndurTranInfoField.TO_ACC.toInt();
					
			Logging.info("Running SQL: "+sql);
			data = Table.tableNew();
			DBaseTable.execISql(data, sql);
			Logging.info("SQL Executed successfully");
			return data;
		}
		catch(OException e)
		{
			ExceptionUtil.logException(e, 0);
			Logging.error("Error took place while getting event data for account: "+accountType+" from: "+startDate+" to: "+endDate+". Error is "+e.getMessage());
			throw new OException("Error took place while getting event data for account: "+accountType+" from: "+startDate+" to: "+endDate+". Error is "+e.getMessage());
		}

	}


	/* (non-Javadoc)
	 * @see com.olf.jm.reportbuilder.ReportBuilderDatasource#initialiseReturnt()
	 * Prepare structure of returnt
	 */
	@Override
	protected void initialiseReturnt() throws OException {
		try{
			returnt.addCol("account_id", COL_TYPE_ENUM.COL_INT);
			returnt.addCol("Account_Name", COL_TYPE_ENUM.COL_STRING);
			returnt.addCol("currency_id", COL_TYPE_ENUM.COL_INT);
			returnt.addCol("delivery_type", COL_TYPE_ENUM.COL_INT);
			returnt.addCol("position", COL_TYPE_ENUM.COL_DOUBLE);
			returnt.addCol("party_id", COL_TYPE_ENUM.COL_INT);
			returnt.addCol("event_num", COL_TYPE_ENUM.COL_INT64);
			returnt.addCol("tran_num", COL_TYPE_ENUM.COL_INT);
			returnt.addCol("event_Date", COL_TYPE_ENUM.COL_DATE_TIME);
			returnt.addCol("ins_type", COL_TYPE_ENUM.COL_INT);
			returnt.addCol("strategy_num", COL_TYPE_ENUM.COL_INT64);
			returnt.addCol("reference", COL_TYPE_ENUM.COL_STRING);
			returnt.addCol("from_Acc", COL_TYPE_ENUM.COL_STRING);
			returnt.addCol("to_Acc", COL_TYPE_ENUM.COL_STRING);
			
		}
		catch(OException e)
		{
			ExceptionUtil.logException(e, 0);
			Logging.error("Error took place while initialising returnt");
			throw new OException("Error took place while initialising returnt "+e.getMessage());

		}

	}

	/* (non-Javadoc)
	 * @see com.olf.jm.reportbuilder.ReportBuilderDatasource#generateOutput()
	 */
	@Override
	protected void generateOutput() throws OException {
		Table nostroOpenPosition = Util.NULL_TABLE;
		Table vostroOpenPosition = Util.NULL_TABLE;
		Table nostroEventData = Util.NULL_TABLE;
		Table vostroEventData = Util.NULL_TABLE ;
		try{
			String nostroAccountsId = Ref.getValue(SHM_USR_TABLES_ENUM.ACCOUNT_TYPE_TABLE,"Nostro")+","+Ref.getValue(SHM_USR_TABLES_ENUM.ACCOUNT_TYPE_TABLE,"Internal/Nostro");
			String vostroAccountsId= Ref.getValue(SHM_USR_TABLES_ENUM.ACCOUNT_TYPE_TABLE,"Vostro")+","+Ref.getValue(SHM_USR_TABLES_ENUM.ACCOUNT_TYPE_TABLE,"Internal/Vostro")+","+Ref.getValue(SHM_USR_TABLES_ENUM.ACCOUNT_TYPE_TABLE,"Vostro (Multiple)");
			String accountId=getParameterValue("Account_Name");
			String startDate= getParameterValue("Start_Date");
			String endDate= getParameterValue("End_Date");
			if(accountId.isEmpty() || startDate.isEmpty() || endDate.isEmpty())
			{
				Logging.error("Could not find one of the parameters: AccountId,StartDate,EndDate");
				throw new OException("Could not find one of the parameters: AccountId,StartDate,EndDate");
			}

			Logging.info("Report will run for accounts: " +accountId+" from start date: "+startDate+" to end date: "+endDate);

			Logging.debug("Started: Fetching Opening Position for Nostro Accounts");
			nostroOpenPosition = getOpenPosition(accountId,startDate,"int_account_id",nostroAccountsId);
			Logging.debug("Finished: Fetching Opening Position for Nostro Accounts");
			Logging.debug("Started: Fetching Opening Position for Vostro Accounts");
			vostroOpenPosition= getOpenPosition(accountId,startDate,"ext_account_id",vostroAccountsId);
			Logging.debug("Finished: Fetching Opening Position for Vostro Accounts");
			Logging.debug("Started: Fetching Event Data for Nostro Accounts");
			nostroEventData = getEventData(accountId, startDate, endDate, "int_account_id",nostroAccountsId);
			Logging.debug("Finished: Fetching Event Data for Nostro Accounts");
			Logging.debug("Started: Fetching Event Data for Vostro Accounts");
			vostroEventData = getEventData(accountId, startDate, endDate, "ext_account_id",vostroAccountsId);
			Logging.debug("Finished: Fetching Event Data for Vostro Accounts");
			returnt.select(nostroOpenPosition, "*", "account_id GT 0");
			returnt.select(vostroOpenPosition, "*", "account_id GT 0");
			returnt.select(nostroEventData, "*", "account_id GT 0");
			returnt.select(vostroEventData, "*", "account_id GT 0");
			Logging.info("Final Data is prepared successfully");
			
			
		}
		catch(OException e)
		{
			ExceptionUtil.logException(e, 0);
			Logging.error("Error took place while processing data: "+e.getMessage());
			throw new OException("Error took place while processing data: "+e.getMessage());
		}
		finally{
			if(Table.isTableValid(nostroOpenPosition)==1)
			nostroOpenPosition.destroy();
			if(Table.isTableValid(vostroOpenPosition)==1)
			vostroOpenPosition.destroy();
			if(Table.isTableValid(nostroEventData)==1)
			nostroEventData.destroy();
			if(Table.isTableValid(vostroEventData)==1)
			vostroEventData.destroy();
		}
		
	}


}
