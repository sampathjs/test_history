package com.matthey.pmm.limits.reporting.translated;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

import org.joda.time.LocalDateTime;

import com.olf.jm.logging.Logging;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class BreachNotifier {
	private final LimitsReportingConnector connector;
	private final EmailSender emailSender;
	
	private final String runDate;
	private final Configuration freemarkerConfig;
	
	public BreachNotifier (final LimitsReportingConnector connector, 
			final EmailSender emailSender, String templateDir) {
		this.connector = connector;
		this.emailSender = emailSender;		
		
		runDate = RunResult.dateFormat.print(connector.getRunDate());
		freemarkerConfig = new Configuration(Configuration.VERSION_2_3_30);

		try {
			ClassTemplateLoader classLoader = new ClassTemplateLoader(BreachNotifier.class, "/email-templates");
			FileTemplateLoader fileLoader = new FileTemplateLoader(new File(templateDir));
			MultiTemplateLoader multiTemplateLoader = new MultiTemplateLoader(
					new TemplateLoader[] { classLoader, fileLoader });
			freemarkerConfig.setTemplateLoader(multiTemplateLoader);
		}  catch (Exception ex) {
			throw new RuntimeException ("Error accessing template folder");
		}
		
//        freemarkerConfig.setClassForTemplateLoading(BreachNotifier.class, "/email-templates");
        freemarkerConfig.setDefaultEncoding("UTF-8");
        freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        freemarkerConfig.setLogTemplateExceptions(false);
        freemarkerConfig.setWrapUncheckedExceptions(true);
        freemarkerConfig.setFallbackOnNullLoopVariable(false);		
	}
	
	public void sendOvernightAlert(final RunResult result) {
		if (!result.getRunType().equals("Overnight")) {
			throw new RuntimeException ("the run type of the result should be overnight");
		}
        if (!result.isBreach()) {
        	return;
        } 
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("runDate", runDate);
        parameters.put("result", result);
        send("Dealing", "Overnight Breach", parameters);
    }

	public void sendDeskAlert(List<RunResult> results) {
		boolean anyBreach=false;
		String runType="";
		for (RunResult result : results) {
			anyBreach |= result.isBreach();
			runType = result.getRunType();
		}
		if (!anyBreach) {
			return;
		}
		if (!runType.endsWith("Desk") ) {
			throw new RuntimeException ("the run type of the result should end with Desk but was: " + runType);
		}
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("runDate", runDate);
        parameters.put("results", results);
		
		send("Dealing", runType + " Breaches", parameters);
    }

    public void sendLeaseAlert(RunResult result) {
    	if (!result.getRunType().equals("Lease")) {
    		throw new RuntimeException ("the run type of the result should be lease");
    	}
        if (!result.isBreach()) {
        	return;
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("runDate", runDate);
        parameters.put("result", result);
        
        send("Lease", "Lease Breach", parameters);
    }

    public void sendLiquidityAlert(List<RunResult> results) {
    	boolean anyBreach=false;    	
    	for (RunResult result : results) {
    		if (!result.getRunType().equals("Liquidity")) {
    			throw new RuntimeException ("the run type of the result should be liquidity");
    		}
    		anyBreach |= result.isBreach();
    	}
        if (!anyBreach) {
        	return;
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("runDate", runDate);
        parameters.put("results", results);
        
        send("Liquidity", "Liquidity Breaches", parameters);
    }

    
    public void sendAlertSummary(List<RunResult> allBreaches) {
        if (allBreaches.isEmpty()) {
        	return;
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("breachGroups", groupBreaches(allBreaches));
        send("Summary", "Breach Weekly Summary", parameters);
    }

    private List<BreachGroup> groupBreaches(List<RunResult> allBreaches) {
    	List<BreachGroup> breachGroups = new ArrayList<>(allBreaches.size());
    	Map<String, List<RunResult>> runResultsPerBreachType = new HashMap<>();

    	// create sorting by breach type and all breaches for a certain
    	// breach type are sorted by run time 
    	for (RunResult result : allBreaches) {
    		String breachType = getBreachType (result);
    		List<RunResult> resultsForBreachType = runResultsPerBreachType.get(breachType);
    		if (resultsForBreachType == null) {
    			resultsForBreachType = new ArrayList<>();
    			runResultsPerBreachType.put(breachType, resultsForBreachType);
    		}
    		resultsForBreachType.add(result);
    		Collections.sort(resultsForBreachType, new Comparator<RunResult> () {
				@Override
				public int compare(RunResult left, RunResult right) {
					return left.getRunTime().compareTo(right.getRunTime());
				}
    		});
    	}
    	for (Entry<String, List<RunResult>> entry : runResultsPerBreachType.entrySet()) {
    		breachGroups.add(new BreachGroup(entry.getKey(), entry.getValue()));
    	}
    	Collections.sort (breachGroups, new Comparator<BreachGroup> () {
			@Override
			public int compare(BreachGroup left, BreachGroup right) {
				return left.getBreachType().compareTo(right.getBreachType());
			}
    	});
    	return breachGroups;
    }

    private String getBreachType(RunResult result) {
        StringBuilder breachType = new StringBuilder(result.getRunType());
        if (result.getRunType().equals("Liquidity")) {
        	breachType.append(result.getLiquidityBreachLimit());
        }
        if (result.getRunType().endsWith("Desk")) {
            if (breachType.length() > 0) {
            	breachType.append(' ');
            }
        	breachType.append(result.getDesk());
        }
        if (result.getMetal() != null && result.getMetal().trim().length() >0 ) {
            if (breachType.length() > 0) {
            	breachType.append(' ');
            }
        	breachType.append(result.getMetal());
        }
        return breachType.toString();
    }

    private void send(String functionalGroup, String title, Map<String, Object> parameters) {
    	try {
            Set<String> emails = connector.getEmails(functionalGroup);
            Logging.info("email addresses to be sent for " + functionalGroup + ": " + emails);
            if (emails.isEmpty()) {
            	return;
            }
            String templateName = title.toLowerCase().replace(" ", "-") + ".ftl";
            Template template = freemarkerConfig.getTemplate(templateName);
            StringWriter content = new StringWriter();
            parameters.put("currentDate", LocalDateTime.now());
            parameters.put("dateSeparator", RunResult.DATE_SEPARATOR);
            parameters.put("datePattern", RunResult.DATE_PATTERN);
            template.process(parameters, content);

            EmailSender.send(title + " " + runDate, content.toString(), emails);
    	} catch (Exception e) {
    		Logging.error("Error sending email: " + e.getMessage());
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error(ste.toString());
			}
		}
    }
}

