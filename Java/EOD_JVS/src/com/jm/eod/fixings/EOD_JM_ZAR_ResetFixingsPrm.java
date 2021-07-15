/********************************************************************************
 * Script Name: EOD_JM_ZAR_ResetFixingsPrm
 * Script Type: Parameter
 *
 * Define params for fixing deals with BFIX FX Spot rate for ZAR currency.
 *
 * Revision History:
 * Version Date       Author      Description
 * 1.0     14-Jul-21  Varadaraju Murthy  Initial Version			
 ********************************************************************************/

package com.jm.eod.fixings;

import com.jm.eod.common.Const;
import com.jm.eod.common.CurrencyEnum;
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

public class EOD_JM_ZAR_ResetFixingsPrm implements IScript
{
    public void execute (IContainerContext context) throws OException
	{
		Utils.setParams(context.getArgumentsTable(), Const.RESET_FIXINGS_QRY_NAME, CurrencyEnum.ZAR.toString().trim());	
		Util.exitSucceed();
	}
}