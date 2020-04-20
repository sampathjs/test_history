package com.matthey.pmm.limits.reporting.translated;

import java.util.ArrayList;
import java.util.List;

import com.openlink.util.logging.PluginLog;

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
        	PluginLog.info("process started for" + connector.getRunDate());
            List<RunResult> breaches = new ArrayList<>(connector.getEodBreaches());
            breaches.addAll(connector.getIntradayBreaches());
            for (int index = breaches.size()-1; index >= 0; index--) {
            	if (breaches.get(index).getRunTime().isAfter(connector.getRunDate().minusDays(7))) {
            		breaches.remove(index);
            	}
            }            
            PluginLog.info("breaches: " + breaches);
            breachNotifier.sendAlertSummary(breaches);

            PluginLog.info("process ended");
        } catch (Exception e) {
            PluginLog.error("error occurred :" + e.toString());
            for (StackTraceElement ste : e.getStackTrace()) {
            	PluginLog.error(ste.toString());
            }            
            throw e;
        }
    }

}
