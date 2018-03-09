/**
 * 
 */
package com.jm.archivepurgeutilities.interfaces;

import com.jm.data.LogTableData;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;

/**
 * @author SharmV03
 * 
 */
public interface ArchivePurgeUtilitiesInterface
{
	/**
	 * @return Name of user table which will contains details needed to process data
	 */
	String getConfigurationUserTableName();

	/**
	 * @return Procedure name to perform operation in database
	 */
	String getStoredProcedureName();

	/**
	 * Add columns to table {@code argArgumentTableForStoredProcedure}. This
	 * table will be used as argument for stored procedure.
	 * 
	 * @param argArgumentTableForStoredProcedure
	 * @throws OException
	 */
	Table setupStoredProcedureArgsTableStructure() throws OException;
	
	/**
	 * Reads data from configuration table and fills table for procedure arguments.
	 * @param row
	 * @return Table filled with data for procedure argument.
	 * @throws OException
	 */
	Table generateStoredProcedureInputArgs( int row ) throws OException;
	
	/**
	 * @return Subject of the e-mail which reports status to users.
	 */
	String getEmailSubject();

	/**
	 * @param argEmailReceipients
	 * @param emailBody
	 */
	void sendReport( String argEmailReceipients, String emailBody );

	/**
	 * @return Table name to log operation related information
	 */
	String getLogUserTableName();
	
	
	/**
	 * Populate data from database in jvs table.
	 * @throws OException
	 */
	void fillConfigurationData() throws OException;
	
	/**
	 * This method verifies {@code argData}. If {@code argData} is {@code null}
	 * or empty it returns {@code false} otherwise {@code true}
	 * 
	 * @param argData
	 *            Data to be validate
	 *
	 * @return True if data is valid as per conditions.
	 */
	boolean isEmpty(String argData);
	
	/**
	 * Update archive purge log table
	 * 
	 * @param argLogTableData
	 * @throws OException 
	 */
	void insertAuditLog(LogTableData argLogTableData) throws OException;
	
	/**
	 * Run procedure with arguments passed to this method.
	 * 
	 * @param table Arguments table containing data to pass as argument to procedure 
	 * @return Table containing processed data
	 * @throws OException
	 */
	void process(Table table) throws OException;
	
	/**
	 * @param table
	 * @return Path of created file.
	 */
	String createCSVFile( Table table );
}
