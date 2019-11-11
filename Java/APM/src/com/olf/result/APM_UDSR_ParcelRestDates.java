/* Released with version 27-Feb-2019_V17_0_7 of APM */
/*
File Name:                 APM_UDSR_ParcelRestDates.java 

Date Of Last Revision:     27-July-2015 
			   			   
Script category:           Simulation Result
Script Type:               Main
Description:               User defined Sim Result which brings back parcel reset dates from core db tables.
                           
 */

package com.olf.result;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.olf.result.APMUtility.APMUtility;

public class APM_UDSR_ParcelRestDates implements IScript {
  
/*-------------------------------------------------------------------------------
Name:          main()
Description:   Parcel reset dates UDSR Main
Parameters:      
Return Values: returnt is a global table  
-------------------------------------------------------------------------------*/
	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();

		int operation;

		operation = argt.getInt( "operation", 1);

		if ( operation == USER_RESULT_OPERATIONS.USER_RES_OP_CALCULATE.toInt() )
			compute_result_using_db(argt, returnt);
		else if ( operation == USER_RESULT_OPERATIONS.USER_RES_OP_FORMAT.toInt() )
			format_result(returnt);

		Util.exitSucceed();
	}

	/*-------------------------------------------------------------------------------
	Name:          compute_result_using_db()
	Description:   reset dates result.
	Parameters:    Table argt, Table returnt  
	Return Values:   
	-------------------------------------------------------------------------------*/
	void compute_result_using_db(Table argt, Table returnt) throws OException
	{
	   Table    tTranNums;        /* Not freeable */
	   Table    tSimDef;          /* Not freeable */
	   int      iQueryId=0;
	   int      iRetVal;
	   String   sRequest;

	   tTranNums   = argt.getTable( "transactions", 1);
	   tSimDef     = argt.getTable( "sim_def", 1);
	        
	   /* If query ID is provided as a parameter, use it! */
	   if (tSimDef.getColNum( "APM Single Deal Query") > 0)
	      iQueryId = tSimDef.getInt( "APM Single Deal Query", 1);

	   /* If query ID was not set or left at zero, create a query ID from the list of transactions */
	   if (iQueryId == 0)
	   {  
	      /* build up query result to get deals from ab.tran which match our sim result. */
	      iQueryId = APMUtility.getQueryID(tTranNums, "tran_num");
	   }
	   
	   if (iQueryId > 0)
	   {
		   Table nonPhysTbl = Table.tableNew();
		   Table resetTbl = Table.tableNew();
		   String isNull = com.olf.openjvs.DBase.getDbType() == DBTYPE_ENUM.DBTYPE_ORACLE.toInt() ? "nvl" : "isnull";
		   sRequest = "SELECT DISTINCT ab_tran.deal_tracking_num dealnum, ab_tran.ins_num, ifp.param_seq_num leg, ab_tran.toolset, prh.proj_index, pc.cflow_id ins_source_id, " +
			   "ipidx.reset_start_date parcel_reset_start_date, ipidx.reset_end_date parcel_reset_end_date, ifp.parcel_id, " +
                           isNull + "(idx.inheritance, 0) inheritance, prh.reset_conv, prh.avg_type, prh.param_reset_header_seq_num prh_seq_num " +
			   " FROM ins_price_idx ipidx, physcash pc, query_result qr, ins_fee_param ifp, ins_price ip, ab_tran, param_reset_header prh " +
			   " LEFT JOIN idx_def idx on idx.index_id = prh.proj_index AND idx.db_status = 2" +
			   " where qr.query_result = ab_tran.tran_num AND  qr.unique_id = " + iQueryId +
			   " AND ab_tran.ins_num = ifp.ins_num AND ifp.ins_num = ipidx.ins_num AND ipidx.ins_num = ip.ins_num " +
			   " AND ipidx.ins_num = prh.ins_num AND ipidx.param_seq_num = prh.param_seq_num " +
			   " AND ipidx.ins_index_usage_seq_num = prh.param_reset_header_seq_num AND ifp.param_seq_num = ipidx.param_seq_num  " +
			   " AND ifp.param_seq_num = ip.param_seq_num AND ipidx.ins_price_seq_num = ip.ins_price_seq_num  " +
			   " AND pc.ins_num = ip.ins_num AND pc.param_seq_num = ip.param_seq_num AND pc.pricing_group_num = ip.pricing_group_num " +
			   " AND ip.pricing_source_id = ifp.fee_seq_num and pricing_source = 1 ";
		   
		   String getResetSql = "select r.ins_num, r.param_seq_num, r.profile_seq_num, r.param_reset_header_seq_num prh_seq_num, r.reset_seq_num, r.reset_date, r.value_status " +
			   " from ab_tran, reset r, query_result qr where qr.query_result = ab_tran.tran_num AND qr.unique_id = " + iQueryId + 
			   " and r.ins_num = ab_tran.ins_num and r.block_end = 0";
		   try
		   {
			   iRetVal = DBase.runSql(sRequest);
			   iRetVal = DBase.createTableOfQueryResults(returnt);
			   
			   iRetVal = DBase.runSql(getResetSql);
			   iRetVal = DBase.createTableOfQueryResults(resetTbl);
			   
			   
			   /*   get floating side only for non physical deals. */
			   sRequest = "SELECT DISTINCT ab.deal_tracking_num dealnum, ab.ins_num, pm.param_seq_num leg, ab.toolset, pm.proj_index, pf.profile_seq_num ins_source_id, " + 
					   "pf.start_date parcel_reset_start_date, pf.end_date parcel_reset_end_date, 0 parcel_id, " +
					   isNull + "(idx.inheritance, 0) inheritance " +
					   " FROM ab_tran ab, query_result qr, profile pf, parameter pm " +
					   " LEFT JOIN idx_def idx on idx.index_id = pm.proj_index AND idx.db_status = 2 " +
					   " where qr.query_result = ab.tran_num AND qr.unique_id = " + iQueryId + 
					   " AND ab.ins_num = pm.ins_num and pf.param_seq_num = pm.param_seq_num and pm.ins_num = pf.ins_num and toolset!=36 and pf.rate_status != 0";
			   iRetVal = DBase.runSql(sRequest);
			   iRetVal = DBase.createTableOfQueryResults(nonPhysTbl);
			   nonPhysTbl.copyRowAddAll(returnt);

			   fixup_reset_dates(returnt, argt, resetTbl);       //  need to fix reset dates for commodity toolset.
	   
			   returnt.colConvertDateTimeToInt( "parcel_reset_start_date");
			   returnt.colConvertDateTimeToInt( "parcel_reset_end_date");
			   duplicateRowsForEachParentIndex(returnt, "proj_index", "inheritance");
			   returnt.delCol("inheritance");
			   returnt.group("dealnum, leg, parcel_id");
		   }
		   catch (OException e)
		   {
			   OConsole.oprint("Error: " + e.getMessage());
		   }
		   finally
		   {
			   nonPhysTbl.destroy();
			   resetTbl.destroy();
			   
			   /* free qid if query ID is not provided as a parameter. */
			   if ((tSimDef.getColNum( "APM Single Deal Query") < 1) || (tSimDef.getInt( "APM Single Deal Query", 1) == 0))
			   {	
				   Query.clear(iQueryId);
			   }
		   }
	   }
	}	

	/*-------------------------------------------------------------------------------
	Name:          format_result()
	Description:   UDSR format function. (Default Formatting used)
	Parameters:      
	Return Values:   
	-------------------------------------------------------------------------------*/
	void format_result(Table returnt) throws OException
	{
		returnt.setColFormatAsDate( "parcel_reset_start_date");
		returnt.setColFormatAsDate( "parcel_reset_end_date");
        returnt.setColFormatAsRef( "toolset", SHM_USR_TABLES_ENUM.TOOLSETS_TABLE);
        returnt.setColFormatAsRef( "proj_index", SHM_USR_TABLES_ENUM.INDEX_TABLE);
		returnt.colHide("reset_conv");
		returnt.colHide("avg_type");
	}

	/*
	 *  for commodity toolset, if projection method has reset_conv = 23 (Daily all) and avg_type = 6 (Notional Weighted),
	 *  set reset start date to reset end date.
	 */
	void fixup_reset_dates(Table returnt, Table argt, Table resetTbl) throws OException
	{
		int colResetConv = returnt.getColNum("reset_conv");
		int colAvgType = returnt.getColNum("avg_type");
		int colInsNum = returnt.getColNum("ins_num");
		int colLeg = returnt.getColNum("leg");
		int colPrhSeqNum = returnt.getColNum("prh_seq_num");
		int colInsSourceIdNum = returnt.getColNum("ins_source_id");
		int colStartDate = returnt.getColNum("parcel_reset_start_date");
		int colEndDate = returnt.getColNum("parcel_reset_end_date");
		int numRows = returnt.getNumRows();
		int dailyAll = RESET_CONV_ENUM.RESETCONV_DAILY_ALL.toInt();
		int ntlWeighted = AVG_TYPE_ENUM.AVGTYPE_NOTNL_WEIGHTED.toInt();
		Table tmpReset = Table.tableNew();
		int priceUnknown = VALUE_STATUS_ENUM.VALUE_UNKNOWN.toInt();

		try
		{
			resetTbl.group("ins_num, param_seq_num, profile_seq_num, prh_seq_num, reset_seq_num");
		
			for (int i=1; i <= numRows; i++ )
			{
				if (returnt.getInt(colResetConv, i) == dailyAll && returnt.getInt(colAvgType, i) == ntlWeighted)
				{
					returnt.setDateTime(colStartDate, i, returnt.getDateTime(colEndDate, i));
					continue;
				}
				tmpReset.clearRows();
				String wStr = "ins_num EQ " + returnt.getInt(colInsNum, i) + " AND param_seq_num EQ " + returnt.getInt(colLeg, i) +
						" AND profile_seq_num EQ " + returnt.getInt(colInsSourceIdNum, i) + " AND prh_seq_num EQ " + returnt.getInt(colPrhSeqNum, i); 	
				tmpReset.select(resetTbl, "profile_seq_num, prh_seq_num, reset_seq_num, reset_date, value_status", wStr);
				tmpReset.group("reset_date, value_status");
				
				int row = 0, rEnd = 0, rStart = 0;
				for (row = tmpReset.getNumRows(); row > 0; row--)
				{
					if (tmpReset.getInt("value_status", row) == priceUnknown)
					{
						if (rEnd == 0) 
							rEnd = row;
						rStart = row;
						continue;
					}
					break;
				}
				if (rStart > 0)
				{					
					returnt.setDateTime(colStartDate, i, tmpReset.getDateTime("reset_date", rStart));
					returnt.setDateTime(colEndDate, i, tmpReset.getDateTime("reset_date", rEnd));
				}
			} 
		}
		finally 
		{
			tmpReset.destroy();
		}
	}

	/*-------------------------------------------------------------------------------
	Name:          duplicateRowsForEachParentIndex
	Description:   This function will duplicate each row in the passed in table for every
	               parent curve of the projection index and will replace the projection
		           index value with the parent value.
	-------------------------------------------------------------------------------*/
	void duplicateRowsForEachParentIndex(Table resetRows, String projIndexColName, String inheritanceColName) throws OException
	{
		int projIndexColNum = resetRows.getColNum(projIndexColName);
		int inheritanceColNum = resetRows.getColNum(inheritanceColName);
		resetRows.group(projIndexColName);
	   	int resetNumRows = resetRows.getNumRows();
	   	int rowProjIndexStart = 1;
	  
	   	while ( rowProjIndexStart <= resetNumRows )
	   	{
	   		int projIndexID = resetRows.getInt(projIndexColNum, rowProjIndexStart);
	   		int inheritance = resetRows.getInt(inheritanceColNum, rowProjIndexStart);
   			int rowProjIndexEnd = resetRows.findIntRange(projIndexColNum, rowProjIndexStart, resetNumRows, projIndexID, SEARCH_ENUM.LAST_IN_GROUP);
   			int numRowsWithProjIndex = rowProjIndexEnd - rowProjIndexStart + 1;
	   		if ( inheritance == 1 )
	   		{
   				Table projIndexParents = Util.NULL_TABLE;
	   			try {
	   				projIndexParents = getParentIndexes(projIndexID);
	   				for (int parentRow = 1; parentRow <= projIndexParents.getNumRows(); parentRow++)
	   				{
	   					int lastRow = resetRows.getNumRows();
	   					int parentIndexID = projIndexParents.getInt(1 /*col*/, parentRow);
	   					resetRows.copyRowAddRange(rowProjIndexStart, rowProjIndexEnd, resetRows);
	   					for (int copiedRow = 1; copiedRow <= numRowsWithProjIndex; copiedRow++)
	   					{
	   						resetRows.setInt(projIndexColNum, lastRow+copiedRow, parentIndexID);
	   					}		
	   				}
	   				projIndexParents.destroy();
	   				projIndexParents = Util.NULL_TABLE;
	   			} finally {
	   				if ( projIndexParents != Util.NULL_TABLE )
	   					projIndexParents.destroy();
	   			}
	   		}
	   		rowProjIndexStart += numRowsWithProjIndex;
	   	} 
	}
	
	/*-------------------------------------------------------------------------------
	Name:          getParentIndexes
	Description:   This function will create a table of parent indexes
		           for that index and return it.  It sorts the table only so that
		           it can unique it.
	-------------------------------------------------------------------------------*/
	Table getParentIndexes(int indexID) throws OException
	{
		Table parents = Util.NULL_TABLE;
		try {
			parents = Table.tableNew();
			Index.tableLoadParentIndexes(parents, indexID, 1 /*inheritance*/);
			if ( parents.getNumRows() > 1 )
			{
				parents.clearGroupBy();
				parents.addGroupBy(1);
				parents.groupBy();
				parents.distinctRows();
			}
		} catch (Exception e) {
			if ( parents != Util.NULL_TABLE )
				parents.destroy();
			throw e;
		}
		return parents;
	}
}
