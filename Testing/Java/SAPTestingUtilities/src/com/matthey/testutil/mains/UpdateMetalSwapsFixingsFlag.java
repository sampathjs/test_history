package com.matthey.testutil.mains;

import com.matthey.testutil.BulkOperationScript;
import com.matthey.testutil.common.Util;
import com.matthey.testutil.enums.EndurTranInfoField;
import com.matthey.testutil.exception.SapTestUtilRuntimeException;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.jm.logging.Logging;

/**
 * Updates the fixing flag in user_jm_jde_extract_data to Y when a metal swap has been fixed
 * @author KailaM01
 */
public class UpdateMetalSwapsFixingsFlag extends BulkOperationScript
{
	@Override
	public Table generateInputData() throws OException 
	{
		Table tblArgt = getArgt();
		
		String csvPath = tblArgt.getString("csv_path_for_fixing_flag", 1);
		
		if (csvPath == null || "".equalsIgnoreCase(csvPath))
		{
			throw new SapTestUtilRuntimeException("Unable to load CSV path: " + csvPath);
		}
		
		int queryId = -1;
		
		Table tblCsvData = Table.tableNew("CSV data");
		Table tblData = Table.tableNew("Deals to cancel");
		Table tblCoverageDeals = null;
		
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
		}
		
		return tblData;
	}

	/**
	 * Return coverage deals from the CSV input
	 * 
	 * @param tblCsvData
	 * @return
	 * @throws OException
	 */
	/**
	 * @param tblCsvData
	 * @return
	 * @throws OException
	 */
	public static Table getCoverageDeals(Table tblCsvData) throws OException
	{
		return loadDeals(tblCsvData, EndurTranInfoField.SAP_ORDER_ID);
	}
	
	/**
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
			throw new SapTestUtilRuntimeException("Unable to load transfer deals for cancellations", e);
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
	public Table performBulkOperation(Table tblInputData) throws OException 
	{
		int ret;
		int numRows = tblInputData.getNumRows();
		final String DEAL_NUM_COLUMN = "deal_num";
		final String FIXINGS_COMPLETE_COLUMN = "fixings_complete"; 
		Logging.debug("Number of deals to update fixings flag for: " + numRows);
		
		Table tblJdeExtractData = null;
		
			try
			{
				tblJdeExtractData = Table.tableNew("USER_jm_jde_extract_data");
				tblJdeExtractData.addCol(DEAL_NUM_COLUMN, COL_TYPE_ENUM.COL_INT);
				tblJdeExtractData.addCol(FIXINGS_COMPLETE_COLUMN, COL_TYPE_ENUM.COL_STRING);
				
				for (int row = 1; row <= numRows; row++)
				{
					int dealNum = tblInputData.getInt("deal_num", row);
					
					int newRow = tblJdeExtractData.addRow();
					tblJdeExtractData.setInt(DEAL_NUM_COLUMN, newRow, dealNum);
					tblJdeExtractData.setString(FIXINGS_COMPLETE_COLUMN, newRow, "Y");
				}
				
				if (tblJdeExtractData.getNumRows() > 0)
				{
					tblJdeExtractData.group(DEAL_NUM_COLUMN);

					ret = DBUserTable.update(tblJdeExtractData);	
					
					if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
					{
						throw new SapTestUtilRuntimeException("Unable to save changes to USER_jm_jde_extract_data");
					}
				}
			}
			catch (Exception e)
			{
				Util.printStackTrace(e);
				throw new SapTestUtilRuntimeException("Error encountered during performBulkOperation in UpdateMetalSwapsFixingFlag", e);
			}
		
		
		return tblJdeExtractData;
	}

	@Override
	public String getOperationName() 
	{
		return "Metal Swaps Fixing Flag";
	}
}
