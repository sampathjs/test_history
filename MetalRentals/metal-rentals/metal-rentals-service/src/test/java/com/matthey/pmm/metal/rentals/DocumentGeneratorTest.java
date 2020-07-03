package com.matthey.pmm.metal.rentals;

import com.matthey.pmm.metal.rentals.data.DataCache;
import com.matthey.pmm.metal.rentals.data.StatementPeriods;
import com.matthey.pmm.metal.rentals.document.DocumentGenerator;
import com.matthey.pmm.metal.rentals.document.InterestBooker;
import com.matthey.pmm.metal.rentals.document.StatementGenerator;
import com.matthey.pmm.metal.rentals.document.StatementSender;
import com.matthey.pmm.metal.rentals.interest.ImmutableInterest;
import com.matthey.pmm.metal.rentals.interest.Interest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentGeneratorTest {

    @Mock
    InterestBooker interestBooker;

    @Mock
    StatementGenerator statementGenerator;

    @Mock
    StatementSender statementSender;

    @Mock
    DataCache dataCache;

    @BeforeEach
    public void setUp() {
        when(dataCache.getStatementPeriods()).thenReturn(new StatementPeriods(LocalDate.now()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void filter_interests_for_both_statements_and_deal_booking() {
        when(statementGenerator.generate(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(statementSender.sendStatements(any(), any(), any(), any())).thenReturn(List.of());
        when(interestBooker.book(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(interestBooker.generateInvoices(any())).thenReturn(true);
        var interests = Map.of("group1",
                               List.of(genInterest(0.01), genInterest(0.0001)),
                               "group2",
                               List.of(genInterest(-0.01), genInterest(0.0001)));

        var sut = new DocumentGenerator(dataCache, interestBooker, statementGenerator, statementSender);
        var result = sut.generateDocuments(interests, true, true, Region.NonCN, "");

        var captor1 = ArgumentCaptor.forClass(List.class);
        verify(interestBooker, times(1)).book(captor1.capture(), any(), any(), any(), any());
        assertThat(captor1.getValue()).containsOnly(genInterest(0.01), genInterest(-0.01));
        var captor2 = ArgumentCaptor.forClass(Map.class);
        verify(statementGenerator, times(1)).generate(captor2.capture(), any(), any(), anyString(), anyString());
        var actual = captor2.getValue();
        assertThat((List<Interest>) actual.get("group1")).containsOnly(genInterest(0.01));
        assertThat((List<Interest>) actual.get("group2")).containsOnly(genInterest(-0.01));
        verify(statementSender, times(1)).sendStatements(any(), any(), anyString(), anyString());
        assertThat(result.isEverythingOk()).isTrue();
    }

    @Test
    public void generate_statements_only() {
        when(statementGenerator.generate(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(statementSender.sendStatements(any(), any(), any(), any())).thenReturn(List.of());
        var interests = Map.of("group1", List.of(genInterest(0.01)), "group2", List.of(genInterest(-0.01)));

        var sut = new DocumentGenerator(dataCache, interestBooker, statementGenerator, statementSender);
        var result = sut.generateDocuments(interests, true, false, Region.NonCN, "");

        verify(interestBooker, never()).book(any(), any(), any(), any(), any());
        verify(statementGenerator, times(1)).generate(any(), any(), any(), any(), any());
        assertThat(result.isEverythingOk()).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void only_send_successful_statements() {
        var run = mock(StatementGeneratingRun.class);
        when(run.isSuccessful()).thenReturn(false);
        when(statementGenerator.generate(any(), any(), any(), any(), any())).thenReturn(List.of(run));
        var interests = Map.of("group1", List.of(genInterest(0.01)), "group2", List.of(genInterest(-0.01)));
        var sut = new DocumentGenerator(dataCache, interestBooker, statementGenerator, statementSender);
        sut.generateDocuments(interests, true, false, Region.NonCN, "");

        var captor = ArgumentCaptor.forClass(List.class);
        verify(statementSender, times(1)).sendStatements(any(), captor.capture(), any(), any());
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    public void book_deal_only() {
        when(interestBooker.book(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(interestBooker.generateInvoices(any())).thenReturn(true);
        var interests = Map.of("group1", List.of(genInterest(0.01)), "group2", List.of(genInterest(-0.01)));

        var sut = new DocumentGenerator(dataCache, interestBooker, statementGenerator, statementSender);
        var result = sut.generateDocuments(interests, false, true, Region.NonCN, "");

        verify(interestBooker, times(1)).book(any(), any(), any(), any(), any());
        verify(statementGenerator, never()).generate(any(), any(), any(), any(), any());
        assertThat(result.isEverythingOk()).isTrue();
    }

    @Test
    public void cannot_generate_statements() {
        when(statementGenerator.generate(any(), any(), any(), any(), any())).thenThrow(RuntimeException.class);
        var interests = Map.of("group1", List.of(genInterest(0.01)), "group2", List.of(genInterest(-0.01)));

        var sut = new DocumentGenerator(dataCache, interestBooker, statementGenerator, statementSender);

        var result = sut.generateDocuments(interests, true, true, Region.NonCN, "");
        assertThat(result.isStatementGeneratingOk()).isFalse();
        assertThat(result.isStatementEmailingOk()).isFalse();
        assertThat(result.isCashDealBookingOk()).isFalse();
        assertThat(result.isInvoiceGeneratingOk()).isFalse();
    }

    @Test
    public void cannot_email_statements() {
        when(statementSender.sendStatements(any(), any(), any(), any())).thenThrow(RuntimeException.class);
        var interests = Map.of("group1", List.of(genInterest(0.01)), "group2", List.of(genInterest(-0.01)));

        var sut = new DocumentGenerator(dataCache, interestBooker, statementGenerator, statementSender);

        var result = sut.generateDocuments(interests, true, true, Region.NonCN, "");
        assertThat(result.isStatementGeneratingOk()).isTrue();
        assertThat(result.isStatementEmailingOk()).isFalse();
        assertThat(result.isCashDealBookingOk()).isFalse();
        assertThat(result.isInvoiceGeneratingOk()).isFalse();
    }

    @Test
    public void cannot_book_deals() {
        when(interestBooker.book(any(), any(), any(), any(), any())).thenThrow(RuntimeException.class);
        var interests = Map.of("group1", List.of(genInterest(0.01)), "group2", List.of(genInterest(-0.01)));

        var sut = new DocumentGenerator(dataCache, interestBooker, statementGenerator, statementSender);

        var result = sut.generateDocuments(interests, true, true, Region.NonCN, "");
        assertThat(result.isStatementGeneratingOk()).isTrue();
        assertThat(result.isStatementEmailingOk()).isTrue();
        assertThat(result.isCashDealBookingOk()).isFalse();
        assertThat(result.isInvoiceGeneratingOk()).isFalse();
    }

    @Test
    public void cannot_generate_invoices() {
        when(interestBooker.generateInvoices(any())).thenThrow(RuntimeException.class);
        var interests = Map.of("group1", List.of(genInterest(0.01)), "group2", List.of(genInterest(-0.01)));

        var sut = new DocumentGenerator(dataCache, interestBooker, statementGenerator, statementSender);

        var result = sut.generateDocuments(interests, true, true, Region.NonCN, "");
        assertThat(result.isStatementGeneratingOk()).isTrue();
        assertThat(result.isStatementEmailingOk()).isTrue();
        assertThat(result.isCashDealBookingOk()).isTrue();
        assertThat(result.isInvoiceGeneratingOk()).isFalse();
    }

    private Interest genInterest(double interestRate) {
        return ImmutableInterest.builder()
                .group("Test")
                .account("Test")
                .metal("XAU")
                .unit("TOz")
                .currency("USD")
                .averageBalanceInTOz(100d)
                .averageBalance(100d)
                .averagePriceForTOz(100d)
                .interestRate(interestRate)
                .numOfDays(31)
                .daysOfYear(365)
                .owner("owner")
                .holder("holder")
                .build();
    }
}
