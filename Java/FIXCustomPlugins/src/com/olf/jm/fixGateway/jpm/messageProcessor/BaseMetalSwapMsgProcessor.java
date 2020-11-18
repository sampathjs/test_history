package com.olf.jm.fixGateway.jpm.messageProcessor;

import java.util.ArrayList;
import java.util.List;

import com.olf.jm.fixGateway.fieldMapper.FieldMapper;
import com.olf.jm.fixGateway.fieldMapper.TradeStatusFieldMapper;
import com.olf.jm.fixGateway.jpm.fieldMapper.BaseMetalCurrency;
import com.olf.jm.fixGateway.jpm.fieldMapper.BaseMetalInternalPortfolio;
import com.olf.jm.fixGateway.jpm.fieldMapper.BaseMetalPreciseNotnl;
import com.olf.jm.fixGateway.jpm.fieldMapper.BaseMetalPriceUnit;
import com.olf.jm.fixGateway.jpm.fieldMapper.BaseMetalRefSource;
import com.olf.jm.fixGateway.jpm.fieldMapper.BaseMetalSpread;
import com.olf.jm.fixGateway.jpm.fieldMapper.BaseMetalStartDate;
import com.olf.jm.fixGateway.jpm.fieldMapper.TradePriceFar;
import com.olf.jm.fixGateway.jpm.fieldMapper.BaseMetalUnit;
import com.olf.jm.fixGateway.jpm.fieldMapper.BuySell;
import com.olf.jm.fixGateway.jpm.fieldMapper.ComSwapToolset;
import com.olf.jm.fixGateway.jpm.fieldMapper.ExternalBunit;
import com.olf.jm.fixGateway.jpm.fieldMapper.InsTypeMetalSwap;
import com.olf.jm.fixGateway.jpm.fieldMapper.InternalBunit;
import com.olf.jm.fixGateway.jpm.fieldMapper.InternalContact;
import com.olf.jm.fixGateway.jpm.fieldMapper.PassThruPortfolio;
import com.olf.jm.fixGateway.jpm.fieldMapper.PassThruUnit;
import com.olf.jm.fixGateway.jpm.fieldMapper.Reference;
import com.olf.jm.fixGateway.jpm.fieldMapper.TradeDate;
import com.olf.jm.fixGateway.jpm.fieldMapper.TradePrice;
import com.olf.jm.fixGateway.jpm.messageAcceptor.BaseMetalSwapMsgAcceptor;
import com.olf.jm.fixGateway.jpm.messageMapper.JmpExecuteMsgMapper;
import com.olf.jm.fixGateway.messageAcceptor.MessageAcceptor;
import com.olf.jm.fixGateway.messageMapper.MessageMapper;
import com.olf.jm.fixGateway.messageMapper.MessageMapperException;
import com.olf.jm.fixGateway.messageProcessor.MessageProcessorBase;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;


/*
 * History:
 * 2020-05-18 - V0.1 - jwaechter - Initial Version created as copy of TradeBookMsgAcceptor
 * 2020-09-04 - V0.2 - jwaechter - Added pre process 
 */


/**
 * Implements message processor to convert JPM Execute
 * fix messages for precious metal FX forward deals into a trade builder object.
 */
public class BaseMetalSwapMsgProcessor extends MessageProcessorBase {

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.messageProcessor.MessageProcessorBase#getFieldMappers()
	 */
	@Override
	public List<FieldMapper> getFieldMappers() {
		List<FieldMapper> mappers = new ArrayList<FieldMapper>();

		mappers.add( new BuySell());
		mappers.add( new Reference());
		mappers.add( new InternalBunit());
		mappers.add( new BaseMetalInternalPortfolio());
		mappers.add( new ExternalBunit());
		mappers.add( new PassThruUnit());
		mappers.add( new PassThruPortfolio());
		mappers.add( new ComSwapToolset());		
		mappers.add( new InsTypeMetalSwap());
		mappers.add( new InternalContact());
		mappers.add( new TradeDate());
		mappers.add( new TradeStatusFieldMapper());
		mappers.add( new TradePrice());
		mappers.add( new BaseMetalCurrency());
		mappers.add( new BaseMetalPreciseNotnl());
		mappers.add( new BaseMetalPriceUnit());
		mappers.add( new BaseMetalRefSource());
		mappers.add( new BaseMetalSpread());
		mappers.add( new BaseMetalStartDate());
		mappers.add( new BaseMetalStartDate());
		mappers.add( new TradePriceFar());
		mappers.add( new BaseMetalUnit());
//		mappers.add( new ExternalContactFieldMapper());
		return mappers;
	}

	@Override
	public List<FieldMapper> getFieldMappersPreProcess() {
		List<FieldMapper> mappers = new ArrayList<FieldMapper>();
        // currently nothing to do in pre process
		return mappers;
	}

	
	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.messageProcessor.MessageProcessorBase#getMessageMapper(com.olf.openjvs.Table)
	 */
	@Override
	public MessageMapper getMessageMapper(Table message) throws MessageMapperException {
		return new JmpExecuteMsgMapper(message);
	}

	@Override
	public Table processMessage(Table message) throws MessageMapperException {
		// skipping call to init logging to avoid creating unecessary log files.
		MessageMapper messgeMapper = getMessageMapper(message);
		
		for(FieldMapper fieldMapper : getFieldMappers()) {
			messgeMapper.accept(fieldMapper);
		}
//		throw new RuntimeException ("This ends here");
		return messgeMapper.getTranFieldTable();
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.messageProcessor.MessageProcessorBase#getMessageAcceptor()
	 */
	@Override
	public MessageAcceptor getMessageAcceptor() {
		return new BaseMetalSwapMsgAcceptor();
	}

	@Override
	public void preProcess(Table message, Transaction tran)
			throws MessageMapperException {
		
	}
}
