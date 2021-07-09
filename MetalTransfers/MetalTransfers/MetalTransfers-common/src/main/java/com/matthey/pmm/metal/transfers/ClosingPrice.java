package com.matthey.pmm.metal.transfers;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@Immutable
@JsonDeserialize(as = ImmutableClosingPrice.class)
@JsonSerialize(as = ImmutableClosingPrice.class)
public interface ClosingPrice {

    @Parameter
    String date();

    @Parameter
    Double price();
}
