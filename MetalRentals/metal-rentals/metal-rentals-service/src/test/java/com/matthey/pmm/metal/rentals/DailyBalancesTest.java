package com.matthey.pmm.metal.rentals;

import com.matthey.pmm.metal.rentals.data.DailyBalances;
import com.matthey.pmm.metal.rentals.data.EnhancedDailyBalance;
import com.matthey.pmm.metal.rentals.data.StatementPeriods;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;

import static com.matthey.pmm.metal.rentals.TestUtils.genAccount;
import static com.matthey.pmm.metal.rentals.TestUtils.genAccountForInternalBorrowings;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyBalancesTest {

    @Mock
    private EndurConnector endurConnector;

    @Test
    public void retrieve_balances_for_accounts_of_group_company_borrowings() {
        Map<String, Account> accounts = Map.of("account@company/1",
                                               genAccount("account@company/1", null, false),
                                               "account@company/2",
                                               genAccount("account@company/2", null, false),
                                               "account@cn_company",
                                               genAccount("account@cn_company", null, true));
        var dailyBalances = new DailyBalance[]{genDailyBalance("account@company/1", 100d),
                                               genDailyBalance("account@company/2", 100d),
                                               genDailyBalance("account@cn_company", 100d)};
        when(endurConnector.get(anyString(), eq(DailyBalance[].class), anyString())).thenReturn(new DailyBalance[]{});
        var dateRangeMatcher = new DateRangeMatcher("2019-11-20", "2019-12-11");
        when(endurConnector.get(anyString(), eq(DailyBalance[].class), argThat(dateRangeMatcher))).thenReturn(
                dailyBalances);
        var sut = new DailyBalances(endurConnector,
                                    Map.of(),
                                    accounts,
                                    new StatementPeriods(LocalDate.of(2019, 12, 1)));
        var dailyBalancesForNonCN = sut.get(Region.NonCN);
        assertThat(dailyBalancesForNonCN).hasSize(11 * 2);
        assertThat(dailyBalancesForNonCN).extracting(EnhancedDailyBalance::accountGroup)
                .containsOnly("account@company");
        assertThat(dailyBalancesForNonCN).extracting(EnhancedDailyBalance::interestKey)
                .containsOnly("account@company/1", "account@company/2");
        var dailyBalancesForCN = sut.get(Region.CN);
        assertThat(dailyBalancesForCN).hasSize(6);
    }

    private DailyBalance genDailyBalance(String account, double balance) {
        return ImmutableDailyBalance.builder()
                .account(account)
                .date("2019-11-11")
                .metal("M1")
                .balance(balance)
                .balanceInTOz(balance)
                .build();
    }

    @Test
    public void retrieve_balances_for_accounts_of_internal_borrowings() {
        Map<String, Account> accounts = Map.of("account 1",
                                               genAccount("account 1", "UK-US", false),
                                               "account 2",
                                               genAccount("account 2", "UK-HK", false));
        var dailyBalances = new DailyBalance[]{genDailyBalance("account 1", 100d), genDailyBalance("account 2", 100d)};
        when(endurConnector.get(anyString(), eq(DailyBalance[].class), anyString())).thenReturn(dailyBalances);
        var sut = new DailyBalances(endurConnector,
                                    Map.of(),
                                    accounts,
                                    new StatementPeriods(LocalDate.of(2019, 12, 1)));
        var allDailyBalances = sut.get(Region.NonCN);
        assertThat(allDailyBalances).hasSize(60);
        assertThat(allDailyBalances).extracting(EnhancedDailyBalance::accountGroup).containsOnly("UK-US", "UK-HK");
        assertThat(allDailyBalances).extracting(EnhancedDailyBalance::interestKey).containsOnly("");
    }

    @Test
    public void reverse_balance_signs_for_internal_borrowings() {
        Map<String, Account> accounts = Map.of("PMM US@PMM UK",
                                               genAccountForInternalBorrowings("PMM US", "PMM UK"),
                                               "PMM UK@PMM HK",
                                               genAccountForInternalBorrowings("PMM UK", "PMM US"),
                                               "PMM HK@PMM US",
                                               genAccountForInternalBorrowings("PMM HK", "PMM US"));
        var dailyBalances = new DailyBalance[]{genDailyBalance("PMM US@PMM UK", 100d),
                                               genDailyBalance("PMM UK@PMM HK", 200d),
                                               genDailyBalance("PMM HK@PMM US", 300d)};
        when(endurConnector.get(anyString(), eq(DailyBalance[].class), anyString())).thenReturn(new DailyBalance[]{});
        when(endurConnector.get(anyString(), eq(DailyBalance[].class), eq("2019-11-11"))).thenReturn(dailyBalances);
        var sut = new DailyBalances(endurConnector,
                                    Map.of("UK", "PMM UK", "US", "PMM US"),
                                    accounts,
                                    new StatementPeriods(LocalDate.of(2019, 12, 1)));
        var allDailyBalances = sut.get(Region.NonCN);
        assertThat(allDailyBalances).hasSize(3);
        assertThat(allDailyBalances).extracting(EnhancedDailyBalance::balance).containsOnly(100d, -200d, -300d);
    }

    @Test
    public void invalid_account_of_daily_balances() {
        var dailyBalances = new DailyBalance[]{genDailyBalance("account 1", 100d), genDailyBalance("account 2", 100d)};
        when(endurConnector.get(anyString(), eq(DailyBalance[].class), anyString())).thenReturn(dailyBalances);
        var sut = new DailyBalances(endurConnector,
                                    Map.of(),
                                    Map.of(),
                                    new StatementPeriods(LocalDate.of(2019, 12, 1)));
        var allDailyBalances = sut.get(Region.NonCN);
        assertThat(allDailyBalances).isEmpty();
    }

    private static class DateRangeMatcher implements ArgumentMatcher<String> {

        private final String startDate;
        private final String endDate;

        public DateRangeMatcher(String startDate, String endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        @Override
        public boolean matches(String argument) {
            return argument.compareTo(startDate) >= 0 && argument.compareTo(endDate) <= 0;
        }
    }
}