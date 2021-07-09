package com.matthey.pmm.metal.transfers.data;

import com.google.common.collect.Maps;
import com.matthey.pmm.metal.transfers.Region;

import static com.matthey.pmm.metal.transfers.Region.CN;
import static com.matthey.pmm.metal.transfers.Region.NonCN;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.stream.Stream;

public class StatementPeriods {

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
    public final LocalDate startDate;
    public final LocalDate endDate;
    public final LocalDate currentDate;
    private final EnumMap<Region, Period> periods = Maps.newEnumMap(Region.class);

    public StatementPeriods(LocalDate currentDate) {
        periods.put(NonCN, new Period(currentDate.minusMonths(1).withDayOfMonth(1)));
        periods.put(CN,
                    new Period(currentDate.minusMonths(currentDate.getDayOfMonth() > 25 ? 1 : 2).withDayOfMonth(26)));
        this.currentDate = currentDate;
        startDate = Stream.of(periods.get(NonCN).startDate, periods.get(CN).startDate).min(LocalDate::compareTo).get();
        endDate = Stream.of(periods.get(NonCN).endDate, periods.get(CN).endDate).max(LocalDate::compareTo).get();
    }

    public Period getPeriod(Region region) {
        return periods.get(region);
    }

    public static class Period {
        public final LocalDate startDate;
        public final LocalDate endDate;
        public final LocalDate settleDate;
        public final String yearMonth;
        public final int numOfDays;

        Period(LocalDate startDate) {
            this.startDate = startDate;
            this.endDate = startDate.plusMonths(1).minusDays(1);
            this.settleDate = this.endDate.plusMonths(1).withDayOfMonth(15);
            this.yearMonth = endDate.format(YEAR_MONTH);
            this.numOfDays = java.time.Period.between(this.startDate, this.endDate).getDays() + 1;
        }
    }
}
