package com.jm.reportbuilder.audit;




import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_CHANGE_TYPE;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_CHANGE_TYPE_ID;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_CHANGE_VERSION;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_EXPLANATION;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_MODIFIED_DATE;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_OBJECT_ID;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_OBJECT_NAME;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_OBJECT_REFERENCE;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_OBJECT_STATUS;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_OBJECT_TYPE;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_OBJECT_TYPE_ID;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_PERSONNEL_FIRSTNAME;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_PERSONNEL_ID;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_PERSONNEL_LASTNAME;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_PERSONNEL_SHORTNAME;
import static com.jm.reportbuilder.audit.SupportChangeAuditConstants.COL_PROJECT_NAME;
import static com.jm.reportbuilder.audit.SupportChangeAuditDataLoad.ChangeType.ARTIFACT_CHANGE_TYPE;
import static com.jm.reportbuilder.audit.SupportChangeAuditDataLoad.ChangeType.DEPLOYMENT_CHANGE_TYPE;
import static com.jm.reportbuilder.audit.SupportChangeAuditDataLoad.ChangeType.FINANCIAL_CHANGE_TYPE;
import static com.jm.reportbuilder.audit.SupportChangeAuditDataLoad.ChangeType.STANDING_DATA_CHANGE_TYPE;
import static com.jm.reportbuilder.audit.SupportChangeAuditDataLoad.ChangeType.USER_TABLE_CHANGE_TYPE;
import static com.jm.reportbuilder.audit.SupportChangeAuditDataLoad.Columns.PERSONNEL_ID;
import static com.olf.openjvs.enums.COL_TYPE_ENUM.COL_DATE_TIME;
import static com.olf.openjvs.enums.COL_TYPE_ENUM.COL_INT;
import static com.olf.openjvs.enums.COL_TYPE_ENUM.COL_INT64;
import static com.olf.openjvs.enums.COL_TYPE_ENUM.COL_STRING;

import java.text.ParseException;

import com.jm.reportbuilder.utils.ReportBuilderUtils;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
//import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_DATALOAD)
public class SupportChangeAuditDataLoad implements IScript {

	

	private static final int NODE_DIR_TOP_LEVEL_DMS = 13;
	private static final int NODE_DIR_TOP_LEVEL_CODE = 4;
	
	private static final String FINANCIAL_CHANGE = "Financial Change";
	private static final String DEPLOYMENT_CHANGE = "Deployment Change";
	private static final String ARTIFACT_CHANGE = "Artifact Change";
	private static final String STANDING_DATA_CHANGE = "Standing Data Change";
	private static final String USER_TABLE_CHANGE = "User Table Change";
	
	private static final int FINANCIAL_CHANGE_ID = 1;
	private static final int DEPLOYMENT_CHANGE_ID = 2;
	private static final int ARTIFACT_CHANGE_ID = 3;
	private static final int STANDING_DATA_CHANGE_ID = 4;
	private static final int USER_TABLE_CHANGE_ID = 5;
	
	private static final String OBJECT_CHANGE_DEAL_CHANGE = "Deal Change";
	private static final String OBJECT_CHANGE_HISTORICAL_CHANGE = "Historical Price";
	private static final String OBJECT_CHANGE_HISTORICAL_FX_CHANGE = "Historical FX Price";
	private static final String OBJECT_CHANGE_MARKET_PRICE = "Market Price";
	private static final String OBJECT_CHANGE_BO_DOCUMENT = "BO Document";
	
	private static final String OBJECT_CHANGE_QUERY_CHANGE = "Query Change";
	private static final String OBJECT_CHANGE_CODE_CHANGE = "Code Change";
	private static final String OBJECT_CHANGE_AUTOMATCH_CHANGE = "AutoMatch/Connex Change";
	private static final String OBJECT_CHANGE_OPS_SERVICE_CHANGE = "Ops Service Change";
	
	private static final String OBJECT_CHANGE_DMS_CHANGE = "DMS Change";
	private static final String OBJECT_CHANGE_APM_CHANGE = "APM Change";
	private static final String OBJECT_CHANGE_SQL_CHANGE = "Datasource Change";
	private static final String OBJECT_CHANGE_TPM_CHANGE = "TPM Change";

	private static final String OBJECT_CHANGE_TASK_CHANGE = "Task Change";
	private static final String OBJECT_CHANGE_REPORT_CHANGE = "Report Change";
	private static final String OBJECT_CHANGE_INDEX_CHANGE = "Index Change";
	

	private static final String OBJECT_CHANGE_PARTYDATA_CHANGE = "Party Change";
	private static final String OBJECT_CHANGE_PERSONNEL_CHANGE = "Personnel Change";
	private static final String OBJECT_CHANGE_ACCOUNTS_CHANGE = "Accounts Change";
	private static final String OBJECT_CHANGE_SI_CHANGE = "SI Change";
	private static final String OBJECT_CHANGE_PORTFOLIO_CHANGE = "Portfolio Change";

	private static final String OBJECT_CHANGE_EXTENSIONSEC_CHANGE = "Extension Security";
	private static final String OBJECT_CHANGE_ARCHIVE_CHANGE = "Archive Config";
	private static final String OBJECT_CHANGE_TABLEAU_CHANGE = "Tableau Table";
	private static final String OBJECT_CHANGE_TEMPLATE_CHANGE= "Template Change";
	private static final String OBJECT_CHANGE_TRAN_INFO_CHANGE= "Tran Info Change";
	
	private static final String OBJECT_CHANGE_EVENT_INFO_CHANGE= "Event Info Change";
	private static final String OBJECT_CHANGE_PARCEL_INFO_CHANGE= "Parcel Info Change";
	private static final String OBJECT_CHANGE_PARAM_INFO_CHANGE= "Parameter Info Change";
	
	private static final String OBJECT_CHANGE_SCREEN_CONFIG_CHANGE= "Screen Config";
	
	
	private static final String NODE_TYPE_LIST = "17, 19";		// 17- auto match, 19 - Connex


	protected enum Columns {
		//SupportChangeAuditConstants
		CHANGE_TYPE(COL_CHANGE_TYPE, "Change Type", COL_STRING, "Change Type - Financial or Deployment")		{		},
		CHANGE_TYPE_ID(COL_CHANGE_TYPE_ID, "Change Type ID", COL_INT, "Internal Change Type ID value")		{		},
		OBJECT_TYPE(COL_OBJECT_TYPE, "Object Type", COL_STRING, "Deal/Query/Code")		{		},
		OBJECT_TYPE_ID(COL_OBJECT_TYPE_ID, "Object Type ID", COL_INT, "Internal Object Type ID value")		{		},

		PERSONNEL_ID(COL_PERSONNEL_ID, "Personnel ID", COL_INT, "Personnel ID"){	},
		PERSONNEL_SHORTNAME(COL_PERSONNEL_SHORTNAME, "Short Name", COL_STRING, "Personnel Short Name"){		},
		PERSONNEL_FIRSTNAME(COL_PERSONNEL_FIRSTNAME, "First Name", COL_STRING, "Personnel First Name"){		},
		PERSONNEL_LASTNAME(COL_PERSONNEL_LASTNAME, "Last Name", COL_STRING, "Personnel First Name")		{		},

		
		OBJECT_NAME(COL_OBJECT_NAME, "Name", COL_STRING, "Object Name/Reference")		{		},
		OBJECT_REFERENCE(COL_OBJECT_REFERENCE, "Reference", COL_STRING, "Category")		{		},
		OBJECT_STATUS(COL_OBJECT_STATUS, "Status", COL_STRING, "Object Status")		{		},
		CHANGE_VERSION(COL_CHANGE_VERSION, "Personnel Version", COL_INT, "Current Personnel Version")		{		},
		MODIFIED_DATE(COL_MODIFIED_DATE, "Modified Date", COL_DATE_TIME, "The Last Time the object was updated")		{		},
		PROJECT_NAME(COL_PROJECT_NAME, "Project Name", COL_STRING, "Project Name")		{		},
		OBJECT_ID(COL_OBJECT_ID, "Object ID", COL_INT64, "Ojbect ID")		{		},
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
			
			COL_TYPE_ENUM thisType = getType();
			String nameType=ReportBuilderUtils.getNameType(thisType);
			
			return nameType;
		}

		
	}
	

	protected enum ChangeType {
		
		FINANCIAL_CHANGE_TYPE(FINANCIAL_CHANGE,FINANCIAL_CHANGE_ID)		{		},
		DEPLOYMENT_CHANGE_TYPE(DEPLOYMENT_CHANGE, DEPLOYMENT_CHANGE_ID)		{		},
		ARTIFACT_CHANGE_TYPE(ARTIFACT_CHANGE, ARTIFACT_CHANGE_ID)		{		},
		STANDING_DATA_CHANGE_TYPE(STANDING_DATA_CHANGE, STANDING_DATA_CHANGE_ID)		{		},
		USER_TABLE_CHANGE_TYPE(USER_TABLE_CHANGE, USER_TABLE_CHANGE_ID)		{		};
		
		
		private String _changeTypeName;
		private int _changeTypeID;

		private ChangeType(String changeTypeName, int changeTypeID) {
			_changeTypeName = changeTypeName;
			_changeTypeID = changeTypeID;
		}

		public String getChangeTypeName() {
			return _changeTypeName;
		}
		public int getChangeTypeID() {
			return _changeTypeID;
		}


	}

	
	public enum ReportType {
		//SupportChangeAuditConstants
		DEAL_CHANGE(FINANCIAL_CHANGE_TYPE, OBJECT_CHANGE_DEAL_CHANGE, 1)		{		},
		HISTORICAL_CHANGE(FINANCIAL_CHANGE_TYPE,  OBJECT_CHANGE_HISTORICAL_CHANGE, 2)		{		},
		HISTORICAL_FX_CHANGE(FINANCIAL_CHANGE_TYPE,  OBJECT_CHANGE_HISTORICAL_FX_CHANGE, 3)		{		},
		MARKET_PRICE_CHANGE(FINANCIAL_CHANGE_TYPE,  OBJECT_CHANGE_MARKET_PRICE, 4)		{		},
		BO_DOCUMENT_CHANGE(FINANCIAL_CHANGE_TYPE,  OBJECT_CHANGE_BO_DOCUMENT, 5)		{		},

		QUERY_CHANGE(DEPLOYMENT_CHANGE_TYPE,  OBJECT_CHANGE_QUERY_CHANGE, 8)		{		},
		CODE_CHANGE(ARTIFACT_CHANGE_TYPE,  OBJECT_CHANGE_CODE_CHANGE, 9)		{		},
		AUTOMATCH_CHANGE(DEPLOYMENT_CHANGE_TYPE,  OBJECT_CHANGE_AUTOMATCH_CHANGE, 10)		{		},
		OPS_SERVICE_CHANGE(DEPLOYMENT_CHANGE_TYPE,  OBJECT_CHANGE_OPS_SERVICE_CHANGE, 11)		{		},

		
		DMS_CHANGE(ARTIFACT_CHANGE_TYPE,  OBJECT_CHANGE_DMS_CHANGE, 12)		{		},
		APM_CHANGE(ARTIFACT_CHANGE_TYPE,  OBJECT_CHANGE_APM_CHANGE, 13)		{		},
		SQL_CHANGE(ARTIFACT_CHANGE_TYPE,  OBJECT_CHANGE_SQL_CHANGE, 14)		{		},
		
		TPM_CHANGE(DEPLOYMENT_CHANGE_TYPE,  OBJECT_CHANGE_TPM_CHANGE, 15)		{		},
		TASK_CHANGE(DEPLOYMENT_CHANGE_TYPE,  OBJECT_CHANGE_TASK_CHANGE, 16)		{		},
		REPORT_CHANGE(DEPLOYMENT_CHANGE_TYPE,  OBJECT_CHANGE_REPORT_CHANGE, 17)		{		},
		INDEX_CHANGE(DEPLOYMENT_CHANGE_TYPE,  OBJECT_CHANGE_INDEX_CHANGE, 18)		{		},

		PARTY_CHANGE(STANDING_DATA_CHANGE_TYPE,  OBJECT_CHANGE_PARTYDATA_CHANGE, 20)		{		},
		PERSONNEL_CHANGE(STANDING_DATA_CHANGE_TYPE,  OBJECT_CHANGE_PERSONNEL_CHANGE, 21)		{		},
		ACCOUNTS_CHANGE(STANDING_DATA_CHANGE_TYPE,  OBJECT_CHANGE_ACCOUNTS_CHANGE, 22)		{		},
		SI_CHANGE(STANDING_DATA_CHANGE_TYPE,  OBJECT_CHANGE_SI_CHANGE, 23)		{		},
		PORTFOLIO_CHANGE(STANDING_DATA_CHANGE_TYPE,  OBJECT_CHANGE_PORTFOLIO_CHANGE, 24)		{		},

		EXTENSIONSEC_CHANGE(USER_TABLE_CHANGE_TYPE,  OBJECT_CHANGE_EXTENSIONSEC_CHANGE, 25)		{		},  // Not Implemented
 		ARCHIVE_CHANGE(USER_TABLE_CHANGE_TYPE,  OBJECT_CHANGE_ARCHIVE_CHANGE, 26)		{		}, // Not Implemented
		TABLEAU_CHANGE(USER_TABLE_CHANGE_TYPE,  OBJECT_CHANGE_TABLEAU_CHANGE, 27)		{		},// Not Implemented
		TEMPLATE_CHANGE(DEPLOYMENT_CHANGE_TYPE, OBJECT_CHANGE_TEMPLATE_CHANGE,28)		{		},
		TRAN_INFO_CHANGE(FINANCIAL_CHANGE_TYPE, OBJECT_CHANGE_TRAN_INFO_CHANGE, 29)		{		},
		
		EVENT_INFO_CHANGE(FINANCIAL_CHANGE_TYPE, OBJECT_CHANGE_EVENT_INFO_CHANGE, 30)		{		}, 
		PARCEL_INFO_CHANGE(FINANCIAL_CHANGE_TYPE, OBJECT_CHANGE_PARCEL_INFO_CHANGE, 31)		{		},// Not Implemented
		PARAM_INFO_CHANGE(FINANCIAL_CHANGE_TYPE, OBJECT_CHANGE_PARAM_INFO_CHANGE, 32)		{		},
		
		SCREEN_CONFIG_CHANGE(DEPLOYMENT_CHANGE_TYPE,  OBJECT_CHANGE_SCREEN_CONFIG_CHANGE, 33)		{		};
		
		
		

		;

		private String _changeTypeName;
		private String _objectTypeName;
		private int _changeTypeID;
		private int _objectTypeID;

		private ReportType(ChangeType changeTypeNameType, String objectTypeName,  int objectTypeID) {
			_changeTypeName = changeTypeNameType.getChangeTypeName();
			_objectTypeName = objectTypeName;
			_changeTypeID = changeTypeNameType.getChangeTypeID();
			_objectTypeID = objectTypeID;
		}

		public String getChangeTypeName() {
			return _changeTypeName;
		}
		public String getObjectTypeName() {
			return _objectTypeName;
		}
		public int getChangeTypeID() {
			return _changeTypeID;
		}
		public int getObjectTypeID() {
			return _objectTypeID;
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
			ConstRepository constRep = new ConstRepository(SupportChangeAuditConstants.REPO_CONTEXT, SupportChangeAuditConstants.REPO_SUB_CONTEXT);
			ReportBuilderUtils.initPluginLog(constRep , SupportChangeAuditConstants.defaultLogFile); //Plug in Log init

			
			// PluginLog.init("INFO");

			PluginLog.info("Start  " + getClass().getSimpleName());

			Table argt = context.getArgumentsTable();
			Table returnt = context.getReturnTable();

			int modeFlag = argt.getInt("ModeFlag", 1);
			PluginLog.debug(getClass().getSimpleName() + " - Started Data Load Script for UserSecAudit Reports - mode: " + modeFlag);

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
				int queryID = argt.getInt("QueryResultID", 1);
				String sQueryTable = argt.getString("QueryResultTable", 1);


				PluginLog.debug("Running Data Load Script For Date: " + OCalendar.formatDateInt(report_date));

				if (queryID > 0) {

					PluginLog.info("Enrich data query ID: " + queryID + " User Table: " + sQueryTable);
					enrichData(returnt, queryID, sQueryTable);

					PluginLog.info("Data Num Rows: " + returnt.getNumRows());

				}



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
	 * Method enrich Data Recovers personnel from Query results and extract in final return table information for 	 * 
	 * @param queryID
	 * @throws OException
	 * @throws ParseException
	 */
	private void enrichData(Table returnt, int queryID, String sQueryTable) throws OException {

		
		Table tblAllChangedData = Table.tableNew();
		Table tblFinacialChangedData = Table.tableNew();
		Table tblDeploymentChangedData = Table.tableNew();
		Table tblArtifactChangedData = Table.tableNew();
		Table tblStandingDataChangedData = Table.tableNew();
		Table tblUserTableChangedData = Table.tableNew();
		
		// add the extra columns

		int totalRows = 0;
		String sqlCommand;

		PluginLog.debug("Attempt to recover Personnel information.");

		try {

			// Start of Financial Changes
			String dateValue = ReportBuilderUtils.getDateValue(SupportChangeAuditConstants.REPO_CONTEXT,SupportChangeAuditConstants.REPO_SUB_CONTEXT);
			ReportType thisReportType=ReportType.DEAL_CHANGE;
			
			String sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
								 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
		 						 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +  
								 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
								 "   ab.deal_tracking_num " + Columns.OBJECT_ID.getColumn() + ",ab.reference " + Columns.OBJECT_NAME.getColumn() + ",\n" +
								 "   i.name + '-' + ist.name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
								 "   ts.name " + Columns.OBJECT_STATUS.getColumn() + ",ab.version_number " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
								 "   ab.last_update " + Columns.MODIFIED_DATE.getColumn() + ",tt.name " + Columns.PROJECT_NAME.getColumn() + "\n" +
								 " FROM ab_tran ab \n" +
								 "   JOIN personnel p ON (p.id_number=ab.personnel_id)\n" +
								 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
								 "   JOIN trans_status ts ON (ts.trans_status_id=ab.tran_status)\n" +
								 "   JOIN trans_type tt ON (tt.id_number=ab.tran_type)\n" +
								 "   JOIN instruments i ON (i.id_number=ab.ins_type)\n" +
								 "   LEFT JOIN ins_sub_type ist ON (ist.id_number=ab.ins_sub_type)\n" +
								 " WHERE qr.unique_id=" + queryID + "\n" +
								 "   AND ab.last_update > '" + dateValue +"'\n" +
								 "   AND ab.tran_status != 15";
			sqlCommand = sqlCommand1;
			
			thisReportType=ReportType.TEMPLATE_CHANGE;
			
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
								 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
		 						 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +  
								 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
								 "   ab.deal_tracking_num " + Columns.OBJECT_ID.getColumn() + ",ab.reference " + Columns.OBJECT_NAME.getColumn() + ",\n" +
								 "   i.name + '-' + ist.name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
								 "   ts.name " + Columns.OBJECT_STATUS.getColumn() + ",ab.version_number " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
								 "   ab.last_update " + Columns.MODIFIED_DATE.getColumn() + ",tt.name " + Columns.PROJECT_NAME.getColumn() + "\n" +
								 " FROM ab_tran ab \n" +
								 "   JOIN personnel p ON (p.id_number=ab.personnel_id)\n" +
								 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
								 "   JOIN trans_status ts ON (ts.trans_status_id=ab.tran_status)\n" +
								 "   JOIN trans_type tt ON (tt.id_number=ab.tran_type)\n" +
								 "   JOIN instruments i ON (i.id_number=ab.ins_type)\n" +
								 "   LEFT JOIN ins_sub_type ist ON (ist.id_number=ab.ins_sub_type)\n" +
								 " WHERE qr.unique_id=" + queryID + "\n" +
								 "   AND ab.last_update > '" + dateValue +"'\n" +
								 "   AND ab.tran_status = 15";
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;

			thisReportType=ReportType.TRAN_INFO_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
								 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
		 						 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +  
								 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
								 "   ab.deal_tracking_num " + Columns.OBJECT_ID.getColumn() + ",ab.reference " + Columns.OBJECT_NAME.getColumn() + ",\n" +
								 "   'TranField: ' + tit.type_name + ' - TranValue: ' + ati.value " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
								 "   ts.name " + Columns.OBJECT_STATUS.getColumn() + ",ab.version_number " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
								 "   ati.last_update " + Columns.MODIFIED_DATE.getColumn() + ",tt.name " + Columns.PROJECT_NAME.getColumn() + "\n" +
								 " FROM ab_tran ab \n" +
								 "   JOIN ab_tran_info ati ON (ati.tran_num=ab.tran_num)\n" +
								 "   JOIN tran_info_types tit ON (tit.type_id=ati.type_id)\n" +
								 "   JOIN personnel p ON (p.id_number=ati.personnel_id)\n" +
								 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
								 "   JOIN trans_status ts ON (ts.trans_status_id=ab.tran_status)\n" +
								 "   JOIN trans_type tt ON (tt.id_number=ab.tran_type)\n" +
								 "   JOIN instruments i ON (i.id_number=ab.ins_type)\n" +
								 "   LEFT JOIN ins_sub_type ist ON (ist.id_number=ab.ins_sub_type)\n" +
								 " WHERE qr.unique_id=" + queryID + "\n" +
								 "   AND ati.last_update > '" + dateValue +"'\n" +
								 "   AND ab.tran_status != 15";
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;

			thisReportType=ReportType.EVENT_INFO_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
								 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
		 						 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +  
								 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
								 "   ab.deal_tracking_num " + Columns.OBJECT_ID.getColumn() + ",ab.reference " + Columns.OBJECT_NAME.getColumn() + ",\n" +
								 "   'EventField: ' + tit.type_name + ' - EventValue: ' + ati.value " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
								 "   ts.name " + Columns.OBJECT_STATUS.getColumn() + ",ab.version_number " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
								 "   ati.last_update " + Columns.MODIFIED_DATE.getColumn() + ",tt.name " + Columns.PROJECT_NAME.getColumn() + "\n" +
								 " FROM ab_tran ab \n" +
								 "   JOIN ab_tran_event ate ON (ab.tran_num=ate.tran_num)\n" +
								 "   JOIN ab_tran_event_info ati ON (ati.event_num=ate.event_num)\n" +
								 "   JOIN tran_event_info_types tit ON (tit.type_id=ati.type_id)\n" +
								 "   JOIN personnel p ON (p.id_number=ati.personnel_id)\n" +
								 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
								 "   JOIN trans_status ts ON (ts.trans_status_id=ab.tran_status)\n" +
								 "   JOIN trans_type tt ON (tt.id_number=ab.tran_type)\n" +
								 "   JOIN instruments i ON (i.id_number=ab.ins_type)\n" +
								 "   LEFT JOIN ins_sub_type ist ON (ist.id_number=ab.ins_sub_type)\n" +
								 " WHERE qr.unique_id=" + queryID + "\n" +
								 "   AND ati.last_update > '" + dateValue +"'\n" +
								 "   AND ab.tran_status != 15";
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;

			thisReportType=ReportType.PARAM_INFO_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
								 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
		 						 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +  
								 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
								 "   ab.deal_tracking_num " + Columns.OBJECT_ID.getColumn() + ",ab.reference " + Columns.OBJECT_NAME.getColumn() + ",\n" +
								 "   'ParamField: ' + tit.type_name + ' - ParamValue: ' + ati.value " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
								 "   ts.name " + Columns.OBJECT_STATUS.getColumn() + ",ab.version_number " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
								 "   ati.last_update " + Columns.MODIFIED_DATE.getColumn() + ",tt.name " + Columns.PROJECT_NAME.getColumn() + "\n" +
								 " FROM ab_tran ab \n" +
								 "   JOIN ins_parameter_info ati ON (ati.ins_num=ab.ins_num AND ati.param_seq_num = 0)\n" +
								 "   JOIN tran_info_types_param_view tit ON (tit.type_id=ati.type_id)\n" +
								 "   JOIN personnel p ON (p.id_number=ati.personnel_id)\n" +
								 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
								 "   JOIN trans_status ts ON (ts.trans_status_id=ab.tran_status)\n" +
								 "   JOIN trans_type tt ON (tt.id_number=ab.tran_type)\n" +
								 "   JOIN instruments i ON (i.id_number=ab.ins_type)\n" +
								 "   LEFT JOIN ins_sub_type ist ON (ist.id_number=ab.ins_sub_type)\n" +
								 " WHERE qr.unique_id=" + queryID + "\n" +
								 "   AND ati.last_update > '" + dateValue +"'\n" +
								 "   AND ab.tran_status != 15";
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;
			
			
			thisReportType=ReportType.PARCEL_INFO_CHANGE;   
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
								 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
		 						 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +  
								 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
								 "   ab.deal_tracking_num " + Columns.OBJECT_ID.getColumn() + ",ab.reference " + Columns.OBJECT_NAME.getColumn() + ",\n" +
								 "   'ParamField: ' + tit.type_name + ' - ParamValue: ' + ati.info_value " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
								 "   ts.name " + Columns.OBJECT_STATUS.getColumn() + ",ab.version_number " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
								 "   ati.last_update " + Columns.MODIFIED_DATE.getColumn() + ",tt.name " + Columns.PROJECT_NAME.getColumn() + "\n" +
								 " FROM ab_tran ab \n" +
								 "   JOIN parcel_data pd ON (pd.ins_num=ab.ins_num)\n" +
								 "   JOIN parcel_info ati ON (ati.parcel_id=pd.ins_num)\n" +
								 "   JOIN parcel_info_types tit ON (tit.type_id=ati.type_id)\n" +
								 "   JOIN personnel p ON (p.id_number=ati.personnel_id)\n" +
								 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
								 "   JOIN trans_status ts ON (ts.trans_status_id=ab.tran_status)\n" +
								 "   JOIN trans_type tt ON (tt.id_number=ab.tran_type)\n" +
								 "   JOIN instruments i ON (i.id_number=ab.ins_type)\n" +
								 "   LEFT JOIN ins_sub_type ist ON (ist.id_number=ab.ins_sub_type)\n" +
								 " WHERE qr.unique_id=" + queryID + "\n" +
								 "   AND ati.last_update > '" + dateValue +"'\n" +
								 "   AND ab.tran_status != 15";
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;
			
			
			thisReportType=ReportType.HISTORICAL_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
						  " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
						  "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
						  "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
						  "   id.index_id " + Columns.OBJECT_ID.getColumn() + ",id.index_name + ' - ' + iit.name + ' - ' + ids.name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
						  "    'Reset: ' + CONVERT(varchar, ihp.reset_date, 103)  + ' - Start: ' + CONVERT(varchar, ihp.start_date, 103) + ' - End: ' + CONVERT(varchar, ihp.end_date, 103) " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
						  "   yb.name " + Columns.OBJECT_STATUS.getColumn() + ", 0 " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
						  "   ihp.last_update " + Columns.MODIFIED_DATE.getColumn() + ",'RefSource: ' + rs.name " + Columns.PROJECT_NAME.getColumn() + "\n" +
						  " FROM idx_historical_prices ihp \n" +
						  "   JOIN personnel p ON (p.id_number=ihp.personnel_id)\n" +
						  "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
						  "   JOIN idx_def id ON (id.index_id = ihp.index_id AND id.db_status=1)\n" +
						  "   JOIN ref_source rs ON (rs.id_number=ihp.ref_source)\n" +
						  "   JOIN yield_basis yb ON (yb.id_number=ihp.yield_basis)\n" + 
						  "   JOIN idx_index_type iit ON (iit.id_number=id.index_type)\n" +
						  "   JOIN idx_status ids ON (ids.id_number=id.index_status)\n" +
						  " WHERE qr.unique_id=" + queryID + "\n" +
						  "   AND ihp.last_update > '" + dateValue +"'";
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;   
			
			thisReportType=ReportType.HISTORICAL_FX_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
					 	  " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
					 	  "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
					 	  "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
						  "   ihfr.currency_id " + Columns.OBJECT_ID.getColumn() + ",c.name + ' - ' + c.description " + Columns.OBJECT_NAME.getColumn() + ",\n" +
						  "    'Rate: ' + CONVERT(varchar, ihfr.fx_rate_date, 103) " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
						  "   'DataType: ' + imdt.name + ' - CurrencyConvention: ' + icct.name " + Columns.OBJECT_STATUS.getColumn() + ", 0 " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
						  "   ihfr.last_update " + Columns.MODIFIED_DATE.getColumn() + ",'IndexName: ' + id.index_name " + Columns.PROJECT_NAME.getColumn() + "\n" +
						  " FROM idx_historical_fx_rates ihfr \n" +
						  "   JOIN personnel p ON (p.id_number=ihfr.personnel_id)\n" +
						  "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
						  "   JOIN currency c ON (c.id_number = ihfr.currency_id )\n" +
						  "   JOIN idx_market_data_type imdt ON (imdt.id_number = ihfr.data_set_type)\n" +
						  "   JOIN idx_currency_convention_type icct ON (icct.id_number = ihfr.currency_convention)\n" +
						  "   JOIN idx_def id ON (id.index_id = c.spot_index AND id.db_status=1)\n" +
						  " WHERE qr.unique_id=" + queryID + "\n" +
						  "   AND ihfr.last_update > '" + dateValue +"'";
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;  
			
			thisReportType=ReportType.MARKET_PRICE_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
					 	  " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
						  "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
						  "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
						  "   id.index_id " + Columns.OBJECT_ID.getColumn() + ",id.index_name + ' - ' + iit.name + ' - ' + ids.name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
						  "    'Dataset_Time: ' + CONVERT(varchar, imd.dataset_time, 103) " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
						  "    'Dataset_Type: ' + imdt.name " + Columns.OBJECT_STATUS.getColumn() + ", 0 " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
						  "   imd.row_creation " + Columns.MODIFIED_DATE.getColumn() + ",imd.dataset_name " + Columns.PROJECT_NAME.getColumn() + "\n" +
						  " FROM idx_market_data imd \n" +
						  "   JOIN personnel p ON (p.id_number=imd.user_id)\n" +
						  "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
						  "   JOIN idx_def id ON (id.index_id = imd.index_id AND id.db_status=1)\n" +
						  "   JOIN idx_index_type iit ON (iit.id_number=id.index_type)\n" +
						  "   JOIN idx_status ids ON (ids.id_number=id.index_status)\n" +
						  "   JOIN idx_market_data_type imdt ON (imdt.id_number=imd.dataset_type)\n" +
						  " WHERE qr.unique_id=" + queryID + "\n" +
						  "   AND imd.row_creation > '" + dateValue +"'";
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;

			
			
			thisReportType=ReportType.BO_DOCUMENT_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
						  " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
						  "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
						  "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
						  "   sdh.document_num " + Columns.OBJECT_ID.getColumn() + ",sd.stldoc_def_name + ' - ' + sdt.doc_type_desc  " + Columns.OBJECT_NAME.getColumn() + ",\n" +
						  "    'Document Type: ' + sdt.doc_type_desc + ' - TemplateName: ' + st.stldoc_template_name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
						  "    'Document Status: ' + sds.doc_status_desc " + Columns.OBJECT_STATUS.getColumn() + ", sdh.doc_version " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
						  "   sdh.last_update " + Columns.MODIFIED_DATE.getColumn() + ",'' " + Columns.PROJECT_NAME.getColumn() + "\n" +
						  " FROM stldoc_header sdh \n" +
						  "   JOIN personnel p ON (p.id_number=sdh.personnel_id)\n" +
						  "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
						  "   JOIN stldoc_document_status sds ON (sds.doc_status=sdh.doc_status)\n" +
						  "   JOIN stldoc_document_type sdt ON (sdt.doc_type=sdh.doc_type)\n" + 
						  "   JOIN stldoc_templates st ON (st.stldoc_template_id=sdh.stldoc_template_id)\n" + 
						  "   JOIN stldoc_definitions sd ON (sd.stldoc_def_id=sdh.stldoc_def_id)\n" + 
						  " WHERE qr.unique_id=" + queryID + "\n" +
						  "   AND sdh.last_update > '" + dateValue +"'";
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;

						 
			DBaseTable.execISql(tblFinacialChangedData, sqlCommand);
			sqlCommand = "";
			PluginLog.debug("Total Number of rows tblFinacialChangedData: " + tblFinacialChangedData.getNumRows());
			// End of Financial Changes
			
			// Start of Deployment Changes
			
			thisReportType=ReportType.QUERY_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
						  " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
						  "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
						  "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
						  "   qv.query_id " + Columns.OBJECT_ID.getColumn() + ",qv.query_name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
						  "   qv.query_group_name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
						  "    'QueryChange: N/A' " + Columns.OBJECT_STATUS.getColumn() + ", qv.query_version " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
						  "   qv.last_update " + Columns.MODIFIED_DATE.getColumn() + ",\n" + 
						  "   'Report Builder Group: ' + ISNULL(dqg.dxr_query_group_name , 'Unknown')" + Columns.PROJECT_NAME.getColumn() + "\n" +
						  " FROM query_view qv \n" +
						  "   JOIN personnel p ON (p.id_number=qv.personnel_id)\n" +
						  "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
						  "   LEFT JOIN dxr_query_group dqg ON (dqg.query_group_id=qv.query_group_id)\n" +
						  " WHERE qr.unique_id=" + queryID + "\n" +
						  "   AND qv.last_update > '" + dateValue +"'";
			sqlCommand = sqlCommand1;

			thisReportType=ReportType.OPS_SERVICE_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
						  " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
						  "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
						  "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
						  "   red.exp_defn_id " + Columns.OBJECT_ID.getColumn() + ",red.defn_name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
						  "   ost.name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
						  "    'Status: ' + sds.name " + Columns.OBJECT_STATUS.getColumn() + ", red.version_number " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
						  "   red.last_update " + Columns.MODIFIED_DATE.getColumn() + ",ost.name " + Columns.PROJECT_NAME.getColumn() + "\n" +
						  " FROM rsk_exposure_defn red \n" +
						  "   JOIN personnel p ON (p.id_number=red.personnel_id)\n" +
						  "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
						  "   JOIN ops_service_type ost ON (ost.id_number=red.defn_type)\n" +
						  "   JOIN service_defn_status sds ON (sds.id_number=red.defn_status)\n" +
						  " WHERE qr.unique_id=" + queryID + "\n" +
						  "   AND red.last_update > '" + dateValue +"'";
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;
			
			
			thisReportType=ReportType.CODE_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
						  " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
						  "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
						  "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
						  "   dn.node_id " + Columns.OBJECT_ID.getColumn() + ", dn.node_name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
						  "    'User: ' + dsp_up.name + ' - Group: ' + dsp_gp.name + ' - Public: ' + dsp_pp.name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
						  "   CONVERT(varchar(10),dn.parent_node_id) " + Columns.OBJECT_STATUS.getColumn() + ", dn.version " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
						  "   dn.last_update " + Columns.MODIFIED_DATE.getColumn() + ",dn.node_name " + Columns.PROJECT_NAME.getColumn() + "\n" +
						  " FROM dir_node_history dn \n" +  
						  "   JOIN personnel p ON (p.id_number=dn.update_user_id)\n" +
						  "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
						  "   JOIN dir_node_type dnt ON (dnt.id_number=dn.node_type)\n" +
						  "   JOIN dir_security_permiss dsp_up ON (dsp_up.id_number=dn.user_permiss)\n" +
						  "   JOIN dir_security_permiss dsp_gp ON (dsp_gp.id_number=dn.group_permiss)\n" +
						  "   JOIN dir_security_permiss dsp_pp ON (dsp_pp.id_number=dn.public_permiss)\n" +
						  " WHERE qr.unique_id=" + queryID + "\n" +
						  "   AND dn.last_update > '" + dateValue +"'\n" + 
						  "   AND dn.node_type = 7";  // Code
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;
			
			thisReportType=ReportType.AUTOMATCH_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
						 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
						 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
						 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
						 "   dn.node_id " + Columns.OBJECT_ID.getColumn() + ", dn.node_name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
						 "    'User: ' + dsp_up.name + ' - Group: ' + dsp_gp.name + ' - Public: ' + dsp_pp.name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
						 "   CONVERT(varchar(10),dn.parent_node_id) " + Columns.OBJECT_STATUS.getColumn() + ", dn.version " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
						 "   dn.last_update " + Columns.MODIFIED_DATE.getColumn() + ",dn.node_name " + Columns.PROJECT_NAME.getColumn() + "\n" +
						 " FROM dir_node dn \n" +
						 "   JOIN personnel p ON (p.id_number=dn.update_user_id)\n" +
						 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
						 "   JOIN dir_node_type dnt ON (dnt.id_number=dn.node_type)\n" +
						 "   JOIN dir_security_permiss dsp_up ON (dsp_up.id_number=dn.user_permiss)\n" +
						 "   JOIN dir_security_permiss dsp_gp ON (dsp_gp.id_number=dn.group_permiss)\n" +
						 "   JOIN dir_security_permiss dsp_pp ON (dsp_pp.id_number=dn.public_permiss)\n" +
						 " WHERE qr.unique_id=" + queryID + "\n" +
						 "   AND dn.last_update > '" + dateValue +"'\n" + 
						 "   AND dn.node_type IN (" + NODE_TYPE_LIST + ")";     // 17- auto match, 19 - Connex
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;

			thisReportType=ReportType.SCREEN_CONFIG_CHANGE;  
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
						 " sct.config_type_name " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
						 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
						 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
						 "   dn.node_id " + Columns.OBJECT_ID.getColumn() + ", dn.node_name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
						 "    'User: ' + dsp_up.name + ' - Group: ' + dsp_gp.name + ' - Public: ' + dsp_pp.name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
						 "    CONVERT(varchar(10),dn.parent_node_id) " + Columns.OBJECT_STATUS.getColumn() + ", dn.version " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
						 "   dn.last_update " + Columns.MODIFIED_DATE.getColumn() + ",dn.node_name " + Columns.PROJECT_NAME.getColumn() + "\n" +
						 " FROM dir_node_history dn \n" +
						 "   JOIN personnel p ON (p.id_number=dn.update_user_id)\n" +
						 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
						 "   JOIN screen_config_type sct ON(sct.config_type_id=dn.category)\n" +
						 "   JOIN dir_node_type dnt ON (dnt.id_number=dn.node_type)\n" +
						 "   JOIN dir_security_permiss dsp_up ON (dsp_up.id_number=dn.user_permiss)\n" +
						 "   JOIN dir_security_permiss dsp_gp ON (dsp_gp.id_number=dn.group_permiss)\n" +
						 "   JOIN dir_security_permiss dsp_pp ON (dsp_pp.id_number=dn.public_permiss)\n" +
						 " WHERE qr.unique_id=" + queryID + "\n" +
						 "   AND dn.last_update > '" + dateValue +"'\n" + 
						 "   AND dn.node_type = 4"; 		// Screen Config
						 
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;
			
			thisReportType=ReportType.TPM_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
						 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
						 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
						 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
						 "   bd.bpm_process_defn_id " + Columns.OBJECT_ID.getColumn() + ", bd.bpm_name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
						 "    'TPM Category: ' + bc.bpm_category_name + ' - Authorization Status: ' + sds.name + ' - Trigger Type: ' + bt.name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
						 "   bs.name " + Columns.OBJECT_STATUS.getColumn() + ", bd.bpm_version_number " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
						 "   bd.last_update " + Columns.MODIFIED_DATE.getColumn() + ",bt.name + '-' + bc.bpm_category_name + '-' + bd.bpm_name " + Columns.PROJECT_NAME.getColumn() + "\n" +
						 " FROM bpm_definition bd\n" +
						 "   JOIN personnel p ON (p.id_number=bd.user_id)\n" +
						 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
						 "   JOIN bpm_category bc ON (bc.bpm_category_id=bd.bpm_category)\n" +
						 "   JOIN service_defn_status sds ON (sds.id_number=bd.bpm_auth_status)\n" +
						 "   JOIN bpm_trigger bt ON (bt.id_number=bd.bpm_trigger)\n" +
						 "   JOIN bpm_status bs ON (bs.id_number=bd.bpm_status)\n" +					 
						 " WHERE qr.unique_id=" + queryID + "\n" +
						 "   AND bd.last_update > '" + dateValue +"'"; 
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;

			thisReportType=ReportType.TASK_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
						 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
						 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
						 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
						 "   atdv.task_id " + Columns.OBJECT_ID.getColumn() + ", dn.node_name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
						 "    'User: ' + dsp_up.name + ' - Group: ' + dsp_gp.name + ' - Public: ' + dsp_pp.name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
						 "   dnt.name + ' - Active' " + Columns.OBJECT_STATUS.getColumn() + ", atdv.version " + Columns.CHANGE_VERSION.getColumn() + ",\n" + 
						 "   dn.last_update " + Columns.MODIFIED_DATE.getColumn() + ",'Param:' + ISNULL(dn_p.node_name,'None') + ' - Main:' + ISNULL(dn_m.node_name,'None')  " + Columns.PROJECT_NAME.getColumn() + "\n" +
						 " FROM dir_node_history dn \n" +
						 "   JOIN personnel p ON (p.id_number=dn.update_user_id)\n" +
						 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
						 "   JOIN dir_node_type dnt ON (dnt.id_number=dn.node_type)\n" +
						 "   JOIN avs_task_def_view atdv ON (atdv.task_id=dn.client_data_id)\n" +
						 "   LEFT JOIN avs_task_scripts ats_p ON (ats_p.task_id=atdv.task_id AND ats_p.script_type=0)\n" +
						 "   LEFT JOIN dir_node  dn_p ON (dn_p.client_data_id=ats_p.script_id AND dn_p.node_type=7)\n" +
						 "   LEFT JOIN avs_task_scripts ats_m ON (ats_m.task_id=atdv.task_id AND ats_m.script_type=1)\n" +
						 "   LEFT JOIN dir_node dn_m ON (dn_m.client_data_id=ats_m.script_id AND dn_m.node_type=7)\n" +
						 "   JOIN dir_security_permiss dsp_up ON (dsp_up.id_number=dn.user_permiss)\n" +
						 "   JOIN dir_security_permiss dsp_gp ON (dsp_gp.id_number=dn.group_permiss)\n" +
						 "   JOIN dir_security_permiss dsp_pp ON (dsp_pp.id_number=dn.public_permiss)\n" +
						 " WHERE qr.unique_id=" + queryID + "\n" +
						 "   AND dn.last_update > '" + dateValue +"'\n" + 
						 "   AND dn.node_type = 2";     // 2 - Tasks
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;

			
			thisReportType=ReportType.REPORT_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
						 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
						 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
						 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
						 "   dd.dxr_definition_id " + Columns.OBJECT_ID.getColumn() + ", dd.dxr_definition_name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
						 "    'User: ' + dsp_up.name + ' - Group: ' + dsp_gp.name + ' - Public: ' + dsp_pp.name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
						 "   das.dxr_auth_status_name " + Columns.OBJECT_STATUS.getColumn() + ", dd.dxr_definition_ver " + Columns.CHANGE_VERSION.getColumn() + ",\n" + 
						 "   dn.last_update " + Columns.MODIFIED_DATE.getColumn() + ",'Group: ' + drg.dxr_report_group_name + ' - Type: ' + dqg.dxr_query_group_name + ' - Query: ' + ISNULL(qv.query_name,'N/A') " + Columns.PROJECT_NAME.getColumn() + "\n" +
						 " FROM dir_node_history dn \n" +
						 "   JOIN personnel p ON (p.id_number=dn.update_user_id)\n" +
						 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
						 "   JOIN dir_node_type dnt ON (dnt.id_number=dn.node_type)\n" +
						 "   JOIN dxr_definition dd ON (dd.dxr_definition_id=dn.client_data_id)\n" +
						 "   JOIN dxr_auth_status das ON (das.dxr_auth_status_id=dd.dxr_auth_status_id)\n" +
						 "   JOIN dxr_report_group drg ON (drg.dxr_report_group_id=dd.dxr_report_group_id )\n" +					 
						 "   JOIN dxr_query_group dqg ON (dqg.query_group_id=dd.query_group_id )\n" +
						 "   LEFT JOIN query_view qv ON (qv.query_id=dd.query_def_id )\n" +
						 "   JOIN dir_security_permiss dsp_up ON (dsp_up.id_number=dn.user_permiss)\n" +
						 "   JOIN dir_security_permiss dsp_gp ON (dsp_gp.id_number=dn.group_permiss)\n" +
						 "   JOIN dir_security_permiss dsp_pp ON (dsp_pp.id_number=dn.public_permiss)\n" +
						 " WHERE qr.unique_id=" + queryID + "\n" +
						 "   AND dn.last_update > '" + dateValue +"'\n" + 
						 "   AND dn.node_type = 15";     // 15 Report Builder
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;

			thisReportType=ReportType.INDEX_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
						 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
						 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
						 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
						 "   id.index_id " + Columns.OBJECT_ID.getColumn() + ",  id.index_name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
						 "    'Index Type: ' + iit.name + ' - Index Status: ' + idxs.name + ' - Index Market: ' + im.name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
						 "   ids.name " + Columns.OBJECT_STATUS.getColumn() + ", id.index_version_id " + Columns.CHANGE_VERSION.getColumn() + ",\n" + 
						 "   id.last_update " + Columns.MODIFIED_DATE.getColumn() + ", 'Index Group: ' + ig.name + ' - Index Currency: ' + c.name + ' - Reference Source: ' + rs.name " + Columns.PROJECT_NAME.getColumn() + "\n" +
						 " FROM idx_def id \n" +
						 "   JOIN personnel p ON (p.id_number=id.update_user_id)\n" +
						 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
						 "   JOIN idx_index_type iit ON (iit.id_number=id.index_type)\n" +
						 "   JOIN idx_status idxs ON (idxs.id_number=id.index_status)\n" +
						 "   JOIN idx_db_status ids ON (ids.id_number=id.db_status)\n" +
						 "   JOIN idx_market im ON (im.id_number=id.market)\n" +					 
						 "   JOIN idx_group ig ON (ig.id_number=id.idx_group)\n" +					 
						 "   JOIN currency c ON (c.id_number=id.currency)\n" +
						 "   JOIN ref_source rs ON (rs.id_number=id.ref_source)\n" +					 
						 " WHERE qr.unique_id=" + queryID + "\n" +
						 "   AND id.last_update > '" + dateValue +"'"; 
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;
			
			
			DBaseTable.execISql(tblDeploymentChangedData, sqlCommand);
			sqlCommand = "";
			PluginLog.debug("Total Number of rows tblDeploymentChangedData: " + tblDeploymentChangedData.getNumRows());
			
			
			// End of Deployment Changes
			
			// Start of Artifact Changes
			thisReportType=ReportType.SQL_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
						 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
						 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
						 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
						 "   dn.node_id " + Columns.OBJECT_ID.getColumn() + ", dn.node_name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
						 "    'User: ' + dsp_up.name + ' - Group: ' + dsp_gp.name + ' - Public: ' + dsp_pp.name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
						 "   CONVERT(varchar(10),dn.parent_node_id) " + Columns.OBJECT_STATUS.getColumn() + ", dn.version " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
						 "   dn.last_update " + Columns.MODIFIED_DATE.getColumn() + ",dn.node_name + ' Node Type: ' + dnt.name " + Columns.PROJECT_NAME.getColumn() + "\n" +
						 " FROM dir_node_history dn \n" + 
						 "   JOIN personnel p ON (p.id_number=dn.update_user_id)\n" +
						 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
						 "   JOIN dir_node_type dnt ON (dnt.id_number=dn.node_type)\n" +
						 "   JOIN dir_security_permiss dsp_up ON (dsp_up.id_number=dn.user_permiss)\n" +
						 "   JOIN dir_security_permiss dsp_gp ON (dsp_gp.id_number=dn.group_permiss)\n" +
						 "   JOIN dir_security_permiss dsp_pp ON (dsp_pp.id_number=dn.public_permiss)\n" +
						 " WHERE qr.unique_id=" + queryID + "\n" +
						 "   AND dn.last_update > '" + dateValue +"'\n" + 
						 "   AND dn.node_type = 9\n" +		// User Defined
						 "   AND dn.parent_node_id = 21588"  ;   //23  -- SQL report builder datasources
			sqlCommand =  sqlCommand1;

			thisReportType=ReportType.APM_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
						 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
						 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
						 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
						 "   dn.node_id " + Columns.OBJECT_ID.getColumn() + ", dn.node_name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
						 "    'User: ' + dsp_up.name + ' - Group: ' + dsp_gp.name + ' - Public: ' + dsp_pp.name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
						 "   CONVERT(varchar(10),dn.parent_node_id) " + Columns.OBJECT_STATUS.getColumn() + ", dn.version " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
						 "   dn.last_update " + Columns.MODIFIED_DATE.getColumn() + ",dn.node_name " + Columns.PROJECT_NAME.getColumn() + "\n" +
						 " FROM dir_node_history dn \n" +
						 "   JOIN personnel p ON (p.id_number=dn.update_user_id)\n" +
						 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
						 "   JOIN dir_node_type dnt ON (dnt.id_number=dn.node_type)\n" +
						 "   JOIN dir_security_permiss dsp_up ON (dsp_up.id_number=dn.user_permiss)\n" +
						 "   JOIN dir_security_permiss dsp_gp ON (dsp_gp.id_number=dn.group_permiss)\n" +
						 "   JOIN dir_security_permiss dsp_pp ON (dsp_pp.id_number=dn.public_permiss)\n" +
						 " WHERE qr.unique_id=" + queryID + "\n" +
						 "   AND dn.last_update > '" + dateValue +"'\n" + 
						 "   AND dn.node_type = 9\n" +		// User Defined
						 //  "   AND dn.parent_node_id IN (23787,23877)\n" + //APM pgg file changes
						 "   AND RIGHT(dn.node_name, 3) = 'ppg'";   
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;

			thisReportType=ReportType.DMS_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
						 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
						 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
						 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
						 "   dn.node_id " + Columns.OBJECT_ID.getColumn() + ", dn.node_name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
						 "    'User: ' + dsp_up.name + ' - Group: ' + dsp_gp.name + ' - Public: ' + dsp_pp.name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
						 "    CONVERT(varchar(10),dn.parent_node_id) " + Columns.OBJECT_STATUS.getColumn() + ", dn.version " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
						 "   dn.last_update " + Columns.MODIFIED_DATE.getColumn() + ",dn.node_name " + Columns.PROJECT_NAME.getColumn() + "\n" +
						 " FROM dir_node_history dn \n" +
						 "   JOIN personnel p ON (p.id_number=dn.update_user_id)\n" +
						 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
						 "   JOIN dir_node_type dnt ON (dnt.id_number=dn.node_type)\n" +
						 "   JOIN dir_security_permiss dsp_up ON (dsp_up.id_number=dn.user_permiss)\n" +
						 "   JOIN dir_security_permiss dsp_gp ON (dsp_gp.id_number=dn.group_permiss)\n" +
						 "   JOIN dir_security_permiss dsp_pp ON (dsp_pp.id_number=dn.public_permiss)\n" +
						 " WHERE qr.unique_id=" + queryID + "\n" +
						 "   AND dn.last_update > '" + dateValue +"'\n" + 
						 "   AND dn.node_type = 9\n" +		// User Defined
						 "   AND dn.parent_node_id NOT IN (21588,23787,23877, 23)\n" +
						 "   AND RIGHT(dn.node_name, 3) IN ('olt','olc')";   // Other must be DMS
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;

			
			
			DBaseTable.execISql(tblArtifactChangedData, sqlCommand);
			sqlCommand = "";
			PluginLog.debug("Total Number of rows tblArtifactChangedData: " + tblArtifactChangedData.getNumRows());
			
			
			// End of Artifact Changes
			
			// Start of Standing Data Changes

			
			thisReportType=ReportType.PARTY_CHANGE;  
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
						 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
						 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
						 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
						 "   pa.party_id " + Columns.OBJECT_ID.getColumn() + ", pa.short_name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
						 "    'Party Class ID: ' + pc.name + ' - Internal/External: ' + ie.name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
						 "   ps.name " + Columns.OBJECT_STATUS.getColumn() + ", pa.party_version " + Columns.CHANGE_VERSION.getColumn() + ",\n" + 
						 "   pa.last_update " + Columns.MODIFIED_DATE.getColumn() + ", 'Party Group: ' + pg.short_name " + Columns.PROJECT_NAME.getColumn() + "\n" +
						 " FROM  party pa \n" +
						 "   JOIN personnel p ON (p.id_number=pa.authoriser_id)\n" +
						 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
						 "   JOIN party_status ps ON (ps.id_number=pa.party_status)\n" +
						 "   JOIN party_class pc ON (pc.id_number=pa.party_class)\n" +
						 "   JOIN internal_external ie ON (ie.id_number=pa.int_ext)\n" +
						 "   JOIN party_group_memb pgm ON (pgm.party_id=pa.party_id)\n" +					 
						 "   JOIN party_group pg ON (pgm.group_id=pg.group_id)\n" +					 
						 " WHERE qr.unique_id=" + queryID + "\n" +
						 "   AND pa.last_update > '" + dateValue +"'"; 
			sqlCommand = sqlCommand1;
			

			thisReportType=ReportType.PERSONNEL_CHANGE;  
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
						 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
						 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
						 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
						 "   per.id_number " + Columns.OBJECT_ID.getColumn() + ", per.name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
						 "    'Personnel Type: ' + pt.name + ' - Country: ' + ISNULL(c.name,'Unknown') " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
						 "   ps.name " + Columns.OBJECT_STATUS.getColumn() + ", per.personnel_version " + Columns.CHANGE_VERSION.getColumn() + ",\n" + 
						 "   per.last_update " + Columns.MODIFIED_DATE.getColumn() + ", 'First Name: ' + per.first_name +  ' Last Name: ' + per.last_name " + Columns.PROJECT_NAME.getColumn() + "\n" +
						 " FROM personnel_history per \n" +
						 "   JOIN personnel p ON (p.id_number=per.authoriser)\n" +
						 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
						 "   JOIN personnel_type pt ON (pt.id_number=per.personnel_type)\n" +
						 "   JOIN personnel_status ps ON (ps.id_number=per.status)\n" +
						 "   LEFT JOIN country c ON (c.id_number=per.country)\n" +
						 " WHERE qr.unique_id=" + queryID + "\n" +
						 "   AND per.last_update > '" + dateValue +"'"; 
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;
			
			thisReportType=ReportType.ACCOUNTS_CHANGE;  
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
						 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
						 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
						 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
						 "   a.account_id " + Columns.OBJECT_ID.getColumn() + ", a.account_name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
						 "    'Account Type: ' + pt.name +  ' - Account Class: ' + ISNULL(ac.account_class_name,'Unknown') + ' - Account Holder: ' + pa.short_name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
						 "   ps.name " + Columns.OBJECT_STATUS.getColumn() + ", a.account_version " + Columns.CHANGE_VERSION.getColumn() + ",\n" + 
						 "   a.last_update " + Columns.MODIFIED_DATE.getColumn() + ", 'Account Num: ' + a.account_number + ' - BaseCurency: ' + cu.name + ' - Country: ' + ISNULL(c.name,'Unknown') " + Columns.PROJECT_NAME.getColumn() + "\n" +
						 " FROM account a \n" + 
						 "   JOIN personnel p ON (p.id_number=a.user_id)\n" +
						 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
						 "   JOIN account_type pt ON (pt.id_number=a.account_type)\n" +
						 "   JOIN ref_authorization ps ON (ps.id_number=a.account_status)\n" +
						 "   JOIN party pa ON (pa.party_id=a.holder_id)\n" +
						 "   JOIN currency cu ON (cu.id_number=a.base_currency)\n" +
						 "   LEFT JOIN country c ON (c.id_number=a.account_country)\n" +
						 "   LEFT JOIN account_class ac ON (ac.account_class_id=a.account_class)\n" +
						 " WHERE qr.unique_id=" + queryID + "\n" +
						 "   AND a.last_update > '" + dateValue +"'"; 
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;
			
			thisReportType=ReportType.SI_CHANGE;  
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
						 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
						 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
						 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
						 "   si.settle_id " + Columns.OBJECT_ID.getColumn() + ", si.settle_name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
						 "    'Account: ' + a.account_name +  ' - Party: ' + pa.short_name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
						 "   ra.name " + Columns.OBJECT_STATUS.getColumn() + ", si.settle_instruct_version " + Columns.CHANGE_VERSION.getColumn() + ",\n" + 
						 "   si.last_update " + Columns.MODIFIED_DATE.getColumn() + ", 'Instruction Method: ' + dc_im.Name + ' - PaymentMethod: ' + dc_pm.name " + Columns.PROJECT_NAME.getColumn() + "\n" +
						 " FROM settle_instructions si \n" + 
						 "   JOIN personnel p ON (p.id_number=si.user_id)\n" +
						 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
						 "   JOIN account a ON (a.account_id=si.account_id)\n" +
						 "   JOIN party pa ON (pa.party_id=si.party_id)\n" +
						 "   JOIN ref_authorization ra ON (ra.id_number=si.settle_status)\n" +
						 "   JOIN delivery_code dc_im ON (dc_im.id_number=si.instruct_method)\n" +
						 "   JOIN delivery_code dc_pm ON (dc_pm.id_number=si.instruct_method)\n" +
						 " WHERE qr.unique_id=" + queryID + "\n" +
						 "   AND si.last_update > '" + dateValue +"'"; 
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;

			thisReportType=ReportType.PORTFOLIO_CHANGE;  
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
						 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
						 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
						 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
						 "   po.id_number " + Columns.OBJECT_ID.getColumn() + ", po.name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
						 "    'Portfolio Type: ' + pt.name +  ' - Restricted Flag: ' + ny_re.name +  ' - Requires Strategy: ' + ny_rs.name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
						 "   ISNULL(pg.portfolio_group_name,'All Portfolios') " + Columns.OBJECT_STATUS.getColumn() + ", po.portfolio_version " + Columns.CHANGE_VERSION.getColumn() + ",\n" + 
						 "   po.last_update " + Columns.MODIFIED_DATE.getColumn() + ", 'Desk Loc: ' + ISNULL(pi_dl.info_value,'Unknown')   + ' - JDE Desk Loc: ' + ISNULL(pi_jdl.info_value,'Unknown') + ' - Include PnL: ' + ISNULL(pi_pnl.info_value,'Yes') " + Columns.PROJECT_NAME.getColumn() + "\n" +
						 " FROM portfolio po \n" + 
						 "   JOIN personnel p ON (p.id_number=po.authoriser_id)\n" +
						 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
						 "   JOIN portfolio_type pt ON (pt.id_number=po.portfolio_type)\n" +
						 "   JOIN no_yes ny_re ON (ny_re.id_number=po.restricted)\n" +
						 "   JOIN no_yes ny_rs ON (ny_rs.id_number=po.requires_strategy )\n" +
						 "   LEFT JOIN portfolio_group_to_port pgp ON (pgp.port_id=po.id_number)\n" +					 
						 "   LEFT JOIN portfolio_groups pg ON (pgp.portfolio_group_id=pg.portfolio_group_id )\n" +					 
						 "   LEFT JOIN (SELECT info_value, portfolio_id FROM portfolio_info pi WHERE pi.info_type_id = 20001) pi_dl ON (pi_dl.portfolio_id=po.id_number)\n" +  // Desk Location	
						 "   LEFT JOIN (SELECT info_value, portfolio_id FROM portfolio_info pi WHERE pi.info_type_id = 20002) pi_jdl ON (pi_jdl.portfolio_id=po.id_number)\n" +  // JDE Desk Location
						 "   LEFT JOIN (SELECT info_value, portfolio_id FROM portfolio_info pi WHERE pi.info_type_id = 20003) pi_pnl ON (pi_pnl.portfolio_id=po.id_number)\n" +  // Include in PnL Calculations
						 " WHERE qr.unique_id=" + queryID + "\n" +
						 " AND pg.portfolio_group_name!='All Portfolios'\n" +
						 "   AND po.last_update > '" + dateValue +"'"; 
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;

			
			DBaseTable.execISql(tblStandingDataChangedData, sqlCommand);
			sqlCommand = "";
			PluginLog.debug("Total Number of rows tblStandingDataChangedData: " + tblStandingDataChangedData.getNumRows());
			
			
			// End of Standing Data Changes
			 

			PluginLog.debug("Finished gathering SQL.");
			tblAllChangedData= tblDeploymentChangedData.copyTable();

			mergeChangesIntoAllChange (tblAllChangedData, tblFinacialChangedData, "Financial");
			mergeChangesIntoAllChange (tblAllChangedData, tblArtifactChangedData  , "Artifact");
			mergeChangesIntoAllChange (tblAllChangedData, tblStandingDataChangedData , "Standing Data");
			//mergeChangesIntoAllChange (tblAllChangedData, tblUserTableChangedData, "User Table" );
			

			

			
			
			Table directoryNodeDir = Table.tableNew();
			directoryNodeDir = getDirectoryNodeDir(directoryNodeDir);
			PluginLog.debug("Successfully ran Dir Node SQL. Dir Node Number of rows: " + directoryNodeDir.getNumRows());

			// Bring in Code Package Name Location
			bringInDirectoryLocation(tblAllChangedData, directoryNodeDir, ReportType.CODE_CHANGE._objectTypeID , "Code", true );

			
			// Bring in SQL Location
			bringInDirectoryLocation(tblAllChangedData, directoryNodeDir, ReportType.SQL_CHANGE._objectTypeID , "SQL", false );

			// Bring in APM Location
			bringInDirectoryLocation(tblAllChangedData, directoryNodeDir, ReportType.APM_CHANGE._objectTypeID , "APM", false );

			// Bring in DMS Location
			bringInDirectoryLocation(tblAllChangedData, directoryNodeDir, ReportType.DMS_CHANGE._objectTypeID , "DMS", false );

			// Bring in Screen Config Location
			bringInDirectoryLocation(tblAllChangedData, directoryNodeDir, ReportType.SCREEN_CONFIG_CHANGE._objectTypeID , "Screen Config" , false);
			
			
			// Enrich With Personal Analysis data.
			Table personnelAnalysisDetails = Table.tableNew();
			personnelAnalysisDetails = getPersonnelAnalysis(personnelAnalysisDetails);
			PluginLog.debug("Successfully ran Dir Node SQL. Personnel Analysis rows: " + personnelAnalysisDetails.getNumRows());
			
			Table deltaChanges = Table.tableNew();  	 
			deltaChanges.select(tblAllChangedData, "*", SupportChangeAuditConstants.COL_OBJECT_TYPE_ID + " EQ " + ReportType.PERSONNEL_CHANGE._objectTypeID);
			PluginLog.debug("Personnel Changes. Total Number of rows: " + deltaChanges.getNumRows());
			getPersonnelAnalysisDetails (deltaChanges,personnelAnalysisDetails  );
			tblAllChangedData.select(deltaChanges, COL_OBJECT_REFERENCE + ","  + COL_PROJECT_NAME, 
								SupportChangeAuditConstants.COL_OBJECT_ID + " EQ $" + SupportChangeAuditConstants.COL_OBJECT_ID 
					+ " AND " + SupportChangeAuditConstants.COL_OBJECT_TYPE_ID + " EQ $" + SupportChangeAuditConstants.COL_OBJECT_TYPE_ID 
					+ " AND " + SupportChangeAuditConstants.COL_CHANGE_VERSION + " EQ $" + SupportChangeAuditConstants.COL_CHANGE_VERSION);
			PluginLog.debug("Post Applying Personnel Details. Total Number of rows: " + tblAllChangedData.getNumRows());

			
			if (Table.isTableValid(directoryNodeDir) == 1) {
				directoryNodeDir.destroy();
			}
			if (Table.isTableValid(personnelAnalysisDetails) == 1) {
				personnelAnalysisDetails.destroy();
			}
			if (Table.isTableValid(deltaChanges) == 1) {
				deltaChanges.destroy();
			}
			// add the extra columns
			formatReturnTable(tblAllChangedData);

			// Get the pointers
			totalRows = tblAllChangedData.getNumRows();
			PluginLog.debug("Total Number of rows Personnel Rows: " + tblAllChangedData.getNumRows());

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
			returnt.select(tblAllChangedData, copyColumns, Columns.PERSONNEL_ID.getColumn() + " GT 0");
			PluginLog.debug("Total Number of rows Personnel Rows: " + returnt.getNumRows());
			// @formatter:on

		} catch (Exception e) {
			throw new OException(e.getMessage());
		} finally {
			PluginLog.debug("Results processing finished. Total Number of results recovered: " + totalRows + " processed: " + tblAllChangedData.getNumRows());

			if (Table.isTableValid(tblAllChangedData) == 1) {
				tblAllChangedData.destroy();
			}
			if (Table.isTableValid(tblFinacialChangedData) == 1) {
				tblFinacialChangedData.destroy();
			}
			if (Table.isTableValid(tblDeploymentChangedData) == 1) {
				tblDeploymentChangedData.destroy();
			}
			if (Table.isTableValid(tblArtifactChangedData) == 1) {
				tblArtifactChangedData.destroy();
			}
			if (Table.isTableValid(tblStandingDataChangedData) == 1) {
				tblStandingDataChangedData.destroy();
			}
			if (Table.isTableValid(tblUserTableChangedData) == 1) {
				tblUserTableChangedData.destroy();
			}
 
		}
	}


	private void mergeChangesIntoAllChange(Table tblAllChangedData, Table tblInsertData, String changeType) throws OException {
		
		int numRows = tblInsertData.getNumRows();
		int columnsBefore = tblAllChangedData.getNumCols();
		int numRowsBefore = tblAllChangedData.getNumRows();
		if (numRows>0){
			int retValue = tblInsertData.copyRowAddAll(tblAllChangedData);
			if (retValue!=1){
				PluginLog.error("Problem inserting rows to All Change" + changeType + " Rows: " + numRows );
			} else {
				PluginLog.debug("Total Number of results recovered" + changeType + " Rows: " + numRows );
			}
		}
		int columnsAfter = tblAllChangedData.getNumCols();
		int numRowsAfter = tblAllChangedData.getNumRows();
		
		PluginLog.debug("Results processing finished. Total Number of rows beofore: " + numRowsBefore + " rows After: " + numRowsAfter);

		if (columnsAfter!=columnsBefore){
			PluginLog.error("Mismatching columns counts of tblAllChangedData Before : " + columnsBefore + " after: " + columnsAfter);

		}
		
	}


	private void bringInDirectoryLocation(Table tblAllChangedData, Table directoryNodeDir, int reportTypeID, String lable, boolean codeChange) throws OException {
		Table deltaChanges;
		deltaChanges = Table.tableNew();
		deltaChanges.select(tblAllChangedData, "*", SupportChangeAuditConstants.COL_OBJECT_TYPE_ID + " EQ " + reportTypeID);
		PluginLog.debug(lable + " Changes. Total Number of rows: " + deltaChanges.getNumRows());
		getCodeProjectDetails (deltaChanges,directoryNodeDir , codeChange);
		
		tblAllChangedData.select(deltaChanges,COL_OBJECT_STATUS + ","  + COL_PROJECT_NAME, 
				SupportChangeAuditConstants.COL_OBJECT_ID + " EQ $" + SupportChangeAuditConstants.COL_OBJECT_ID 
				+ " AND " + SupportChangeAuditConstants.COL_OBJECT_TYPE_ID + " EQ $" + SupportChangeAuditConstants.COL_OBJECT_TYPE_ID  +
				 " AND " + SupportChangeAuditConstants.COL_CHANGE_VERSION + " EQ $" + SupportChangeAuditConstants.COL_CHANGE_VERSION);

		PluginLog.debug("Post Applying " + lable + " Directory. Total Number of rows: " + tblAllChangedData.getNumRows());
		
		deltaChanges.destroy();
	}



	private Table getPersonnelAnalysis(Table personnelAnalysisDetails) throws OException {
		String sql = "SELECT * FROM USER_support_personnel_analysis usga\n" +
					 "ORDER BY usga.id_number, usga.per_personnel_version";

		DBaseTable.execISql(personnelAnalysisDetails, sql);
	
		return personnelAnalysisDetails;
	}
	 


	private void getCodeProjectDetails(Table codeChanges, Table directoryNodeDir, boolean codeChange) throws OException {
		
		codeChanges.group( SupportChangeAuditConstants.COL_OBJECT_ID + "," + SupportChangeAuditConstants.COL_CHANGE_VERSION );
		int codeChangesCount = codeChanges.getNumRows();
		for (int iLoop = 1; iLoop<=codeChangesCount;iLoop++){
			int parentNodeID = 0;

			String parentNodeIDStr = codeChanges.getString(SupportChangeAuditConstants.COL_OBJECT_STATUS , iLoop);
			parentNodeID = Integer.parseInt(parentNodeIDStr);
			
			String getProjectPackageName = getPackageName(parentNodeID,directoryNodeDir, codeChange);
			codeChanges.setString(SupportChangeAuditConstants.COL_PROJECT_NAME, iLoop ,getProjectPackageName);

			codeChanges.setString(SupportChangeAuditConstants.COL_OBJECT_STATUS , iLoop, "Active");
			if (iLoop>1){
				if (codeChanges.getInt(SupportChangeAuditConstants.COL_OBJECT_ID, iLoop-1) ==codeChanges.getInt(SupportChangeAuditConstants.COL_OBJECT_ID, iLoop)){
					codeChanges.setString(SupportChangeAuditConstants.COL_OBJECT_STATUS , iLoop-1, "Old");
				} 				
			} 
			
			
		}
		
	}

	private void getPersonnelAnalysisDetails(Table personnelChanges, Table personnelAnalysis) throws OException {
		

		
		int codeChangesCount = personnelChanges.getNumRows();
		for (int iLoop = 1; iLoop<=codeChangesCount;iLoop++){
			String referenceField = "";
			String projectNameField = "";
			
			int personalID = personnelChanges.getInt(SupportChangeAuditConstants.COL_OBJECT_ID, iLoop) ; // ol_object_id
			int versionID = personnelChanges.getInt(SupportChangeAuditConstants.COL_CHANGE_VERSION , iLoop);
			int foundFirstPersonnelRow = personnelAnalysis.findInt("id_number", personalID, SEARCH_ENUM.FIRST_IN_GROUP);
			int foundLastPersonnelRow = personnelAnalysis.findInt("id_number", personalID, SEARCH_ENUM.LAST_IN_GROUP);
			boolean foundRow = false;
			int jLoop = 0;
			String getHeaderChange = "";
			String getLicenceChange = "";
			String getSecChange = "";
			String getFunctionalChange = "";

			if (foundFirstPersonnelRow<0){
				projectNameField = "Underived Changes";
				referenceField = "Underived Changes";
			} else {
				for (jLoop = foundFirstPersonnelRow; jLoop<=foundLastPersonnelRow;jLoop++){
					int foundVersionNum = personnelAnalysis.getInt("per_personnel_version", jLoop);
					if (foundVersionNum==versionID){
						foundRow = true;
						break;
					}
				}
				if (!foundRow){
					jLoop = foundLastPersonnelRow;
				}
				getHeaderChange = personnelAnalysis.getString("personnel_diff", jLoop);
				getLicenceChange = personnelAnalysis.getString("licence_diff", jLoop);
				getSecChange = personnelAnalysis.getString("security_diff", jLoop);
				getFunctionalChange = personnelAnalysis.getString("functional_diff", jLoop);

			}
			
			
			if (getHeaderChange.trim().length()>0){
				referenceField = referenceField + "Header Change";
				projectNameField = projectNameField + getHeaderChange;
			}
			if (getLicenceChange.trim().length()>0){
				if (referenceField.length()>0){
					referenceField = referenceField + " + Licence Change";
				} else {
					referenceField = referenceField + "Licence Change";
				}
				projectNameField = projectNameField + getLicenceChange;
			}
			if (getSecChange.trim().length()>0){
				if (referenceField.length()>0){
					referenceField = referenceField + " + Security Change";
				} else {
					referenceField = referenceField + "Security Change";
				}
				projectNameField = projectNameField + getSecChange;
			}
			if (getFunctionalChange.trim().length()>0){
				if (referenceField.length()>0){
					referenceField = referenceField + " + Functional Change";
				} else {
					referenceField = referenceField + "Functional Change";
				}
				projectNameField = projectNameField + getFunctionalChange;
			}
 
			personnelChanges.setString(SupportChangeAuditConstants.COL_PROJECT_NAME, iLoop ,projectNameField);
			personnelChanges.setString(SupportChangeAuditConstants.COL_OBJECT_REFERENCE, iLoop ,referenceField);
			
			
			
		}
		
	}

	
	private String getPackageName(int parentNodeID, Table directoryNodeDir, boolean codeChange) throws OException {

		String nodeName = "";
		int findRow = directoryNodeDir.findInt("node_id", parentNodeID , SEARCH_ENUM.FIRST_IN_GROUP);
		if (findRow>0){
			int nextParentNodeID = directoryNodeDir.getInt("parent_node_id", findRow);
			if (nextParentNodeID ==0 || nextParentNodeID == NODE_DIR_TOP_LEVEL_CODE || nextParentNodeID == NODE_DIR_TOP_LEVEL_DMS || nextParentNodeID == 12 ){
				nodeName = directoryNodeDir.getString("node_name", findRow);
			} else {
				nodeName = directoryNodeDir.getString("node_name", findRow);
				String getNextNodeName = getPackageName(nextParentNodeID, directoryNodeDir , codeChange);
				if (getNextNodeName.length()>0){
					nodeName = getNextNodeName + "." + nodeName ;
				}
			}
		}
		return nodeName;
	}


	private Table getDirectoryNodeDir(Table directoryNodeDir) throws OException {
		String sql = "SELECT * FROM dir_node\n" +
				 	 " WHERE node_type= 6\n" +
				 	 " ORDER BY node_id";
	
		DBaseTable.execISql(directoryNodeDir, sql);
		
		return directoryNodeDir;
	}


	



	/**
	 * @param returnt - Table to return to report
	 * @throws OException - Error, cannot format table
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
			ReportBuilderUtils.addColumnToMetaData(tableCreate, column.getColumn(), iColCountStr + "_" + column.getTitle(), column.getNameType(), column.getColumnCaption());
			iColCount ++;
		}
	}

	
}
