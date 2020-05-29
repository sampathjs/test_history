package com.openlink.jm.bo;

/**
 * Project:  D377 - JM-Invoices
 * 
 * Description:
 * - This script loads Invoice related additional data for Metal into the event table (BO blotter).
 * 
 * Revision History:
 *  13.11.15  jbonetzk  initial version
 *  10.05.16  jwaechter fixed data retrieval and join in first SQL
 *                      added retrieval of tran_unit from volume unit of the leg of the event  for certain toolsets
 *  16.09.16  jneufert  change the retrieval of Tax Type and Tax Sub Type from tran level to event level
 *  14.05.18  sma       For metal account remove ins_para_seq_num from where match criteria
 *  06.01.20  GuptaN02	Added functionality to report invoices in local currency
 *  12.02.20  kumarh02	Added logging for time taken by various queries and Formating the queries.
 */


import java.util.HashMap;
import java.util.HashSet;

import com.matthey.utilities.enums.EndurTranInfoField;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.IDX_GROUP_ENUM;
import com.olf.openjvs.enums.IDX_STATUS_ENUM;
import com.olf.openjvs.enums.IDX_UNIT_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.constrepository.ConstantNameException;
import com.olf.jm.logging.Logging;

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_DATALOAD)
@com.olf.openjvs.ScriptAttributes(allowNativeExceptions=false)
public class JM_DL_Metal implements IScript {
	
	final String ACCT_CLASS_METAL = "Metal Account"; // TODO ask ConstRepo
	final String ACCT_CLASS_CASH  = "Cash Account";
	final String ACCT_TYPE_NOSTRO = "Nostro";
	protected final static int OLF_RETURN_SUCCEED = OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt();

	// default log level - optionally overridden by ContRepo value
	private final String defaultLogLevel = "warn";

	private final String
	  CONST_REPO_CONTEXT        = "BackOffice"
	, CONST_REPO_SUBCONTEXT     = "Dataload Metal"
	, CONST_REPO_VAR_LOGLEVEL   = "logLevel"
	, CONST_REPO_VAR_LOGFILE    = "logFile"
	, CONST_REPO_VAR_LOGDIR     = "logDir"
	, CONST_REPO_VAR_VIEWTABLES = "viewTablesInDebugMode"
	, ARGT_COL_NAME_TRAN_UNIT   = "tran_unit"
	, ARGT_COL_NAME_DEAL_UNIT   = "deal_unit"
	, ARGT_COL_NAME_EXT_METAL_ACCOUNT  = "ext_metal_account"
	, ARGT_COL_NAME_INT_METAL_ACCOUNT  = "int_metal_account"
	, ARGT_COL_TITLE_EXT_METAL_ACCOUNT = "CP Metal\nAccount"
	, ARGT_COL_TITLE_DEAL_UNIT   = "Deal\nUnit"
	, ARGT_COL_TITLE_INT_METAL_ACCOUNT = "Our Metal\nAccount"
	, ARGT_COL_NAME_TAX_TYPE     = "tax_tran_type"
	, ARGT_COL_NAME_TAX_SUBTYPE  = "tax_tran_subtype"
	, ARGT_COL_TITLE_TAX_TYPE     = "Tax Type"
	, ARGT_COL_TITLE_TAX_SUBTYPE  = "Tax Subtype"
	, ARGT_COL_NAME_FX_RATE       = "event_info_type_20005"
	, ARGT_COL_NAME_APPLY_EXT_FX_RATE = "stldoc_info_type_20002"
	, TRAN_INFO_JM_FX_RATE_NAME       = "JM FX Rate"
	,ARGT_COL_NAME_PYMT_CURRENCY ="Local Currency"
	,ARGT_COL_NAME_PYMT_AMOUNT="Local Currency Amount"
	,ARGT_COL_NAME_PYMT_ACCOUNT="Local Currency Account";

	// frequently used constants:
	ConstRepository _constRepo = null;
	boolean _viewTables = false;
	String _logLevel = "info"
	     , _logFile = getClass().getSimpleName()+".log"
	     , _logDir = null
	     ;

	public void execute(IContainerContext context) throws OException {
		
		// measure execution time
		long start = System.currentTimeMillis();

		// repository
		_constRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
		

		// logging
		tryRetrieveLogSettingsFromConstRepo(defaultLogLevel, null, getClass().getSimpleName() + ".log");
		try { 
			Logging.init(this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT); 
		} catch (Exception e) { 
			OConsole.oprint(e.getMessage()); 
		}

		// main process
		try { 
			process(context); 
		} catch (Exception e) { 
			Logging.error("Exception: " + e.getMessage());  
		} finally { 
			Logging.info("Done in " + (System.currentTimeMillis()-start) + " ms"); 
			Logging.close();
		}
	}

	boolean tryRetrieveLogSettingsFromConstRepo(String logLevel, String logDir, String logFile) {
		
		try {
			boolean viewTablesInDebugMode = false;
			if (_constRepo != null) {
				logLevel = _constRepo.getStringValue(CONST_REPO_VAR_LOGLEVEL, logLevel);
				logFile  = _constRepo.getStringValue(CONST_REPO_VAR_LOGFILE, logFile);
				logDir   = _constRepo.getStringValue(CONST_REPO_VAR_LOGDIR, logDir);
				viewTablesInDebugMode = _constRepo.getStringValue(CONST_REPO_VAR_VIEWTABLES, "no").equalsIgnoreCase("yes");
			}
			_logLevel = logLevel;
			_logFile  = logFile;
			_logDir   = logDir;
			_viewTables = viewTablesInDebugMode ;
			return true;
		} catch (Exception e) { 
			return false; 
		}
	}

	private void process(IContainerContext context) throws OException {
		Table argt = context.getArgumentsTable();

		final COL_TYPE_ENUM COL_TYPE_STRING = COL_TYPE_ENUM.COL_STRING;
		argt.addCol(ARGT_COL_NAME_INT_METAL_ACCOUNT, COL_TYPE_STRING, ARGT_COL_TITLE_INT_METAL_ACCOUNT);
		argt.addCol(ARGT_COL_NAME_EXT_METAL_ACCOUNT, COL_TYPE_STRING, ARGT_COL_TITLE_EXT_METAL_ACCOUNT);

		argt.addCol(ARGT_COL_NAME_TAX_TYPE, ARGT_COL_TITLE_TAX_TYPE, SHM_USR_TABLES_ENUM.TAX_TRAN_TYPE_TABLE);
		argt.addCol(ARGT_COL_NAME_TAX_SUBTYPE, ARGT_COL_TITLE_TAX_SUBTYPE, SHM_USR_TABLES_ENUM.TAX_TRAN_SUBTYPE_TABLE);
		argt.addCol(ARGT_COL_NAME_DEAL_UNIT, ARGT_COL_TITLE_DEAL_UNIT, SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);
		
		argt.addCol(ARGT_COL_NAME_PYMT_CURRENCY, COL_TYPE_ENUM.COL_STRING, ARGT_COL_NAME_PYMT_CURRENCY);
		argt.addCol(ARGT_COL_NAME_PYMT_AMOUNT, COL_TYPE_ENUM.COL_INT, ARGT_COL_NAME_PYMT_AMOUNT);
		argt.addCol(ARGT_COL_NAME_PYMT_ACCOUNT, COL_TYPE_ENUM.COL_STRING, ARGT_COL_NAME_PYMT_ACCOUNT);

		int numRowsArgt = argt.getNumRows();
		HashSet<Integer> uniqueBUSet= new HashSet<>();
		HashMap<Integer, String> buToLocalCurrencyReportingMap= new HashMap<>();
		Logging.info("Getting list of unique internal business units from the input events: ");
		for(int row=1;row<=numRowsArgt;row++)
		{
		int internal_bunit = argt.getInt("internal_bunit", row);
		uniqueBUSet.add(internal_bunit);
		}
		Logging.info("Successfully fetched list of unique bunit: " +uniqueBUSet);
		buToLocalCurrencyReportingMap = getParameterForLocalCurrencyReporting(uniqueBUSet);
		
		for (int row=numRowsArgt; row >= 1; row--) {
			int tranUnit = argt.getInt(ARGT_COL_NAME_TRAN_UNIT, row);
			argt.setInt(ARGT_COL_NAME_DEAL_UNIT, row, tranUnit);
			int deal_num=argt.getInt("deal_tracking_num", row);
			String tranInfoId=Integer.toString(EndurTranInfoField.IS_LOCAL_CURRENCY_INVOICING.toInt());
			String isLocalCurrencyInvoicing= argt.getString("tran_info_type_"+tranInfoId, row);
			if(("Yes").equalsIgnoreCase(isLocalCurrencyInvoicing) )
			{
				Logging.info("Deal Num: "+deal_num+" is valid for local currency invoicing");
				populateLocalCurrencyValue(argt,row,buToLocalCurrencyReportingMap,deal_num);
			}
		}
		
		if (numRowsArgt > 0) {
			
			int acm = Ref.getValue(SHM_USR_TABLES_ENUM.ACCOUNT_CLASS_TABLE, ACCT_CLASS_METAL);
			int qid = Query.tableQueryInsert(argt, "tran_num");
			String sql, qtbl = Query.getResultTableForId(qid);
			Table tbl;

			sql = "SELECT distinct ate.tran_num, acc1.account_number " + ARGT_COL_NAME_INT_METAL_ACCOUNT + ", acc2.account_number " + ARGT_COL_NAME_EXT_METAL_ACCOUNT + "\n" +
				  " FROM ab_tran_event ate \n" +
				  "    JOIN "+qtbl+" qr ON (ate.tran_num=qr.query_result AND qr.unique_id="+qid + ") \n" +
				  "    JOIN ab_tran_event_settle ates ON (ate.event_num=ates.event_num)\n" +
				  "    LEFT JOIN account acc1 ON (acc1.account_id=ates.int_account_id AND acc1.account_status=1 AND acc1.account_class="+acm + ")\n" +
				  "    LEFT JOIN account acc2 ON (acc2.account_id=ates.ext_account_id AND acc2.account_status=1 AND acc2.account_class="+acm + ")\n" +
				  "    JOIN parameter p_same ON (p_same.ins_num = ate.ins_num AND ate.ins_para_seq_num = p_same.param_seq_num)\n" +
				  "    JOIN parameter p_all ON (p_all.ins_num = ate.ins_num AND p_all.param_group = p_same.param_group)\n" +
				  " WHERE ate.event_type IN (14,98)\n" +
				  "    AND ate.unit<>0";
			tbl = Table.tableNew("queried");
			long currentTime = System.currentTimeMillis();
			DBaseTable.execISql(tbl, sql);
			Logging.info("Query(for Our and CP Metal\nAccount)- completed in " + (System.currentTimeMillis()-currentTime) + " ms"); 
			if (tbl.getNumRows() > 0) {
				argt.select(tbl, ARGT_COL_NAME_INT_METAL_ACCOUNT+","+ARGT_COL_NAME_EXT_METAL_ACCOUNT,  "tran_num EQ $tran_num");
			}
				
			tbl.destroy();
			
			sql = "SELECT distinct att.tran_num, att.tax_tran_type " +ARGT_COL_NAME_TAX_TYPE + ",att.tax_tran_subtype "+ARGT_COL_NAME_TAX_SUBTYPE + ",att.tranf_group,att.param_seq_num,att.seq_num_2 \n" +
				  " FROM \n" +
				  "  ( \n" + //FX Cash Settlement events
				  "    SELECT att.tran_num, att.tax_tran_type, att.tax_tran_subtype, att.tranf_group, 1 param_seq_num, att.seq_num_2, at.ins_type, ate.event_num \n" +
				  "     FROM ab_tran_tax att, ab_tran at, ab_tran_event ate \n" + 
				  "     WHERE att.tran_num = at.tran_num AND at.ins_type = 26001 AND att.tranf_group = 1 \n" +
				  " 		 AND at.tran_num = ate.tran_num AND ate.event_type = 14\n" +	
				  "  UNION \n" + //FX Tax Settlement events 
				  "    (SELECT att.tran_num, att.tax_tran_type, att.tax_tran_subtype, att.tranf_group, 1 param_seq_num, 0 seq_num_2, at.ins_type, ate.event_num \n" +
				  "     FROM ab_tran_tax att, ab_tran at, ab_tran_event ate \n" + 
				  "     WHERE att.tran_num = at.tran_num AND at.ins_type = 26001 AND att.tranf_group = 1\n" +
				  " 	      AND at.tran_num = ate.tran_num AND ate.event_type = 98)\n" +		
				  "  UNION \n" + //FX Tax Settlement events 
				  "    (SELECT att.tran_num, att.tax_tran_type, att.tax_tran_subtype, att.tranf_group, 1 param_seq_num, 1 seq_num_2, at.ins_type, ate.event_num \n" +
				  "     FROM ab_tran_tax att, ab_tran at, ab_tran_event ate \n" + 
				  "     WHERE att.tran_num = at.tran_num AND at.ins_type = 26001 AND att.tranf_group = 1\n" +
				  " 	      AND at.tran_num = ate.tran_num AND ate.event_type = 98)\n" +		
				  "  UNION \n" + //CASH: Cash Settlement events
				  "    (SELECT att.tran_num, att.tax_tran_type, att.tax_tran_subtype, att.tranf_group, 0 param_seq_num, att.seq_num_2, at.ins_type, ate.event_num \n" +
				  "     FROM ab_tran_tax att, ab_tran at, ab_tran_event ate \n" + 
				  "     WHERE att.tran_num = at.tran_num AND at.ins_type = 27001 AND att.tranf_group = 1 \n" +
				  " 	      AND at.tran_num = ate.tran_num AND ate.event_type = 14)" + "\n" +		
				  "  UNION \n" + //CASH: Tax Settlement events
				  "    (SELECT att.tran_num, att.tax_tran_type, att.tax_tran_subtype, att.tranf_group, 0 param_seq_num, 0 seq_num_2, at.ins_type, ate.event_num \n" +
				  "     FROM ab_tran_tax att, ab_tran at, ab_tran_event ate \n" + 
				  "     WHERE att.tran_num = at.tran_num AND at.ins_type = 27001 AND att.tranf_group = 1\n" +
				  " 	      AND at.tran_num = ate.tran_num AND ate.event_type = 98)\n" +		
				  "  UNION \n" + //CASH: Tax Settlement events
				  "    (SELECT att.tran_num, att.tax_tran_type, att.tax_tran_subtype, att.tranf_group, 0 param_seq_num, 1 seq_num_2, at.ins_type, ate.event_num \n" +
				  "     FROM ab_tran_tax att, ab_tran at, ab_tran_event ate \n" + 
				  "     WHERE att.tran_num = at.tran_num AND at.ins_type = 27001 AND att.tranf_group = 1\n" +
				  " 	      AND at.tran_num = ate.tran_num AND ate.event_type = 98)\n" +		
				  "  UNION \n" + //other: Cash and Tax Settlement events
				  "    (SELECT att.tran_num, att.tax_tran_type, att.tax_tran_subtype, att.tranf_group, 0 param_seq_num, att.seq_num_2, at.ins_type, ate.event_num \n" +			
				  "     FROM ab_tran_tax att, ab_tran at, ab_tran_event ate   \n" + 
				  "     WHERE att.tran_num = at.tran_num AND tranf_group = 3 \n" +
				  " 	      AND at.tran_num = ate.tran_num AND ate.event_type in (14, 98))\n" +	
				  "  UNION \n" + //COMM-PHYS: Cash and Tax Settlement events
				  "    (SELECT distinct att.tran_num, att.tax_tran_type, att.tax_tran_subtype, att.tranf_group, param_seq_num, att.seq_num_2, at.ins_type, ate.event_num \n" +
				  "     FROM ab_tran_tax att, ab_tran at, ab_tran_event ate  \n" + 
				  "     WHERE att.tran_num = at.tran_num AND tranf_group = 16 \n" +
				  " 	      AND at.tran_num = ate.tran_num AND ate.event_type in (14, 98))\n" +	
				  "  ) att\n" +
				  " JOIN "+qtbl+" qr ON (att.tran_num=qr.query_result AND qr.unique_id=" + qid + ")";
				  
			tbl = Table.tableNew("queried");
			currentTime = System.currentTimeMillis();
			DBaseTable.execISql(tbl, sql);
			Logging.info("Query(for Tax_Type and Tax_SubType)- completed in " + (System.currentTimeMillis()-currentTime) + " ms"); 
			//tbl.viewTable();
			
			if (tbl.getNumRows() > 0) {
				
				argt.select(tbl, ARGT_COL_NAME_TAX_TYPE, ARGT_COL_NAME_TAX_SUBTYPE+" EQ -1 AND tran_num EQ $tran_num AND param_seq_num EQ $ins_para_seq_num AND seq_num_2 EQ $ins_seq_num");
				argt.select(tbl, ARGT_COL_NAME_TAX_SUBTYPE, ARGT_COL_NAME_TAX_TYPE+" EQ -1 AND tran_num EQ $tran_num AND param_seq_num EQ $ins_para_seq_num AND seq_num_2 EQ $ins_seq_num");
			}
			tbl.destroy();
			
			sql =  " SELECT tran_num, value as " + ARGT_COL_NAME_FX_RATE + ", 'Yes' as  " + ARGT_COL_NAME_APPLY_EXT_FX_RATE + "\n" +
			       " FROM ab_tran_info_view \n" +
                   "    JOIN "+qtbl+" qr ON (tran_num=qr.query_result AND qr.unique_id="+qid + ")\n" +
				   " WHERE type_name = '" + TRAN_INFO_JM_FX_RATE_NAME + "'"; 
			tbl = Table.tableNew("queried");
			currentTime = System.currentTimeMillis();
			DBaseTable.execISql(tbl, sql);
			Logging.info("Query(for FX Rate)- completed in " + (System.currentTimeMillis()- currentTime) + " ms");
			if (tbl.getNumRows() > 0) {
				
				argt.select(tbl, ARGT_COL_NAME_FX_RATE + ", " + ARGT_COL_NAME_APPLY_EXT_FX_RATE, "tran_num EQ $tran_num");
			}
			tbl.destroy();			

			sql =  "SELECT abe.event_num, p.unit " + ARGT_COL_NAME_DEAL_UNIT + "\n" +
				   " FROM " + qtbl + " qr \n" +
				   "  INNER JOIN ab_tran_event abe ON (abe.tran_num = qr.query_result)\n" +
				   "  INNER JOIN ab_tran ab ON (ab.tran_num = qr.query_result AND ab.toolset IN (SELECT t.id_number FROM toolsets t WHERE t.name IN ('MetalSwap', 'ComSwap')))\n" + 
				   "  INNER JOIN parameter p ON (p.ins_num = abe.ins_num AND p.param_seq_num = abe.ins_para_seq_num) \n" +
				   " WHERE qr.unique_id  = " + qid  ;  
			tbl = Table.tableNew("queried");
			currentTime = System.currentTimeMillis();
			DBaseTable.execISql(tbl, sql);
			Logging.info("Query(for Deal Unit) - completed in " + (System.currentTimeMillis()- currentTime) + " ms");  
			if (tbl.getNumRows() > 0) {
				
				argt.select(tbl, ARGT_COL_NAME_DEAL_UNIT, "event_num EQ $event_num");
			}
			tbl.destroy();
			
			Query.clear(qid);
		}
	}
	
	/**
	 * @param argt
	 * @param row
	 * @param deal_num 
	 * @param buToLocalCurrencyReporting2 
	 * @throws OException
	 * This method updates the payment currency to Local Currency and corresponding payment amount with appropriate conversion rate.
	 */
	private void populateLocalCurrencyValue(Table argt,int row, HashMap<Integer, String> buToLocalCurrencyReporting, int deal_num) throws OException{
		
		try{
			int internal_bunit = argt.getInt("internal_bunit", row);
			String localCurrencyReporting = buToLocalCurrencyReporting.get(internal_bunit);
			if(localCurrencyReporting==null)
			{
				Logging.info("Could not find BU in constant repository "+internal_bunit);
				return;
			}
			int fromCcy=  argt.getInt("delivery_ccy", row);
			int tradeDate= argt.getDate("trade_date", row);
			if(fromCcy!=-1)
			{
				argt.setString (ARGT_COL_NAME_PYMT_CURRENCY , row,localCurrencyReporting);
				double fxPrice=getFXRate(fromCcy, tradeDate, localCurrencyReporting);
				double settlePrice= argt.getDouble("curr_settle_amount", row);
				double finalAmountdbl=fxPrice*settlePrice;
				int finalAmount=(int)Math.round(finalAmountdbl);
				Logging.info("FX Price "+fxPrice+", settlePrice "+settlePrice+" and final amount is "+finalAmount);
				String accountNumber=getAccountNumber(internal_bunit, localCurrencyReporting);
				Logging.info("Account in local currency is "+accountNumber);
				argt.setString (ARGT_COL_NAME_PYMT_ACCOUNT , row,accountNumber);
				argt.setInt (ARGT_COL_NAME_PYMT_AMOUNT , row,finalAmount);
				Logging.info("Deal Number "+deal_num+" Local Currecy,Amount and Local account has been set to:  "+localCurrencyReporting+" , "+finalAmount+" , "+ accountNumber+" respectively");
			}
		}

		catch (OException e) {
			Logging.error("Error occured while populating LocalCurrency and Local Currency Amount for deal number " +deal_num +". Error is "+e.getMessage());
			throw new OException("Error occured while populating LocalCurrency and Local Currency Amount for deal number " +deal_num+". Error is  "+e.getMessage());

		}
	}
	
	/**
	 * @param fromCcy
	 * @param date
	 * @param localCurrencyReporting
	 * @return FX Price from FX_fromCcy_getReportingCurrecny
	 */
	private double getFXRate(int fromCcy, int date, String localCurrencyReporting) throws OException
	{
		Double fxPrice=0.0D;
		Table tblPrice=null;
		try{
			String sqlPrice ="select  hp.price from idx_historical_prices hp, idx_def d, currency c "
					+ "where hp.index_id=d.index_id "
					+ " AND d.db_status=1 " // Validated
					+ " AND d.idx_group="+IDX_GROUP_ENUM.IDX_GROUP_FX.toInt()// FX
					+ " AND d.index_status="+IDX_STATUS_ENUM.IDX_STATUS_OFFICIAL.toInt() // Official
					+ " AND d.unit=" + IDX_UNIT_ENUM.IDX_UNIT_CURRENCY.toInt() // Currency
					+ " AND d.currency2="+fromCcy
					+ " AND d.currency="+Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, localCurrencyReporting)
					+ " AND d.index_id = c.spot_index AND d.currency=c.id_number"
					+ " AND hp.start_date="+date
					+ " ORDER BY hp.reset_date";
			tblPrice = Table.tableNew();
			Logging.info("Executing: "+sqlPrice+" to determine fx rate");
			DBaseTable.execISql(tblPrice, sqlPrice);
			if(tblPrice.getNumRows()!=1){
				Logging.error("Could not find fx price");
			}
				
			fxPrice=tblPrice.getDouble("price", 1);
		}
		catch(OException e)
		{
			Logging.error("Error fetching fx rate for fromCcy: "+fromCcy+".Error is: "+e.getMessage());
			throw new OException("Error fetching fx rate for fromCcy: "+fromCcy+".Error is: "+e.getMessage());
		}
		finally{
			if(tblPrice!=null)
				tblPrice.destroy();
		}
		return fxPrice;
	}
	
	
	/**
	 * This Method determines the accountNumber which internal bunit holds for receiving payment
	 * @param external_bunit
	 * @param localCurrencyReporting
	 * @return
	 * @throws OException
	 */
	private String getAccountNumber(int internal_bunit, String localCurrencyReporting) throws OException
	{
		Table tblAccount=null;
		String  accountNumber="";
		try{
			String sqlAccount = "SELECT account_number FROM account acc JOIN party_account pa ON "
					+"acc.account_id=pa.account_id JOIN account_delivery ad ON acc.account_id=ad.account_id AND pa.party_id = " +internal_bunit+" "
					+"AND ad.currency_id = " +Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, localCurrencyReporting);

			tblAccount = Table.tableNew();
			Logging.info("Executing: "+sqlAccount+" to determine corresponding "+localCurrencyReporting+" account");
			DBaseTable.execISql(tblAccount, sqlAccount);
			if(tblAccount.getNumRows()!=1){
				Logging.error("Could not find account or found more than one account");
				return "";
			}
			accountNumber = tblAccount.getString("account_number", 1);

		}
		catch(OException e)
		{
			Logging.error("Error fetching accountNumber for internal bunit : "+internal_bunit+". and currency "+localCurrencyReporting+ " Error is: "+e.getMessage());
			throw new OException("Error fetching accountNumber for customer : "+internal_bunit+". and currency "+localCurrencyReporting+ " Error is: "+e.getMessage());
		}
		finally{
			if(tblAccount!=null)
				tblAccount.destroy();
		}
		return accountNumber;
	}
	
	/**
	 * @param ourBuSet
	 * @return Map with Key as internal BU and LocalCurenccy as BU
	 * @throws OException
	 */
	protected HashMap<Integer, String> getParameterForLocalCurrencyReporting(HashSet<Integer> ourBuSet) throws OException {
		Logging.info("Fetching local currency from const repository");
		HashMap<Integer,String> buToLocalCurrency =new HashMap<>();
		try {
			for(Integer intBu : ourBuSet) {
				try{
					String ourPartyName=Ref.getName(SHM_USR_TABLES_ENUM.PARTY_TABLE, intBu);
					Logging.info("Looking in const repository, if business unit: " +ourPartyName+" "
							+ "exists in subcontext: "+CONST_REPO_SUBCONTEXT);

					String constRepoReportingCurrency=ourPartyName+"_"+"REPORTING_CURRENCY";
					String ReportingCurrency=_constRepo.getStringValue(constRepoReportingCurrency);
					buToLocalCurrency.put(intBu, ReportingCurrency);
					Logging.info("Local Currency reporting for bunit: "+ourPartyName+" is: "+ReportingCurrency.toString());
				}
				catch(ConstantNameException e)
				{
					Logging.info("Could not find Local Currency for Business Unit"+e.getMessage());
				}	
			}
		}
		catch (OException e) {
			Logging.error("Error found while fetching local Currency for Invoicing" + e.getMessage());
			throw new OException("Error found while fetching local Currency Invoicing"+e.getMessage() );
		}
		return buToLocalCurrency;
		
	}
	
}


