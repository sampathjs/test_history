package com.matthey.pmm.limits.reporting.translated;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.olf.jm.logging.Logging;

public class DealingLimitChecker {
	static enum DealingLimitType {
	    OVERNIGHT("Overnight"), INTRADAY_DESK("Intraday Desk"), OVERNIGHT_DESK("Overnight Desk")
	    ;
		private final String fullName;
		private DealingLimitType (final String fullName) {
			this.fullName = fullName;
		}
		public String getFullName() {
			return fullName;
		}
	}	
	
    private final LimitsReportingConnector connector;
    
    public DealingLimitChecker (final LimitsReportingConnector connector) {
    	this.connector = connector;
    }
    
    public List<RunResult> check(DealingLimitType type) {
    	Logging.info("checking overnight limits");
        double fxRate = connector.getGbpUsdRate(connector.getRunDate());
        Logging.info("GBP/USD rate: " + fxRate);
        Map<String, Double> metalPrices = connector.getMetalPrices();
        Logging.info("metal prices: " + metalPrices);
        ImmutableTable<String, String, Double> closingPositions = connector.getClosingPositions();
        Logging.info("closing positions: " + closingPositions);

        switch (type) {
        case OVERNIGHT:
        	return processOvernightResults (fxRate, metalPrices, closingPositions);
        case INTRADAY_DESK:
        	return processIntradayDeskResults (fxRate, metalPrices, closingPositions);
        case OVERNIGHT_DESK:
        	return processOvernightDeskResults (fxRate, metalPrices, closingPositions);
        }
        return null;
    }

    private List<RunResult> processOvernightDeskResults(double fxRate, Map<String, Double> metalPrices, ImmutableTable<String, String, Double> closingPositions) {
        List<DealingLimit> overnightDeskLimits = connector.getDealingLimits();
        for (int index = overnightDeskLimits.size()-1; index >= 0; index--) {
        	if (!overnightDeskLimits.get(index).getLimitType().equals(DealingLimitType.OVERNIGHT_DESK.fullName)) {
        		overnightDeskLimits.remove(index);
        	}
        }
        Logging.info("overnight desk limits " + overnightDeskLimits);
        
        Table<String, String, Double> positions = HashBasedTable.create(connector.getClosingPositions());
        Set<String> metals = new HashSet<>(positions.columnKeySet());
        for (String metal : metals) {
        	Double ukPosition = positions.get("JM PMM UK", metal);
        	Double usPosition = positions.get("JM PMM US", metal);
        	Double hkPosition = positions.get("JM PMM HK", metal);
        	double position = (ukPosition!=null?ukPosition:0.0d)
        			+         (usPosition!=null?usPosition:0.0d)
        			+         (hkPosition!=null?hkPosition:0.0d);
        	positions.put("JM PMM One Book", metal, position);
        }
        Logging.info("overnight positions " + positions);
        return checkDeskLimits(overnightDeskLimits, fxRate, metalPrices, positions);
	}

	private List<RunResult> processIntradayDeskResults(double fxRate, Map<String, Double> metalPrices, ImmutableTable<String, String, Double> closingPositions) {
        List<DealingLimit> intradayDeskLimits = connector.getDealingLimits();
        for (int index = intradayDeskLimits.size()-1; index >= 0; index--) {
        	if (!intradayDeskLimits.get(index).getLimitType().equals(DealingLimitType.INTRADAY_DESK.fullName)) {
        		intradayDeskLimits.remove(index);
        	}
        }
        Logging.info("intraday desk limits " + intradayDeskLimits);
        return checkDeskLimits(intradayDeskLimits, fxRate, metalPrices, closingPositions);
	}

	private List<RunResult> processOvernightResults(double fxRate, Map<String, Double> metalPrices, ImmutableTable<String, String, Double> closingPositions) {
        List<DealingLimit> overnightLimits = connector.getDealingLimits();
        for (int index = overnightLimits.size()-1; index >= 0; index--) {
        	if (!overnightLimits.get(index).getLimitType().equals(DealingLimitType.OVERNIGHT.fullName)) {
        		overnightLimits.remove(index);
        	}
        }
        if (overnightLimits.size() != 1) {
        	throw new RuntimeException ("Unexpected count of overnights limits found in '" + overnightLimits + "'. Expected 1");
        }
        
        Logging.info("overnight limit "  + overnightLimits);
        Map<String, Double> positionsByMetalOverAllBus = new HashMap<String, Double>();
        for (Map<String, Double> metalAndPosPerBu : closingPositions.rowMap().values()) {
        	for (Entry<String, Double> metalAndPos : metalAndPosPerBu.entrySet()) {
        		Double oldValue = positionsByMetalOverAllBus.get(metalAndPos.getKey());
        		if (oldValue == null) {
        			oldValue = 0.0;
        		}
            	positionsByMetalOverAllBus.put(metalAndPos.getKey(), oldValue + metalAndPos.getValue());
        	}
        }
        
        Logging.info("closing positions by metal: " + positionsByMetalOverAllBus);
        Map<String, Double> extraPositions = connector.getUnhedgedAndRefiningGainsPositions();
        Logging.info("unhedged and refining gains positions: " + extraPositions);
        double position =
            (       sumForAllMetals(positionsByMetalOverAllBus, metalPrices)
            	+	sumForAllMetals(extraPositions, metalPrices)) 
            / fxRate;
        boolean breach = position > overnightLimits.get(0).getLimit();
        
        RunResult overnightResult = new RunResult (
        		connector.getRunDate(), // runTime
        		DealingLimitType.OVERNIGHT.fullName, // runType
        		"", // desk
        		"", // metal
        		0, // liquidityLowerLimit
        		0, // liquidityUpperLimit
        		0, // liquidityMaxLiability
        		overnightLimits.get(0).getLimit(), // position limit
        		breach, // breach
        		"", // liquidityBreachLimit
        		position, // currentPosition
        		0, // liquidityDiff
        		0.0d, // breachTOz
        		Math.max(position - overnightLimits.get(0).getLimit(), 0.0), // breachGBP
        		false,  // critical
        		RunResult.getBreachDates(connector, breach, DealingLimitType.OVERNIGHT.fullName, "", "", "") // breachDates
        		);
        
        return Arrays.asList(overnightResult);
	}

	private List<RunResult> checkDeskLimits(
			List<DealingLimit> deskLimits,
			Double fxRate,
			Map<String, Double> metalPrices,
			Table<String, String, Double> positions)  {
		List<RunResult> results = new ArrayList<>(deskLimits.size());
		for (DealingLimit deskLimit : deskLimits) {
			if (!positions.contains(deskLimit.getDesk(), deskLimit.getMetal())) {
				continue;
			}
			Double position = positions.get(deskLimit.getDesk(), deskLimit.getMetal());
			boolean breach = (position!=null?position:-1.0d) > deskLimit.getLimit();
			double breachTOz = Math.max(position - deskLimit.getLimit(), 0);
			RunResult result = new RunResult(
					connector.getRunDate(), // runTime,
					deskLimit.getLimitType(), //runType,
					deskLimit.getDesk(), //desk 
					deskLimit.getMetal(), // metal
					0, //liquidityLowerLimit,
					0, //liquidityUpperLimit,
					0, //liquidityMaxLiability,
					deskLimit.getLimit(), // positionLimit
					breach, // breach
					"", // liquidityBreachLimit,
					position, //currentPosition,
					0, // liquidityDiff,
					breachTOz,
					breachTOz * metalPrices.get(deskLimit.getMetal()) / fxRate,
					false,
					RunResult.getBreachDates(
		                    connector,
		                    breach,
		                    deskLimit.getLimitType(),// runType
		                    "",
		                    deskLimit.getDesk(), //desk,
		                    deskLimit.getMetal()//metal
		                ));
			results.add(result);
		}		
		return results;
    }

    private double sumForAllMetals(Map<String, Double> positions, Map<String, Double> metalPrices) {
    	double sum=0.0d;
    	for (Entry<String, Double> metalAndPosition : positions.entrySet()) {
    		Double metalPrice = metalPrices.get(metalAndPosition.getKey());
    		if (metalPrice == null) {
    			throw new RuntimeException ("no price for metal '" + metalAndPosition.getKey() + "' found in price list" + metalPrices);
    		}
    		sum += metalAndPosition.getValue() * metalPrice;
    	}
    	
        return sum;
    }
}
