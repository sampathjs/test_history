package com.jm.reportbuilder.audit;


import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_ADMINISTRATION;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_APM;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_APM_LA_DATE;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_BACK_OFFICE;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_BACK_OFFICE_CN;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_BACK_OFFICE_SNR;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_BACK_OFFICE_US;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_BO_PHYS_TRANSFER;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_CN_FRONT_OFFICE;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_CONNEX;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_CONNEX_WS_USER;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_COUNTRY;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_CREDIT;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_CREDIT_SENIOR;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_DEPLOYMENT;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_EOD;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FRONTOFFICE_HK;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FRONTOFFICE_SNR;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FRONTOFFICE_UK;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FRONTOFFICE_US;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FULL_ACCESS;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FULL_AND_COMMODITY;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_CorporateActions;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_Credit;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_General;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_InvoicesCN;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_InvoicesHK;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_InvoicesUK;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_InvoicesUS;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_JMPriceCN;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_JMPriceHK;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_JMPriceUK;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_JMPriceUS;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_LRDealing;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_LRLease;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_LRLiquidity;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_LRSummary;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_Logistics;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_ManagementApprovalGroup;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_MetalStatements;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_Operations;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_OptionExercise;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_TradeConfirmationsCN;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_TradeConfirmationsHK;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_TradeConfirmationsUK;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_TradeConfirmationsUS;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_Trading;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_TransfersCN;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_TransfersHK;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_TransfersUK;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_TransfersUS;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_FUNCTIONALGROUP_WellheadScheduling;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_ITSUPPORT;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_IT_SUPPORT_AUDIT;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_IT_SUPP_ELAVATED;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_LASTACTIVE_DATE;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_LAST_CHANGED;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_LOGIN_COUNT;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_MANAGEMENT_APPROVAL;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_MARKET;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_MARKET_PRICES;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_MIGRATION;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_MODIFIED_DATE;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_PERSONNEL_FIRSTNAME;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_PERSONNEL_ID;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_PERSONNEL_LASTNAME;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_PERSONNEL_SHORTNAME;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_PERSONNEL_STATUS;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_PERSONNEL_TYPE;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_PERSONNEL_VERSION;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_PURGE_TABLES;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_READ_ONLY;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_RISK;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_RISK_SENIOR;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_ROLE_BASED_TESTING;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_RO_INVENTORY;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_SAFE_WAREHOUSE;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_SAFE_WAREHOUSE_CN;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_SCREEN_CONFIG_NAME;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_SECADMIN;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_SERVER;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_SERVERUSER;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_STOCK_TAKE;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_SUBSIDARY;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_TRADEONLYVIEW;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_USER_APM_EDITOR;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_USER_CATEGORISATION;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditConstants.COL_USER_EMAIL;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_LIC_APM;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_LIC_CONNEX;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_LIC_FULL_ACCESS;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_LIC_FULL_AND_COMMODITY;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_LIC_READONLY;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_LIC_SERVER;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_LIC_SUBSIDIARY;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_ADMINISTRATION;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_BACK_OFFICE;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_BACK_OFFICE_CN;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_BACK_OFFICE_SNR;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_BACK_OFFICE_US;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_BO_Phys_TRANSFER;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_CN_FRONT_OFFICE;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_CONNEX_WS_USER;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_CREDIT;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_CREDIT_SENIOR;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_DEPLOYMENT;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_EOD;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_FRONTOFFICE_HK;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_FRONTOFFICE_SNR;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_FRONTOFFICE_UK;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_FRONTOFFICE_US;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_ITSUPPORT;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_IT_SUPPORT_AUDIT;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_IT_SUPPORT_ELAVATED;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_MANAGEMENT_APPROVAL;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_MARKET;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_MARKET_PRICES;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_MIGRATION;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_PURGE_TABLES;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_RISK;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_RISK_SENIOR;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_ROLEBASED_TESTING;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_RO_INVENTORY;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_SAFE_WAREHOUSE;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_SAFE_WAREHOUSE_CN;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_SECADMIN;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_SERVERUSER;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_STOCK_TAKE;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_TRADEVIEWONLY;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.HAS_SG_USER_APM_EDITOR;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.LAST_ACTIVE;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.LAST_APM_ACTIVE;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.LAST_CHANGED;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.LOGIN_COUNT;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.MODIFIED_DATE;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.PERSONNEL_COUNTRY;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.PERSONNEL_FIRSTNAME;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.PERSONNEL_ID;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.PERSONNEL_LASTNAME;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.PERSONNEL_SHORTNAME;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.PERSONNEL_STATUS;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.PERSONNEL_TYPE;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.PERSONNEL_VERSION;
import static com.jm.reportbuilder.audit.SupportPersonnelAuditDataLoad.Columns.SCREEN_CONFIG_NAME;
import static com.olf.openjvs.enums.COL_TYPE_ENUM.COL_INT;
import static com.olf.openjvs.enums.COL_TYPE_ENUM.COL_STRING;

import java.text.ParseException;

import com.olf.jm.logging.Logging;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
//import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.openlink.util.constrepository.ConstRepository;

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_DATALOAD)
public class SupportPersonnelAuditDataLoad implements IScript
{

	

	protected enum Columns
	{
		//SupportPersonnelAuditConstants
		PERSONNEL_ID(COL_PERSONNEL_ID, "Personnel ID", COL_INT, "Personnel ID"){	},
		PERSONNEL_SHORTNAME(COL_PERSONNEL_SHORTNAME, "Short Name", COL_STRING, "Personnel Short Name"){		},
		PERSONNEL_FIRSTNAME(COL_PERSONNEL_FIRSTNAME, "First Name", COL_STRING, "Personnel First Name"){		},
		PERSONNEL_COUNTRY(COL_COUNTRY, "Country", COL_STRING, "Personnel Region Country"){		},
		PERSONNEL_LASTNAME(COL_PERSONNEL_LASTNAME, "Last Name", COL_STRING, "Personnel First Name")		{		},
		PERSONNEL_TYPE(COL_PERSONNEL_TYPE, "Personnel Type", COL_STRING, "Personnel Type")		{		},
		PERSONNEL_STATUS(COL_PERSONNEL_STATUS, "Personnel Status", COL_STRING, "Personnel Status")		{		},
		
		PERSONNEL_VERSION(COL_PERSONNEL_VERSION, "Personnel Version", COL_INT, "Personnel Version")		{		},
		LAST_CHANGED(COL_LAST_CHANGED, "Last Changed By", COL_STRING, "Last Changed By")		{		},
		

		
		HAS_LIC_FULL_AND_COMMODITY(COL_FULL_AND_COMMODITY, "Full And Commodity", COL_STRING, "Full and Commodity Licence")		{		},
		HAS_LIC_FULL_ACCESS(COL_FULL_ACCESS, "Full Access", COL_STRING, "Full Access Licence")		{		},
		HAS_LIC_READONLY(COL_READ_ONLY, "Read Only", COL_STRING, "Read Only Licence"){		},
		HAS_LIC_APM(COL_APM, "APM", COL_STRING, "Has APM Licence")		{		},

		HAS_LIC_SUBSIDIARY(COL_SUBSIDARY, "Subsidiary", COL_STRING, "Subsidary Licence")		{		},
		HAS_LIC_CONNEX(COL_CONNEX, "Connex", COL_STRING, "Connex Licence"){		},
		HAS_LIC_SERVER(COL_SERVER, "Server", COL_STRING, "Server Licence")		{		},

		HAS_SG_SECADMIN(COL_SECADMIN, "Sec Admin", COL_STRING, "Is a member of Security Admin")		{		},
		HAS_SG_ITSUPPORT(COL_ITSUPPORT, "IT Support", COL_STRING, "Is a member of IT Support")		{		},
		HAS_SG_FRONTOFFICE_UK(COL_FRONTOFFICE_UK, "Front Office UK", COL_STRING, "Is a member of Front Office UK")		{		},
		HAS_SG_FRONTOFFICE_HK(COL_FRONTOFFICE_HK, "Front Office HK", COL_STRING, "Is a member of Front Office HK")		{		},
		HAS_SG_FRONTOFFICE_US(COL_FRONTOFFICE_US, "Front Office US", COL_STRING, "Is a member of Front Office US")		{		},
		HAS_SG_FRONTOFFICE_SNR(COL_FRONTOFFICE_SNR, "Front Office SNR", COL_STRING, "Is a member of Front Office SNR")		{		},
		
		HAS_SG_ADMINISTRATION(COL_ADMINISTRATION, "Administration", COL_STRING, "Is a member of Administration")		{		},
		
		HAS_SG_SERVERUSER(COL_SERVERUSER, "Server User", COL_STRING, "Is a member of Server User")		{		},
		HAS_SG_MIGRATION(COL_MIGRATION, "Migration", COL_STRING, "Is a member of Migration")		{		},
		HAS_SG_MARKET_PRICES(COL_MARKET_PRICES, "Market Prices", COL_STRING, "Is a member of Market Prices")		{		},
		HAS_SG_MANAGEMENT_APPROVAL(COL_MANAGEMENT_APPROVAL, "Management Approval", COL_STRING, "Is a member of Management Approval")		{		},

		HAS_SG_SAFE_WAREHOUSE(COL_SAFE_WAREHOUSE, "Safe Warehouse", COL_STRING, "Is a member of Safe Warehouse")		{		},
		HAS_SG_EOD(COL_EOD, "EOD User", COL_STRING, "Is a member of EOD User")		{		},
		HAS_SG_STOCK_TAKE(COL_STOCK_TAKE, "Stock Take", COL_STRING, "Is a member of Stock Take")		{		},
		HAS_SG_RO_INVENTORY(COL_RO_INVENTORY, "RO Inventory", COL_STRING, "Is a member of Ro Inventory")		{		},

		HAS_SG_TRADEVIEWONLY(COL_TRADEONLYVIEW, "Trade Only View", COL_STRING, "Is a member of Trade Only View")		{		},
		HAS_SG_MARKET(COL_MARKET, "Market User", COL_STRING, "Is a member of Market")		{		},
		HAS_SG_CREDIT(COL_CREDIT, "Credit", COL_STRING, "Is a member of Credit")		{		},
		HAS_SG_CREDIT_SENIOR(COL_CREDIT_SENIOR, "Credit Senior", COL_STRING, "Is a member of Credit Snr")		{		},

		HAS_SG_RISK(COL_RISK, "Risk", COL_STRING, "Is a member of Risk")		{		},
		HAS_SG_RISK_SENIOR(COL_RISK_SENIOR, "Risk Senior", COL_STRING, "Is a member of Risk Senior")		{		},
		HAS_SG_BACK_OFFICE(COL_BACK_OFFICE, "Back Office", COL_STRING, "Is a member of Back Office")		{		},
		HAS_SG_BACK_OFFICE_US(COL_BACK_OFFICE_US, "Back Office US", COL_STRING, "Is a member of Back Office US")		{		},
		HAS_SG_BACK_OFFICE_SNR(COL_BACK_OFFICE_SNR, "Back Office SNR", COL_STRING, "Is a member of Back Office SNR")		{		},
		
		
		HAS_SG_IT_SUPPORT_ELAVATED(COL_IT_SUPP_ELAVATED, "IT Support Elavated", COL_STRING, "Is a member of IT Support Elavated")		{		},
		HAS_SG_ROLEBASED_TESTING(COL_ROLE_BASED_TESTING, "Role Based Testing", COL_STRING, "Is a member of Role Based Testing")		{		},
		HAS_SG_DEPLOYMENT(COL_DEPLOYMENT, "Deployment", COL_STRING, "Is a member of Deployment")		{		},
		HAS_SG_BO_Phys_TRANSFER(COL_BO_PHYS_TRANSFER, "BO Phys Transfer", COL_STRING, "Is a member of BO Phys Transfer")		{		},

		HAS_SG_CONNEX_WS_USER(COL_CONNEX_WS_USER, "ConnexWsUser", COL_STRING, "Is a member of connexWsUser")		{		},
		HAS_SG_PURGE_TABLES(COL_PURGE_TABLES, "PurgeTables", COL_STRING, "Is a member of PurgeTables")		{		},
		HAS_SG_CN_FRONT_OFFICE(COL_CN_FRONT_OFFICE, "CN Front Office", COL_STRING, "Is a member of CN Front Office")		{		},
		HAS_SG_USER_APM_EDITOR(COL_USER_APM_EDITOR, "User APM Editor", COL_STRING, "Is a member of User APM Editor")		{		},
		HAS_SG_BACK_OFFICE_CN(COL_BACK_OFFICE_CN, "Back Office CN", COL_STRING, "Is a member of Back Office CN")		{		},
		HAS_SG_SAFE_WAREHOUSE_CN(COL_SAFE_WAREHOUSE_CN, "Safe Warehouse CN", COL_STRING, "Is a member of Safe Warehouse CN")		{		},
		HAS_SG_IT_SUPPORT_AUDIT(COL_IT_SUPPORT_AUDIT, "IT Support Audit", COL_STRING, "Is a member of IT Support Audit")		{		},
		
		USER_CATEGORISATION(COL_USER_CATEGORISATION, "User Categorisation", COL_STRING, "User Categorisation")		{		},
		USER_EMAIL(COL_USER_EMAIL, "User Email", COL_STRING, "User Email")		{		},
		
//		SupportPersonnelAuditConstants  
		HAS_FG_General(COL_FUNCTIONALGROUP_General, "General", COL_STRING, "Is a member of Fun group General")		{		},
		HAS_FG_Trading(COL_FUNCTIONALGROUP_Trading, "Trading", COL_STRING, "Is a member of Fun group Trading")		{		},
		HAS_FG_Operations (COL_FUNCTIONALGROUP_Operations , "Operations ", COL_STRING, "Is a member of Fun group Operations")		{		},
		HAS_FG_Credit(COL_FUNCTIONALGROUP_Credit, "Credit", COL_STRING, "Is a member of Fun group Credit")		{		},
		HAS_FG_OptionExercise(COL_FUNCTIONALGROUP_OptionExercise, "OptionExercise", COL_STRING, "Is a member of Fun group OptionExercise")		{		},
		HAS_FG_WellheadScheduling(COL_FUNCTIONALGROUP_WellheadScheduling, "WellheadScheduling", COL_STRING, "Is a member of Fun group ")		{		},
		HAS_FG_CorporateActions(COL_FUNCTIONALGROUP_CorporateActions, "CorporateActions", COL_STRING, "Is a member of Fun group CorporateActions")		{		},
		HAS_FG_ManagementApprovalGroup(COL_FUNCTIONALGROUP_ManagementApprovalGroup, "ManagementApprovalGroup", COL_STRING, "Is a member of Fun group ManagementApprovalGroup")		{		},
		HAS_FG_JMPriceHK(COL_FUNCTIONALGROUP_JMPriceHK, "JMPriceHK", COL_STRING, "Is a member of Fun group JMPriceHK")		{		},
		HAS_FG_JMPriceUK(COL_FUNCTIONALGROUP_JMPriceUK, "JMPriceUK", COL_STRING, "Is a member of Fun group JMPriceUK"){},
		HAS_FG_JMPriceUS(COL_FUNCTIONALGROUP_JMPriceUS, "JMPriceUS", COL_STRING, "Is a member of Fun group JMPriceUS"){},
		HAS_FG_JMPriceCN(COL_FUNCTIONALGROUP_JMPriceCN, "JMPriceCN", COL_STRING, "Is a member of Fun group JMPriceCN"){},
		HAS_FG_TradeConfirmationsUK(COL_FUNCTIONALGROUP_TradeConfirmationsUK, "TradeConfirmationsUK", COL_STRING, "Is a member of Fun group "){},
		HAS_FG_TradeConfirmationsUS(COL_FUNCTIONALGROUP_TradeConfirmationsUS, "TradeConfirmationsUS", COL_STRING, "Is a member of Fun group TradeConfirmationsUS"){},
		HAS_FG_TradeConfirmationsHK(COL_FUNCTIONALGROUP_TradeConfirmationsHK, "TradeConfirmationsHK", COL_STRING, "Is a member of Fun group TradeConfirmationsHK"){},
		HAS_FG_TradeConfirmationsCN(COL_FUNCTIONALGROUP_TradeConfirmationsCN, "TradeConfirmationsCN", COL_STRING, "Is a member of Fun group TradeConfirmationsCN"){},
		HAS_FG_InvoicesUK(COL_FUNCTIONALGROUP_InvoicesUK, "InvoicesUK", COL_STRING, "Is a member of Fun group InvoicesUK"){},
		HAS_FG_InvoicesUS(COL_FUNCTIONALGROUP_InvoicesUS, "InvoicesUS", COL_STRING, "Is a member of Fun group InvoicesUS"){},
		HAS_FG_InvoicesHK(COL_FUNCTIONALGROUP_InvoicesHK, "InvoicesHK", COL_STRING, "Is a member of Fun group InvoicesHK"){},
		HAS_FG_InvoicesCN(COL_FUNCTIONALGROUP_InvoicesCN, "InvoicesCN", COL_STRING, "Is a member of Fun group InvoicesCN"){},
		HAS_FG_TransfersUK(COL_FUNCTIONALGROUP_TransfersUK, "TransfersUK", COL_STRING, "Is a member of Fun group TransfersUK"){},
		HAS_FG_TransfersUS(COL_FUNCTIONALGROUP_TransfersUS, "TransfersUS", COL_STRING, "Is a member of Fun group TransfersUS"){},
		HAS_FG_TransfersHK(COL_FUNCTIONALGROUP_TransfersHK, "TransfersHK", COL_STRING, "Is a member of Fun group TransfersHK"){},
		HAS_FG_TransfersCN(COL_FUNCTIONALGROUP_TransfersCN, "TransfersCN", COL_STRING, "Is a member of Fun group TransfersCN"){},
		HAS_FG_MetalStatements(COL_FUNCTIONALGROUP_MetalStatements, "MetalStatements", COL_STRING, "Is a member of Fun group MetalStatements"){},
		HAS_FG_Logistics(COL_FUNCTIONALGROUP_Logistics, "Logistics", COL_STRING, "Is a member of Fun group Logistics"){},
		HAS_FG_LRDealing(COL_FUNCTIONALGROUP_LRDealing, "LRDealing", COL_STRING, "Is a member of Fun group LRDealing"){},
		HAS_FG_LRLease(COL_FUNCTIONALGROUP_LRLease, "LRLease", COL_STRING, "Is a member of Fun group LRLease"){},
		HAS_FG_LRLiquidity(COL_FUNCTIONALGROUP_LRLiquidity, "LRLiquidity", COL_STRING, "Is a member of Fun group LRLiquidity"){},
		HAS_FG_LRSummary(COL_FUNCTIONALGROUP_LRSummary , "LRSummary", COL_STRING, "Is a member of Fun group LRSummary"){},
		
		
		
		
		

		

		
		MODIFIED_DATE(COL_MODIFIED_DATE, "Modified Date", COL_TYPE_ENUM.COL_DATE_TIME, "The Last Time the user was updated")		{		},
		LAST_ACTIVE(COL_LASTACTIVE_DATE, "Last Active Date", COL_TYPE_ENUM.COL_DATE_TIME, "The Last Time the user was in the system")		{		},
		LAST_APM_ACTIVE(COL_APM_LA_DATE, "APM Last Active Date", COL_TYPE_ENUM.COL_DATE_TIME, "The Last Time the user used APM")		{		},
		
		LOGIN_COUNT(COL_LOGIN_COUNT, "Login Couunt", COL_TYPE_ENUM.COL_INT, "Login Counts fro start of year")		{		},
		SCREEN_CONFIG_NAME(COL_SCREEN_CONFIG_NAME, "Screen Config Name", COL_STRING, "Screen Config Name")		{		},
		
		
		;

		private String _name;
		private String _title;
		private COL_TYPE_ENUM _format;
		private String _columnCaption;

		private Columns(String name, String title, COL_TYPE_ENUM format, String columnCaption){
			_name = name;
			_title = title;
			_format = format;
			_columnCaption = columnCaption;
		}

		public String getColumn() {
			return _name;
		}

		private String getTitle() {
			return _title;
		}

		private COL_TYPE_ENUM getType() {
			return _format;
		}

		private String getColumnCaption() {
			return _columnCaption;
		}

		public String getNameType() {
			String monthString = "";
			COL_TYPE_ENUM thisType = getType();
			switch (thisType) {
			case COL_INT:
				monthString = "INT";
				break;
			case COL_DOUBLE:
				monthString = "DOUBLE";
				break;
			case COL_STRING:
				monthString = "CHAR";
				break;
			case COL_DATE_TIME:
				monthString = "DATETIME";
				break;
			default:
				monthString = thisType.toString().toUpperCase();
				break;
			}

			return monthString;
		}
	}


 

	@Override
	/**
	 * execute: Main Gateway into script from the AScript extender class
	 */
	public void execute(IContainerContext context) throws OException {

		int queryID;
		String sQueryTable;

		try {
			// Setting up the log file.
			//Constants Repository init
			ConstRepository constRep = new ConstRepository(SupportPersonnelAuditConstants.REPO_CONTEXT, SupportPersonnelAuditConstants.REPO_SUB_CONTEXT);
			SupportPersonnelAuditConstants.initLogging(constRep); //Plug in Log init

			
			// Logging.init("INFO");

			Logging.info("Start  " + getClass().getSimpleName());

			Table argt = context.getArgumentsTable();
			Table returnt = context.getReturnTable();

			int modeFlag = argt.getInt("ModeFlag", 1);
			Logging.debug(getClass().getSimpleName() + " - Started Data Load Script for SupportPersonnelAuditDataLoad Reports - mode: " + modeFlag);

			if (modeFlag == 0) {
				/* Add the Table Meta Data */
				Table pluginMetadata = argt.getTable("PluginMetadata", 1);
				Table tableMetadata = pluginMetadata.getTable("table_metadata", 1);
				Table columnMetadata = pluginMetadata.getTable("column_metadata", 1);
				Table joinMetadata = pluginMetadata.getTable("join_metadata", 1);

				tableMetadata.addNumRows(1);
				tableMetadata.setString("table_name", 1, getClass().getSimpleName());
				tableMetadata.setString("table_title", 1, getClass().getSimpleName());
				tableMetadata.setString("table_description", 1, getClass().getSimpleName() + " Data Source: ");
				tableMetadata.setString("pkey_col1", 1, PERSONNEL_ID.getColumn());

				/* Add the Column Meta Data */

				addColumnsToMetaData(columnMetadata);

				/* Add the JOIN Meta Data */
				joinMetadata.addNumRows(1);

				int iRow = 1;
				joinMetadata.setString("table_name", iRow, "generated_values");
				joinMetadata.setString("join_title", iRow, "Join on id_number (personnel)");
				joinMetadata.setString("fkey_col1", iRow, PERSONNEL_ID.getColumn());
				joinMetadata.setString("pkey_table_name", iRow, "personnel");
				joinMetadata.setString("rkey_col1", iRow, PERSONNEL_ID.getColumn());
				joinMetadata.setString("fkey_description", iRow, "Joins our filter table into the transaction table");


				Logging.debug("Completed Data Load Script Metadata:");

				return;
			} else {

				formatReturnTable(returnt);
				if (modeFlag == 0) {
					return;
				}
				queryID = argt.getInt("QueryResultID", 1);
				sQueryTable = argt.getString("QueryResultTable", 1);


				if (queryID > 0) {

					Logging.info("Enrich data query ID: " + queryID + " User Table: " + sQueryTable);
					
					Table tblTemp = argt.getTable("PluginParameters", 1);
					String personnelID = tblTemp.getString("parameter_value", tblTemp.unsortedFindString("parameter_name", "personnel_id", SEARCH_CASE_ENUM.CASE_INSENSITIVE));

					
					enrichData(returnt, queryID, sQueryTable ,personnelID);

					Logging.info("Data Num Rows: " + returnt.getNumRows());

				}

			}

		} catch (Exception e) {

			String errMsg = e.toString();
			com.olf.openjvs.Util.exitFail(errMsg);
			throw new RuntimeException(e);
		} finally {
			Logging.info("End " + getClass().getSimpleName());
			Logging.close();
		}


		return;
	}



	/**
	 * Method recoveryTransactionalInformaiton Recovers TranPointers from Query results and extract in final return table information for particular Transaction fields
	 * 
	 * @param queryID
	 * @throws OException
	 * @throws ParseException
	 */
	private void enrichData(Table returnt, int queryID, String sQueryTable, String personnelID) throws OException {

		Table tblPersonnelData = Table.tableNew();
		Table tblPersonnelFunctionalGroup = Table.tableNew();

		int totalRows = 0;
		String sqlCommand;

		Logging.debug("Attempt to recover Personnel information.");

		try {
			boolean runForAll = true;
			int personnel_id = 0;
			try {
				personnel_id = Integer.parseInt(personnelID);
				runForAll = false;
			} catch (Exception e) {
			}

			String startOfYear = "01-Jan-2018";

			sqlCommand = "SELECT p.id_number " + PERSONNEL_ID.getColumn() + ",p.name " + PERSONNEL_SHORTNAME.getColumn() + ",\n" +
						" p.first_name " + PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + PERSONNEL_LASTNAME.getColumn() + ",\n" +
						" ps.name " + PERSONNEL_STATUS.getColumn() + ",pt.name " + PERSONNEL_TYPE.getColumn() + ",\n" +
						" c.name " + PERSONNEL_COUNTRY.getColumn() + ",p.last_update " + MODIFIED_DATE.getColumn() + ",\n" +
						" p.personnel_version " + PERSONNEL_VERSION.getColumn() + ",lc.name " + LAST_CHANGED.getColumn() + ",\n" +

						" (CASE WHEN fa.l IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_LIC_FULL_ACCESS.getColumn() + ",\n" +
						" (CASE WHEN ro.l IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_LIC_READONLY.getColumn() + ",\n" +
						" (CASE WHEN fc.l IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_LIC_FULL_AND_COMMODITY.getColumn() + ",\n" +
						" (CASE WHEN apm.l IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_LIC_APM.getColumn() + ",\n" +

						" (CASE WHEN sb.l IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_LIC_SUBSIDIARY.getColumn() + ",\n" +
						" (CASE WHEN co.l IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_LIC_CONNEX.getColumn() + ",\n" +
						" (CASE WHEN se.l IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_LIC_SERVER.getColumn() + ",\n" +

						" (CASE WHEN ha.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_ADMINISTRATION.getColumn() + ",\n" +
						" (CASE WHEN hsa.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_SECADMIN.getColumn() + ",\n" +
						" (CASE WHEN his.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_ITSUPPORT.getColumn() + ",\n" +

						" (CASE WHEN husfo.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_FRONTOFFICE_US.getColumn() + ",\n" +
						" (CASE WHEN hhkfo.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_FRONTOFFICE_HK.getColumn() + ",\n" +
						" (CASE WHEN hukfo.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_FRONTOFFICE_UK.getColumn() + ",\n" +
						" (CASE WHEN hfos.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_FRONTOFFICE_SNR.getColumn() + ",\n" +

						" (CASE WHEN hsu.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_SERVERUSER.getColumn() + ",\n" +
						" (CASE WHEN hm.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_MIGRATION.getColumn() + ",\n" +
						" (CASE WHEN hmp.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_MARKET_PRICES.getColumn() + ",\n" +
						" (CASE WHEN hma.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_MANAGEMENT_APPROVAL.getColumn() + ",\n" +

						" (CASE WHEN hsw.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_SAFE_WAREHOUSE.getColumn() + ",\n" +
						" (CASE WHEN hed.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_EOD.getColumn() + ",\n" +
						" (CASE WHEN hst.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_STOCK_TAKE.getColumn() + ",\n" +
						" (CASE WHEN hri.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_RO_INVENTORY.getColumn() + ",\n" +
						" (CASE WHEN htvo.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_TRADEVIEWONLY.getColumn() + ",\n" +
						" (CASE WHEN hmar.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_MARKET.getColumn() + ",\n" +
						" (CASE WHEN hcr.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_CREDIT.getColumn() + ",\n" +
						" (CASE WHEN hcs.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_CREDIT_SENIOR.getColumn() + ",\n" +
						" (CASE WHEN hr.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_RISK.getColumn() + ",\n" +
						" (CASE WHEN hrs.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_RISK_SENIOR.getColumn() + ",\n" +
						" (CASE WHEN hbo.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_BACK_OFFICE.getColumn() + ",\n" +
						" (CASE WHEN hbous.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_BACK_OFFICE_US.getColumn() + ",\n" +
						" (CASE WHEN hbos.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_BACK_OFFICE_SNR.getColumn() + ",\n" +

						" (CASE WHEN ise.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_IT_SUPPORT_ELAVATED.getColumn() + ",\n" +
						" (CASE WHEN rbt.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_ROLEBASED_TESTING.getColumn() + ",\n" +
						" (CASE WHEN dep.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_DEPLOYMENT.getColumn() + ",\n" +
						" (CASE WHEN bpt.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_BO_Phys_TRANSFER.getColumn() + ",\n" +

						" (CASE WHEN hcon.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_CONNEX_WS_USER.getColumn() + ",\n" +
						" (CASE WHEN hpt.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_PURGE_TABLES.getColumn() + ",\n" +
						" (CASE WHEN hcnfo.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_CN_FRONT_OFFICE.getColumn() + ",\n" +
						" (CASE WHEN hapme.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_USER_APM_EDITOR.getColumn() + ",\n" +
						" (CASE WHEN hcnbo.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_BACK_OFFICE_CN.getColumn() + ",\n" +
						" (CASE WHEN hcnsw.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_SAFE_WAREHOUSE_CN.getColumn() + ",\n" +
						" (CASE WHEN hsupa.id IS NULL THEN 'No' ELSE 'Yes' END) " + HAS_SG_IT_SUPPORT_AUDIT.getColumn() + ",\n" +
						
						" p.fax " + Columns.USER_CATEGORISATION.getColumn() + ",p.email " + Columns.USER_EMAIL.getColumn() + ",\n" +
						
						" msi.last_login_time " + LAST_ACTIVE.getColumn() + ", csi.logincount " + LOGIN_COUNT.getColumn() + ",\n" +
						" a1.lu " + LAST_APM_ACTIVE.getColumn() + ",\n" + 
						" (CASE WHEN(scgl.screen_config_name)IS NULL THEN dsn.screen_config_name ELSE scgl.screen_config_name END) " + SCREEN_CONFIG_NAME.getColumn() + "\n" +

						" FROM personnel p\n" +
						" JOIN personnel_status ps ON (ps.id_number=p.status)\n" +
						" JOIN personnel_type pt ON (pt.id_number=p.personnel_type)\n" +
						" JOIN personnel lc ON (lc.id_number=p.authoriser)\n" +
						" JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
						" LEFT JOIN(SELECT license_type l,personnel_id i FROM pers_license_types_link p JOIN personnel_license_type t ON (t.type_id=p.license_type AND t.type_name='Full Access'))fa ON(p.id_number=fa.i)\n" +  
						" LEFT JOIN(SELECT license_type l,personnel_id i FROM pers_license_types_link p JOIN personnel_license_type t ON (t.type_id=p.license_type AND t.type_name='Read Only'))ro ON(p.id_number=ro.i)\n" + 
						" LEFT JOIN(SELECT license_type l,personnel_id i FROM pers_license_types_link p JOIN personnel_license_type t ON (t.type_id=p.license_type AND t.type_name='Full and Commodity'))fc ON(p.id_number=fc.i)\n" +  
						" LEFT JOIN(SELECT license_type l,personnel_id i FROM pers_license_types_link p JOIN personnel_license_type t ON (t.type_id=p.license_type AND t.type_name='APM'))apm ON(p.id_number=apm.i) \n" + 
						
						" LEFT JOIN(SELECT license_type l,personnel_id i FROM pers_license_types_link p JOIN personnel_license_type t ON (t.type_id=p.license_type AND t.type_name='Server'))se ON(p.id_number=se.i)\n" + 
						" LEFT JOIN(SELECT license_type l,personnel_id i FROM pers_license_types_link p JOIN personnel_license_type t ON (t.type_id=p.license_type AND t.type_name='Subsidiary'))sb ON(p.id_number=sb.i)\n" + 
						" LEFT JOIN(SELECT license_type l,personnel_id i FROM pers_license_types_link p JOIN personnel_license_type t ON (t.type_id=p.license_type AND t.type_name='Connex JMS Operator'))co ON(p.id_number=co.i)\n" + 
						
						" LEFT JOIN country c ON (c.id_number=p.country)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='Administration'))ha ON(ha.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='IT Support'))his ON(his.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='Security Admin'))hsa ON(hsa.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='US Front Office'))husfo ON(husfo.id=p.id_number)\n" +

						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='Server User'))hsu ON(hsu.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='Migration'))hm ON(hm.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='Market Prices'))hmp ON(hmp.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='Management Approval'))hma ON(hma.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='Safe / Warehouse'))hsw ON(hsw.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='EOD'))hed ON(hed.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='Stock Take'))hst ON(hst.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='RO Inventory'))hri ON(hri.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='Trade View Only'))htvo ON(htvo.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='HK Front Office'))hhkfo ON(hhkfo.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='UK Front Office'))hukfo ON(hukfo.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='Front Office Senior'))hfos ON(hfos.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='Market'))hmar ON(hmar.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='Credit'))hcr ON(hcr.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='Credit Senior'))hcs ON(hcs.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='Risk'))hr ON(hr.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='Risk Senior'))hrs ON(hrs.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='Back Office'))hbo ON(hbo.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='Back Office US'))hbous ON(hbous.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='Back Office Senior'))hbos ON(hbos.id=p.id_number)\n" +

						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='IT Support Elavated'))ise ON(ise.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='RoleBasedTesting'))rbt ON(rbt.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='Deployment'))dep ON(dep.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='BO Phys Transfer'))bpt ON(bpt.id=p.id_number)\n" +
						
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='connexWsUser'))hcon ON(hcon.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='PurgeTables'))hpt ON(hpt.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='CN Front Office'))hcnfo ON(hcnfo.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='User APM Editor'))hapme ON(hapme.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='Back Office CN'))hcnbo ON(hcnbo.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='Safe / Warehouse CN'))hcnsw ON(hcnsw.id=p.id_number)\n" +
						" LEFT JOIN(SELECT u.user_number id FROM users_to_groups u JOIN groups g ON(g.id_number=u.group_number AND g.name='IT Support Audit'))hsupa ON(hsupa.id=p.id_number)\n" +
						
						
						
						" LEFT JOIN(SELECT msi.personnel_id, max(msi.row_creation) last_login_time FROM  sysaudit_activity_log msi WHERE msi.action_id=15 GROUP BY msi.personnel_id) msi ON(msi.personnel_id=p.id_number)\n" +
						" LEFT JOIN(SELECT sal.personnel_id,COUNT(sal1.row_creation_date) logincount \n" +
						"    FROM sysaudit_activity_log sal\n" +
						"    JOIN (SELECT sal.personnel_id,MAX(sal.id_number) id_number,sal1.row_creation_date FROM sysaudit_activity_log sal\n" +
						"         JOIN (SELECT DISTINCT isal.personnel_id,CONVERT(date, isal.row_creation) row_creation_date FROM sysaudit_activity_log isal WHERE isal.action_id=15 AND isal.row_creation>'" + startOfYear + "') sal1 ON(sal.personnel_id=sal1.personnel_id)\n" + 
						"         GROUP BY sal.personnel_id,sal1.row_creation_date\n" +
						"    )sal1 ON(sal1.personnel_id=sal.personnel_id AND sal.id_number=sal1.id_number)\n" +
						"    GROUP BY sal.personnel_id \n" +
						" )csi ON (csi.personnel_id=p.id_number)\n" +
						" LEFT JOIN (\n" +
						"    SELECT MAX(last_update)lu,\n" + 
						"    LEFT(RIGHT(filename,LEN(filename)-13),CHARINDEX('_',RIGHT(filename,LEN(filename)-13))-1)un\n" +
						"		   FROM tfe_file_system\n" +
						"		   WHERE last_update > '" + startOfYear + "'\n" +
						"		   GROUP BY LEFT(RIGHT(filename,LEN(filename)-13),CHARINDEX('_',RIGHT(filename,LEN(filename)-13))-1))a1 ON(a1.un=p.name)\n" +
						" LEFT JOIN (SELECT scgl.screen_config_id, scgl.screen_config_user_id , scv.screen_config_name FROM screen_cfg_generic_link scgl\n" + 
						"		JOIN  screen_config_view  scv ON (scv.screen_config_id =scgl.screen_config_id)\n" +
						"		WHERE  scgl.screen_config_type_id = 2 \n" +
						"		) scgl ON (scgl.screen_config_user_id = p.id_number),\n" +
						" (SELECT scv.screen_config_name FROM screen_cfg_generic_link scgl\n" +
						"		JOIN screen_config_view scv ON (scv.screen_config_id=scgl.screen_config_id)\n" +
						"		WHERE scgl.screen_config_type_id=2 AND scgl.system_default=1) dsn\n" +

						" WHERE qr.unique_id=" + queryID + 
						(runForAll ? "" : " \n"
								+ " AND p.id_number = " + personnel_id );


			DBaseTable.execISql(tblPersonnelData, sqlCommand);

			sqlCommand = "SELECT p.id_number " + PERSONNEL_ID.getColumn() + ",p.name " + PERSONNEL_SHORTNAME.getColumn() + ",\n" +


					"(CASE WHEN fg_gen.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_General.getColumn() + ",\n" + 
					"(CASE WHEN fg_tra.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_Trading.getColumn() + ",\n" + 
					"(CASE WHEN fg_ops.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_Operations.getColumn() + ",\n" + 
					"(CASE WHEN fg_cre.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_Credit.getColumn() + ",\n" +
					"(CASE WHEN fg_ope.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_OptionExercise.getColumn() + ",\n" +
					"(CASE WHEN fg_well.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_WellheadScheduling.getColumn() + ",\n" +
					"(CASE WHEN fg_ca.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_CorporateActions.getColumn() + ",\n" +
					"(CASE WHEN fg_mag.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_ManagementApprovalGroup.getColumn() + ",\n" +
					"(CASE WHEN fg_phk.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_JMPriceHK.getColumn() + ",\n" +
					"(CASE WHEN fg_puk.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_JMPriceUK.getColumn() + ",\n" +
					"(CASE WHEN fg_pus.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_JMPriceUS.getColumn() + ",\n" +
					"(CASE WHEN fg_pcn.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_JMPriceCN.getColumn() + ",\n" +
					"(CASE WHEN fg_tcuk.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_TradeConfirmationsUK.getColumn() + ",\n" +
					"(CASE WHEN fg_tcus.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_TradeConfirmationsUS.getColumn() + ",\n" +
					"(CASE WHEN fg_tchk.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_TradeConfirmationsHK.getColumn() + ",\n" +
					"(CASE WHEN fg_tccn.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_TradeConfirmationsCN.getColumn() + ",\n" +
					"(CASE WHEN fg_inuk.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_InvoicesUK.getColumn() + ",\n" +
					"(CASE WHEN fg_inus.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_InvoicesUS.getColumn() + ",\n" +
					"(CASE WHEN fg_inhk.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_InvoicesHK.getColumn() + ",\n" +
					"(CASE WHEN fg_incn.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_InvoicesCN.getColumn() + ",\n" +
					"(CASE WHEN fg_truk.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_TransfersUK.getColumn() + ",\n" +
					"(CASE WHEN fg_trus.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_TransfersUS.getColumn() + ",\n" +
					"(CASE WHEN fg_trhk.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_TransfersHK.getColumn() + ",\n" +
					"(CASE WHEN fg_trcn.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_TransfersCN.getColumn() + ",\n" +
					"(CASE WHEN fg_ms.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_MetalStatements.getColumn() + ",\n" +
					"(CASE WHEN fg_log.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_Logistics.getColumn() + ",\n" +
					"(CASE WHEN fg_lrdeal.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_LRDealing.getColumn() + ",\n" +
					"(CASE WHEN fg_lrlea.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_LRLease.getColumn() + ",\n" +
					"(CASE WHEN fg_lrliq.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_LRLiquidity.getColumn() + ",\n" +
					"(CASE WHEN fg_lrsum.id IS NULL THEN 'No' ELSE 'Yes' END) " + Columns.HAS_FG_LRSummary.getColumn() + "\n" +

					" FROM personnel p\n" +
					" JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='General'))fg_gen ON(fg_gen.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='Trading'))fg_tra ON(fg_tra.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='Operations'))fg_ops ON(fg_ops.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='Credit'))fg_cre ON(fg_cre.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='Option Exercise'))fg_ope ON(fg_ope.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='Wellhead Scheduling'))fg_well ON(fg_well.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='Corporate Actions'))fg_ca ON(fg_ca.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='Management Approval Group'))fg_mag ON(fg_mag.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='JM Price HK'))fg_phk ON(fg_phk.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='JM Price UK'))fg_puk ON(fg_puk.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='JM Price US'))fg_pus ON(fg_pus.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='JM Price CN'))fg_pcn ON(fg_pcn.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='Trade Confirmations UK'))fg_tcuk ON(fg_tcuk.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='Trade Confirmations US'))fg_tcus ON(fg_tcus.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='Trade Confirmations HK'))fg_tchk ON(fg_tchk.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='Trade Confirmations CN'))fg_tccn ON(fg_tccn.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='Invoices UK'))fg_inuk ON(fg_inuk.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='Invoices US'))fg_inus ON(fg_inus.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='Invoices HK'))fg_inhk ON(fg_inhk.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='Invoices CN'))fg_incn ON(fg_incn.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='Transfers UK'))fg_truk ON(fg_truk.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='Transfers US'))fg_trus ON(fg_trus.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='Transfers HK'))fg_trhk ON(fg_trhk.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='Transfers CN'))fg_trcn ON(fg_trcn.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='Metal Statements'))fg_ms ON(fg_ms.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='Logistics'))fg_log ON(fg_log.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='EOD Limits Reporting - Dealing'))fg_lrdeal ON(fg_lrdeal.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='EOD Limits Reporting - Lease'))fg_lrlea ON(fg_lrlea.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='EOD Limits Reporting - Liquidity'))fg_lrliq ON(fg_lrliq.id=p.id_number)\n" +  
					"LEFT JOIN(SELECT g.personnel_id id, g.func_group_id fid FROM personnel_functional_group g JOIN functional_group fg ON(fg.id_number=g.func_group_id AND fg.name='EOD Limits Reporting - Summary'))fg_lrsum ON(fg_lrsum.id=p.id_number)\n" +  

					" WHERE qr.unique_id=" + queryID + 
					(runForAll ? "" : " \n"
							+ " AND p.id_number = " + personnel_id );
			 

			DBaseTable.execISql(tblPersonnelFunctionalGroup, sqlCommand);
			tblPersonnelData.select(tblPersonnelFunctionalGroup, "*", Columns.PERSONNEL_ID.getColumn() + " EQ $" + Columns.PERSONNEL_ID.getColumn());

			
			// add the extra columns
			formatReturnTable(tblPersonnelData);


			// Get the pointers
			totalRows = tblPersonnelData.getNumRows();

			// @formatter:off
			String copyColumns = "";
			boolean firstColumn = true;
			for (Columns column : Columns.values()) {
				if (firstColumn) {
					firstColumn = false;
					copyColumns = column.getColumn();
				} else {
					copyColumns = copyColumns + "," + column.getColumn();
				}
			}
			returnt.select(tblPersonnelData, copyColumns, Columns.PERSONNEL_ID.getColumn() + " GT 0");
			// @formatter:on

		} catch (Exception e) {
			throw new OException(e.getMessage());
		} finally {
			Logging.debug("Results processing finished. Total Number of results recovered: " + totalRows + " processed: " + tblPersonnelData.getNumRows());

			if (Table.isTableValid(tblPersonnelData) == 1) {
				tblPersonnelData.destroy();
			}
			if (Table.isTableValid(tblPersonnelFunctionalGroup) == 1) {
				tblPersonnelFunctionalGroup.destroy();
			}
		}
	}

	/**
	 * @param returnt
	 *            - Table to return to report
	 * @throws OException
	 *             - Error, cannot format table
	 */
	private void formatReturnTable(Table returnt) throws OException	{

		for (Columns column : Columns.values()) {
			if (returnt.getColNum(column.getColumn()) < 0) {
				returnt.addCol(column.getColumn(), column.getType(), column.getTitle());
			}
		}

		return;
	}

	private void addColumnsToMetaData(Table tableCreate) throws OException {
		
		int iColCount = 1; 
		for (Columns column : Columns.values()) {	
			String iColCountStr = "" + iColCount ; 
			if (iColCountStr.length()==1){
				iColCountStr = "0" + iColCount ;
			}
			addColumnToMetaData(tableCreate, column.getColumn(), iColCountStr + "_" + column.getTitle(), column.getNameType(), column.getColumnCaption());
			iColCount ++;
		}
	}

	private void addColumnToMetaData(Table columnMetadata, String colColumnName, String colColumnCaption, String columnType, String detailedCaption) throws OException {

		int rowAdded = columnMetadata.addRow();
		columnMetadata.setString("table_name", rowAdded, "generated_values");
		columnMetadata.setString("column_name", rowAdded, colColumnName);
		columnMetadata.setString("column_title", rowAdded, colColumnCaption);
		columnMetadata.setString("olf_type", rowAdded, columnType);
		columnMetadata.setString("column_description", rowAdded, detailedCaption);
	}
}
