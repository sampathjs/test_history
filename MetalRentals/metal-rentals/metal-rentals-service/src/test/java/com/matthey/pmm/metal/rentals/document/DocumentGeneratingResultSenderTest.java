package com.matthey.pmm.metal.rentals.document;

import com.matthey.pmm.metal.rentals.EndurConnector;
import com.matthey.pmm.metal.rentals.ImmutableStatementEmailingRun;
import com.matthey.pmm.metal.rentals.PartyContact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

import static com.matthey.pmm.metal.rentals.RunResult.Failed;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentGeneratingResultSenderTest {

    @Mock
    EmailSender emailSender;

    @Mock
    EndurConnector endurConnector;
    DocumentGeneratingResult result = ImmutableDocumentGeneratingResult.builder()
            .addStatementEmailingRuns(ImmutableStatementEmailingRun.builder()
                                              .user("")
                                              .runTime("")
                                              .statementMonth("")
                                              .result(Failed)
                                              .partyContact(mock(PartyContact.class))
                                              .statementPath("")
                                              .build())
            .build();
    @Mock
    private Resource emailTemplate;

    @Test
    public void send_to_user_and_support() {
        when(endurConnector.get("/support_emails", String[].class)).thenReturn(new String[]{"test1@email.com",
                                                                                            "test2@email.com"});
        var sut = new DocumentGeneratingResultSender(emailSender, endurConnector, emailTemplate);
        sut.send(result, "user@email.com");

        verify(emailSender, times(1)).send(any(), any(), isNull(), eq("user@email.com"));
        verify(emailSender, times(1)).send(any(), any(), isNull(), eq("test1@email.com"));
        verify(emailSender, times(1)).send(any(), any(), isNull(), eq("test2@email.com"));
    }

    @Test
    public void still_send_to_others_even_one_fail() {
        when(endurConnector.get("/support_emails", String[].class)).thenReturn(new String[]{"test1@email.com",
                                                                                            "test2@email.com"});
        doThrow(RuntimeException.class).when(emailSender).send(any(), any(), isNull(), eq("test1@email.com"));

        var sut = new DocumentGeneratingResultSender(emailSender, endurConnector, emailTemplate);
        sut.send(result, "user@email.com");

        verify(emailSender, times(1)).send(any(), any(), isNull(), eq("user@email.com"));
        verify(emailSender, times(1)).send(any(), any(), isNull(), eq("test2@email.com"));
    }
}