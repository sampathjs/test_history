package com.jm.accountingfeed.util;

import java.text.SimpleDateFormat;

public class Constants 
{
	public static final String LOG_FILE_NAME = "EndurAccountingFeed.log";
	
	public static final int TROY_OUNCES = 55;
	
	public static final String USER_JM_TRANSACTION_ID = "USER_jm_transaction_id";
	
	public static final String USER_JM_SL_DOC_TRACKING = "USER_jm_sl_doc_tracking";
	
    public static final String USER_JM_JDE_EXTRACT_DATA = "USER_jm_jde_extract_data";
    
    public static final String JM_TRAN_DATA_SIM = "JM Tran Data";
    public static final String JM_TRAN_DATA_SIM_ENUM = "USER_RESULT_JM_TRAN_DATA";
    
	public static SimpleDateFormat endurDateFormat = new SimpleDateFormat("dd-MMM-yyyy");
	public static SimpleDateFormat jdeDateFormat = new SimpleDateFormat("yyyyMMdd");
	
	public static final String STORED_PROC_UPDATE_LEDGER_EXTRACTION = "USER_update_jm_ledger_extraction";
	
	public static final String PHYSICAL_SETTLEMENT = "Physical Settlement";
	public static final String CASH_SETTLEMENT = "Cash Settlement";
	public static final String CASH_TYPE = "CASH";
	
    
	/**
	 * For UK region: default tax currency is 'GBP'
	 * For US region: tax is NA
	 * For HK region: need input from Business 
	 */
	public static final String TAX_CCY_DEFAULT = "GBP";
	
	public static final String TAX_CODE_FOR_MANUAL_VAT = "UK Std Tax";
	
	public static final String ADVANCED_PRICING_AP = "AP";
	public static final String ADVANCED_PRICING_DP = "DP";
}
