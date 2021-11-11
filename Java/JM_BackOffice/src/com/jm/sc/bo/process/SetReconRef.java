package com.jm.sc.bo.process;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.Table;
import com.olf.openjvs.OException;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.enums.*;
import com.olf.jm.logging.Logging;

/* History:
 * 2021-04-01	V1.0	dnagy	- Initial Version
 * Sets the Recon Ref event info field to mark past receivables reconciliation status
 * Part of the Payments and Statements Automation Project Phase 1
 */
	
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_AUTOMATCH_ACTIONS)
public class SetReconRef implements IScript {
	
	public void execute(IContainerContext context) throws OException {
	
		Table amr_action_data = Table.tableNew();
		Table t = Table.tableNew();
		String value, value_save;
		long actual_event_num = 0L;
		int row, ret;
		
		try {
			Logging.init(this.getClass(), "AutoMatch", "SetReconRef");
			Logging.info("Starting " + getClass().getSimpleName());

			Table argt = context.getArgumentsTable();
			amr_action_data = argt.getTable("amr_action_data", 1);
			//argt.viewTable();
			
			for (int i=1; i <= amr_action_data.getNumRows(); i++) {
				value = amr_action_data.getString("recon_ref", i);
				if ( ! "Yes".equals(value) ) { value = "No"; }  // any input other than "Yes" means "No" (Recon_Ref event info is a Yes-No picklist 
				actual_event_num = amr_action_data.getInt64("event_num", i);
				t = Transaction.loadEventInfo(actual_event_num);
				row = t.unsortedFindString("type_name", "Recon_Ref", SEARCH_CASE_ENUM.CASE_SENSITIVE);
				value_save = t.getString("value",  row);
				if (!value.equals(value_save)) {
					t.setString("value", row, value);
					Logging.info("Info " + "Setting Recon_Ref flag for event " + actual_event_num + " from " + value_save + " to " + value);
					ret = Transaction.saveEventInfo(actual_event_num, t);
				}
			}
	        
		} 
		catch (Exception e) {
			String message = "Exception caught:" + e.getMessage();
			Logging.info(message);
			for (StackTraceElement ste : e.getStackTrace() )  {
				Logging.error(ste.toString(), e);
			}
		} finally {
			Logging.info("End " + getClass().getSimpleName());
			Logging.close();
		}
		
	}
	
}
