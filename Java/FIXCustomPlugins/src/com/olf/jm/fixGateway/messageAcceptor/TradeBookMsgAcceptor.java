package com.olf.jm.fixGateway.messageAcceptor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.olf.jm.fixGateway.fieldMapper.FieldMapperException;
import com.olf.jm.fixGateway.fieldMapper.fields.EnumExecutionReport;
import com.olf.jm.fixGateway.fieldMapper.fields.EnumInstrumentTags;
import com.olf.jm.fixGateway.fieldUtils.FixMessageHelper;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2017-10-10 - V0.1 - scurran - Initial Version
 */


/**
 * The Class TradeBookMsgAcceptor. Test is the Tradebook message is valid and should be processed.
 * 
 * Skip order cancelation and rejection messages
 * Skip spread orders
 */
public class TradeBookMsgAcceptor implements MessageAcceptor {

	/** The Constant PATTERN defines valid tickers. */
	private final static String PATTERN = "^[A-Z]{3}[0-9]{1}$";
	
	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.messageAcceptor.MessageAcceptor#acceptMessage(com.olf.openjvs.Table)
	 */
	@Override
	public boolean acceptMessage(Table message) throws MessageAcceptorException {

		try {
			if(message == null || message.getNumRows() != 1) {
				String errorMessage = "Invalid message table, table is null or wrong number of rows.";
				PluginLog.error(errorMessage);
				throw new MessageAcceptorException(errorMessage);				
			}
		} catch (OException e1) {
			String errorMessage = "Error validating the mesage table. " + e1.getMessage();
			PluginLog.error(errorMessage);
			throw new MessageAcceptorException(errorMessage);	
		}
		
		if(!checkOrderStatus(message)) {
			return false;
		}

		if(!checkInstrument(message)) {
			return false;
		}		

		checkForDuplicates(message);
		
		return true;
	}
	
	/**
	 * Check the order status. reject cancellation and rejection messages
	 *
	 * @param message the message to validate
	 * @return true, if message should be processed false otherwise
	 * @throws MessageAcceptorException 
	 */
	private boolean checkOrderStatus(Table message) throws MessageAcceptorException {
		try {
			String tagValue = message.getString(EnumExecutionReport.ORD_STATUS.getTagName(), 1);
			
			if(tagValue == null || tagValue.length() == 0) {
				String errorMessage = "Error reading field " + EnumExecutionReport.ORD_STATUS.getTagName() + ". Value is empty or missing.";
				PluginLog.error(errorMessage);
				throw new MessageAcceptorException(errorMessage);				
			}
			
			if(tagValue.equalsIgnoreCase("Canceled") || tagValue.equalsIgnoreCase("Rejected")) {
				PluginLog.info("Skipping message, order status is " + tagValue);
				return false;
			}
			
		} catch (OException e) {
			String errorMessage = "Error reading field " + EnumExecutionReport.ORD_STATUS.getTagName() + ". " + e.getMessage();
			PluginLog.error(errorMessage);
			throw new MessageAcceptorException(errorMessage);
		}		
		return true;
	}
	
	/**
	 * Check instrument is a supported ticker.
	 *
	 * @param message the message to validate
	 * @return true, if ticker is supported, false otherwise
	 * @throws MessageAcceptorException 
	 */
	private boolean checkInstrument(Table message) throws MessageAcceptorException {
		
		String ticker = null; 
				
		try {
			ticker = FixMessageHelper.getInstrumentField(EnumInstrumentTags.SYMBOL, message);
		} catch (FieldMapperException e) {
			String errorMessage = "Error reading field " + EnumInstrumentTags.SYMBOL.getTagName() + ". " + e.getMessage();
			PluginLog.error(errorMessage);
			throw new MessageAcceptorException(errorMessage);
		}

		if(ticker == null || ticker.length() == 0) {
			String errorMessage = "Error reading field " + EnumInstrumentTags.SYMBOL.getTagName() + ". Value is empty or missing.";
			PluginLog.error(errorMessage);
			throw new MessageAcceptorException(errorMessage);				
		}	
		
		Pattern pattern = Pattern.compile(PATTERN);
		
		Matcher matcher = pattern.matcher(ticker);
		
		if(!matcher.matches()) {
			PluginLog.info("Skipping message, ticker not correct format " + ticker);
			return false;
		}
		
	
		return true;
	}
	
	/**
	 * Check for duplicates messages. Check is the execution id already exists in the datebase. 
	 *
	 * @param message the message to validate
	 * @throws MessageAcceptorException if the message already exists in the db.
	 */
	private void checkForDuplicates(Table message) throws MessageAcceptorException {
		String executionId;
		Table existingTrade = null;
		try {
			executionId = message.getString(EnumExecutionReport.EXEC_ID.getTagName(), 1);
			if(executionId == null || executionId.length() == 0) {
				String errorMessage = "Error reading field " + EnumExecutionReport.EXEC_ID.getTagName() + ". Value is empty or missing.";
				PluginLog.error(errorMessage);
				throw new MessageAcceptorException(errorMessage);				
			}
			
			existingTrade = loadTradeDetails(executionId);
			
			if(existingTrade == null) {
				String errorMessage = "Error checking for existing trade, error executing the SQL.";
				PluginLog.error(errorMessage);
				throw new MessageAcceptorException(errorMessage);				
			}
			
			if(existingTrade.getNumRows() != 0) {
				String errorMessage = "A trade already exists for execution id " + executionId + " deal number " + existingTrade.getInt(1, 1);
				PluginLog.error(errorMessage);
				throw new MessageAcceptorException(errorMessage);					
			}
			
			
		} catch (OException e) {
			String errorMessage = "Error checking for duplicate trades. " + e.getMessage();
			PluginLog.error(errorMessage);
			throw new MessageAcceptorException(errorMessage);
		} finally {
			if(existingTrade != null) {
				try {
					existingTrade.destroy();
				} catch (OException e) {
				}
				existingTrade = null;
			}
		}
		

	}
	
	
	/**
	 * Check is an execution id already exists in the database. 
	 *
	 * @param executionId the execution id to lookup
	 * @return the deal number associated with the execution id. 
	 * @throws OException 
	 */
	private Table loadTradeDetails(String executionId) throws OException {
		String sql = " SELECT deal_tracking_num " + 
					 " FROM   ab_tran_info_view abi " + 
					 "        JOIN ab_tran ab " + 
					 "          ON abi.tran_num = ab.tran_num " + 
					 "             AND tran_status = 3 " + 
					 "             AND current_flag = 1 " + 
					 " WHERE  type_name = 'TradeBook Execution Id' " + 
					 "        AND value = '" + executionId + "' ";
		
		Table tmp = Table.tableNew();
		DBaseTable.execISql(tmp , sql); 

		return tmp;
		
	}
	

}
