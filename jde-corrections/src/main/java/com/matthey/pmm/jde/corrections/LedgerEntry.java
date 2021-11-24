package com.matthey.pmm.jde.corrections;

import org.immutables.value.Value.Auxiliary;

public interface LedgerEntry {
    
    int extractionId();
    
    Region region();
    
    @Auxiliary
    String payload();
}
