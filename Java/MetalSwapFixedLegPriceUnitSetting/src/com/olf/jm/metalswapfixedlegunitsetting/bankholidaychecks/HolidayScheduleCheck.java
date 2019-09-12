package com.olf.jm.bankholidaychecks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.openjvs.OException;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.calendar.HolidaySchedules;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumResetDefinitionFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class HolidayScheduleCheck extends AbstractTradeProcessListener {

	private ConstRepository constRep;

	/** context of constants repository */
	private static final String CONST_REPO_CONTEXT = "OpsService";

	/** sub context of constants repository */
	private static final String CONST_REPO_SUBCONTEXT = "MetalSwapValidations";

	public PreProcessResult preProcess(final Context context, final EnumTranStatus targetStatus, final PreProcessingInfo<EnumTranStatus>[] infoArray,
			final Table clientData) {
		PreProcessResult preProcessResult = PreProcessResult.succeeded();
		Transaction tran = null;
		String missingHolSchedules = "";
		String refSource = "";
		String allowedHolSchedules = "";
		try {
			
			HashMap<String, List<String>> holScheduleConfig = loadHolScheduleConfig(context);
			PluginLog.info("Starting " + getClass().getSimpleName());
			init();
			CalendarFactory cf = context.getCalendarFactory();
			for (PreProcessingInfo<EnumTranStatus> ppi : infoArray) {
				
				Transaction newTran = ppi.getTransaction();
				int dealNumber = newTran.getDealTrackingId();
				PluginLog.info("Started processing deal number " + dealNumber);
			

				for (Leg leg : newTran.getLegs()) {
					// reset holiday schedule needs to be set for floating leg
					// only.

					if (leg.getValueAsInt(EnumLegFieldId.FixFloat) == (com.olf.openrisk.trading.EnumFixedFloat.FloatRate.getValue())) {
						refSource = leg.getResetDefinition().getField(EnumResetDefinitionFieldId.ReferenceSource).getValueAsString();
						if (!holScheduleConfig.containsKey(refSource)) {
							String message = "No Reset Holiday Schedule defined for reference source " + refSource + " in  USER_jm_price_web_ref_source_hol.";
							PluginLog.error(message);
						}else{
							List<String> holidaySchList = holScheduleConfig.get(refSource);
							allowedHolSchedules = holidaySchList.toString();
							HolidaySchedules holSchdeules =  leg.getResetDefinition().getField(EnumResetDefinitionFieldId.HolidayList).getValueAsHolidaySchedules();
							int scheduleCount = holSchdeules.getCount();
							for(int i =0; i < scheduleCount; i++){
								String holSchOnDeal = holSchdeules.getSchedule(i).getName();
								if(!holidaySchList.contains(holSchOnDeal)){
									missingHolSchedules = missingHolSchedules + holSchOnDeal + "\n";
								}
							}

						}
						

					}
				}
				
			if(missingHolSchedules!= null && !missingHolSchedules.isEmpty()){
				preProcessResult = PreProcessResult.failed(String.format("\u2022 Holiday schedule(s) that can be selected for the selected Reference Source '%s' are: \n %s \n\n Current selected Holiday Schedule(s): %s ", refSource, allowedHolSchedules, missingHolSchedules), true);
			}
				
				

			}
		} catch (Exception e) {
			String message = "Exception caught:" + e.getMessage();
			PluginLog.error(message);
			preProcessResult = PreProcessResult.failed(message, false);
		}finally{
			if(tran != null){
				tran.dispose();
			}
		}

		PluginLog.info("End " + getClass().getSimpleName());
		return preProcessResult;
	}

	/**
	 * populateUserConfig. This Method creates aquery result table for all the
	 * indexes contained in userHistPriceConfig table. The indexes in
	 * userHistPriceConfig are in string format and has to be converted to Ids
	 * before inserting them to query table.
	 * 
	 * @param session
	 *            Session Object
	 * @return Table - table containing refference source reset holiday schedule
	 *         mapping.
	 * @throws OException
	 */
	private HashMap<String, List<String>> loadHolScheduleConfig(Context context) throws OException {
		Table refSrcConfig = null;
		HashMap<String, List<String>> refSrcHolSchMap = new HashMap<String, List<String>>();
		try {
			String SQL = "SELECT * " + " FROM USER_jm_price_web_ref_source_hol";
			IOFactory iof = context.getIOFactory();
			
			PluginLog.info("\n About to run SQL - " + SQL);
			refSrcConfig = iof.runSQL(SQL);
			int rowCount = refSrcConfig.getRowCount();
			PluginLog.info("\n Number of Rows returned from USER_jm_price_web_ref_source_hol Table " + rowCount);
			if (rowCount <= 0) {
				String message = "No Ref Source/Reset Holiday Schedule Mappings defined in USER_jm_price_web_ref_source_hol";
				PluginLog.error(message);
				//showMesage(message);
				throw new OException(message);
			}
			for (int row = 0; row < rowCount; row++) {
				int refSourceId = refSrcConfig.getInt("ref_source", row);
				
				String refSource = context.getStaticDataFactory().getReferenceObject(EnumReferenceObject.ReferenceSource, refSourceId).getName();
				int holId = refSrcConfig.getInt("holiday_id", row);
				
				String holSchName = context.getStaticDataFactory().getReferenceObject(EnumReferenceObject.HolidaySchedule, holId).getName();
				if (refSrcHolSchMap.containsKey(refSource)) {
					refSrcHolSchMap.get(refSource).add(holSchName);
				} else {
					List<String> resetHolSchList = new ArrayList<String>();
					resetHolSchList.add(holSchName);
					refSrcHolSchMap.put(refSource, resetHolSchList);
				}
			}

		} catch (Exception exp) {
			PluginLog.error("Failed in populateUserConfig method " + exp.getMessage());
			throw new OException(exp.getCause());
		} finally {
			if (refSrcConfig != null)
				refSrcConfig.dispose();
		}
		return refSrcHolSchMap;

	}

	/**
	 * Initialise logging
	 * 
	 * @throws Exception
	 * 
	 * @throws OException
	 */
	private void init() throws Exception {
		constRep = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);

		String logLevel = "info";
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
			throw new Exception("Error initialising logging. " + e.getMessage());
		}
	}

}