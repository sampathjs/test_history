package com.jm.archivepurgeutilities.util;

import com.jm.archivepurgeutilities.exception.ArchivePurgeUtilitiesRuntimeException;
import com.openlink.util.constrepository.ConstRepository;

/**
 * Loads up Reconciliation config from the Constants Repository
 */
public class ConstRepoConfig 
{
	private static final ConstRepository constRepo;

	private static String CONST_REP_CONTEXT = "Utilities";
	private static String CONST_REP_SUBCONTEXT = "ArchivePurgeUtilities";
		
	static 
	{
		try 
		{
			constRepo = new ConstRepository(CONST_REP_CONTEXT, CONST_REP_SUBCONTEXT);
		} 
		catch (Exception e) 
		{
			throw new ArchivePurgeUtilitiesRuntimeException("Error initializing ConstantsRepository: " + CONST_REP_CONTEXT + CONST_REP_SUBCONTEXT+e.getMessage());
		}
	}
	
	public static String getValue(String variableName, String defaultValue) 
	{
		try
		{
			return constRepo.getStringValue(variableName, defaultValue);	
		}
		catch (Exception e)
		{
			throw new ArchivePurgeUtilitiesRuntimeException("Unable to get const repo variable: " + variableName);
		}	
	}
}
