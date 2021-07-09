package com.matthey.pmm.metal.transfers;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(stagedBuilder = true)
@JsonDeserialize(as = ImmutableParty.class)
@JsonSerialize(as = ImmutableParty.class)
public interface Party {

    String name();

    String address();

    String telephone();

    String vatNumber();
}
