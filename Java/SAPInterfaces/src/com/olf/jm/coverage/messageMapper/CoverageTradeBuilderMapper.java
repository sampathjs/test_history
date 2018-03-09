package com.olf.jm.coverage.messageMapper;

import java.util.List;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;
import com.olf.jm.SapInterface.messageMapper.IAuxDataMapper;
import com.olf.jm.SapInterface.messageMapper.IFieldMapperFactory;
import com.olf.jm.SapInterface.messageMapper.TradeBuilderMapperBase;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.IFieldMapper;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.template.ITemplateNameMapper;
import com.olf.jm.coverage.businessObjects.ICoverageTrade;
import com.olf.jm.coverage.messageMapper.fieldMapper.auxData.CoverageAuxData;
import com.olf.openrisk.table.Table;

/**
 * The Class CoverageTradeBuilderMapper. Class to map the input source message
 * into a Connex standard tradebuilder message.
 */
public class CoverageTradeBuilderMapper extends TradeBuilderMapperBase {

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
	public CoverageTradeBuilderMapper(final Context currentContext,
			final ISapPartyData currentSapPartyData,
			final ISapTemplateData currentSapTemplateData,
			final ICoverageTrade trade) {
		super(currentContext, currentSapPartyData, currentSapTemplateData,
				trade);

	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.TradeBuilderMapperBase#getFieldMappers(com.olf.openrisk.table.Table)
	 */
	@Override
	protected final List<IFieldMapper> getFieldMappers(final Table source) {
		ICoverageTrade coverageTrade = (ICoverageTrade) getExistingTrade();
		IFieldMapperFactory mapperFactory = new CoverageFieldMapperFactory(
				getContext(), source, getSapPartyData(), getSapTemplateData(),
				coverageTrade);

		return mapperFactory.buildFieldMappers(getSapTemplateData()
				.getInsType());
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.TradeBuilderMapperBase#getAuxDataMapper(com.olf.openrisk.table.Table)
	 */
	@Override
	protected final IAuxDataMapper getAuxDataMapper(final Table source) {
		return new CoverageAuxData(getContext(), source);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.TradeBuilderMapperBase#getTemplateMapper(com.olf.openrisk.table.Table)
	 */
	@Override
	protected final ITemplateNameMapper getTemplateMapper(final Table source) {
		ICoverageTrade coverageTrade = (ICoverageTrade) getExistingTrade();
		IFieldMapperFactory mapperFactory = new CoverageFieldMapperFactory(
				getContext(), source, getSapPartyData(), getSapTemplateData(),
				coverageTrade);

		return mapperFactory.buildTemplateMapper(getSapTemplateData()
				.getInsType());
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.TradeBuilderMapperBase#getResponseTableName()
	 */
	@Override
	protected final String getResponseTableName() {
		return "cdres:getCoverageDealResponse";
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.TradeBuilderMapperBase#getResponseNameSpace()
	 */
	@Override
	protected final String getResponseNameSpaceTag() {
		return "cdres";
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.TradeBuilderMapperBase#getResponseNameSpaceLabel()
	 */
	@Override
	protected final String getResponseNameSpaceLabel() {
		return "xmlns:cdres=\"urn:or-getCoverageDealResponse\"";
	}

}
