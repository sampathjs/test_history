package com.olf.jm.swaps.app;

import com.olf.openrisk.trading.EnumTranfField;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2016-07-27     V1.0       scurran            - initial version 
 */ 


/**
 * The Class Synchroniser. 
 * 
 * Synchronise a leg level field with all other legs on the trade.
 */
public class Synchroniser {
	
	/** The name of the field to synchronise across legs. */
	private EnumTranfField fieldToSync;
	
	/**
	 * Instantiates a new synchroniser.
	 *
	 * @param fieldToSync the name of the field to sync
	 */
	public Synchroniser(EnumTranfField fieldToSync) {
		this.fieldToSync = fieldToSync;
	}

	/**
	 * Synchronise. Take the value for the field parameter and apply it to all other legs.
	 *
	 * @param field the field to use for synchronisation.
	 */
	public void synchronise(Field field) {
		if(!isApplicable(field)) {
			PluginLog.debug("Skipping field " + field.getName() + ". Not applicable.");
			return;
		}
		
		Transaction parentTran = getTransaction(field);
		
		String value = field.getValueAsString();
		
		for(Leg leg : parentTran.getLegs()) {
			try(Field fieldToSet = parentTran.retrieveField(fieldToSync, leg.getLegNumber())) {
				setField( fieldToSet,  value);
			} catch (Exception e) {
				String errorMessage = "Error setting the field " + field.getName() + ". with value " + value + " on leg " + leg.getLegNumber();
				PluginLog.error(errorMessage);
				throw new RuntimeException(errorMessage);
			}
		}
	}
	
	/**
	 * Checks if is applicable. Should the value be synchronised to 
	 * all other legs. 
	 *
	 * @param field the field
	 * @return true, if is applicable
	 */
	protected boolean isApplicable(Field field) {
		if(field.getTranfId() == fieldToSync) {
			return true;
		} 
		
		return false; 
	}
	
	/**
	 * Gets the transaction the field is associated with.
	 *
	 * @param field the field to get the parent transaction from
	 * 
	 * @return the parent transaction
	 */
	protected Transaction getTransaction(Field field) {
		
		Transaction parentTran = field.getTransaction();
		
		if(parentTran == null) {
			String errorMessage = "Error retrieving the parent transaction";
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		return parentTran;
				
	}
	
	/**
	 * Sets the field with a new value.
	 *
	 * @param fieldToSet the field to set
	 * @param value the new value to use
	 */
	protected void setField(Field fieldToSet, String value) {
		if(fieldToSet == null || !fieldToSet.isApplicable() || fieldToSet.isReadOnly()) {
			// Log an error but don't throw an exception so can carry on with the other legs.
			String errorMessage = "Field is null, readonly or not applicable.";
			PluginLog.error(errorMessage);
			return;			
		}
		
		fieldToSet.setValue(value);

	}
	
}
