package com.jm.accountingfeed.udsr.ledgers;

import java.util.HashMap;

import com.jm.accountingfeed.exception.AccountingFeedRuntimeException;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TRANF_FIELD;

public class ComFutDealCalculator extends BaseDealCalculator 
{
	/* Map to store projection index on future contracts and the corresponding metal currency */
	private static HashMap<Integer, Integer> metalProjectionIndexCurrency = null;
	
	public ComFutDealCalculator()
	{
		setupProjectionIndexMap();
	}
	
	public ComFutDealCalculator(Transaction tran) 
	{
		super(tran);

		setupProjectionIndexMap();
	}
	
	/**
	 * Initialise the static map
	 */
	private void setupProjectionIndexMap()
	{
		if (metalProjectionIndexCurrency == null)
		{
			metalProjectionIndexCurrency = new HashMap<Integer, Integer>();
		}
	}
	
	@Override
	public double getPositionUom(int leg) throws OException 
	{
		double lots = tran.getFieldDouble(TRANF_FIELD.TRANF_POSITION.toInt(), 0);
		double contractSize = tran.getFieldDouble(TRANF_FIELD.TRANF_NOTNL.toInt(), 0);
		
		return lots * contractSize;
	}
	
	@Override
	public double getPositionToz(int leg) throws OException 
	{
		return getPositionUom(leg);
	}
	
	@Override
	public double getCashAmount(int leg) throws OException 
	{
		return getTradePrice(leg) * getPositionUom(leg);
	}

	@Override
	public int getFromCurrency(int leg) throws OException 
	{
		Table tblIndexData = null;
		
		try
		{
			int projectionIndex = tran.getFieldInt(TRANF_FIELD.TRANF_PROJ_INDEX_FILTER.toInt());
			
			if (!metalProjectionIndexCurrency.containsKey(projectionIndex))
			{
				tblIndexData = Table.tableNew();
				
				/* Future contracts are minimal so grab and cache! */
				String sqlQuery = "SELECT currency2 FROM idx_def id WHERE id.index_id = " + projectionIndex + " AND id.db_status = 1";
				
				int ret = DBaseTable.execISql(tblIndexData, sqlQuery);
				
				if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				{
					throw new AccountingFeedRuntimeException("Unable to run query: " + sqlQuery);
				}
				
				int currency = tblIndexData.getInt("currency2", 1);
				
				metalProjectionIndexCurrency.put(projectionIndex, currency);
			}
			
			if (metalProjectionIndexCurrency.containsKey(projectionIndex))
			{
				return metalProjectionIndexCurrency.get(projectionIndex);
			}
			
			return tran.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt());
		}
		finally
		{
			if (tblIndexData != null)
			{
				tblIndexData.destroy();
			}
		}
	}
	
	@Override
	public int getToCurrency(int leg) throws OException
	{
		return tran.getFieldInt(TRANF_FIELD.TRANF_CURRENCY.toInt());
	}
	
    @Override
    public int getSettlementDate(int leg) throws OException 
    {
        return tran.getHoldingTran().getFieldInt(TRANF_FIELD.TRANF_SETTLE_DATE.toInt());
    }

    @Override
    public int getUom(int leg) throws OException 
    {
        return tran.getFieldInt(TRANF_FIELD.TRANF_UNIT.toInt(), 0);
    }
    
	@Override
	public double getTradePrice(int leg) throws OException 
	{
		return tran.getFieldDouble(TRANF_FIELD.TRANF_PRICE.toInt(), 0);
	}
	
    @Override
    public int getPaymentDate(int leg) throws OException 
    {
        //Payment date of first leg (side=0)
        return tran.getHoldingTran().getFieldInt(TRANF_FIELD.TRANF_PROFILE_PYMT_DATE.toInt(), 0, "", 0);
    }   
}
