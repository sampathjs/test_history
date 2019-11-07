/********************************************************************************
Status:         completed

Revision History:
1.0 - 2010-12-08 - eikesass - initial version
1.1 - 2014-07-28 - jbonetzk - changed SubContext submitting
1.2 - 2016-08-15 - scurran  - JM custom version

**********************************************************************************/

package com.openlink.jm.bo;


/**
 * Derived from base class to enable different constant repository settings for 
 * different applications of the plug-in 
 *
 * @author eikesass
 * @version 1.0
 * @category none
 */
@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@com.olf.openjvs.ScriptAttributes(allowNativeExceptions = false)
public class JM_AutomatedConfirmationProcessing_03 extends JM_AutomatedDocumentProcessing implements com.olf.openjvs.IScript {

	protected String getConstRepoSubcontext() {
		return "Auto Confirmation Processing";
	}
	
	protected int getProcessingIteration() {
		return 3;
	}

}


