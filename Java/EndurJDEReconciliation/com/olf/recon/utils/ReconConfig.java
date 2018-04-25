package com.olf.recon.utils;

import com.olf.recon.enums.ReportingDeskName;
import com.olf.recon.exception.ReconciliationRuntimeException;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/**
 * Loads up Reconciliation config from the Constants Repository
 */
public class ReconConfig 
{
	private ConstRepository constRepo;
	private String subContext;
	
	private static String CONST_REP_CONTEXT = "Reconciliation";
	private static String CONST_REP_SUBCONTEXT_UK = "EndurJDEReconciliation_UK";
	private static String CONST_REP_SUBCONTEXT_US = "EndurJDEReconciliation_US";
	private static String CONST_REP_SUBCONTEXT_HK = "EndurJDEReconciliation_HK";
	
	public ReconConfig(String region)
	{
		/* Default case */
		subContext = CONST_REP_SUBCONTEXT_UK;
		
		/* Override for other regions, iof set */
		if (ReportingDeskName.US.toString().equalsIgnoreCase(region))
		{
			subContext = CONST_REP_SUBCONTEXT_US;
		}
		else if (ReportingDeskName.HK.toString().equalsIgnoreCase(region))
		{
			subContext = CONST_REP_SUBCONTEXT_HK;
		}
		
		PluginLog.info("Constants Repository - sub context set to: " + subContext);

		try 
		{
			constRepo = new ConstRepository(CONST_REP_CONTEXT, subContext);
		} 
		catch (Exception e) 
		{
			throw new ReconciliationRuntimeException("Error initializing ConstantsRepository: " + CONST_REP_CONTEXT + subContext, e);
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
			throw new ReconciliationRuntimeException("Unable to get const repo variable: " + variableName + ", for context: " + subContext);
		}	
	}
}
