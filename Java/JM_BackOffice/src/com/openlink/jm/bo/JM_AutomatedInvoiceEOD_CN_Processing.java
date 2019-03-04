package com.openlink.jm.bo;

/**
 * Derived from base class to enable different constant repository settings for 
 * different applications of the plug-in 
 */

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@com.olf.openjvs.ScriptAttributes(allowNativeExceptions = false)
public class JM_AutomatedInvoiceEOD_CN_Processing extends JM_AutomatedDocumentProcessing {
	protected String getConstRepoSubcontext()
	{
		return "Auto Invoice EOD CN Processing";
	}
}
