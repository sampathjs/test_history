package com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase;
import com.olf.openrisk.trading.EnumTradingObject;
import com.olf.openrisk.trading.EnumTransactionFieldId;


/**
 * The Class ToolSetMapper. Maps the toolset field.
 */
public class ReferenceMapper extends FieldMapperBase {
	
	/**
	 * Instantiates a new toolset mapper.
	 *
	 * @param context the context

	 */
	public ReferenceMapper(final Context context) {
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
		return EnumTransactionFieldId.ReferenceString;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getValue()
	 */
	@Override
	public final String getValue() {
		Date serverDate = context.getServerTime();
		
		SimpleDateFormat format = new SimpleDateFormat("ddMMyyyy_HHmmss");
		String timeStamp = format.format(serverDate);

		return context.getUser().getId() + "_" + timeStamp;
	}
	
}
