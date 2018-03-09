package com.olf.jm.SapInterface.messageMapper.RefMapping;

/**
 * The Interface IRefMapManager.
 */
public interface IRefMapManager {

	/**
	 * Adds the mapping look up.
	 *
	 * @param columnName the column name
	 * @param value the value
	 * @throws RefMapException the ref map exception
	 */
	void addMappingLookUp(String columnName, String value) throws RefMapException;
	
	/**
	 * Calculate mapping.
	 *
	 * @throws RefMapException the ref map exception
	 */
	void calculateMapping() throws RefMapException;
	
	/**
	 * Look up.
	 *
	 * @param columnName the column name
	 * @return the string
	 * @throws RefMapException the ref map exception
	 */
	String lookUp(String columnName) throws RefMapException;
}
