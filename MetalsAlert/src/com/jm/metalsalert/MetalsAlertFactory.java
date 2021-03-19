package com.jm.metalsalert;
/*******************************************************************************
 * Script File Name: MetalsAlertFactory
 * 
 * Description: Instantiates the Metals Balance Alert and Metals Price and Lease rate change classes
 * and executes alert methods 
 * 
 * Revision History:
 * 
 * Date         Developer         Comments
 * ----------   --------------    ------------------------------------------------
 * 15-Mar-21    Makarand Lele	  Initial Version.
 *******************************************************************************/ 
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
@ScriptCategory({ EnumScriptCategory.Generic })
public class MetalsAlertFactory extends AbstractGenericScript {
	public Table execute(Context context, ConstTable Table) {
		
		Table taskParams = context.getTableFactory().createTable();
		Logging.init(this.getClass(), MetalsAlertConst.CONST_REPOSITORY_CONTEXT, 
				MetalsAlertConst.CONST_REPOSITORY_SUBCONTEXT);
		
		try {
			String taskName = context.getTaskName();
			Logging.info("Script " + taskName + " started.");
			//retrieve task parameters from metals alert user table for the task that is being executed
			taskParams = retrieveTaskParams(context, taskName);
			int reportDate = OCalendar.today();
			if (taskParams.getRowCount() > 0) {
				switch (taskName) {
				case	"Metals_Balance_Change_Alert":
					MetalsBalanceAlert mba = new MetalsBalanceAlert();
					mba.MonitorAndRaiseAlerts(context, taskParams, reportDate, MetalsAlertConst.CONST_alertBalance, MetalsAlertConst.CONST_unitBalance);
					break;
				case	"Metals_SpotPrice_Change_Alert":
					MetalsSpotRateAlert msra = new MetalsSpotRateAlert();
					msra.MonitorAndRaiseAlerts(context, taskParams, reportDate,MetalsAlertConst.CONST_alertPrice, MetalsAlertConst.CONST_unitSpotPrice);
					break;
				case	"Metals_LeaseRate_Change_Alert":
					MetalsSpotRateAlert msrb = new MetalsSpotRateAlert();
					msrb.MonitorAndRaiseAlerts(context, taskParams, reportDate, MetalsAlertConst.CONST_alertRate, MetalsAlertConst.CONST_unitLeaseRate);
					break;
				}
			} 
		} catch (Exception e) {
			taskParams.dispose();
			Logging.error("Error occured " + e.getMessage());
			throw new RuntimeException("Error occured " + e.getMessage());
		} 
		taskParams.dispose();
		return null;
		
	}
	private Table retrieveTaskParams(Context context, String taskName) throws Exception {
		
		Table taskParams = context.getTableFactory().createTable();
		StringBuilder strSQL = new StringBuilder();
		strSQL.append("\n SELECT * from " + MetalsAlertConst.USER_jm_metals_email_alert_rule)
			  .append("\n WHERE task_name = '" + taskName + "' and active = '" + MetalsAlertConst.CONST_Y + "'" );
		
		taskParams = context.getIOFactory().runSQL(strSQL.toString());
		return taskParams;
		
	}
}

