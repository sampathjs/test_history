package com.matthey.pmm.metal.rentals.data;

import com.google.common.base.Stopwatch;
import com.matthey.pmm.metal.rentals.Account;
import com.matthey.pmm.metal.rentals.DailyBalance;
import com.matthey.pmm.metal.rentals.EndurConnector;
import com.matthey.pmm.metal.rentals.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static com.rainerhahnekamp.sneakythrow.Sneaky.sneak;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class DailyBalances {

    private static final Logger logger = LoggerFactory.getLogger(DailyBalances.class);

    private final EndurConnector endurConnector;
    private final Map<String, String> holdingBanks;
    private final Map<LocalDate, List<EnhancedDailyBalance>> forAllDays;
    private final Map<String, Account> allAccounts;
    private final StatementPeriods statementPeriods;

    public DailyBalances(EndurConnector endurConnector,
                         Map<String, String> holdingBanks,
                         Map<String, Account> allAccounts,
                         StatementPeriods statementPeriods) {
        this.endurConnector = endurConnector;
        this.holdingBanks = holdingBanks;
        this.allAccounts = allAccounts;
        this.statementPeriods = statementPeriods;
        this.forAllDays = getDailyBalances();
    }

    private Map<LocalDate, List<EnhancedDailyBalance>> getDailyBalances() {
        ForkJoinPool customThreadPool = new ForkJoinPool(11);
        return sneak(() -> customThreadPool.submit(() -> statementPeriods.startDate.datesUntil(statementPeriods.endDate.plusDays(
                1)).parallel().collect(toMap(identity(), this::getDailyBalances))).get());
    }

    private List<EnhancedDailyBalance> getDailyBalances(LocalDate date) {
        var stopwatch = Stopwatch.createStarted();
        var dailyBalances = endurConnector.get("/account_balances/{date}",
                                               DailyBalance[].class,
                                               ISO_LOCAL_DATE.format(date));
        var enhancedDailyBalances = Arrays.stream(dailyBalances)
                .map(this::enhanceDailyBalance)
                .filter(Objects::nonNull)
                .collect(toList());
        stopwatch.stop();
        logger.info("retrieve {} daily balances for the date {} within {} ms",
                    enhancedDailyBalances.size(),
                    date,
                    stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return enhancedDailyBalances;
    }

    private EnhancedDailyBalance enhanceDailyBalance(DailyBalance dailyBalance) {
        var account = allAccounts.get(dailyBalance.account());
        if (account == null) {
            return null;
        }
        var sign = balanceSign(account);
        return ImmutableEnhancedDailyBalance.builder()
                .account(account)
                .date(dailyBalance.date())
                .metal(dailyBalance.metal())
                .balance(dailyBalance.balance() * sign)
                .balanceInTOz(dailyBalance.balanceInTOz() * sign)
                .build();
    }

    private int balanceSign(Account account) {
        var group = InternalBorrowingsGroup.from(account.group());
        if (group.isEmpty()) {
            return 1;
        }
        var groupHolder = holdingBanks.get(group.get().holderRegion());
        var groupOwner = holdingBanks.get(group.get().ownerRegion());
        return account.owner().equals(groupHolder) || account.holder().equals(groupOwner) ? -1 : 1;
    }

    public List<EnhancedDailyBalance> get(Region region) {
        var period = statementPeriods.getPeriod(region);
        return period.startDate.datesUntil(period.endDate.plusDays(1))
                .map(forAllDays::get)
                .flatMap(List::stream)
                .filter(enhancedDailyBalance -> Accounts.getRegion(enhancedDailyBalance.account()).equals(region))
                .collect(toList());
    }
}
