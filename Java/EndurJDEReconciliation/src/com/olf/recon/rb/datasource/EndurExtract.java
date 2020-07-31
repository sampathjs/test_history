package com.olf.recon.rb.datasource;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.recon.exception.ReconciliationRuntimeException;
import com.olf.jm.logging.Logging;

public abstract class EndurExtract extends ReportEngine{
	
	/**
	 * Retrieve applicable reference data for input table
	 * 
	 * @param tblData
	 * @throws OException
	 */
	protected void enrichSupplementaryData(Table tblData) throws OException
	{
		Table tblOutput = null;
		int queryId = 0;
		
		try
		{
			if (tblData.getNumRows() > 0)
			{
				queryId = Query.tableQueryInsert(tblData, "deal_num");
				
				if (queryId > 0)
				{
					tblOutput = Table.tableNew();
					
					String sqlQuery = 
						"SELECT \n" +
							"ab.deal_tracking_num AS deal_num, \n" +
							"ab.ins_type, " +
							"ab.tran_status, \n" +
							"ab.internal_lentity, \n" +
							"ab.internal_bunit, \n" +
							"ab.external_bunit \n" +
						"FROM query_result qr \n" +
						"JOIN ab_tran ab ON qr.query_result = ab.deal_tracking_num \n" +
						"WHERE qr.unique_id = " + queryId + " \n" +
						"AND ab.current_flag = 1";
					
					int ret = DBaseTable.execISql(tblOutput, sqlQuery);
					
					if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
					{
						throw new ReconciliationRuntimeException("Unable to run query: " + sqlQuery);
					}
					
					tblData.select(tblOutput, "ins_type, tran_status, internal_lentity, internal_bunit, external_bunit(counterparty)", "deal_num EQ $deal_num");
				}
			}	
		}
		finally
		{
			if (tblOutput != null)
			{
				tblOutput.destroy();
			}
			
			if (queryId > 0)
			{
				Query.clear(queryId);
			}
		}
	}
	
	/**
	 * Any deals that are generated and then cancelled on the same day should not be reported.
	 * 
	 * @param output
	 * @throws OException
	 */
	protected void removeSameDayCancellations(Table output) throws OException 
	{
		Table tblTemp = Table.tableNew("Deals cancelled on the same day");
		int queryId = 0;
		
		try
		{
			if (output.getNumRows() > 0)
			{
				queryId = Query.tableQueryInsert(output, "deal_num");
				String queryTableName = Query.getResultTableForId(queryId);
				
				String sqlQuery = 
					"SELECT \n" +
						"DISTINCT new_trades.* \n" + 
					"FROM \n" +
					"( \n" +
						"SELECT \n" + 
						"ab.deal_tracking_num AS deal_num, \n" + 
						"CAST(abh.row_creation AS DATE) as new_row_creation_date \n" + // cast = loose the time stamp
						"FROM \n" +
						"ab_tran_history abh, \n" +
						"ab_tran ab, \n" +
						queryTableName + " qr \n" +
						"WHERE abh.tran_num = ab.tran_num \n" +
						"AND ab.deal_tracking_num = qr.query_result \n" + 
						"AND qr.unique_id = " + queryId + " \n" +
						"AND abh.version_number = 1 \n" +
						" AND abh.tran_status IN (" + TRAN_STATUS_ENUM.TRAN_STATUS_PENDING.toInt() + ", " + TRAN_STATUS_ENUM.TRAN_STATUS_NEW.toInt() + ", " + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() + ") \n" +
					") new_trades \n" +
					"JOIN \n" +
					"( \n" +
						"SELECT \n" +
						"ab.deal_tracking_num AS deal_num, \n" + 
						"CAST(abh.row_creation AS DATE) as cancelled_row_creation_date \n" +
						"FROM \n" +
						"ab_tran_history abh, \n" + 
						"ab_tran ab, \n" +
						queryTableName + " qr \n" +
						"WHERE abh.tran_num = ab.tran_num \n" + 
						"AND ab.deal_tracking_num = qr.query_result \n" + 
						"AND qr.unique_id = " + queryId + " \n" +
						"AND abh.tran_status IN (" + TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt() + ") \n" +
						") cancelled_trades \n" +
					"ON new_trades.deal_num = cancelled_trades.deal_num AND new_trades.new_row_creation_date = cancelled_trades.cancelled_row_creation_date";

				int ret = DBaseTable.execISql(tblTemp, sqlQuery);
				
				if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				{
					throw new ReconciliationRuntimeException("Unable to load query: " + sqlQuery);
				}
				
				tblTemp.addCol("cancelled_on_same_day", COL_TYPE_ENUM.COL_INT);
				tblTemp.setColValInt("cancelled_on_same_day", 1);
				
				output.select(tblTemp, "cancelled_on_same_day", "deal_num EQ $deal_num");
				
				output.deleteWhereValue("cancelled_on_same_day", 1);				
			}
		}
		finally
		{
			if (tblTemp != null)
			{
				tblTemp.destroy();
			}
			
			if (queryId > 0)
			{
				Query.clear(queryId);
			}
		}
	}
	
	/**
	 * Remove non regional records based on a party info parameter
	 * 
	 * @param tblData
	 * @throws OException
	 */
	protected void removeNonRegionalRecords(Table tblData) throws OException 
	{
		int queryId = 0;
		Table tblOutput = null;
		
		try
		{
			if (tblData.getNumRows() > 0 && exclusionExternalBunitPartyInfo != null && exclusionExternalBunitPartyInfo.length() > 2)
			{
				Logging.info("Removing non regional records based on party info: " + exclusionExternalBunitPartyInfo);
				
				queryId = Query.tableQueryInsert(tblData, "counterparty");
				
				if (queryId > 0)
				{
					tblOutput = Table.tableNew("Party Info data");
					
					/* Enrich data with counterparty and party info values */
					String sqlQuery = 
						"SELECT \n" +
							"p.party_id, \n" + 
							"p.int_ext, \n" +
							"p.short_name, \n" +
							"party_info_data.value AS party_info_value \n" + 
							"FROM query_result qr \n" +
							"JOIN party p ON qr.query_result = p.party_id \n" + 
						"LEFT JOIN \n" +
						"( \n" +
							"SELECT \n" +
							"pi.* \n" +
							"FROM \n" +
							"party_info pi \n" +
							"JOIN party_info_types pit ON pi.type_id = pit.type_id \n" +
							"WHERE pit.type_name = '" + exclusionExternalBunitPartyInfo + "' \n" +
						") party_info_data \n" +
						"ON p.party_id = party_info_data.party_id \n" +
						"WHERE qr.unique_id = " + queryId + " \n" +
						"AND p.party_class = 1 -- Busines Unit"; 
							
					int ret = DBaseTable.execISql(tblOutput, sqlQuery);
					
					if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
					{
						throw new ReconciliationRuntimeException("Unable to run query: " + sqlQuery);
					}
					
					tblData.select(tblOutput, "int_ext, party_info_value", "party_id EQ $counterparty");
					
					/* 
					 * Remove rows where 
					 * 1. The counterparty is an External type
					 * 2. The party info value (driven of report builder) is empty or non existent 
					 */
					for (int row = tblData.getNumRows(); row >= 1; row--)
					{
						int intExt = tblData.getInt("int_ext", row);
						String partyInfoValue = tblData.getString("party_info_value", row);
					
						if (intExt == 1 && (partyInfoValue == null || "".equalsIgnoreCase(partyInfoValue)))
						{
							tblData.delRow(row);
						}
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
			
			if (tblOutput != null)
			{
				tblOutput.destroy();
			}
		}
	}
}
