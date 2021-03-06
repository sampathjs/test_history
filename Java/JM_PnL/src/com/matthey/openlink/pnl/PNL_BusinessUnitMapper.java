package com.matthey.openlink.pnl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2020-02-18   V1.1    agrawa01 - memory leaks & formatting changes
 */

public class PNL_BusinessUnitMapper {
	
	// Hard-code the mappings between business units
	private static final String[] s_mapBUFrom = new String[] { "JM PM LTD" };
	private static final String[] s_mapBUTo = new String[] { "JM PMM UK" };		
	
	private static HashMap<Integer, Integer> s_intBuMap = null;
	private static HashMap<Integer, HashSet<Integer> > s_backwardMap = null;	

	private static boolean m_initialised = false;
	
	/**
	 * Initialise the mappings
	 */
	private static void initialise()
	{
		try {
			initLogging();
		} catch (OException e1) {
			e1.printStackTrace();
			Logging.error(e1.getMessage());
		}

		// Build the int BU mapping HashMap from s_mapBUFrom to s_mapBUTo
		s_intBuMap = new HashMap<Integer, Integer>();
		s_backwardMap = new HashMap<Integer, HashSet<Integer> >();
		
		for (int i = 0; i < s_mapBUFrom.length; i++) {
			try {
				int from = Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, s_mapBUFrom[i]);
				int to = Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, s_mapBUTo[i]);
				s_intBuMap.put(from, to);	
				
				if (!s_backwardMap.containsKey(to)) {
					s_backwardMap.put(to, new HashSet<Integer>());
				}
				s_backwardMap.get(to).add(from);
				
			} catch (Exception e) {
				Logging.error("PNL_BusinessUnitMapper:: initialise - " + e.getMessage() + "\n");
				OConsole.message("PNL_BusinessUnitMapper:: initialise - " + e.getMessage() + "\n");
			}
		}	
		
		m_initialised = true;
	}

	/**
	 * Given a set of business units, return a comma-separated list of the same business units, and any others that map to them
	 * @param bunitSet - input set of business units
	 * @return
	 */
	public static String getExtendedBUList(Set<Integer> bunitSet) {
		StringBuilder output = new StringBuilder();
		Set<Integer> extendedBUSet = getExtendedBUSet(bunitSet);
		
		for (int i : extendedBUSet) {
			if (output.length() > 0) {
				output.append(", ");
			}
			output.append("" + i);			
		}
		return output.toString();
	}
	
	/**
	 * Given a set of business units, return a set of the same business units, and any others that map to them
	 * @param bunitSet - input set of business units
	 * @return
	 */
	public static Set<Integer> getExtendedBUSet(Set<Integer> bunitSet) {
		if (!m_initialised) {
			initialise();
		}
		
		HashSet<Integer> extendedSet = new HashSet<Integer>();
		for (int intBU : bunitSet) {
			extendedSet.add(intBU);
			if (s_backwardMap.containsKey(intBU)) {
				extendedSet.addAll(s_backwardMap.get(intBU));
			}
		}
		
		return extendedSet;
	}
	
	/**
	 * Returns either the input BU, or the one it should map to, if defined
	 * @param intBU
	 * @return
	 */
	public Integer getMappedBU(int intBU) {
		if (!m_initialised) {
			initialise();
		}		
		
		int output = intBU;
		if (s_intBuMap.containsKey(intBU)) {
			output = s_intBuMap.get(intBU);
		}
		
		return output;
	}
	
	/**
	 * Initialise standard Plugin log functionality
	 * @throws OException
	 */
	private static void initLogging() throws OException 
	{	
		String abOutdir =  SystemUtil.getEnvVariable("AB_OUTDIR");
		String logLevel = ConfigurationItemPnl.LOG_LEVEL.getValue();
		String logFile = ConfigurationItemPnl.LOG_FILE.getValue();
		String logDir = ConfigurationItemPnl.LOG_DIR.getValue();
		if (logDir.trim().isEmpty()) 
		{
			logDir = abOutdir + "\\error_logs";
		}
		if (logFile.trim().isEmpty()) 
		{
			logFile = PNL_BusinessUnitMapper.class + ".log";
		}
		try 
		{
			Logging.init( PNL_BusinessUnitMapper.class, ConfigurationItemPnl.CONST_REP_CONTEXT, ConfigurationItemPnl.CONST_REP_SUBCONTEXT);
			
		} 
		catch (Exception e) 
		{
			throw new RuntimeException (e);
		}
		Logging.info("Plugin: " + PNL_BusinessUnitMapper.class.getName() + " started.\r\n");
	}
}
