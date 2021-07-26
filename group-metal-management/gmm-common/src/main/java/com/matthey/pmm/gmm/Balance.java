package com.matthey.pmm.gmm;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(stagedBuilder = true)
@JsonDeserialize(as = ImmutableBalance.class)
@JsonSerialize(as = ImmutableBalance.class)
public interface Balance {
    
    String customer();
    
    Double currentBalance();
    
    Double shipmentVolume();
    
    @Nullable
    Integer shipmentWindow();
    
    @Nullable
    String basisOfAssumption();
}
