package com.matthey.pmm.apdp.pricing.window;

import org.immutables.value.Value.Immutable;

import java.time.LocalDate;

@Immutable
public interface UnmatchedDeal {
    
    int dealNum();
    
    LocalDate dealDate();
    
    PricingWindowKey pricingWindowKey();
}
