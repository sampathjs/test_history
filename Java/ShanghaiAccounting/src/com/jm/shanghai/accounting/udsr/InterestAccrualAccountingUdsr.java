package com.jm.shanghai.accounting.udsr;

import com.jm.shanghai.accounting.udsr.model.fixed.ConfigurationItem;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;

@ScriptCategory({ EnumScriptCategory.SimResult })
public class InterestAccrualAccountingUdsr extends AbstractShanghaiAccountingUdsr {

	protected InterestAccrualAccountingUdsr() {
		super(ConfigurationItem.IA_PREFIX);
	}
}
