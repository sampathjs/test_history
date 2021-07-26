package com.matthey.pmm.gmm;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Value.Immutable
@Value.Style(stagedBuilder = true)
@JsonDeserialize(as = ImmutableForecast.class)
@JsonSerialize(as = ImmutableForecast.class)
public interface Forecast {
    
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
    
    List<Balance> balances();
}
