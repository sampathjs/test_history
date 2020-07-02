package com.matthey.pmm.metal.rentals;

import com.matthey.pmm.metal.rentals.data.DataCache;
import com.matthey.pmm.metal.rentals.data.StatementPeriods;
import com.matthey.pmm.metal.rentals.document.CashDealGenerator;
import com.matthey.pmm.metal.rentals.interest.ImmutableInterest;
import com.matthey.pmm.metal.rentals.interest.Interest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashDealGeneratorTest {

    @Mock(answer = RETURNS_DEEP_STUBS)
    DataCache dataCache;

    @Test
    public void generate_deal_for_cn() {
        when(dataCache.getFeePortfolios()).thenReturn(Map.of("JM PMM CN", "CN Fees"));
        when(dataCache.getStatementPeriods()).thenReturn(new StatementPeriods(LocalDate.of(2020, 4, 26)));
        when(dataCache.getAveragePrices().getUsdCnyRate()).thenReturn(7d);

        var sut = new CashDealGenerator(dataCache, List.of());
        var interest = ImmutableInterest.builder()
                .group("Test")
                .account("Test")
                .metal("XAU")
                .unit("TOz")
                .currency("USD")
                .averageBalanceInTOz(100d)
                .averageBalance(100d)
                .averagePriceForTOz(100d)
                .interestRate(0.01)
                .numOfDays(31)
                .daysOfYear(365)
                .owner("owner")
                .holder("JM PMM CN")
                .build();

        var actual = sut.generate(interest, Region.CN, LocalDate.of(2020, 4, 30));

        var expected = ImmutableCashDeal.builder()
                .currency("USD")
                .cashflowType("Metal Rentals - GOLD")
                .internalBU("JM PMM CN")
                .internalPortfolio("CN Fees")
                .externalBU("owner")
                .settleDate("2020-05-15")
                .statementDate("30-Apr-2020")
                .position(-8.493150684931507)
                .fxRate(7d)
                .build();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void generate_deal_for_non_cn() {
        when(dataCache.getFeePortfolios()).thenReturn(Map.of("JM PMM UK", "UK Fees"));
        when(dataCache.getStatementPeriods()).thenReturn(new StatementPeriods(LocalDate.of(2020, 5, 1)));

        var sut = new CashDealGenerator(dataCache, List.of());
        var interest = ImmutableInterest.builder()
                .group("Test")
                .account("Test")
                .metal("XAG")
                .unit("TOz")
                .currency("USD")
                .averageBalanceInTOz(100d)
                .averageBalance(100d)
                .averagePriceForTOz(100d)
                .interestRate(0.01)
                .numOfDays(31)
                .daysOfYear(365)
                .owner("owner")
                .holder("JM PMM UK")
                .build();

        var actual = sut.generate(interest, Region.NonCN, LocalDate.of(2020, 4, 30));

        var expected = ImmutableCashDeal.builder()
                .currency("USD")
                .cashflowType("Metal Rentals - SILVER")
                .internalBU("JM PMM UK")
                .internalPortfolio("UK Fees")
                .externalBU("owner")
                .settleDate("2020-05-15")
                .statementDate("30-Apr-2020")
                .position(-8.493150684931507)
                .build();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void generate_deal_for_internal_borrowings() {
        when(dataCache.getFeePortfolios()).thenReturn(Map.of("JM PMM UK", "UK Fees", "JM PMM US", "US Fees"));
        when(dataCache.getStatementPeriods()).thenReturn(new StatementPeriods(LocalDate.of(2020, 5, 1)));

        var sut = new CashDealGenerator(dataCache, List.of());
        var interest = ImmutableInterest.builder()
                .group("Test")
                .account("Test")
                .metal("XAG")
                .unit("TOz")
                .currency("USD")
                .averageBalanceInTOz(100d)
                .averageBalance(100d)
                .averagePriceForTOz(100d)
                .interestRate(0.01)
                .numOfDays(31)
                .daysOfYear(365)
                .owner("JM PMM US")
                .holder("JM PMM UK")
                .build();

        var actual = sut.generate(interest, Region.NonCN, LocalDate.now());
        assertThat(actual.externalPortfolio()).isEqualTo("US Fees");
    }

    @Test
    public void holder_does_not_have_proper_portfolio() {
        when(dataCache.getFeePortfolios()).thenReturn(Map.of("JM PMM UK", "UK Fees"));
        when(dataCache.getStatementPeriods()).thenReturn(new StatementPeriods(LocalDate.of(2020, 5, 1)));

        var sut = new CashDealGenerator(dataCache, List.of());
        var interest = mock(Interest.class);
        when(interest.account()).thenReturn("Test Account");
        when(interest.holder()).thenReturn("Invalid Holder");
        assertThatThrownBy(() -> sut.generate(interest, Region.NonCN, LocalDate.now())).isInstanceOf(
                NullPointerException.class).hasMessage("no portfolio for the holder of Test Account: Invalid Holder");
    }
}