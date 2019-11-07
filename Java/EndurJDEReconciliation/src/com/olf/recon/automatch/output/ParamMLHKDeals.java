package com.olf.recon.automatch.output;

import java.util.ArrayList;

import com.olf.recon.utils.Constants;

public class ParamMLHKDeals extends AbstractParam 
{
	@Override
	protected String getAutoMatchDefinitionName() 
	{
		return Constants.AUTOMATCH_ML_RECONCILIATION_HK;
	}

	@Override
	protected ArrayList<String> getAutoMatchActions() 
	{
		ArrayList<String> actions = new ArrayList<String>();
		
		actions.add(Constants.AUTOMATCH_ACTION_MLHK_UNMATCHED_IN_ENDUR);
		actions.add(Constants.AUTOMATCH_ACTION_MLHK_UNMATCHED_IN_JDE);
		
		return actions;
	}
}
