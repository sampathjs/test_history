package com.matthey.pmm.apdp.pricing.window;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

import java.time.LocalDate;

@Immutable
@Style(stagedBuilder = true)
public interface UnmatchedDeal {
    
    int dealNum();
    
    LocalDate dealDate();
    
    PricingWindowKey pricingWindowKey();
    
    double unmatchedVolume();
}
