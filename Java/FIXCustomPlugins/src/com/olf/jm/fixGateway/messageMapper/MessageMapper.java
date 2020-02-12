package com.olf.jm.fixGateway.messageMapper;

import com.olf.jm.fixGateway.fieldMapper.FieldMapper;
import com.olf.openjvs.Table;

// TODO: Auto-generated Javadoc
/*
 * History:
 * 2017-10-10 - V0.1 - scurran - Initial Version
 */


/**
 * The Interface MessageMapper. Maps the inbound message into a tradebuilder. Implements the visitor pattern with the 
 * 
 */
public interface MessageMapper {

	/**
	 * Return a table representing the tradebuilder trade fields. Valid after the mapping had been applied.
	 *
	 * @return the tran field table
	 */
	Table getTranFieldTable();
	
	/**
	 * Accept a filed mapper object and apply it's mapping rules to the inbound message. 
	 *
	 * @param mapper to apply to the message.
	 * @throws MessageMapperException 
	 */
	void accept(FieldMapper mapper) throws MessageMapperException;
}
