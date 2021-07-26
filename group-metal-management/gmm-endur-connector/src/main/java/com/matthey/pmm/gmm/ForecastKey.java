package com.matthey.pmm.gmm;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(stagedBuilder = true)
@JsonDeserialize(as = ImmutableForecastKey.class)
@JsonSerialize(as = ImmutableForecastKey.class)
public interface ForecastKey {
    String group();
    
    String balanceDate();
    
    String metal();
    
    String user();
    
    String companyCode();
    
    String unit();
    
    @Nullable
    String comments();
    
    Double deliverable();
    
    @Nullable
    String createTime();
}
