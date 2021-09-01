package com.matthey.pmm.jde.corrections;

import java.time.LocalDateTime;

import static org.immutables.value.Value.Immutable;
import static org.immutables.value.Value.Style;

@Immutable
@Style(stagedBuilder = true)
public interface RunLog {
    
    int extractionId();
    
    LedgerType interfaceMode();
    
    Region region();
    
    int internalBU();
    
    int dealNum();
    
    int docNum();
    
    int tradeDate();
    
    int metalValueDate();
    
    int currencyValueDate();
    
    String accountNum();
    
    double qtyToz();
    
    double ledgerAmount();
    
    double taxAmount();
    
    String debitCredit();
    
    String ledgerType();
    
    LocalDateTime timeIn();
    
    int docDate();
    
    int tranNum();
}
