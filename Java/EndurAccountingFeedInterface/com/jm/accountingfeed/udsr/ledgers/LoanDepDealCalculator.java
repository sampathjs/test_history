package com.jm.accountingfeed.udsr.ledgers;

import com.jm.accountingfeed.util.Constants;
import com.olf.openjvs.OException;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.TRANF_FIELD;

public class LoanDepDealCalculator extends BaseDealCalculator
{
	public LoanDepDealCalculator() 
	{
		
	}
	
	public LoanDepDealCalculator(Transaction tran) 
	{
		super(tran);
	}
	
	@Override
	public int getEndDate() throws OException
	{
		int endDate = tran.getFieldInt(TRANF_FIELD.TRANF_MAT_DATE.toInt(), 1);
		return endDate;
	}
	
	@Override
	public double getPositionUom(int leg) throws OException
	{
		double positionUom = tran.getFieldDouble(TRANF_FIELD.TRANF_NOTNL.toInt(), leg);
		int unit = getUom(leg);

		/*We need to do conversion here because due to some issue with JVS we are getting notional in Toz unit*/
       	positionUom *= Transaction.getUnitConversionFactor( Constants.TROY_OUNCES, unit);
        
		return positionUom;
	}
	
	@Override
	public int getUom(int leg) throws OException
	{
		int uom = tran.getFieldInt(TRANF_FIELD.TRANF_UNIT.toInt(), leg);
		return uom;
	}
	
	@Override
    public double getPositionToz(int leg) throws OException 
    {
		/*We are using here notional because due to some issue with JVS it is already converted in Toz*/
        double position = tran.getFieldDouble(TRANF_FIELD.TRANF_NOTNL.toInt(), leg);

        return position;
    }

	
	@Override
	public int getToCurrency(int leg) throws OException
	{
		return tran.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt());
	}

	public int getSettlementDate(int leg) throws OException
	{
		int startDate = tran.getFieldInt(TRANF_FIELD.TRANF_START_DATE.toInt(), leg);
		return startDate;
	}
	
	@Override
	public double getInterestRate() throws OException 
	{
		double interestRate = tran.getFieldDouble( TRANF_FIELD.TRANF_RATE.toInt() );
		return interestRate;
	}
	
	@Override
	public int getFromCurrency(int leg) throws OException 
	{
		return tran.getFieldInt(TRANF_FIELD.TRANF_NOTNL_CURRENCY.toInt());
	}
}