package com.olf.jm.operation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractFieldListener;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.calendar.HolidaySchedule;
import com.olf.openrisk.calendar.HolidaySchedules;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumResetDefinitionFieldId;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

@ScriptCategory({ EnumScriptCategory.OpsSvcTranfield })
public class ResetHolScheduleDefaulting extends AbstractFieldListener {

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "OpsService";
	
	/** The Constant SUBCONTEXT used to identify entries in the const repository.. */
	public static final String SUBCONTEXT = "ResetHolScheduleDefaulting";
	
	private static final String FLOAT = "Float"; //TODO -- delete
	
	protected static final String MSG_GUI_WARNING_PREFIX = "Reset Holiday Schedule handling";
	
	
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception the exception
	 */
	private void init() throws OException {
		constRep = new ConstRepository(CONTEXT, SUBCONTEXT);

		String logLevel = "Info";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {

			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			if (logDir == null) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			throw new OException("Error initialising logging. " + e.getMessage());
		}

	}
	
	@Override
	public void postProcess(Session session, Field field, String oldValue, String newValue, Table clientData) {
		HashMap<String, List<String>> userConfig = null;
		try {
			init();
			Transaction tran = field.getTransaction();
			Boolean isMetalSwap = tran.getField(EnumTransactionFieldId.InstrumentType).getValueAsInt() == ((EnumInsType.MetalSwap).getValue());
			
			if(isMetalSwap){
				 userConfig = populateUserConfig(session);
				
				CalendarFactory cf = session.getCalendarFactory();
				for (Leg leg : tran.getLegs()) {
					//reset holiday schedule needs to be set for floating leg only.
					//TODO -- leg.getFieldName(EnumLegFieldId.FixFloat)
					//SHM_USR_TABLES_ENUM.LEG_TYPE_TABLE
					
					 if(leg.getLegLabel().contains(FLOAT)){
						String refSource = leg.getResetDefinition().getField(EnumResetDefinitionFieldId.ReferenceSource).getValueAsString();
						 if(!userConfig.containsKey(refSource)){
							 String message = "No Reset Holiday Schedule defined for reference source " + refSource +" in  USER_jm_price_web_ref_source_hol";
							 showMesage(message);
							PluginLog.error(message);
							throw new OException (message);
						 }
						HolidaySchedules holSchdeules =  cf.createHolidaySchedules();
						List <String> holidaySchList = userConfig.get(refSource);
						
						for(String holScheduleName : holidaySchList){
							 PluginLog.info("Holiday Schedules defined for ref source " + refSource + " is " + holScheduleName);
							 // Retrieve a holiday schedule
							 HolidaySchedule holSch = cf.getHolidaySchedule(holScheduleName);
							 //add holiday schedule to the schedule list.
							 holSchdeules.addSchedule(holSch);	
						}
						//set the reset holiday schedule for current leg
						leg.getResetDefinition().setValue(EnumResetDefinitionFieldId.HolidayList, holSchdeules);
					}
				}
			} else {
				PluginLog.info("Deal being processed is not a Metal Swap, Default Reset Holiday Schedule is set for Metal Swaps only");
				
			}
		} catch (Exception e) {
			String errorMessage = "Error while setting Reset Holiday schedule " + e.getMessage();
			PluginLog.error(errorMessage);

			throw new RuntimeException(errorMessage);
		}
		
	}
	
	/**
	 * populateUserConfig.
	 * This Method creates aquery result table for all the indexes contained in 
	 * userHistPriceConfig table. The indexes in userHistPriceConfig are in string format
	 * and has to be converted to Ids before inserting them to query table. 
	 * 
	 * @param session Session Object
	 * @return Table - table containing refference source reset holiday schedule mapping.
	 * @throws OException 
	 */
	private HashMap<String, List<String>> populateUserConfig(Session session) throws OException 
	{
		Table refSrcConfig = null;
		HashMap<String, List<String>> refSrcHolSchMap = new HashMap<String, List<String>>();
		try{
			String SQL = "SELECT * " + " FROM USER_jm_price_web_ref_source_hol";
			IOFactory iof = session.getIOFactory();
			PluginLog.info("\n About to run SQL - " + SQL);
			refSrcConfig = iof.runSQL(SQL);
			int rowCount = refSrcConfig.getRowCount();
			PluginLog.info("\n Number of Rows returned from USER_jm_price_web_ref_source_hol Table "+ rowCount);
			if(rowCount <= 0){
				String message = "No Ref Source/Reset Holiday Schedule Mappings defined in USER_jm_price_web_ref_source_hol";
				PluginLog.error(message);
				showMesage(message);
				throw new OException (message);
			}
			for(int row = 0; row <rowCount; row++){		
				int refSourceId = refSrcConfig.getInt("ref_source", row);
				String refSource = Ref.getName(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, refSourceId );
				int holId = refSrcConfig.getInt("holiday_id", row);
				String holSchName = Ref.getName(SHM_USR_TABLES_ENUM.HOL_ID_TABLE, holId );
				if(refSrcHolSchMap.containsKey(refSource)){				
					refSrcHolSchMap.get(refSource).add(holSchName);
				}else{
					List<String> resetHolSchList = new ArrayList<String> (); 
					resetHolSchList.add(holSchName);
					refSrcHolSchMap.put(refSource,resetHolSchList );
				}
			}

		}catch(Exception exp){
			PluginLog.error("Failed in populateUserConfig method " + exp.getMessage());
			throw new OException(exp.getCause());
		}finally{
			if(refSrcConfig != null)
				refSrcConfig.dispose();
		}
		return refSrcHolSchMap;

	}
	
	private void showMesage(String message) throws OException{
		if(com.olf.openjvs.Util.canAccessGui() == 1)
			com.olf.openjvs.Ask.ok(MSG_GUI_WARNING_PREFIX + ": " + message); // It does works in In-Process Post-proc

	}
}