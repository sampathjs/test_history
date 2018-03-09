package com.jm.accountingfeed.udsr.ledgers;

import java.util.HashMap;

import com.jm.accountingfeed.util.Constants;
import com.jm.accountingfeed.util.Util;
import com.olf.openjvs.OException;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.OPTION_SETTLEMENT_TYPE;
import com.olf.openjvs.enums.TRANF_FIELD;

public class CommodityDealCalculator extends BaseDealCalculator 
{
    private HashMap<Integer, Integer> legToPhySideMap = null;
    private HashMap<Integer, Integer> legToFinSideMap = null;
    
    protected int physicalSettlement = -1;
    protected int cashSettlement = -1;
    
	public CommodityDealCalculator() throws OException 
	{
		physicalSettlement = OPTION_SETTLEMENT_TYPE.SETTLEMENT_TYPE_PHYSICAL.toInt();
		cashSettlement = OPTION_SETTLEMENT_TYPE.SETTLEMENT_TYPE_CASH.toInt();
	}
	
	@Override
	public void initialize(Transaction tran) 
	{
	    super.initialize(tran);
	    this.legToPhySideMap = new HashMap<>();
	    this.legToFinSideMap = new HashMap<>();
	}

	public CommodityDealCalculator(Transaction tran) 
	{
		super(tran);
	}

    @Override
    public int getNumLegs() throws OException
    {
        return getSide(physicalSettlement, -1);
    }

    @Override
    public int getFinSideNum(int leg) throws OException 
    {
        int finSide = 0;
        
        if (this.legToFinSideMap.containsKey(leg))
        {
            finSide = this.legToFinSideMap.get(leg);   
        }
        else
        {
            finSide = getSide(cashSettlement, leg);
           
            this.legToFinSideMap.put(leg, finSide);
        }
        return finSide;
    }


    /**
     * Gets the physical-side of the leg using cache
     * 
     * @param leg
     * @return
     * @throws OException
     */
    @Override
    public int getPhySideNum(int leg) throws OException
    {
        int phySide = 0;
        
        if (this.legToPhySideMap.containsKey(leg))
        {
            phySide = this.legToPhySideMap.get(leg);   
        }
        else
        {
            phySide = getSide(physicalSettlement, leg);
           
            this.legToPhySideMap.put(leg, phySide);
        }
        return phySide;
    }
    
    /**
     * In case @param leg=0, returns total count of sides of the @param settlementType
     * In case @param leg > 0, returns the side number of the leg. E.g. side number is 3 for settlementType=Physical and leg=2;side number is 4 for settlementType=Financial and leg=2
     * Ignores side=0 during all calculations
     * @param settlementType. For Physical=2, Financial=1
     * @param specificLeg
     * @return
     * @throws OException
     */
    private int getSide(int settlementType, int specificLeg) throws OException
    {
        int paramCount = tran.getNumParams();
        
        int numSettlementTypeLegsFound = 0;
        int sideNumber = 1; //Ignore side=0
        
        for(;sideNumber < paramCount; sideNumber++) 
        {
            int settleType = tran.getFieldInt(TRANF_FIELD.TRANF_SETTLEMENT_TYPE.toInt(), sideNumber);
            
            if(settleType == settlementType) 
            {
                numSettlementTypeLegsFound++;
                
                if (numSettlementTypeLegsFound == specificLeg) break;
            }
        }
        
        if (specificLeg == -1)
        {
            return numSettlementTypeLegsFound;            
        }
        else
        {
            return sideNumber;            
        }
    }
    
    @Override
    public String getLocation(int leg) throws OException 
    {
        return tran.getField(TRANF_FIELD.TRANF_LOCATION.toInt(), getPhySideNum(leg));
    }

    @Override
    public int getForm(int leg) throws OException 
    {
        return tran.getFieldInt(TRANF_FIELD.TRANF_COMMODITY_FORM.toInt(), getPhySideNum(leg));
    }
	
    @Override
    public int getPurity(int leg) throws OException 
    {
        return tran.getFieldInt(TRANF_FIELD.TRANF_MEASURE_GROUP.toInt(), getPhySideNum(leg));
    }

    @Override
    public int getFromCurrency(int leg) throws OException 
    {
    	int financialSideCurrency = tran.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt(), getFinSideNum(leg));
    			
    	if (Util.isPreciousMetalCurrency(financialSideCurrency))
    	{
    		return financialSideCurrency;
    	}
    	
    	/* Default case = physical side currency */
    	return tran.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt(), getPhySideNum(leg));
    }

    @Override
    public double getPositionUom(int leg) throws OException 
    {
        return tran.getFieldDouble(TRANF_FIELD.TRANF_DEAL_VOLUME.toInt(), getPhySideNum(leg));
    }

    @Override
    public double getPositionToz(int leg) throws OException 
    {
        double position = getPositionUom(leg);

        position *= Transaction.getUnitConversionFactor(getUom(leg), Constants.TROY_OUNCES);  

        return position;
    }

    @Override
    public int getUom(int leg) throws OException 
    {
        return tran.getFieldInt(TRANF_FIELD.TRANF_UNIT.toInt(), getPhySideNum(leg));
    }
    
    @Override
    public int getSettlementDate(int leg) throws OException 
    {
    	/* 
    	 * This is the value date for a COMM-PHYS. It should be equivalent to the metal event date 
    	 * from ab _tran_events. The profile tab will not be seen on COMM-PHYS trades, but there is a profile
    	 * structure in the database so we can just grab the first profile element on the physical leg
    	 */
    	return tran.getFieldInt(TRANF_FIELD.TRANF_PROFILE_PYMT_DATE.toInt(), getPhySideNum(leg), "", 0);
    }
}