package com.olf.jm.SapInterface.messageMapper;

import com.olf.embedded.connex.Request;
import com.olf.openrisk.table.Table;


/**
 * The Interface IMessageMapper.
 */
public interface IMessageMapper {

	/**
	 * Map request.
	 *
	 * @param destination the destination
	 * @param source the source
	 * @throws MessageMapperException the message mapper exception
	 */
	void mapRequest(Request destination, Table source) throws MessageMapperException;
	
	/**
	 * Map response.
	 *
	 * @param destination the destination
	 * @param source the source
	 * @throws MessageMapperException the message mapper exception
	 */
	void mapResponse(Request destination, Table source) throws MessageMapperException;
}
