package com.matthey.testutil.mains;

import com.matthey.testutil.BulkInstrumentFixings;
import com.matthey.testutil.enums.EndurTranInfoField;
import com.matthey.testutil.exception.SapTestUtilRuntimeException;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.sc.bo.docproc.BO_CommonLogic.Query;

/**
 * Utility for instrument fixings
 * @author KailaM01
 *
 */
public class InstrumentFixings extends BulkInstrumentFixings
{
	@Override
	public Table generateInputData() throws OException 
	{
		Table tblArgt = getArgt();
		
		String csvPath = tblArgt.getString("csv_path_for_instrument_fixings", 1);
		
		if (csvPath == null || "".equalsIgnoreCase(csvPath))
		{
			throw new SapTestUtilRuntimeException("Unable to load CSV path for Instrument Fixings: " + csvPath);
		}
		
		int queryId = -1;
		
		Table tblCsvData = Table.tableNew("CSV data");
		Table tblData = Table.tableNew("Deals to fix");
		
		try
		{
			int ret = tblCsvData.inputFromCSVFile(csvPath);
			
			tblData.addCols("I(deal_num) I(tran_num) I(tran_status) I(toolset) I(ins_type) S(sap_order_id)");
			 
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				throw new SapTestUtilRuntimeException("Unable to load CSV file into JVS table: " + csvPath);
			}
			
			/* Fix header column names! */
			com.matthey.testutil.common.Util.updateTableWithColumnNames(tblCsvData);
			
			int numRows = tblCsvData.getNumRows();
			for (int row = 1; row <= numRows; row++)
			{
				String sapOrderIdStr = tblCsvData.getString("sap_order_id", row);
				
				Table tblTemp = null;
				
				try
				{
					tblTemp = Table.tableNew("Temp data");
					
					int today = OCalendar.today();
					
					String sqlQuery = 
							"SELECT \n" +
								"ab.deal_tracking_num AS deal_num, \n" +
								"ab.tran_num, \n" +
								"ab.tran_status, \n" +
								"ab.toolset, \n" +
								"ab.ins_type, \n" +
								"ati.value AS sap_order_id, \n" +
								"r.reset_date \n" +
							"FROM \n" +
								"ab_tran_info ati, \n" +
								"ab_tran ab, \n" +
								"reset r \n" +
							"WHERE ati.tran_num = ab.tran_num \n" +
							"AND r.ins_num = ab.ins_num \n" +
							"AND r.reset_seq_num > 0 \n" +
							"AND ati.type_id = " + EndurTranInfoField.SAP_ORDER_ID.toInt() + " \n" +
							"AND ati.value = '" + sapOrderIdStr + "' \n" +
							"AND ab.current_flag = 1 \n" +
							"AND r.reset_date = '" + OCalendar.formatJdForDbAccess(today) + "'";
					
					ret = DBaseTable.execISql(tblTemp, sqlQuery);
					
					if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
					{
						throw new SapTestUtilRuntimeException("Unable to load query: " + sqlQuery);
					}
					
					tblTemp.copyRowAddAllByColName(tblData);
				}
				finally
				{
					if (tblTemp != null)
					{
						tblTemp.destroy();	
					}
				}
			}
		}
		catch (Exception e)
		{
			throw new SapTestUtilRuntimeException("Unable to run Instrument Fixings", e);
		}
		finally
		{
			if (queryId > 0)
			{
				Query.clear(queryId);
			}
			
			if (tblCsvData != null)
			{
				tblCsvData.destroy();
			}
		}
		
		return tblData;
	}
	
	@Override
	public Table performBulkOperation(Table tblInputData) throws OException 
	{
		com.matthey.testutil.common.Util.unLockDeals();
		
		return super.performBulkOperation(tblInputData);
	}
}