package com.matthey.openlink.utilities.stub;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.tpm.AbstractProcessStep;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.Token;
import com.olf.openrisk.tpm.Variable;
import com.olf.openrisk.tpm.Variables;

@ScriptCategory({ EnumScriptCategory.TpmStep })
public class RetrieveCollateralStub extends AbstractProcessStep {

	@Override
	public Table execute(Context context, Process process, Token token,
			Person submitter, boolean transferItemLocks, Variables variables) {

		Variable collateral = null;
		if (variables.contains("Collateral")) {
			collateral = variables.getVariable("Collateral");
		}
		if (collateral == null) {
			throw new OpenRiskException(
					"Variable not defined before use Collateral!");
		}

		collateral.setValue(0);

		return variables.asTable().cloneData();
	}

}