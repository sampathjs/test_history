package com.olf.jm.fixGateway.messageProcessor;

import java.util.List;

import com.olf.jm.fixGateway.fieldMapper.FieldMapper;
import com.olf.jm.fixGateway.messageAcceptor.MessageAcceptor;
import com.olf.jm.fixGateway.messageAcceptor.MessageAcceptorException;
import com.olf.jm.fixGateway.messageMapper.MessageMapper;
import com.olf.jm.fixGateway.messageMapper.MessageMapperException;
import com.olf.openjvs.Table;


/*
 * History:
 * 2017-10-10 - V0.1 - scurran - Initial Version
 */


/**
 * The Class MessageProcessorBase. base implementation for a message processor. 
 */
public abstract class MessageProcessorBase implements MessageProcessor {

	/**
	 * Gets the field mappers used to convert the message.
	 *
	 * @return the field mappers
	 */
	abstract List<FieldMapper> getFieldMappers();
	
	/**
	 * Gets the message mapper class used to convert the inbound message into a trade builder 
	 * message.
	 *
	 * @param message the message to be converted
	 * @return the message mapper object.
	 * @throws MessageMapperException 
	 */
	abstract MessageMapper getMessageMapper(Table message) throws MessageMapperException;
	
	/**
	 * Gets the message acceptor. Get the acceptor object used to check if the message should be 
	 * processed or skipped.
	 *
	 * @return the message acceptor
	 * @throws MessageAcceptorException the message acceptor exception
	 */
	abstract MessageAcceptor getMessageAcceptor() throws MessageAcceptorException;
	
	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.messageProcessor.MessageProcessor#processMessage(com.olf.openjvs.Table)
	 */
	public Table processMessage(Table message) throws MessageMapperException {
		MessageMapper messgeMapper = getMessageMapper(message);
		
		for(FieldMapper fieldMapper : getFieldMappers()) {
			messgeMapper.accept(fieldMapper);
		}
		
		return messgeMapper.getTranFieldTable();
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.messageProcessor.MessageProcessor#acceptMessage(com.olf.openjvs.Table)
	 */
	@Override
	public boolean acceptMessage(Table message) throws MessageAcceptorException {
		MessageAcceptor messageAcceptor = getMessageAcceptor();

		return messageAcceptor.acceptMessage(message);
	}
	
}
