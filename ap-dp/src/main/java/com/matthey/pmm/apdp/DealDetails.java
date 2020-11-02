package com.matthey.pmm.apdp;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

import java.time.LocalDate;

@Immutable
@Style(stagedBuilder = true)
public interface DealDetails {
    
    int dealNum();
    
    double volumeInToz();
    
    double volumeLeftInToz();
    
    String matchStatus();
    
    LocalDate matchDate();
    
    int customerId();
    
    int metalType();
    
    boolean isSell();
    
    boolean isCancelled();
}
