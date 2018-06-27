package com.jm.utils;

import com.jm.exception.SapExtensionsRuntimeException;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * Loads up Reconciliation config from the Constants Repository
 */
public class SapExtensionsConfig 
{
	private ConstRepository constRepo;
	private String subContext;
	private static final String CONST_REP_CONTEXT = "SAP";
	
	public static final String CONST_REP_SUBCONTEXT_OPSERVICES = "OpsServices";
	public static final String CONST_REP_SUBCONTEXT_DELETEQUOTES = "DeleteQuotes";
	
	public SapExtensionsConfig(String subContext)
	{
		/* Default case */
		this.subContext = subContext;
		
		PluginLog.info("Constants Repository - sub context set to: " + subContext);

		try 
		{
			constRepo = new ConstRepository(CONST_REP_CONTEXT, subContext);
		} 
		catch (Exception e) 
		{
			throw new SapExtensionsRuntimeException("Error initializing ConstantsRepository: " + CONST_REP_CONTEXT + subContext, e);
		}
	}
	
	/**
	 * Get the value for the specified parameter/variable (from the Consants Repository)
	 * 
	 * @param variableName
	 * @return
	 */
	public String getValue(String variableName) 
	{
		try
		{
			return constRepo.getStringValue(variableName);	
		}
		catch (Exception e)
		{
			throw new SapExtensionsRuntimeException("Unable to get const repo variable: " + variableName + ", for context: " + subContext);
		}	
	}
}
