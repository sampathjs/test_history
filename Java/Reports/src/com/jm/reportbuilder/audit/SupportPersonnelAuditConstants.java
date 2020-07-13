package com.jm.reportbuilder.audit;

import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Util;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

public class SupportPersonnelAuditConstants  {
	
	protected static final String USER_SUPPORT_PERSONNEL_AUDIT = "USER_support_personnel_audit";
	
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

	protected static final String COL_LATEST_VERSION = "latest_version";

	protected static final String COL_PERSONNEL_ID = "id_number";

	protected static final String COL_PERSONNEL_SHORTNAME = "per_sn";
	protected static final String COL_PERSONNEL_FIRSTNAME = "per_fn";
	protected static final String COL_PERSONNEL_LASTNAME = "per_ln";
	protected static final String COL_COUNTRY = "per_cn";
	protected static final String COL_PERSONNEL_TYPE = "per_ty";
	protected static final String COL_PERSONNEL_STATUS = "per_status";
	protected static final String COL_PERSONNEL_VERSION = "per_ver";
	protected static final String COL_LAST_CHANGED = "mod_user";

	protected static final String COL_FULL_AND_COMMODITY = "lt_fc";
	protected static final String COL_FULL_ACCESS = "lt_fa";
	protected static final String COL_READ_ONLY = "lt_ro";
	protected static final String COL_APM = "lt_apm";

	protected static final String COL_SUBSIDARY = "lt_sbs";
	protected static final String COL_CONNEX = "lt_co";
	protected static final String COL_SERVER = "lt_ser";

	protected static final String COL_ADMINISTRATION = "sg_ad";
	protected static final String COL_SECADMIN = "sg_sa";
	protected static final String COL_ITSUPPORT = "sg_it";

	protected static final String COL_FRONTOFFICE_UK = "sg_fo_uk";
	protected static final String COL_FRONTOFFICE_US = "sg_fo_us";
	protected static final String COL_FRONTOFFICE_HK = "sg_fo_hk";
	protected static final String COL_FRONTOFFICE_SNR = "sg_fo_snr";

	protected static final String COL_SERVERUSER = "sg_su";
	protected static final String COL_MIGRATION = "sg_mi";
	protected static final String COL_MARKET_PRICES = "sg_mp";
	protected static final String COL_MANAGEMENT_APPROVAL = "sg_ma";
	
	protected static final String COL_SAFE_WAREHOUSE = "sg_sw";
	protected static final String COL_EOD = "sg_eod";
	protected static final String COL_STOCK_TAKE = "sg_st";
	protected static final String COL_RO_INVENTORY = "sg_ro";
	
	protected static final String COL_TRADEONLYVIEW = "sg_tov";
	protected static final String COL_MARKET = "sg_m";
	protected static final String COL_CREDIT = "sg_c";
	protected static final String COL_CREDIT_SENIOR = "sg_cs";
	
	protected static final String COL_RISK = "sg_r";
	protected static final String COL_RISK_SENIOR = "sg_rs";
	protected static final String COL_BACK_OFFICE = "sg_bo";
	protected static final String COL_BACK_OFFICE_US = "sg_bo_us";
	protected static final String COL_BACK_OFFICE_SNR = "sg_bo_s";

	protected static final String COL_IT_SUPP_ELAVATED = "sg_sup_ela";
	protected static final String COL_ROLE_BASED_TESTING = "sg_rbt";
	protected static final String COL_DEPLOYMENT = "sg_dep";
	protected static final String COL_BO_PHYS_TRANSFER = "sg_bo_phys";

	protected static final String COL_CONNEX_WS_USER = "sg_con";
	protected static final String COL_PURGE_TABLES = "sg_pt";
	protected static final String COL_CN_FRONT_OFFICE = "sg_cn_fo";
	protected static final String COL_USER_APM_EDITOR = "sg_amp_e";
	protected static final String COL_BACK_OFFICE_CN = "sg_cn_bo";
	protected static final String COL_SAFE_WAREHOUSE_CN = "sg_cn_sw";
	protected static final String COL_IT_SUPPORT_AUDIT = "sg_supa";
	
	protected static final String COL_USER_CATEGORISATION = "per_cat";
	protected static final String COL_USER_EMAIL = "per_email";
	

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

	

	protected static final String COL_MODIFIED_DATE = "mod_date";
	protected static final String COL_LASTACTIVE_DATE = "la_date";
	protected static final String COL_APM_LA_DATE = "apm_la_date";
	protected static final String COL_LOGIN_COUNT = "log_count";
	
	protected static final String COL_SCREEN_CONFIG_NAME = "scn";

	
	  //Initiate plug in logging
		public static void initPluginLog(ConstRepository constRep) throws OException {

		String logLevel = constRep.getStringValue("logLevel", "info");
		String logFile = constRep.getStringValue("logFile",defaultLogFile + ".log");
		String logDir = constRep.getStringValue("logDir", SystemUtil.getEnvVariable("AB_OUTDIR") + "\\Error_Logs\\");

			try {
		
				if (logDir.trim().equalsIgnoreCase("")) {
					PluginLog.init(logLevel);
				} else {
					PluginLog.init(logLevel, logDir, logFile);
				}
			} 
			catch (Exception e) {
				String errMsg = defaultLogFile	+ ": Failed to initialize logging module.";
				Util.exitFail(errMsg);
			}
		}

}