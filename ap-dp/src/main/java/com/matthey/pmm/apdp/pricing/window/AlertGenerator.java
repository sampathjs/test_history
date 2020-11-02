package com.matthey.pmm.apdp.pricing.window;

import com.google.common.collect.ImmutableMap;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class AlertGenerator {
    
    private final Template template;
    private final BiConsumer<String, String> emailSender;
    
    public AlertGenerator(BiConsumer<String, String> emailSender) {
        this.emailSender = emailSender;
        
        Configuration freemarkerConfig = new Configuration(Configuration.VERSION_2_3_30);
        freemarkerConfig.setClassForTemplateLoading(this.getClass(), "/email-templates");
        freemarkerConfig.setDefaultEncoding("UTF-8");
        freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        freemarkerConfig.setLogTemplateExceptions(false);
        freemarkerConfig.setWrapUncheckedExceptions(true);
        freemarkerConfig.setFallbackOnNullLoopVariable(false);
        try {
            template = freemarkerConfig.getTemplate("pricing-window-alert.ftl");
        } catch (Exception e) {
            throw new RuntimeException("failed to load the alert email template", e);
        }
    }
    
    public void generateAndSendAlert(LocalDate runDate, LocalDateTime timestamp, Set<CheckResult> results) {
        if (results.isEmpty()) {
            return;
        }
        
        StringWriter htmlContent = new StringWriter();
        Map<String, Object> dataModel = ImmutableMap.of("runDate",
                                                        runDate,
                                                        "timestamp",
                                                        timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                                                        "results",
                                                        results);
        try {
            template.process(dataModel, htmlContent);
        } catch (Exception e) {
            throw new RuntimeException("error occurred when generating the alert content", e);
        }
        emailSender.accept("AP DP Deals Expiring " + runDate, htmlContent.toString());
    }
}
