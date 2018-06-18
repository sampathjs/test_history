package com.olf.jm.advancedpricing.fieldnotification;

import com.olf.embedded.application.Context;
import com.olf.openrisk.table.Table;

/*
 * History: 
 * 2016-11-07	V1.0 	scurran	- Initial Version
 */ 

/**
 * Reset a Pick list info field to the default values as defined in the table
 * tran_info_types.
 */
public class ResetPicklistToDefault extends AbstractFieldReset {

	/** The defaults. */
	private Table defaults;
	
	/**
	 * Instantiates a the class, loading the tran info data from the database.
	 *
	 * @param context the context the script is running in.
	 */
	public ResetPicklistToDefault(Context context) {
		defaults = context.getIOFactory().runSQL("select * from tran_info_types");
		
		if(defaults == null || defaults.getRowCount() == 0) {
			throw new RuntimeException("Error loading tran info defaults. No data returned.");
		}
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.trading.fieldNotification.AbstractFieldReset#getValue(java.lang.String)
	 */
	@Override
	protected String getValue(String fieldName) {
		int rowId = defaults.find(defaults.getColumnId("type_name"), fieldName, 0);
		
		if(rowId < 0) {
			throw new RuntimeException("Info field " + fieldName + " does not have an entry in the default table");
		}
		return defaults.getString("default_value", rowId);
	}

}
