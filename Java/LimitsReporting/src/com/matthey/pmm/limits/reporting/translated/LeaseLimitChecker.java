package com.matthey.pmm.limits.reporting.translated;

import java.util.ArrayList;
import java.util.List;

import com.olf.jm.logging.Logging;

public class LeaseLimitChecker {
	private final LimitsReportingConnector connector;	
    
    public LeaseLimitChecker (final LimitsReportingConnector connector) {
    	this.connector = connector;
    }
    
    public RunResult check()  {
    	Logging.info("checking lease limit");
    	Logging.info("lease limit: " + connector.getLeaseLimit());
    	Logging.info("metal prices: " + connector.getMetalPrices());
        double fxRate = connector.getGbpUsdRate(connector.getRunDate());
        Logging.info("FX rate: " + fxRate);

        String runType = "Lease";
        double loanFacilityExposure = calculateLoanFacilityExposure(fxRate, runType);
        Logging.info("exposure from loan facilities: " + loanFacilityExposure);        
        
        double dealExposure = 0.0;
        for (LeaseDeal leaseDeal : connector.getLeaseDeals()) {
        	dealExposure += calcExposure (leaseDeal);
        }
                        
        Logging.info("exposure from deals: " + dealExposure);
        double totalExposure = loanFacilityExposure + dealExposure;
        boolean breach = totalExposure > connector.getLeaseLimit();
        return new RunResult(
        		connector.getRunDate(), // runTime
            runType, //runType,
            "", // desk
            "", // metal
            0, // liquidityLowerLimit
            0, // liquidityUpperLimit
            0, // liquidityMaxLiability
            connector.getLeaseLimit(), // positionLimit
            breach, // breach
            "", // liquidityBreachLimit
            totalExposure, // currentPositio
            0, // liquidityDiff
            0.0, // breachTOz
            Math.max(totalExposure - connector.getLeaseLimit(), 0.0), // breachGBP
            false, // critical
            RunResult.getBreachDates(connector, breach, runType, "", "", "") // breachDates = 
        );
    }

	private double calculateLoanFacilityExposure(double fxRate, String runType) {
		double sumLoanFacilityExposure=0.0;
        for (BalanceLine balanceLine : connector.getBalanceLines()) {
        	if (balanceLine.getPurpose().equals(runType)) {
        		sumLoanFacilityExposure += sumLoanFacility (balanceLine.getLineTitle());
        	}
        }
        double loanFacilityExposure = sumLoanFacilityExposure / fxRate;
		return loanFacilityExposure;
	}

    private double sumLoanFacility(String balanceLine) {
    	double sum = 0.0;
    	for (String metal : MetalBalances.metalNames.keySet()) {
    		double balance = connector.getMetalBalances().getBalance(balanceLine, metal);
    		double exposure = (balance<0?-balance:0.0) * connector.getMetalPrices().get(metal);
            Logging.info(balanceLine + ": metal -> " + metal + "; balance -> " + balance + "; exposure ->  " + exposure);
            sum += exposure;
    	}
        Logging.info(balanceLine + ": sum -> " + (int)sum);
        return sum;
    }

    private double calcExposure( LeaseDeal deal) {
        double gbpUsdRate = connector.getGbpUsdRate(deal.getStartDate());
        Logging.info("deal: " + deal + "; GBP/USD rate: " + gbpUsdRate);
        return deal.getNotnl() * deal.getCurrencyFxRate() / gbpUsdRate;
    }

}
