package com.matthey.openlink.generic;

import com.olf.openjvs.OException;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2021-10-13 	V1.0 - jwaechter - initial version 
 */


/**
 * Class containing logic to retrieve data from constants repository used throughout the project
 * @author jwaechter
 * @version 1.0
 */
public class ConstRepoRetrieval {
	public static final String CONTEXT = "PNL";
	public static final String SUBCONTEXT = "General";
	
	private static final String LIBOR_INDEX_NAME_VAR = "LIBOR Index Name";
	private static final String SHIBOR_INDEX_NAME_VAR = "SHIBOR Index Name";
	private static final String LIBOR_INDEX_NAME_DEFAULT = "FF_OIS.USD";
	private static final String SHIBOR_INDEX_NAME_DEFAULT = "OIS.CNY";
	
	public static final String getLiborIndexName ()  {
		try {
			ConstRepository constRepo;
			constRepo = new ConstRepository(CONTEXT, SUBCONTEXT);
			return constRepo.getStringValue(LIBOR_INDEX_NAME_VAR, LIBOR_INDEX_NAME_DEFAULT);
		} catch (OException e) {
			PluginLog.error("Error retrieving " + CONTEXT + "\\" + SUBCONTEXT + "\\"+ LIBOR_INDEX_NAME_VAR + " from ConstRepo" + e.toString());
			throw new RuntimeException (e);
		}
	}

	public static final String getShiborIndexName ()  {
		try {
			ConstRepository constRepo;
			constRepo = new ConstRepository(CONTEXT, SUBCONTEXT);
			return constRepo.getStringValue(SHIBOR_INDEX_NAME_VAR, SHIBOR_INDEX_NAME_DEFAULT);
		} catch (OException e) {
			PluginLog.error("Error retrieving " + CONTEXT + "\\" + SUBCONTEXT + "\\"+ SHIBOR_INDEX_NAME_VAR + " from ConstRepo" + e.toString());
			throw new RuntimeException (e);
		}
	}
}
