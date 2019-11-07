/********************************************************************************
 * Script Name: EOD_JM_CN_LockedDealsPrm
 * Script Type: Parameter
 *
 * Report any locked trades within the China region.
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

public class EOD_JM_CN_LockedDealsPrm implements IScript
{
    public void execute (IContainerContext context) throws OException
	{
		Utils.setParams(context.getArgumentsTable(), Const.LOCKED_DEALS_QRY_NAME, RegionEnum.CHINA.description().trim());	
		Util.exitSucceed();
	}
}