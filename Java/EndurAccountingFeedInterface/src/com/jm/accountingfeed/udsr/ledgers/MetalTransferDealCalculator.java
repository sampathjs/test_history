package com.jm.accountingfeed.udsr.ledgers;

import com.jm.accountingfeed.enums.EndurDealCommentType;
import com.jm.accountingfeed.exception.AccountingFeedRuntimeException;
import com.jm.accountingfeed.util.Constants;
import com.olf.openjvs.OException;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.BUY_SELL_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.TRANF_GROUP;
import com.olf.jm.logging.Logging;

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
    
    @Override
    public String getDealReference() throws OException 
    {
    	String reference = "";
    	int tranNum = tran.getTranNum();
    	
    	try
    	{
        	int buySell = tran.getFieldInt(TRANF_FIELD.TRANF_BUY_SELL.toInt(), 0);
        	
        	/* 
        	 * The reference field for a cash transfer should come from;
        	 * 1. FromAccountdeal comment for a Buy deal
        	 * 2. To Account deal comment for a Sell deal
        	 */
        	String targetCommentType = (buySell == BUY_SELL_ENUM.BUY.toInt() ? EndurDealCommentType.FROM_ACCOUNT.toString() : EndurDealCommentType.TO_ACCOUNT.toString());
        	
        	int ret = tran.loadDealComments();
    		if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
    		{
    			throw new AccountingFeedRuntimeException("Unable to load deal comments for tran_num: " + tranNum);
    		}
    		
    		int numDealComments = tran.getNumRows(-1, TRANF_GROUP.TRANF_GROUP_DEAL_COMMENTS.toInt());
    		for (int row = 0; row < numDealComments; row++)
    		{
    			String noteType = tran.getField(TRANF_FIELD.TRANF_DEAL_COMMENTS_NOTE_TYPE.toInt(), 0, "", row);
    			
    			if (targetCommentType.equalsIgnoreCase(noteType))
    			{
        			reference = tran.getField(TRANF_FIELD.TRANF_DEAL_COMMENTS_COMMENT.toInt(), 0, "", row);
        			break;
    			}
    		}
    	}
    	catch (Exception e)
    	{
    		String message = "Unable to set deal reference accordingly on tran_num: " + tranNum + ", " + e.getMessage();
    		Logging.error(message);
    	}
    	
    	return reference;
    }
}