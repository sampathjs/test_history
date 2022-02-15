package com.matthey.pmm.apdp.reports;

import org.immutables.value.Value;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;

@Immutable
@Value.Style(stagedBuilder = true)
public interface CustomerDetails {
    
    String customer();
    
    @Derived
    default double adjustmentValueUsd() {
        return deferredValueUsd() + pricedValueUsd() + unpricedValueUsd();
    }
    
    double deferredValueUsd();
    
    double pricedValueUsd();
    
    @Derived
    default double unpricedValueUsd() {
        return unpricedAmount() * avgPriceUsd();
    }
    
    @Derived
    default double avgPriceUsd() {
        return deferredAmount() == 0 ? 0 : deferredValueUsd() / deferredAmount();
    }
    
    @Derived
    default double unpricedAmount() {
        return Math.abs(deferredAmount()) - pricedAmount();
    }
    
    double deferredAmount();
    
    double pricedAmount();
    
    @Derived
    default double adjustmentValueHkd() {
        return deferredValueHkd() + pricedValueHkd() + unpricedValueHkd();
    }
    
    double deferredValueHkd();
    
    double pricedValueHkd();
    
    @Derived
    default double unpricedValueHkd() {
        return unpricedAmount() * avgPriceHkd();
    }
    
    @Derived
    default double avgPriceHkd() {
        return deferredAmount() == 0 ? 0 : deferredValueHkd() / deferredAmount();
    }
}
