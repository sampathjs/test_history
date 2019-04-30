package com.jm.shanghai.accounting.udsr;

import com.jm.shanghai.accounting.udsr.model.fixed.ConfigurationItem;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;

@ScriptCategory({ EnumScriptCategory.SimResult })
public class ShanghaiGlAccountingUdsr extends AbstractShanghaiAccountingUdsr {

	protected ShanghaiGlAccountingUdsr() {
		super(ConfigurationItem.GL_PREFIX);
	}
}
