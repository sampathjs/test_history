package com.matthey.pmm.gmm;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(stagedBuilder = true)
@JsonDeserialize(as = ImmutableGroup.class)
@JsonSerialize(as = ImmutableGroup.class)
public interface Group {
    
    String name();
    
    String companyCode();
}
