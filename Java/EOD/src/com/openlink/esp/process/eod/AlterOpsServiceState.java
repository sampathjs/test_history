package com.openlink.esp.process.eod;
import java.util.ArrayList;
import java.util.List;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.OpService;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.openlink.util.constrepository.ConstRepository;
import  com.olf.jm.logging.Logging;

/*
 * History:
 * 2016-MM-DD	V1.0	unknown		- Initial version
 * 2016-03-16	V1.1	jwaechter	- added retrieval of OPS to block from variables of TPM workflow
 */


public class AlterOpsServiceState implements IScript {

	private ConstRepository repository = null;
    private String runType = null;
    private String def = null;
    private static final String CONTEXT = "EOD";
	private static final String SUBCONTEXT = "AlterOpsServiceState";
    
    public AlterOpsServiceState(String runType) throws OException {
    	this.repository = new ConstRepository(CONTEXT, SUBCONTEXT);
    	this.def = "OpServiceName";
    	this.runType = runType;
	}
    
    public AlterOpsServiceState() throws OException {
    	this.repository = new ConstRepository(CONTEXT, SUBCONTEXT);
    	this.def = "OpServiceName";
	}
	
	@Override
	public void execute(IContainerContext context) throws OException {		
		try {
			
			initLogging ();
			
        	// Retrieve input parameter, this cannot be null
			runType = getRunType(context);
        	if (runType.isEmpty() || runType == null) 
        		throw new OException("This process expects parameteres to be passed, exiting.");
        	
        	// Get the ops service to be stopped/started
        	List<String> services = getOpsServices(context);
        	for (String serviceName : services) {
            	if (serviceName.isEmpty() == false)
            	{
            		// Change the service
                    int retCode = alterService(serviceName, runType);
                    if (retCode == 1) OConsole.print("Sucessfully change the state of the Ops Services");
                    else			  OConsole.print("Failed to change the state of the Ops Services");
            	}        		
        	}
        }
        catch (OException e) {
            String strMessage = "Unexpected: " + e.getMessage();
            Logging.error(strMessage);
        } finally{
        	Logging.close();
        }
       
	}	
	
	private List<String> getOpsServices (IContainerContext context) throws OException {
		Table argt = context.getArgumentsTable();
		List<String> services = new ArrayList<>(99);
		for (int i=1; i < 100; i++) {
			String colName = String.format("OPS_Service_%02d", i);
			if (argt.getColNum(colName) >= 1) {
				String ops = argt.getString(colName, 1);
				services.add(ops);
			}
		}
		return services;
	}
	
	private String getRunType(IContainerContext context) throws OException{
		Table argt = context.getArgumentsTable();
		String runType = argt.getString("OPSAlterationSwitch", 1);
		return runType;
	}

	/**
	 * @description Start/Stop the relevant op service
	 * @param 		serviceName
	 * @param 		runType
	 * @return		retCode
	 * @throws 		OException
	 */
	private int alterService(String serviceName, String runType) throws OException {
		int retCode = 0;
		if (runType.length() > 0) {
			switch (runType) {
				case "Start" :
					OConsole.print("\nAbout to start Ops Service");
					retCode = OpService.startMonitoring(serviceName);
					break;
				case "Stop":
					OConsole.print("\nAbout to stop Ops Service");
					retCode = OpService.stopMonitoring(serviceName);
					break;
				default:
					throw new OException("Unexpected runType detected: " + runType + ", on OpService: " + serviceName + ", exiting process");
			}
		}
		return retCode;
	}

	/**
	 * @description	Initialises our routine
	 * @throws 		OException
	 */
	private void initLogging () throws OException {   
		String abOutdir = Util.getEnv("AB_OUTDIR");
		String logLevel = repository.getStringValue ("logLevel", "Error");
        String logFile = repository.getStringValue ("logFile", "AlterOpsService.log");
        String logDir = repository.getStringValue ("logDir", abOutdir);        
        try {
           Logging.init(this.getClass(), repository.getContext(),repository.getSubcontext());
        }
        catch (Exception ex) {
            String strMessage = getClass().getSimpleName () + " - Failed to initialize log.";
            OConsole.oprint(strMessage + "\n");
            Util.exitFail();
        }
    }
}
