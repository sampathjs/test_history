package com.matthey.pmm.mtm.reporting.pnl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PnlCalculatorTest {
    
    @Mock
    private BiFunction<TranResultSet, String, Double> tranResultRetriever;
    
    @Test
    public void run_unrealised_pnl() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "CallNot Balance Change")).thenReturn(1.11);
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "Unrealized P&L")).thenReturn(3.33);
        PnlCalculator sut = new PnlCalculator(tranResultRetriever, "JM Unrealized P&L", "01-01", LocalDate.now());
        assertThat(sut.calc()).isEqualTo(2.22, within(0.001));
    }
    
    @Test
    public void run_base_unrealised_pnl() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "CallNot Base P&L Adjustment")).thenReturn(2.22);
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "Base Unrealized P&L")).thenReturn(6.66);
        PnlCalculator sut = new PnlCalculator(tranResultRetriever, "JM Base Unrealized P&L", "01-01", LocalDate.now());
        assertThat(sut.calc()).isEqualTo(4.44, within(0.001));
    }
    
    @Test
    public void run_unrealised_pnl_mtd_at_month_beginning() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "JM Unrealized P&L")).thenReturn(11.11);
        when(tranResultRetriever.apply(TranResultSet.PRIOR, "JM Unrealized P&L MTD")).thenReturn(22.22);
        PnlCalculator sut = new PnlCalculator(tranResultRetriever,
                                              "JM Unrealized P&L MTD",
                                              "01-01",
                                              LocalDate.parse("2020-12-01"));
        assertThat(sut.calc()).isEqualTo(11.11, within(0.001));
    }
    
    @Test
    public void run_unrealised_pnl_mtd_at_non_month_beginning() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "JM Unrealized P&L")).thenReturn(11.11);
        when(tranResultRetriever.apply(TranResultSet.PRIOR, "JM Unrealized P&L MTD")).thenReturn(22.22);
        PnlCalculator sut = new PnlCalculator(tranResultRetriever,
                                              "JM Unrealized P&L MTD",
                                              "01-01",
                                              LocalDate.parse("2020-12-11"));
        assertThat(sut.calc()).isEqualTo(33.33, within(0.001));
    }
    
    @Test
    public void run_unrealised_pnl_ytd_at_year_start() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "JM Unrealized P&L")).thenReturn(111.11);
        when(tranResultRetriever.apply(TranResultSet.PRIOR, "JM Unrealized P&L YTD")).thenReturn(222.22);
        PnlCalculator sut = new PnlCalculator(tranResultRetriever,
                                              "JM Unrealized P&L YTD",
                                              "04-01",
                                              LocalDate.parse("2020-04-01"));
        assertThat(sut.calc()).isEqualTo(111.11, within(0.001));
    }
    
    @Test
    public void run_unrealised_pnl_ytd_at_non_year_start() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "JM Unrealized P&L")).thenReturn(111.11);
        when(tranResultRetriever.apply(TranResultSet.PRIOR, "JM Unrealized P&L YTD")).thenReturn(222.22);
        PnlCalculator sut = new PnlCalculator(tranResultRetriever,
                                              "JM Unrealized P&L YTD",
                                              "04-01",
                                              LocalDate.parse("2020-01-01"));
        assertThat(sut.calc()).isEqualTo(333.33, within(0.001));
    }
    
    @Test
    public void run_unrealised_pnl_ltd() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "JM Unrealized P&L")).thenReturn(1111.11);
        when(tranResultRetriever.apply(TranResultSet.PRIOR, "JM Unrealized P&L LTD")).thenReturn(2222.22);
        PnlCalculator sut = new PnlCalculator(tranResultRetriever, "JM Unrealized P&L LTD", "04-01", LocalDate.now());
        assertThat(sut.calc()).isEqualTo(3333.33, within(0.001));
    }
    
    @Test
    public void run_base_unrealised_pnl_mtd_at_month_beginning() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "JM Base Unrealized P&L")).thenReturn(11.11);
        when(tranResultRetriever.apply(TranResultSet.PRIOR, "JM Base Unrealized P&L MTD")).thenReturn(22.22);
        PnlCalculator sut = new PnlCalculator(tranResultRetriever,
                                              "JM Base Unrealized P&L MTD",
                                              "01-01",
                                              LocalDate.parse("2020-12-01"));
        assertThat(sut.calc()).isEqualTo(11.11, within(0.001));
    }
    
    @Test
    public void run_base_unrealised_pnl_mtd_at_non_month_beginning() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "JM Base Unrealized P&L")).thenReturn(11.11);
        when(tranResultRetriever.apply(TranResultSet.PRIOR, "JM Base Unrealized P&L MTD")).thenReturn(22.22);
        PnlCalculator sut = new PnlCalculator(tranResultRetriever,
                                              "JM Base Unrealized P&L MTD",
                                              "01-01",
                                              LocalDate.parse("2020-12-11"));
        assertThat(sut.calc()).isEqualTo(33.33, within(0.001));
    }
    
    @Test
    public void run_base_unrealised_pnl_ytd_at_year_start() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "JM Base Unrealized P&L")).thenReturn(111.11);
        when(tranResultRetriever.apply(TranResultSet.PRIOR, "JM Base Unrealized P&L YTD")).thenReturn(222.22);
        PnlCalculator sut = new PnlCalculator(tranResultRetriever,
                                              "JM Base Unrealized P&L YTD",
                                              "04-01",
                                              LocalDate.parse("2020-04-01"));
        assertThat(sut.calc()).isEqualTo(111.11, within(0.001));
    }
    
    @Test
    public void run_base_unrealised_pnl_ytd_at_non_year_start() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "JM Base Unrealized P&L")).thenReturn(111.11);
        when(tranResultRetriever.apply(TranResultSet.PRIOR, "JM Base Unrealized P&L YTD")).thenReturn(222.22);
        PnlCalculator sut = new PnlCalculator(tranResultRetriever,
                                              "JM Base Unrealized P&L YTD",
                                              "04-01",
                                              LocalDate.parse("2020-01-01"));
        assertThat(sut.calc()).isEqualTo(333.33, within(0.001));
    }
    
    @Test
    public void run_base_unrealised_pnl_ltd() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "JM Base Unrealized P&L")).thenReturn(1111.11);
        when(tranResultRetriever.apply(TranResultSet.PRIOR, "JM Base Unrealized P&L LTD")).thenReturn(2222.22);
        PnlCalculator sut = new PnlCalculator(tranResultRetriever,
                                              "JM Base Unrealized P&L LTD",
                                              "04-01",
                                              LocalDate.now());
        assertThat(sut.calc()).isEqualTo(3333.33, within(0.001));
    }
    
    @Test
    public void run_realised_pnl_ytd_at_year_start() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "JM Realized P&L")).thenReturn(111.11);
        when(tranResultRetriever.apply(TranResultSet.PRIOR, "JM Realized P&L YTD")).thenReturn(222.22);
        PnlCalculator sut = new PnlCalculator(tranResultRetriever,
                                              "JM Realized P&L YTD",
                                              "04-01",
                                              LocalDate.parse("2020-04-01"));
        assertThat(sut.calc()).isEqualTo(111.11, within(0.001));
    }
    
    @Test
    public void run_realised_pnl_ytd_at_non_year_start() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "JM Realized P&L")).thenReturn(111.11);
        when(tranResultRetriever.apply(TranResultSet.PRIOR, "JM Realized P&L YTD")).thenReturn(222.22);
        PnlCalculator sut = new PnlCalculator(tranResultRetriever,
                                              "JM Realized P&L YTD",
                                              "04-01",
                                              LocalDate.parse("2020-01-01"));
        assertThat(sut.calc()).isEqualTo(333.33, within(0.001));
    }
    
    @Test
    public void run_base_realised_pnl_ytd_at_year_start() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "JM Base Realized P&L")).thenReturn(111.11);
        when(tranResultRetriever.apply(TranResultSet.PRIOR, "JM Base Realized P&L YTD")).thenReturn(222.22);
        PnlCalculator sut = new PnlCalculator(tranResultRetriever,
                                              "JM Base Realized P&L YTD",
                                              "04-01",
                                              LocalDate.parse("2020-04-01"));
        assertThat(sut.calc()).isEqualTo(111.11, within(0.001));
    }
    
    @Test
    public void run_base_realised_pnl_ytd_at_non_year_start() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "JM Base Realized P&L")).thenReturn(111.11);
        when(tranResultRetriever.apply(TranResultSet.PRIOR, "JM Base Realized P&L YTD")).thenReturn(222.22);
        PnlCalculator sut = new PnlCalculator(tranResultRetriever,
                                              "JM Base Realized P&L YTD",
                                              "04-01",
                                              LocalDate.parse("2020-01-01"));
        assertThat(sut.calc()).isEqualTo(333.33, within(0.001));
    }
    
    @Test
    public void run_realised_pnl() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "Realized P&L")).thenReturn(111.11);
        PnlCalculator sut = new PnlCalculator(tranResultRetriever, "JM Realized P&L", "01-01", LocalDate.now());
        assertThat(sut.calc()).isEqualTo(111.11, within(0.001));
    }
    
    @Test
    public void run_base_realised_pnl() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "Base Realized P&L")).thenReturn(111.11);
        PnlCalculator sut = new PnlCalculator(tranResultRetriever, "JM Base Realized P&L", "01-01", LocalDate.now());
        assertThat(sut.calc()).isEqualTo(111.11, within(0.001));
    }
    
    @Test
    public void run_realised_pnl_mtd() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "Realized P&L MTD")).thenReturn(111.11);
        PnlCalculator sut = new PnlCalculator(tranResultRetriever,
                                              "JM Realized P&L MTD",
                                              "01-01",
                                              LocalDate.parse("2020-12-01"));
        assertThat(sut.calc()).isEqualTo(111.11, within(0.001));
    }
    
    @Test
    public void run_base_realised_pnl_mtd() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "Base Realized P&L MTD")).thenReturn(111.11);
        PnlCalculator sut = new PnlCalculator(tranResultRetriever,
                                              "JM Base Realized P&L MTD",
                                              "01-01",
                                              LocalDate.parse("2020-12-01"));
        assertThat(sut.calc()).isEqualTo(111.11, within(0.001));
    }
    
    @Test
    public void run_realised_pnl_ltd() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "Realized P&L LTD")).thenReturn(111.11);
        PnlCalculator sut = new PnlCalculator(tranResultRetriever, "JM Realized P&L LTD", "01-01", LocalDate.now());
        assertThat(sut.calc()).isEqualTo(111.11, within(0.001));
    }
    
    @Test
    public void run_base_realised_pnl_ltd() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "Base Realized P&L LTD")).thenReturn(111.11);
        PnlCalculator sut = new PnlCalculator(tranResultRetriever,
                                              "JM Base Realized P&L LTD",
                                              "01-01",
                                              LocalDate.now());
        assertThat(sut.calc()).isEqualTo(111.11, within(0.001));
    }
}