package com.matthey.testutil;

import java.util.Arrays;
import java.util.List;

import com.matthey.testutil.common.SAPTestUtilitiesConstants;
import com.matthey.testutil.exception.SapTestUtilRuntimeException;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBase;
import com.olf.openjvs.DataMaint;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.ARCHIVE_DATA_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.logging.PluginLog;

/**
 * Main script to Bulk purge trades.
 * This class extends BulkOperationScript and Overrides the Methods specific to Purge operation.
 * @author jains03
 *
 */
public abstract class BulkPurgeDeals extends BulkOperationScript
{
	private final String DEAL_NUM_COLUMN = "deal_num";
	private final String TRAN_NUM_COLUMN = "tran_num";
	private final String TRAN_STATUS_COLUMN = "tran_status";
	private final String TOOLSET_COLUMN = "toolset";
	
	@Override
	public Table generateInputData() throws OException 
	{
		return null;
	}

	@Override
	public Table performBulkOperation(Table tblInputData) throws OException 
	{
		int numRows = tblInputData.getNumRows();
		
		PluginLog.debug("Number of deals to purge: " + numRows);
		
		tblInputData.addCol(SAPTestUtilitiesConstants.STATUS, COL_TYPE_ENUM.COL_STRING);
		
		for (int row = 1; row <= numRows; row++)
		{
			int dealNum = tblInputData.getInt(DEAL_NUM_COLUMN, row);
			int tranNum = tblInputData.getInt(TRAN_NUM_COLUMN, row);
			int tranStatus = tblInputData.getInt(TRAN_STATUS_COLUMN, row);
			int toolset = tblInputData.getInt(TOOLSET_COLUMN, row);
			
			PluginLog.debug("Attempting to purge deal_num: " + dealNum);
			
			ARCHIVE_DATA_TYPES archiveDataTypes = null;
			if (toolset == TOOLSET_ENUM.COMPOSER_TOOLSET.toInt())
			{	
				archiveDataTypes = ARCHIVE_DATA_TYPES.ADT_PURGE_COMPOSER_DEAL;
			}
			else if (tranStatus == TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt()) 
			{
				archiveDataTypes = ARCHIVE_DATA_TYPES.ADT_PURGE_CANCELLED_DEAL;
			} 
			else if (tranStatus == TRAN_STATUS_ENUM.TRAN_STATUS_DELETED.toInt()) 
			{
				archiveDataTypes = ARCHIVE_DATA_TYPES.ADT_PURGE_DELETED_DEAL;
			} 
			else if (tranStatus == TRAN_STATUS_ENUM.TRAN_STATUS_CLOSEOUT.toInt()) 
			{
				archiveDataTypes = ARCHIVE_DATA_TYPES.ADT_PURGE_CLOSEOUT_DEAL;
			} 
			else 
			{
				tblInputData.setString(SAPTestUtilitiesConstants.STATUS, row, "Transaction status " + tranStatus + " is not supported. Unable to purge deal " + dealNum);
			}
			
			if (archiveDataTypes != null) 
			{
				try
				{
					if (toolset == TOOLSET_ENUM.COMPOSER_TOOLSET.toInt())
					{
						adjustComposerTradeFlag(tranNum);
					}
					
					int retValue = DataMaint.tranPurge(archiveDataTypes.toInt(), dealNum);
					
					if (retValue != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) 
					{
						tblInputData.setString(SAPTestUtilitiesConstants.STATUS, row, "Unable to purge deal " + dealNum);
					}
					else
					{
						PluginLog.debug("Purged deal_num: " + dealNum);
						tblInputData.setString("status", row, "Purged deal " + dealNum);
					}	
				}
				catch (Exception e)
				{
					com.matthey.testutil.common.Util.printStackTrace(e);
					PluginLog.error("Error during purging deal_num" + dealNum + ", " + e.getMessage());
				}
			}
		}
		
		return tblInputData;
	}

	@Override
	public String getOperationName() 
	{
		return "Bulk Purge Operation";
	}
	
	/**
	 * Return the transaction status in which the operation is allowed
	 * 
	 * @return List of transaction status in which the operation is allowed
	 */
	public List<Integer> getAllowedTranStatus() 
	{
		return Arrays.asList(TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt()
				,TRAN_STATUS_ENUM.TRAN_STATUS_DELETED.toInt()
				,TRAN_STATUS_ENUM.TRAN_STATUS_CLOSEOUT.toInt());
	}
	
	/**
	 * Composer/Strategy deals are not real transactions in the database. Purging
	 * only works for trades that have ab_tran.trade_flag set to 1. This is a workaround
	 * to set the flag to 1 so that the core purge API can flush these out.
	 * 
	 * @param tranNum
	 * @throws OException
	 */
	private void adjustComposerTradeFlag(int tranNum) throws OException
	{
		Table tblArgs = null;
		
		try
		{
			tblArgs = Table.tableNew();
			tblArgs.addCol(TRAN_NUM_COLUMN, COL_TYPE_ENUM.COL_INT);
			tblArgs.addRow();
			tblArgs.setInt(TRAN_NUM_COLUMN, 1, tranNum);

			int ret = DBase.runProc("USER_update_ab_tran_composer_deal", tblArgs);
			
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				String message = DBUserTable.dbRetrieveErrorInfo(ret, "DBase.runProc() failed");
				PluginLog.error(message);
				throw new SapTestUtilRuntimeException(message);
			}
		}
		catch (Exception e)
		{
			throw new SapTestUtilRuntimeException("Unable to purge composer toolset tran_num: " + tranNum, e);
		}
		finally
		{
			if (tblArgs != null)
			{
				tblArgs.destroy();
			}
		}
	}
}
