package com.openlink.esp.boprocesses.autodocprocessing.app;




/**
 * Derived from base class to enable different constant repository settings for 
 * different applications of the plug-in 
 */

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@com.olf.openjvs.ScriptAttributes(allowNativeExceptions = false)

public class BO_AutomatedPaymentNetting_US extends
	com.openlink.sc.bo.autodocproc.BO_AutomatedDocumentProcessing
{
	protected String getConstRepoSubcontext()
	{
		return "Auto Payment Netting US";
	}
}
