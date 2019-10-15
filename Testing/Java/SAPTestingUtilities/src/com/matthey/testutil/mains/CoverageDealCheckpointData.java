package com.matthey.testutil.mains;

import com.matthey.testutil.common.SAPTestUtilitiesConstants;
import com.matthey.testutil.enums.EndurTranInfoField;
import com.matthey.testutil.exception.SapTestUtilRuntimeException;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.INS_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;

/**
 * Generate deal data for all coverage deals that have been cloned
 * or injected via the test pack
 * @author KailaM01
 */
public class CoverageDealCheckpointData extends GenerateDealCheckpointData
{
	private final String TRAN_NUM = "tran_num";
	
	@Override
	public Table performBulkOperation(Table tblInput) throws OException 
	{	
		Table tblOutputData = createOutputFormat();
		
		Table tblFilterFxDeals = null;
		Table tblFilterMetalSwaps = null;
		
		Table tblFxData = null;
		Table tblMetalSwapsData = null;
		
		try
		{
			tblFilterFxDeals = Table.tableNew("FX filter");
			tblFilterFxDeals.select(tblInput, "*", "ins_type EQ " + INS_TYPE_ENUM.fx_instrument.toInt());
			tblFxData = getFxDeals(tblFilterFxDeals);
			adjustFxPosition(tblFxData);
			tblFxData.copyRowAddAllByColName(tblOutputData);
			
			tblFilterMetalSwaps = Table.tableNew("Metal Swaps filter");
			tblFilterMetalSwaps.select(tblInput, "*", "ins_type EQ " + INS_TYPE_ENUM.metal_swap.toInt());
			tblMetalSwapsData = getMetalSwaps(tblFilterMetalSwaps);
			tblMetalSwapsData.copyRowAddAllByColName(tblOutputData);

			tblOutputData.defaultFormat();		
			tblOutputData.setColFormatAsRef("metal_currency", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
			tblOutputData.setColFormatAsRef("financial_currency", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
			tblOutputData.setColFormatAsRef("uom", SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE);
		}
		catch (Exception e)
		{
			throw new SapTestUtilRuntimeException("Error encountered during coverage deal checkping data generator", e);
		}
		finally
		{
			if (tblFilterFxDeals != null)
			{
				tblFilterFxDeals.destroy();
			}
			
			if (tblFilterMetalSwaps != null)
			{
				tblFilterMetalSwaps.destroy();
			}
			
			if (tblFxData != null)
			{
				tblFxData.destroy();
			}

			if (tblMetalSwapsData != null)
			{
				tblMetalSwapsData.destroy();
			}		
		}

		return tblOutputData;
	}

	/**
	 * Generate a trade listing for FX coverage deals, with some specific trade attributes 
	 * 
	 * @param tblFxDeals
	 * @return
	 * @throws OException
	 */
	private Table getFxDeals(Table tblFxDeals) throws OException 
	{
		Table tblData = Table.tableNew("FX Deals");
		int queryId = -1;
		
		try
		{
			if (tblFxDeals.getNumRows() > 0)
			{
				queryId = Query.tableQueryInsert(tblFxDeals, TRAN_NUM);
				
				if (queryId > 0)
				{
					String sqlQuery = 
						"SELECT \n" +
							"ati1.value AS sap_order_id, \n" +
							"ati2.value AS is_coverage, \n" +
							"ab.deal_tracking_num AS deal_num, \n" +
							"ab.tran_num, \n" +
							"ab.tran_status, \n" +
							"ab.toolset, \n" +
							"ab.ins_type, \n" +
							"ab.buy_sell, \n" +
							"ab.cflow_type, \n" +
							"ab.internal_bunit, \n" +
							"ab.external_bunit, \n" +
							"ab.internal_lentity, \n" +
							"ab.external_lentity, \n" +
							"ab.trade_date, \n" +
							"ab.settle_date AS value_date, \n" +
							"ab.currency AS metal_currency, \n" +
							"fx.ccy2 AS financial_currency, \n" +
							"ab.position AS metal_position, \n" +
							"ab.unit AS uom \n" +
						"FROM \n" +
						Query.getResultTableForId(queryId) + " qr JOIN ab_tran ab ON ab.tran_num = qr.query_result \n" +
						"JOIN fx_tran_aux_data fx ON ab.tran_num = fx.tran_num \n" +
						"LEFT JOIN ab_tran_info ati1 ON ab.tran_num = ati1.tran_num AND ati1.type_id = " + EndurTranInfoField.SAP_ORDER_ID.toInt() + " \n" +
						"LEFT JOIN ab_tran_info ati2 ON ab.tran_num = ati2.tran_num AND ati2.type_id = " + EndurTranInfoField.IS_COVERAGE.toInt() + " \n" +
						"WHERE qr.unique_id = " + queryId + " \n" +
						"AND ab.current_flag = 1 \n" +
						"AND ab.toolset = " + TOOLSET_ENUM.FX_TOOLSET.toInt();

					int ret = DBaseTable.execISql(tblData, sqlQuery);
					
					if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
					{
						throw new SapTestUtilRuntimeException("Unable to load query: " + sqlQuery);
					}
				}				
			}
		}
		finally
		{
			if (queryId > 0)
			{
				Query.clear(queryId);
			}
		}
		
		return tblData;
	}
	
	/**
	 * Generate a trade listing for metal swap coverage deals, with some specific trade attributes 
	 * 
	 * @param tblFilterMetalSwaps
	 * @return
	 * @throws OException
	 */
	private Table getMetalSwaps(Table tblFilterMetalSwaps) throws OException 
	{
		Table tblData = Table.tableNew();;
		int queryId = -1;
		
		try
		{
			if (tblFilterMetalSwaps.getNumRows() > 0)
			{
				queryId = Query.tableQueryInsert(tblFilterMetalSwaps, TRAN_NUM);
				
				if (queryId > 0)
				{
					String sqlQuery = 
						"SELECT \n" +
							"ati1.value AS sap_order_id, \n" +
							"ati2.value AS is_coverage, \n" +
							"ab.deal_tracking_num AS deal_num, \n" +
							"ab.tran_num, \n" +
							"ab.tran_status, \n" +
							"ab.toolset, \n" +
							"ab.ins_type, \n" +
							"ab.buy_sell, \n" +
							"ab.cflow_type, \n" +
							"ab.internal_bunit, \n" +
							"ab.external_bunit, \n" +
							"ab.internal_lentity, \n" +
							"ab.external_lentity, \n" +
							"ab.trade_date, \n" +
							"ab.start_date AS fixing_date, \n" +
							"ab.currency AS metal_currency, \n" +
							"ab.position AS metal_position, \n" +
							"p.currency AS financial_currency," +
							"p.proj_index, \n" +
							"prh.ref_source, \n" +
							"p.price_unit AS uom \n" +
						"FROM \n" +
							Query.getResultTableForId(queryId) + " qr JOIN ab_tran ab ON ab.tran_num = qr.query_result \n" +
							"JOIN parameter p ON ab.ins_num = p.ins_num AND p.param_seq_num = 1 \n" +
							"JOIN param_reset_header prh ON ab.ins_num = prh.ins_num AND prh.param_seq_num = 1 \n" +
							"LEFT JOIN ab_tran_info ati1 ON ab.tran_num = ati1.tran_num AND ati1.type_id = " + EndurTranInfoField.SAP_ORDER_ID.toInt() + " \n" +
							"LEFT JOIN ab_tran_info ati2 ON ab.tran_num = ati2.tran_num AND ati2.type_id = " + EndurTranInfoField.IS_COVERAGE.toInt() + " \n" +
						"WHERE qr.unique_id = " + queryId + " \n" +
						"AND ab.current_flag = 1";	

					int ret = DBaseTable.execISql(tblData, sqlQuery);
					
					if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
					{
						throw new SapTestUtilRuntimeException("Unable to load query: " + sqlQuery);
					}
				}
			}
		}
		finally
		{
			if (queryId > 0)
			{
				Query.clear(queryId);
			}
		}
		
		return tblData;
	}

	@Override
	public Table createOutputFormat() throws OException 
	{
		Table tblData = Table.tableNew("Output Data");
		
		tblData.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		tblData.addCol("tran_num", COL_TYPE_ENUM.COL_INT);
		tblData.addCol("sap_order_id", COL_TYPE_ENUM.COL_STRING);
		tblData.addCol("is_coverage", COL_TYPE_ENUM.COL_STRING);
		tblData.addCol("tran_status", COL_TYPE_ENUM.COL_INT);
		tblData.addCol("toolset", COL_TYPE_ENUM.COL_INT);
		tblData.addCol("ins_type", COL_TYPE_ENUM.COL_INT);
		tblData.addCol("buy_sell", COL_TYPE_ENUM.COL_INT);
		tblData.addCol("cflow_type", COL_TYPE_ENUM.COL_INT);
		tblData.addCol("internal_bunit", COL_TYPE_ENUM.COL_INT);
		tblData.addCol("external_bunit", COL_TYPE_ENUM.COL_INT);
		tblData.addCol("internal_lentity", COL_TYPE_ENUM.COL_INT);
		tblData.addCol("external_lentity", COL_TYPE_ENUM.COL_INT);
		tblData.addCol("trade_date", COL_TYPE_ENUM.COL_DATE_TIME);
		tblData.addCol("value_date", COL_TYPE_ENUM.COL_DATE_TIME);
		tblData.addCol("fixing_date", COL_TYPE_ENUM.COL_DATE_TIME);
		tblData.addCol("metal_currency", COL_TYPE_ENUM.COL_INT);
		tblData.addCol("financial_currency", COL_TYPE_ENUM.COL_INT);
		tblData.addCol(SAPTestUtilitiesConstants.METAL_POSITION, COL_TYPE_ENUM.COL_DOUBLE);
		tblData.addCol("uom", COL_TYPE_ENUM.COL_INT);
		tblData.addCol("proj_index", COL_TYPE_ENUM.COL_INT);
		tblData.addCol("ref_source", COL_TYPE_ENUM.COL_INT);
		
		return tblData;
	}

	/**
	 * The db stores all values as Toz (base unit) in ab_tran_events. So if this is a Kg trade (or any other unit different to Toz), 
	 * convert from Toz > trade unit 
	 *			 
	 * @param tblData
	 * @throws OException
	 */
	private void adjustFxPosition(Table tblData) throws OException 
	{
		int toz = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "TOz");
		
		int numRows = tblData.getNumRows();
		for (int row = 1; row <= numRows; row++)
		{
			int metalUnit = tblData.getInt("uom", row);
			double metalPosition = tblData.getDouble(SAPTestUtilitiesConstants.METAL_POSITION, row);
			
			if (metalUnit != toz)
			{
				metalPosition *= Transaction.getUnitConversionFactor(toz, metalUnit);
			}

			tblData.setDouble(SAPTestUtilitiesConstants.METAL_POSITION, row, metalPosition);				
		}
	}
}
