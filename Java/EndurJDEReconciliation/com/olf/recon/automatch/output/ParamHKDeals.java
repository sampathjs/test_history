package com.olf.recon.automatch.output;

import java.util.ArrayList;

import com.olf.recon.utils.Constants;

public class ParamHKDeals extends AbstractParam 
{
	@Override
	protected String getAutoMatchDefinitionName() 
	{
		return Constants.AUTOMATCH_DEALS_RECONCILIATION_HK;
	}

	@Override
	protected ArrayList<String> getAutoMatchActions() 
	{
		ArrayList<String> actions = new ArrayList<String>();
		
		actions.add(Constants.AUTOMATCH_ACTION_DEALS_UNMATCHED_IN_ENDUR);
		actions.add(Constants.AUTOMATCH_ACTION_DEALS_UNMATCHED_IN_JDE);
		
		return actions;
	}
}
