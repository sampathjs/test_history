package com.matthey.testutil;

import java.util.List;

import com.matthey.testutil.common.SAPTestUtilitiesConstants;
import com.matthey.testutil.exception.SapTestUtilRuntimeException;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.logging.PluginLog;

/**
 * Amend the status for a collection of deals
 * 
 * @author KailaM01
 */
public abstract class BulkAmendDealStatus extends BulkOperationScript
{
	@Override
	public Table performBulkOperation(Table tblInputData) throws OException
	{
		int numRows = tblInputData.getNumRows();
		tblInputData.addCol(SAPTestUtilitiesConstants.NEW_TRAN_STATUS, COL_TYPE_ENUM.COL_STRING);

		for (int row = 1; row <= numRows; row++)
		{
			Transaction trade = null;

			int tranNum = -1;

			try
			{
				tranNum = tblInputData.getInt("tran_num", row);
				trade = Transaction.retrieve(tranNum);
				TRAN_STATUS_ENUM tranStatusEnum = getDestinationStatus();
				insertTransactionInEndur(tblInputData, row, trade, tranNum, tranStatusEnum);
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

	/**
	 * @param tblInputData
	 * @param row
	 * @param trade
	 * @param tranNum
	 * @throws OException
	 */
	protected void insertTransactionInEndur(Table tblInputData, int row, Transaction trade, int tranNum, TRAN_STATUS_ENUM tranStatus) throws OException
	{
		try
		{
			trade.insertByStatus(tranStatus);
			tblInputData.setString(SAPTestUtilitiesConstants.NEW_TRAN_STATUS, row, tranStatus.name());
		}
		catch (Exception e)
		{
			String message = getOperationName() + " error, tran_num: " + tranNum + " - " + e.getMessage();
			PluginLog.error(message);
			tblInputData.setString(SAPTestUtilitiesConstants.NEW_TRAN_STATUS, row, e.getMessage());
		}
	}

	@Override
	public String getOperationName()
	{
		return "Bulk Amend Deal Status";
	}

	/**
	 * Return the transaction status in which the operation is allowed
	 * 
	 * @return List of transaction status in which the operation is allowed
	 */
	public abstract List<Integer> getAllowedTranStatus();

	/**
	 * Return a destination status for which this operation will move the deal
	 * to
	 * 
	 * @return A destination status for which this operation will move the deal
	 *         to
	 */
	public abstract TRAN_STATUS_ENUM getDestinationStatus();
}
