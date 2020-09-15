package com.jm.accountingfeed.udsr.ledgers;

import com.jm.accountingfeed.enums.EndurTranInfoField;
import com.jm.accountingfeed.util.Constants;
import com.jm.accountingfeed.util.Util;
import com.olf.openjvs.OException;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.jm.logging.Logging;

public class FXDealCalculator extends BaseDealCalculator 
{	
	public FXDealCalculator() 
	{
		
	}
	
	public FXDealCalculator(Transaction tran) 
	{
		super(tran);
	}
	
	@Override
	public int getFromCurrency(int leg) throws OException 
	{
		return tran.getFieldInt(TRANF_FIELD.TRANF_BASE_CURRENCY.toInt(), 0);
	}
	
	@Override
	public int getToCurrency(int leg) throws OException 
	{
		return tran.getFieldInt(TRANF_FIELD.TRANF_BOUGHT_CURRENCY.toInt(), 0);
	}
	
	@Override
    public int getSettlementDate(int leg) throws OException {
	    int settlementDate = 0;
        if(getPreciousMetal(1) == 1)
        {
            settlementDate = tran.getFieldInt(TRANF_FIELD.TRANF_SETTLE_DATE.toInt());
        }
        else
        {
            settlementDate = tran.getFieldInt(TRANF_FIELD.TRANF_FX_TERM_SETTLE_DATE.toInt());
        }
        return settlementDate;
    }

    @Override
	public double getPositionUom(int leg) throws OException 
	{
    	double positionUom = 0.0;
    	
    	/* 
    	 * This fetches the number exactly how you see on the deal skin. It was observed that a tran.getFieldDouble on an FX SWAP
    	 * with cflow_type = Spot returns the uom position auto-converted to the base weight.
    	 * 
    	 * We use the deal skin value here to avoid any supplementary conversion in-case this behaves any different for any other
    	 * FX cflow type.
    	 */
    	String amount = tran.getField(TRANF_FIELD.TRANF_FX_D_AMT.toInt(), 0);
    	
    	if (amount != null && !"".equalsIgnoreCase(amount))
    	{
    		try
    		{
    			String parseAmount = amount.replace(",", "");
    			positionUom = Double.valueOf(parseAmount);
    	    	
    		}
    		catch (Exception e)
    		{
    			Logging.error("Problem converting amount: " + amount + " to monetary value for deal: " + tran.getField(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt()));
    		}
    	}
    	
		return positionUom;
	}
	
	@Override
	public double getPositionToz(int leg) throws OException 
	{
		double position = getPositionUom(leg);
		int metalUnit = tran.getFieldInt(TRANF_FIELD.TRANF_FX_BASE_CCY_UNIT.toInt(), 0);
		int metalCurrencyId = tran.getFieldInt(TRANF_FIELD.TRANF_BASE_CURRENCY.toInt(), 0);
		
		if (Util.isPreciousMetalCurrency(metalCurrencyId) && metalUnit != Constants.TROY_OUNCES)
		{
			position *= Transaction.getUnitConversionFactor(metalUnit, Constants.TROY_OUNCES);	
		}
		
		return position;
	}
	
	@Override
	public double getCashAmount(int leg) throws OException 
	{
		return getCashAmount(leg, TRANF_FIELD.TRANF_FX_C_AMT);
	}
	
	@Override
	public double getCashAmount(int leg, TRANF_FIELD field) throws OException {
		double cashAmount = 0.0;
		String cashAmountStr = tran.getField(field.toInt(), 0);
		
		if (cashAmountStr != null && !"".equalsIgnoreCase(cashAmountStr)) {
			try {
				String parseAmount = cashAmountStr.replace(",", "");
				cashAmount = Double.valueOf(parseAmount);
			} catch (Exception e) {
				Logging.error("Problem converting amount: " + cashAmountStr + " to monetary value for deal: "  + tran.getField(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt()));
			}
		}

		if (cashAmount == 0.0) {
			cashAmount = tran.getFieldDouble(field.toInt(), 0);
		}

		return cashAmount;
	}

	
	@Override
	public int getUom(int leg) throws OException 
	{
		return tran.getFieldInt(TRANF_FIELD.TRANF_FX_BASE_CCY_UNIT.toInt(), 0);
	}

	@Override
	public double getTradePrice(int leg) throws OException 
	{
		return tran.getFieldDouble(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, EndurTranInfoField.TRADE_PRICE.toString());
	}
	
    @Override
    public int getPaymentDate(int leg) throws OException 
    {
        return tran.getFieldInt(TRANF_FIELD.TRANF_FX_TERM_SETTLE_DATE.toInt());
    }
}
