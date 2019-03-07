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
import static com.jm.reportbuilder.audit.SupportChangeAuditDataLoad.Columns.CHANGE_TYPE;
import static com.jm.reportbuilder.audit.SupportChangeAuditDataLoad.Columns.CHANGE_VERSION;
import static com.jm.reportbuilder.audit.SupportChangeAuditDataLoad.Columns.MODIFIED_DATE;
import static com.jm.reportbuilder.audit.SupportChangeAuditDataLoad.Columns.OBJECT_NAME;
import static com.jm.reportbuilder.audit.SupportChangeAuditDataLoad.Columns.OBJECT_STATUS;
import static com.jm.reportbuilder.audit.SupportChangeAuditDataLoad.Columns.PERSONNEL_FIRSTNAME;
import static com.jm.reportbuilder.audit.SupportChangeAuditDataLoad.Columns.PERSONNEL_ID;
import static com.jm.reportbuilder.audit.SupportChangeAuditDataLoad.Columns.PERSONNEL_LASTNAME;
import static com.jm.reportbuilder.audit.SupportChangeAuditDataLoad.Columns.PERSONNEL_SHORTNAME;
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
@com.olf.openjvs.PluginType(com.olf.openjvs.enums.SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class SupportChangeAuditDataLoad implements IScript {

	

	private static final String FINANCIAL_CHANGE = "Financial Change";
	private static final String DEPLOYMENT_CHANGE = "Deployment Change";
	private static final String ARTIFACT_CHANGE = "Artifact Change";
	
	private static final int FINANCIAL_CHANGE_ID = 1;
	private static final int DEPLOYMENT_CHANGE_ID = 2;
	private static final int ARTIFACT_CHANGE_ID = 3;
	
	private static final String OBJECT_CHANGE_DEAL_CHANGE = "Deal Change";
	private static final String OBJECT_CHANGE_HISTORICAL_CHANGE = "Historical Price";
	private static final String OBJECT_CHANGE_HISTORICAL_FX_CHANGE = "Historical FX Price";
	private static final String OBJECT_CHANGE_MARKET_PRICE = "Market Price";
	
	private static final String OBJECT_CHANGE_QUERY_CHANGE = "Query Change";
	private static final String OBJECT_CHANGE_CODE_CHANGE = "Code Change";
	private static final String OBJECT_CHANGE_PROCESS_CHANGE = "Other Change";
	private static final String OBJECT_CHANGE_OPS_SERVICE_CHANGE = "Ops Service Change";
	
	private static final String OBJECT_CHANGE_DMS_CHANGE = "DMS Change";
	private static final String OBJECT_CHANGE_APM_CHANGE = "APM Change";
	private static final String OBJECT_CHANGE_SQL_CHANGE = "Datasource Change";
	
	
	private static final String NODE_TYPE_LIST = "2, 15, 17, 19";		// 2 - Tasks, 15-Report Builder 17- auto match, 19 - Connex


	protected enum Columns {
		//SupportChangeAuditConstants
		CHANGE_TYPE(COL_CHANGE_TYPE, "Change Type", COL_STRING, "Change Type - Financial or Deployment")		{		},
		OBJECT_TYPE(COL_OBJECT_TYPE, "Object Type", COL_STRING, "Deal/Query/Code")		{		},

		PERSONNEL_ID(COL_PERSONNEL_ID, "Personnel ID", COL_INT, "Personnel ID"){	},
		PERSONNEL_SHORTNAME(COL_PERSONNEL_SHORTNAME, "Short Name", COL_STRING, "Personnel Short Name"){		},
		PERSONNEL_FIRSTNAME(COL_PERSONNEL_FIRSTNAME, "First Name", COL_STRING, "Personnel First Name"){		},
		PERSONNEL_LASTNAME(COL_PERSONNEL_LASTNAME, "Last Name", COL_STRING, "Personnel First Name")		{		},

		OBJECT_ID(COL_OBJECT_ID, "Object ID", COL_INT, "Ojbect ID")		{		},
		OBJECT_NAME(COL_OBJECT_NAME, "Name", COL_STRING, "Object Name/Reference")		{		},
		OBJECT_REFERENCE(COL_OBJECT_REFERENCE, "Reference", COL_STRING, "Category")		{		},
		OBJECT_STATUS(COL_OBJECT_STATUS, "Status", COL_STRING, "Object Status")		{		},
		CHANGE_VERSION(COL_CHANGE_VERSION, "Personnel Version", COL_INT, "Current Personnel Version")		{		},
		PROJECT_NAME(COL_PROJECT_NAME, "Project Name", COL_STRING, "Project Name")		{		},
		MODIFIED_DATE(COL_MODIFIED_DATE, "Modified Date", COL_DATE_TIME, "The Last Time the object was updated")		{		},
		
		EXPLANATION (COL_EXPLANATION , "Explanation", COL_STRING, "Explanation - Filled in later")		{		},

		CHANGE_TYPE_ID(COL_CHANGE_TYPE_ID, "Change Type ID", COL_INT, "Internal Change Type ID value")		{		},
		OBJECT_TYPE_ID(COL_OBJECT_TYPE_ID, "Object Type ID", COL_INT, "Internal Object Type ID value")		{		},


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
	

	protected enum ChangeType {
		
		FINANCIAL_CHANGE_TYPE(FINANCIAL_CHANGE,FINANCIAL_CHANGE_ID)		{		},
		DEPLOYMENT_CHANGE_TYPE(DEPLOYMENT_CHANGE, DEPLOYMENT_CHANGE_ID)		{		},
		ARTIFACT_CHANGE_TYPE(ARTIFACT_CHANGE, ARTIFACT_CHANGE_ID)		{		};
		
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

	
	protected enum ReportType {
		//SupportChangeAuditConstants
		DEAL_CHANGE(FINANCIAL_CHANGE_TYPE, OBJECT_CHANGE_DEAL_CHANGE, 1)		{		},
		HISTORICAL_CHANGE(FINANCIAL_CHANGE_TYPE,  OBJECT_CHANGE_HISTORICAL_CHANGE, 2)		{		},
		HISTORICAL_FX_CHANGE(FINANCIAL_CHANGE_TYPE,  OBJECT_CHANGE_HISTORICAL_FX_CHANGE, 3)		{		},
		MARKET_PRICE_CHANGE(FINANCIAL_CHANGE_TYPE,  OBJECT_CHANGE_MARKET_PRICE, 4)		{		},
		QUERY_CHANGE(DEPLOYMENT_CHANGE_TYPE,  OBJECT_CHANGE_QUERY_CHANGE, 5)		{		},
		CODE_CHANGE(DEPLOYMENT_CHANGE_TYPE,  OBJECT_CHANGE_CODE_CHANGE, 6)		{		},
		PROCESS_CHANGE(DEPLOYMENT_CHANGE_TYPE,  OBJECT_CHANGE_PROCESS_CHANGE, 7)		{		},
		OPS_SERVICE_CHANGE(DEPLOYMENT_CHANGE_TYPE,  OBJECT_CHANGE_OPS_SERVICE_CHANGE, 8)		{		},

		
		DMS_CHANGE(ARTIFACT_CHANGE_TYPE,  OBJECT_CHANGE_DMS_CHANGE, 9)		{		},
		APM_CHANGE(ARTIFACT_CHANGE_TYPE,  OBJECT_CHANGE_APM_CHANGE, 10)		{		},
		SQL_CHANGE(ARTIFACT_CHANGE_TYPE,  OBJECT_CHANGE_SQL_CHANGE, 11)		{		},
		

		
		
		

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
			SupportChangeAuditConstants.initPluginLog(constRep); //Plug in Log init

			
			// PluginLog.init("INFO");

			PluginLog.info("Start  " + getClass().getSimpleName());

			Table argt = context.getArgumentsTable();
			Table returnt = context.getReturnTable();

			int modeFlag = argt.getInt("ModeFlag", 1);
			PluginLog.debug(getClass().getSimpleName() + " - Started Data Load Script for UserSecAuit Reports - mode: " + modeFlag);

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

		
		Table tblPersonnelData = Util.NULL_TABLE;
		
		int totalRows = 0;
		String sqlCommand;

		PluginLog.debug("Attempt to recover Personnel information.");

		try {

			
			String dateValue = getDateValue();
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
						 "   JOIN ins_sub_type ist ON (ist.id_number=ab.ins_sub_type)\n" +
						 " WHERE qr.unique_id=" + queryID + "\n" +
						 "   AND ab.last_update > '" + dateValue +"'";

			sqlCommand = sqlCommand1;
			
			thisReportType=ReportType.HISTORICAL_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
					 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
					 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
					 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
					 "   id.index_id " + Columns.OBJECT_ID.getColumn() + ",id.index_name + ' - ' + iit.name + ' - ' + ids.name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
					 "    'Reset: ' + CONVERT(varchar, ihp.reset_date, 103)  + ' - Start: ' + CONVERT(varchar, ihp.start_date, 103) + ' - End: ' + CONVERT(varchar, ihp.end_date, 103) " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
					 "   yb.name " + Columns.OBJECT_STATUS.getColumn() + ", 0 " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
					 "   ihp.last_update " + Columns.MODIFIED_DATE.getColumn() + ",rs.name " + Columns.PROJECT_NAME.getColumn() + "\n" +
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
						  "   ihfr.last_update " + Columns.MODIFIED_DATE.getColumn() + ",id.index_name " + Columns.PROJECT_NAME.getColumn() + "\n" +
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
			
			
			thisReportType=ReportType.QUERY_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
					 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
					 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
					 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
					 "   qv.query_id " + Columns.OBJECT_ID.getColumn() + ",qv.query_name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
					 "   qv.query_group_name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
					 "    'QueryChange: N/A' " + Columns.OBJECT_STATUS.getColumn() + ", qv.query_version " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
					 "   qv.last_update " + Columns.MODIFIED_DATE.getColumn() + ",dqg.dxr_query_group_name " + Columns.PROJECT_NAME.getColumn() + "\n" +
					 " FROM query_view qv \n" +
					 "   JOIN personnel p ON (p.id_number=qv.personnel_id)\n" +
					 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
					 "   LEFT JOIN dxr_query_group dqg ON (dqg.query_group_id=qv.query_group_id)\n" +
					 " WHERE qr.unique_id=" + queryID + "\n" +
					 "   AND qv.last_update > '" + dateValue +"'";
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;

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
//			 "   JOIN service_general_status sgs ON (sgs.status_id=red.defn_status)\n" +

			
			
			thisReportType=ReportType.CODE_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
					 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
					 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
					 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
					 "   dn.node_id " + Columns.OBJECT_ID.getColumn() + ", dn.node_name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
					 "    'Node Type: ' + dnt.name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
					 "    'User Permmision: ' + dsp_up.name + ' - Group Permmision: ' + dsp_gp.name + ' - Public Permmision: ' + dsp_pp.name " + Columns.OBJECT_STATUS.getColumn() + ", dn.parent_node_id " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
					 "   dn.last_update " + Columns.MODIFIED_DATE.getColumn() + ",dn.node_name " + Columns.PROJECT_NAME.getColumn() + "\n" +
					 " FROM dir_node dn \n" +
					 "   JOIN personnel p ON (p.id_number=dn.update_user_id)\n" +
					 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
					 "   LEFT JOIN dir_node_type dnt ON (dnt.id_number=dn.node_type)\n" +
					 "   JOIN dir_security_permiss dsp_up ON (dsp_up.id_number=dn.user_permiss)\n" +
					 "   JOIN dir_security_permiss dsp_gp ON (dsp_gp.id_number=dn.group_permiss)\n" +
					 "   JOIN dir_security_permiss dsp_pp ON (dsp_pp.id_number=dn.public_permiss)\n" +
					 " WHERE qr.unique_id=" + queryID + "\n" +
					 "   AND dn.last_update > '" + dateValue +"'\n" + 
					 "   AND dn.node_type = 7";  // Code
			
	
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;
			
			thisReportType=ReportType.PROCESS_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
					 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
					 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
					 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
					 "   dn.node_id " + Columns.OBJECT_ID.getColumn() + ", dn.node_name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
					 "    'Node Type: ' + dnt.name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
					 "    'User Permmision: ' + dsp_up.name + ' - Group Permmision: ' + dsp_gp.name + ' - Public Permmision: ' + dsp_pp.name " + Columns.OBJECT_STATUS.getColumn() + ", dn.parent_node_id " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
					 "   dn.last_update " + Columns.MODIFIED_DATE.getColumn() + ",dn.node_name " + Columns.PROJECT_NAME.getColumn() + "\n" +
					 " FROM dir_node dn \n" +
					 "   JOIN personnel p ON (p.id_number=dn.update_user_id)\n" +
					 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
					 "   LEFT JOIN dir_node_type dnt ON (dnt.id_number=dn.node_type)\n" +
					 "   JOIN dir_security_permiss dsp_up ON (dsp_up.id_number=dn.user_permiss)\n" +
					 "   JOIN dir_security_permiss dsp_gp ON (dsp_gp.id_number=dn.group_permiss)\n" +
					 "   JOIN dir_security_permiss dsp_pp ON (dsp_pp.id_number=dn.public_permiss)\n" +
					 " WHERE qr.unique_id=" + queryID + "\n" +
					 "   AND dn.last_update > '" + dateValue +"'\n" + 
					 "   AND dn.node_type IN (" + NODE_TYPE_LIST + ")";   
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;

			
			thisReportType=ReportType.SQL_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
					 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
					 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
					 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
					 "   dn.node_id " + Columns.OBJECT_ID.getColumn() + ", dn.node_name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
					 "    'Node Type: ' + dnt.name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
					 "    'User Permmision: ' + dsp_up.name + ' - Group Permmision: ' + dsp_gp.name + ' - Public Permmision: ' + dsp_pp.name " + Columns.OBJECT_STATUS.getColumn() + ", dn.parent_node_id " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
					 "   dn.last_update " + Columns.MODIFIED_DATE.getColumn() + ",dn.node_name " + Columns.PROJECT_NAME.getColumn() + "\n" +
					 " FROM dir_node dn \n" +
					 "   JOIN personnel p ON (p.id_number=dn.update_user_id)\n" +
					 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
					 "   LEFT JOIN dir_node_type dnt ON (dnt.id_number=dn.node_type)\n" +
					 "   JOIN dir_security_permiss dsp_up ON (dsp_up.id_number=dn.user_permiss)\n" +
					 "   JOIN dir_security_permiss dsp_gp ON (dsp_gp.id_number=dn.group_permiss)\n" +
					 "   JOIN dir_security_permiss dsp_pp ON (dsp_pp.id_number=dn.public_permiss)\n" +
					 " WHERE qr.unique_id=" + queryID + "\n" +
					 "   AND dn.last_update > '" + dateValue +"'\n" + 
					 "   AND dn.node_type = 9\n" +		// User Defined
					 "   AND dn.parent_node_id = 21588"  ;   //23 
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;

			thisReportType=ReportType.APM_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
					 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
					 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
					 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
					 "   dn.node_id " + Columns.OBJECT_ID.getColumn() + ", dn.node_name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
					 "    'Node Type: ' + dnt.name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
					 "    'User Permmision: ' + dsp_up.name + ' - Group Permmision: ' + dsp_gp.name + ' - Public Permmision: ' + dsp_pp.name " + Columns.OBJECT_STATUS.getColumn() + ", dn.parent_node_id " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
					 "   dn.last_update " + Columns.MODIFIED_DATE.getColumn() + ",dn.node_name " + Columns.PROJECT_NAME.getColumn() + "\n" +
					 " FROM dir_node dn \n" +
					 "   JOIN personnel p ON (p.id_number=dn.update_user_id)\n" +
					 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
					 "   LEFT JOIN dir_node_type dnt ON (dnt.id_number=dn.node_type)\n" +
					 "   JOIN dir_security_permiss dsp_up ON (dsp_up.id_number=dn.user_permiss)\n" +
					 "   JOIN dir_security_permiss dsp_gp ON (dsp_gp.id_number=dn.group_permiss)\n" +
					 "   JOIN dir_security_permiss dsp_pp ON (dsp_pp.id_number=dn.public_permiss)\n" +
					 " WHERE qr.unique_id=" + queryID + "\n" +
					 "   AND dn.last_update > '" + dateValue +"'\n" + 
					 "   AND dn.node_type = 9\n" +		// User Defined
					 "   AND dn.parent_node_id IN (23787,23877)"  ;   //23 
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;

			thisReportType=ReportType.DMS_CHANGE;
			sqlCommand1 = "SELECT '" + thisReportType.getChangeTypeName() + "' " + Columns.CHANGE_TYPE.getColumn() + ", " + thisReportType.getChangeTypeID() + " " + Columns.CHANGE_TYPE_ID.getColumn() + ",\n" +
					 " '" + thisReportType.getObjectTypeName() + "' " + Columns.OBJECT_TYPE.getColumn() + ", " + thisReportType.getObjectTypeID() + " " + Columns.OBJECT_TYPE_ID.getColumn() + ",\n" +
					 "   p.id_number " + Columns.PERSONNEL_ID.getColumn() + " , p.name " + Columns.PERSONNEL_SHORTNAME.getColumn() + ",\n" +
					 "   p.first_name " + Columns.PERSONNEL_FIRSTNAME.getColumn() + ",p.last_name " + Columns.PERSONNEL_LASTNAME.getColumn() + ",\n" +
					 "   dn.node_id " + Columns.OBJECT_ID.getColumn() + ", dn.node_name " + Columns.OBJECT_NAME.getColumn() + ",\n" +
					 "    'Node Type: ' + dnt.name " + Columns.OBJECT_REFERENCE.getColumn() + ",\n" +
					 "    'User Permmision: ' + dsp_up.name + ' - Group Permmision: ' + dsp_gp.name + ' - Public Permmision: ' + dsp_pp.name " + Columns.OBJECT_STATUS.getColumn() + ", dn.parent_node_id " + Columns.CHANGE_VERSION.getColumn() + ",\n" +
					 "   dn.last_update " + Columns.MODIFIED_DATE.getColumn() + ",dn.node_name " + Columns.PROJECT_NAME.getColumn() + "\n" +
					 " FROM dir_node dn \n" +
					 "   JOIN personnel p ON (p.id_number=dn.update_user_id)\n" +
					 "   JOIN " + sQueryTable + " qr ON(p.id_number=CONVERT(INT, qr.query_result))\n" +
					 "   LEFT JOIN dir_node_type dnt ON (dnt.id_number=dn.node_type)\n" +
					 "   JOIN dir_security_permiss dsp_up ON (dsp_up.id_number=dn.user_permiss)\n" +
					 "   JOIN dir_security_permiss dsp_gp ON (dsp_gp.id_number=dn.group_permiss)\n" +
					 "   JOIN dir_security_permiss dsp_pp ON (dsp_pp.id_number=dn.public_permiss)\n" +
					 " WHERE qr.unique_id=" + queryID + "\n" +
					 "   AND dn.last_update > '" + dateValue +"'\n" + 
					 "   AND dn.node_type = 9\n" +		// User Defined
					 "   AND dn.parent_node_id NOT IN (21588,23787,23877, 23)"  ;   //23 
			sqlCommand = sqlCommand + "\n UNION \n" + sqlCommand1;

			tblPersonnelData = Table.tableNew();
			DBaseTable.execISql(tblPersonnelData, sqlCommand);

			
			
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
			PluginLog.debug("Results processing finished. Total Number of results recovered: " + totalRows + " processed: " + tblPersonnelData.getNumRows());

			if (Table.isTableValid(tblPersonnelData) == 1) {
				tblPersonnelData.destroy();
			}
		}
	}



	private String getDateValue() throws OException {
		ODateTime dateValue = ODateTime.dtNew();
//		int dateValue = 0;
		String sql = "SELECT date_value FROM USER_const_repository\n" +
					 " WHERE context='" + SupportChangeAuditConstants.REPO_CONTEXT + "'\n" +
					 " AND sub_context = '" + SupportChangeAuditConstants.REPO_SUB_CONTEXT + "'\n" +
				     " AND name = 'LastRunTime'";
		Table tblPersonnelData = Table.tableNew();

		DBaseTable.execISql(tblPersonnelData, sql);
		
		dateValue = tblPersonnelData.getDateTime("date_value", 1);
		String retValue = dateValue.formatForDbAccess();
		
		tblPersonnelData.destroy();
		return retValue;
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
