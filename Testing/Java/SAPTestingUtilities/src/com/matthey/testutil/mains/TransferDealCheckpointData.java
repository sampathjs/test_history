package com.matthey.testutil.mains;

import com.matthey.testutil.enums.EndurTranInfoField;
import com.matthey.testutil.exception.SapTestUtilRuntimeException;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TOOLSET_ENUM;

/**
 * This utility creates CSV files containing deal data
 * @author KailaM01
 *
 */
public class TransferDealCheckpointData extends GenerateDealCheckpointData
{
	@Override
	public Table performBulkOperation(Table tblInput) throws OException 
	{			
		Table tblStrategyFilter = null;
		Table tblStrategyData = Table.tableNew();
		
		try
		{
			tblStrategyFilter = Table.tableNew();
			tblStrategyFilter.select(tblInput, "*", "toolset EQ " + TOOLSET_ENUM.COMPOSER_TOOLSET.toInt());
			
			tblStrategyData = getStrategyData(tblStrategyFilter);

			tblStrategyData.defaultFormat();
		}
		catch (Exception e)
		{
			throw new SapTestUtilRuntimeException("Error encountered during transfer deal checkping data generator", e);
		}
		finally
		{
			if (tblStrategyFilter != null)
			{
				tblStrategyFilter.destroy();
			}
		}

		return tblStrategyData;
	}
	
	/**
	 * Generate a trade listing for Strategy deals. These are custom input deals and tran info's make up most of
	 * the necessary attributes for these
	 * 
	 * @param tblStrategyFilter
	 * @return
	 * @throws OException
	 */
	/**
	 * @param tblStrategyFilter
	 * @return
	 * @throws OException
	 */
	private Table getStrategyData(Table tblStrategyFilter) throws OException 
	{
		Table tblData = Table.tableNew();;
		int queryId = -1;
		
		try
		{
			if (tblStrategyFilter.getNumRows() > 0)
			{
				queryId = Query.tableQueryInsert(tblStrategyFilter, "tran_num");
				
				if (queryId > 0)
				{
					String sqlQuery = 
						"SELECT \n" +
							"ati1.value AS metal_transfer_request_number, \n" +
							"ab.deal_tracking_num AS deal_num, \n" +
							"ab.tran_num, \n" +
							"ab.tran_status, \n" +
							"ab.toolset, \n" +
							"ab.ins_type, \n" +
							"ab.internal_bunit, \n" +
							"ab.external_bunit, \n" +
							"ab.internal_lentity, \n" +
							"ab.external_lentity, \n" +
							"ab.settle_date, \n" +
							"ati2.value AS metal_currency, \n" +
							"ati3.value AS weight, \n" +
							"ati4.value AS unit, \n" +
							"ati5.value AS from_acc, \n" +
							"ati6.value AS from_acc_bu, \n" +
							"ati7.value AS from_acc_form, \n" +
							"ati8.value AS from_acc_loco, \n" +						
							"ati9.value AS to_acc, \n" +
							"ati10.value AS to_acc_bu, \n" +
							"ati11.value AS to_acc_form, \n" +
							"ati12.value AS to_acc_loco \n" +
						"FROM \n" +
							Query.getResultTableForId(queryId) + " qr JOIN ab_tran ab ON ab.tran_num = qr.query_result \n" +
							"LEFT JOIN ab_tran_info ati1 ON ab.tran_num = ati1.tran_num AND ati1.type_id = " + EndurTranInfoField.SAP_METAL_TRANSFER_REQUEST_NUMBER.toInt() + " \n" +
							"LEFT JOIN ab_tran_info ati2 ON ab.tran_num = ati2.tran_num AND ati2.type_id = " + EndurTranInfoField.METAL.toInt() + " \n" +
							"LEFT JOIN ab_tran_info ati3 ON ab.tran_num = ati3.tran_num AND ati3.type_id = " + EndurTranInfoField.QTY.toInt() + " \n" +
							"LEFT JOIN ab_tran_info ati4 ON ab.tran_num = ati4.tran_num AND ati4.type_id = " + EndurTranInfoField.UNIT.toInt() + " \n" +
							"LEFT JOIN ab_tran_info ati5 ON ab.tran_num = ati5.tran_num AND ati5.type_id = " + EndurTranInfoField.FROM_ACC.toInt() + " \n" +
							"LEFT JOIN ab_tran_info ati6 ON ab.tran_num = ati6.tran_num AND ati6.type_id = " + EndurTranInfoField.FROM_ACC_BU.toInt() + " \n" +
							"LEFT JOIN ab_tran_info ati7 ON ab.tran_num = ati7.tran_num AND ati7.type_id = " + EndurTranInfoField.FROM_ACC_FORM.toInt() + " \n" +
							"LEFT JOIN ab_tran_info ati8 ON ab.tran_num = ati8.tran_num AND ati8.type_id = " + EndurTranInfoField.FROM_ACC_LOCO.toInt() + " \n" +
							"LEFT JOIN ab_tran_info ati9 ON ab.tran_num = ati9.tran_num AND ati9.type_id = " + EndurTranInfoField.TO_ACC.toInt() + " \n" +
							"LEFT JOIN ab_tran_info ati10 ON ab.tran_num = ati10.tran_num AND ati10.type_id = " + EndurTranInfoField.TO_ACC_BU.toInt() + " \n" +
							"LEFT JOIN ab_tran_info ati11 ON ab.tran_num = ati11.tran_num AND ati11.type_id = " + EndurTranInfoField.TO_ACC_FORM.toInt() + " \n" +
							"LEFT JOIN ab_tran_info ati12 ON ab.tran_num = ati12.tran_num AND ati12.type_id = " + EndurTranInfoField.TO_ACC_LOCO.toInt() + " \n" +
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
		return null;
	}
}
