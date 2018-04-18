package com.olf.jm.receiptworkflow.persistence;

import com.olf.jm.receiptworkflow.model.ConfigurationItem;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.staticdata.SecurityGroup;

public class BatchUtil {
	
	/**
	 * Figure out if I am doing Batch Injections - 
	 * @param nominations
	 * @return True / False
	 */
	public static boolean amIDoingBatchInjection(Nominations nominations) {
		 for (Nomination nom : nominations)
		 {
			if (nom instanceof Batch)
			{
				Batch bnom = (Batch)nom;
				if (bnom.isOriginBatch())
					return true;
			}
			else
			{
				return false;
			}
				
		 }
		 return false;
	}
	
	/**
	 * Checks if the provided personnel is a safe desktop user.
	 * This is done by checking if the user is  belonging to the security group 
	 * @param p
	 * @return
	 */
	public static boolean isSafeUser (final Person p) {
		String safeUserSecGroup = ConfigurationItem.SAFE_SECURITY_GROUP.getValue();
		for (SecurityGroup group : p.getSecurityGroups()) {
			if (group.getName().equals(safeUserSecGroup)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * To prevent initialization.
	 */
	private BatchUtil() {
		
	}
}
