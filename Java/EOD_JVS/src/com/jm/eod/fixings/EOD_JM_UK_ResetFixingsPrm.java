/********************************************************************************
 * Script Name: EOD_JM_UK_ResetFixingsPrm
 * Script Type: Parameter
 *
 * Define params for fixing deals within UK region.
 *
 * Revision History:
 * Version Date       Author      Description
 * 1.0     11-Nov-15  D.Connolly  Initial Version
 ********************************************************************************/

package com.jm.eod.fixings;

import com.jm.eod.common.*;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SCRIPT_TYPE_ENUM;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)

public class EOD_JM_UK_ResetFixingsPrm implements IScript
{
    public void execute (IContainerContext context) throws OException
	{
		Utils.setParams(context.getArgumentsTable(), Const.RESET_FIXINGS_QRY_NAME, RegionEnum.UK.description().trim());	
		Util.exitSucceed();
	}
}