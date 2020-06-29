package com.matthey.pmm.metal.rentals;

import com.matthey.pmm.metal.rentals.document.CashDealGenerator;
import com.matthey.pmm.metal.rentals.document.InterestBooker;
import com.matthey.pmm.metal.rentals.interest.Interest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.matthey.pmm.metal.rentals.RunResult.Failed;
import static com.matthey.pmm.metal.rentals.RunResult.Skipped;
import static com.matthey.pmm.metal.rentals.RunResult.Successful;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterestBookerTest {

    private final List<Interest> interests = List.of(mock(Interest.class), mock(Interest.class), mock(Interest.class));
    private final CashDeal deal1 = mockCashDeal("cashflow 1", "BU 1", "BU 2", "2020-01-01");
    private final CashDeal deal2 = mockCashDeal("cashflow 2", "BU 2", "BU 1", "2020-01-01");
    private final CashDeal deal3 = mockCashDeal("cashflow 3", "BU 3", "BU 4", "2020-01-02");

    private InterestBooker sut;
    @Mock
    private EndurConnector endurConnector;
    @Mock
    private CashDealGenerator cashDealGenerator;

    private static CashDeal mockCashDeal(String cashflowType, String internalBU, String externalBU, String settleDate) {
        var cashDeal = mock(CashDeal.class, Mockito.RETURNS_MOCKS);
        when(cashDeal.cashflowType()).thenReturn(cashflowType);
        when(cashDeal.internalBU()).thenReturn(internalBU);
        when(cashDeal.externalBU()).thenReturn(externalBU);
        when(cashDeal.settleDate()).thenReturn(settleDate);
        return cashDeal;
    }

    @BeforeEach
    public void setUp() {
        sut = new InterestBooker(endurConnector, cashDealGenerator, 1);
    }

    @Test
    public void generate_cash_deals_from_interests_without_error() {
        when(cashDealGenerator.generate(any(Interest.class), any(Region.class))).thenReturn(deal1, deal2, deal3);
        when(endurConnector.get("/cash_deals", CashDeal[].class)).thenReturn(new CashDeal[]{})
                .thenReturn(new CashDeal[]{deal1, deal2, deal3});
        when(endurConnector.get("/cash_deals/booking_status", Boolean.class)).thenReturn(false);

        var runs = sut.book(interests, Region.NonCN, "2020-04", "test user");

        var captor = ArgumentCaptor.forClass(List.class);
        verify(endurConnector, times(1)).post(eq("/cash_deals"), captor.capture());
        verify(endurConnector, times(2)).get("/cash_deals", CashDeal[].class);
        verify(endurConnector, times(1)).get("/cash_deals/booking_status", Boolean.class);
        verify(endurConnector, times(1)).saveRuns("/runs/cash_deal_booking?user={user}", runs, "test user");
        var dealBooked = captor.getValue();
        assertThat(dealBooked).hasSize(3);
        assertThat(runs).hasSize(3);
        assertThat(runs.stream().map(Run::runTime).collect(toSet())).hasSize(1);
        assertThat(runs.stream().map(Run::statementMonth).collect(toSet())).containsOnly("2020-04");
        assertThat(runs.stream().map(Run::user).collect(toSet())).containsOnly("test user");
        assertThat(runs.stream().map(Run::isSuccessful).collect(toSet())).containsOnly(true);
    }

    @Test
    public void only_generate_cash_deals_not_exist() {
        when(cashDealGenerator.generate(any(Interest.class), any(Region.class))).thenReturn(deal1, deal2, deal3);
        when(endurConnector.get("/cash_deals", CashDeal[].class)).thenReturn(new CashDeal[]{deal1})
                .thenReturn(new CashDeal[]{deal1, deal2, deal3});
        when(endurConnector.get("/cash_deals/booking_status", Boolean.class)).thenReturn(false);

        var runs = sut.book(interests, Region.NonCN, "", "");

        var captor = ArgumentCaptor.forClass(List.class);
        verify(endurConnector, times(1)).post(eq("/cash_deals"), captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(runs).extracting(Run::result).containsExactlyInAnyOrder(Successful, Successful, Skipped);
    }

    @Test
    public void one_cash_deal_not_generated() {
        when(cashDealGenerator.generate(any(Interest.class), any(Region.class))).thenReturn(deal1, deal2, deal3);
        when(endurConnector.get("/cash_deals", CashDeal[].class)).thenReturn(new CashDeal[]{})
                .thenReturn(new CashDeal[]{deal1, deal2});
        when(endurConnector.get("/cash_deals/booking_status", Boolean.class)).thenReturn(false);

        var runs = sut.book(interests, Region.NonCN, "", "");

        assertThat(runs).extracting(Run::result).containsExactlyInAnyOrder(Successful, Successful, Failed);
    }

    @Test
    public void cannot_get_existing_deals() {
        var exception = new RuntimeException("test");
        when(endurConnector.get("/cash_deals", CashDeal[].class)).thenThrow(exception);

        assertThatThrownBy(() -> sut.book(interests, Region.NonCN, "", "")).isEqualTo(exception);
    }

    @Test
    public void cannot_send_booking_request() {
        when(cashDealGenerator.generate(any(Interest.class), any(Region.class))).thenReturn(deal1, deal2, deal3);
        when(endurConnector.get("/cash_deals", CashDeal[].class)).thenReturn(new CashDeal[]{})
                .thenReturn(new CashDeal[]{deal1, deal2});
        var exception = new RuntimeException("test");
        doThrow(exception).when(endurConnector).post(eq("/cash_deals"), any());

        assertThatThrownBy(() -> sut.book(interests, Region.NonCN, "", "")).isEqualTo(exception);
    }

    @Test
    public void cannot_check_booking_status() {
        when(cashDealGenerator.generate(any(Interest.class), any(Region.class))).thenReturn(deal1, deal2, deal3);
        when(endurConnector.get("/cash_deals", CashDeal[].class)).thenReturn(new CashDeal[]{})
                .thenReturn(new CashDeal[]{deal1, deal2});
        var exception = new RuntimeException("test");
        when(endurConnector.get("/cash_deals/booking_status", Boolean.class)).thenThrow(exception);

        assertThatThrownBy(() -> sut.book(interests, Region.NonCN, "", "")).isEqualTo(exception);
    }

    @Test
    public void cannot_get_existing_deals_after_booked() {
        when(cashDealGenerator.generate(any(Interest.class), any(Region.class))).thenReturn(deal1, deal2, deal3);
        var exception = new RuntimeException("test");
        when(endurConnector.get("/cash_deals", CashDeal[].class)).thenReturn(new CashDeal[]{}).thenThrow(exception);
        when(endurConnector.get("/cash_deals/booking_status", Boolean.class)).thenReturn(false);

        assertThatThrownBy(() -> sut.book(interests, Region.NonCN, "", "")).isEqualTo(exception);
    }

    @Test
    public void error_occurred_when_generating_cash_deal() {
        var exception = new RuntimeException("test");
        when(cashDealGenerator.generate(any(Interest.class), any(Region.class))).thenThrow(exception);
        when(endurConnector.get("/cash_deals", CashDeal[].class)).thenReturn(new CashDeal[]{});

        assertThatThrownBy(() -> sut.book(interests, Region.NonCN, "", "")).isEqualTo(exception);
    }

    @Test
    public void generate_invoices_successfully() {
        when(endurConnector.get("/invoices/generating_status/NonCN", Boolean.class)).thenReturn(false);
        when(endurConnector.get("/invoices/complete_status", Boolean.class)).thenReturn(true);
        assertThat(sut.generateInvoices(Region.NonCN)).isTrue();
    }

    @Test
    public void some_events_not_processed_when_generate_invoices() {
        when(endurConnector.get("/invoices/generating_status/NonCN", Boolean.class)).thenReturn(false);
        when(endurConnector.get("/invoices/complete_status/NonCN", Boolean.class)).thenReturn(false);
        assertThat(sut.generateInvoices(Region.NonCN)).isFalse();
    }

    @Test
    public void cannot_post_request() {
        doThrow(RuntimeException.class).when(endurConnector).post("/invoices/NonCN", null);
        assertThat(sut.generateInvoices(Region.NonCN)).isFalse();
    }

    @Test
    public void cannot_check_generating_status() {
        when(endurConnector.get("/invoices/generating_status/NonCN", Boolean.class)).thenThrow(RuntimeException.class);
        assertThat(sut.generateInvoices(Region.NonCN)).isFalse();
    }

    @Test
    public void cannot_get_remaining_events() {
        when(endurConnector.get("/invoices/generating_status/NonCN", Boolean.class)).thenReturn(false);
        when(endurConnector.get("/invoices/complete_status/NonCN", Boolean.class)).thenThrow(RuntimeException.class);
        assertThat(sut.generateInvoices(Region.NonCN)).isFalse();
    }
}