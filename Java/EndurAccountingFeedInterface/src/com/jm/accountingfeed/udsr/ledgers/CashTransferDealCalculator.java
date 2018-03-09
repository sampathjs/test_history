package com.jm.accountingfeed.udsr.ledgers;

import com.olf.openjvs.OException;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.TRANF_FIELD;


public class CashTransferDealCalculator extends BaseDealCalculator 
{
    public CashTransferDealCalculator() 
    {

    }

    public CashTransferDealCalculator(Transaction tran) 
    {
        super(tran);
    }

    @Override
    public int getToCurrency(int leg) throws OException 
    {
        return tran.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt(), 0);
    }

    @Override
    public double getCashAmount(int leg) throws OException 
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