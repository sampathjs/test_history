package com.matthey.pmm.mtm.reporting.pnl;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdjustmentCalculatorTest {
    
    @Mock
    private BiFunction<TranResultSet, String, Double> tranResultRetriever;
    
    @Test
    public void invalid_result_name() {
        assertThatThrownBy(() -> new AdjustmentCalculator("Invalid Name",
                                                          ImmutableMap.of("USD", 1d),
                                                          tranResultRetriever,
                                                          "USD",
                                                          "USD",
                                                          "USD")).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unsupported result: Invalid Name");
    }
    
    @Test
    public void no_fx_rate_for_payment_ccy() {
        assertThatThrownBy(() -> new AdjustmentCalculator("CallNot Balance Change",
                                                          ImmutableMap.of("USD", 1d),
                                                          tranResultRetriever,
                                                          "GBP",
                                                          "USD",
                                                          "USD")).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("there is no fx rate for GBP");
    }
    
    @Test
    public void no_fx_rate_for_notional_ccy() {
        assertThatThrownBy(() -> new AdjustmentCalculator("CallNot Balance Change",
                                                          ImmutableMap.of("USD", 1d),
                                                          tranResultRetriever,
                                                          "USD",
                                                          "EUR",
                                                          "USD")).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("there is no fx rate for EUR");
    }
    
    @Test
    public void no_fx_rate_for_base_ccy() {
        assertThatThrownBy(() -> new AdjustmentCalculator("CallNot Balance Change",
                                                          ImmutableMap.of("USD", 1d),
                                                          tranResultRetriever,
                                                          "USD",
                                                          "USD",
                                                          "CNY")).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("there is no fx rate for CNY");
    }
    
    @Test
    public void calc_call_notice_balance_change() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "Current Notional Result")).thenReturn(3.33);
        when(tranResultRetriever.apply(TranResultSet.PRIOR, "Current Notional Result")).thenReturn(1.11);
        AdjustmentCalculator sut = new AdjustmentCalculator("CallNot Balance Change",
                                                            ImmutableMap.of("XPD", 1 / 1800d, "EUR", 1 / 1.2, "GBP", 1 / 1.5),
                                                            tranResultRetriever,
                                                            "GBP",
                                                            "XPD",
                                                            "EUR");
        assertThat(sut.calc()).isEqualTo((3.33 - 1.11) * 1800 / 1.5, within(0.001));
    }
    
    @Test
    public void calc_call_notice_base_pnl_adjustment() {
        when(tranResultRetriever.apply(TranResultSet.CURRENT, "CallNot Balance Change")).thenReturn(3.33);
        AdjustmentCalculator sut = new AdjustmentCalculator("CallNot Base P&L Adjustment",
                                                            ImmutableMap.of("XPD", 1 / 1800d, "EUR", 1 / 1.2, "GBP", 1 / 1.5),
                                                            tranResultRetriever,
                                                            "GBP",
                                                            "XPD",
                                                            "EUR");
        assertThat(sut.calc()).isEqualTo(3.33 * 1.5 / 1.2, within(0.001));
    }
}