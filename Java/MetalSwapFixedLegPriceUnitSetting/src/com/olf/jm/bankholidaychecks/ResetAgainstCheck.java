package com.olf.jm.bankholidaychecks;

import com.olf.openjvs.OException;
import com.olf.openrisk.trading.EnumFixedFloat;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumResetDefinitionFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.ResetDefinition;
import com.olf.openrisk.trading.Transaction;
import com.olf.jm.logging.Logging;


public class ResetAgainstCheck {

private final String  resetAgainst; 

public ResetAgainstCheck(String resetAgainst){
	this.resetAgainst = resetAgainst;
}

	protected String checkResetAgainst(Transaction newTran) throws OException {

		String errorMessage = "";
		for (Leg leg : newTran.getLegs()) {

			if (leg.getValueAsInt(EnumLegFieldId.FixFloat) == (EnumFixedFloat.FloatRate.getValue())) {
				ResetDefinition resetdef = leg.getResetDefinition();
				if(resetdef != null){
					String dealResetAgainst = resetdef.getField(EnumResetDefinitionFieldId.Against).getValueAsString();
					Logging.info(String.format("Deal# %s has reset against set to %s", newTran.getDealTrackingId(), dealResetAgainst));
					if (!resetAgainst.contains(dealResetAgainst)) {
						
						errorMessage = errorMessage + String.format("\u2022 Reset Against Value has been set to %s \nPlease go to 'Primary Page Toolset Standard' Tab to make it %s and add %s to RFI Shift\n"
								+ "This is needed to avoid pricing issues over bank holidays\n\n", dealResetAgainst,resetAgainst,dealResetAgainst);
					}
					
				}else{
					throw new OException("Reset Definition was found to be null");
				}
			}
		}
		Logging.info(errorMessage);
		return errorMessage;

	}

}