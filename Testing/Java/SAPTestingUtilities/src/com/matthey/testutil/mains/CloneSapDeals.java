package com.matthey.testutil.mains;

import java.util.HashMap;
import java.util.Map;

import com.matthey.testutil.BulkCloneDeals;
import com.matthey.testutil.common.Util;
import com.matthey.testutil.exception.SapTestUtilRuntimeException;
import com.matthey.testutil.toolsetupdate.CsvWrapper;
import com.matthey.testutil.toolsetupdate.DealDelta;
import com.matthey.testutil.toolsetupdate.ToolsetFactory;
import com.matthey.testutil.toolsetupdate.ToolsetI;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SCRIPT_TYPE_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.openlink.util.logging.PluginLog;

/**
 * Utility to clone deals. This is created specifically for SAP
 * @author SharmV04
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
public class CloneSapDeals extends BulkCloneDeals
{
	protected final String TRADE_PRICE_COLUMN = "trade_price";
	protected final String STATUS_COLUMN = "status";
	protected final String COMMENTS_COLUMN = "comments";
	protected final String NEW_DEAL_NUMBER_COLUMN = "new_deal_num";
	protected final String REFERENCE_COLUMN = "reference";
	private final String EMPTY_STRING = "";
	
	@Override
	public Table generateInputData() throws OException 
	{
		Table tblArgt = getArgt();
		
		String csvPath = tblArgt.getString("csv_path_for_cloning", 1);
		
		if (csvPath == null || "".equalsIgnoreCase(csvPath))
		{
			throw new SapTestUtilRuntimeException("Unable to load CSV path: " + csvPath);
		}
		
		Table tblCsvData = Table.tableNew("CSV data");
		int ret = tblCsvData.inputFromCSVFile(csvPath);
		
		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			throw new SapTestUtilRuntimeException("Unable to load CSV file into JVS table: " + csvPath);
		}
		
		Util.updateTableWithColumnNames(tblCsvData);
		
		return tblCsvData;
	}

	@Override
	public Table performBulkOperation(Table tblInput) throws OException 
	{
		int numberOfRows = tblInput.getNumRows();
		int dealNum;		
		
		tblInput.addCol(TRADE_PRICE_COLUMN, COL_TYPE_ENUM.COL_STRING);
		tblInput.addCol(STATUS_COLUMN, COL_TYPE_ENUM.COL_STRING);
		tblInput.addCol(COMMENTS_COLUMN, COL_TYPE_ENUM.COL_STRING);
		tblInput.addCol(NEW_DEAL_NUMBER_COLUMN, COL_TYPE_ENUM.COL_INT);
		tblInput.addCol(REFERENCE_COLUMN, COL_TYPE_ENUM.COL_STRING);
		
		PluginLog.info("Cloning " + numberOfRows + " deals");

		for (int row = 1; row <= numberOfRows; row++)
		{
			
			String dealNumStr = tblInput.getString(CsvWrapper.DEAL_NUM_COLUMN, row);
			
			if (dealNumStr == null || "".equalsIgnoreCase(dealNumStr))
			{
				continue;
			}
			
			dealNum = Integer.valueOf(dealNumStr);
			updateDealDelta(dealNum, tblInput, row);
		}
		
		/* Clean up any locks that may occur as a result of clashing with OLF ops/field/event plugins */
		Util.unLockDeals();
		
		return tblInput;
	}
	
	/**
	 * Return a maps of Tran Info field name and default value. Only fields
	 * where default value is defined are included in the response
	 * 
	 * @return
	 * @throws OException
	 */
	private Map<String, String> getTranInfoDefaultValue() throws OException
	{
		String infoFieldValuesQuery = "SELECT type_name,default_value FROM tran_info_types WHERE (default_value IS NOT NULL) AND (default_value NOT LIKE '') ";
		PluginLog.debug("infoFieldValuesQuery=" + infoFieldValuesQuery);

		Table infoFieldDefaultValuesTable = com.olf.openjvs.Util.NULL_TABLE;
		Map<String, String> infoFieldDefaultValues = new HashMap<String, String>();
		try
		{
			infoFieldDefaultValuesTable = Table.tableNew();
			int iRetVal = DBaseTable.execISql(infoFieldDefaultValuesTable, infoFieldValuesQuery);
			if (iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue())
			{
				throw new OException(" Unable to execute query SQL. Return code= " + iRetVal + "." + infoFieldValuesQuery);
			}
			int numberOfRows = infoFieldDefaultValuesTable.getNumRows();

			for (int currentRow = 1; currentRow <= numberOfRows; currentRow++)
			{
				infoFieldDefaultValues.put(infoFieldDefaultValuesTable.getString("type_name", currentRow), infoFieldDefaultValuesTable.getString("default_value", currentRow));
			}
		}
		finally
		{
			infoFieldDefaultValuesTable.destroy();
		}
		return infoFieldDefaultValues;
	}
	
	public void updateDealDelta( int dealNum, Table tblInput, int row ) throws OException
	{
		int iRetVal;
		String searchOldTranQuery;
		Table tblTranNum;
		ToolsetFactory tFactory = new ToolsetFactory();
		CsvWrapper csvWrapper = new CsvWrapper(tblInput);
		Map<String, String> infoFieldDefaultValues = getTranInfoDefaultValue();
		String comments = EMPTY_STRING;
		
		searchOldTranQuery = null;
		if (dealNum > 0)
		{
			searchOldTranQuery = "select tran_num,deal_tracking_num,reference from ab_tran where deal_tracking_num =" + dealNum + " and current_flag = 1";
		}

		PluginLog.debug("searchOldTranQuery=" + searchOldTranQuery);
		
		tblTranNum = Table.tableNew();
		iRetVal = DBaseTable.execISql(tblTranNum, searchOldTranQuery);
		if (iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue())
		{
			throw new OException(" Unable to execute query SQL. Return code= " + iRetVal + "." + searchOldTranQuery);
		}

		if (tblTranNum.getNumRows() == 0)
		{
			PluginLog.warn("Original deal not found. dealNumber = " + dealNum);
			tblInput.setString(STATUS_COLUMN, row, "Failure");
			tblInput.setString(COMMENTS_COLUMN, row, "No deal found");
		}
		else
		{
			Transaction copyTransaction = com.olf.openjvs.Util.NULL_TRAN;
			Transaction clone = com.olf.openjvs.Util.NULL_TRAN;

			try
			{
				int transactionNumber = tblTranNum.getInt("tran_num", 1);
				dealNum = tblTranNum.getInt("deal_tracking_num", 1);
				
				PluginLog.debug("Started cloning. transactionNumber=" + transactionNumber + ",dealNumber=" + dealNum);
				copyTransaction = Transaction.retrieveCopy(transactionNumber);
				
				DealDelta dealDelta = csvWrapper.getDealDelta(row);
				dealDelta.setDealNum(dealNum);
				dealDelta.setTranNum(transactionNumber);

				PluginLog.debug("Test reference = " + dealDelta.getTestReference());
				
				ToolsetI toolset = tFactory.createToolset(copyTransaction, dealDelta);
				PluginLog.debug("ToolsetI=" + toolset.getClass().getName() + " for originalTranNum=" + transactionNumber);
				toolset.updateToolset();

				PluginLog.debug("Updating TransactionInfo for " + dealDelta.getTestReference());
				toolset.updateTransactionInfo(infoFieldDefaultValues);

				PluginLog.debug("Updating SSI's for " + dealDelta.getTestReference());
				toolset.copySettlementInstructions();
				clone = toolset.createClone();
				int newDealNumber = clone.getFieldInt(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.jvsValue());
				String refText = clone.getField(TRANF_FIELD.TRANF_REFERENCE.jvsValue());

				tblInput.setString(TRADE_PRICE_COLUMN, row, dealDelta.getTradePrice());
				tblInput.setInt(NEW_DEAL_NUMBER_COLUMN, row, newDealNumber);
				tblInput.setString(STATUS_COLUMN, row, "Success");
				tblInput.setString(COMMENTS_COLUMN, row, comments);
				tblInput.setString(REFERENCE_COLUMN, row, refText);
			}
			catch (OException oException)
			{
				Util.printStackTrace(oException);
				PluginLog.error("Transaction with deal number " + dealNum + " cloning failed." + oException.getMessage());
				comments += "Unable to clone.Exception occurred:" + oException.getMessage();
				tblInput.setString(STATUS_COLUMN, row, "Failure");
				tblInput.setString(COMMENTS_COLUMN, row, comments);
			}
			finally
			{
				if (Transaction.isNull(copyTransaction) != 1)
				{
					copyTransaction.destroy();
				}
				if (Transaction.isNull(clone) != 1)
				{
					clone.destroy();
				}
				if (Table.isTableValid(tblTranNum) == 1)
				{
					tblTranNum.destroy();
				}
			}
		}
	}
}