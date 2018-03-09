package com.olf.jm.coverage.messageMapper.fieldMapper.tran;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase;
import com.olf.jm.coverage.businessObjects.ICoverageTrade;
import com.olf.openrisk.trading.EnumTradingObject;
import com.olf.openrisk.trading.EnumTransactionFieldId;

/**
 * The Class CashFlowTypeMapper. sets the field cash flow type on the tradebuilder message
 */
public class CashFlowTypeMapper extends FieldMapperBase {

	/** The coverage trade loaded from the database. */
	private ICoverageTrade coverageTrade;
	
	/** The template data loaded from the database. */
	private ISapTemplateData templateData;

	/**
	 * Instantiates a new cash flow type mapper.
	 *
	 * @param context the context the script is running in.
	 * @param loadedCoverageTrade the  coverage trade loaded from the db.
	 * @param loadedTemplateData the loaded template data
	 */
	public CashFlowTypeMapper(final Context context, final ICoverageTrade loadedCoverageTrade, final ISapTemplateData loadedTemplateData) {
		super(context);
		coverageTrade = loadedCoverageTrade;
		
		templateData = loadedTemplateData;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.fieldMapper.FieldMapperBase#isApplicable()
	 */
	@Override
	public final boolean isApplicable() {
		// Field is only valid on new trades, if a coverage trade already exists then 
		// don't output the field.
		return (coverageTrade.isValid() && coverageTrade.getDealTrackingNumber() > 0) ? false : true;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.fieldMapper.FieldMapperBase#getTradingObject()
	 */
	@Override
	protected final EnumTradingObject getTradingObject() {
		return EnumTradingObject.Transaction;
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.fieldMapper.FieldMapperBase#getTransactionFieldId()
	 */
	@Override
	protected final EnumTransactionFieldId getTransactionFieldId() {
		return EnumTransactionFieldId.CashflowType;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.fieldMapper.FieldMapperBase#getValue()
	 */
	@Override
	public final String getValue() {
		return templateData.getCflowType();
	}
}
