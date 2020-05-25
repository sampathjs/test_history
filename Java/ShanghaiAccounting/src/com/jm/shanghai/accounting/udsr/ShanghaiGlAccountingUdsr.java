package com.jm.shanghai.accounting.udsr;

import com.jm.shanghai.accounting.udsr.model.fixed.ConfigurationItem;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;

/*
 * History:
 * 2018-11-DD		V1.0	jwaechter	 - Initial Version
 */

/**
 * The main plugin for the GL sim result. Currently setting the type prefix
 * to {@link ConfigurationItem#GL_PREFIX} only. 
 * The type prefix applies a hard coded value used to filter the mapping tables
 * for rules just belonging to the defined type.
 * @author jwaechter
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.SimResult })
public class ShanghaiGlAccountingUdsr extends AbstractShanghaiAccountingUdsr {

	protected ShanghaiGlAccountingUdsr() {
		super(ConfigurationItem.GL_PREFIX);
	}
}
