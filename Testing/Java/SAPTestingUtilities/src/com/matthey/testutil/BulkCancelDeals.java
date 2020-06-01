package com.matthey.testutil;

import java.util.Arrays;
import java.util.List;

import com.matthey.testutil.common.SAPTestUtilitiesConstants;
import com.matthey.testutil.exception.SapTestUtilRuntimeException;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.jm.logging.Logging;

public abstract class BulkCancelDeals extends BulkAmendDealStatus
{
	@Override
	public Table generateInputData() throws OException 
	{
		return null;
	}

	@Override
	public Table performBulkOperation(Table tblInputData) throws OException 
	{
		int numRows = tblInputData.getNumRows();
		Logging.info("Started executing performBulkOperation()");
		tblInputData.addCol(SAPTestUtilitiesConstants.NEW_TRAN_STATUS, COL_TYPE_ENUM.COL_STRING);
		
		for (int row = 1; row <= numRows; row++)
		{
			Transaction trade = null;
		
			int tranNum = -1;
			
			try 
			{
				tranNum = tblInputData.getInt("tran_num", row);
				trade = Transaction.retrieve(tranNum);
				
				int latestTranStatus = trade.getFieldInt(TRANF_FIELD.TRANF_TRAN_STATUS.toInt(), 0);
				
				try
				{
					if (TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() == latestTranStatus)
					{
						trade.insertByStatus(TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED);
						tblInputData.setString(SAPTestUtilitiesConstants.NEW_TRAN_STATUS, row, TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.name());
					}
					else if (getAllowedTranStatus().contains(latestTranStatus))
					{
						trade.insertByStatus(TRAN_STATUS_ENUM.TRAN_STATUS_DELETED);	
						tblInputData.setString(SAPTestUtilitiesConstants.NEW_TRAN_STATUS, row, TRAN_STATUS_ENUM.TRAN_STATUS_DELETED.name());
					}	
				}
				catch (Exception e)
				{
					tblInputData.setString(SAPTestUtilitiesConstants.NEW_TRAN_STATUS, row, e.getMessage());
				}
			} 
			catch (Exception e) 
			{
				throw new SapTestUtilRuntimeException("Unable to perform operation " + getOperationName() + ": ", e);
			} 
			finally 
			{
				try 
				{
					trade.destroy();
				} 
				catch (OException e) 
				{
					Logging.warn("Unable to destroy() Transaction " + tranNum);
				}
				Logging.info("Completed executing performBulkOperation()");
			}
		}
		
		return tblInputData;
	}

	@Override
	public String getOperationName() 
	{
		return "Bulk Cancel Operation";
	}
	
	@Override
	public List<Integer> getAllowedTranStatus() {
		return Arrays.asList(TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt(), 
							 TRAN_STATUS_ENUM.TRAN_STATUS_PENDING.toInt(), 
							 TRAN_STATUS_ENUM.TRAN_STATUS_PROPOSED.toInt(), 
							 TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt());
	}
}
