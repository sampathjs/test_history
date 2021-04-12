package com.matthey.pmm.jde.corrections;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(stagedBuilder = true)
public interface GeneralLedgerEntry extends LedgerEntry {
    
    int dealNum();
    
    int tranNum();
    
    int tranStatus();
}
