/********************************************************************************
Status:         completed

Revision History:
1.0   2010-12-08   Prashanth    EPI-1687    initial version

**********************************************************************************/

package com.openlink.jm.bo;


/**
 * Derived from base class to enable different constant repository settings for 
 * different applications of the plug-in 
 *
 * @author Prashanth
 * @version 1.0
 * @category none
 */
@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@com.olf.openjvs.ScriptAttributes(allowNativeExceptions = false)
public class JM_AutomatedPaymentsProcessing_00 extends JM_AutomatedDocumentProcessing implements com.olf.openjvs.IScript {

	protected String getConstRepoSubcontext() {
		return "Auto Payments Processing";
	}
	
	protected int getProcessingIteration() {
		return 0;
	}

}


