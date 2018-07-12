package com.olf.jm.advancedPricingReporting.util;

import java.util.HashMap;
import java.util.Map;

import com.olf.embedded.application.Context;
import com.olf.openrisk.market.EnumElementType;
import com.olf.openrisk.market.EnumGptField;
import com.olf.openrisk.market.ForwardCurve;
import com.olf.openrisk.market.GridPoint;
import com.olf.openrisk.market.GridPoints;
import com.olf.openrisk.market.Market;
import com.olf.openrisk.market.MarketFactory;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * A factory class for loading metal prices. Contains information mapping the metal name to the currency id and the metal to the 
 * market curve
 */
public class PriceFactory {
	

	/** The Constant XAG. */
	public final static int XAG	= 53;
	
	/** The Constant XAU. */
	public final static int XAU	= 54;
	
	/** The Constant XPD. */
	public final static int XPD	= 55;
	
	/** The Constant XPT. */
	public final static int XPT	= 56;
	
	/** The Constant XRH. */
	public final static int XRH	= 58;
	
	/** The Constant XIR. */
	public final static int XIR	= 61;
	
	/** The Constant XOS. */
	public final static int XOS	= 62;
	
	/** The Constant XRU. */
	public final static int XRU	= 63;
	
	/** The Constant METAL_ID_LOOKUP. Maps the currency id to the market curve */
	private final static Map<Integer, String> METAL_ID_LOOKUP = new HashMap<Integer, String>() {
		private static final long serialVersionUID = 7994655896937689758L;

	{
		put(XAG,"PX_XAG.USD");
		put(XAU,"PX_XAU.USD");
		put(XPD,"PX_XPD.USD");
		put(XPT,"PX_XPT.USD");
		put(XRH,"PX_XRH.USD");
		put(XIR,"PX_XIR.USD");
		put(XOS,"PX_XOS.USD");
		put(XRU,"PX_XRU.USD");
		
		}};
		
		/** The Constant METAL_NAME_LOOKUP. Maps the metal short name to the currency id.  */
		private final static Map<String, Integer> METAL_NAME_LOOKUP = new HashMap<String, Integer>() {
			private static final long serialVersionUID = 7994655896937689758L;

		{
			put("XAG",XAG);
			put("XAU",XAU);
			put("XPD",XPD);
			put("XPT",XPT);
			put("XRH",XRH);
			put("XIR",XIR);
			put("XOS",XOS);
			put("XRU",XRU);
			
			}};	
	
	/** The market. */
	private Market market;
	
	/**
	 * Instantiates a new price factory.
	 *
	 * @param context the current script context
	 */
	public 	PriceFactory( Context context) {
		MarketFactory mf = context.getMarketFactory();
		
		market = mf.getMarket();		
		
		if(market == null) {
			String errorMessage = "Error initilising PriceFactory, error loading market context";
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}else {
			PluginLog.info("Refreshing market prices - Loading latest universal prices if changed");
			market.refresh(false, false);
		}

	}
	
	/**
	 * Gets the spot rate for a given metal.
	 *
	 * @param metal the metal name to fetch the price for
	 * @return the spot rate
	 */
	public double getSpotRate(String metal) {
		
		if(metal == null || metal.length() == 0) {
			PluginLog.warn("no metal specificed returning 0.");
			return 0.0;
		}
	
		Integer metalId = METAL_NAME_LOOKUP.get(metal);
		
		if(metalId == null ) {
			String errorMessage = "Error loading the market curve for metal " + metal;
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		return getSpotRate(metalId);
	}

	/**
	 * Gets the spot rate for a given metal.
	 *
	 * @param metal the currency id of the metal to fetch the price for
	 * @return the spot rate
	 */
	public double getSpotRate(int metal) {
		
		String curveName = METAL_ID_LOOKUP.get(metal);
		
		if(curveName == null || curveName.length() == 0) {
			String errorMessage = "Error loading the market curve for metal " + metal;
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);			
		}
		
		ForwardCurve curve = (ForwardCurve)market.getElement(EnumElementType.ForwardCurve, curveName);
		
		if(curve == null) {
			String errorMessage = "Error loading the market curve " + curveName;
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}

		GridPoints gpts = curve.getGridPoints();

		return getSpotPrice(gpts);
	}
	
	/**
	 * Gets the spot price from the curves grid points.
	 *
	 * @param gridPoints the grid points loaded from the curve
	 * @return the spot price
	 */
	private double getSpotPrice(final GridPoints gridPoints) {
		double  spotPrice = Double.NaN;
		if (gridPoints != null && gridPoints.getCount() > 0) {
			GridPoint gridPoint = gridPoints.getGridPoint("Spot");
			
			if (gridPoint != null && gridPoint.isActive()) {
				spotPrice = gridPoint.getValue(EnumGptField.EffInput);
				return spotPrice;
			}
		}
		return spotPrice;
	}	
}
