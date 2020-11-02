package com.matthey.pmm.apdp.pricing.window;

import org.immutables.value.Value.Immutable;

@Immutable
public interface PricingWindow {
    
    PricingWindowKey pricingWindowKey();
    
    int numOfDays();
}
