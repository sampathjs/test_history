package com.matthey.pmm.limits.reporting.translated;

import java.util.List;

import com.matthey.pmm.limits.reporting.translated.DealingLimitChecker.DealingLimitType;
import com.olf.jm.logging.Logging;

public class LimitsReportingIntradayChecker {
    private final LimitsReportingConnector connector;
    private final DealingLimitChecker dealingLimitChecker;
    
    public LimitsReportingIntradayChecker (final LimitsReportingConnector connector) {
    	this.connector = connector;    	
    	dealingLimitChecker = new DealingLimitChecker(connector);
    }
    
    public List<RunResult> run() {
<<<<<<< HEAD
        Logging.info("checking liquidity limits");
=======
        PluginLog.info("checking liquidity limits");
>>>>>>> refs/remotes/origin/v17_master

        List<RunResult> intradayResults = dealingLimitChecker.check(DealingLimitType.INTRADAY_DESK);

        for ( RunResult runResult : intradayResults) {
            connector.saveRunResult(runResult);        	
        }
        return intradayResults;
    }
}
