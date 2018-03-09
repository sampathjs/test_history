package com.olf.jm.sapTransfer.messageMapper;

import java.util.List;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;
import com.olf.jm.SapInterface.messageMapper.IAuxDataMapper;
import com.olf.jm.SapInterface.messageMapper.IFieldMapperFactory;
import com.olf.jm.SapInterface.messageMapper.TradeBuilderMapperBase;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.IFieldMapper;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.template.ITemplateNameMapper;
import com.olf.jm.sapTransfer.messageMapper.fieldMapper.auxData.TransferAuxData;
import com.olf.openrisk.table.Table;

/**
 * The Class CoverageTradeBuilderMapper. Class to map the input source message
 * into a Connex standard tradebuilder message.
 */
public class TransferTradeBuilderMapper extends TradeBuilderMapperBase {

	/**
	 * Instantiates a new coverage trade builder mapper.
	 * 
	 * @param currentContext
	 *            the current context
	 * @param currentSapPartyData
	 *            the current sap party data
	 * @param currentSapTemplateData
	 *            the current sap template data
	 * @param trade
	 *            the trade
	 */
	public TransferTradeBuilderMapper(final Context currentContext,
			final ISapPartyData currentSapPartyData,
			final ISapTemplateData currentSapTemplateData,
			final ISapEndurTrade trade) {
		super(currentContext, currentSapPartyData, currentSapTemplateData,
				trade);

	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.TradeBuilderMapperBase#getFieldMappers(com.olf.openrisk.table.Table)
	 */
	@Override
	protected final List<IFieldMapper> getFieldMappers(final Table source) {
		ISapEndurTrade existingTrade = (ISapEndurTrade) getExistingTrade();
		IFieldMapperFactory mapperFactory = new TransferFieldMapperFactory(
				getContext(), source, getSapPartyData(), getSapTemplateData(),
				existingTrade);

		return mapperFactory.buildFieldMappers(getSapTemplateData()
				.getInsType());
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.TradeBuilderMapperBase#getAuxDataMapper(com.olf.openrisk.table.Table)
	 */
	@Override
	protected final IAuxDataMapper getAuxDataMapper(final Table source) {
		return new TransferAuxData(getContext(), source);
 	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.TradeBuilderMapperBase#getTemplateMapper(com.olf.openrisk.table.Table)
	 */
	@Override
	protected final ITemplateNameMapper getTemplateMapper(final Table source) {
		ISapEndurTrade existingTrade = (ISapEndurTrade) getExistingTrade();
		IFieldMapperFactory mapperFactory = new TransferFieldMapperFactory(
				getContext(), source, getSapPartyData(), getSapTemplateData(),
				existingTrade);

		return mapperFactory.buildTemplateMapper(getSapTemplateData()
				.getInsType());
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.TradeBuilderMapperBase#getResponseTableName()
	 */
	@Override
	protected final String getResponseTableName() {
		return "mtres:getMetalTransferResponse";
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.TradeBuilderMapperBase#getResponseNameSpace()
	 */
	@Override
	protected final String getResponseNameSpaceTag() {
		return "mtres";
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.TradeBuilderMapperBase#getResponseNameSpaceLabel()
	 */
	@Override
	protected String getResponseNameSpaceLabel() {
		return "xmlns:mtres=\"urn:or-getMetalTransferResponse\"";
	}
}
