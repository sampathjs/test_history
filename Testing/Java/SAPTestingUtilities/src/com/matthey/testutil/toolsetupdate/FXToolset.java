package com.matthey.testutil.toolsetupdate;

import com.matthey.testutil.enums.EndurTranInfoField;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.CFLOW_TYPE;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.jm.logging.Logging;

/**
 * Concrete class of Deal Injector specific to FX Toolset
 * @author SharmV04
 *
 */
public class FXToolset extends PerpetualToolset 
{
	/**
	 * @param argClonedTransaction
	 * @param argDealDelta
	 */
	public FXToolset(Transaction argClonedTransaction, DealDelta argDealDelta) 
	{
		super(argClonedTransaction, argDealDelta);
	}

	/**
	 * Sets the value of following FX specific transaction fields -
	 * (i) FX date
	 * (ii) Metal Settle date
	 * (ii) Cash Settle date
	 */
	public void updateToolset() throws OException 
	{
		final String TRADE_PRICE_INFO_FIELD_NAME = "Trade Price";
		int cflowType;
		super.updateToolset();
		String tradeUnit = transaction.getField(TRANF_FIELD.TRANF_FX_TERM_CCY_UNIT.toInt());
		double dealtRate;
		double tradePrice;
		final int FMT_PREC = 8;
		final int FMT_WIDTH = 12;
		
		String fxDate = dealDelta.getFxDate();
		
		Logging.info("Started updating fiels in toolset");
		
		updateTransactionField(TRANF_FIELD.TRANF_BUY_SELL, 0, dealDelta.getBuySell());
		updateTransactionField(TRANF_FIELD.TRANF_TICKER, 0, dealDelta.getTicker());
		updateTransactionField(TRANF_FIELD.TRANF_TRADE_DATE, 0, dealDelta.getTradeDate());
		
		updateTransactionField(TRANF_FIELD.TRANF_CFLOW_TYPE, 0, dealDelta.getCflowType());
		
		updateTransactionField(TRANF_FIELD.TRANF_INTERNAL_BUNIT, 0, dealDelta.getOurUnit());
		updateTransactionField(TRANF_FIELD.TRANF_INTERNAL_PORTFOLIO	, 0, dealDelta.getOurPfolio());
		updateTransactionField(TRANF_FIELD.TRANF_EXTERNAL_BUNIT, 0, dealDelta.getCeUnit());
		updateTransactionField(TRANF_FIELD.TRANF_INTERNAL_CONTACT, 0, dealDelta.getTrader());
		
		updateTransactionField(TRANF_FIELD.TRANF_FX_DATE,0, fxDate);
		updateTransactionField(TRANF_FIELD.TRANF_SETTLE_DATE,0, fxDate);
		updateTransactionField(TRANF_FIELD.TRANF_FX_TERM_SETTLE_DATE,0, fxDate);
		
		//We have to set unit of measure before setting the weight otherwise it will change value of weight 
		updateTransactionField(TRANF_FIELD.TRANF_FX_BASE_CCY_UNIT, 0, dealDelta.getMetalUom());
		updateTransactionField(TRANF_FIELD.TRANF_FX_D_AMT, 0, dealDelta.getMetalWeight());
		
		if (tradeUnit.equalsIgnoreCase("Currency"))
		{
			tradeUnit = transaction.getField(TRANF_FIELD.TRANF_FX_BASE_CCY_UNIT.toInt());
		}

		tradePrice = transaction.getFieldDouble(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, TRADE_PRICE_INFO_FIELD_NAME);
		dealtRate = getDealtRate( tradePrice, tradeUnit );
		cflowType = transaction.getFieldInt(TRANF_FIELD.TRANF_CFLOW_TYPE.toInt(), 0);
		if(cflowType == CFLOW_TYPE.FX_SPOT_CFLOW.toInt())
		{
			
		}
		else
		{
			transaction.setField(TRANF_FIELD.TRANF_FX_DEALT_RATE.toInt(), 
				0, "", Str.formatAsDouble(dealtRate, FMT_WIDTH, FMT_PREC));
		}
		
		double fxSpotRate = transaction.getFieldDouble(TRANF_FIELD.TRANF_FX_SPOT_RATE.toInt(), 0);
		String metalUnit =  transaction.getField(TRANF_FIELD.TRANF_FX_BASE_CCY_UNIT.toInt(), 0);
		int metalUnitRef = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, metalUnit);
		int toz = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "TOz");
		
		double newTradePrice = fxSpotRate *= Transaction.getUnitConversionFactor(toz, metalUnitRef);
		updateTranInfoField(EndurTranInfoField.TRADE_PRICE.toString(), newTradePrice + "");
		dealDelta.setTradePrice(newTradePrice + "");
		
		Logging.info("Completed updating fiels in toolset");
	}
	
	@Override
	public Transaction createClone() throws OException
	{
		
		super.createClone();			
		return this.transaction;
	}
	
	/**
	 * @param toUnit
	 * @param fromUnit
	 * @return
	 * @throws OException
	 */
	private double getConversionFactor(String toUnit, String fromUnit) throws OException
	{
		if (fromUnit.equalsIgnoreCase(toUnit))
		{
			return 1.0;
		}
		String sql = "\nSELECT uc.factor" + "\nFROM unit_conversion uc" + "\nINNER JOIN idx_unit src" + "\nON src.unit_label = '" + fromUnit + "'" + "\n  AND src.unit_id = uc.src_unit_id" + "\nINNER JOIN idx_unit dest" + "\n  ON dest.unit_label = '"
				+ toUnit + "'" + "\n  AND dest.unit_id = uc.dest_unit_id";
		Table factorTable = null;
		try
		{
			Logging.info("Started calculating conversion factor");
			Logging.debug("sql: " + sql);
			factorTable = Table.tableNew("conversion factor from " + fromUnit + " to " + toUnit);
			int ret = DBaseTable.execISql(factorTable, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				throw new OException("Error executing SQL " + sql);
			}
			if (factorTable.getNumRows() != 1)
			{
				throw new IllegalArgumentException("There is no unit conversion factor defined from " + fromUnit + " to " + toUnit);
			}
			Logging.info("Completed calculating conversion factor");
			return factorTable.getDouble("factor", 1);
		}
		finally
		{
			if( Table.isTableValid(factorTable) == 1 )
			{
				factorTable.destroy();
			}
			Logging.info("Completed calculating conversion factor");
		}
	}
	
	/**
	 * @param tradePrice
	 * @param tradeUnit
	 * @return
	 * @throws OException
	 */
	private double getDealtRate(double tradePrice, String tradeUnit) throws OException
	{
		final String UNIT_OUNCE_TROY = "TOz";
		double dealtRate;
		
		double conversionFactor = 1.0;
		if (!tradeUnit.equalsIgnoreCase("Currency")) 
		{
			conversionFactor = getConversionFactor(tradeUnit, UNIT_OUNCE_TROY);
		} 
		else
		{
			Logging.info("Using conversionFactor = 1.0 for Currency trade");
		}
		
		dealtRate= tradePrice * conversionFactor;
		return dealtRate;
		
	}
}
