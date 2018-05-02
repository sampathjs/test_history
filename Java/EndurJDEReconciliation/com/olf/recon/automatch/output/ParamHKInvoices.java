package com.olf.recon.automatch.output;

import java.util.ArrayList;

import com.olf.recon.utils.Constants;

public class ParamHKInvoices extends AbstractParam 
{
	@Override
	protected String getAutoMatchDefinitionName() 
	{
		return Constants.AUTOMATCH_INVOICES_RECONCILIATION_HK;
	}

	@Override
	protected ArrayList<String> getAutoMatchActions() 
	{
		ArrayList<String> actions = new ArrayList<String>();
		
		actions.add(Constants.AUTOMATCH_ACTION_INVOICES_UNMATCHED_IN_ENDUR);
		actions.add(Constants.AUTOMATCH_ACTION_INVOICES_UNMATCHED_IN_JDE);
		
		return actions;
	}
}
