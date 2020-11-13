package com.olf.jm.fixGateway.messageProcessor;

import java.util.List;

import com.olf.jm.fixGateway.fieldMapper.FieldMapper;
import com.olf.jm.fixGateway.fieldMapper.FieldMapperException;
import com.olf.jm.fixGateway.messageAcceptor.MessageAcceptor;
import com.olf.jm.fixGateway.messageAcceptor.MessageAcceptorException;
import com.olf.jm.fixGateway.messageMapper.MessageMapper;
import com.olf.jm.fixGateway.messageMapper.MessageMapperException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.jm.logging.Logging;


/*
 * History:
 * 2017-10-10 - V0.1 - scurran 		- Initial Version
 * 2020-05-11 - V0.2 - jwaechter	- Made abstract methods public to support cross package
 *                                    class hierarchies.
 */


/**
 * The Class MessageProcessorBase. base implementation for a message processor. 
 */
public abstract class MessageProcessorBase implements MessageProcessor {
		
	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "Connex";
	
	/** The Constant SUBCONTEXT used to identify entries in the const repository.. */
	public static final String SUBCONTEXT = "FIXGateway";

	/**
	 * Gets the field mappers used to convert the message.
	 *
	 * @return the field mappers
	 */
	abstract public List<FieldMapper> getFieldMappers();

	/**
	 * Gets the additional field mappings that should be applied after transaction
	 * creation but before validation in the FIX Framework preprocessing. 
	 * 
	 * @return the field mappers
	 */
	abstract public List<FieldMapper> getFieldMappersPreProcess();

	
	/**
	 * Gets the message mapper class used to convert the inbound message into a trade builder 
	 * message.
	 *
	 * @param message the message to be converted
	 * @return the message mapper object.
	 * @throws MessageMapperException 
	 */
	abstract public MessageMapper getMessageMapper(Table message) throws MessageMapperException;
	
	/**
	 * Gets the message acceptor. Get the acceptor object used to check if the message should be 
	 * processed or skipped.
	 *
	 * @return the message acceptor
	 * @throws MessageAcceptorException the message acceptor exception
	 */
	abstract public MessageAcceptor getMessageAcceptor() throws MessageAcceptorException;
	
	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.messageProcessor.MessageProcessor#processMessage(com.olf.openjvs.Table)
	 */
	public Table processMessage(Table message) throws MessageMapperException {
		try {
			MessageMapper messgeMapper = getMessageMapper(message);
			
			for(FieldMapper fieldMapper : getFieldMappers()) {
				messgeMapper.accept(fieldMapper);
			}
			return messgeMapper.getTranFieldTable();
		} finally {
			Logging.close();
		}
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.messageProcessor.MessageProcessor#acceptMessage(com.olf.openjvs.Table)
	 */
	@Override
	public boolean acceptMessage(Table message) throws MessageAcceptorException {
		MessageAcceptor messageAcceptor = getMessageAcceptor();

		return messageAcceptor.acceptMessage(message);
	}
	
	
	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.messageProcessor.MessageProcessor#acceptMessage(com.olf.openjvs.Table, com.olf.openjvs.Transaction)
	 */
	@Override
	public void preProcess (Table message, Transaction tran) throws MessageMapperException {
		for(FieldMapper fieldMapper : getFieldMappersPreProcess()) {
			if(fieldMapper == null) {
				String errorMessage = "Error applying field mapping. Invalid mapping object";
				Logging.error(errorMessage);
				throw new MessageMapperException(errorMessage);			
			}

			TRANF_FIELD fieldName = fieldMapper.getTranFieldName();
			int side = fieldMapper.getSide();
			String value = null;
			try {
				value = fieldMapper.getTranFieldValue(message);
			} catch (FieldMapperException e1) {
				String errorMessage = "Error applying mapping " + fieldMapper + ". " + e1.getMessage();
				Logging.error(errorMessage);
				throw new MessageMapperException(errorMessage);
			}

			try {
				if(!fieldMapper.isInfoField()) {
					Logging.info("Adding field mapping field [" + fieldName + "] side [" + side + "] value [" + value + "]");
				    tran.setField(fieldName.toInt(), side, null, value, fieldMapper.getSeqNum2(), 
				    		fieldMapper.getSeqNum3(), fieldMapper.getSeqNum4(), fieldMapper.getSeqNum5());
				}
				else {
					Logging.info("Adding field mapping info field [" + fieldMapper.infoFieldName() + "] side [" + side + "] value [" + value + "]");
				    tran.setField(fieldName.toInt(), side, fieldMapper.infoFieldName(), value, fieldMapper.getSeqNum2(),
				    		fieldMapper.getSeqNum3(), fieldMapper.getSeqNum4(), fieldMapper.getSeqNum5());
				}
			} catch (Exception e) {
				String errorMessage = "Error adding tranf field [" + fieldName + "] side [" + side + "] value [" + value + "] to message. " + e.getMessage();
				Logging.error(errorMessage);
				throw new MessageMapperException(errorMessage);
			}	
		}
	}	
}
