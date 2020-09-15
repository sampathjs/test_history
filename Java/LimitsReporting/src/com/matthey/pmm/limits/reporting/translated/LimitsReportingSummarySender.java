package com.matthey.pmm.limits.reporting.translated;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDateTime;

import com.olf.jm.logging.Logging;

public class LimitsReportingSummarySender {
	private final LimitsReportingConnector connector;
	private final EmailSender emailSender;
    private final BreachNotifier breachNotifier;
        
	public LimitsReportingSummarySender (LimitsReportingConnector connector,
			final EmailSender emailSender, String abOutdir) {
		this.connector = connector;
		this.emailSender = emailSender;
		this.breachNotifier = new BreachNotifier(connector, emailSender, abOutdir);
	}
	
    public void run() {
        try {
        	Logging.info("process started for" + connector.getRunDate());
            List<RunResult> breaches = new ArrayList<>(connector.getEodBreaches());
            breaches.addAll(connector.getIntradayBreaches());
            LocalDateTime start = connector.getRunDate().minusDays(7).withMillisOfDay(0);
            for (int index = breaches.size()-1; index >= 0; index--) {
            	if (!(breaches.get(index).getRunTime().isAfter(start))) {
            		breaches.remove(index);
            	}
            }            
            Logging.info("breaches: " + breaches);
            breachNotifier.sendAlertSummary(breaches);

            Logging.info("process ended");
        } catch (Exception e) {
            Logging.error("error occurred :" + e.toString());
            for (StackTraceElement ste : e.getStackTrace()) {
            	Logging.error(ste.toString());
            }            
            throw e;
        }
    }

}
