package com.jm.reportbuilder.audit;


import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_COUNTRY;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_EXPLANATION;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_ID_NUMBER;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_LAST_CHANGED;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_LICENCE_DIFFERENCE;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_LT_APM;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_LT_CONNEX;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_LT_FULL_ACCESS;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_LT_FULL_COMMODITY;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_LT_READ_ONLY;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_LT_SERVER;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_LT_SUBSIDIARY;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_MODIFIED_DATE;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_PERSONNEL_FIRSTNAME;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_PERSONNEL_ID;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_PERSONNEL_LASTNAME;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_PERSONNEL_LASTVERSION;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_PERSONNEL_SHORTNAME;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_PERSONNEL_STATUS;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_PERSONNEL_TYPE;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_PERSONNEL_VERSION;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_PER_COUNTRY;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_PER_FIRSTNAME;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_PER_LASTNAME;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_PER_MODIFIED_DATE;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_PER_MOD_USER;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_PER_NAME;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_PER_PERSONNEL_CURRENT_VERSION;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_PER_PERSONNEL_TYPE;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_PER_PESONNEL_STATUS;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SEC_DIFFERENCE;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_ADMINISTRATOR;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_BO;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_BO_SNR;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_BO_US;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_CREDIT;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_CREDIT_SNR;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_DEPLOYMENT;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_EOD;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_FO_HK;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_FO_SNR;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_FO_UK;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_FO_US;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_IT_SUPPORT;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_MAN_APPROVAL;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_MARKET_PRICES;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_MARKET_USER;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_MIGRATION;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_PHYS_TRANSFER;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_RISK;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_RISK_SNR;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_ROLE_BASED_TESTING;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_RO_INVENTORY;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_SAFE_WAREHOUSE;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_SECURITY_ADMIN;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_SERVER_USER;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_STOCK_TAKE;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_SUPPORT_ELEVATED;
import static com.jm.reportbuilder.audit.SupportPersonnelAnalysisConstants.COL_SG_TRADE_ONLY_VIEW;
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
import com.olf.jm.logging.Logging;

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
		
		LICENCE_DIFFERENCE(COL_LICENCE_DIFFERENCE, "Licence Change", COL_STRING, "Licence Changes")		{		},
		SEC_DIFFERENCE(COL_SEC_DIFFERENCE, "Sec Group Change", COL_STRING, "Sec Group Change")		{		},
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

			Logging.info("Start  " + getClass().getSimpleName());

			Table argt = context.getArgumentsTable();
			Table returnt = context.getReturnTable();

			int modeFlag = argt.getInt("ModeFlag", 1);
			Logging.debug(getClass().getSimpleName() + " - Started Data Load Script for UserSecAuit Reports - mode: " + modeFlag);

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


				Logging.debug("Completed Data Load Script Metadata:");

				return;
			} else {

				formatReturnTable(returnt);
				if (modeFlag == 0) {
					return;
				}
 
				Table tblTemp = argt.getTable("PluginParameters", 1);
				report_date = Util.getBusinessDate();// OCalendar.parseString(tblTemp.getString("parameter_value", tblTemp.unsortedFindString("parameter_name", "GEN_TIME", SEARCH_CASE_ENUM.CASE_INSENSITIVE)));

				Logging.debug("Running Data Load Script For Date: " + OCalendar.formatDateInt(report_date));


				Logging.info("Enrich data" );
				enrichData(returnt);

				Logging.info("Data Num Rows: " + returnt.getNumRows());



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
	private void enrichData(Table returnt) throws OException {

		Table tblAllPersonnelAuditData = Table.tableNew();
		Table tblAllPersonnelAnalysisData = Table.tableNew();
		
		Table tblPersonnelData = Table.tableNew();
		//ODateTime dateValue = ODateTime.dtNew();

		int totalRows = 0;
		String sqlCommand;

		Logging.debug("Attempt to recover Personnel information.");

		try {
			tblAllPersonnelAuditData = getAllPersnnelAuditData();
			tblAllPersonnelAnalysisData = getAllPersnnelAnalysisData();
			ODateTime dateValue = getDateValue();
			sqlCommand = "SELECT uspa.*\n" +  
						" FROM " + SupportPersonnelAuditConstants.USER_SUPPORT_PERSONNEL_AUDIT + " uspa\n" +
						" WHERE uspa." + SupportPersonnelAuditConstants.COL_LATEST_VERSION + " = -1" ; 	// + dateValue.formatForDbAccess() 
			DBaseTable.execISql(tblPersonnelData, sqlCommand);

			
			tblPersonnelData.setColName(COL_ID_NUMBER, PERSONNEL_ID.getColumn());
			tblPersonnelData.setColName(COL_PER_NAME, PERSONNEL_SHORTNAME.getColumn());
			tblPersonnelData.setColName(COL_PER_FIRSTNAME, PERSONNEL_FIRSTNAME.getColumn());
			tblPersonnelData.setColName(COL_PER_LASTNAME, PERSONNEL_LASTNAME.getColumn());
			tblPersonnelData.setColName(COL_PER_PESONNEL_STATUS, PERSONNEL_STATUS.getColumn());
			tblPersonnelData.setColName(COL_PER_PERSONNEL_TYPE, PERSONNEL_TYPE.getColumn());
			tblPersonnelData.setColName(COL_PER_COUNTRY, PERSONNEL_COUNTRY.getColumn() );
			tblPersonnelData.setColName(COL_PER_MODIFIED_DATE, MODIFIED_DATE.getColumn());
			tblPersonnelData.setColName(COL_PER_PERSONNEL_CURRENT_VERSION, PERSONNEL_VERSION.getColumn());
			tblPersonnelData.setColName(COL_PER_MOD_USER, LAST_CHANGED.getColumn());
			
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
				
//				Table previousVersionTbl = getPreviousVersionNum(tblAllPersonnelAuditData,personnelID, dateValue);

				int getThisRowLoop = getThisRow(tblPersonnelData, personnelID, currentVer);
//				int previousVer = 0;
//				if (previousVersionTbl.getNumRows()>0){
//					previousVer = previousVersionTbl.getInt(COL_PER_PERSONNEL_CURRENT_VERSION, 1);
//				} 
				
				boolean currentVersionExist = doesCurrentVersionExist(tblAllPersonnelAnalysisData,personnelID,currentVer);
				if (currentVersionExist){
					tblPersonnelData.setInt("delete_me",getThisRowLoop,1);
				} else {
					Table previousVersionTbl = getPreviousVersionNum(tblAllPersonnelAnalysisData, tblAllPersonnelAuditData,personnelID,currentVer, dateValue);

					int previousVer = 0;
					if (previousVersionTbl.getNumRows()>0){
						previousVer = previousVersionTbl.getInt(COL_PER_PERSONNEL_CURRENT_VERSION, 1);
					} 

					
					String licenceDifference = getLicenceDiff(currentVersionTbl, previousVersionTbl, previousVer);
					String securityDifference = getSecurityDiff(currentVersionTbl, previousVersionTbl, previousVer);
					if (licenceDifference.length()>0 || securityDifference.length()>0){
						tblPersonnelData.setString(Columns.LICENCE_DIFFERENCE.getColumn(), getThisRowLoop, licenceDifference);
						tblPersonnelData.setString(Columns.SEC_DIFFERENCE.getColumn(), getThisRowLoop, securityDifference);
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
			Logging.debug("Results processing finished. Total Number of results recovered: " + totalRows + " processed: " + tblPersonnelData.getNumRows());

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
		 
		if (!currentVersionTbl.getString(COL_SG_SECURITY_ADMIN , 1).equals(previousVersionTbl.getString(COL_SG_SECURITY_ADMIN , 1)) ){
			if (currentVersionTbl.getString(COL_SG_SECURITY_ADMIN , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added Security Admin ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost Security Admin ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_SG_IT_SUPPORT , 1).equals(previousVersionTbl.getString(COL_SG_IT_SUPPORT , 1)) ){
			if (currentVersionTbl.getString(COL_SG_IT_SUPPORT , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added IT Support ";
			} else {
				if (previousVer>0){	
					securityRoleDiff = securityRoleDiff + "Lost IT Support ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_SG_FO_UK , 1).equals(previousVersionTbl.getString(COL_SG_FO_UK , 1)) ){
			if (currentVersionTbl.getString(COL_SG_FO_UK , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added FO UK ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost FO UK ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_SG_FO_HK , 1).equals(previousVersionTbl.getString(COL_SG_FO_HK , 1)) ){
			if (currentVersionTbl.getString(COL_SG_FO_HK , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added FO HK ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost FO HK ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_SG_FO_US , 1).equals(previousVersionTbl.getString(COL_SG_FO_US , 1)) ){
			if (currentVersionTbl.getString(COL_SG_FO_US , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added FO US ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost FO US ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_SG_FO_SNR , 1).equals(previousVersionTbl.getString(COL_SG_FO_SNR , 1)) ){
			if (currentVersionTbl.getString(COL_SG_FO_SNR , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added FO Snr ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost FO Snr ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_SG_ADMINISTRATOR , 1).equals(previousVersionTbl.getString(COL_SG_ADMINISTRATOR , 1)) ){
			if (currentVersionTbl.getString(COL_SG_ADMINISTRATOR , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added Administrator ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost Administrator ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_SG_SERVER_USER , 1).equals(previousVersionTbl.getString(COL_SG_SERVER_USER , 1)) ){
			if (currentVersionTbl.getString(COL_SG_SERVER_USER , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added Server User ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost Server User ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_SG_MIGRATION , 1).equals(previousVersionTbl.getString(COL_SG_MIGRATION , 1)) ){
			if (currentVersionTbl.getString(COL_SG_MIGRATION , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added Migration ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost Migration ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_SG_MARKET_PRICES , 1).equals(previousVersionTbl.getString(COL_SG_MARKET_PRICES , 1)) ){
			if (currentVersionTbl.getString(COL_SG_MARKET_PRICES , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added Market Prices ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost Market Prices ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_SG_MAN_APPROVAL , 1).equals(previousVersionTbl.getString(COL_SG_MAN_APPROVAL , 1)) ){
			if (currentVersionTbl.getString(COL_SG_MAN_APPROVAL , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added Man Approval ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost Man Approval ";
				}
			}
		} 
		if (!currentVersionTbl.getString(COL_SG_SAFE_WAREHOUSE , 1).equals(previousVersionTbl.getString(COL_SG_SAFE_WAREHOUSE , 1)) ){
			if (currentVersionTbl.getString(COL_SG_SAFE_WAREHOUSE , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added Safe Warehouse ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost Safe Warehouse ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_SG_EOD , 1).equals(previousVersionTbl.getString(COL_SG_EOD , 1)) ){
			if (currentVersionTbl.getString(COL_SG_EOD , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added EOD ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost EOD ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_SG_STOCK_TAKE , 1).equals(previousVersionTbl.getString(COL_SG_STOCK_TAKE , 1)) ){
			if (currentVersionTbl.getString(COL_SG_STOCK_TAKE , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added Stock Take ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost Stock Take ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_SG_RO_INVENTORY , 1).equals(previousVersionTbl.getString(COL_SG_RO_INVENTORY , 1)) ){
			if (currentVersionTbl.getString(COL_SG_RO_INVENTORY , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added RO Inventory ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost RO Inventory ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_SG_TRADE_ONLY_VIEW , 1).equals(previousVersionTbl.getString(COL_SG_TRADE_ONLY_VIEW , 1)) ){
			if (currentVersionTbl.getString(COL_SG_TRADE_ONLY_VIEW , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added Trade Only View ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost Trade Only View ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_SG_MARKET_USER , 1).equals(previousVersionTbl.getString(COL_SG_MARKET_USER , 1)) ){
			if (currentVersionTbl.getString(COL_SG_MARKET_USER , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added Market User ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost Market User ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_SG_CREDIT , 1).equals(previousVersionTbl.getString(COL_SG_CREDIT , 1)) ){
			if (currentVersionTbl.getString(COL_SG_CREDIT , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added Credit ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost Credit ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_SG_CREDIT_SNR , 1).equals(previousVersionTbl.getString(COL_SG_CREDIT_SNR , 1)) ){
			if (currentVersionTbl.getString(COL_SG_CREDIT_SNR , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added Credit Snr ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost Credit Snr ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_SG_RISK , 1).equals(previousVersionTbl.getString(COL_SG_RISK , 1)) ){
			if (currentVersionTbl.getString(COL_SG_RISK , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added Risk ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost Risk ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_SG_RISK_SNR , 1).equals(previousVersionTbl.getString(COL_SG_RISK_SNR , 1)) ){
			if (currentVersionTbl.getString(COL_SG_RISK_SNR , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added Risk Snr ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost Risk Snr ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_SG_BO , 1).equals(previousVersionTbl.getString(COL_SG_BO , 1)) ){
			if (currentVersionTbl.getString(COL_SG_BO , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added Back Office ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost Back Office ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_SG_BO_US , 1).equals(previousVersionTbl.getString(COL_SG_BO_US , 1)) ){
			if (currentVersionTbl.getString(COL_SG_BO_US , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added Back Office US ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost Back Office US ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_SG_BO_SNR , 1).equals(previousVersionTbl.getString(COL_SG_BO_SNR , 1)) ){
			if (currentVersionTbl.getString(COL_SG_BO_SNR , 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added Back Office Snr ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost Back Office Snr ";
				}
			}
		}

		
		if (!currentVersionTbl.getString(COL_SG_SUPPORT_ELEVATED , 1).equals(previousVersionTbl.getString(COL_SG_SUPPORT_ELEVATED, 1)) ){
			if (currentVersionTbl.getString(COL_SG_SUPPORT_ELEVATED, 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added Support Elavated ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost Support Elavated ";
				}
			}
		}

		if (!currentVersionTbl.getString(COL_SG_ROLE_BASED_TESTING , 1).equals(previousVersionTbl.getString(COL_SG_ROLE_BASED_TESTING, 1)) ){
			if (currentVersionTbl.getString(COL_SG_ROLE_BASED_TESTING, 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added Role Based Testing ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost Role Based Testing ";
				}
			}
		}

		if (!currentVersionTbl.getString(COL_SG_DEPLOYMENT , 1).equals(previousVersionTbl.getString(COL_SG_DEPLOYMENT, 1)) ){
			if (currentVersionTbl.getString(COL_SG_DEPLOYMENT, 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added Deployment ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost Deployment ";
				}
			}
		}

		if (!currentVersionTbl.getString(COL_SG_PHYS_TRANSFER , 1).equals(previousVersionTbl.getString(COL_SG_PHYS_TRANSFER, 1)) ){
			if (currentVersionTbl.getString(COL_SG_PHYS_TRANSFER, 1).equals("Yes")){
				securityRoleDiff = securityRoleDiff + "Added BO Phys Transfer ";
			} else {
				if (previousVer>0){
					securityRoleDiff = securityRoleDiff + "Lost BO Phys Transfer ";
				}
			}
		}

		return securityRoleDiff;
	}


	private String getLicenceDiff(Table currentVersionTbl, Table previousVersionTbl, int previousVer) throws OException {
		String licenceDiff = "";
		
		if (!currentVersionTbl.getString(COL_LT_FULL_COMMODITY , 1).equals(previousVersionTbl.getString(COL_LT_FULL_COMMODITY , 1)) ){
			if (currentVersionTbl.getString(COL_LT_FULL_COMMODITY , 1).equals("Yes")){
				licenceDiff = licenceDiff + "Added Full And Commodity ";
			} else {
				if (previousVer>0){
					licenceDiff = licenceDiff + "Lost Full And Commodity ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_LT_FULL_ACCESS , 1).equals(previousVersionTbl.getString(COL_LT_FULL_ACCESS , 1)) ){
			if (currentVersionTbl.getString(COL_LT_FULL_ACCESS , 1).equals("Yes")){
				licenceDiff = licenceDiff + "Added Full Access ";
			} else {
				if (previousVer>0){
					licenceDiff = licenceDiff + "Lost Full Access ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_LT_READ_ONLY , 1).equals(previousVersionTbl.getString(COL_LT_READ_ONLY , 1)) ){
			if (currentVersionTbl.getString(COL_LT_READ_ONLY , 1).equals("Yes")){
				licenceDiff = licenceDiff + "Added Read Only ";
			} else {
				if (previousVer>0){
					licenceDiff = licenceDiff + "Lost Read Only ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_LT_APM , 1).equals(previousVersionTbl.getString(COL_LT_APM , 1)) ){
			if (currentVersionTbl.getString(COL_LT_APM , 1).equals("Yes")){
				licenceDiff = licenceDiff + "Added APM ";
			} else {
				if (previousVer>0){
					licenceDiff = licenceDiff + "Lost APM ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_LT_SUBSIDIARY , 1).equals(previousVersionTbl.getString(COL_LT_SUBSIDIARY , 1)) ){
			if (currentVersionTbl.getString(COL_LT_SUBSIDIARY , 1).equals("Yes")){
				licenceDiff = licenceDiff + "Added Subsidiary ";
			} else {
				if (previousVer>0){
					licenceDiff = licenceDiff + "Lost Subsidiary ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_LT_CONNEX , 1).equals(previousVersionTbl.getString(COL_LT_CONNEX , 1)) ){
			if (currentVersionTbl.getString(COL_LT_CONNEX , 1).equals("Yes")){
				licenceDiff = licenceDiff + "Added Connex ";
			} else {
				if (previousVer>0){
					licenceDiff = licenceDiff + "Lost Connex ";
				}
			}
		}
		if (!currentVersionTbl.getString(COL_LT_SERVER , 1).equals(previousVersionTbl.getString(COL_LT_SERVER , 1)) ){
			if (currentVersionTbl.getString(COL_LT_SERVER , 1).equals("Yes")){
				licenceDiff = licenceDiff + "Added Server ";
			} else {
				if (previousVer>0){
					licenceDiff = licenceDiff + "Lost Server ";
				}
			}
		}

		

		return licenceDiff;
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
//		retTable.addCol("delete_me" , COL_INT);
//		retTable.setColValInt("delete_me", 1);
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
		
//		for (int iLoop = 1; iLoop <=retTableCount; iLoop++){
//			ODateTime thisDataTime = retTable.getDateTime(SupportPersonnelAnalysisConstants.COL_PER_MODIFIED_DATE, iLoop);
//			if (thisDataTime.getDate()<=dateValue.getDate() && thisDataTime.getTime()<dateValue.getTime()){
//				retTable.setInt("delete_me", iLoop, 0);
//				if (iLoop>1){
//					retTable.setInt("delete_me", iLoop-1, 1);					
//				}
//			}
//		}
		
//		retTable.deleteWhereValue("delete_me", 1);
		
		
		return retTable;
	}

	private Table getPreviousVersionNum(Table tblAllPersonnelData, int personnelID, ODateTime dateValue) throws OException {
		
		Table retTable = Table.tableNew();
		retTable.select (tblAllPersonnelData,"*", Columns.PERSONNEL_ID.getColumn()+ " EQ " + personnelID);
		retTable.addCol("delete_me" , COL_INT);
		retTable.setColValInt("delete_me", 1);
		retTable.sortCol(COL_PER_PERSONNEL_CURRENT_VERSION);
		
		int retTableCount = retTable.getNumRows();
		for (int iLoop = 1; iLoop <=retTableCount; iLoop++){
			ODateTime thisDataTime = retTable.getDateTime(SupportPersonnelAnalysisConstants.COL_PER_MODIFIED_DATE, iLoop);
			if (thisDataTime.getDate()<=dateValue.getDate() && thisDataTime.getTime()<dateValue.getTime()){
				retTable.setInt("delete_me", iLoop, 0);
				if (iLoop>1){
					retTable.setInt("delete_me", iLoop-1, 1);					
				}
			}
		}
		
		retTable.deleteWhereValue("delete_me", 1);
		
		
		return retTable;
		
	}


	private ODateTime getDateValue() throws OException {
		ODateTime dateValue = ODateTime.dtNew();
//		int dateValue = 0;
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
