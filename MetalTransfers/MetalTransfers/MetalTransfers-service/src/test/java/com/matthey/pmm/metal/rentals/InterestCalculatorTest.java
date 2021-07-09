package com.matthey.pmm.metal.transfers;

import com.matthey.pmm.metal.transfers.Account;
import com.matthey.pmm.metal.transfers.Region;
import com.matthey.pmm.metal.transfers.data.EnhancedDailyBalance;
import com.matthey.pmm.metal.transfers.data.ImmutableEnhancedDailyBalance;
import com.matthey.pmm.metal.transfers.interest.ImmutableInterestCalculationParameters;
import com.matthey.pmm.metal.transfers.interest.InterestCalculator;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.matthey.pmm.metal.transfers.TestUtils.genAccount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InterestCalculatorTest {

    private final Map<String, Double> interestRates = Map.of("M1", 0.01, "M2", 0.02);
    private final Map<String, Map<String, Double>> averagePrices = Map.of("GBP",
                                                                          Map.of("M1", 100.00, "M2", 200.00),
                                                                          "USD",
                                                                          Map.of("M1", 200.00, "M2", 400.00));
    private final Map<String, String> holdingBanks = Map.of("UK", "JM PMM UK", "US", "JM PMM US");

    @Test
    public void filter_interest_based_on_accounts() {
        var dailyBalances = List.of(genDailyBalance("2019-11-01", 100d),
                                    genDailyBalance("2019-11-01", 100d),
                                    genDailyBalance("2019-11-02", 100d),
                                    genDailyBalance("2019-11-03", 100d));
        var sut = new InterestCalculator(holdingBanks, List.of(), 0.13);
        var parameters = ImmutableInterestCalculationParameters.builder()
                .accounts(Map.of())
                .interestRates(interestRates)
                .averagePrices(averagePrices)
                .build();
        var interest = sut.calculateAllInterests(dailyBalances, parameters, 31, Region.NonCN);
        assertThat(interest).isEmpty();
    }

    @Test
    public void calculate_interest_for_internal_borrowings() {
        var dailyBalances = List.of(genDailyBalance("2019-11-01", 100d),
                                    genDailyBalance("2019-11-01", 200d),
                                    genDailyBalance("2019-11-02", 150d),
                                    genDailyBalance("2019-11-03", -50d));
        var sut = new InterestCalculator(holdingBanks, List.of(), 0.13);
        var parameters = ImmutableInterestCalculationParameters.builder()
                .accounts(Map.of("UK-US", Set.of(mock(Account.class, RETURNS_MOCKS))))
                .interestRates(interestRates)
                .averagePrices(averagePrices)
                .build();
        var interest = sut.calculateAllInterests(dailyBalances, parameters, 31, Region.NonCN).get("UK-US").get(0);
        assertThat(interest.group()).isEqualTo("UK-US");
        assertThat(interest.account()).isBlank();
        assertThat(interest.holder()).isEqualTo("JM PMM UK");
        assertThat(interest.owner()).isEqualTo("JM PMM US");
        assertThat(interest.metal()).isEqualTo("M1");
        assertThat(interest.unit()).isEqualTo("TOz");
        assertThat(interest.currency()).isEqualTo("USD");
        assertThat(interest.averagePrice()).isEqualTo(600);
        assertThat(interest.averagePriceForTOz()).isEqualTo(200);
        assertThat(interest.interestRate()).isEqualTo(0.01);
        assertThat(interest.averageBalance()).isEqualTo(133.34, within(0.01));
        assertThat(interest.averageBalanceInTOz()).isEqualTo(400.0, within(0.01));
        assertThat(interest.value()).isEqualTo(-67.95, within(0.01));
    }

    @Test
    public void calculate_interest_for_internal_borrowings_for_cn_with_vat() {
        var accountName = "account@holder";
        Account account = genAccount(accountName, "TOz", "USD");
        var dailyBalances = List.of(genDailyBalance(account, "M1"));
        var sut = new InterestCalculator(Map.of(), List.of(), 0.13);
        var parameters = ImmutableInterestCalculationParameters.builder()
                .accounts(Map.of(accountName, Set.of(account)))
                .interestRates(interestRates)
                .averagePrices(averagePrices)
                .build();
        var interests = sut.calculateAllInterests(dailyBalances, parameters, 31, Region.CN);
        var interest = interests.get(accountName).get(0);
        assertThat(interest.currency()).isEqualTo("USD");
        assertThat(interest.unit()).isEqualTo("TOz");
        assertThat(interest.owner()).isEqualTo("owner");
        assertThat(interest.holder()).isEqualTo("holder");
        assertThat(interest.averagePrice()).isEqualTo(225.99999999999997);
        assertThat(interest.averagePriceForTOz()).isEqualTo(225.99999999999997);
        assertThat(interest.interestRate()).isEqualTo(0.01);
        assertThat(interest.averageBalance()).isEqualTo(100);
        assertThat(interest.averageBalanceInTOz()).isEqualTo(100);
        assertThat(interest.value()).isEqualTo(-19.46, within(0.01));
    }

    @Test
    public void calculate_interest_for_internal_borrowings_for_cn_without_vat() {
        var accountName = "account@holder";
        Account account = genAccount(accountName, "TOz", "USD");
        var dailyBalances = List.of(genDailyBalance(account, "M1"));
        var sut = new InterestCalculator(Map.of(), List.of("owner"), 0.13);
        var parameters = ImmutableInterestCalculationParameters.builder()
                .accounts(Map.of(accountName, Set.of(account)))
                .interestRates(interestRates)
                .averagePrices(averagePrices)
                .build();
        var interests = sut.calculateAllInterests(dailyBalances, parameters, 31, Region.CN);
        var interest = interests.get(accountName).get(0);
        assertThat(interest.currency()).isEqualTo("USD");
        assertThat(interest.unit()).isEqualTo("TOz");
        assertThat(interest.owner()).isEqualTo("owner");
        assertThat(interest.holder()).isEqualTo("holder");
        assertThat(interest.averagePrice()).isEqualTo(200);
        assertThat(interest.averagePriceForTOz()).isEqualTo(200);
        assertThat(interest.interestRate()).isEqualTo(0.008849, within(0.000001));
        assertThat(interest.averageBalance()).isEqualTo(100);
        assertThat(interest.averageBalanceInTOz()).isEqualTo(100);
        assertThat(interest.value()).isEqualTo(-15.24, within(0.01));
    }

    @Test
    public void fill_interest_with_account_info() {
        var accountName = "account@holder";
        Account account = genAccount(accountName, "kgs", "GBP");
        var dailyBalances = List.of(genDailyBalance(account, "M1"));
        var sut = new InterestCalculator(Map.of(), List.of(), 0.13);
        var parameters = ImmutableInterestCalculationParameters.builder()
                .accounts(Map.of(accountName, Set.of(account)))
                .interestRates(interestRates)
                .averagePrices(averagePrices)
                .build();
        var interests = sut.calculateAllInterests(dailyBalances, parameters, 31, Region.NonCN);
        var interest = interests.get(accountName).get(0);
        assertThat(interest.currency()).isEqualTo("GBP");
        assertThat(interest.unit()).isEqualTo("kgs");
        assertThat(interest.owner()).isEqualTo("owner");
        assertThat(interest.holder()).isEqualTo("holder");
    }

    @Test
    public void use_usd_when_account_preferred_currency_is_missing() {
        var accountName = "account@holder";
        Account account = genAccount(accountName, "gms", "");
        var dailyBalances = List.of(genDailyBalance(account, "M1"));
        var sut = new InterestCalculator(Map.of(), List.of(), 0.13);
        var parameters = ImmutableInterestCalculationParameters.builder()
                .accounts(Map.of(accountName, Set.of(account)))
                .interestRates(interestRates)
                .averagePrices(averagePrices)
                .build();
        var interests = sut.calculateAllInterests(dailyBalances, parameters, 31, Region.NonCN);
        var interest = interests.get(accountName).get(0);
        assertThat(interest.currency()).isEqualTo("USD");
        assertThat(interest.unit()).isEqualTo("gms");
    }

    @Test
    public void group_daily_balances_for_interests() {
        var accountName1 = "account@holder/1";
        var accountName2 = "account@holder/2";
        var accountName3 = "account 1";
        var accountName4 = "account 2";
        var account1 = genAccount(accountName1);
        var account2 = genAccount(accountName2);
        var account3 = genAccount(accountName3, "UK-US", false);
        var account4 = genAccount(accountName4, "UK-US", false);
        var dailyBalances = List.of(genDailyBalance(account1, "M1"),
                                    genDailyBalance(account1, "M1"),
                                    genDailyBalance(account1, "M2"),
                                    genDailyBalance(account2, "M1"),
                                    genDailyBalance(account3, "M1"),
                                    genDailyBalance(account4, "M1"));
        var sut = new InterestCalculator(holdingBanks, List.of(), 0.13);
        var parameters = ImmutableInterestCalculationParameters.builder()
                .accounts(Map.of("account@holder", Set.of(account1, account2), "UK-US", Set.of(account3, account4)))
                .interestRates(interestRates)
                .averagePrices(averagePrices)
                .build();
        var interests = sut.calculateAllInterests(dailyBalances, parameters, 31, Region.NonCN);
        assertThat(interests).containsOnlyKeys("account@holder", "UK-US");
        var groupBorrowingsInterests = interests.get("account@holder");
        assertThat(groupBorrowingsInterests).hasSize(3);
        assertThat(groupBorrowingsInterests).filteredOn(interest -> interest.account().equals(accountName1) &&
                                                                    interest.metal().equals("M1")).hasSize(1);
        assertThat(groupBorrowingsInterests).filteredOn(interest -> interest.account().equals(accountName1) &&
                                                                    interest.metal().equals("M2")).hasSize(1);
        assertThat(groupBorrowingsInterests).filteredOn(interest -> interest.account().equals(accountName2) &&
                                                                    interest.metal().equals("M1")).hasSize(1);
        var internalBorrowingsInterests = interests.get("UK-US");
        assertThat(internalBorrowingsInterests).hasSize(1);
    }

    @Test
    public void generate_other_interests_even_one_fail() {
        var accountName = "account@holder";
        Account account = genAccount(accountName);
        var dailyBalances = List.of(genDailyBalance(account, "M1"), genDailyBalance(account, "Invalid Metal"));
        var sut = new InterestCalculator(Map.of(), List.of(), 0.13);
        var parameters = ImmutableInterestCalculationParameters.builder()
                .accounts(Map.of(accountName, Set.of(account)))
                .interestRates(interestRates)
                .averagePrices(averagePrices)
                .build();
        var interests = sut.calculateAllInterests(dailyBalances, parameters, 31, Region.NonCN);
        assertThat(interests.get(accountName)).hasSize(1);
    }

    private EnhancedDailyBalance genDailyBalance(String date, double balance) {
        var mockAccount = mock(Account.class);
        when(mockAccount.group()).thenReturn("UK-US");
        when(mockAccount.internalBorrowings()).thenReturn("UK-US");

        return ImmutableEnhancedDailyBalance.builder()
                .account(mockAccount)
                .date(date)
                .metal("M1")
                .balance(balance)
                .balanceInTOz(balance * 3)
                .build();
    }

    private EnhancedDailyBalance genDailyBalance(Account account, String metal) {
        return ImmutableEnhancedDailyBalance.builder()
                .account(account)
                .date("2019-11-11")
                .metal(metal)
                .balance(100d)
                .balanceInTOz(100d)
                .build();
    }
}