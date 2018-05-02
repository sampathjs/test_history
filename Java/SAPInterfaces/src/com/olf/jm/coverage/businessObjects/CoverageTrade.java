package com.olf.jm.coverage.businessObjects;

import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.SapEndurTrade;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.TradingFactory;
import com.openlink.util.logging.PluginLog;


/**
 * The Class CoverageTrade. Represents a existing trade in the database. Trade is used during the validation
 * process to check the values in the message against the existing trade.
 */
public class CoverageTrade  extends SapEndurTrade implements ICoverageTrade {


	
	/**
	 * Instantiates a new coverage trade, not loading a trade from the database.
	 */
	public CoverageTrade() {
	}
	
	/**
	 * Instantiates a new coverage trade loading a trade from the database based on the 
	 * Endur deal id.
	 *
	 * @param context the OC context
	 * @param endurDealId the Endur deal id
	 */
	public CoverageTrade(final Context context, final int endurDealId) {
		super(context, endurDealId);
	}
	
	/**
	 * Instantiates a new coverage trade loading a trade from the database based on
	 * the the info field sapOrderId.
	 *
	 * @param context the context
	 * @param sapOrderId the sap order id
	 */
	public CoverageTrade(final Context context, final String sapOrderId) {
		
		//String sql = "select * from ab_tran_info_view where type_name = 'SAP_Order_ID' and value = '" + sapOrderId + "'";
		
		String sql = "select  ativ.tran_num from ab_tran_info_view  ativ " 
				+ " join ab_tran ab on ativ.tran_num = ab.tran_num and current_flag = 1 "
				+ "where value = '" + sapOrderId + "'  and type_name = 'SAP_Order_ID'";
		
		IOFactory ioFactory = context.getIOFactory();
		
		PluginLog.debug("Running SQL \n. " + sql);
		
		try (Table rawData = ioFactory.runSQL(sql)) {

			if (rawData.getRowCount() == 1) {
				int tranNumber = rawData.getInt("tran_num", 0);
				TradingFactory tf = context.getTradingFactory();
				trancaction = tf.retrieveTransactionById(tranNumber);
			} else if (rawData.getRowCount() > 1) {
				String errorMessage = "Error loading tran for id " + sapOrderId + ". More than one trade with with id detected";
				PluginLog.error(errorMessage);
				throw new RuntimeException(errorMessage);
			}
		} catch (Exception e) {
			String errorMessage = "Error loading tran for id " + sapOrderId + ". " + e.getMessage();
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		} 
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.ICoverageTrade#getQuotationWeight()
	 */
	@Override
	public final double getQuotationWeight() {
		usageGuard("QuotationWeight");
		double quotationWeight = 0.0;
		switch (trancaction.getToolset()) {
		case Fx:
			// Use the string value to avoid the TOz conversion trancaction.getValueAsDouble(EnumTransactionFieldId.FxDealtAmount);
			quotationWeight = new Double(trancaction.getValueAsString(EnumTransactionFieldId.FxDealtAmount).replace(",", ""));
			
			break;	
		case ComSwap:			
			quotationWeight = trancaction.getLeg(0).getValueAsDouble(EnumLegFieldId.Notional);
			break;
			
		default:
			throw new RuntimeException("Unsupported instrument type " + trancaction.getToolset());
		}
		return quotationWeight;
		 
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.ICoverageTrade#getQuotationUOM()
	 */
	@Override
	public final String getQuotationUOM() {
		usageGuard("QuotationUOM");
		String uom = null;
		switch (trancaction.getToolset()) {
		case Fx:
			uom = trancaction.getValueAsString(EnumTransactionFieldId.FxBaseCurrencyUnit);
			break;
		case ComSwap:
			uom = trancaction.getLeg(1).getValueAsString(EnumLegFieldId.PriceUnit);
			break;
			
		default:
			throw new RuntimeException("Unsupported instrument type " + trancaction.getToolset());
		}
		return uom;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.ICoverageTrade#getQuotationPrice()
	 */
	@Override
	public final double getQuotationPrice() {
		usageGuard("QuotationPrice");
		double price = 0;
		switch (trancaction.getToolset()) {
		case Fx:
			//Get the price from the tran info field
			try (Field tradePrice = trancaction.getField("Trade Price")) {
				price = tradePrice.getValueAsDouble();
			} catch (Exception e) {
				throw new RuntimeException("Error reading the trade price. " + e.getMessage());
			}
			break;	
		case ComSwap:	
			throw new RuntimeException("Field  QuotationPrice is not supported for toolset" + trancaction.getToolset());
			
		default:
			throw new RuntimeException("Unsupported instrument type " + trancaction.getToolset());
		}		
		return price;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.ICoverageTrade#getQuotationCurrency()
	 */
	@Override
	public final String getQuotationCurrency() {
		usageGuard("QuotationCurrency");
		String currency = null;
		switch (trancaction.getToolset()) {
		case Fx:
			currency = trancaction.getLeg(0).getValueAsString(EnumLegFieldId.BoughtCurrency);
			break;	
		case ComSwap:	
			currency = trancaction.getLeg(1).getValueAsString(EnumLegFieldId.Currency);
			break;
			
		default:
			throw new RuntimeException("Unsupported instrument type " + trancaction.getToolset());
		}		
		return currency;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.ICoverageTrade#getQuotationContract()
	 */
	@Override
	public final Date getQuotationContractDate() {
		usageGuard("QuotationContractDate");
		Date contractDate = null;
		switch (trancaction.getToolset()) {
		case Fx:
		case ComSwap:	
			contractDate = trancaction.getValueAsDate(EnumTransactionFieldId.TradeDate);
			break;
			
		default:
			throw new RuntimeException("Unsupported instrument type " + trancaction.getToolset());
		}		
		return contractDate;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.ICoverageTrade#getQuotationValue()
	 */
	@Override
	public final double getQuotationValue()  {
		usageGuard("QuotationValue");
		switch (trancaction.getToolset()) {
		case Fx:
			break;	
		case ComSwap:			
			break;
			
		default:
			throw new RuntimeException("Unsupported instrument type " + trancaction.getToolset());
		}		
		return 0;
	}
	


	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.ICoverageTrade#getQuotationValueDate()
	 */
	@Override
	public final Date getQuotationValueDate() {
		usageGuard("QuotationValueDate");
		Date valueDate = null;
		switch (trancaction.getToolset()) {
		case Fx:
			valueDate = trancaction.getValueAsDate(EnumTransactionFieldId.FxDate);
			break;	
		case ComSwap:			
			valueDate = trancaction.getLeg(0).getValueAsDate(EnumLegFieldId.StartDate);
			break;
			
		default:
			throw new RuntimeException("Unsupported instrument type " + trancaction.getToolset());
		}		
		return valueDate;
	}



	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.ICoverageTrade#isCoverage()
	 */
	@Override
	public final boolean isCoverage() {
		usageGuard("isCoverage");
		boolean returnValue = false;
		switch (trancaction.getToolset()) {
		case Fx:
		case ComSwap:			
			try (Field isCoverage = trancaction.getField("IsCoverage"))	{
				
				if (isCoverage.getValueAsString().equalsIgnoreCase("Yes")) {
					returnValue = true;
				}
			}
			break;
			
		default:
			throw new RuntimeException("Unsupported instrument type " + trancaction.getToolset());
		}
	
		return returnValue;
	}

	@Override
	public String getBuySellFlag() {
		
		String buySellFlag = null;
		switch (trancaction.getToolset()) {
		case Fx:
		case ComSwap:	
			buySellFlag = trancaction.getValueAsString(EnumTransactionFieldId.BuySell);
			break;
			
		default:
			throw new RuntimeException("Unsupported instrument type " + trancaction.getToolset());
		}		
		return buySellFlag;
	}
	



}
