package com.olf.jm.fixGateway.jpm.messageAcceptor;

import com.olf.jm.fixGateway.fieldMapper.FieldMapperException;
import com.olf.jm.fixGateway.fieldMapper.fields.EnumExecutionReport;
import com.olf.jm.fixGateway.fieldMapper.fields.EnumInstrumentTags;
import com.olf.jm.fixGateway.jpm.fieldMapper.Reference;
import com.olf.jm.fixGateway.fieldUtils.FixMessageHelper;
import com.olf.jm.fixGateway.messageAcceptor.MessageAcceptor;
import com.olf.jm.fixGateway.messageAcceptor.MessageAcceptorException;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2020-05-XX 	V1.0	jwaechter 	- Initial Version
 * 2020-08-13 	V1.1	jwaechter 	- adpated checkDuplicates to new reference field
 */

public abstract class JpmExecuteMessageAcceptor implements MessageAcceptor {

	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "Connex";

	/** The Constant SUBCONTEXT used to identify entries in the const repository.. */
	public static final String SUBCONTEXT = "FIXGateway";

	public JpmExecuteMessageAcceptor() {
		super();
	}

	public abstract String getExpectedSecuritySubtype();

	@Override
	public boolean acceptMessage(Table message) throws MessageAcceptorException {
		try {
			init ();
			try {
				if(message == null || message.getNumRows() != 1) {
					String errorMessage = "Invalid message table, table is null or wrong number of rows.";
					Logging.error(errorMessage);
					throw new MessageAcceptorException(errorMessage);				
				}
			} catch (OException e1) {
				String errorMessage = "Error validating the mesage table. " + e1.getMessage();
				Logging.error(errorMessage);
				throw new MessageAcceptorException(errorMessage);	
			}		

			if(!checkOrderStatus(message)) {
				return false;
			}

			if(!checkSecurityType(message)) {
				return false;
			}	

			if(!checkSecuritySubType(message)) {
				return false;
			}

			checkForDuplicates(message);
			return true;
		} finally {
			Logging.close();
		}
	}

	/**
	 * Check the order status. reject cancellation and rejection messages
	 *
	 * @param message the message to validate
	 * @return true, if message should be processed false otherwise
	 * @throws MessageAcceptorException 
	 */
	private boolean checkOrderStatus(Table message)
			throws MessageAcceptorException {
		try {
			String tagValue = message.getString(EnumExecutionReport.ORD_STATUS.getTagName(), 1);

			if(tagValue == null || tagValue.length() == 0) {
				String errorMessage = "Error reading field " + EnumExecutionReport.ORD_STATUS.getTagName() + ". Value is empty or missing.";
				Logging.error(errorMessage);
				throw new MessageAcceptorException(errorMessage);				
			}

			if(!tagValue.equalsIgnoreCase("FILLED") && !tagValue.equalsIgnoreCase("2")) {
				Logging.info("Skipping message, order status is '" + tagValue + "', but 'FILLED' (2) was expected");
				return false;
			}

		} catch (OException e) {
			String errorMessage = "Error reading field " + EnumExecutionReport.ORD_STATUS.getTagName() + ". " + e.getMessage();
			Logging.error(errorMessage);
			throw new MessageAcceptorException(errorMessage);
		}
		Logging.info("Incoming message matches expected order status 'FILLED'");
		return true;
	}

	/**
	 * Check if the security type is as expected
	 *
	 * @param message the message to validate
	 * @return true, if ticker is supported, false otherwise
	 * @throws MessageAcceptorException 
	 */
	private boolean checkSecurityType(Table message)
			throws MessageAcceptorException {

		String tagValue = null; 

		try {
			tagValue = FixMessageHelper.getInstrumentField(EnumInstrumentTags.SECURITY_TYPE, message);
		} catch (FieldMapperException e) {
			String errorMessage = "Error reading field " + EnumInstrumentTags.SECURITY_TYPE.getTagName() + ". " + e.getMessage();
			Logging.error(errorMessage);
			throw new MessageAcceptorException(errorMessage);
		}

		if(tagValue == null || tagValue.length() == 0) {
			String errorMessage = "Error reading field " + EnumInstrumentTags.SECURITY_TYPE.getTagName() + ". Value is empty or missing.";
			Logging.error(errorMessage);
			throw new MessageAcceptorException(errorMessage);				
		}	

		if(!tagValue.equalsIgnoreCase("FOREIGN_EXCHANGE_CONTRACT")) {
			Logging.info("Skipping message, field " + EnumInstrumentTags.SECURITY_TYPE.getTagName() + " is '" + tagValue + "', but 'FOR' (FX) was expected");
			return false;
		}
		Logging.info("Incoming message matches expected security type 'FOREIGN_EXCHANGE_CONTRACT'");			
		return true;
	}

	private boolean checkSecuritySubType(Table message)
			throws MessageAcceptorException {
		String tagValue = null;
		try {
			tagValue = FixMessageHelper.getInstrumentField(EnumInstrumentTags.SECURITY_SUB_TYPE, message);
		} catch (FieldMapperException e) {
			String errorMessage = "Error reading field " + EnumInstrumentTags.SECURITY_SUB_TYPE.getTagName() + ". " + e.getMessage();
			Logging.error(errorMessage);
			throw new MessageAcceptorException(errorMessage);
		}

		if(tagValue == null || tagValue.length() == 0) {
			String errorMessage = "Error reading field " + EnumInstrumentTags.SECURITY_SUB_TYPE.getTagName() + ". Value is empty or missing.";
			Logging.error(errorMessage);
			throw new MessageAcceptorException(errorMessage);				
		}

		if(!tagValue.equalsIgnoreCase(getExpectedSecuritySubtype())) {
			Logging.info("Skipping message, field " + EnumInstrumentTags.SECURITY_SUB_TYPE.getTagName() + " is '" + tagValue + "', but '" + getExpectedSecuritySubtype() + "' was expected");
			return false;
		}
		Logging.info("Incoming message matches expected security subtype '" + getExpectedSecuritySubtype() + "'");
		return true;
	}

	/**
	 * Check for duplicates messages. Check is the execution id already exists in the datebase. 
	 *
	 * @param message the message to validate
	 * @throws MessageAcceptorException if the message already exists in the db.
	 */
	private void checkForDuplicates(Table message) throws MessageAcceptorException {
		String reference;
		Table existingTrade = null;
		try {
			Reference rfm = new Reference ();
			reference = rfm.getTranFieldValue(message);
			if(reference == null || reference.length() == 0) {
				String errorMessage = "Error reading reference. Value is empty or missing.";
				Logging.error(errorMessage);
				throw new MessageAcceptorException(errorMessage);				
			}

			existingTrade = loadTradeDetails(reference);

			if(existingTrade == null) {
				String errorMessage = "Error checking for existing trade, error executing the SQL.";
				Logging.error(errorMessage);
				throw new MessageAcceptorException(errorMessage);				
			}

			if(existingTrade.getNumRows() != 0) {
				String errorMessage = "A trade already exists for reference " + reference + " deal number " + existingTrade.getInt(1, 1);
				Logging.error(errorMessage);
				throw new MessageAcceptorException(errorMessage);					
			}
		} catch (OException e) {
			String errorMessage = "Error checking for duplicate trades. " + e.getMessage();
			Logging.error(errorMessage);
			throw new MessageAcceptorException(errorMessage);
		} catch (FieldMapperException e) {
			Logging.error ("Error accessing Reference: " + e.toString());
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
				" FROM   ab_tran ab " + 
				" WHERE  " + 
				"        reference = '" + executionId + "' " +
				"        AND tran_status = 3" +
				"        AND current_flag = 1";

		Table tmp = Table.tableNew();
		DBaseTable.execISql(tmp , sql); 

		return tmp;
	}

	/**
	 * Initialise the class loggers.
	 * @throws OException 
	 *
	 * @throws Exception the exception
	 */
	private void init() {
		try {
			Logging.init(JpmExecuteMessageAcceptor.class, CONTEXT, SUBCONTEXT);
		} catch (Exception e) {
			throw new RuntimeException("Error initialising logging. " + e.getMessage());
		}

	}
}