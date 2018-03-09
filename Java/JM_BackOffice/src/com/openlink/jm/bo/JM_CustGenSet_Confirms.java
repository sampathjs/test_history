package com.openlink.jm.bo;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.jm.bo.JM_GEN_DocNumbering;
import com.openlink.sc.bo.datatransformer.OLI_GEN_DataTransformer; 

/*
 * History:
 * 2015-MM-DD	V1.0	<unknown>	- Initial Version
 * 2016-04-05	V1.1	jwaechter	- removed JM_GEN_DocNumbering
 *                                  - added JM_GEN_Output_Param
 */


@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_GENERATE)
@com.olf.openjvs.ScriptAttributes(allowNativeExceptions = false)
public class JM_CustGenSet_Confirms implements IScript
{
	public void execute(IContainerContext context) throws OException
	{
		String xmlData = StlDoc.getXmlData();

		JM_GEN_Output_Param op = new JM_GEN_Output_Param();
		op.execute(context);
		
//		JM_GEN_DocNumbering dn = new JM_GEN_DocNumbering();
//		dn.setXmlData(xmlData);
//		dn.execute(context);
//		xmlData = dn.getXmlData();
		
		OLI_GEN_DataTransformer dt = new OLI_GEN_DataTransformer();
		dt.setXmlData(xmlData);
		dt.execute(context);
		xmlData = dt.getXmlData();
		
		StlDoc.setReturnXmlData(xmlData);
	}
}
