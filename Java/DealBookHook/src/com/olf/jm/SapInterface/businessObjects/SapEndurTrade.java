package com.olf.jm.SapInterface.businessObjects;

import com.olf.embedded.application.Context;
import com.olf.openrisk.io.IOFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.logging.PluginLog;

/**
 * The Class SapEndurTrade. Represents a Endur transaction used in the interface.
 */
public class SapEndurTrade implements ISapEndurTrade {
	
	/** The  trade loaded from the endur database. */
	protected Transaction trancaction;
	
	/**
	 * Instantiates a new trade, not loading a trade from the database.
	 */
	public SapEndurTrade() {
		trancaction = null;
	}

	/**
	 * Instantiates a new trade loading a trade from the database based on the 
	 * Endur deal id.
	 *
	 * @param context the OC context
	 * @param endurDealId the Endur deal id
	 */
	public SapEndurTrade(final Context context, final int endurDealId) {
		TradingFactory tf = context.getTradingFactory();
		
		trancaction = tf.retrieveTransactionByDeal(endurDealId);
		
		// TODO validate the trade returned is a coverage trade.
	}
	
	/**
	 * Instantiates a new  trade loading a trade from the database based on
	 * the the info field sapOrderId.
	 *
	 * @param context the context
	 * @param sapOrderId the sap order id
	 */
	public SapEndurTrade(final Context context, final String sapOrderId) {
		
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
	
	@Override
	public final EnumToolset  getToolset() {
		usageGuard("Toolset");
		EnumToolset toolset = null;
		switch (trancaction.getToolset()) {
		case Fx:
		case ComSwap:			
			toolset = trancaction.getToolset();
			break;
			
		default:
			throw new RuntimeException("Unsupported instrument type " + trancaction.getToolset());
		}		
		return toolset;
	}	

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.ICoverageTrade#getDealTrackingNumber()
	 */
	@Override
	public final int getDealTrackingNumber() {
		usageGuard("DealTrackingNumber");
		int dealTrackingNumber = -1;
		switch (trancaction.getToolset()) {
		case Fx:
		case ComSwap:			
			dealTrackingNumber = trancaction.getDealTrackingId();
			break;
			
		default:
			throw new RuntimeException("Unsupported instrument type " + trancaction.getToolset());
		}		
		return dealTrackingNumber;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.ICoverageTrade#getTradeStatus()
	 */
	@Override
	public final EnumTranStatus getTradeStatus() {
		usageGuard("TradeStatus");
		EnumTranStatus status = null;
		
		switch (trancaction.getToolset()) {
		case Fx:
		case ComSwap:	
			status = trancaction.getTransactionStatus();
			break;
			
		default:
			throw new RuntimeException("Unsupported instrument type " + trancaction.getToolset());
		}		
		return status;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.ICoverageTrade#isValid()
	 */
	@Override
	public final boolean isValid() {
		return trancaction == null ? false : true;
	}
	
	/**
	 * Usage guard. Check that the class contains a valid transaction. Throws a runtime exception
	 * on error. 
	 *
	 * @param field the name of the field being checked, used to generate the error message. 
	 */
	protected final void usageGuard(final String field) {
		if (this.trancaction == null) {
			String errorMessage = "Error transaction object is not initilised. Error reading value for field " + field + ". ";
			
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
	}	

}
