/* Released with version 05-Feb-2020_V17_0_126 of APM */

package standard.apm;

//import standard.apm.ads.Factory;

//import standard.apm.ads.IGatherServiceStatus;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
//import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
//import com.olf.openjvs.Util;

// This file is now just a script wrapper.
// All code that used to exist in this file has been moved to ADS_GatherServiceStatus.

/**
 *************************************************************************** 
 * 
 * Copyright 2008 Open Link Financial, Inc. *
 * 
 * ALL RIGHTS RESERVED *
 * 
 * ***************************************************************************
 * 
 * @author kdonepud
 * 
 *         Description: 
 *         This script will be run on APM start or when a service config changes.
 * 
 *         //Dataset Key for APM Map.put("dataset_type_id", 1); Map.put("scenario_id", 2); Map.put("entity_group_id", 5); Map.put("package", "GAS");
 * 
 */
public class GatherServiceStatusJVS implements IScript {
	
        ///
        ADS_GatherServiceStatus m_serviceStatus = null;

	/// constructor.
	public GatherServiceStatusJVS() throws OException {
           m_serviceStatus = new ADS_GatherServiceStatus();
	}
	
	/// Delegates to the specific implementation which is loaded by the factory	 
	public void execute(IContainerContext context) throws OException {

	   // New ads apm common jar, no longer need this.
	   // NIC: This will lock up if OCONSOLE is set in log4j.rootCategory,  
	   // or if credentials is null or empty string.
//	   print("before Login");
//	   try {
//		   String credentials = EndurSecurityUtils.lookupCredentials();
//		   ADSSecurity.login(credentials);
//
//	   } catch (SecurityException e) {
//		   print("Unable to login to ADS " + e.getMessage());
//		   throw new OException ("Unable to login to ADS " + e);
//	   }
//	   print("after Login");
	
           m_serviceStatus.execute();
	  /* 
      try {
         IGatherServiceStatus gatherSS = Factory.getGatherServiceStatusImpl();
         gatherSS.process(context);

      } catch (Exception e) {
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         e.printStackTrace(pw);
         sw.flush();
         pw.flush();
         OConsole.oprint(sw.toString());
         
         OConsole.oprint("Unable to load and execute GatherServiceStatusJVS script" + e);
         throw new OException("Unable to load and execute GatherServiceStatusJVS script" + e);
      }
      */
   }
}
