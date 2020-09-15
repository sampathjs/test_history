package com.olf.jm.storageDealManagement;

import com.olf.openjvs.Ask;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openjvs.enums.ASK_TEXT_DATA_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.fnd.OCalendarBase;
import com.openlink.util.constrepository.ConstRepository;
import  com.olf.jm.logging.Logging;

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class StorageDealManagementParam_01 implements IScript {
	
	private static final String COL_NAME_PROCESS_DATE = "process_date";
	private static final String COL_NAME_TARGET_MAT_DATE = "target_mat_date";
	private static final String COL_NAME_LOCAL_DATE = "local_date";
	private static final String COL_NAME_LOCATION = "location";
	private static final String COL_NAME_METAL = "metal";

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** The Constant CONTEXT used to identify entries in the const repository. */
	private static final String CONTEXT = "StorageDealManagement";
	
	@Override
	public void execute(IContainerContext context) throws OException {
		Table locationsList = Util.NULL_TABLE;		
		Table metalsList = Util.NULL_TABLE;
		Table defaultLocationList = Util.NULL_TABLE;
		Table defaultMetalsList = Util.NULL_TABLE;
		Table defaultValues = Util.NULL_TABLE;
		Table date_table  = Util.NULL_TABLE;
		try {
			
			init();
			Logging.info("Processing storage dates Param Script: " );
			
			
			Table argt = context.getArgumentsTable();
			locationsList = getLocations();
			metalsList = getMetals();
			defaultValues = getDefaultValues();
			String defaultDealDuration = defaultValues.getString("deal_duration", 1);
			String defaultMetal = defaultValues.getString(COL_NAME_METAL, 1);
			String defaultLocation = defaultValues.getString(COL_NAME_LOCATION, 1);
			
			
			argt.addCol(COL_NAME_PROCESS_DATE, COL_TYPE_ENUM.COL_DATE_TIME);
			argt.addCol(COL_NAME_TARGET_MAT_DATE, COL_TYPE_ENUM.COL_DATE_TIME);
			argt.addCol(COL_NAME_LOCAL_DATE, COL_TYPE_ENUM.COL_DATE_TIME);
			argt.addCol(COL_NAME_LOCATION, COL_TYPE_ENUM.COL_STRING);
			argt.addCol(COL_NAME_METAL, COL_TYPE_ENUM.COL_STRING);
			
			if(argt.getNumRows() < 1) {
				argt.addRow();
			}
		    date_table = Table.tableNew();
		    date_table.addCol("symb_str", COL_TYPE_ENUM.COL_STRING);
		    date_table.addCol("date_str", COL_TYPE_ENUM.COL_STRING);
		    date_table.addCol("jd", COL_TYPE_ENUM.COL_INT);
		    date_table.addRow();
		    date_table.setString("symb_str",1, "1fom");
		    OCalendarBase.parseSymbolicDates(date_table, "symb_str", "date_str", "jd", OCalendar.today());  //OCalendar.getServerDate()
		    int currentMatDate = date_table.getInt("jd", 1);
		    if(OCalendar.getSOM(OCalendar.today())== OCalendar.today()){
		    	currentMatDate = OCalendar.today();
		    }
		    date_table.setString("symb_str",1, defaultDealDuration);
			OCalendarBase.parseSymbolicDates(date_table, "symb_str", "date_str", "jd", currentMatDate);
			int defaultEndMatDate = date_table.getInt("jd", 1);
			
			
			if (Util.canAccessGui() == 1) {
				Logging.info("Storage Deal Gui Mode:" );
				// GUI access prompt the user for the process date to run for
				Table tAsk = Table.tableNew ("Storage Deal Management");
				 // Convert the found symbolic date to a julian day.
				Ask.setTextEdit (tAsk ,"Current Maturity Date" ,OCalendar.formatDateInt (currentMatDate) ,ASK_TEXT_DATA_TYPES.ASK_DATE ,"Please select current maturity date" ,1);
				Ask.setTextEdit (tAsk ,"New Maturity Date" ,OCalendar.formatDateInt (defaultEndMatDate) ,ASK_TEXT_DATA_TYPES.ASK_DATE ,"Please select new maturity date on any storage deals created" ,1);
				
				
				defaultLocationList = getDefaultTable(locationsList, defaultLocation);
				Ask.setAvsTable(tAsk , locationsList.copyTable(), "Select Location: " , 1, ASK_SELECT_TYPES.ASK_MULTI_SELECT.toInt(), 1, defaultLocationList, "Select Location to run for");			
				
				defaultMetalsList = getDefaultTable(metalsList, defaultMetal);
				Ask.setAvsTable(tAsk , metalsList.copyTable(), "Select Metals: " , 1, ASK_SELECT_TYPES.ASK_MULTI_SELECT.toInt(), 1, defaultMetalsList, "Select Metals to run for");			
				
				/* Get User to select parameters */
				String opsServiceMessage = "PLEASE turn off the following Ops Services: LIMS Helper\n" +
											"LIMS: Nom Booking V2, LIMS: Nom Booking Dispatch Check\n" + 
											"Receipt Workflow - Receipt Deal Creation\n" +
											"Generate: Dispatch Documents, Block Transfers from and to allocated";
				if(Ask.viewTable (tAsk,"Storage Deal Management",opsServiceMessage) == 0) {
					String errorMessages = "The Adhoc Ask has been cancelled.";
					Ask.ok ( errorMessages );
					Logging.info(errorMessages );

					tAsk.destroy();
					throw new OException( "User Clicked Cancel" );
				}

				/* Verify Start and End Dates */
				int processDate = OCalendar.parseString (tAsk.getTable( "return_value", 1).getString("return_value", 1));
				int tatgetMatDate = OCalendar.parseString (tAsk.getTable( "return_value", 2).getString("return_value", 1));
				Table locationRetTable = tAsk.getTable( "return_value", 3);
				String location = "";
				int locationRetTableCount = locationRetTable.getNumRows();
				if (locationRetTableCount>0){					
					for (int iLoop = 1; iLoop<=locationRetTableCount;iLoop++){
						String tempLocation = "'" + locationRetTable.getString("return_val", iLoop) +"'";
						if (iLoop==1){
							location = tempLocation;
							Logging.info("Locations Selected:" +  locationRetTable.getString("ted_str_value", iLoop)  );
						} else {
							location = location + "," + tempLocation;
						}
					}					
				} else {
					location = "*";
				}
				Table metalsRetTable = tAsk.getTable( "return_value", 4);
				String metal = "";
				int metalRetTableCount = metalsRetTable.getNumRows();
				if (metalRetTableCount>0){					
					for (int iLoop = 1; iLoop<=metalRetTableCount;iLoop++){
						String tempMetal = "'" + metalsRetTable.getString("return_val", iLoop) +"'";
						if (iLoop==1){
							metal = tempMetal;
							Logging.info("Metals Selected:" +  metalsRetTable.getString("ted_str_value", iLoop)  );
						} else {
							metal = metal + "," + tempMetal;
						}
					}					
				} else {
					metal = "*";
				}
				
				
				argt.setDateTime(COL_NAME_PROCESS_DATE, 1, new ODateTime(processDate));
				argt.setDateTime(COL_NAME_TARGET_MAT_DATE, 1, new ODateTime(tatgetMatDate));
				argt.setDateTime(COL_NAME_LOCAL_DATE, 1, new ODateTime(OCalendar.today()));				
				argt.setString(COL_NAME_LOCATION, 1, location);
				argt.setString(COL_NAME_METAL, 1, metal);
				
				tAsk.destroy();
				
				if (processDate!=OCalendar.today()){
//					Ask.ok("Your current processing date is going to be changed to " + OCalendar.formatDateInt(processDate, DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH) + "\n" +
//							" Once processing has completed you will need to revert to the correct day " + OCalendar.formatDateInt(OCalendar.getServerDate(),DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH) + "\n" +
//							" Please refrain from running other processes until completion (apart from other storage roll processes)");	
					
					//Util.setCurrentDate(processDate);
				}
				
				Logging.info("Storage Deal Gui Mode: Finished" );
				
			} else {
				Logging.info("Storage Deal Non Gui Mode: Setting to defaults" );
				
				// no gui so default to the current EOD date. 
				argt.setDateTime(COL_NAME_PROCESS_DATE, 1, new ODateTime(currentMatDate));
				argt.setDateTime(COL_NAME_TARGET_MAT_DATE, 1, new ODateTime(defaultEndMatDate));
				argt.setDateTime(COL_NAME_LOCAL_DATE, 1, new ODateTime(OCalendar.getServerDate()));
				argt.setString(COL_NAME_LOCATION, 1, defaultLocation);
				argt.setString(COL_NAME_METAL, 1, defaultMetal);
			}
		} catch (Exception e) {
			
			e.printStackTrace();
			String msg = e.getMessage();
			throw new OException(msg);
		} finally {
			Logging.close();
			if (Table.isTableValid(locationsList)!=0){
				locationsList.destroy();
			}
			if (Table.isTableValid(metalsList)!=0){
				metalsList.destroy();
			}
			if (Table.isTableValid(defaultMetalsList)!=0){
				defaultMetalsList.destroy();
			}
			if (Table.isTableValid(defaultLocationList)!=0){
				defaultLocationList.destroy();
			}
			if (Table.isTableValid(date_table)!=0){
				date_table.destroy();
			}
			if (Table.isTableValid(defaultValues)!=0){
				defaultValues.destroy();
			}
			
		}
		
		
	}
	
	private Table getLocations() throws OException {
		Table locationList = Table.tableNew("Location List");
		String sql = "SELECT f.facility_name 'Location', gl.name 'Region' FROM facility f\n" + 
					 " JOIN geographic_locations gl ON (gl.geo_loc_id = f.geo_loc_id)\n" + 
					 " ORDER BY gl.name, facility_name"; 

		DBaseTable.execISql(locationList, sql);
		int row = locationList.addRow();
		locationList.setString(1, row, "*");
		locationList.setString(2, row, "N/A");
		return locationList;
	}

	private Table getMetals() throws OException {
		Table metalsList = Table.tableNew("Metal List");
		String sql = "SELECT isg.name 'Metal Name', isg.code 'Metal Code' FROM idx_subgroup isg WHERE LEN(isg.code)>0 ORDER BY isg.name"; 

		DBaseTable.execISql(metalsList, sql);
		int row = metalsList.addRow();
		metalsList.setString(1, row, "*");
		metalsList.setString(2, row, "N/A");
		return metalsList;
	}
	private Table getDefaultValues() throws OException {
		Table defaultValues = Table.tableNew("DefaultValues");
		String sql = "SELECT * FROM USER_jm_comm_stor_mgmt WHERE system_default = 1"; 

		DBaseTable.execISql(defaultValues, sql);
		return defaultValues;
	}	
	private Table getDefaultTable(Table coreTable, String preferedDefaultValue) throws OException {
		Table retTable = Table.tableNew();
		retTable = coreTable.cloneTable();
		int foundVal = coreTable.unsortedFindString(1, preferedDefaultValue	, SEARCH_CASE_ENUM.CASE_INSENSITIVE	);
		if (foundVal<=0){
			foundVal = coreTable.unsortedFindString(2, preferedDefaultValue	, SEARCH_CASE_ENUM.CASE_INSENSITIVE	);
		}
		if (foundVal<=0){
			foundVal=coreTable.getNumRows();
		}
		coreTable.copyRowAdd(foundVal, retTable);
		return retTable;
	}

	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception the exception
	 */
	private void init() throws Exception {
		constRep = new ConstRepository(CONTEXT);

		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			Logging.init(this.getClass(),constRep.getContext(),constRep.getSubcontext());
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}

	}	

}
