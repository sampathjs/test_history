package com.matthey.pmm.metal.rentals.document;

import com.matthey.pmm.metal.rentals.EndurConnector;
import com.matthey.pmm.metal.rentals.ImmutableStatementEmailingRun;
import com.matthey.pmm.metal.rentals.PartyContact;
import com.matthey.pmm.metal.rentals.StatementEmailingRun;
import com.matthey.pmm.metal.rentals.StatementGeneratingRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

import static com.matthey.pmm.metal.rentals.RunResult.Failed;
import static com.matthey.pmm.metal.rentals.RunResult.Successful;
import static com.rainerhahnekamp.sneakythrow.Sneaky.sneak;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

@Component
public class StatementSender {

    private static final Logger logger = LoggerFactory.getLogger(StatementSender.class);

    private final String emailTemplate;
    private final EmailSender emailSender;
    private final EndurConnector endurConnector;

    public StatementSender(@Value("${statement.email.template}") Resource emailTemplate,
                           EmailSender emailSender,
                           EndurConnector endurConnector) {
        this.emailTemplate = sneak(() -> StreamUtils.copyToString(emailTemplate.getInputStream(), UTF_8));
        this.emailSender = emailSender;
        this.endurConnector = endurConnector;
    }

    public List<StatementEmailingRun> sendStatements(Map<String, Set<PartyContact>> partyContacts,
                                                     List<StatementGeneratingRun> statementGeneratingRuns,
                                                     String statementMonth,
                                                     String user) {
        ForkJoinPool customThreadPool = new ForkJoinPool(33);
        var runTime = LocalDateTime.now(ZoneOffset.UTC).toString();
        var runs = sneak(() -> customThreadPool.submit(() -> statementGeneratingRuns.parallelStream()
                .map(statementGeneratingRun -> partyContacts.get(statementGeneratingRun.party())
                        .stream()
                        .filter(partyContact -> isEmailValid(partyContact.email()))
                        .map(email -> send(email, statementGeneratingRun, runTime, statementMonth, user))
                        .collect(toList()))
                .flatMap(List::stream)
                .collect(toList())).get());
        endurConnector.saveRuns("/runs/statement_emailing?user={user}", runs, user);
        return runs;
    }

    private boolean isEmailValid(String email) {
        return !(email.isBlank() || email.equals("XXXXXX@dummy.com"));
    }

    private StatementEmailingRun send(PartyContact partyContact,
                                      StatementGeneratingRun statementGeneratingRun,
                                      String runTime,
                                      String statementMonth,
                                      String user) {
        var successfulRun = ImmutableStatementEmailingRun.builder()
                .user(user)
                .runTime(runTime)
                .statementMonth(statementMonth)
                .result(Successful)
                .partyContact(partyContact)
                .statementPath(statementGeneratingRun.statementPath())
                .build();
        try {
            emailSender.send("Metals Utilisation Statement: " + statementGeneratingRun.accountGroup(),
                             emailTemplate,
                             statementGeneratingRun.statementPath(),
                             partyContact.email());
            return successfulRun;
        } catch (Exception e) {
            logger.error("error occurred when sending email to {} with attachment {}: {}",
                         partyContact,
                         statementGeneratingRun.statementPath(),
                         e.getMessage(),
                         e);
            return successfulRun.withResult(Failed);
        }
    }
}
