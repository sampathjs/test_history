package com.matthey.pmm.apdp.pricing.window;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@Immutable
public interface PricingWindowKey {
    
    @Parameter
    int customerId();
    
    @Parameter
    String pricingType();
    
    @Parameter
    int metalId();
}
