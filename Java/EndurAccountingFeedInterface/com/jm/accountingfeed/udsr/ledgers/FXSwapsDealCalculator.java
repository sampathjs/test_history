package com.jm.accountingfeed.udsr.ledgers;

import java.util.HashSet;
import java.util.Set;

import com.jm.accountingfeed.enums.EndurTranInfoField;
import com.jm.accountingfeed.util.Util;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.INS_SUB_TYPE;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.openlink.util.logging.PluginLog;

/**
 * This is a special case of FX deals. 
 * There are two legs(Near and Far) in a FX Swap but they are defined as two different deals in Endur.
 * The two legs are joined by TranGroup but differ in the InsSubType.   
 * @author jains03
 *
 */
public class FXSwapsDealCalculator extends FXDealCalculator 
{	
    private static int NEAR_LEG = 1;
    private int farLegDealNum;
    private int farLegTranNum;
    private Transaction farLegTran;
    private Set<Integer> processedDeals = null;
    /**
     * This table contains all FX swap deals of the UDSR input.
     * It stores deal and tran number of the other leg. i.e. if input deal is FX Swap NEAR, then other leg is FAR; and vice-versa
     * In case input deal is FAR, then this table contains the tran ptr of Near leg  
     */
    private Table tblOtherLegTransactions = null;
    
	public FXSwapsDealCalculator(Table tblTransactions) throws OException 
	{
        Table dealTable = Table.tableNew();
        dealTable.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
        tblTransactions.copyCol("deal_num", dealTable, "deal_num");
        //Bulk fetch details of FX Swap deals
        Table fxSwapTranDetails = Util.getFxSwapOtherLegDetails(dealTable);
        dealTable.destroy();

	    this.tblOtherLegTransactions = fxSwapTranDetails;
        this.processedDeals = new HashSet<>();
	}
	
	public FXSwapsDealCalculator(Transaction tran) 
	{
		super(tran);
	}
	
    @Override
	public void initialize(Transaction tran) 
	{
        super.initialize(tran);
        try {
            this.farLegDealNum = getDealNum(NEAR_LEG);
            int dealRow = tblOtherLegTransactions.findInt("deal_num", this.farLegDealNum, SEARCH_ENUM.FIRST_IN_GROUP);
            if(INS_SUB_TYPE.fx_far_leg.toInt() == getInsSubType(NEAR_LEG) )
            {
                //All the get operations on the Transactions are to be done on the Near leg, so replace this.tran with the Near leg tran ptr
                this.farLegTranNum = getTranNum(NEAR_LEG);
                this.farLegTran = this.tran;
                this.tran = tblOtherLegTransactions.getTran("other_leg_tran_ptr", dealRow);                
            } else {
                this.farLegDealNum = tblOtherLegTransactions.getInt("other_leg_deal_num", dealRow);
                this.farLegTranNum = tblOtherLegTransactions.getInt("other_leg_tran_num", dealRow);
                this.farLegTran = tblOtherLegTransactions.getTran("other_leg_tran_ptr", dealRow);
            }
        } catch (OException e) {
            PluginLog.error("Failed to initialize");
        }
	}
	
    @Override
    public boolean isProcessed() throws OException 
    {
        int nearDealNum = getDealNum(NEAR_LEG);
        if(this.processedDeals.contains(nearDealNum))
        {
            return true;            
        } 
        else
        {
            //Transaction in context has not been processed previously. Add both Near & Far legs in processedDeals.
            this.processedDeals.add(nearDealNum);
            this.processedDeals.add(this.farLegDealNum);
            return false; 
        }
    }

	@Override
	public int getNumLegs() throws OException 
	{
	    return 2;
	}

	private boolean isNearLeg(int leg)
	{
	    return leg == NEAR_LEG;
	}

	@Override
    public int getDealNum(int leg) throws OException 
    {
        if(isNearLeg(leg)) 
        {
            return super.getDealNum(leg);
        }
        else 
        {
            return this.farLegDealNum;
        }
    }

    @Override
    public int getTranNum(int leg) throws OException 
    {
        if(isNearLeg(leg)) 
        {
            return super.getTranNum(leg);
        }
        else 
        {
            return this.farLegTranNum;
        }
    }
    
    @Override
    public int getSettlementDate(int leg) throws OException 
    {
        if(isNearLeg(leg))
        {
            return super.getSettlementDate(leg);
        }
        else
        {
            /**
             * this.tran is always the near leg of the FX swap.
             * Source of this tran pointer is either-
             * (i) Input table to the UDSR
             * (ii) Retrieval of transaction in Util.getFxSwapOtherLegDetails() in case Input to the UDSR is far leg
             * There should be no need to fetch another tran pointer to Near Leg, but the api call to get TRANF_FX_FAR_BASE_SETTLE_DATE value using this.tran returns 0 in case (i) above. 
             */
            Transaction nearLeg = com.olf.openjvs.Util.NULL_TRAN;
            try
            {
                int nearLegTranNum = getTranNum(NEAR_LEG);
                nearLeg = Transaction.retrieve(nearLegTranNum);
                return nearLeg.getFieldInt(TRANF_FIELD.TRANF_FX_FAR_BASE_SETTLE_DATE.toInt());
            }
            finally
            {
                if (Transaction.isNull(nearLeg) != 1) 
                {
                    nearLeg.destroy();
                }
            }
        }
    }

    @Override
    public int getInsSubType(int leg) throws OException 
    {
        if(isNearLeg(leg))
        {
            return super.getInsSubType(leg);
        }
        else
        {
            return INS_SUB_TYPE.fx_far_leg.toInt();
        }
    }

    @Override
    public int getBuySell(int leg) throws OException 
    {
        if(isNearLeg(leg))
        {
            return super.getBuySell(leg);
        }
        else
        {
            return tran.getFieldInt(TRANF_FIELD.TRANF_FX_FAR_BUY_SELL.toInt());
        }
    }
    
    /**
     * Returns value of Tran Info field 'JM_Transaction_Id'
     */
    @Override
    public String getTradeType(int leg) throws OException 
    {
        if(isNearLeg(leg))
        {
            return super.getTradeType(leg);
        }
        else
        {
            return this.farLegTran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, EndurTranInfoField.TRADE_TYPE.toString());
        }
    }

    /**
     * Returns value of Tran Info field 'Loco'
     */
    @Override
    public String getTradeLocation(int leg) throws OException 
    {
        if(isNearLeg(leg))
        {
            return super.getTradeLocation(leg);
        }
        else
        {
            return this.farLegTran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, EndurTranInfoField.LOCATION.toString());
        }
    }

    @Override
    public double getTradePrice(int leg) throws OException 
    {
        if(isNearLeg(leg))
        {
            return super.getTradePrice(leg);
        }
        else
        {
            return this.farLegTran.getFieldDouble(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, EndurTranInfoField.TRADE_PRICE.toString());
        }
    }

    @Override
    public double getCashAmount(int leg) throws OException 
    {
        if(isNearLeg(leg))
        {
            return super.getCashAmount(leg);
        }
        else
        {
            return tran.getFieldDouble(TRANF_FIELD.TRANF_FX_FAR_C_AMT.toInt(), 0);
        }
    }

    @Override
    public int getPaymentDate(int leg) throws OException 
    {
        if(isNearLeg(leg))
        {
            return super.getPaymentDate(leg);
        }
        else
        {
            /**
             * this.tran is always the near leg of the FX swap.
             * Source of this tran pointer is either-
             * (i) Input table to the UDSR
             * (ii) Retrieval of transaction in Util.getFxSwapOtherLegDetails() in case Input to the UDSR is far leg
             * There should be no need to fetch another tran pointer to Near Leg, but the api call to get TRANF_FX_FAR_TERM_SETTLE_DATE value using this.tran returns 0 in case (i) above. 
             */
            Transaction nearLeg = com.olf.openjvs.Util.NULL_TRAN;
            try
            {
                int nearLegTranNum = getTranNum(NEAR_LEG);
                nearLeg = Transaction.retrieve(nearLegTranNum);
                return nearLeg.getFieldInt(TRANF_FIELD.TRANF_FX_FAR_TERM_SETTLE_DATE.toInt());
            }
            finally
            {
                if (Transaction.isNull(nearLeg) != 1) 
                {
                    nearLeg.destroy();
                }
            }            
        }        
    }

}
