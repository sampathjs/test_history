package com.jm.metalsalert;
/*******************************************************************************
 * Script File Name: MetalsAlert
 * 
 * Description: This class is an interface for MetalsAlertFactory instantiated object 
 * methods
 * 
 * Revision History:
 * 
 * Date         Developer         Comments
 * ----------   --------------    ------------------------------------------------
 * 15-Mar-21    Makarand Lele	  Initial Version.
 *******************************************************************************/ 
import com.olf.embedded.application.ScriptCategory;
import com.olf.openrisk.table.Table;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
@ScriptCategory({ EnumScriptCategory.Generic })
public interface MetalsAlert {

	public void MonitorAndRaiseAlerts(Context context, Table taskParams, int reportDate, String alertType, String unit);
	
	
}
