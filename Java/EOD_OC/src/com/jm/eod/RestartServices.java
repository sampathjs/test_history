package com.jm.eod;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.control.ControlFactory;
import com.olf.openrisk.control.EnumRunSiteStatus;
import com.olf.openrisk.control.EnumServiceStatus;
import com.olf.openrisk.control.GridCluster;
import com.olf.openrisk.control.GridScheduler;
import com.olf.openrisk.control.Service;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.openlink.util.constrepository.ConstRepository;
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
			
    		if (args.getRowCount() < 1){
    			throw new Exception(String.format("Missing argt "));
    		}

    		String strRunRestart = args.getString("run_restart", 0);
    		if(strRunRestart.equals("Y")){
    			
    			ConstRepository constRep = new ConstRepository("EOD", "RestartServices");

    			// SERVICES RESTART
    			
    			//Stop the services
    			toggleServices(context,constRep,"STOP");
    			// Stop the clusters
    			toggleCluster(context,constRep,"STOP");
    			// stop the scheduler
    			toggleScheduler( context,constRep,"STOP");
    			// start the scheduler
    			toggleScheduler( context,constRep,"START");
    			// Start the clusters
    			toggleCluster(context,constRep,"START");
    			// Start the services
    			toggleServices(context,constRep,"START");
    			
    		}else{
    			Logging.info("Restart parameter pre-checks failed - exiting.");
    		}
		} catch (Exception e) {
			Logging.info(e.toString());
			Logging.error(getStackTraceMessage(e));
		}
		
		Logging.info("END RestartServices");
		
		return null;
	}

	
	private void waitForScheduler(ConstRepository constRep, ControlFactory cf, String strScheduler, EnumServiceStatus enumSS ) throws Exception {
		
		int intSecondsToWait = constRep.getIntValue("SecondsToWait");
		
		long start = System.currentTimeMillis();
		long end = start + intSecondsToWait*1000;
		GridScheduler gs = cf.getGridScheduler(strScheduler);
		while(gs.getStatus() != enumSS && System.currentTimeMillis() < end ){
			Logging.info("Waiting for " + strScheduler + " to go to" + enumSS.toString());
			Thread.sleep (1000);
		}

		gs = cf.getGridScheduler(strScheduler);
		if(gs.getStatus() != enumSS){
			throw new Exception("Scheduler did not go to " + enumSS.toString() );
		}
	}
	
	
	private void waitForClusters(ConstRepository constRep, ControlFactory cf, EnumRunSiteStatus runSiteStatus ) throws Exception {
		
		int intSecondsToWait = constRep.getIntValue("SecondsToWait");
		
		GridCluster[] gridClusters = cf.getGridClusters();
		
		for (GridCluster gs : gridClusters) {

			long start = System.currentTimeMillis();
			long end = start + intSecondsToWait*1000;

			while(gs.getStatus() != runSiteStatus && System.currentTimeMillis() < end ){
				Logging.info("Waiting for cluster to come online");
				Thread.sleep (1000);
			}

			EnumRunSiteStatus currRunSiteStatus = gs.getStatus();

			if(currRunSiteStatus != runSiteStatus){
				throw new Exception("Dispatcher did not go to " + runSiteStatus.toString() );
			}
			
		}

	}

	private void toggleScheduler(Context context , ConstRepository constRep, String strStopStart) throws Exception {
	
		Table tblSchedulers = context.getTableFactory().fromOpenJvs(constRep.getMultiStringValue("Scheduler"));
		ControlFactory cf = context.getControlFactory();
		
		for(int i=0;i<tblSchedulers.getRowCount();i++){

			String strSchedulerName = tblSchedulers.getString("value", i);
			GridScheduler gs = cf.getGridScheduler(strSchedulerName);
			
			if(strStopStart.equals("STOP")){

				stopScheduler(gs); 
				
			}else if (strStopStart.equals("START")){
				
				// WAIT FOR SCHEDULER TO BE DOWN BEFORE STARTING SCHEDULER
				waitForScheduler( constRep, cf, strSchedulerName , EnumServiceStatus.Down) ;
				
				Logging.info("STARTING SchedulerScheduler " + strSchedulerName + " ");
				gs.start();
				Logging.info("STARTED Scheduler " + strSchedulerName + " ");
			}
		}
	}
	
	private void stopScheduler(GridScheduler gs) {

		String strSchedulerName = gs.getName();
		try{
			
			Logging.info("STOPPING Scheduler " + strSchedulerName + " ");	
			gs.stop();
			Logging.info("STOPPED SchedulerScheduler " + strSchedulerName + " ");

		}catch(Exception e){
			Logging.info("Unable to stop scheduler " + strSchedulerName);
			Logging.error(getStackTraceMessage(e));
		}
	}
	
	private void stopCluster(GridCluster gc) {

		String strClusterName = gc.getName();
		try{
			
			Logging.info("STOPPING Cluster " + strClusterName + " ");	
			gc.stop();
			Logging.info("STOPPED Cluster " + strClusterName + " ");

		}catch(Exception e){
			Logging.info("Unable to stop cluster " + strClusterName);
			Logging.error(getStackTraceMessage(e));
		}
	}
	
	
	private void toggleCluster(Context context , ConstRepository constRep, String strStopStart) throws Exception{
		
		Table tblClusters = context.getTableFactory().fromOpenJvs(constRep.getMultiStringValue("Cluster"));
		ControlFactory cf = context.getControlFactory();
		
		for(int i=0;i<tblClusters.getRowCount();i++){
			
			String strClusterName = tblClusters.getString("value", i);
			GridCluster gc = cf.getGridCluster(strClusterName);
			
			if(strStopStart.equals("STOP")){

				stopCluster(gc);
				
			}else if (strStopStart.equals("START")){
			
				String strSchedulerName = gc.getGridScheduler();
				
				// WAIT FOR SCHEDULER TO BE ONLINE BEFORE STARTING CLUSTER
				waitForScheduler(constRep, cf, strSchedulerName, EnumServiceStatus.Running);

				Logging.info("STARTING Cluster " + strClusterName + " ");
				gc.start();
				Logging.info("STARTED Cluster " + strClusterName + " ");
			}
		}
	}
	
	private void toggleServices(Context context ,ConstRepository constRep, String strStopStart) throws Exception{
		
		Table tblSvc = context.getTableFactory().fromOpenJvs(constRep.getMultiStringValue("Service"));
		ControlFactory cf = context.getControlFactory();
		Service[] svcs = cf.getServices();
		
		for (Service svc : svcs) {
			
			String strCurrSvcName = svc.getName();
			
			int intRowNum = tblSvc.find(tblSvc.getColumnId("value"), strCurrSvcName, 0);
			
			if(intRowNum >= 0){
				
				if(strStopStart.equals("STOP")){
					
					Logging.info("STOPPING service: " + svc.getName());
					svc.stop();
					Logging.info("STOPPED service: " + svc.getName());
					
				}else if (strStopStart.equals("START")){
					
					startService(constRep, cf, svc );
				}
			}
			else{
				Logging.info(svc.getName() + " Not in table, skipping.");
			}
		} 
	}
	
	private void startService(ConstRepository constRep, ControlFactory cf, Service svc ){

		try{

			waitForClusters( constRep, cf, EnumRunSiteStatus.Running );
			Logging.info("STARTING service: " + svc.getName());
			svc.start();
			Logging.info("STARTED service: " + svc.getName());

		}catch(Exception e){
			Logging.info(e.toString() + " Unable to start " + svc.getName() );
			Logging.error(getStackTraceMessage(e));
		}
	}
	
	private String getStackTraceMessage(Exception e){
		
		String strStackTrace="";
		StackTraceElement[] stackTraceElements = e.getStackTrace();
		for (StackTraceElement stackTraceElement : stackTraceElements) {
			
			strStackTrace += stackTraceElement.toString();
		}
		return strStackTrace;
	}
	
}
