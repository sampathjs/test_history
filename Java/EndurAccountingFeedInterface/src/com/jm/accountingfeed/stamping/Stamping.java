package com.jm.accountingfeed.stamping;

import com.jm.accountingfeed.enums.AuditRecordStatus;
import com.jm.accountingfeed.exception.AccountingFeedRuntimeException;
import com.jm.accountingfeed.util.Util;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.jm.logging.Logging;

/**
 * Stamping for Accounting feed interface is the process performed 
 * after Ledger has been extracted to the JDE specific XML format.
 * 
 * This involves 
 * 1. reading the extracted Entities (deals for Gl,ML AND invoices for SL) from the Boundary table
 * 2. updating the JDE status for the Entities
 */
public abstract class Stamping implements IScript 
{
	private Table tblArgt;
	
	protected Table tblRecordsToStamp;
	
	public void execute(IContainerContext context) throws OException
	{
		Util.setupLog();
		
		tblArgt = context.getArgumentsTable();
		
		try
		{
			tblRecordsToStamp = getRecordsToStamp();
			
			/* Initialise any prerequisites, if any */
			Logging.info("Initialising prerequisites..");
			initialisePrerequisites();
			
			/* Perform stamping logic */
			Logging.info("Stamping ledger records..");
			stampRecords();
			Logging.info("Finished stamping ledger records!");
			
			/* Mark records as processed */
			Logging.info("Stamping audit records as complete..");
			stampingProcessed();	
		}
		catch (Exception e)
		{
			Util.printStackTrace(e);
			throw new AccountingFeedRuntimeException("Error occured during Stamping.execute", e);
		}
		finally
		{
			cleanup();
			
			if (tblRecordsToStamp != null)
			{
				tblRecordsToStamp.destroy();
			}
			
			Logging.info("Finished stamping audit records as complete!");
			Logging.close();
		}
	}
	
	/**
	 * Get the region from the task
	 * 
	 * @return
	 * @throws OException
	 */
	protected String getRegion() throws OException
	{
		if (tblArgt.getNumRows() == 0)
		{
			throw new AccountingFeedRuntimeException("Invalid argt table specified, no region parameter!");
		}
		
		String region = tblArgt.getString("regional_segregation", 1);
		return region;
	}
	
	protected abstract String getAuditUserTable();
	
	/**
	 * Fetch records that need stamping. Depending on the ledger type, these could be deals or invoices
	 * 
	 * @return
	 * @throws OException
	 */
	protected Table getRecordsToStamp() throws OException
	{
		Table tblData = Table.tableNew("Records to Stamp");
		
		String sqlQuery = 
				"SELECT * \n" +
				"FROM \n" +
					getAuditUserTable() + " audit_data \n" +
				"WHERE audit_data.region = '" + getRegion() + "' \n" +
				"AND audit_data.process_status = '" + AuditRecordStatus.NEW + "'";
		
		int ret = DBaseTable.execISql(tblData, sqlQuery);
		
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			throw new AccountingFeedRuntimeException("Unable to run query: " + sqlQuery);
		}
		
		return tblData;
	}
	
	protected abstract void initialisePrerequisites() throws OException;
	
	protected abstract void stampRecords() throws OException;
	
	protected abstract void stampingProcessed() throws OException;
	
	protected abstract void cleanup() throws OException;
}
