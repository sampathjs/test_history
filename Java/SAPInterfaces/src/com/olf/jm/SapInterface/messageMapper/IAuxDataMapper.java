package com.olf.jm.SapInterface.messageMapper;

import com.olf.openrisk.table.Table;

/**
 * The Interface IAuxDataMapper.
 */
public interface IAuxDataMapper {

	/**
	 * Builds the aux data table.
	 *
	 * @return the table
	 */
	Table buildAuxDataTable();
	
	/**
	 * Sets the aux data.
	 *
	 * @param auxData the table to populate
	 */
	void setAuxData(Table auxData);
}
