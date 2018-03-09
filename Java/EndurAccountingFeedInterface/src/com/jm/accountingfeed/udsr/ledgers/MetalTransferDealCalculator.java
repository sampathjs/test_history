package com.jm.accountingfeed.udsr.ledgers;

import com.jm.accountingfeed.util.Constants;
import com.olf.openjvs.OException;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.TRANF_FIELD;


public class MetalTransferDealCalculator extends BaseDealCalculator 
{
    public MetalTransferDealCalculator() 
    {

    }

    public MetalTransferDealCalculator(Transaction tran) 
    {
        super(tran);
    }

    @Override
    public int getFromCurrency(int leg) throws OException 
    {
        return tran.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt(), 0);
    }

    @Override
    public double getPositionUom(int leg) throws OException 
    {
        double position = getPositionToz(leg);
        position *= Transaction.getUnitConversionFactor(Constants.TROY_OUNCES, getUom(leg));  
        return position;        
    }

    @Override
    public double getPositionToz(int leg) throws OException 
    {

        return tran.getFieldDouble(TRANF_FIELD.TRANF_POSITION.toInt(), 0);
    }

    @Override
    public int getUom(int leg) throws OException 
    {
        return tran.getFieldInt(TRANF_FIELD.TRANF_UNIT.toInt(), 0);
    }
    
    @Override
    public int getSettlementDate(int leg) throws OException 
    {
        return tran.getFieldInt(TRANF_FIELD.TRANF_SETTLE_DATE.toInt(), 0);
    }
}