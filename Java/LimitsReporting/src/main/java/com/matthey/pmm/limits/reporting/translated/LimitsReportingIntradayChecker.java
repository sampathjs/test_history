package com.matthey.pmm.limits.reporting.translated;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.matthey.pmm.limits.reporting.translated.DealingLimitChecker.DealingLimitType;
import com.openlink.util.logging.PluginLog;

public class LimitsReportingIntradayChecker {
    private static final String runType = "Liquidity";

    private final LimitsReportingConnector connector;
    private final DealingLimitChecker dealingLimitChecker;
    
    public LimitsReportingIntradayChecker (final LimitsReportingConnector connector) {
    	this.connector = connector;    	
    	dealingLimitChecker = new DealingLimitChecker(connector);
    }
    
    public List<RunResult> run() {
        PluginLog.info("checking liquidity limits");

        List<RunResult> intradayResults = new ArrayList<>();
        for (LiquidityLimit liquidityLimit : connector.getLiquidityLimits()) {
            PluginLog.info("checking liquidity limit: " + liquidityLimit);
            double fxRate = connector.getGbpUsdRate(connector.getRunDate());
            PluginLog.info("GBP/USD rate: " + fxRate);
            Map<String, Double> metalPrices = connector.getMetalPrices();
            PluginLog.info("metal prices: " + metalPrices);
            MetalBalances metalBalances = connector.getMetalBalances();
            double liquidity = metalBalances.getBalance(runType, liquidityLimit.getMetal());
            boolean breach = !(   liquidity >= liquidityLimit.getLowerLimit() 
            		           && liquidity <= liquidityLimit.getUpperLimit());
            boolean breachLowerLimit = liquidity < liquidityLimit.getLowerLimit();
            int diff = (int)(!breach?0:(breachLowerLimit?
            		liquidityLimit.getLowerLimit() - liquidity:
            		liquidity - liquidityLimit.getUpperLimit()
            		));
            double breachTOz = getBreachBalancesInTOz(metalBalances, liquidityLimit.getMetal(), diff);
            PluginLog.info("breach in TOz: " + breachTOz);
            double liability = breachTOz * metalPrices.get(liquidityLimit.getMetal()) / fxRate;
            PluginLog.info("liability: " + liability);            
            boolean critical = !breachLowerLimit && liability > liquidityLimit.getMaxLiability();
            String liquidityBreachLimit = breach?(breachLowerLimit?"Lower Limit":"UpperLimit"):"";
            
            RunResult runResult =   new RunResult (
            		connector.getRunDate(), // runTime
            		runType, // runType
            		"", // desk
            		liquidityLimit.getMetal(), // metal
            		liquidityLimit.getLowerLimit(), // liquidityLowerLimit
            		liquidityLimit.getUpperLimit(), // liquidityUpperLimit
            		liquidityLimit.getMaxLiability(), // liquidityMaxLiability
            		0, // position limit
            		breach, // breach
            		liquidityBreachLimit, // liquidityBreachLimit
            		liquidity, // currentPosition
            		diff, // liquidityDiff
            		breachTOz, // breachTOz
            		liability, // breachGBP
            		critical,  // critical
            		RunResult.getBreachDates(connector, breach, runType, 
            				liquidityBreachLimit, "", liquidityLimit.getMetal()) // breachDates
            		);
            intradayResults.add(runResult);
        }
        for ( RunResult runResult : intradayResults) {
            connector.saveRunResult(runResult);        	
        }
        return intradayResults;
    }

    private double getBreachBalancesInTOz(MetalBalances metalBalances, String metal, int diff) {
    	List<String> balancesLinesTitlesForRunType = new ArrayList<>();
    	for (BalanceLine balanceLine : connector.getBalanceLines()) {
    		if (balanceLine.getPurpose().equals(runType)) {
    			balancesLinesTitlesForRunType.add(balanceLine.getLineTitle());
    		}
    	}    	
        PluginLog.info("balance lines to be used " + balancesLinesTitlesForRunType);
        int totalBalances = 0;
        for (String balanceLineTitle : balancesLinesTitlesForRunType) {
            int balance = metalBalances.getBalance(balanceLineTitle, metal);
            totalBalances += balance;
        }
        PluginLog.info("total balance for " + metal + " in TOz:" + totalBalances);
        return totalBalances * diff / 100.0d;
    }
}
