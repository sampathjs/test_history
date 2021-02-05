/*
 * File updated 05/02/2021, 17:52
 */

package com.olf.jm.advancedPricingReporting.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.olf.openjvs.OException;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;


public class MathUtils {
	private static final String CONTEXT = "Warehouse";
	
	private static final String SUB_CONTEXT = "ContainerWeightConverter";
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	public static double gmsRounding(double value, int places) {
		String tolerance  = "0.02";
		
		try {
			ConstRepository constRepository = new ConstRepository(CONTEXT, SUB_CONTEXT);
			
			String TOLERANCE = "gmsTolerance";
			tolerance = constRepository.getStringValue(TOLERANCE, tolerance);
		} catch (OException e) {
			String errorMessage = "Error loading HK conversion factor. " + e.getLocalizedMessage();
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		double roundedValue = MathUtils.round(value, places);
		
		if(Math.abs(roundedValue - MathUtils.round(roundedValue, 0)) < Double.parseDouble(tolerance)) {
			return MathUtils.round(roundedValue, 0);
		}
		
		return roundedValue;
	}
	
	public static double getHkTozToGmsConversionFactor() {
		String conversionFactor = "31.1035";
				
		try {
			ConstRepository constRepository = new ConstRepository(CONTEXT, SUB_CONTEXT);
			
			String CONVERSION_FACTOR = "hkConversionFactor";
			conversionFactor = constRepository.getStringValue(CONVERSION_FACTOR, conversionFactor);
		} catch (OException e) {
			String errorMessage = "Error loading HK conversion factor. " + e.getLocalizedMessage();
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
	
		return Double.parseDouble(conversionFactor);
		
	}
}
