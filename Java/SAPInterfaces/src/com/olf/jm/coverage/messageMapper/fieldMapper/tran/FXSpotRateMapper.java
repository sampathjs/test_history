package com.olf.jm.coverage.messageMapper.fieldMapper.tran;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase;
import com.olf.jm.coverage.businessObjects.ICoverageTrade;
import com.olf.openrisk.market.Element;
import com.olf.openrisk.market.Elements;
import com.olf.openrisk.market.EnumElementType;
import com.olf.openrisk.market.EnumGptField;
import com.olf.openrisk.market.ForwardCurve;
import com.olf.openrisk.market.GridPoint;
import com.olf.openrisk.market.GridPoints;
import com.olf.openrisk.market.Market;
import com.olf.openrisk.trading.EnumTradingObject;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.jm.logging.Logging;

/**
 * The Class FXSpotRateMapper.
 */
public class FXSpotRateMapper extends FieldMapperBase {



	/** The template data. */
	private ISapTemplateData templateData;
	
	/** The current transaction. */
	private ICoverageTrade transaction;
	
	
	/**
	 * Instantiates a new FX spot rate mapper.
	 *
	 * @param context the context
	 * @param currentTemplateData the current template data
	 * @param transaction the transaction
	 */
	public FXSpotRateMapper(final Context context, final ISapTemplateData currentTemplateData, final ICoverageTrade currentTransaction) {
		super(context);
		
		templateData = currentTemplateData;
		
		transaction = currentTransaction;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#isApplicable()
	 */
	@Override
	public final boolean isApplicable() {
		if (transaction.isValid()) {
			return false;
		}
		
		return true;
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
		return EnumTransactionFieldId.FxSpotRate;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getValue()
	 */
	@Override
	public final String getValue() {
		Market market = context.getMarket();
		
		String projectionIndex = templateData.getProjectionIndex();
		
		ForwardCurve forwardCurve = (ForwardCurve) market.getElement(EnumElementType.ForwardCurve, projectionIndex);

		if (forwardCurve == null) {
			throw new RuntimeException("Error loading index " + projectionIndex);
		}
		
		GridPoints gridPoints = forwardCurve.getActiveGridPoints();
		
		String spotPrice = getSpotPrice(gridPoints);
		
		if (spotPrice != null) {
			return spotPrice;
		}
		
		// Spot price not found check parent
		Elements parentCurves = forwardCurve.getDirectParents();
		
		for (Element curve : parentCurves) {
			try {
				forwardCurve = (ForwardCurve) curve;
			} catch (Exception e) {
				Logging.info("Not a forward curve, skipping index " + curve.getName());
				continue;
			}
			
			if (forwardCurve != null) {
				forwardCurve.loadUniversal();
				gridPoints = forwardCurve.getActiveGridPoints();
				
				spotPrice = getSpotPrice(gridPoints);
				
				if (spotPrice != null) {
					return spotPrice;
				}
			}
		}
		
		throw new RuntimeException("Error loading spot price for index " + projectionIndex);

	}
	
	/**
	 * Gets the spot price.
	 *
	 * @param gridPoints the grid points
	 * @return the spot price
	 */
	private String getSpotPrice(final GridPoints gridPoints) {
		double  spotPrice = 0.0;
		if (gridPoints != null && gridPoints.getCount() > 0) {
			GridPoint gridPoint = gridPoints.getGridPoint("Spot");
			
			if (gridPoint != null && gridPoint.isActive()) {
				spotPrice = gridPoint.getValue(EnumGptField.EffInput);
				return Double.toString(spotPrice);
			}
		}
		return null;
	}
	

	
}
