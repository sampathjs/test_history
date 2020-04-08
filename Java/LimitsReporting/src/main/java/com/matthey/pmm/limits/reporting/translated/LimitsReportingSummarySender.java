package com.matthey.pmm.limits.reporting.translated;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LimitsReportingSummarySender {
    private static final Logger logger = LoggerFactory.getLogger(LimitsReportingSummarySender.class);
	private final LimitsReportingConnector connector;
	private final EmailSender emailSender;
    private final BreachNotifier breachNotifier;
        
	public LimitsReportingSummarySender (LimitsReportingConnector connector,
			final EmailSender emailSender) {
		this.connector = connector;
		this.emailSender = emailSender;
		this.breachNotifier = new BreachNotifier(connector, emailSender);
	}
	
    public void run() {
        try {
            logger.info("process started for" + connector.getRunDate());
            List<RunResult> breaches = new ArrayList<>(connector.getEodBreaches());
            breaches.addAll(connector.getIntradayBreaches());
            for (int index = breaches.size()-1; index >= 0; index--) {
            	if (breaches.get(index).getRunTime().isAfter(connector.getRunDate().minusDays(7)));
            }            
            logger.info("breaches: " + breaches);
            breachNotifier.sendAlertSummary(breaches);

            logger.info("process ended");
        } catch (Exception e) {
            logger.error("error occurred ${e.message}", e);
            for (StackTraceElement ste : e.getStackTrace()) {
            	logger.error(ste.toString());
            }            
            throw e;
        }
    }

}
