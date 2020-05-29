package com.jm.accountingfeed.udsr;

import com.jm.accountingfeed.udsr.ledgers.FactoryDealCalculator;
import com.jm.accountingfeed.udsr.ledgers.IDealCalculator;
import com.jm.accountingfeed.util.Util;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.jm.logging.Logging;

/**
 * UDSR to populate the class variable 'tblReturnt' with -
 * 1. Ledger fields that can be fetched from Transaction pointer
 * 2. Ledger fields that are common on all ledgers (GL,ML,SL)
 * @author jains03
 *
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
public class TranData extends UdsrBase 
{
    /**
     * Add common(for all Ledgers) columns to the outData Table.
     */
    @Override
    protected Table defineOutputTable(Table outData) throws OException 
    {
        outData = super.defineOutputTable(outData);
        
        outData.addCol("deal_num", COL_TYPE_ENUM.COL_INT, "Deal Number");
        outData.addCol("tran_num", COL_TYPE_ENUM.COL_INT, "Transaction Number");
        outData.addCol("tran_group", COL_TYPE_ENUM.COL_INT, "Transaction Group");
        outData.addCol("deal_leg_phy", COL_TYPE_ENUM.COL_INT, "Deal Side Physical");//ML only
        outData.addCol("deal_leg_fin", COL_TYPE_ENUM.COL_INT, "Deal Side Financial");//ML only
        outData.addCol("desk_location", COL_TYPE_ENUM.COL_STRING, "Desk Location");
        outData.addCol("trade_date", COL_TYPE_ENUM.COL_INT, "Trade Date");
        outData.addCol("reference", COL_TYPE_ENUM.COL_STRING, "Trade Reference");
        outData.addCol("tran_status", COL_TYPE_ENUM.COL_INT, "Trade Status"); 
        outData.addCol("is_precious_metal", COL_TYPE_ENUM.COL_INT, "Is Precious Metal"); 
        outData.addCol("internal_bunit", COL_TYPE_ENUM.COL_INT, "Internal Party Id"); 
        outData.addCol("external_bunit", COL_TYPE_ENUM.COL_INT, "External Party Id"); 
        outData.addCol("internal_lentity", COL_TYPE_ENUM.COL_INT, "Internal Lentity"); 
        outData.addCol("internal_portfolio", COL_TYPE_ENUM.COL_INT, "Portfolio");
        outData.addCol("ins_type", COL_TYPE_ENUM.COL_STRING, "Instrument Type");
        outData.addCol("trade_type", COL_TYPE_ENUM.COL_STRING, "Trade Type"); 
        outData.addCol("buy_sell", COL_TYPE_ENUM.COL_INT); //Hidden
        outData.addCol("ins_type_id", COL_TYPE_ENUM.COL_INT); //Hidden
        outData.addCol("ins_sub_type_id", COL_TYPE_ENUM.COL_INT); //Hidden
        outData.addCol("fixed_float", COL_TYPE_ENUM.COL_INT, "Fixed Float"); 
        outData.addCol("trading_location", COL_TYPE_ENUM.COL_STRING, "Trading Location"); 
        outData.addCol("from_currency", COL_TYPE_ENUM.COL_INT, "From Currency"); 
        outData.addCol("to_currency", COL_TYPE_ENUM.COL_INT, "To Currency"); 
        outData.addCol("value_date", COL_TYPE_ENUM.COL_INT, "Value Date"); 
        outData.addCol("position_uom", COL_TYPE_ENUM.COL_DOUBLE, "From Value");
        outData.addCol("position_toz", COL_TYPE_ENUM.COL_DOUBLE, "Base Weight");
        outData.addCol("cash_amount", COL_TYPE_ENUM.COL_DOUBLE, "To Value");
        outData.addCol("uom", COL_TYPE_ENUM.COL_INT, "Unit of Measure");
        outData.addCol("unit_price", COL_TYPE_ENUM.COL_DOUBLE, "Unit Price");
        outData.addCol("interest_rate", COL_TYPE_ENUM.COL_DOUBLE, "Interest Rate");
        outData.addCol("payment_date", COL_TYPE_ENUM.COL_INT, "Payment Date");
        outData.addCol("is_coverage", COL_TYPE_ENUM.COL_STRING, "Is Coverage");
        outData.addCol("coverage_text", COL_TYPE_ENUM.COL_STRING, "Coverage Text");
        outData.addCol("location", COL_TYPE_ENUM.COL_STRING, "Location");//ML only 
        outData.addCol("form", COL_TYPE_ENUM.COL_INT, "Form"); //ML only
        outData.addCol("purity", COL_TYPE_ENUM.COL_INT, "Purity"); //ML only
        outData.addCol("base_currency_int_bu", COL_TYPE_ENUM.COL_STRING, "Base Currency - Internal Bunit");//SL only 
        
        outData.addCol("returnDate", COL_TYPE_ENUM.COL_INT,"Return Date");
        outData.addCol("pricing_type", COL_TYPE_ENUM.COL_STRING, "Pricing Type"); // Added for HK
        
        return outData;
    }

    /**
     * For all transactions in the UDSR input -
     * 1. Populate value of columns using Transaction pointer
     * 2. Populate value of Region of internal business unit - Region is a party info field 
     */
    @Override
    protected void calculate(IContainerContext context) throws OException 
    {
        defineOutputTable(tblReturnt);

        int numTrans = tblTransactions.getNumRows();
        Logging.info("Calculating Tran data. numTrans = " + numTrans);
        FactoryDealCalculator factory = new FactoryDealCalculator(tblTransactions);

        for (int row = 1; row <= numTrans; row++) 
        {
        	int dealNum = tblTransactions.getInt("deal_num", row);
        	if (dealNum == 0)
        	{
        		continue;
        	}

        	Transaction tran = tblTransactions.getTran("tran_ptr", row);
            IDealCalculator calculator = factory.getDealCalculator(tran);
            if(calculator.isProcessed())
            {
                continue;
            }

            int legsOnDeal = calculator.getNumLegs();
            Logging.debug("Processing dealNum=" + dealNum + ", numOfLegs=" + legsOnDeal + ", IDealCalculator=" + calculator.getClass().getSimpleName());
            for(int leg = 1; leg <= legsOnDeal; leg++)
            {
                int newRow = tblReturnt.addRow();
                
                /**
                 * Deal numbers on different legs of a deal can different in case of Fx Swap deal.
                 * Fx Swap defined in Endur is having two legs (Near & Far) but they are actually two different deals with same tran-group. 
                 */
                tblReturnt.setInt("deal_num", newRow, calculator.getDealNum(leg));
                tblReturnt.setInt("tran_num", newRow, calculator.getTranNum(leg));
                tblReturnt.setInt("tran_group", newRow, calculator.getTranGroup());
                tblReturnt.setInt("trade_date", newRow, calculator.getTradeDate());
                tblReturnt.setString("reference", newRow, calculator.getDealReference());
                tblReturnt.setInt("tran_status", newRow, calculator.getTranStatusId());
                tblReturnt.setInt("internal_bunit", newRow, calculator.getInternalBunit());
                tblReturnt.setInt("external_bunit", newRow, calculator.getExternalBunit());
                tblReturnt.setInt("internal_lentity", newRow, calculator.getInternalLentity());
                tblReturnt.setInt("internal_portfolio", newRow, calculator.getInternalPortfolio());
                tblReturnt.setString("ins_type", newRow, calculator.getInsType());
                tblReturnt.setString("trade_type", newRow, calculator.getTradeType(leg));
                tblReturnt.setInt("buy_sell", newRow, calculator.getBuySell(leg));
                tblReturnt.setInt("ins_type_id", newRow, calculator.getInsTypeId());
                tblReturnt.setInt("ins_sub_type_id", newRow, calculator.getInsSubType(leg));
                tblReturnt.setInt("fixed_float", newRow, calculator.getFixedFloat());
                tblReturnt.setString("trading_location", newRow, calculator.getTradeLocation(leg));
                tblReturnt.setDouble("cash_amount", newRow, calculator.getCashAmount(leg));
                tblReturnt.setDouble("unit_price", newRow, calculator.getTradePrice(leg));
                tblReturnt.setDouble("interest_rate", newRow, calculator.getInterestRate());
                tblReturnt.setInt("payment_date", newRow, calculator.getPaymentDate(leg));
                tblReturnt.setString("is_coverage", newRow, calculator.getIsCoverage());
                tblReturnt.setString("coverage_text", newRow, calculator.getCoverageText());

                //Leg Level Attributes
                tblReturnt.setInt("deal_leg_phy", newRow, calculator.getPhySideNum(leg));
                tblReturnt.setInt("deal_leg_fin", newRow, calculator.getFinSideNum(leg));
                tblReturnt.setInt("is_precious_metal", newRow, calculator.getPreciousMetal(leg));
                tblReturnt.setInt("value_date", newRow, calculator.getSettlementDate(leg));
                tblReturnt.setInt("from_currency", newRow, calculator.getFromCurrency(leg));
                tblReturnt.setInt("to_currency", newRow, calculator.getToCurrency(leg));
                tblReturnt.setDouble("position_uom", newRow, calculator.getPositionUom(leg));
                tblReturnt.setDouble("position_toz", newRow, calculator.getPositionToz(leg));
                tblReturnt.setInt("uom", newRow, calculator.getUom(leg));
                tblReturnt.setString("location", newRow, calculator.getLocation(leg));
                tblReturnt.setInt("form", newRow, calculator.getForm(leg));
                tblReturnt.setInt("purity", newRow, calculator.getPurity(leg));
                tblReturnt.setInt("returnDate", newRow, calculator.getEndDate());
                tblReturnt.setString("pricing_type", newRow, calculator.getPricingType());
            }
        }
        Logging.debug("Deal parameters populated ");
        int numOutputRow = tblReturnt.getNumRows();

        //Populate Region based on internal bunit
        tblReturnt.select(Util.getPartyInfoIntBUnitRegion(), "region(desk_location)", "party_id EQ $internal_bunit ");

        Logging.debug("Region populated ");
        if(numOutputRow != tblReturnt.getNumRows()) 
        {
            Logging.warn("Number of rows in output changed during region(desk_location) enrichment. Expected=" +numOutputRow + ",Actual="+ tblReturnt.getNumRows());
        }
        
        //Populate Base Currency based on internal legal entity
        tblReturnt.select(Util.getPartyInfoIntBunitBaseCurrency(), "base_currency(base_currency_int_bu)", "party_id EQ $internal_bunit");

        Logging.debug("Base Currency populated ");
        if(numOutputRow != tblReturnt.getNumRows()) 
        {
            Logging.warn("Number of rows in output changed during base_currency(base_currency_int_bu) enrichment. Expected=" +numOutputRow + ",Actual="+ tblReturnt.getNumRows());
        }

        Logging.info("Finished calculating Tran data. numOutputRow = " + numOutputRow);
    }

	@Override
    protected void format(IContainerContext context) throws OException 
    {
        Table returnt = context.getReturnTable();
        
        returnt.defaultFormat();
        returnt.setColFormatAsRef("uom", SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE); 
        returnt.setColFormatAsRef("from_currency", SHM_USR_TABLES_ENUM.CURRENCY_TABLE); 
        returnt.setColFormatAsRef("to_currency", SHM_USR_TABLES_ENUM.CURRENCY_TABLE); 
        returnt.setColFormatAsRef("ins_sub_type_id", SHM_USR_TABLES_ENUM.INS_SUB_TYPE_TABLE); 
        
        returnt.group("deal_num");
    }
}
