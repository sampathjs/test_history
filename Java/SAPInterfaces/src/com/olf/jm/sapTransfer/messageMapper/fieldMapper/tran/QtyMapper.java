package com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase;
import com.olf.jm.sapTransfer.businessObjects.enums.EnumSapTransferRequest;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTradingObject;


/**
 * The Class MetalMapper. Mapping logic to populate the
 * qty info field.
 */
public class QtyMapper extends FieldMapperBase {

	/** The source data. */
	private Table sourceData;
	
	/**
	 * Instantiates a new qty mapper.
	 *
	 * @param context the context
	 * @param currentSourceData the source data
	 */
	public QtyMapper(final Context context, final Table currentSourceData) {
		super(context);
		
		this.sourceData = currentSourceData;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getTradingObject()
	 */
	@Override
	protected final EnumTradingObject getTradingObject() {
		
		throw new RuntimeException("Info field.");
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#isInfoField()
	 */
	@Override
	protected final boolean isInfoField() {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getInfoFieldName()
	 */
	@Override
	protected final String getInfoFieldName() {
		return EnumStrategyInfoFields.QTY.getFieldName();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getValue()
	 */
	@Override
	public final String getValue() {
		return sourceData.getString(EnumSapTransferRequest.WEIGHT.getColumnName(), 0);
	}
	
}
