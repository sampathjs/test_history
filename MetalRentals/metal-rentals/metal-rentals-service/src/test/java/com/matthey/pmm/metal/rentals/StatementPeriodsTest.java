package com.matthey.pmm.metal.rentals;

import com.matthey.pmm.metal.rentals.data.StatementPeriods;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class StatementPeriodsTest {

    @Test
    public void current_date_is_before_26th_day_of_month() {
        var sut = new StatementPeriods(LocalDate.of(2019, 11, 11));

        assertThat(sut.startDate).isEqualTo(LocalDate.of(2019, 9, 26));
        assertThat(sut.endDate).isEqualTo(LocalDate.of(2019, 10, 31));
        var periodForCN = sut.getPeriod(Region.CN);
        assertThat(periodForCN.startDate).isEqualTo(LocalDate.of(2019, 9, 26));
        assertThat(periodForCN.endDate).isEqualTo(LocalDate.of(2019, 10, 25));
        assertThat(periodForCN.yearMonth).isEqualTo("2019-10");
        assertThat(periodForCN.numOfDays).isEqualTo(30);
        var periodForNonCN = sut.getPeriod(Region.NonCN);
        assertThat(periodForNonCN.startDate).isEqualTo(LocalDate.of(2019, 10, 1));
        assertThat(periodForNonCN.endDate).isEqualTo(LocalDate.of(2019, 10, 31));
        assertThat(periodForNonCN.yearMonth).isEqualTo("2019-10");
        assertThat(periodForNonCN.numOfDays).isEqualTo(31);
    }

    @Test
    public void current_date_is_after_26th_day_of_month() {
        var sut = new StatementPeriods(LocalDate.of(2020, 3, 26));

        assertThat(sut.startDate).isEqualTo(LocalDate.of(2020, 2, 1));
        assertThat(sut.endDate).isEqualTo(LocalDate.of(2020, 3, 25));
        var periodForCN = sut.getPeriod(Region.CN);
        assertThat(periodForCN.startDate).isEqualTo(LocalDate.of(2020, 2, 26));
        assertThat(periodForCN.endDate).isEqualTo(LocalDate.of(2020, 3, 25));
        assertThat(periodForCN.yearMonth).isEqualTo("2020-03");
        assertThat(periodForCN.numOfDays).isEqualTo(29);
        var periodForNonCN = sut.getPeriod(Region.NonCN);
        assertThat(periodForNonCN.startDate).isEqualTo(LocalDate.of(2020, 2, 1));
        assertThat(periodForNonCN.endDate).isEqualTo(LocalDate.of(2020, 2, 29));
        assertThat(periodForNonCN.yearMonth).isEqualTo("2020-02");
        assertThat(periodForNonCN.numOfDays).isEqualTo(29);
    }
}