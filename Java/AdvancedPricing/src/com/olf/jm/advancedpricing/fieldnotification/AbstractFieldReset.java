package com.olf.jm.advancedpricing.fieldnotification;

import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.logging.PluginLog;


/*
 * History: 
 * 2016-11-07	V1.0 	scurran	- Initial Version
 */


/**
 * Abstract base class for resetting tran info field. Derived classes 
 * control the value the field it reset to.
 */
public abstract class AbstractFieldReset {

	/**
	 * Gets the value to reset the field to.
	 *
	 * @param fieldName the name of the field being reset
	 * @return the value to set
	 */
	protected abstract String getValue(String fieldName);
	
	/**
	 * Reset a tran info field.
	 *
	 * @param tran the transaction the field belongs to. 
	 * @param fieldName the name of the field to reset.
	 */
	public void resetField(Transaction tran, String fieldName) {
		
		try(Field field = tran.getField(fieldName)) {
			if(field != null) {
				if(field.isApplicable() && field.isWritable()) {
					field.setValue(getValue(fieldName));
				} else {
					PluginLog.info("Field " + fieldName + " is not applicable or is read only.");
				}
			} else {
				PluginLog.info("Field " + fieldName + " is null");
			}
		} catch( Exception e) {
			throw new RuntimeException("Error resetting fiels " + fieldName + ". Error " + e.getMessage());
		}
	}
}
