package com.jm.accountingfeed.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;

import com.jm.accountingfeed.enums.EndurDocumentStatus;
import com.jm.accountingfeed.enums.EndurPartyInfoInternalBunit;
import com.jm.accountingfeed.exception.AccountingFeedRuntimeException;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.BUY_SELL_ENUM;
import com.olf.openjvs.enums.CFLOW_TYPE;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.INS_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.jm.logging.Logging;

/**
 * Helper class with misc static functions
 */
public class Util 
{
	/**
	 * Round cash amounts as specified in JDE
	 * 
	 * @param value
	 * @param decimalPlaces
	 * @return
	 */
	public static BigDecimal roundCashAmount(double value) 
	{
		BigDecimal rounded = BigDecimal.valueOf(value);
		
		rounded = rounded.setScale(2, RoundingMode.HALF_UP);
		
	    return rounded;
	}

	/**
	 * Round notional position as specified in JDE
	 * 
	 * @param value
	 * @param decimalPlaces
	 * @return
	 */
	public static BigDecimal roundPosition(double value) 
	{
		BigDecimal rounded = BigDecimal.valueOf(value);
		
		rounded = rounded.setScale(4, RoundingMode.HALF_UP);
		
        return rounded;
	}
	
	/**
	 * Round notional position as specified in JDE
	 * 
	 * @param double1
	 * @return
	 */
	public static BigDecimal roundDoubleTo6DPS(double value)
	{
		BigDecimal rounded = BigDecimal.valueOf(value);
		
		rounded = rounded.setScale(6, RoundingMode.HALF_UP);
		
        return rounded;
	}
	
	/**
	 * Setup a log file
	 * 
	 * @param logFileName
	 * @throws OException
	 */
	public static void setupLog() throws OException
	{
		ConstRepoConfig constRepoConfig = new ConstRepoConfig();
		initialiseLog(constRepoConfig);
	}
	
	/**
	 * Setup a log file
	 * 
	 * @param logFileName
	 * @throws OException
	 */
	public static void setupLog(ConstRepoConfig constRepoConfig) throws OException
	{
		initialiseLog(constRepoConfig);
	}
	
	public static void initialiseLog(ConstRepoConfig constRepoConfig) throws OException
	{
		String abOutDir =  SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";
		
		String logDir = abOutDir;
		
		/* Can be overriden to DEBUG or any other level via constants repository */
		String logLevel = constRepoConfig.getValue("logLevel", "INFO");
		String logFile = Constants.LOG_FILE_NAME;

        try
        {
        	Logging.init(Util.class, "Interfaces", "EndurAccountingFeed");
        } 
		catch (Exception e) 
		{
			String errMsg = "Failed to initialize logging module.";
			Util.printStackTrace(e);
			com.olf.openjvs.Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Check if the specified currency is of type precious metal 
	 * 
	 * @param currencyId
	 * @return
	 * @throws OException
	 */
	private static HashSet<Integer> preciousMetalCurrencies = null;

	public static HashSet<Integer> getPreciousMetalCurrencies() throws OException
	{
        Table tblData = null;
        
        try
        {
            if (preciousMetalCurrencies == null)
            {
                /* Lazy load */
                preciousMetalCurrencies = new HashSet<Integer>(); 
                        
                tblData = Table.tableNew("Precious Metal Currencies");
                
                String sqlQuery = "select id_number from currency where precious_metal = 1";
                
                int ret = DBaseTable.execISql(tblData, sqlQuery);
                
                if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
                {
                    throw new AccountingFeedRuntimeException("Unable to execute query: " + sqlQuery);
                }
                
                int numRows = tblData.getNumRows();
                for (int row = 1; row <= numRows; row++)
                {
                    preciousMetalCurrencies.add(tblData.getInt("id_number", row));
                }
            }
            
            return preciousMetalCurrencies;
        }
        catch (Exception e)
        {
            throw new AccountingFeedRuntimeException("Error encountered during getPreciousMetalCurrencies ", e);
        }
        finally
        {
            if (tblData != null)
            {
                tblData.destroy();
            }
        }

	}
	
	public static boolean isPreciousMetalCurrency(int currencyId) throws OException
	{	
	    return getPreciousMetalCurrencies().contains(currencyId);
	}
	
	private static Table tblInsTypeSuffix = null;
	public static Table getInsTypeSuffix() throws OException
	{	
		if (tblInsTypeSuffix == null)
		{
			tblInsTypeSuffix = Table.tableNew();
			
			String sqlQuery = 
				"SELECT \n" +
					"uti.suffix, \n" +
					"uti.ins_name, \n" +
					"uti.buy_sell, \n" +
					"uti.trade_status, \n" +
					"uti.precious_metal \n" +
				"FROM " + Constants.USER_JM_TRANSACTION_ID + " uti \n" +
				 "WHERE uti.ins_subtype = ''";
			
			int ret = DBaseTable.execISql(tblInsTypeSuffix, sqlQuery);
			
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				throw new AccountingFeedRuntimeException("Unable to run query: " + sqlQuery);
			}
			
			tblInsTypeSuffix.addCol("tran_status_id", COL_TYPE_ENUM.COL_INT);
			
			adjustInsTypeTable(tblInsTypeSuffix);	
		}
		
		return tblInsTypeSuffix;
	}

	private static Table tblInsSubTypeSuffix = null;
	public static Table getInsSubTypeSuffix() throws OException
	{	
		if (tblInsSubTypeSuffix == null)
		{
			tblInsSubTypeSuffix = Table.tableNew();
			
			String sqlQuery = 
				"SELECT \n" +
					"uti.suffix, \n" +
					"uti.ins_name, \n" +
					"uti.ins_subtype, \n" +
					"uti.buy_sell, \n" +
					"uti.trade_status, \n" +
					"uti.precious_metal \n" +
				"FROM " + Constants.USER_JM_TRANSACTION_ID + " uti \n" +
				"WHERE uti.ins_subtype <> ''";
			
			int ret = DBaseTable.execISql(tblInsSubTypeSuffix, sqlQuery);
			
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				throw new AccountingFeedRuntimeException("Unable to run query: " + sqlQuery);
			}
			
			/* ins_subtype and trade_status are String columns - convert them to ints */
			tblInsSubTypeSuffix.addCol("tran_status_id", COL_TYPE_ENUM.COL_INT);
			tblInsSubTypeSuffix.addCol("ins_sub_type_id", COL_TYPE_ENUM.COL_INT);
		
			adjustInsTypeTable(tblInsSubTypeSuffix);
		}
		
		return tblInsSubTypeSuffix;
	}
	
	/**
	 * Adjust the ins type table to make it more compatible with table selects
	 * 
	 * @param tbl
	 * @throws OException
	 */
	private static void adjustInsTypeTable(Table tbl) throws OException
	{
		boolean containsTranStatusIdCol = tbl.getColNum("tran_status_id") > 0;
		boolean containsInsSubTypeIdIdCol = tbl.getColNum("ins_sub_type_id") > 0;
		
		int numRows = tbl.getNumRows();
		for (int row = 1; row <= numRows; row++)
		{	
			if (containsTranStatusIdCol)
			{
				String tradeStatus = tblInsTypeSuffix.getString("trade_status", row);
			
				if (tradeStatus != null && !"".equalsIgnoreCase(tradeStatus))
				{
					int tranStatusId = Ref.getValue(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, tradeStatus);
					
					tbl.setInt("tran_status_id", row, tranStatusId);
				}		
			}
			
			if (containsInsSubTypeIdIdCol)
			{
				String insSubTypeStr = tblInsTypeSuffix.getString("ins_subtype", row);
				if (insSubTypeStr != null && !"".equalsIgnoreCase(insSubTypeStr))
				{
					int insSubTypeId = Ref.getValue(SHM_USR_TABLES_ENUM.INS_SUB_TYPE_TABLE, insSubTypeStr);
					if (insSubTypeId == -1) insSubTypeId = 0;
					
					tblInsTypeSuffix.setInt("ins_sub_type_id", row, insSubTypeId);
				}	
			}
			
			int preciousMetal = tbl.getInt("precious_metal", row);
			if (preciousMetal == -1)
			{
				tbl.setInt("precious_metal", row, 0);
			}
		}
	}
	
	/**
	 * Returns the market data from table USER_jm_jde_extract_data for the deal numbers in the @param dealTable
	 * 1. Insert the deal_num in query_result table
	 * 2. Read Market data
	 * 3. Select Deal specific market data
     * 
	 * @param dealTable
	 * @return pnlMarketData (this table shall be destroyed by the caller)
	 * @throws AccountingFeedRuntimeException
	 * @throws OException 
	 */
	public static Table getPnlMarketData(Table dealTable) throws AccountingFeedRuntimeException, OException 
	{
	    Table pnlMarketData = null;
        Table pnlMarketDataAll = null;
	    int dealTableQueryId = 0;

	    try 
	    {
	        dealTableQueryId = Query.tableQueryInsert(dealTable, 1);
	        Logging.debug(" queryId " + dealTableQueryId);
	        
	        if (dealTableQueryId <= 0) 
	        {
	        	String error = "Unable to insert deal numbers in DB query_result table";
	            
	            throw new AccountingFeedRuntimeException(error);
	        }

	        String sQueryTable = Query.getResultTableForId(dealTableQueryId);
	        pnlMarketData = dealTable.copyTable();
	        pnlMarketDataAll = Table.tableNew();

	        String marketDataQuery = "select query_result as deal_num,spot_equiv_value as spot_equivalent_value, \n "
	                + "spot_equiv_price as spot_equivalent_price,\n "
	                + "fx_fwd_rate as fwd_rate, "
                    + "CASE fixings_complete \n "
                    + "WHEN 'N' THEN 1 \n "
                    + "ELSE 0 END as reverse_gl, \n "
                    + "metal_volume_uom, \n "
                    + "metal_volume_toz, \n "
                    + "settlement_value, \n "
                    + "trade_price \n "
	                + "from " + Constants.USER_JM_JDE_EXTRACT_DATA + " mkt right outer join " 
	                + "( select query_result from " + sQueryTable + " where unique_id=" + dealTableQueryId + " ) as q on \n" 
	                + " mkt.deal_num = q.query_result " ; 
	        int iRetVal = DBaseTable.execISql(pnlMarketDataAll, marketDataQuery);
	        if (iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) 
	        {
	            throw new AccountingFeedRuntimeException(" Unable to execute query SQL. Return code= " + iRetVal + "." + marketDataQuery);
	        }
	        
	        pnlMarketData.select(pnlMarketDataAll, "fwd_rate,reverse_gl,spot_equivalent_value,spot_equivalent_price,metal_volume_uom,metal_volume_toz,settlement_value,trade_price ", "deal_num EQ $deal_num ");
	    } 
	    catch (OException oe) 
	    {
	        Logging.error("Exception for queryId=" + dealTableQueryId + "." + oe.getMessage());
	        throw oe;
	    }
	    finally 
	    {
	        if (Table.isTableValid(pnlMarketDataAll) == 1) 
	        {
	            pnlMarketDataAll.destroy();
	        }
	        
	        if (dealTableQueryId > 0)
	        {
	        	Query.clear(dealTableQueryId);
	        }
	    }
	    
	    return pnlMarketData;
	}
	
	private static Table tblPartyInfoIntBUnitRegion = null;
	public static Table getPartyInfoIntBUnitRegion() throws OException
	{   
	    if (tblPartyInfoIntBUnitRegion == null)
	    {
	        tblPartyInfoIntBUnitRegion = Table.tableNew();

	        String sqlQuery = 
	                "SELECT \n" +
    	                " party_id, \n" +
    	                " value as region \n" +
	                "FROM party_info uti \n" +
	                "WHERE type_id = " + EndurPartyInfoInternalBunit.REGION.toInt();

	        int ret = DBaseTable.execISql(tblPartyInfoIntBUnitRegion, sqlQuery);

	        if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
	        {
	            throw new AccountingFeedRuntimeException("Unable to run query: " + sqlQuery);
	        }
	    }

	    return tblPartyInfoIntBUnitRegion;
	}

    private static Table tblPartyInfoIntBunitBaseCurrency = null;
    public static Table getPartyInfoIntBunitBaseCurrency() throws OException
    {   
        if (tblPartyInfoIntBunitBaseCurrency == null)
        {
        	tblPartyInfoIntBunitBaseCurrency = Table.tableNew();

            String sqlQuery = 
                    "SELECT \n" +
                        " party_id, \n" +
                        " value as base_currency \n" +
                    "FROM party_info uti \n" +
                    "WHERE type_id = " + EndurPartyInfoInternalBunit.BASE_CURRENCY.toInt();

            int ret = DBaseTable.execISql(tblPartyInfoIntBunitBaseCurrency, sqlQuery);

            if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
            {
                throw new AccountingFeedRuntimeException("Unable to run query: " + sqlQuery);
            }
        }

        return tblPartyInfoIntBunitBaseCurrency;
    }

	/**
	 * Remove rows from "tblSource", which already exist in "tblDestination" - based on the key column.
	 * 
	 * @param tblSource
	 * @param tblDestination
	 * @param KeyColumn
	 * @throws OException
	 */
	protected void removeRowsWhereExist(Table tblSource, Table tblDestination, String keyColumn) throws OException
	{
		Table tblDistinct = null;
		
		try
		{
			if (tblSource.getNumRows() > 0)
			{
				/* Get a distinct list of records based on the key, and a flag = 1 against all of these rows */
				tblDistinct = Table.tableNew();
				tblDistinct.select(tblDestination, "DISTINCT, " + keyColumn, keyColumn + " GT -1");
				tblDistinct.addCol("flag", COL_TYPE_ENUM.COL_INT);
				tblDistinct.setColValInt("flag", 1);
				
				/* Select "flag" into "tblSource" based on key-match, and then remove rows where flag == 1 */
				tblSource.select(tblDistinct, "flag", keyColumn + " EQ $" + keyColumn);
				tblSource.deleteWhereValue("flag", 1);
				tblSource.delCol("flag");
			}	
		}
		finally
		{
			if (tblDistinct != null)
			{
				tblDistinct.destroy();	
			}
		}
	}

	/**
	 * Retrieve historic invoice numbers from stldoc_info_h
	 * 
	 * @return
	 * @throws OException
	 */
	public static Table getHistoricDocumentNumbers(Table tblInvoices, int[] applicableDocInfos) throws OException
	{
		Table tblData = Table.tableNew("JM Historic Invoice Numbers");
		int queryId = 0;
		
		try
		{
			if (tblInvoices.getNumRows() == 0)
			{
				return tblData;
			}
			
			/* Build a csv list of doc info fields */
			StringBuilder builder = new StringBuilder();
			for (int docInfoFieldId : applicableDocInfos)
			{
				if (builder.toString().equalsIgnoreCase(""))
				{
					builder.append(docInfoFieldId);
				}
				else
				{
					builder.append(", ");
					builder.append(docInfoFieldId);
				}
			}
			
			queryId = Query.tableQueryInsert(tblInvoices, "endur_doc_num");
			
			if (queryId > 0)
			{
				String resultTable = Query.getResultTableForId(queryId);
				
				/* 
				 * We select the max invoice number because there are instances in the database where two doc numbers are generated 
				 * and logged in the historics for the same stldoc_hdr_hist_id. 
				 * 
				 * It's not clear why this occurs, but must be something to do with doc generation failing in the first place. The selection of the 
				 * max invoice number resolves this.
				 */
				String sqlQuery =
					"SELECT \n" +
						"data.endur_doc_num, \n" +
						"data.type_id, \n" +
						"MAX(data.invoice_number) as invoice_number, \n" +
						"data.stldoc_hdr_hist_id \n" +
						"FROM \n" +
						"( \n" +
							"SELECT \n" + 
								"sih.document_num AS endur_doc_num, \n" +
								"sih.type_id, \n" +	
								"CAST(sih.value AS INT) AS invoice_number, \n" +
								"sih.stldoc_hdr_hist_id \n" +
							"FROM \n" +
								resultTable + " qr \n" +
							"JOIN stldoc_info_h sih ON qr.query_result = sih.document_num \n" +
							"WHERE qr.unique_id = " + queryId + " \n" +
							"AND sih.type_id IN (" + builder.toString() + ") \n" +
						") data \n" +
					"GROUP BY data.endur_doc_num, data.type_id, data.stldoc_hdr_hist_id";
				
				int ret = DBaseTable.execISql(tblData, sqlQuery);
				if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				{
					throw new AccountingFeedRuntimeException("Unable to run query: " + sqlQuery);
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
	 * Return current doc info field states from stldoc_info
	 * 
	 * @param tblInvoices
	 * @param applicableDocInfos
	 * @return
	 * @throws OException
	 */
	public static Table getCurrentDocumentNumbers(Table tblInvoices, int[] applicableDocInfos) throws OException
	{
		Table tblData = Table.tableNew("JM Invoice Numbers");
		int queryId = 0;
		
		try
		{
			if (tblInvoices.getNumRows() == 0)
			{
				return tblData;
			}
			
			StringBuilder builder = new StringBuilder();
			for (int docInfoFieldId : applicableDocInfos)
			{
				if (builder.toString().equalsIgnoreCase(""))
				{
					builder.append(docInfoFieldId);
				}
				else
				{
					builder.append(", ");
					builder.append(docInfoFieldId);
				}
			}
			
			queryId = Query.tableQueryInsert(tblInvoices, "endur_doc_num");
			if (queryId > 0)
			{
				String resultTable = Query.getResultTableForId(queryId);
				
				String sqlQuery =
					"SELECT DISTINCT \n" +
							"sih.document_num AS endur_doc_num, \n" + 
							"sih.type_id, \n" +
							"CAST(sih.value AS INT) AS invoice_number \n" +
					"FROM \n" +
					resultTable + " qr \n" +
					"JOIN stldoc_info sih ON qr.query_result = sih.document_num \n" +
					"WHERE qr.unique_id = " + queryId + " \n" + 
					"AND sih.type_id IN (" + builder.toString() + ")"; 
				
				int ret = DBaseTable.execISql(tblData, sqlQuery);
				if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				{
					throw new AccountingFeedRuntimeException("Unable to run query: " + sqlQuery);
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
	 * One row of the fxSwapTran table represents one FX Swap deal having two legs (Near, Far)
	 * 1. Filter FX Swap, Quality swap & Location swap deals from @param dealTable
	 * 2. Search the other leg : if input deal is NEAR leg, then search FAR leg and vice-versa
	 * 3. Whenever input-leg is Far, populate the tran-ptr of corresponding Near leg in row 
	 * @param dealTable: all the deal_num under processing. Combination of (a)Fx Swap Near, (b)Fx SwapFar, (c) Others
	 * @return
	 * @throws OException 
	 */
	public static Table getFxSwapOtherLegDetails(Table dealTable) throws OException
	{
        int dealTableQueryId = 0;
        Table fxSwapTran = Table.tableNew();

        try 
        {
            dealTableQueryId = Query.tableQueryInsert(dealTable, 1);
            Logging.debug(" queryId " + dealTableQueryId);
            
            if (dealTableQueryId <= 0) 
            {
                String error = "Unable to insert deal numbers in DB query_result table";
                
                throw new AccountingFeedRuntimeException(error);
            }

            String sQueryTable = Query.getResultTableForId(dealTableQueryId);

            String sqlQuery = 
                    "select \n"
                        + "query_result as deal_num, "
                        + "secondLeg.tran_num as other_leg_tran_num, secondLeg.deal_tracking_num as other_leg_deal_num "
                    + "from ab_tran firstLeg  " 
                        //firstLeg is the FX deals having cflow type Swap, Quality swap or Location swap out of all the input deals
                    + "join ( select query_result from " + sQueryTable + " where unique_id=" + dealTableQueryId + " ) as q on \n" 
                        + " firstLeg.deal_tracking_num = q.query_result \n"
                        + "AND firstLeg.ins_type = " + INS_TYPE_ENUM.fx_instrument.toInt() + "\n"
                        + "AND firstLeg.cflow_type IN (" + CFLOW_TYPE.FX_SWAP_CFLOW.toInt() +  ", " + CFLOW_TYPE.FX_QUALITY_SWAP_CFLOW.toInt() +", " + CFLOW_TYPE.FX_LOCATION_SWAP_CFLOW.toInt() + ") \n"
                        + "AND firstLeg.current_flag = 1 \n"
                        /* secondLeg is second part of the FX deal. 
                         * If firstLeg ins_sub_type is NEAR, then secondLeg ins_sub_type is FAR
                         * If firstLeg ins_sub_type is FAR, then secondLeg ins_sub_type is NEAR
                         */
                    + "join ab_tran secondLeg on \n" 
                        + " firstLeg.tran_group = secondLeg.tran_group \n"
                        + "AND secondLeg.ins_sub_type <> firstLeg.ins_sub_type \n"; 
            int iRetVal = DBaseTable.execISql(fxSwapTran, sqlQuery);
            if (iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) 
            {
                throw new AccountingFeedRuntimeException(" Unable to execute query SQL. Return code= " + iRetVal + "." + sqlQuery);
            }
            fxSwapTran.addCol("other_leg_tran_ptr", COL_TYPE_ENUM.COL_TRAN);
            int numFxSwap = fxSwapTran.getNumRows();
            for (int rowNum=1; rowNum <= numFxSwap; rowNum++)
            {
                int otherTranNum = fxSwapTran.getInt("other_leg_tran_num", rowNum);
                //Retrieve the tran-ptr to the other leg. 
                Transaction otherTran = Transaction.retrieve(otherTranNum);
                fxSwapTran.setTran("other_leg_tran_ptr", rowNum, otherTran);                    
            }
            
            if (fxSwapTran.getNumRows() > 0)
            {
            	fxSwapTran.clearGroupBy();
            	fxSwapTran.addGroupBy("deal_num");
            	fxSwapTran.groupBy();
            }
        } 
        catch (OException oe) 
        {
            Logging.error("Exception for queryId=" + dealTableQueryId + "." + oe.getMessage());
            throw oe;
        }
        finally 
        {
            if (dealTableQueryId > 0)
            {
            	Query.clear(dealTableQueryId);
            }
        }
        
        return fxSwapTran;
	}

	/**
	 * Adjust signage for extracts as per JDE rules
	 * 
	 *				Buy				Sell			Buy				Sell	 	 	 	 
 	 *				Validated	 	Validated		Cancelled	 	Cancelled 	 	 	 
	 *	position	-				+				+				-			[fromValue, baseWeight]	 	 
	 *	cash		+				-				-				+			[toValue, spotEqVal, taxDealCcy, taxReportCcy]
	 *
	 * The signage in Endur vs JDE is opposite (as one follows the Nostro style and the other follows Vostro) so
	 * this logic for signage has been agreed to sit on the Endur side with no custom signage needed on the JDE side
	 * of the interface
	 * 
	 * @param tblData
	 * @statusColumn - tran_status or endur_doc_status to identify a deal or invoice status
	 * @throws OException
	 */
	private static HashSet<String> positionColumns = null;
	private static HashSet<String> cashColumns = null;
	public static void adjustSignageForJDE(Table tblData, String statusColumn) throws OException
	{
		Logging.info("Adjusting signage of Position & Cash fields.");
	    int numRows = tblData.getNumRows();
		
		if (positionColumns == null)
		{
			positionColumns = new HashSet<String>();
			positionColumns.add("position_uom");
			positionColumns.add("position_toz");
		}
		
		if (cashColumns == null)
		{
			cashColumns = new HashSet<String>();
            cashColumns.add("cash_amount");
            cashColumns.add("settle_amount"); 
			cashColumns.add("spot_equivalent_value");
			cashColumns.add("tax_in_deal_ccy");
			cashColumns.add("tax_in_tax_ccy");
		}
		
		for (int row = 1; row <= numRows; row++)
		{
			int buySell = tblData.getInt("buy_sell", row);
			int status = tblData.getInt(statusColumn, row);
			
			boolean isBuy = (BUY_SELL_ENUM.BUY.toInt() == buySell);
			boolean isSell = (BUY_SELL_ENUM.SELL.toInt() == buySell);
			
			boolean isValidated = (status == TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() 
			        || status == EndurDocumentStatus.SENT_TO_COUNTERPARTY.id()
			        || status == EndurDocumentStatus.SEL_FOR_PAYMENT.id()
			        || status == EndurDocumentStatus.UNDO_PAYMENT.id()
			        || status == EndurDocumentStatus.RECEIVE_PAYMENT.id()
					|| status == EndurDocumentStatus.IGNORE_FOR_PAYMENT.id());
			
			boolean isCancelled = (status == TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt() 
					|| status == EndurDocumentStatus.CANCELLED.id());
			
			for (String col : positionColumns)
			{
				if (tblData.getColNum(col) > 0)
				{
					double currentAbsoluteValue = Math.abs(tblData.getDouble(col, row));
					double newValue = currentAbsoluteValue;
					
					if ((isBuy && isValidated) || (isSell && isCancelled))
					{
						newValue *= -1;
					}

					tblData.setDouble(col, row, newValue);
				}
			}
			
			for (String col : cashColumns)
			{
				if (tblData.getColNum(col) > 0)
				{
					double currentAbsoluteValue = Math.abs(tblData.getDouble(col, row));
					double newValue = currentAbsoluteValue;
					
					if ((isSell && isValidated) || (isBuy && isCancelled))
					{
						newValue *= -1;
					}

					tblData.setDouble(col, row, newValue);
				}
			}
		}
	}
	
	/**
	 * Print out the full stack trace to the OConsole.
	 * 
	 * @param message
	 * @param e
	 *            @
	 */
	public static void printStackTrace( Exception e )
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String logInString = sw.toString();
		String logLines[] = logInString.split("\n");
		Logging.debug("Printing stack trace");
		for (String logLine : logLines)
		{
			Logging.error(logLine);
		}
	}
}