package com.jm.reportbuilder.audit;


import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Util;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

public class SupportPersonnelAnalysisConstants  {

	protected static final String USER_SUPPORT_PERSONNEL_ANALYSIS = "USER_support_personnel_analysis";
	/**
	 * Specifies the constants' repository context parameter.
	 */
	protected static final String REPO_CONTEXT = "Support";

	/**
	 * Specifies the constants' repository sub-context parameter.
	 */
	protected static final String REPO_SUB_CONTEXT = "UserSecAudit";

	//Constants Repository Statics
	
	protected static final String defaultLogFile = "UserSecAudit";
	 
	protected static final String COL_PERSONNEL_ID = "id_number";

	protected static final String COL_PERSONNEL_SHORTNAME = "per_sn";
	protected static final String COL_PERSONNEL_FIRSTNAME = "per_fn";
	protected static final String COL_PERSONNEL_LASTNAME = "per_ln";
	protected static final String COL_COUNTRY = "per_cn";
	protected static final String COL_PERSONNEL_TYPE = "per_ty";
	protected static final String COL_PERSONNEL_STATUS = "per_status";
	protected static final String COL_PERSONNEL_VERSION = "per_ver";
	protected static final String COL_PERSONNEL_LASTVERSION = "per_last_ver";
	protected static final String COL_LAST_CHANGED = "mod_user";

	
	protected static final String COL_HEADER_DIFFERENCE = "personnel_diff";
	protected static final String COL_LICENCE_DIFFERENCE = "lic_diff";
	protected static final String COL_SEC_DIFFERENCE = "sec_diff";
	protected static final String COL_FUNCTIONAL_DIFFERENCE = "functional_diff";
	protected static final String COL_EXPLANATION = "explanation";

	

	

	
	protected static final String COL_MODIFIED_DATE = "mod_date";

	protected static final String COL_ID_NUMBER = "id_number";
	protected static final String COL_PER_NAME = "per_name";
	protected static final String COL_PER_FIRSTNAME = "per_firstname";
	protected static final String COL_PER_COUNTRY = "per_country";
	protected static final String COL_PER_LASTNAME = "per_lastname";
	protected static final String COL_PER_PERSONNEL_TYPE = "per_personnel_type";
	protected static final String COL_PER_PESONNEL_STATUS = "per_pesonnel_status";
	protected static final String COL_PER_PERSONNEL_CURRENT_VERSION = "per_personnel_version";
	protected static final String COL_PER_PERSONNEL_PREVIOUS_VERSION = "per_previous_personnel_version";
	protected static final String COL_PER_MOD_USER = "per_mod_user";
	protected static final String COL_LT_FULL_COMMODITY = "lt_full_commodity";
	protected static final String COL_LT_FULL_ACCESS = "lt_full_access";
	protected static final String COL_LT_READ_ONLY = "lt_read_only";
	protected static final String COL_LT_APM = "lt_apm";
	protected static final String COL_LT_SUBSIDIARY = "lt_subsidiary";
	protected static final String COL_LT_CONNEX = "lt_connex";
	protected static final String COL_LT_SERVER = "lt_server";
	protected static final String COL_SG_SECURITY_ADMIN = "sg_security_admin";
	protected static final String COL_SG_IT_SUPPORT = "sg_it_support";
	protected static final String COL_SG_FO_UK = "sg_fo_uk";
	protected static final String COL_SG_FO_HK = "sg_fo_hk";
	protected static final String COL_SG_FO_US = "sg_fo_us";
	protected static final String COL_SG_FO_SNR = "sg_fo_snr";
	protected static final String COL_SG_ADMINISTRATOR = "sg_administrator";
	protected static final String COL_SG_SERVER_USER = "sg_server_user";
	protected static final String COL_SG_MIGRATION = "sg_migration";
	protected static final String COL_SG_MARKET_PRICES = "sg_market_prices";
	protected static final String COL_SG_MAN_APPROVAL = "sg_man_approval";
	protected static final String COL_SG_SAFE_WAREHOUSE = "sg_safe_warehouse";
	protected static final String COL_SG_EOD = "sg_eod";
	protected static final String COL_SG_STOCK_TAKE = "sg_stock_take";
	protected static final String COL_SG_RO_INVENTORY = "sg_ro_inventory";
	protected static final String COL_SG_TRADE_ONLY_VIEW = "sg_trade_only_view";
	protected static final String COL_SG_MARKET_USER = "sg_market_user";
	protected static final String COL_SG_CREDIT = "sg_credit";
	protected static final String COL_SG_CREDIT_SNR = "sg_credit_snr";
	protected static final String COL_SG_RISK = "sg_risk";
	protected static final String COL_SG_RISK_SNR = "sg_risk_snr";
	protected static final String COL_SG_BO = "sg_bo";
	protected static final String COL_SG_BO_US = "sg_bo_us";
	protected static final String COL_SG_BO_SNR = "sg_bo_snr";
	
	protected static final String COL_SG_SUPPORT_ELEVATED = "sg_support_elv";
	protected static final String COL_SG_ROLE_BASED_TESTING = "sg_role_testing";
	protected static final String COL_SG_DEPLOYMENT = "sg_deployment";
	protected static final String COL_SG_PHYS_TRANSFER = "sg_phys_transfer";
	
	protected static final String COL_SG_CONNEX_WS = "sg_connex_ws_user";
	protected static final String COL_SG_PURGE_TABLES = "sg_purge_tables";
	protected static final String COL_SG_FO_CN = "sg_fo_cn";
	protected static final String COL_SG_AMP_EDITOR = "sg_amp_editor";
	protected static final String COL_SG_BO_CN = "sg_bo_cn";
	protected static final String COL_SG_ROLE_SAFE_CN = "sg_safe_warehouse_cn";
	protected static final String COL_SG_IT_AUDIT = "sg_it_audit";

	protected static final String COL_PER_CAT = "per_cat";
	protected static final String COL_PER_EMAIL = "per_email";

///////////////////
	protected static final String COL_FUNCTIONALGROUP_General = "fg_gen"; 
	protected static final String COL_FUNCTIONALGROUP_Trading = "fg_tra";
	protected static final String COL_FUNCTIONALGROUP_Operations = "fg_ops"; 
	protected static final String COL_FUNCTIONALGROUP_Credit = "fg_cre"; 
	protected static final String COL_FUNCTIONALGROUP_OptionExercise = "fg_ope"; 
	protected static final String COL_FUNCTIONALGROUP_WellheadScheduling = "fg_well";
	protected static final String COL_FUNCTIONALGROUP_CorporateActions = "fg_ca";
	protected static final String COL_FUNCTIONALGROUP_ManagementApprovalGroup = "fg_mag"; 

	protected static final String COL_FUNCTIONALGROUP_JMPriceHK = "fg_phk";
	protected static final String COL_FUNCTIONALGROUP_JMPriceUK = "fg_puk";
	protected static final String COL_FUNCTIONALGROUP_JMPriceUS = "fg_pus";
	protected static final String COL_FUNCTIONALGROUP_JMPriceCN = "fg_pcn";

	protected static final String COL_FUNCTIONALGROUP_TradeConfirmationsUK = "fg_tcuk";
	protected static final String COL_FUNCTIONALGROUP_TradeConfirmationsUS = "fg_tcus";
	protected static final String COL_FUNCTIONALGROUP_TradeConfirmationsHK = "fg_tchk";
	protected static final String COL_FUNCTIONALGROUP_TradeConfirmationsCN = "fg_tccn";

	protected static final String COL_FUNCTIONALGROUP_InvoicesUK = "fg_inuk";
	protected static final String COL_FUNCTIONALGROUP_InvoicesUS = "fg_inus";
	protected static final String COL_FUNCTIONALGROUP_InvoicesHK = "fg_inhk";
	protected static final String COL_FUNCTIONALGROUP_InvoicesCN = "fg_incn";

	protected static final String COL_FUNCTIONALGROUP_TransfersUK = "fg_truk";
	protected static final String COL_FUNCTIONALGROUP_TransfersUS = "fg_trus";
	protected static final String COL_FUNCTIONALGROUP_TransfersHK = "fg_trhk";
	protected static final String COL_FUNCTIONALGROUP_TransfersCN = "fg_trcn";

	protected static final String COL_FUNCTIONALGROUP_MetalStatements = "fg_ms";
	protected static final String COL_FUNCTIONALGROUP_Logistics = "fg_log";
	protected static final String COL_FUNCTIONALGROUP_LRDealing = "fg_lrdeal";
	protected static final String COL_FUNCTIONALGROUP_LRLease = "fg_lrlea";

	protected static final String COL_FUNCTIONALGROUP_LRLiquidity  = "fg_lrliq";
	protected static final String COL_FUNCTIONALGROUP_LRSummary  = "fg_lrsum";

	
	
	protected static final String COL_PER_MODIFIED_DATE = "per_modified_date";
	protected static final String COL_LAST_ACTIVE_DATE = "last_active_date";
	protected static final String COL_APM_LAST_ACTIVE_DATE = "apm_last_active_date";
	protected static final String COL_LOGIN_COUNT = "login_count";
	protected static final String COL_SCREEN_CONFIG_NAME = "screen_config_name";
	protected static final String COL_REPORT_DATE = "report_date";

	
	  //Initiate plug in logging
		public static void initLogging(ConstRepository constRep) throws OException {

		String logLevel = constRep.getStringValue("logLevel", "info");
		String logFile = constRep.getStringValue("logFile",defaultLogFile + ".log");
		String logDir = constRep.getStringValue("logDir", SystemUtil.getEnvVariable("AB_OUTDIR") + "\\Error_Logs\\");

			try {
		
				Logging.init(SupportPersonnelAnalysisConstants.class, constRep.getContext(), constRep.getSubcontext());
			} 
			catch (Exception e) {
				String errMsg = defaultLogFile	+ ": Failed to initialize logging module.";
				Util.exitFail(errMsg);
			}
		}

}