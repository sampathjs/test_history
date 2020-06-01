package com.matthey.utilities;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Util;
import  com.olf.jm.logging.Logging;
import com.openlink.util.misc.TableUtilities;
public class TpmUtils {
/*Fetch TPM variable where wflowId i.e. is TPM run ID required can be obtain by using below :
	wflowId = Tpm.getWorkflowId();
	toLoofFor is the variable for which value is to be fetched	
*/
	public static String getTpmVariableValue( long wflowId,  String toLookFor) throws OException {
		com.olf.openjvs.Table varsAsTable = Util.NULL_TABLE;
		try {
			Logging.info("Fetching TPM Variable "+toLookFor+" from workflow Id"+wflowId);
			varsAsTable = Tpm.getVariables(wflowId);
			if (Table.isTableValid(varsAsTable) == 1 || varsAsTable.getNumRows() > 0) {
				com.olf.openjvs.Table varSub = varsAsTable.getTable("variable", 1);
				for (int row = varSub.getNumRows(); row >= 1; row--) {
					String name = varSub.getString("name", row).trim();
					String value = varSub.getString("value", row).trim();
					if (toLookFor.equals(name)) {
						return value;
					}
				}
			} 
		} catch(Exception oe){
			Logging.error("Failed to fetching TPM Variable "+toLookFor+" from workflow Id"+wflowId);
		}finally {
			if (Table.isTableValid(varsAsTable) == 1) {
				varsAsTable = TableUtilities.destroy(varsAsTable);
			}
		}
		return "";
	}
}
