package com.olf.jm.coverage.messageMapper.fieldMapper.leg;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTradingObject;


/**
 * The Class IndexMapper. Populates the tradebuilder field projection index.
 */
public class IndexMapper extends FieldMapperBase {
	
	/** The sap template data loaded from the database. */
	private ISapTemplateData sapTemplateData;
	
	/** The leg to set. */
	private int legToSet;
	
	/**
	 * Instantiates a new index mapper.
	 *
	 * @param context the context the script is running in
	 * @param currentSapTemplateData the current sap template data loaded from the db
	 * @param leg the leg to set the field on
	 */
	public IndexMapper(final Context context, final ISapTemplateData currentSapTemplateData, final int leg) {
		super(context);
		
		sapTemplateData = currentSapTemplateData;
		
		legToSet = leg;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.fieldMapper.FieldMapperBase#getTradingObject()
	 */
	@Override
	protected final EnumTradingObject getTradingObject() {
		
		return EnumTradingObject.Leg;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.fieldMapper.FieldMapperBase#getValue()
	 */
	@Override
	public final String getValue() {
		return sapTemplateData.getProjectionIndex();
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.fieldMapper.FieldMapperBase#getLegFieldId()
	 */
	@Override
	protected final EnumLegFieldId getLegFieldId() {
		return EnumLegFieldId.ProjectionIndex;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.fieldMapper.FieldMapperBase#getLegId()
	 */
	@Override
	protected final int getLegId() {
		return legToSet;
	}		
}
