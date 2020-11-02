package com.matthey.pmm.apdp;

import org.immutables.value.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Value.Immutable
@Value.Style(stagedBuilder = true)
public interface DealLink {
    
    int buyDealNum();
    
    int sellDealNum();
    
    double matchVolume();
    
    LocalDate matchDate();
    
    int metalType();
    
    int buyInsType();
    
    double sellPrice();
    
    double settleAmount();
    
    long sellEventNum();
    
    int invoiceDocNum();
    
    String invoiceStatus();
    
    LocalDateTime lastUpdate();
}
