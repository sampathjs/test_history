package com.matthey.pmm.metal.transfers.document;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import com.matthey.pmm.metal.transfers.EndurConnector;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.matthey.pmm.metal.transfers.RunResult.Successful;
import static com.rainerhahnekamp.sneakythrow.Sneaky.sneak;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toSet;

@Component
public class DocumentGeneratingResultSender {

    private static final Logger logger = LoggerFactory.getLogger(DocumentGeneratingResultSender.class);

    private final EmailSender emailSender;
    private final EndurConnector endurConnector;
    private final String emailTemplate;

    public DocumentGeneratingResultSender(EmailSender emailSender,
                                          EndurConnector endurConnector,
                                          @Value("${summary.email.template}") Resource emailTemplate) {
        this.emailSender = emailSender;
        this.endurConnector = endurConnector;
        this.emailTemplate = sneak(() -> StreamUtils.copyToString(emailTemplate.getInputStream(), UTF_8));
    }

    public void send(DocumentGeneratingResult result, String userEmail) {
        var supportEmails = endurConnector.get("/support_emails", String[].class);
        var emails = Stream.of(ArrayUtils.add(supportEmails, userEmail)).collect(toSet());
        String summary = (result.isEverythingOk()
                          ? "All the steps are finished successful"
                          : "There are issues during the procedure, please contact support for details: " +
                            System.lineSeparator() +
                            formatResult("Statement Generating", result.isStatementGeneratingOk()) +
                            formatResult("Statement Emailing", result.isStatementEmailingOk()) +
                            formatResult("Cash Deal Booking", result.isCashDealBookingOk()) +
                            formatResult("Invoice Generating", result.isMetalRentalsWorkflowOk())) +
                         System.lineSeparator() +
                         "For the result of accounting postings generation, please check corresponding tasks";
        Map<String, String> variables = new HashMap<>();
        variables.put("summary", summary);
        String content = new StringSubstitutor(variables).replace(emailTemplate);
        emails.forEach(email -> this.send(content, email));
    }

    private String formatResult(String title, boolean result) {
        return System.lineSeparator() + title + ": " + (result ? Successful : "Need Attention");
    }

    private void send(String content, String email) {
        try {
            emailSender.send("Metal Rentals Summary", content, null, email);
        } catch (Exception e) {
            logger.error("error occurred during sending email to {}", email, e);
        }
    }
}
