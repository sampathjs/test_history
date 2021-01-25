package com.matthey.pmm.apdp.reports;

import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

import java.time.LocalDate;

@Immutable
@Style(stagedBuilder = true)
public interface FXSellDeal {
    
    int dealNum();
    
    String reference();
    
    String externalBU();
    
    String metal();
    
    LocalDate tradeDate();
    
    LocalDate settleDate();
    
    @Derived
    default double hkdAmount() {
        return usdAmount() * hkdFxRate();
    }
    
    @Derived
    default double usdAmount() {
        return position() * price();
    }
    
    double position();
    
    double price();
    
    double hkdFxRate();
}
