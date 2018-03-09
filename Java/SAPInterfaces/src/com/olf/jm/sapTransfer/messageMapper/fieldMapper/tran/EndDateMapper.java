package com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase;
import com.olf.jm.SapInterface.util.DateUtils;
import com.olf.jm.sapTransfer.businessObjects.enums.EnumSapTransferRequest;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.calendar.SymbolicDate;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTradingObject;
import com.olf.openrisk.trading.EnumTransactionFieldId;


/**
 * The Class EndDateMapper. Maps the end date field.
 */
public class EndDateMapper extends FieldMapperBase {
	/** The source data. */
	private Table sourceData;	
	
	/**
	 * Instantiates a new end date mapper.
	 *
	 * @param context the context
	 * @param currentSourceData the current source data
	 */
	public EndDateMapper(final Context context, final Table currentSourceData) {
		super(context);
		
		this.sourceData = currentSourceData;
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
		return EnumTransactionFieldId.MaturityDate;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getValue()
	 */
	@Override
	public final String getValue() {
		Date approvalDate = DateUtils.getDate(sourceData.getString(EnumSapTransferRequest.APPROVAL_DATE.getColumnName(), 0));
		
		CalendarFactory calenderFactory = context.getCalendarFactory();
		
		SymbolicDate endDate = calenderFactory.createSymbolicDate("3eom");
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
		
		return sdf.format(endDate.evaluate(approvalDate));
	}
	
}
