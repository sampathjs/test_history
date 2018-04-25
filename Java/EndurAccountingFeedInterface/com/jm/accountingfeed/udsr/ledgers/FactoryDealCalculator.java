package com.jm.accountingfeed.udsr.ledgers;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.CFLOW_TYPE;
import com.olf.openjvs.enums.INS_SUB_TYPE;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;

/**
 * Factory class to create an Object of IDealCalculator based on the Toolset & Cash flow type of underlying Transaction
 * Single object of FactoryDealCalculator shall be used to fetch calculator of all the Transaction in the Ledger.
 * Initialisation of few implementations of IDealCalculator (e.g. FXSwapsDealCalculator) may perform bulk DB operation on all Transactions of the Ledger
 * @author jains03
 *
 */
public class FactoryDealCalculator 
{
	private IDealCalculator defaultCalculator;
	private IDealCalculator fxCalculator;
	private IDealCalculator comFutCalculator;
	private IDealCalculator comSwapCalculator;
	private IDealCalculator loanDepCalculator;
	private IDealCalculator commodityCalculator;
	private IDealCalculator cashTransferCalculator;
    private IDealCalculator metalTransferCalculator;
	private IDealCalculator fxSwapCalculator;
	
	public FactoryDealCalculator(Table tblTransactions) throws OException
	{
		defaultCalculator = new BaseDealCalculator();
		fxCalculator = new FXDealCalculator();
		comFutCalculator = new ComFutDealCalculator();
		comSwapCalculator = new MetalSwapsDealCalculator();
		loanDepCalculator = new LoanDepDealCalculator();
		commodityCalculator = new CommodityDealCalculator();
		cashTransferCalculator = new CashTransferDealCalculator();		
        metalTransferCalculator = new MetalTransferDealCalculator();        
		fxSwapCalculator = new FXSwapsDealCalculator(tblTransactions);
	}
	
	/**
	 * Returns the Concrete Calculator object based on Toolset & Cash flow type of @param tran
	 * @param tran
	 * @return
	 * @throws OException
	 */
	public IDealCalculator getDealCalculator(Transaction tran) throws OException
	{
		int toolset = tran.getFieldInt(TRANF_FIELD.TRANF_TOOLSET_ID.toInt());
		int cflowType = tran.getFieldInt(TRANF_FIELD.TRANF_CFLOW_TYPE.toInt(), 0);
        int insSubType = tran.getFieldInt(TRANF_FIELD.TRANF_INS_SUB_TYPE.toInt());
		
		IDealCalculator calculator = defaultCalculator;
		
		if (toolset == TOOLSET_ENUM.FX_TOOLSET.toInt() && 
				(cflowType == CFLOW_TYPE.FX_SWAP_CFLOW.toInt() || 
				cflowType == CFLOW_TYPE.FX_LOCATION_SWAP_CFLOW.toInt() || 
				cflowType == CFLOW_TYPE.FX_QUALITY_SWAP_CFLOW.toInt()))
		{
			calculator = fxSwapCalculator;
		}
		else if (toolset == TOOLSET_ENUM.FX_TOOLSET.toInt())
		{
			calculator = fxCalculator;
		}
		else if (toolset == TOOLSET_ENUM.COM_FUT_TOOLSET.toInt())
		{
			calculator = comFutCalculator;
		}
		else if (toolset == TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt())
		{
			calculator = comSwapCalculator;
		}
		else if (toolset == TOOLSET_ENUM.LOANDEP_TOOLSET.toInt())
		{
			calculator = loanDepCalculator;
		}
		else if (toolset == TOOLSET_ENUM.COMMODITY_TOOLSET.toInt())
		{
			calculator = commodityCalculator;
		}
		/**
		 * Cash deals can be Cash Transfer (Ins sub type = cash tran) OR Metal Transfer (Ins sub type = cash transfer)
		 * 
         *                 fromCurrency    fromValue   toCurrency       toValue     .xml file
         * Metal Transfer  Populated        Populated   Empty           Empty       ML
         * Cash Transfer   Empty            Empty       Populated       Populated   SL

		 */
		else if (toolset == TOOLSET_ENUM.CASH_TOOLSET.toInt() && insSubType == INS_SUB_TYPE.cash_transaction.toInt())
		{
			calculator = cashTransferCalculator;
		}
        else if (toolset == TOOLSET_ENUM.CASH_TOOLSET.toInt())
        {
            calculator = metalTransferCalculator;
        }
		
        calculator.initialize(tran);
		
        return calculator;
	}
}
