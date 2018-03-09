package com.matthey.openlink.utilities.stub;

import com.olf.embedded.application.Context;
import com.olf.embedded.tpm.AbstractProcessStep;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.Token;
import com.olf.openrisk.tpm.Variable;
import com.olf.openrisk.tpm.Variables;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;

@ScriptCategory({ EnumScriptCategory.TpmStep })
public class testTPM extends AbstractProcessStep {

	@Override
	public Table execute(Context context, Process process, Token token,
			Person submitter, boolean transferItemLocks, Variables variables) {
		
		System.out.println("Starting...");
        Variable lgd = null;
        if (variables == null)
        	throw new OpenRiskException("Variables NOT avaialble");
		if (variables.contains("JM_LGD")) {
			lgd = variables.getVariable("JM_LGD");
		} else if (variables.contains("UK_LGD")) {
			lgd = variables.getVariable("UK_LGD");
		} else if (variables.contains("US_LGD")) {
			lgd = variables.getVariable("US_LGD");
		} if (lgd == null) {
			throw new OpenRiskException("Variable not defined before use [JM|UK|US]_LGD");
		}
		System.out.println("ALL DONE!");
		return null;
	}

}
