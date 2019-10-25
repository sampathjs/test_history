package com.matthey.testutil.mains;

import com.matthey.testutil.BulkCancelDeleteDeals;
import com.matthey.testutil.enums.EndurTranInfoField;
import com.matthey.testutil.exception.SapTestUtilRuntimeException;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;

/**
 * Cancel SAP deals based on;
 * 1. A list of input SAP Order Id's
 * 2. A list of input Metal Transfer Request numbers
 * 3. A list of input reference strings 
 */
public class CancelSapDeals extends BulkCancelDeleteDeals
{
	@Override
	public Table generateInputData() throws OException 
	{
		Table tblArgt = getArgt();
		
		String csvPath = tblArgt.getString("csv_path_for_cancellations", 1);
		
		if (csvPath == null || "".equalsIgnoreCase(csvPath))
		{
			throw new SapTestUtilRuntimeException("Unable to load CSV path: " + csvPath);
		}
		
		int queryId = -1;
		
		Table tblCsvData = Table.tableNew("CSV data");
		Table tblData = Table.tableNew("Deals to cancel");
		
		Table tblCoverageDeals = null;
		Table tblTransferDeals = null;
		Table tblReferenceDeals = null;
		
		try
		{
			int ret = tblCsvData.inputFromCSVFile(csvPath);
			
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				throw new SapTestUtilRuntimeException("Unable to load CSV file into JVS table: " + csvPath);
			}
			
			/* Fix header column names! */
			com.matthey.testutil.common.Util.updateTableWithColumnNames(tblCsvData);
			
			tblCoverageDeals = getCoverageDeals(tblCsvData);
			tblTransferDeals = getTransferDeals(tblCsvData);
			tblReferenceDeals = getReferenceDeals(tblCsvData);
			
			/* Merge everything into one table */
			tblTransferDeals.copyRowAddAllByColName(tblCoverageDeals);
			tblReferenceDeals.copyRowAddAllByColName(tblCoverageDeals);
			
			tblData.select(tblCoverageDeals, "*", "tran_num GT -1");
		}
		catch (Exception e)
		{
			throw new SapTestUtilRuntimeException("Unable to run generateInputData() for cancellations", e);
		}
		finally
		{
			if (queryId > 0)
			{
				Query.clear(queryId);
			}
			
				tblCsvData.destroy();
			
			if (tblCoverageDeals != null)
			{
				tblCoverageDeals.destroy();
			}
			
			if (tblTransferDeals != null)
			{
				tblTransferDeals.destroy();
			}
			
			if (tblReferenceDeals != null)
			{
				tblReferenceDeals.destroy();
			}
		}
		
		return tblData;
	}

	/**
	 * Return coverage deals from the CSV input, based on SAP orders
	 * 
	 * @param tblCsvData
	 * @return
	 * @throws OException
	 */
	public static Table getCoverageDeals(Table tblCsvData) throws OException
	{
		return loadDeals(tblCsvData, EndurTranInfoField.SAP_ORDER_ID);
	}
	
	/**
	 * Return transfer deals from the CSV input, based on Metal transfer requests
	 * 
	 * @param tblCsvData
	 * @return
	 * @throws OException
	 */
	public static Table getTransferDeals(Table tblCsvData) throws OException
	{
		return loadDeals(tblCsvData, EndurTranInfoField.SAP_METAL_TRANSFER_REQUEST_NUMBER);
	}
	
	/**
	 * Looks up Endur deals given a list of values for a specific tran info
	 * 
	 * @param tblCsvData
	 * @param tranInfo
	 * @return
	 * @throws OException
	 */
	private static Table loadDeals(Table tblCsvData, EndurTranInfoField tranInfo) throws OException
	{
		Table tblDeals = Table.tableNew("Deals");
		tblDeals.addCols("I(deal_num) I(tran_num) I(tran_status) I(toolset) I(ins_type)");
		
		Table tblSplitCsvData = null;
		
		try
		{
			tblSplitCsvData = Table.tableNew();
			tblSplitCsvData.select(tblCsvData, "*", "tran_info_name EQ " + tranInfo.toString());
			
			int numRows = tblSplitCsvData.getNumRows();
			for (int row = 1; row <= numRows; row++)
			{
				String tranInfoValue = tblSplitCsvData.getString("tran_info_value", row);
				
				Table tblTempData = null;
				
				try
				{
					tblTempData = Table.tableNew("Temp data");
					
					String sqlQuery = 
							"SELECT \n" +
								"ab.deal_tracking_num AS deal_num, \n" +
								"ab.tran_num, \n" +
								"ab.tran_status, \n" +
								"ab.toolset, \n" +
								"ab.ins_type, \n" +
								"ab.internal_bunit, \n" +
								"ab.external_bunit, \n" +
								"ab.internal_lentity, \n" +
								"ab.external_lentity, \n" +
								"ati.value \n" +
							"FROM \n" +
								"ab_tran_info ati, \n" +
								"ab_tran ab \n" +
							"WHERE ati.tran_num = ab.tran_num \n" +
							"AND ati.type_id = " + tranInfo.toInt() + " \n" +
							"AND ati.value = '" + tranInfoValue + "' \n" +
							"AND ab.current_flag = 1";
					
					int ret = DBaseTable.execISql(tblTempData, sqlQuery);
					
					if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
					{
						throw new SapTestUtilRuntimeException("Unable to load query: " + sqlQuery);
					}
					
					tblTempData.copyRowAddAllByColName(tblDeals);
				}
				finally
				{	
					if (tblTempData != null)
					{
						tblTempData.destroy();
					}
				}
			}
		}
		catch (Exception e)
		{
			throw new SapTestUtilRuntimeException("Unable to load deals for tran info: " + tranInfo.toString(), e);
		}
		finally
		{
			if (tblSplitCsvData != null)
			{
				tblSplitCsvData.destroy();
			}
		}
		
		return tblDeals;
	}
	
	/**
	 * Returns a list of deals based on the reference field matching 'Clone_TC%'
	 * 
	 * @param tblCsvData
	 * @return
	 * @throws OException
	 */
	public static Table getReferenceDeals(Table tblCsvData) throws OException 
	{
		Table tblDeals = Table.tableNew("Deals");
		tblDeals.addCols("I(deal_num) I(tran_num) I(tran_status) I(toolset) I(ins_type)");
		
		Table tblSplitCsvData = null;
		
		try
		{
			tblSplitCsvData = Table.tableNew();
			tblSplitCsvData.select(tblCsvData, "*", "tran_info_name EQ reference");
			
			int numRows = tblSplitCsvData.getNumRows();
			for (int row = 1; row <= numRows; row++)
			{
				String tranInfoValue = tblSplitCsvData.getString("tran_info_value", row);
				
				Table tblTempData = null;
				
				try
				{
					tblTempData = Table.tableNew("Temp data");
					
					String sqlQuery = 
							"SELECT \n" +
								"ab.deal_tracking_num AS deal_num, \n" +
								"ab.tran_num, \n" +
								"ab.tran_status, \n" +
								"ab.toolset, \n" +
								"ab.ins_type, \n" +
								"ab.internal_bunit, \n" +
								"ab.external_bunit, \n" +
								"ab.internal_lentity, \n" +
								"ab.external_lentity \n" +
							"FROM ab_tran ab \n" +
							"WHERE ab.reference LIKE 'Clone_" + tranInfoValue + "%' \n" +
							"AND ab.current_flag = 1";
					
					int ret = DBaseTable.execISql(tblTempData, sqlQuery);
					
					if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
					{
						throw new SapTestUtilRuntimeException("Unable to load query: " + sqlQuery);
					}
					
					tblTempData.copyRowAddAllByColName(tblDeals);
				}
				finally
				{	
					if (tblTempData != null)
					{
						tblTempData.destroy();
					}
				}
			}
		}
		catch (Exception e)
		{
			throw new SapTestUtilRuntimeException("Unable to load reference deals for cancellations", e);
		}
		finally
		{
			if (tblSplitCsvData != null)
			{
				tblSplitCsvData.destroy();
			}
		}
		
		return tblDeals;
	}

	@Override
	public TRAN_STATUS_ENUM getDestinationStatus() 
	{
		return null;
	}
	
	@Override
	public Table performBulkOperation(Table tblInputData) throws OException 
	{
		com.matthey.testutil.common.Util.unLockDeals();

		return super.performBulkOperation(tblInputData);
	}
}
