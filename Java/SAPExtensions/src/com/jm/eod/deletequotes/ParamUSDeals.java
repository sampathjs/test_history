package com.jm.eod.deletequotes;

import com.jm.utils.Constants;

public class ParamUSDeals extends AbstractParam 
{
	@Override
	protected String getQueryName() 
	{
		return Constants.SAP_OPEN_QUOTES_US;
	}
}
