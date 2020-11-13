package com.olf.jm.fixGateway.jpm.messageProcessor;

import java.util.ArrayList;
import java.util.List;

import com.olf.jm.fixGateway.fieldMapper.FieldMapper;
import com.olf.jm.fixGateway.fieldMapper.TradeStatusFieldMapper;
import com.olf.jm.fixGateway.jpm.fieldMapper.BaseCurrency;
import com.olf.jm.fixGateway.jpm.fieldMapper.BoughtCurrency;
import com.olf.jm.fixGateway.jpm.fieldMapper.FxDealtRate;
import com.olf.jm.fixGateway.jpm.fieldMapper.FxFarBaseSettleDate;
import com.olf.jm.fixGateway.jpm.fieldMapper.FxFarDealtRate;
import com.olf.jm.fixGateway.jpm.fieldMapper.TradePriceFar;
import com.olf.jm.fixGateway.jpm.fieldMapper.BuySell;
import com.olf.jm.fixGateway.jpm.fieldMapper.ExternalBunit;
import com.olf.jm.fixGateway.jpm.fieldMapper.Form;
import com.olf.jm.fixGateway.jpm.fieldMapper.FxBaseCurrency;
import com.olf.jm.fixGateway.jpm.fieldMapper.FxCounterAmount;
import com.olf.jm.fixGateway.jpm.fieldMapper.FxDealtAmount;
import com.olf.jm.fixGateway.jpm.fieldMapper.FxFarDealtAmount;
import com.olf.jm.fixGateway.jpm.fieldMapper.FxFarTermSettleDate;
import com.olf.jm.fixGateway.jpm.fieldMapper.FxFarSettleDate;
import com.olf.jm.fixGateway.jpm.fieldMapper.FxTermSettleDate;
import com.olf.jm.fixGateway.jpm.fieldMapper.FxSettleDate;
import com.olf.jm.fixGateway.jpm.fieldMapper.FxSpotRate;
import com.olf.jm.fixGateway.jpm.fieldMapper.FxToolset;
import com.olf.jm.fixGateway.jpm.fieldMapper.InsTypeFx;
import com.olf.jm.fixGateway.jpm.fieldMapper.InternalBunit;
import com.olf.jm.fixGateway.jpm.fieldMapper.InternalContact;
import com.olf.jm.fixGateway.jpm.fieldMapper.InternalPortfolio;
import com.olf.jm.fixGateway.jpm.fieldMapper.Loco;
import com.olf.jm.fixGateway.jpm.fieldMapper.PrecMetalSwapCashflowType;
import com.olf.jm.fixGateway.jpm.fieldMapper.Reference;
import com.olf.jm.fixGateway.jpm.fieldMapper.SettleDate;
import com.olf.jm.fixGateway.jpm.fieldMapper.Ticker;
import com.olf.jm.fixGateway.jpm.fieldMapper.TradeDate;
import com.olf.jm.fixGateway.jpm.fieldMapper.TradePrice;
import com.olf.jm.fixGateway.jpm.messageAcceptor.PrecMetalSwapMsgAcceptor;
import com.olf.jm.fixGateway.jpm.messageMapper.JmpExecuteMsgMapper;
import com.olf.jm.fixGateway.messageAcceptor.MessageAcceptor;
import com.olf.jm.fixGateway.messageMapper.MessageMapper;
import com.olf.jm.fixGateway.messageMapper.MessageMapperException;
import com.olf.jm.fixGateway.messageProcessor.MessageProcessorBase;
import com.olf.openjvs.Table;


/*
 * History:
 * 2020-05-14 - V0.1 - jwaechter - Initial Version created as copy of TradeBookMsgAcceptor
 * 2020-08-27 - V0.2 - jwaechter - Changed iteration through list of mappers.
 */


/**
 * Implements message processor to convert JPM Execute
 * fix messages for precious metal FX forward deals into a trade builder object.
 */
public class PrecMetalSwapMsgProcessor extends MessageProcessorBase {

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.messageProcessor.MessageProcessorBase#getFieldMappers()
	 */
	@Override
	public List<FieldMapper> getFieldMappers() {
		List<FieldMapper> mappers = new ArrayList<FieldMapper>();

		mappers.add( new BuySell());
		mappers.add( new Reference());
		mappers.add( new ExternalBunit());
		mappers.add( new InsTypeFx());
		mappers.add( new PrecMetalSwapCashflowType());
		mappers.add( new Ticker());
		mappers.add( new BaseCurrency());
		mappers.add( new BoughtCurrency());
		mappers.add( new InternalBunit());
		mappers.add( new InternalPortfolio());
		mappers.add( new InternalContact());
		mappers.add( new FxBaseCurrency());
		mappers.add( new FxDealtAmount());
		mappers.add( new FxDealtRate());
		mappers.add( new FxCounterAmount());
		mappers.add( new FxToolset());
		mappers.add( new FxSettleDate());
		mappers.add( new FxSpotRate());
		mappers.add( new FxTermSettleDate());
		mappers.add( new SettleDate());
		mappers.add( new TradeDate());
		mappers.add( new TradeStatusFieldMapper());
		mappers.add( new TradePrice());
		mappers.add( new Loco());
		mappers.add( new Form());		
		mappers.add( new FxFarSettleDate());
		mappers.add( new FxFarTermSettleDate());
		mappers.add( new FxFarBaseSettleDate());
		mappers.add( new FxFarDealtAmount());
		mappers.add( new FxFarDealtRate());
//		mappers.add( new FxFarCounterAmount());
		return mappers;
	}
	
	@Override
	public List<FieldMapper> getFieldMappersPreProcess() {
		List<FieldMapper> mappers = new ArrayList<FieldMapper>();
		mappers.add( new TradePriceFar());
		return mappers;
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.messageProcessor.MessageProcessorBase#getMessageMapper(com.olf.openjvs.Table)
	 */
	@Override
	public MessageMapper getMessageMapper(Table message) throws MessageMapperException {
		return new JmpExecuteMsgMapper(message);
	}



	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.messageProcessor.MessageProcessorBase#getMessageAcceptor()
	 */
	@Override
	public MessageAcceptor getMessageAcceptor() {
		return new PrecMetalSwapMsgAcceptor();
	}
	
	@Override
	public Table processMessage(Table message) throws MessageMapperException {
		
		MessageMapper messgeMapper = getMessageMapper(message);
		List<FieldMapper> fieldMappers = getFieldMappers();
		
		for (int i=0; i < fieldMappers.size(); i++) {
			FieldMapper fieldMapper = fieldMappers.get(i);
			messgeMapper.accept(fieldMapper);			
		}
//		throw new RuntimeException ("This ends here");
		return messgeMapper.getTranFieldTable();
	}


}
