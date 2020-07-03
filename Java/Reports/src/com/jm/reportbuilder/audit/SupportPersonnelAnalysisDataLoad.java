package com.jm.reportbuilder.audit;


import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.*;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisDataLoad.Columns.LAST_CHANGED;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisDataLoad.Columns.MODIFIED_DATE;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisDataLoad.Columns.PERSONNEL_COUNTRY;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisDataLoad.Columns.PERSONNEL_FIRSTNAME;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisDataLoad.Columns.PERSONNEL_ID;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisDataLoad.Columns.PERSONNEL_LASTNAME;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisDataLoad.Columns.PERSONNEL_SHORTNAME;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisDataLoad.Columns.PERSONNEL_STATUS;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisDataLoad.Columns.PERSONNEL_TYPE;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisDataLoad.Columns.PERSONNEL_VERSION;
import static com.olf.openjvs.enums.COL_TYPE_ENUM.COL_DATE_TIME;
import static com.olf.openjvs.enums.COL_TYPE_ENUM.COL_INT;
import static com.olf.openjvs.enums.COL_TYPE_ENUM.COL_STRING;

import java.text.ParseException;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
//import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_DATALOAD)
public class SupportPersonnelAnalysisDataLoad implements IScript {

	

	protected enum Columns {
		//SupportPersonnelAnalysisConstants
		PERSONNEL_ID(COL_PERSONNEL_ID, "Personnel ID", COL_INT, "Personnel ID"){	},
		PERSONNEL_SHORTNAME(COL_PERSONNEL_SHORTNAME, "Short Name", COL_STRING, "Personnel Short Name"){		},
		PERSONNEL_FIRSTNAME(COL_PERSONNEL_FIRSTNAME, "First Name", COL_STRING, "Personnel First Name"){		},
		PERSONNEL_LASTNAME(COL_PERSONNEL_LASTNAME, "Last Name", COL_STRING, "Personnel First Name")		{		},
		PERSONNEL_COUNTRY(COL_COUNTRY, "Country", COL_STRING, "Personnel Region Country"){		},
		PERSONNEL_TYPE(COL_PERSONNEL_TYPE, "Personnel Type", COL_STRING, "Personnel Type")		{		},
		PERSONNEL_STATUS(COL_PERSONNEL_STATUS, "Personnel Status", COL_STRING, "Personnel Status")		{		},
		
		PERSONNEL_VERSION(COL_PERSONNEL_VERSION, "Personnel Version", COL_INT, "Current Personnel Version")		{		},
		PERSONNEL_LASTVERSION(COL_PERSONNEL_LASTVERSION, "Personnel Last Version", COL_INT, "Previous Personnel Version")		{		},
		LAST_CHANGED(COL_LAST_CHANGED, "Last Changed By", COL_STRING, "Last Changed By")		{		},
		MODIFIED_DATE(COL_MODIFIED_DATE, "Modified Date", COL_DATE_TIME, "The Last Time the user was updated")		{		},
		
		HEADER_DIFFERENCE(COL_HEADER_DIFFERENCE, "Header Change", COL_STRING, "Header Changes")		{		},
		SEC_DIFFERENCE(COL_SEC_DIFFERENCE, "Sec Group Change", COL_STRING, "Sec Group Change")		{		},
		LICENCE_DIFFERENCE(COL_LICENCE_DIFFERENCE, "Licence Change", COL_STRING, "Licence Changes")		{		},
		FUNCTIONAL_DIFFERENCE(COL_FUNCTIONAL_DIFFERENCE, "Functional Group Change", COL_STRING, "Functional Group Change")		{		},

		EXPLANATION (COL_EXPLANATION , "Explanation", COL_STRING, "Explanation - Filled in later")		{		},
		
		;

		private String _name;
		private String _title;
		private COL_TYPE_ENUM _format;
		private String _columnCaption;

		private Columns(String name, String title, COL_TYPE_ENUM format, String columnCaption) {
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
			switch (thisType)
			{
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


	private int report_date;

	@Override
	/**
	 * execute: Main Gateway into script from the AScript extender class
	 */
	public void execute(IContainerContext context) throws OException 	{


		try {
			// Setting up the log file.
			//Constants Repository init
			ConstRepository constRep = new ConstRepository(SupportPersonnelAnalysisConstants.REPO_CONTEXT, SupportPersonnelAnalysisConstants.REPO_SUB_CONTEXT);
			SupportPersonnelAnalysisConstants.initPluginLog(constRep); //Plug in Log init

			
			// PluginLog.init("INFO");

			PluginLog.info("Start  " + getClass().getSimpleName());

			Table argt = context.getArgumentsTable();
			Table returnt = context.getReturnTable();

			int modeFlag = argt.getInt("ModeFlag", 1);
			PluginLog.debug(getClass().getSimpleName() + " - Started Data Load Script for SupportPersonnelAnalysisDataLoad Reports - mode: " + modeFlag);

			if (modeFlag == 0)
			{
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


				PluginLog.debug("Completed Data Load Script Metadata:");

				return;
			} else {

				formatReturnTable(returnt);
				if (modeFlag == 0) {
					return;
				}
 
				report_date = Util.getBusinessDate();// OCalendar.parseString(tblTemp.getString("parameter_value", tblTemp.unsortedFindString("parameter_name", "GEN_TIME", SEARCH_CASE_ENUM.CASE_INSENSITIVE)));

				PluginLog.debug("Running Data Load Script For Date: " + OCalendar.formatDateInt(report_date));


				PluginLog.info("Enrich data" );
				enrichData(returnt);

				PluginLog.info("Data Num Rows: " + returnt.getNumRows());



			}

		} catch (Exception e) {

			String errMsg = e.toString();
			com.olf.openjvs.Util.exitFail(errMsg);
			throw new RuntimeException(e);
		} finally {

		}

		PluginLog.info("End " + getClass().getSimpleName());

		return;
	}


	/**
	 * Method recoveryTransactionalInformaiton Recovers TranPointers from Query results and extract in final return table information for particular Transaction fields
	 * 
	 * @param queryID
	 * @throws OException
	 * @throws ParseException
	 */
	private void enrichData(Table returnt) throws OException {

		Table tblAllPersonnelAuditData = Table.tableNew();
		Table tblAllPersonnelAnalysisData = Table.tableNew();
		
		Table tblPersonnelData = Table.tableNew();


		int totalRows = 0;
		String sqlCommand;

		PluginLog.debug("Attempt to recover Personnel information.");

		try {
			tblAllPersonnelAuditData = getAllPersnnelAuditData();
			tblAllPersonnelAnalysisData = getAllPersnnelAnalysisData();
			ODateTime dateValue = getDateValue();
			sqlCommand = "SELECT uspa.*\n" +  
						" FROM " + SupportPersonnelAuditConstants.USER_SUPPORT_PERSONNEL_AUDIT + " uspa\n" +
						" WHERE uspa." + SupportPersonnelAuditConstants.COL_LATEST_VERSION + " = 1" ; 	// + dateValue.formatForDbAccess() 
			DBaseTable.execISql(tblPersonnelData, sqlCommand);

			
			tblPersonnelData.setColName(COL_ID_NUMBER, PERSONNEL_ID.getColumn());
			tblPersonnelData.setColName(COL_PER_NAME, PERSONNEL_SHORTNAME.getColumn());
			tblPersonnelData.setColName(COL_PER_FIRSTNAME, PERSONNEL_FIRSTNAME.getColumn());
			tblPersonnelData.setColName(COL_PER_LASTNAME, PERSONNEL_LASTNAME.getColumn());
			
			tblPersonnelData.setColName(COL_PER_MODIFIED_DATE, MODIFIED_DATE.getColumn());
			tblPersonnelData.setColName(COL_PER_PERSONNEL_CURRENT_VERSION, PERSONNEL_VERSION.getColumn());
			tblPersonnelData.setColName(COL_PER_MOD_USER, LAST_CHANGED.getColumn());
			tblPersonnelData.setColName(COL_PER_PESONNEL_STATUS, PERSONNEL_STATUS.getColumn());
			tblPersonnelData.setColName(COL_PER_PERSONNEL_TYPE, PERSONNEL_TYPE.getColumn());
			tblPersonnelData.setColName(COL_PER_COUNTRY, PERSONNEL_COUNTRY.getColumn() );
			
			// add the extra columns
			formatReturnTable(tblPersonnelData);

			tblPersonnelData.addCol("delete_me", COL_INT);
			
			Table distinctPersonnel = Table.tableNew();
			distinctPersonnel.select(tblPersonnelData,"DISTINCT, " + PERSONNEL_ID.getColumn(), PERSONNEL_ID.getColumn() + " GT 0");
			int numPersonnel= distinctPersonnel.getNumRows();
			for (int iLoop = 1; iLoop<=numPersonnel;iLoop++){
				int personnelID = distinctPersonnel.getInt(PERSONNEL_ID.getColumn(), iLoop);
				Table currentVersionTbl = getCurrentVersionNum(tblPersonnelData,personnelID);
				int currentVer = currentVersionTbl.getInt(PERSONNEL_VERSION.getColumn(), 1);
				


				int getThisRowLoop = getThisRow(tblPersonnelData, personnelID, currentVer);

				
				boolean currentVersionExist = doesCurrentVersionExist(tblAllPersonnelAnalysisData,personnelID,currentVer);
				if (currentVersionExist){
					tblPersonnelData.setInt("delete_me",getThisRowLoop,1);
				} else {
					Table previousVersionTbl = getPreviousVersionNum(tblAllPersonnelAnalysisData, tblAllPersonnelAuditData,personnelID,currentVer, dateValue);

					int previousVer = 0;
					if (previousVersionTbl.getNumRows()>0){
						previousVer = previousVersionTbl.getInt(COL_PER_PERSONNEL_CURRENT_VERSION, 1);
					} 

					
					String headerDifference = getHeaderDiff(currentVersionTbl, previousVersionTbl, previousVer);
					String licenceDifference = getLicenceDiff(currentVersionTbl, previousVersionTbl, previousVer);
					String securityDifference = getSecurityDiff(currentVersionTbl, previousVersionTbl, previousVer);
					String functionalDifference = getFunctionalDiff(currentVersionTbl, previousVersionTbl, previousVer);
					
					if (headerDifference.length()>0 || licenceDifference.length()>0 || securityDifference.length()>0 || functionalDifference.length()>0){
						tblPersonnelData.setString(Columns.HEADER_DIFFERENCE.getColumn(), getThisRowLoop, headerDifference);
						tblPersonnelData.setString(Columns.SEC_DIFFERENCE.getColumn(), getThisRowLoop, securityDifference);
						tblPersonnelData.setString(Columns.LICENCE_DIFFERENCE.getColumn(), getThisRowLoop, licenceDifference);
						tblPersonnelData.setString(Columns.FUNCTIONAL_DIFFERENCE.getColumn(), getThisRowLoop, functionalDifference);
						if (previousVer>0){
							tblPersonnelData.setInt(Columns.PERSONNEL_LASTVERSION.getColumn(), getThisRowLoop, previousVer);
						} else {
							tblPersonnelData.setString(Columns.EXPLANATION.getColumn(), getThisRowLoop, "New User");
						}
					} else {
						tblPersonnelData.setInt("delete_me",getThisRowLoop,1);
					}
					previousVersionTbl.destroy();
				}
				setOtherRowsToDelete(tblPersonnelData, getThisRowLoop, personnelID, currentVer);
				
				
				currentVersionTbl.destroy();
			}
			
			tblPersonnelData.deleteWhereValue("delete_me", 1);
			distinctPersonnel.destroy();
			
			
			

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
			PluginLog.debug("Results processing finished. Total Number of results recovered: " + totalRows + " processed: " + tblPersonnelData.getNumRows());

			if (Table.isTableValid(tblPersonnelData) == 1) {
				tblPersonnelData.destroy();
			}
			if (Table.isTableValid(tblAllPersonnelAuditData) == 1) {
				tblAllPersonnelAuditData.destroy();
			}
			if (Table.isTableValid(tblAllPersonnelAnalysisData) == 1) {
				tblAllPersonnelAnalysisData.destroy();
			}
			
		}
	}



	private boolean doesCurrentVersionExist(Table tblAllPersonnelAnalysisData, int personnelID, int currentVer) throws Exception {
		int iLoop = 0; 
		int numRows = tblAllPersonnelAnalysisData.getNumRows();
		boolean foundRecord = false;
		for (iLoop = 1; iLoop<=numRows;iLoop++){
			int thisPersonalID = tblAllPersonnelAnalysisData.getInt(Columns.PERSONNEL_ID.getColumn(), iLoop);
			if (personnelID==thisPersonalID){
				int thisCurrentVer = tblAllPersonnelAnalysisData.getInt( SupportPersonnelAnalysisConstants.COL_PER_PERSONNEL_CURRENT_VERSION , iLoop);
				if (currentVer==thisCurrentVer){
					foundRecord = true;	
					break;
				}
			}
		}
		return foundRecord ;
	}


	private void setOtherRowsToDelete(Table tblPersonnelData, int getThisRowLoop, int personnelID, int currentVer) throws OException {
		
		int numRows = tblPersonnelData.getNumRows();
		
		for (int iLoop = 1; iLoop<=numRows;iLoop++){
			int thisPersonalID = tblPersonnelData.getInt(Columns.PERSONNEL_ID.getColumn(), iLoop);
			if (personnelID==thisPersonalID){
				int thisCurrentVer = tblPersonnelData.getInt(Columns.PERSONNEL_VERSION.getColumn(), iLoop);
				if (currentVer==thisCurrentVer){
					if (iLoop!=getThisRowLoop){
						tblPersonnelData.setInt("delete_me", iLoop, 1);
					}
				}
			}
		}
	}


	private String getSecurityDiff(Table currentVersionTbl, Table previousVersionTbl, int previousVer) throws OException {

		String securityRoleDiff = "";
		 
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_SECURITY_ADMIN, "Sec Admin", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_IT_SUPPORT, "IT Support", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_FO_UK, "FO UK", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_FO_HK, "FO HK", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_FO_US, "FO US", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_FO_SNR, "FO Snr", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_ADMINISTRATOR, "Administrator", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_SERVER_USER, "Server User", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_MIGRATION, "Migration", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_MARKET_PRICES, "Market Prices", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_MAN_APPROVAL, "Man Approval", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_SAFE_WAREHOUSE, "Safe Warehouse", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_EOD, "EOD", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_STOCK_TAKE, "Stock Take", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_RO_INVENTORY, "RO Inventory", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_TRADE_ONLY_VIEW, "Trade Only View", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_MARKET_USER, "Market User", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_CREDIT, "Credit", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_CREDIT_SNR, "Credit Snr", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_RISK, "Risk", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_RISK_SNR, "Risk Snr", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_BO, "Back Office", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_BO_US, "Back Office US", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_BO_SNR, "Back Office Snr", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_SUPPORT_ELEVATED, "IT Elavated", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_ROLE_BASED_TESTING, "Role Based Testing", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_DEPLOYMENT, "Deployment", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_PHYS_TRANSFER, "BO Phys Transfer", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_CONNEX_WS, "Connex", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_PURGE_TABLES, "Purge Tables", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_FO_CN, "CN FrontOffice", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_AMP_EDITOR, "APM Editor", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_BO_CN, "CN BackOffice", previousVer, securityRoleDiff);
 		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_ROLE_SAFE_CN, "CN Safe", previousVer, securityRoleDiff);
		securityRoleDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_SG_IT_AUDIT, "IT Audit", previousVer, securityRoleDiff);
		 
	 
		return securityRoleDiff;
	}


	private String columnDifference(Table currentVersionTbl, Table previousVersionTbl,String colCompare, String displayColName, int previousVer, String retStringDifference) throws OException {
		 
		if (!currentVersionTbl.getString(colCompare, 1).equals(previousVersionTbl.getString(colCompare , 1)) ){
			if (previousVer>0 && "Unknown".equals(previousVersionTbl.getString(colCompare , 1))){
				// Do nothing first time round
			} else if ("Yes".equals(currentVersionTbl.getString(colCompare , 1))){
				retStringDifference = retStringDifference  + "+ " + displayColName + ", ";
			} else {
				if (previousVer>0){
					retStringDifference = retStringDifference  + "- " + displayColName + ", ";
				}
			}
		}
		return retStringDifference;
	}

	private String columnHeaderDifference(Table currentVersionTbl, Table previousVersionTbl,String colCurrentCompare, String colPreviousCompare, String displayColName , String retStringDifference) throws OException {
		 
		String currentValue = currentVersionTbl.getString(colCurrentCompare, 1);
		
		String previousValue = previousVersionTbl.getString(colPreviousCompare, 1);
		if (currentValue==null ){
			currentValue = "Not Set";
		}
		if (previousValue ==null){
			previousValue = "Not Set";
		}
		
		
		if (!currentValue.equals(previousValue) ){
			retStringDifference = retStringDifference  + " " + displayColName + ": " + previousValue + " -> " + currentValue + ", ";
		}
	
		return retStringDifference;
	}

	
	private String getLicenceDiff(Table currentVersionTbl, Table previousVersionTbl, int previousVer) throws OException {
		String licenceDiff = "";
		
		licenceDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_LT_FULL_COMMODITY , "F&C", previousVer, licenceDiff);
		licenceDiff = columnDifference(currentVersionTbl, previousVersionTbl,COL_LT_FULL_ACCESS  , "Full Access", previousVer, licenceDiff);
		licenceDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_LT_READ_ONLY , "Read Only", previousVer, licenceDiff);
		licenceDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_LT_APM , "APM", previousVer, licenceDiff);
		licenceDiff = columnDifference(currentVersionTbl, previousVersionTbl,COL_LT_SUBSIDIARY  , "Subsidiary", previousVer, licenceDiff);
		licenceDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_LT_CONNEX , "Connex", previousVer, licenceDiff);
		licenceDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_LT_SERVER , "Server", previousVer, licenceDiff);

		return licenceDiff;
	}

	private String getHeaderDiff(Table currentVersionTbl, Table previousVersionTbl, int previousVer) throws OException {
		String headerDiff = "";
		 

		headerDiff = columnHeaderDifference(currentVersionTbl, previousVersionTbl,PERSONNEL_COUNTRY.getColumn(), COL_PER_COUNTRY , "Country", headerDiff);
		headerDiff = columnHeaderDifference(currentVersionTbl, previousVersionTbl,PERSONNEL_TYPE.getColumn(), COL_PER_PERSONNEL_TYPE , "Type", headerDiff);
		headerDiff = columnHeaderDifference(currentVersionTbl, previousVersionTbl, PERSONNEL_STATUS.getColumn(),COL_PER_PESONNEL_STATUS , "Status", headerDiff);
		headerDiff = columnHeaderDifference(currentVersionTbl, previousVersionTbl, COL_PER_CAT , COL_PER_CAT, "Categogy", headerDiff);
		headerDiff = columnHeaderDifference(currentVersionTbl, previousVersionTbl, COL_PER_EMAIL , COL_PER_EMAIL, "Email", headerDiff);
		if (headerDiff.length()>0){  
			int currentVer = currentVersionTbl.getInt(COL_PERSONNEL_VERSION, 1);
			int previousVers = previousVersionTbl.getInt(COL_PER_PERSONNEL_CURRENT_VERSION, 1);
			headerDiff = "Version: " + previousVers + " -> " + currentVer + ", " + headerDiff;
		}
		


		return headerDiff;
	}


	private String getFunctionalDiff(Table currentVersionTbl, Table previousVersionTbl, int previousVer) throws OException {
		String functionalDiff = "";

		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_General , "General", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_Trading , "Trading", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_Operations , "Operations", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_Credit , "Credit", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_OptionExercise , "OptionExercise", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_WellheadScheduling , "WellheadScheduling", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_CorporateActions , "CorporateActions", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_ManagementApprovalGroup , "ManagementApprovalGroup", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_JMPriceHK , "JMPriceHK", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_JMPriceUK , "JMPriceUK", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_JMPriceUS , "JMPriceUS", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_JMPriceCN , "JMPriceCN", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_TradeConfirmationsUK , "TradeConfirmationsUK", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_TradeConfirmationsUS , "TradeConfirmationsUS", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_TradeConfirmationsHK , "TradeConfirmationsHK", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_TradeConfirmationsCN , "TradeConfirmationsCN", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_InvoicesUK , "InvoicesUK", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_InvoicesUS , "InvoicesUS", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_InvoicesHK , "InvoicesHK", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_InvoicesCN , "InvoicesCN", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_TransfersUK , "TransfersUK", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_TransfersUS , "TransfersUS", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_TransfersHK , "TransfersHK", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_TransfersCN , "TransfersCN", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_MetalStatements , "MetalStatements", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_Logistics , "Logistics", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_LRDealing , "LR_Dealing", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_LRLease , "LR_Lease", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_LRLiquidity , "LR_Liquidity", previousVer, functionalDiff);
		functionalDiff = columnDifference(currentVersionTbl, previousVersionTbl, COL_FUNCTIONALGROUP_LRSummary , "LR_Summary", previousVer, functionalDiff);
		


		return functionalDiff;
	}

	
	private int getThisRow(Table tblPersonnelData, int personnelID, int currentVer) throws OException {
		int iLoop = 0; 
		int numRows = tblPersonnelData.getNumRows();
		for (iLoop = 1; iLoop<=numRows;iLoop++){
			int thisPersonalID = tblPersonnelData.getInt(Columns.PERSONNEL_ID.getColumn(), iLoop);
			if (personnelID==thisPersonalID){
				int thisCurrentVer = tblPersonnelData.getInt(Columns.PERSONNEL_VERSION.getColumn(), iLoop);
				if (currentVer==thisCurrentVer){
					break;
				}
			}
		}
		return iLoop;
	}

	private Table getPreviousVersionNum(Table tblAllPersonnelAnalysisData, Table tblAllPersonnelAuditData, int personnelID, int currentVer, ODateTime dateValue) throws OException {
	
		Table retTable = Table.tableNew();
		retTable.select (tblAllPersonnelAnalysisData,"*", Columns.PERSONNEL_ID.getColumn()+ " EQ " + personnelID + " AND " + COL_PER_PERSONNEL_CURRENT_VERSION + " LE " +  currentVer);

		retTable.sortCol(COL_PER_PERSONNEL_CURRENT_VERSION);
		
		int retTableCount = retTable.getNumRows();
		int lastVersion = 0;
		if (retTableCount>0){
			lastVersion = retTable.getInt(COL_PER_PERSONNEL_CURRENT_VERSION,retTableCount );
		}
		
		
		if (lastVersion>0){
			retTable.destroy();
			retTable = Table.tableNew();
			retTable.select (tblAllPersonnelAuditData,"*", Columns.PERSONNEL_ID.getColumn()+ " EQ " + personnelID + " AND " + COL_PER_PERSONNEL_CURRENT_VERSION + " EQ " +  lastVersion);

		}
		

		
		
		return retTable;
	}



	private ODateTime getDateValue() throws OException {
		ODateTime dateValue = ODateTime.dtNew();

		String sql = "SELECT date_value FROM USER_const_repository\n" +
					 " WHERE context='" + SupportPersonnelAnalysisConstants.REPO_CONTEXT + "'\n" +
					 " AND sub_context = '" + SupportPersonnelAnalysisConstants.REPO_SUB_CONTEXT + "'\n" +
				     " AND name = 'LastRunTime'";
		Table tblPersonnelData = Table.tableNew();

		DBaseTable.execISql(tblPersonnelData, sql);
		
		dateValue = tblPersonnelData.getDateTime("date_value", 1);
		int dateComponent = dateValue.getDate();
		int timeComponent = dateValue.getTime();
		dateValue = ODateTime.dtNew();
		dateValue.setDateTime(dateComponent, timeComponent);
		tblPersonnelData.destroy();
		return dateValue;
	}


	private Table getAllPersnnelAuditData() throws OException {
		Table tblPersonnelData = Table.tableNew();
		String sqlCommand = "SELECT *\n" +  
				" FROM " + SupportPersonnelAuditConstants.USER_SUPPORT_PERSONNEL_AUDIT;
		DBaseTable.execISql(tblPersonnelData, sqlCommand);
		return tblPersonnelData;
	}

	private Table getAllPersnnelAnalysisData() throws OException {
		Table tblPersonnelData = Table.tableNew();
		String sqlCommand = "SELECT *\n" +  
				" FROM " + SupportPersonnelAnalysisConstants.USER_SUPPORT_PERSONNEL_ANALYSIS;
		DBaseTable.execISql(tblPersonnelData, sqlCommand);
		return tblPersonnelData;
	}

	
	private Table getCurrentVersionNum(Table tblPersonnelData, int personnelID) throws OException {
		
		Table retTable = Table.tableNew();
		retTable.select (tblPersonnelData,"*", Columns.PERSONNEL_ID.getColumn() + " EQ " + personnelID);
		retTable.addCol("delete_me" , COL_INT);
		retTable.setColValInt("delete_me", 1);
		retTable.sortCol(Columns.PERSONNEL_VERSION.getColumn());
		retTable.setInt("delete_me", 1, 0);
		retTable.deleteWhereValue("delete_me", 1);
		
		return retTable;
	}


	/**
	 * @param returnt
	 *            - Table to return to report
	 * @throws OException
	 *             - Error, cannot format table
	 */
	private void formatReturnTable(Table returnt) throws OException
	{

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
