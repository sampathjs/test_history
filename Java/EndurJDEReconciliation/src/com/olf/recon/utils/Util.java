package com.olf.recon.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.BUY_SELL_ENUM;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.recon.enums.EndurDocumentStatus;
import com.olf.recon.exception.ReconciliationRuntimeException;
import com.openlink.util.logging.PluginLog;

/**
 * Helper class with misc static functions
 */
public class Util 
{
	private static HashSet<Integer> preciousMetalCurrencies = null;
	public static boolean isPreciousMetalCurrency(int currencyId) throws OException
	{	
		Table tblData = Table.tableNew("Precious Metal Currencies");
		
		try
		{
			if (preciousMetalCurrencies == null)
			{
				/* Lazy load */
				preciousMetalCurrencies = new HashSet<Integer>(); 
						
				tblData = Table.tableNew("Precious Metal Currencies");
				
				String sqlQuery = "select id_number from currency where precious_metal = 1";
				
				int ret = DBaseTable.execISql(tblData, sqlQuery);
				
				if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				{
					throw new ReconciliationRuntimeException("Unable to execute query: " + sqlQuery);
				}
				
				int numRows = tblData.getNumRows();
				for (int row = 1; row <= numRows; row++)
				{
					preciousMetalCurrencies.add(tblData.getInt("id_number", row));
				}
			}
			
			return preciousMetalCurrencies.contains(currencyId);	
		}
		finally
		{
			if (tblData != null)
			{
				tblData.destroy();
			}
		}
	}

	/**
	 * Round cash amounts as specified in JDE
	 * 
	 * @param value
	 * @param decimalPlaces
	 * @return
	 */
	public static BigDecimal roundCashAmount(double value) 
	{
		BigDecimal rounded = BigDecimal.valueOf(value);
		
		rounded = rounded.setScale(2, RoundingMode.HALF_UP);
		
	    return rounded;
	}

	/**
	 * Round notional position as specified in JDE
	 * 
	 * @param value
	 * @param decimalPlaces
	 * @return
	 */
	public static BigDecimal roundPosition(double value) 
	{
		BigDecimal rounded = BigDecimal.valueOf(value);
		
		rounded = rounded.setScale(4, RoundingMode.HALF_UP);
		
        return rounded;
	}
	
	/**
	 * Initialise a new logger instance
	 * 
	 * @throws OException
	 */
	public static void initialiseLog() throws OException
	{
		String abOutDir =  SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs\\";
		
		String logDir = abOutDir;
		String logLevel = "INFO";
		String logFile = Constants.LOG_FILE_NAME;

        try
        {
        	if (logDir.trim().equals("")) 
        	{
        		PluginLog.init(logLevel);
        	}
        	else  
        	{
        		PluginLog.init(logLevel, logDir, logFile);
        	}
        } 
		catch (Exception e) 
		{
			String errMsg = "Failed to initialize logging module.";
			com.olf.openjvs.Util.exitFail(errMsg);
			throw new RuntimeException(e);
		}
	}
	

	/**
	 * Adjust signage for extracts as per JDE rules
	 * 
	 *				Buy				Sell			Buy				Sell	 	 	 	 
 	 *				Validated	 	Validated		Cancelled	 	Cancelled 	 	 	 
	 *	position	-				+				+				-			[fromValue, baseWeight]	 	 
	 *	cash		+				-				-				+			[toValue, spotEqVal, taxDealCcy, taxReportCcy]
	 *
	 * The signage in Endur vs JDE is opposite (as one follows the Nostro style and the other follows Vostro) so
	 * this logic for signage has been agreed to sit on the Endur side with no custom signage needed on the JDE side
	 * of the interface
	 * 
	 * @param tblData
	 * @statusColumn - tran_status or endur_doc_status to identify a deal or invoice status
	 * @throws OException
	 */
	private static HashSet<String> positionColumns = null;
	private static HashSet<String> cashColumns = null;
	public static void adjustSignageForJDE(Table tblData, String statusColumn) throws OException
	{
		PluginLog.info("Adjusting signage of Position & Cash fields..");
	    int numRows = tblData.getNumRows();
		
		if (positionColumns == null)
		{
			positionColumns = new HashSet<String>();
			positionColumns.add("position_metal_unit");
			positionColumns.add("position_toz");
		}
		
		if (cashColumns == null)
		{
			cashColumns = new HashSet<String>();
            cashColumns.add("settlement_value");
            cashColumns.add("spot_equivalent_value");
            cashColumns.add("settlement_value_net");
            cashColumns.add("tax_amount_in_tax_ccy");
            cashColumns.add("settlement_value_gross");
		}
		
		for (int row = 1; row <= numRows; row++)
		{
			int buySell = tblData.getInt("buy_sell", row);
			int status = tblData.getInt(statusColumn, row);
			
			boolean isBuy = (BUY_SELL_ENUM.BUY.toInt() == buySell);
			boolean isSell = (BUY_SELL_ENUM.SELL.toInt() == buySell);
			
			boolean isValidated = (status == TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() 
			        || status == EndurDocumentStatus.SENT_TO_COUNTERPARTY.id()
			        || status == EndurDocumentStatus.SEL_FOR_PAYMENT.id()
			        || status == EndurDocumentStatus.UNDO_PAYMENT.id()
			        || status == EndurDocumentStatus.RECEIVE_PAYMENT.id()
					|| status == EndurDocumentStatus.IGNORE_FOR_PAYMENT.id());
			
			boolean isCancelled = (status == TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt() 
					|| status == EndurDocumentStatus.CANCELLED.id());
			
			for (String col : positionColumns)
			{
				if (tblData.getColNum(col) > 0)
				{
					double currentAbsoluteValue = Math.abs(tblData.getDouble(col, row));
					double newValue = currentAbsoluteValue;
					
					if ((isBuy && isValidated) || (isSell && isCancelled))
					{
						newValue *= -1;
					}

					tblData.setDouble(col, row, newValue);
				}
			}
			
			for (String col : cashColumns)
			{
				if (tblData.getColNum(col) > 0)
				{
					double currentAbsoluteValue = Math.abs(tblData.getDouble(col, row));
					double newValue = currentAbsoluteValue;
					
					if ((isSell && isValidated) || (isBuy && isCancelled))
					{
						newValue *= -1;
					}

					tblData.setDouble(col, row, newValue);
				}
			}
		}
	}
	
	/**
	 * The db stores all values as Toz (base unit) in ab_tran_events. So if this is a Kg trade (or any other unit different to Toz), 
	 * convert from Toz > trade unit. The original position value is added as new column and the existing column is updated. 
	 * 
	 * @param output
	 * @throws OException
	 */
	public static void convertPositionFromTOz(Table output) throws OException {
		output.addCol("position_toz", COL_TYPE_ENUM.COL_DOUBLE);
		
		int numRows = output.getNumRows();
		int toz = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, Constants.TROY_OUNCES);
		for (int row = 1; row <= numRows; row++)
		{
			int metalUnit = output.getInt("metal_unit", row);
			double metalPosition = output.getDouble("position_metal_unit", row);
			
			double metalPositionToz = metalPosition;
			if (metalUnit != toz)
			{
				metalPosition *= Transaction.getUnitConversionFactor(toz, metalUnit);
			}

			output.setDouble("position_metal_unit", row, metalPosition);
			output.setDouble("position_toz", row, metalPositionToz);
		}
	}
}
