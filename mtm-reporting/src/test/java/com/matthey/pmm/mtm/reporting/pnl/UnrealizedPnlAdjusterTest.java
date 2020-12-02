package com.matthey.pmm.mtm.reporting.pnl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnrealizedPnlAdjusterTest {
    
    @Test
    public void result_name_configured_correctly() {
        new UnrealizedPnlAdjuster("CallNot Unrealized P&L");
        new UnrealizedPnlAdjuster("CallNot Unrealized P&L MTD");
        new UnrealizedPnlAdjuster("CallNot Unrealized P&L YTD");
        new UnrealizedPnlAdjuster("CallNot Unrealized P&L LTD");
        new UnrealizedPnlAdjuster("CallNot Base Unrealized P&L");
        new UnrealizedPnlAdjuster("CallNot Base Unrealized P&L MTD");
        new UnrealizedPnlAdjuster("CallNot Base Unrealized P&L YTD");
        new UnrealizedPnlAdjuster("CallNot Base Unrealized P&L LTD");
    }
    
    @Test
    public void result_name_configured_incorrectly() {
        assertThatThrownBy(() -> new UnrealizedPnlAdjuster("CallNot Unrealized PnL")).isInstanceOf(
                IllegalArgumentException.class).hasMessage("unsupported result: CallNot Unrealized PnL");
    }
    
    @Test
    public void dependencies_for_unrealized_pnl() {
        UnrealizedPnlAdjuster sut = new UnrealizedPnlAdjuster("CallNot Unrealized P&L");
        assertThat(sut.getDependentResultName()).isEqualTo("Unrealized P&L");
        assertThat(sut.getPnlResultName()).isEqualTo("CallNot Unrealized P&L");
        assertThat(sut.getBalanceChangeResultName()).isEqualTo("CallNot Balance Change");
    }
    
    @Test
    public void dependencies_for_base_unrealized_pnl() {
        UnrealizedPnlAdjuster sut = new UnrealizedPnlAdjuster("CallNot Base Unrealized P&L");
        assertThat(sut.getDependentResultName()).isEqualTo("Base Unrealized P&L");
        assertThat(sut.getPnlResultName()).isEqualTo("CallNot Base Unrealized P&L");
        assertThat(sut.getBalanceChangeResultName()).isEqualTo("CallNot Base Balance Change");
    }
    
    @Test
    public void dependencies_for_cumulative_unrealized_pnl() {
        UnrealizedPnlAdjuster sut = new UnrealizedPnlAdjuster("CallNot Unrealized P&L MTD");
        assertThat(sut.getDependentResultName()).isEqualTo("Unrealized P&L MTD");
        assertThat(sut.getPnlResultName()).isEqualTo("CallNot Unrealized P&L");
        assertThat(sut.getBalanceChangeResultName()).isEqualTo("CallNot Balance Change");
    }
    
    @Test
    public void dependencies_for_cumulative_base_unrealized_pnl() {
        UnrealizedPnlAdjuster sut = new UnrealizedPnlAdjuster("CallNot Base Unrealized P&L YTD");
        assertThat(sut.getDependentResultName()).isEqualTo("Base Unrealized P&L YTD");
        assertThat(sut.getPnlResultName()).isEqualTo("CallNot Base Unrealized P&L");
        assertThat(sut.getBalanceChangeResultName()).isEqualTo("CallNot Base Balance Change");
    }
    
    @Test
    public void adjust_pnl() {
        UnrealizedPnlAdjuster sut = new UnrealizedPnlAdjuster("CallNot Base Unrealized P&L");
        assertThat(sut.adjust(0, 22.22, 0, 11.11)).isEqualTo(22.22 - 11.11);
    }
    
    @Test
    public void adjust_cumulative_pnl() {
        UnrealizedPnlAdjuster sut = new UnrealizedPnlAdjuster("CallNot Base Unrealized P&L MTD");
        assertThat(sut.adjust(33.33, 22.22, 11.11, 0)).isEqualTo(33.33 + 11.11);
    }
}