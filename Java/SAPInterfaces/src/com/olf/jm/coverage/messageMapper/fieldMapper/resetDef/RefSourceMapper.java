package com.olf.jm.coverage.messageMapper.fieldMapper.resetDef;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumResetDefinitionFieldId;
import com.olf.openrisk.trading.EnumTradingObject;

/**
 * The Class RefSourceMapper.
 */
public class RefSourceMapper extends FieldMapperBase {

	/** The source data. */
	private ISapTemplateData templateData;
	
	/** The leg to set. */
	private int legToSet;
	
	/**
	 * Instantiates a new ref source mapper.
	 *
	 * @param context the context
	 * @param currentSourceData the current source data
	 * @param leg the leg
	 */
	public RefSourceMapper(final Context context, final ISapTemplateData currentTemplateData, final int leg) {
		super(context);
		
		templateData = currentTemplateData;
		
		legToSet = leg;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getTradingObject()
	 */
	@Override
	protected final EnumTradingObject getTradingObject() {
		return EnumTradingObject.ResetDefinition;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.fieldMapper.FieldMapperBase#getResetDefinitionFieldId()
	 */
	@Override
	protected final EnumResetDefinitionFieldId getResetDefinitionFieldId() {
		return EnumResetDefinitionFieldId.ReferenceSource;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.fieldMapper.FieldMapperBase#getValue()
	 */
	@Override
	public final String getValue() {
		return templateData.getRefSource();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getLegId()
	 */
	@Override
	protected final int getLegId() {
		return legToSet;
	}


}
