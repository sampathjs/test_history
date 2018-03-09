package com.jm.accountingfeed.udsr.ledgers;

import com.olf.openjvs.OException;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.TRANF_GROUP;
import com.olf.openjvs.enums.VALUE_STATUS_ENUM;

public class MetalSwapsDealCalculator extends BaseDealCalculator 
{	
	public MetalSwapsDealCalculator() 
	{
		
	}
	
	public MetalSwapsDealCalculator(Transaction tran) 
	{
		super(tran);
	}
	
	@Override
	public int getFromCurrency(int leg) throws OException 
	{
	    return tran.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt(), 0);
	}

	@Override
	public int getToCurrency(int leg) throws OException 
	{
        return tran.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt(), 1);
	}

/*
	@Override
	public double getPositionUom(int leg) throws OException 
	{
	    return tran.getFieldDouble(TRANF_FIELD.TRANF_NOTNL.toInt(), 0);
	}

	@Override
	public double getPositionToz(int leg) throws OException 
	{
	    double position = getPositionUom(leg);

	    position *= Transaction.getUnitConversionFactor(getUom(leg), Constants.TROY_OUNCES);  

	    return position;
	}

	@Override
	public double getCashAmount() throws OException 
	{
	    return getTradePrice() * getPositionUom(1);
	}

    @Override
    public double getTradePrice() throws OException  
    {
        int numParams = tran.getNumParams();
        double resetValue = 0.0;
        int numOfFloatLegs = 0;
        for (int leg = 0; leg < numParams; leg++) 
        {
            String fxFloat = tran.getField(TRANF_FIELD.TRANF_FX_FLT.toInt(), leg);
            if ("float".equalsIgnoreCase(fxFloat)) 
            {
                numOfFloatLegs++;
                resetValue += getTradePrice(leg);

            }
        }
        if(numOfFloatLegs > 0)
        {
            resetValue /= numOfFloatLegs; //Average of all legs
        }

        return resetValue;
    }
    
    /**
     * Return the average of Known reset values
     * @param leg
     * @return
     * @throws OException
     *//*
    private double getTradePrice(int leg) throws OException 
    {
        int numOfResets = tran.getNumRows(leg, TRANF_GROUP.TRANF_GROUP_RESET.toInt());
        int numOfKnownResets = 0;
        double resetValueOnLeg = 0.0;
        for(int resetIndex =0; resetIndex < numOfResets; resetIndex++) 
        {
            int resetStatus = tran.getFieldInt(TRANF_FIELD.TRANF_RESET_VALUE_STATUS.toInt(), leg, "", resetIndex);
            if(VALUE_STATUS_ENUM.VALUE_KNOWN.toInt() == resetStatus || VALUE_STATUS_ENUM.VALUE_FIXED.toInt() == resetStatus) 
            {
                double resetValueOnRow = tran.getFieldDouble(TRANF_FIELD.TRANF_RESET_VALUE.toInt(), leg, "", resetIndex);
                resetValueOnLeg += resetValueOnRow;
                numOfKnownResets++;
            }
        }
        if(numOfKnownResets > 0)
        {
            resetValueOnLeg /=numOfKnownResets; //Average of all values
        }
        return resetValueOnLeg;
    }
    */
	
    @Override
    public int getUom(int leg) throws OException 
    {
        return tran.getFieldInt(TRANF_FIELD.TRANF_UNIT.toInt(), 0);
    }
    
	@Override
	public int getTradeDate() throws OException 
	{
		// The latest fixing date on reset tabs
        int numParams = tran.getNumParams();
        int tradeDate = 0;
        for (int leg = 0; leg < numParams; leg++) 
        {
            String fxFloat = tran.getField(TRANF_FIELD.TRANF_FX_FLT.toInt(), leg);
            if ("float".equalsIgnoreCase(fxFloat)) 
            {
                int maxResetDateOnLeg = getTradeDate(leg);
                if(maxResetDateOnLeg > tradeDate)
                {
                    tradeDate = maxResetDateOnLeg;
                }
            }
        }
        if(tradeDate == 0)
        {
            tradeDate = super.getTradeDate();
        }

		return tradeDate;
	}
	
	/**
	 * Return the latest reset date (on leg) where fixing has happened and price is known
	 * @param leg
	 * @return
	 * @throws OException
	 */
	private int getTradeDate(int leg) throws OException
	{
        int numOfResets = tran.getNumRows(leg, TRANF_GROUP.TRANF_GROUP_RESET.toInt());
        int maxResetDateOnLeg = 0;
        for(int resetIndex =0; resetIndex < numOfResets; resetIndex++) 
        {
            int resetStatus = tran.getFieldInt(TRANF_FIELD.TRANF_RESET_VALUE_STATUS.toInt(), leg, "", resetIndex);
            if(VALUE_STATUS_ENUM.VALUE_KNOWN.toInt() != resetStatus && VALUE_STATUS_ENUM.VALUE_FIXED.toInt() != resetStatus) 
            {
                break;
            }
            maxResetDateOnLeg = tran.getFieldInt(TRANF_FIELD.TRANF_RESET_DATE.toInt(), leg, "", resetIndex);
        }
        return maxResetDateOnLeg;
	}

    @Override
    public int getPaymentDate(int leg) throws OException 
    {
        //Payment date of first floating leg (side=1)
        return tran.getFieldInt(TRANF_FIELD.TRANF_PROFILE_PYMT_DATE.toInt(), 1, "", 0);
    }

    @Override
    public int getSettlementDate(int leg) throws OException 
    {
        //Payment date of fixed(metal) leg (side=0)
        return tran.getFieldInt(TRANF_FIELD.TRANF_PROFILE_PYMT_DATE.toInt(), 0, "", 0);
    }
}
