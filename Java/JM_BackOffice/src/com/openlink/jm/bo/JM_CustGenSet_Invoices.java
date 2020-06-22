package com.openlink.jm.bo;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.StlDoc;
import com.openlink.sc.bo.docproc.OLI_GEN_Invoice;

/*
 * History:
 * 2020-03-25  V1.1  YadavP03  	- memory leaks, remove console prints & formatting changes
 */

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_GENERATE)
@com.olf.openjvs.ScriptAttributes(allowNativeExceptions = false)
public class JM_CustGenSet_Invoices implements IScript {
	
	public void execute(IContainerContext context) throws OException {
		
		new OLI_GEN_Invoice().execute(context);

		String xmlData = StlDoc.getXmlData();

		JM_GEN_DocNumbering dn = new JM_GEN_DocNumbering();
		dn.setXmlData(xmlData);
		dn.execute(context);
		xmlData = dn.getXmlData();

//		OLI_GEN_DataTransformer dt = new OLI_GEN_DataTransformer();
//		dt.setXmlData(xmlData);
//		dt.execute(context);
//		xmlData = dt.getXmlData();

		StlDoc.setReturnXmlData(xmlData);
	}
}
