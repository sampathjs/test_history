/********************************************************************************
 * Script Name: EOD_JM_CN_MissingResetsPrm
 * Script Type: Parameter
 *
 * Report deals with missing resets within the China region.
 *
 * Revision History:
 * Version Date       Author      Description
 * 1.0     15-Nov-18  S.Arora  Initial Version
 ********************************************************************************/

package com.jm.eod.reports;

import com.jm.eod.common.*;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SCRIPT_TYPE_ENUM;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.PARAM_SCRIPT)

public class EOD_JM_CN_MissingResetsPrm implements IScript
{
    public void execute (IContainerContext context) throws OException
	{
		Utils.setParams(context.getArgumentsTable(), Const.MISSING_RESETS_QRY_NAME, RegionEnum.CHINA.description().trim());		
		Util.exitSucceed();
	}
}
