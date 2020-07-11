package com.olf.jm.pricewebservice.persistence;

import java.util.HashMap;
import java.util.Map;

import com.olf.jm.pricewebservice.model.Triple;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Tpm;
import com.openlink.util.misc.TableUtilities;

/*
 * History: 
 * 2015-04-16 	V1.0	jwaechter	- initial version
 */

/**
 * This class contains methods to deal with the TPM API as used in the PriceWebInterface
 * @author jwaechter
 * @version 1.0
 */
public class TpmHelper {
	
	/**
	 * Returns a map from variable names to triples of (Value, Type, "Info") as taken
	 * from the TPM workflow identified by wflowId.
	 * @param wflowId
	 * @return
	 * @throws OException
	 */
	public static Map<String, Triple<String, String, String>> getTpmVariables (long wflowId) throws OException {
		Table varsAsTable = null;
		Map<String, Triple<String, String, String>> varsAsMap = new HashMap<String, Triple<String, String, String>> ();
		
		try {
			varsAsTable = Tpm.getVariables(wflowId);
			Table varSub = varsAsTable.getTable("variable", 1);			
			for (int row=varSub.getNumRows(); row >= 1; row--) {
				String name  = varSub.getString("name", row).trim();				
				String value  = varSub.getString("value", row).trim();
				String type = varSub.getString("type", row).trim();
				String info  = varSub.getString("info", row).trim();
				varsAsMap.put(name, new Triple<String, String, String>(value, type, info));
			}
		} finally {
			// Possible engine crash destroying table - commenting out Jira 1336
			// varsAsTable = TableUtilities.destroy(varsAsTable);
		}				
		return varsAsMap;
	}	
	
	/**
	 * To prevent instantiation.
	 */
	private TpmHelper () {
		
	}
}
