package com.matthey.pmm.limits.reporting.translated;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.matthey.pmm.limits.reporting.translated.DealingLimitChecker.DealingLimitType;

public class LimitsReportingEODChecker {
	private final LimitsReportingConnector connector;
	private final EmailSender emailSender;
	
    private static final Logger logger = LoggerFactory.getLogger(LimitsReportingEODChecker.class);
    private final BreachNotifier breachNotifier;
    private LiquidityLimitsChecker liquidityLimitsChecker;
    private DealingLimitChecker dealingLimitChecker;
    private LeaseLimitChecker leaseLimitChecker;
    
    public LimitsReportingEODChecker (final LimitsReportingConnector connector,
    	final EmailSender emailSender) {
    	this.connector = connector;
    	this.emailSender = emailSender;

        breachNotifier = new BreachNotifier(connector, emailSender);
        liquidityLimitsChecker = new LiquidityLimitsChecker(connector);
        dealingLimitChecker = new DealingLimitChecker(connector);
        leaseLimitChecker = new LeaseLimitChecker(connector);
    }
    
    public void run() {
        try {
            logger.info("process started for " + connector.getRunDate());

            List<RunResult> liquidityResults = liquidityLimitsChecker.check();
            logger.info("result for liquidity limits check " + liquidityResults);
            saveRunResults(liquidityResults);
            breachNotifier.sendLiquidityAlert(liquidityResults);

            RunResult overnightResult = dealingLimitChecker.check(DealingLimitType.OVERNIGHT).get(0);
            logger.info("result for overnight limit check " + overnightResult);
            connector.saveRunResult(overnightResult);
            breachNotifier.sendOvernightAlert(overnightResult);

            List<RunResult> overnightDeskResults = dealingLimitChecker.check(DealingLimitType.OVERNIGHT_DESK);
            logger.info("result for overnight desk limit check " + overnightDeskResults);
            saveRunResults(overnightDeskResults);
            breachNotifier.sendDeskAlert(overnightDeskResults);

            List<RunResult> intradayDeskResults = 
            		getTodaysIntradayBreachesSorted (connector.getIntradayBreaches());
            logger.info("result for intraday desk limit check " + intradayDeskResults);
            breachNotifier.sendDeskAlert(intradayDeskResults);

            RunResult leaseResult = leaseLimitChecker.check();
            logger.info("result for lease limit check " + leaseResult);
            connector.saveRunResult(leaseResult);
            breachNotifier.sendLeaseAlert(leaseResult);
            logger.info("process ended");
        } catch (Exception ex) {
            logger.error("error occurred " + ex.getMessage());
            for (StackTraceElement ste : ex.getStackTrace()) {
            	logger.error(ste.toString());
            }
            throw ex;
        }
    }

	private List<RunResult> getTodaysIntradayBreachesSorted(List<RunResult> intradayBreaches) {
		for (int index=intradayBreaches.size()-1; index>= 0; index--) {
			RunResult intradayBreach = intradayBreaches.get(index);
			if (!intradayBreach.getRunTime().toLocalDate().equals(LocalDate.now())) {
				intradayBreaches.remove(index);
			}
		}
		Collections.sort(intradayBreaches, new Comparator<RunResult>() {
			@Override
			public int compare(RunResult o1, RunResult o2) {
				return o1.getRunTime().compareTo(o2.getRunTime());
			}
		});
		return intradayBreaches;
	}

	private void saveRunResults(List<RunResult> liquidityResults) {
		for (RunResult runResult : liquidityResults) {
			connector.saveRunResult(runResult);
		}
	}
}