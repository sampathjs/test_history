package com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumTradingObject;
import com.olf.openrisk.trading.EnumTransactionFieldId;


/**
 * The Class InstrumentTypeMapper. Maps the toolset field.
 */
public class InstrumentTypeMapper extends FieldMapperBase {
	
	/**
	 * Instantiates a new toolset mapper.
	 *
	 * @param context the context

	 */
	public InstrumentTypeMapper(final Context context) {
		super(context);
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getTradingObject()
	 */
	@Override
	protected final EnumTradingObject getTradingObject() {
		return EnumTradingObject.Transaction;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getTransactionFieldId()
	 */
	@Override
	protected final EnumTransactionFieldId getTransactionFieldId() {
		return EnumTransactionFieldId.InstrumentType;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getValue()
	 */
	@Override
	public final String getValue() {
		return EnumInsType.Strategy.getName();
	}
	
}
