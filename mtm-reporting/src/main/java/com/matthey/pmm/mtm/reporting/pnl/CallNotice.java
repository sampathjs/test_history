package com.matthey.pmm.mtm.reporting.pnl;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(stagedBuilder = true)
public interface CallNotice {
    
    int tranNum();
    
    String paymentCcy();
    
    String notionalCcy();
    
    double currentNotional();
    
    double previousNotional();
}
