package com.jm.eod.process;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.ScriptAttributes;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.openlink.util.constrepository.ConstRepository;

@ScriptAttributes(allowNativeExceptions=false)
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class PaymentDateAlertParamHK implements IScript{

	@Override
	public void execute(IContainerContext context) throws OException {
		ConstRepository repository = new ConstRepository("Alerts",
				"Payment Date Alert HK");
		String savedQueryName = repository.getStringValue("Saved Query Name");
		Table argt = context.getArgumentsTable();
		argt.addCol("Saved Query Name",COL_TYPE_ENUM.COL_STRING);
		argt.addCol("Region",COL_TYPE_ENUM.COL_STRING);
		argt.addRow();
		argt.setString("Saved Query Name", 1,savedQueryName);
		argt.setString("Region", 1, "HK");
	}

}
