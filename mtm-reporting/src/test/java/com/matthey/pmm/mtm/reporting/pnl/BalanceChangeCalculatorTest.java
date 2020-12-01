package com.matthey.pmm.mtm.reporting.pnl;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class BalanceChangeCalculatorTest {
    
    @Test
    public void result_name_configured_correctly() {
        new BalanceChangeCalculator("CallNot Balance Change", "USD", ImmutableMap.of("USD", 1d));
        new BalanceChangeCalculator("CallNot Base Balance Change", "USD", ImmutableMap.of("USD", 1d));
    }
    
    @Test
    public void result_name_configured_incorrectly() {
        ThrowingCallable sut = () -> new BalanceChangeCalculator("Call Notice Balance Change",
                                                                 "USD",
                                                                 ImmutableMap.of("USD", 1d));
        assertThatThrownBy(sut).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unsupported result: Call Notice Balance Change");
    }
    
    @Test
    public void fx_rate_not_exist_for_base_currency_when_calc_base_balance_change() {
        ThrowingCallable sut = () -> new BalanceChangeCalculator("CallNot Base Balance Change",
                                                                 "EUR",
                                                                 ImmutableMap.of("USD", 1d));
        assertThatThrownBy(sut).isInstanceOf(IllegalArgumentException.class).hasMessage("there is no fx rate for EUR");
    }
    
    @Test
    public void base_currency_ignored_when_calc_balance_change() {
        new BalanceChangeCalculator("CallNot Balance Change", "EUR", ImmutableMap.of());
    }
    
    @Test
    public void calc_balance_change() {
        BalanceChangeCalculator sut = new BalanceChangeCalculator("CallNot Balance Change",
                                                                  "EUR",
                                                                  ImmutableMap.of("MT1",
                                                                                  1d / 1000,
                                                                                  "EUR",
                                                                                  1d / 1.20,
                                                                                  "MT2",
                                                                                  1d / 400));
        CallNotice callNotice = ImmutableCallNotice.builder()
                .tranNum(100)
                .paymentCcy("MT1")
                .notionalCcy("MT2")
                .currentNotional(300)
                .previousNotional(100)
                .build();
        double actual = sut.calc(callNotice);
        assertThat(actual).isEqualTo(200d * 400d / 1000d, within(0.0000001));
    }
    
    @Test
    public void calc_base_balance_change() {
        BalanceChangeCalculator sut = new BalanceChangeCalculator("CallNot Base Balance Change",
                                                                  "GBP",
                                                                  ImmutableMap.of("MT1",
                                                                                  1d / 1000,
                                                                                  "GBP",
                                                                                  1d / 1.2,
                                                                                  "MT2",
                                                                                  1d / 400));
        CallNotice callNotice = ImmutableCallNotice.builder()
                .tranNum(100)
                .paymentCcy("MT1")
                .notionalCcy("MT2")
                .currentNotional(300)
                .previousNotional(100)
                .build();
        double actual = sut.calc(callNotice);
        assertThat(actual).isEqualTo(200d * 400d / 1.2d, within(0.0000001));
    }
    
    @Test
    public void no_fx_rate_for_payment_ccy() {
        BalanceChangeCalculator sut = new BalanceChangeCalculator("CallNot Balance Change",
                                                                  "USD",
                                                                  ImmutableMap.of("MT2", 1d / 400));
        CallNotice callNotice = ImmutableCallNotice.builder()
                .tranNum(100)
                .paymentCcy("MT1")
                .notionalCcy("MT2")
                .currentNotional(300)
                .previousNotional(100)
                .build();
        assertThatThrownBy(() -> sut.calc(callNotice)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("there is no fx rate for MT1");
    }
    
    @Test
    public void no_fx_rate_for_notional_ccy() {
        BalanceChangeCalculator sut = new BalanceChangeCalculator("CallNot Balance Change",
                                                                  "USD",
                                                                  ImmutableMap.of("MT1", 1d / 1000));
        CallNotice callNotice = ImmutableCallNotice.builder()
                .tranNum(100)
                .paymentCcy("MT1")
                .notionalCcy("MT2")
                .currentNotional(300)
                .previousNotional(100)
                .build();
        assertThatThrownBy(() -> sut.calc(callNotice)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("there is no fx rate for MT2");
    }
}