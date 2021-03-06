package com.olf.jm.fixGateway.messageMapper;

import com.olf.cxplugins.adapter.fixgateway.FIXSTDHelperInclude;
import com.olf.jm.fixGateway.fieldMapper.FieldMapper;
import com.olf.jm.fixGateway.fieldMapper.FieldMapperException;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2017-10-10 - V0.1 - scurran - Initial Version
 */


/**
 * The Class TradeBookMessageMapper. Message mapper to convert a TradeBook fix message into a
 * TradeBuilder table. 
 */
public class TradeBookMessageMapper implements MessageMapper, AutoCloseable {
	
	/** The fix message. */
	private Table fixMessage;
	
	/** The tran field table. */
	private Table tranFieldTable;
	
	/** The fix std helper. */
	private FIXSTDHelperInclude fixStdHelper;
	
	/**
	 * Instantiates a new trade book message mapper.
	 *
	 * @param fixMessage the fix message to be mapped
	 * @throws MessageMapperException 
	 */
	public TradeBookMessageMapper(Table fixMessage) throws MessageMapperException{
		fixStdHelper = new FIXSTDHelperInclude();
		
		this.fixMessage = fixMessage;
		validateFixMessage();
		
		try {
			tranFieldTable = fixStdHelper.HelperInc_Create_TradeField_Table();
		} catch (OException e) {
			String errorMessage = "Error initialising message mapper. " + e.getMessage();
			Logging.error(errorMessage);
			throw new MessageMapperException(errorMessage);
		}
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.messageMapper.MessageMapper#getTranFieldTable()
	 */
	@Override
	public Table getTranFieldTable() {
		return tranFieldTable;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.messageMapper.MessageMapper#accept(com.olf.jm.fixGateway.fieldMapper.FieldMapper)
	 */
	@Override
	public void accept(FieldMapper mapper) throws MessageMapperException {
		
		if(mapper == null) {
			String errorMessage = "Error applying field mapping. Invalid mapping object";
			Logging.error(errorMessage);
			throw new MessageMapperException(errorMessage);			
		}

		TRANF_FIELD fieldName = mapper.getTranFieldName();
		int side = mapper.getSide();
		String value = null;
		try {
			value = mapper.getTranFieldValue(fixMessage);
		} catch (FieldMapperException e1) {
			String errorMessage = "Error applying mapping " + mapper + ". " + e1.getMessage();
			Logging.error(errorMessage);
			throw new MessageMapperException(errorMessage);
		}

		try {
			if(!mapper.isInfoField()) {
				Logging.info("Adding field mapping field [" + fieldName + "] side [" + side + "] value [" + value + "]");
				fixStdHelper.HelperInc_Add_TradeField(tranFieldTable, fieldName.toInt(), new Integer(side).toString(), "", "", value);
			}
			else {
				Logging.info("Adding field mapping info field [" + mapper.infoFieldName() + "] side [" + side + "] value [" + value + "]");
				fixStdHelper.HelperInc_Add_TradeField(tranFieldTable, fieldName.toInt(), mapper.infoFieldName(), new Integer(side).toString(), "", "", value);
			}
		} catch (Exception e) {
			String errorMessage = "Error adding tranf field [" + fieldName + "] side [" + side + "] value [" + value + "] to message. " + e.getMessage();
			Logging.error(errorMessage);
			throw new MessageMapperException(errorMessage);
		}		 

	}
	
	/**
	 * Validate fix message. Check that the inbound fix message and contains details to be mapped.
	 *
	 * @throws MessageMapperException 
	 */
	private void validateFixMessage() throws MessageMapperException {
		try {
			if(fixMessage == null || fixMessage.getNumRows()  == 0) {
				String errorMessage = "Error validating fix message. Table is null or empty.";
				Logging.error(errorMessage);
				throw new MessageMapperException(errorMessage);			
			}
		} catch (OException e) {
			String errorMessage = "Error validating fix message. " + e.getMessage();
			Logging.error(errorMessage);
			throw new MessageMapperException(errorMessage);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.AutoCloseable#close()
	 */
	@Override
	public void close() throws Exception {
		if(fixMessage != null) {
			fixMessage.destroy();
		}
	
		if(tranFieldTable != null) {
			tranFieldTable.destroy();
		}
	}

}
