package com.jm.eod.fixings;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.ScriptAttributes;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class EOD_JM_ReRun_ResetFixings_AdhocPrm implements IScript{
	public void execute (IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();	
		argt.addCol("queryName", COL_TYPE_ENUM.COL_STRING);
		argt.addRow();
		argt.setColValString("queryName", "prior_day_reset_NY_MW_MID_deals");
	}

}
