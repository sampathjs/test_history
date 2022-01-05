package com.matthey.pmm.jde.corrections;

import static org.immutables.value.Value.Immutable;
import static org.immutables.value.Value.Parameter;
import static org.immutables.value.Value.Style;

@Immutable
@Style(stagedBuilder = true)
public interface LedgerExtraction {
    
    @Parameter
    Region region();
    
    @Parameter
    LedgerType ledgerType();
}
