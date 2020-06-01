package com.jm.accountingfeed.udsr.ledgers;

import com.jm.accountingfeed.udsr.TranData;
import com.jm.accountingfeed.util.Util;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.OException;
import com.olf.openjvs.OLog;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.INS_TYPE_ENUM;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.jm.logging.Logging;

/**
 * UDSR for General Ledger
 * This class extends 'TranData' and adds GL specific fields.
 * Supported toolsets -
 * 1. FX
 * 2. ComFut
 * 3. ComSwap
 * @author jains03
 *
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
public class GeneralLedgerData extends TranData 
{
    /**
     * This method adds Market data columns to the Output table of UDSR
     */
    @Override
    protected Table defineOutputTable(Table data) throws OException
    {
        data = super.defineOutputTable(data);
        
        data.addCol("reverse_gl", COL_TYPE_ENUM.COL_INT, "Reverse GL"); 
        data.addCol("exchange_rate", COL_TYPE_ENUM.COL_DOUBLE, "Exchange Rate");
        data.addCol("spot_equivalent_price", COL_TYPE_ENUM.COL_DOUBLE, "Spot Equiv Price");
        data.addCol("spot_equivalent_value", COL_TYPE_ENUM.COL_DOUBLE, "Spot Equiv Value");
        
        return data;
    }
	   
    /**
     * For all transactions in the UDSR input -
     * 1. Populate the Market data fields using DB table USER_jm_jde_extract_data
     */
    @Override
    protected void calculate(IContainerContext context) throws OException 
    {
        Logging.info("Calculating General Ledger data");
        super.calculate(context);

        Table dealTable = Table.tableNew();
        Table pnlMarketData = null;
        Table metalSwaps = null;
        Table missingMarketData = null;
        try 
        {
            int numOfDeals = tblReturnt.getNumRows();
            dealTable.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
            tblReturnt.copyCol("deal_num", dealTable, "deal_num");

            pnlMarketData = Util.getPnlMarketData(dealTable);

            //Populate market data from USER_jm_jde_extract_data
            tblReturnt.select(pnlMarketData, "spot_equivalent_price(spot_equivalent_price),spot_equivalent_value(spot_equivalent_value),fwd_rate(exchange_rate),reverse_gl(reverse_gl)",
                    "deal_num EQ $deal_num ");
            Logging.debug("market data populated ");
            if(numOfDeals != tblReturnt.getNumRows()) 
            {
                Logging.warn("Number of rows in output changed during MarketData enrichment. Expected=" +numOfDeals + ",Actual="+ tblReturnt.getNumRows());
            }

            metalSwaps = Table.tableNew();
            metalSwaps.select(tblReturnt, "deal_num", "ins_type_id EQ " + INS_TYPE_ENUM.metal_swap.toInt());
            metalSwaps.select(pnlMarketData, "metal_volume_uom(position_uom), metal_volume_toz(position_toz), settlement_value(cash_amount), trade_price(unit_price)", "deal_num EQ $deal_num");
            
            tblReturnt.select(metalSwaps, "position_uom,position_toz,cash_amount,unit_price", "deal_num EQ $deal_num");
            Logging.debug("data populated for metal swaps");
            if(numOfDeals != tblReturnt.getNumRows()) 
            {
                Logging.warn("Number of rows in output changed during Metal swap Data enrichment. Expected=" +numOfDeals + ",Actual="+ tblReturnt.getNumRows());
            }

            /**
             * In case of any error in OpenLink's Operations Service which populates the Market data in the User table.
             * Log Error for the deals missing the market data
             */
            missingMarketData = Table.tableNew();
            missingMarketData.select(tblReturnt, "deal_num", "is_precious_metal EQ 1 AND spot_equivalent_value EQ 0.0");
            
            int numMissingData = missingMarketData.getNumRows();
            for(int row =1 ;row <= numMissingData; row++)
            {
                Logging.error("Market data User table not populated for deal num = " + missingMarketData.getInt("deal_num", row));
                OLog.logError(1, "Market data User table not populated for deal num = " + missingMarketData.getInt("deal_num", row));
            }
            
            /* Adjust signage for position and cash */
            Util.adjustSignageForJDE(tblReturnt, "tran_status");
            Logging.info("Finished calculating General Ledger data");
        } 
        finally 
        {
            if (Table.isTableValid(dealTable) == 1) 
            {
                dealTable.destroy();
            }
            
            if (Table.isTableValid(pnlMarketData) == 1) 
            {
                pnlMarketData.destroy();
            }
            if (Table.isTableValid(metalSwaps) == 1) 
            {
                metalSwaps.destroy();
            }
            if (Table.isTableValid(missingMarketData) == 1) 
            {
                missingMarketData.destroy();
            }
        }
    }

    @Override
    protected void format(IContainerContext context) throws OException 
    {
        super.format(context);
        Table returnt = context.getReturnTable();

        //Show SL specific columns
        returnt.colShow("to_currency");
        returnt.colShow("value_date");
        returnt.colShow("cash_amount");
        returnt.colShow("unit_price");
        returnt.colShow("interest_rate");
        returnt.colShow("payment_date");
        returnt.colShow("is_coverage");

        returnt.setColFormatAsRef("to_currency", SHM_USR_TABLES_ENUM.CURRENCY_TABLE); 
    }

}