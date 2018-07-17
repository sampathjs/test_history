package com.olf.recon.automatch.output;

import java.util.ArrayList;

import com.olf.recon.utils.Constants;

public class ParamUKDeals extends AbstractParam 
{
	@Override
	protected String getAutoMatchDefinitionName() 
	{
		return Constants.AUTOMATCH_DEALS_RECONCILIATION_UK;
	}

	@Override
	protected ArrayList<String> getAutoMatchActions() 
	{
		ArrayList<String> actions = new ArrayList<String>();
		
		actions.add(Constants.AUTOMATCH_ACTION_DEALS_UNMATCHED_IN_ENDUR);
		actions.add(Constants.AUTOMATCH_ACTION_DEALS_UNMATCHED_IN_JDE);
		
		
		actions.add(Constants.AUTOMATCH_ACTION_DEALS_MATCHED_DUPLICATE_IN_JDE);
	
		return actions;

	}
}
