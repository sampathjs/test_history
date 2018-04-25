package com.jm.accountingfeed.util;

import com.jm.accountingfeed.exception.AccountingFeedRuntimeException;
import com.openlink.util.constrepository.ConstRepository;

/**
 * Loads up Reconciliation config from the Constants Repository
 */
public class ConstRepoConfig 
{
	private static final ConstRepository constRepo;

	private static String CONST_REP_CONTEXT = "Interfaces";
	private static String CONST_REP_SUBCONTEXT = "EndurAccountingFeed";
		
	static 
	{
		try 
		{
			constRepo = new ConstRepository(CONST_REP_CONTEXT, CONST_REP_SUBCONTEXT);
		} 
		catch (Exception e) 
		{
			Util.printStackTrace(e);
			throw new AccountingFeedRuntimeException("Error initializing ConstantsRepository: " + CONST_REP_CONTEXT + CONST_REP_SUBCONTEXT, e);
		}
	}
	
	public String getValue(String variableName) 
	{
		try
		{
			return constRepo.getStringValue(variableName);	
		}
		catch (Exception e)
		{
			throw new AccountingFeedRuntimeException("Unable to get const repo variable: " + variableName);
		}	
	}
	
	public String getValue(String variableName, String defaultValue) 
	{
		try
		{
			return constRepo.getStringValue(variableName, defaultValue);	
		}
		catch (Exception e)
		{
			Util.printStackTrace(e);
			throw new AccountingFeedRuntimeException("Unable to get const repo variable: " + variableName);
		}	
	}
}
