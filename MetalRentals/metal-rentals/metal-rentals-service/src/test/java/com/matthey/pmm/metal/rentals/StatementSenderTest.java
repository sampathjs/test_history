package com.matthey.pmm.metal.rentals;

import com.matthey.pmm.metal.rentals.document.EmailSender;
import com.matthey.pmm.metal.rentals.document.StatementSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.matthey.pmm.metal.rentals.RunResult.Failed;
import static com.matthey.pmm.metal.rentals.RunResult.Successful;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StatementSenderTest {

    private StatementSender sut;

    @Mock
    private EndurConnector endurConnector;

    @Mock
    private Resource emailTemplate;

    @Mock
    private EmailSender emailSender;

    @BeforeEach
    public void setUp() {
        sut = new StatementSender(emailTemplate, emailSender, endurConnector);
    }

    @Test
    public void send_emails_to_all_contacts() {
        var partyContacts = Map.of("party1",
                                   Set.of(genPartyContact("party1", "email1"), genPartyContact("party1", "email2")),
                                   "party2",
                                   Set.of(genPartyContact("party2", "email3")));
        List<StatementGeneratingRun> statementPaths = List.of(ImmutableStatementGeneratingRun.builder()
                                                                      .user("")
                                                                      .runTime("")
                                                                      .statementMonth("")
                                                                      .result(Successful)
                                                                      .party("party1")
                                                                      .accountGroup("account group 1")
                                                                      .statementPath("path1")
                                                                      .build(),
                                                              ImmutableStatementGeneratingRun.builder()
                                                                      .user("")
                                                                      .runTime("")
                                                                      .statementMonth("")
                                                                      .result(Successful)
                                                                      .party("party2")
                                                                      .accountGroup("account group 2")
                                                                      .statementPath("path2")
                                                                      .build());

        var runs = sut.sendStatements(partyContacts, statementPaths, "2020-04", "test user");

        verify(emailSender, times(1)).send(eq("Metals Utilisation Statement: account group 1"),
                                           anyString(),
                                           eq("path1"),
                                           eq("email1"));
        verify(emailSender, times(1)).send(eq("Metals Utilisation Statement: account group 1"),
                                           anyString(),
                                           eq("path1"),
                                           eq("email2"));
        verify(emailSender, times(1)).send(eq("Metals Utilisation Statement: account group 2"),
                                           anyString(),
                                           eq("path2"),
                                           eq("email3"));
        verify(endurConnector, times(1)).saveRuns("/runs/statement_emailing?user={user}", runs, "test user");
        assertThat(runs).hasSize(3);
        assertThat(runs.stream().map(Run::runTime).collect(toSet())).hasSize(1);
        assertThat(runs.stream().map(Run::statementMonth).collect(toSet())).containsOnly("2020-04");
        assertThat(runs.stream().map(Run::user).collect(toSet())).containsOnly("test user");
        assertThat(runs.stream().map(Run::isSuccessful).collect(toSet())).containsOnly(true);
    }

    @Test
    public void ignore_empty_emails() {
        var partyContacts = Map.of("party",
                                   Set.of(genPartyContact("party", ""), genPartyContact("party", "XXXXXX@dummy.com")));
        List<StatementGeneratingRun> statementPaths = List.of(ImmutableStatementGeneratingRun.builder()
                                                                      .user("")
                                                                      .runTime("")
                                                                      .statementMonth("")
                                                                      .result(Successful)
                                                                      .party("party")
                                                                      .accountGroup("")
                                                                      .statementPath("path")
                                                                      .build());

        var runs = sut.sendStatements(partyContacts, statementPaths, "", "");

        verify(emailSender, never()).send(anyString(), anyString(), anyString(), anyString());
        assertThat(runs).isEmpty();
    }

    @Test
    public void send_other_emails_even_one_fails() {
        var partyContacts = Map.of("party1",
                                   Set.of(genPartyContact("party1", "email1")),
                                   "party2",
                                   Set.of(genPartyContact("party2", "email2")));
        List<StatementGeneratingRun> statementPaths = List.of(ImmutableStatementGeneratingRun.builder()
                                                                      .user("")
                                                                      .runTime("")
                                                                      .statementMonth("")
                                                                      .result(Successful)
                                                                      .party("party1")
                                                                      .accountGroup("")
                                                                      .statementPath("path1")
                                                                      .build(),
                                                              ImmutableStatementGeneratingRun.builder()
                                                                      .user("")
                                                                      .runTime("")
                                                                      .statementMonth("")
                                                                      .result(Successful)
                                                                      .party("party2")
                                                                      .accountGroup("")
                                                                      .statementPath("path2")
                                                                      .build());
        lenient().doThrow(RuntimeException.class)
                .when(emailSender)
                .send(anyString(), anyString(), eq("path1"), eq("email1"));

        var runs = sut.sendStatements(partyContacts, statementPaths, "", "");

        verify(emailSender, times(1)).send(anyString(), anyString(), eq("path2"), eq("email2"));
        assertThat(runs).extracting(Run::result).containsExactlyInAnyOrder(Successful, Failed);
    }

    private PartyContact genPartyContact(String party, String email) {
        return ImmutablePartyContact.builder().party(party).contact("").email(email).build();
    }
}