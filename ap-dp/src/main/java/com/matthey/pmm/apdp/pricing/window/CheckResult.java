package com.matthey.pmm.apdp.pricing.window;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(stagedBuilder = true)
public interface CheckResult {
    
    String pricingType();
    
    String dealNum();
    
    String customer();
    
    String dealDate();
    
    String expiryDate();
    
    int numOfDaysToExpiry();
    
    double unmatchedVolume();
}
