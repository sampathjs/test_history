package com.jm.eod;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.Ask;
import com.olf.openjvs.ODateTime;
import com.olf.openrisk.control.ControlFactory;
import com.olf.openrisk.control.GridScheduler;
import com.olf.openrisk.control.Service;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.embedded.application.ScriptCategory;


import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;

@ScriptCategory({ EnumScriptCategory.Generic })
public class RestartServices_Output extends AbstractGenericScript {

	
	@Override
	public Table execute(Context context, ConstTable table) {
		
		try {

			Logging.init(this.getClass(), "", "");

			Logging.info("START RestartServices_Output");
			
    		ConstRepository constRep = new ConstRepository("EOD", "RestartServices");
    		int intCutOffHour = constRep.getIntValue("CutOffHour");
    		double dblCutOffSecsPastMidnight = 60*60*intCutOffHour;
    		int intNowSecsPastMidnight = ODateTime.getServerCurrentDateTime().getTime();
			
			if(context.hasDisplay()&& intNowSecsPastMidnight > dblCutOffSecsPastMidnight) {
				
				// check if all services are online
				
				Table tblSvcs = context.getTableFactory().fromOpenJvs(constRep.getMultiStringValue("Service"));
				ControlFactory cf = context.getControlFactory();
				
				Service[] svcs = cf.getServices();
				
				boolean blnRestartFailed = false;
				
				for (Service svc : svcs) {
					
					String strCurrSvcName = svc.getName();
					
					int intRowNum = tblSvcs.find(tblSvcs.getColumnId("value"), strCurrSvcName, 0);
					if(intRowNum > 0){
						// check if service is running
						if(!svc.isRunning() && !svc.getStatus().name().equals("Initializing")){
							blnRestartFailed = true;
						}
					}
				}

				if(blnRestartFailed == true){
					Ask.ok("SERVICE RESTART FAILED - PLEASE MANUALLY START OFFLINE SERVICES");
				}
				else{
					Ask.ok("SERVICES RESTARTED - PLEASE CHECK THEY ARE ONLINE");
				}
			}
		} catch (Exception e) {
			Logging.info(e.toString());
		}
		
		Logging.info("END RestartServices_Output");
	
	return null;
	}
	
}