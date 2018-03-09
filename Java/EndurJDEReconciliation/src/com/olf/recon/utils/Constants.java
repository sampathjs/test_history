package com.olf.recon.utils;

import java.text.SimpleDateFormat;

public class Constants 
{
	public static final String CONST_REPO_VARIABLE_SERVER_NAME = "ServerName";
	public static final String CONST_REPO_VARIABLE_DATABASE_NAME = "DatabaseName";
	public static final String CONST_REPO_VARIABLE_USERNAME = "UserName";
	public static final String CONST_REPO_VARIABLE_PASSWORD = "Password";
	public static final String CONST_REPO_VARIABLE_SUMMARY_EMAIL_RECIPIENTS = "SummaryEmailRecipients";
	public static final String CONST_REPO_VARIABLE_STORED_PROC_QUERY_TIMEOUT = "StoredProcQueryTimeout";
	public static final String CONST_REPO_VARIABLE_STORED_PROC_NAME_DEALS = "JDEStoredProcNameDeals";
	public static final String CONST_REPO_VARIABLE_STORED_PROC_NAME_INVOICES = "JDEStoredProcNameInvoices";
	public static final String CONST_REPO_VARIABLE_IGNORE_BREAK_NOTES_IN_EMAIL = "IgnoreBreakNotesInEmailOutput";

	public static SimpleDateFormat jdeStoredProcDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	public static SimpleDateFormat jdeInboundDateFormat = new SimpleDateFormat("yyyyMMdd");
	public static SimpleDateFormat endurDateFormat = new SimpleDateFormat("dd-MMM-yyyy");

	public static final String USER_JM_INVOICE_REC_NOTES = "user_jm_invoice_rec_notes";
	public static final String USER_JM_DEAL_REC_NOTES = "user_jm_deal_rec_notes";
	public static final String USER_MIGR_DEALS_ALL = "user_migr_deals_all";
    public static final String USER_JM_JDE_EXTRACT_DATA = "USER_jm_jde_extract_data";
	
	public static final int TROY_OUNCES = 55;
	
	public static final String AUTOMATCH_INVOICES_RECONCILIATION_UK = "EndurJDE Reconciliation - Invoices - UK";
	public static final String AUTOMATCH_DEALS_RECONCILIATION_UK = "EndurJDE Reconciliation - Deals - UK";

	public static final String AUTOMATCH_INVOICES_RECONCILIATION_US = "EndurJDE Reconciliation - Invoices - US";
	public static final String AUTOMATCH_DEALS_RECONCILIATION_US = "EndurJDE Reconciliation - Deals - US";
	
	public static final String AUTOMATCH_INVOICES_RECONCILIATION_HK = "EndurJDE Reconciliation - Invoices - HK";
	public static final String AUTOMATCH_DEALS_RECONCILIATION_HK = "EndurJDE Reconciliation - Deals - HK";

	public static final String AUTOMATCH_ACTION_INVOICES_UNMATCHED_IN_ENDUR = "Send Summary Email - Unmatched in Endur - Invoices";
	public static final String AUTOMATCH_ACTION_INVOICES_UNMATCHED_IN_JDE = "Send Summary Email - Unmatched in JDE - Invoices";
	public static final String AUTOMATCH_ACTION_DEALS_UNMATCHED_IN_ENDUR = "Send Summary Email - Unmatched in Endur - Deals";
	public static final String AUTOMATCH_ACTION_DEALS_UNMATCHED_IN_JDE = "Send Summary Email - Unmatched in JDE - Deals";
	
	public static final String LOG_FILE_NAME = "EndurJDEReconciliation.log";
}