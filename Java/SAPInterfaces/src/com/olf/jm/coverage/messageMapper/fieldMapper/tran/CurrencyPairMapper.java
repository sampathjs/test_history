package com.olf.jm.coverage.messageMapper.fieldMapper.tran;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase;
import com.olf.jm.coverage.businessObjects.ICoverageTrade;
import com.olf.openrisk.trading.EnumTradingObject;



/**
 * The Class CurrencyPairMapper. Sets the currency pair on the trade builder object. 
 * Field is only applicable on new trades, not valid on updates.
 * 
 * In the new cut MR2  /MR3 the currency pair is restricted by the portfolio. to get round the issue
 * we populate the Pair field. this is not supported via the OC enumerations so is being set as an info
 * field.
 * 
 */
public class CurrencyPairMapper extends FieldMapperBase {

	/** The coverage trade loaded from the database. */
	private ICoverageTrade coverageTrade;
	
	/** The template data loaded from the database. */
	private ISapTemplateData templateData;
	
	/**
	 * Instantiates a new currency pair mapper.
	 *
	 * @param context the context the script is running in
	 * @param currentCoverageTrade the coverage trade loaded from the database
	 * @param currentTemplateData the  template data loaded from the database
	 */
	public CurrencyPairMapper(final Context context, final ICoverageTrade currentCoverageTrade, 
			final ISapTemplateData currentTemplateData) {
		super(context);
		
		templateData = currentTemplateData;
		
		coverageTrade = currentCoverageTrade;
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
		return "Pair";
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.fieldMapper.FieldMapperBase#getValue()
	 */
	@Override
	public final String getValue() {
		return templateData.getTicker();
	}


}
