/********************************************************************************
 * Script Name: EOD_JM_US_MissingValidationsPrm
 * Script Type: Parameter
 *
 * Report non-validated deals within the USA region.
 *
 * Revision History:
 * Version Date       Author      Description
 * 1.0     05-Nov-15  D.Connolly  Initial Version
 ********************************************************************************/

package com.jm.eod.reports;

import com.jm.eod.common.Const;
import com.jm.eod.common.RegionEnum;
import com.jm.eod.common.Utils;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.ScriptAttributes;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)

public class EOD_JM_US_MissingValidationsPrm implements IScript
{
    public void execute (IContainerContext context) throws OException
	{
		String filename = "Missed_Validations.eod"; 
    	Utils.setDefaultParams(Const.MISSING_VALIDATIONS_QRY_NAME, RegionEnum.USA.description().trim());
		Utils.setParams(Const.FILE_COL_NAME, filename);
		Utils.addParams(context.getArgumentsTable());
		Util.exitSucceed();
	}
}