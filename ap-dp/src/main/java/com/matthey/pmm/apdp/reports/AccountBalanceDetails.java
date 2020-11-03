package com.matthey.pmm.apdp.reports;

import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

import java.time.LocalDate;

@Immutable
@Style(stagedBuilder = true)
public interface AccountBalanceDetails {
    
    long eventNum();
    
    LocalDate eventDate();
    
    String metal();
    
    String internalAccount();
    
    @Derived
    default boolean isDeferred() {
        return settleDifference() < 0;
    }
    
    @Derived
    default double settleDifference() {
        return settleAmount() - actualAmount();
    }
    
    double settleAmount();
    
    double actualAmount();
    
    @Derived
    default boolean isPriced() {
        return settleDifference() > 0;
    }
    
    String customer();
}
