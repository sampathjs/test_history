package com.jm.eod;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.Ask;
import com.olf.openjvs.Util;
import com.olf.openrisk.control.ControlFactory;
import com.olf.openrisk.control.EnumServiceStatus;
import com.olf.openrisk.control.GridCluster;
import com.olf.openrisk.control.GridScheduler;
import com.olf.openrisk.control.Service;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.embedded.application.ScriptCategory;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;

@ScriptCategory({ EnumScriptCategory.Generic })
public class RestartServices extends AbstractGenericScript {

	
	@Override
	public Table execute(Context context, ConstTable args) {
		
		try {

			Logging.init(this.getClass(), "", "");

			Logging.info("START RestartServices");
			
    		if (args.getRowCount() < 1)
    		{
    			throw new Exception(String.format("Missing argt "));
    		}

    		String strRunRestart = args.getString("run_restart", 0);
    		
    		if(strRunRestart.equals("Y")){
    			
    			ControlFactory cf = context.getControlFactory();
    			
    			// SERVICES RESTART
    			
    			//Stop the services
    			toggleService(cf, "Risk","STOP");
    			toggleService(cf, "Credit","STOP");
    			toggleService(cf, "Mail","STOP");
    			toggleService(cf, "Accounting","STOP");
    			toggleService(cf, "ANE","STOP");
    			toggleService(cf, "Maintenance","STOP");
    			toggleService(cf, "Post Process","STOP");
    			toggleService(cf, "Report Builder","STOP");
    			toggleService(cf, "Reval","STOP");
    			toggleService(cf, "APM_UK","STOP");
    			toggleService(cf, "APM_US","STOP");
    			toggleService(cf, "APM_HK","STOP");
    			toggleService(cf, "APM_CN","STOP");
    			toggleService(cf, "Trade Process Mgmt","STOP");
    			toggleService(cf, "APM_Base_Metals","STOP");
    			toggleService(cf, "TPM_Support","STOP");
    			toggleService(cf, "Report Builder_Node9","STOP");
    			//toggleService(cf, "Post Process_Node9","STOP");
    			
    			// Stop the clusters
    			toggleCluster(cf,"DispatchCluster","Grid_Scheduler","STOP");
    			toggleCluster(cf,"JobCluster","Grid_Scheduler","STOP");
    			
    			// Stop the scheduler 
    			toggleScheduler(cf,"Grid_Scheduler","STOP");
    			
    			// Start the scheduler
    			toggleScheduler(cf,"Grid_Scheduler","START");
    			
    			// Start the clusters
    			toggleCluster(cf,"DispatchCluster","Grid_Scheduler","START");
    			toggleCluster(cf,"JobCluster","Grid_Scheduler","START");
    			
    			// Start the services
    			toggleService(cf, "Risk","START");
    			toggleService(cf, "Credit","START");
    			toggleService(cf, "Mail","START");
    			toggleService(cf, "Accounting","START");
    			toggleService(cf, "ANE","START");
    			toggleService(cf, "Maintenance","START");
    			toggleService(cf, "Post Process","START");
    			toggleService(cf, "Report Builder","START");
    			toggleService(cf, "Reval","START");
    			toggleService(cf, "APM_UK","START");
    			toggleService(cf, "APM_US","START");
    			toggleService(cf, "APM_HK","START");
    			toggleService(cf, "APM_CN","START");
    			toggleService(cf, "Trade Process Mgmt","START");
    			toggleService(cf, "APM_Base_Metals","START");
    			toggleService(cf, "TPM_Support","START");
    			toggleService(cf, "Report Builder_Node9","START");
    			
    			//CONNEX SERVICE RESTART
    			
    			toggleService(cf, "WSGateway eJM","STOP");
    			toggleService(cf, "WSGateway SAP","STOP");
    			toggleScheduler(cf,"Connex_Scheduler","STOP");
    			
    			toggleScheduler(cf,"Connex_Scheduler","START");
    			
    			
    			toggleService(cf, "WSGateway eJM","START");
    			toggleService(cf, "WSGateway SAP","START");

    			
    		}else{
    			
    			Logging.info("Pre validation checks failed - exiting.");
    		}
			
			
		} catch (Exception e) {
			Logging.info(e.toString());
		}
		
		Logging.info("END RestartServices");
		
		return null;
	}

	
	private void waitForScheduler(ControlFactory cf, String strScheduler, EnumServiceStatus enumSS ) throws Exception {
		
		long start = System.currentTimeMillis();
		long end = start + 30*1000;
		GridScheduler gs = cf.getGridScheduler(strScheduler);
		while(gs.getStatus() != enumSS && System.currentTimeMillis() < end ){
			Thread.sleep (1000);
		}

		gs = cf.getGridScheduler(strScheduler);
		if(gs.getStatus() != enumSS){
			throw new Exception("Scheduler did not go to " + enumSS.toString() );
		}
	}
	

	private void toggleScheduler(ControlFactory cf, String strSchedulerName, String strStopStart) throws Exception {
	
		GridScheduler gs = cf.getGridScheduler(strSchedulerName);
		
		if(strStopStart.equals("STOP")){

			Logging.info("STOPPING Scheduler " + strSchedulerName + " ");	
			gs.stop();
			Logging.info("STOPPED SchedulerScheduler " + strSchedulerName + " ");
			
		}else if (strStopStart.equals("START")){
			
			// WAIT FOR SCHEDULER TO BE DOWN BEFORE STARTING SCHEDULER
			waitForScheduler( cf, strSchedulerName , EnumServiceStatus.Down) ;
			
			Logging.info("STARTING SchedulerScheduler " + strSchedulerName + " ");
			gs.start();
			Logging.info("STARTED Scheduler " + strSchedulerName + " ");
		}
		
	}
	
	
	private void toggleCluster(ControlFactory cf, String strClusterName, String strSchedulerName, String strStopStart) throws Exception{
		
		GridCluster gc = cf.getGridCluster(strClusterName);
		
		if(strStopStart.equals("STOP")){

			Logging.info("STOPPING Cluster " + strClusterName + " ");	
			gc.stop();
			Logging.info("STOPPED Cluster " + strClusterName + " ");
			
		}else if (strStopStart.equals("START")){
			
			// WAIT FOR SCHEDULER TO BE ONLINE BEFORE STARTING CLUSTER
			waitForScheduler(cf, strSchedulerName, EnumServiceStatus.Running);

			Logging.info("STARTING Cluster " + strClusterName + " ");
			gc.start();
			Logging.info("STARTED Cluster " + strClusterName + " ");
		}
			
	}
	
	
	private void toggleService(ControlFactory cf, String strSvcName, String strStopStart) throws Exception{
		
		Service[] svcs = cf.getServices();
		
		for (Service svc : svcs) {
			
			String strCurrSvcName = svc.getName();
			if(strCurrSvcName.equals(strSvcName)){
				
				if(strStopStart.equals("STOP")){
					
					Logging.info("STOPPING service: " + svc.getName());
					svc.stop();
					Logging.info("STOPPED service: " + svc.getName());
					
				}else if (strStopStart.equals("START")){
					
					Logging.info("STARTING service: " + svc.getName());
					svc.start();
					Logging.info("STARTED service: " + svc.getName());
				}
			}
		}
	}
}
