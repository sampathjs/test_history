package com.olf.jm.coverage.messageMapper.fieldMapper.tran;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTradingObject;

/**
 * The Class PriceUnitMapper.
 */
public class PriceUnitMapper extends FieldMapperBase {

	/** The source data. */
	private Table sourceData;
	
	/** The leg to set. */
	private int legToSet;
	
	/**
	 * Instantiates a new price unit mapper.
	 *
	 * @param context the context
	 * @param currentSourceData the current source data
	 * @param leg the leg
	 */
	public PriceUnitMapper(final Context context, final Table currentSourceData, final int leg) {
		super(context);
		
		sourceData = currentSourceData;
		
		legToSet = leg;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getTradingObject()
	 */
	@Override
	protected final EnumTradingObject getTradingObject() {
		return EnumTradingObject.Leg;
	}

	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getLegFieldId()
	 */
	@Override
	protected final EnumLegFieldId getLegFieldId() {
		return EnumLegFieldId.PriceUnit;
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getValue()
	 */
	@Override
	public final String getValue() {
		return sourceData.getString(EnumSapCoverageRequest.WEIGHT_UOM.getColumnName(), 0);
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getLegId()
	 */
	@Override
	protected final int getLegId() {
		return legToSet;
	}


}
