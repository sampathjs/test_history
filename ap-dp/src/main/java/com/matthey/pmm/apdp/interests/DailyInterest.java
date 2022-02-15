package com.matthey.pmm.apdp.interests;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

import java.time.LocalDate;

@Immutable
public interface DailyInterest {
    
    @Parameter
    String customer();
    
    @Parameter
    LocalDate date();
    
    @Parameter
    double interest();
}
