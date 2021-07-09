package com.matthey.pmm.metal.transfers;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@Immutable
@JsonDeserialize(as = ImmutableInterestRate.class)
@JsonSerialize(as = ImmutableInterestRate.class)
public interface InterestRate {

    @Parameter
    String metal();

    @Parameter
    Double rate();
}
