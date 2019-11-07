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
import com.openlink.util.logging.PluginLog;

/**
 * Cancel/delete a group of deals depending on the source status.
 * 
 * This utility exists mainly as a prerequisite to purging deals out of the
 * system so the getAllowedTranStatus indicates the source status for this
 * utlity
 */
public abstract class BulkCancelDeleteDeals extends BulkAmendDealStatus
{
	@Override
	public Table performBulkOperation(Table tblInputData) throws OException
	{
		int numRows = tblInputData.getNumRows();
		PluginLog.debug("Number of deals to cancel/delete: " + numRows);
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

				if (TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() == latestTranStatus)
				{
					insertTransactionInEndur(tblInputData, row, trade, tranNum, TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED);
				}
				else if (getAllowedTranStatus().contains(latestTranStatus))
				{
					insertTransactionInEndur(tblInputData, row, trade, tranNum, TRAN_STATUS_ENUM.TRAN_STATUS_DELETED);
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
					if (trade != null)
					{
						trade.destroy();
					}
				}
				catch (OException e)
				{
					PluginLog.warn("Unable to destroy() Transaction " + tranNum);
				}
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
	public List<Integer> getAllowedTranStatus()
	{
		return Arrays.asList(TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt(), TRAN_STATUS_ENUM.TRAN_STATUS_PENDING.toInt(), TRAN_STATUS_ENUM.TRAN_STATUS_PROPOSED.toInt(), TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt());
	}
}
