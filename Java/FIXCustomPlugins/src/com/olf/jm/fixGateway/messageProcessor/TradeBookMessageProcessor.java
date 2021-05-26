package com.olf.jm.fixGateway.messageProcessor;

import java.util.ArrayList;
import java.util.List;

import com.olf.jm.fixGateway.fieldMapper.BuySellFieldMapper;
import com.olf.jm.fixGateway.fieldMapper.ExecutionIdFieldMapper;
import com.olf.jm.fixGateway.fieldMapper.ExternalBunitFieldMapper;
import com.olf.jm.fixGateway.fieldMapper.ExternalContactFieldMapper;
import com.olf.jm.fixGateway.fieldMapper.FieldMapper;
import com.olf.jm.fixGateway.fieldMapper.InsTypeFieldMapper;
import com.olf.jm.fixGateway.fieldMapper.InternalBunitFieldMapper;
import com.olf.jm.fixGateway.fieldMapper.InternalContactFieldMapper;
import com.olf.jm.fixGateway.fieldMapper.InternalPortfolioFieldMapper;
import com.olf.jm.fixGateway.fieldMapper.OrderIdFieldMapper;
import com.olf.jm.fixGateway.fieldMapper.PositionFieldMapper;
import com.olf.jm.fixGateway.fieldMapper.PriceFieldMapper;
import com.olf.jm.fixGateway.fieldMapper.ProjectionIndexFieldMapper;
import com.olf.jm.fixGateway.fieldMapper.ReferenceFieldMapper;
import com.olf.jm.fixGateway.fieldMapper.TickerFieldMapper;
import com.olf.jm.fixGateway.fieldMapper.ToolsetFieldMapper;
import com.olf.jm.fixGateway.fieldMapper.TradeDateFieldMapper;
import com.olf.jm.fixGateway.fieldMapper.TradeStatusFieldMapper;
import com.olf.jm.fixGateway.messageAcceptor.MessageAcceptor;
import com.olf.jm.fixGateway.messageAcceptor.TradeBookMsgAcceptor;
import com.olf.jm.fixGateway.messageMapper.TradeBookMessageMapper;
import com.olf.jm.fixGateway.messageMapper.MessageMapper;
import com.olf.jm.fixGateway.messageMapper.MessageMapperException;
import com.olf.openjvs.Table;


/*
 * History:
 * 2017-10-10 - V0.1 - scurran    - Initial Version
 * 2020-05-11 - V0.2 - jwaechter  - changed access modifiers of methods to public
 * 2020-09-04 - V0.3 - jwaechter  - Added preProcess method
 */


/**
 * The Class TradeBookMessageProcessor. Implments message processor to convert Bloomberg TradeBook
 * fix messages into a trade builder object.
 */
public class TradeBookMessageProcessor extends MessageProcessorBase {

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.messageProcessor.MessageProcessorBase#getFieldMappers()
	 */
	@Override
	public List<FieldMapper> getFieldMappers() {
		List<FieldMapper> mappers = new ArrayList<FieldMapper>();

		mappers.add( new BuySellFieldMapper());
		mappers.add( new ExecutionIdFieldMapper());
		mappers.add( new ExternalBunitFieldMapper());
		mappers.add( new InsTypeFieldMapper());
		mappers.add( new InternalBunitFieldMapper());
		mappers.add( new InternalContactFieldMapper());
		mappers.add( new InternalPortfolioFieldMapper());
		mappers.add( new OrderIdFieldMapper());
		mappers.add( new PositionFieldMapper());
		mappers.add( new PriceFieldMapper());
		mappers.add( new ProjectionIndexFieldMapper());
		mappers.add( new ReferenceFieldMapper());
		mappers.add( new TickerFieldMapper());
		mappers.add( new ToolsetFieldMapper());
		mappers.add( new TradeDateFieldMapper());
		mappers.add( new TradeStatusFieldMapper());
		mappers.add( new ExternalContactFieldMapper());
		return mappers;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.messageProcessor.MessageProcessorBase#getMessageMapper(com.olf.openjvs.Table)
	 */
	@Override
	public MessageMapper getMessageMapper(Table message) throws MessageMapperException {
		return new TradeBookMessageMapper(message);
	}



	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.messageProcessor.MessageProcessorBase#getMessageAcceptor()
	 */
	@Override
	public MessageAcceptor getMessageAcceptor() {
		return new TradeBookMsgAcceptor();
	}

	@Override
	public List<FieldMapper> getFieldMappersPreProcess() {
		List<FieldMapper> mappers = new ArrayList<FieldMapper>();
		return mappers;
	}

}
