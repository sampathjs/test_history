package com.olf.jm.copymetalvaluedate.app;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.embedded.io.AbstractQueryScript;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.calendar.HolidaySchedule;
import com.olf.openrisk.io.QueryResult;
import com.olf.openrisk.staticdata.BusinessUnit;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.table.Table;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;
/*
 * History:
 * 2016-01-15	V1.0	jwaechter	-	Initial Version
 */

/**
 * This is a event query result plugin restricting all events delivered by 
 * @author jwaechter
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.Query })
public class RestrictToMetalValueDate extends AbstractQueryScript {	
	private static final String CONST_REPO_CONTEXT = "BO";
	private static final String CONST_REPO_SUBCONTEXT = "MetalValueDateQuery";
	private static final String INFO_TYPE_METAL_VALUE_DATE = "Metal Value Date";
	
	@Override
	public void execute(Context context, QueryResult queryResult) {
		try {
			init (context);
			process (context, queryResult);
			Logging.info(this.getClass().getName() + " has been executed successfully");			
		} catch (Throwable t) {
			Logging.error("Exception thrown during execution of " + this.getClass().getName() + "\n" + t.toString());
			throw t;
		}finally{
			Logging.close();
		}
	}
	
	private void process(Session session, QueryResult queryResult) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date businessDate = session.getBusinessDate();
		String sql = getSql (session, queryResult);
		Table sqlResult = null;
		try {
			sqlResult = session.getIOFactory().runSQL(sql);
			for (int rowId = sqlResult.getRowCount()-1; rowId >= 0; rowId--) {
				long eventNum = sqlResult.getLong("event_num", rowId);
				int internalBunit = sqlResult.getInt("internal_bunit", rowId);
				String metalTransferDate = sqlResult.getString("metal_transfer_date", rowId);
				BusinessUnit bunit = 
						(BusinessUnit) session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.BusinessUnit, internalBunit);
				HolidaySchedule h = bunit.getHolidaySchedule();	
				Date firstGBD = h.getNextGoodBusinessDay(businessDate);
				Date secondGBD = h.getNextGoodBusinessDay(firstGBD);
				String formattedSecondGBD = sdf.format(secondGBD);
				if (formattedSecondGBD.compareTo(metalTransferDate) <= 0) {
					queryResult.remove(eventNum);
				}
			}
		} finally {
			if (sqlResult != null) {
				sqlResult.dispose();
			}
		}
	}
	
	private String getSql(Session session, QueryResult queryResult) {
		StringBuilder sql = new StringBuilder();
		sql.append("\nSELECT e.event_num, ab.internal_bunit, CAST (ei.value AS DATE) metal_transfer_date");
		sql.append("\nFROM ab_tran_event e");
		sql.append("\nINNER JOIN ").append(queryResult.getDatabaseTableName()).append(" q");
		sql.append("\n  ON q.query_result = e.event_num");
		sql.append("\n  AND q.unique_id = ").append(queryResult.getId());
		sql.append("\nINNER JOIN ab_tran ab");
		sql.append("\n  ON ab.tran_num = e.tran_num");
		sql.append("\nINNER JOIN tran_event_info_types teit");
		sql.append("\n  ON teit.type_name = '").append(INFO_TYPE_METAL_VALUE_DATE).append("'");
		sql.append("\nINNER JOIN ab_tran_event_info ei");
		sql.append("\n   ON ei.event_num = e.event_num");
		sql.append("\n   AND ei.type_id = teit.type_id");
		
		return sql.toString();
	}

	/**
	 * Initializes the plugin by retrieving the constants repository values
	 * and initializing Logging.
	 * @param session
	 * @return
	 */
	private void init(final Session session) {
		String abOutdir = session.getSystemSetting("AB_OUTDIR"); 
		String logLevel;
		try {
			ConstRepository constRepo = new ConstRepository(CONST_REPO_CONTEXT, 
					CONST_REPO_SUBCONTEXT);
			logLevel = constRepo.getStringValue("logLevel", "info");
			String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
			String logDir = constRepo.getStringValue("logDir", abOutdir);
			Logging.init(session, this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
		}  catch (OException e) {
			throw new RuntimeException(e);
		}  catch (Exception e) {
			throw new RuntimeException(e);
		}
		Logging.info(this.getClass().getName() + " started");		
	}
}
