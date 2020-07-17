package com.matthey.pmm.metal.rentals;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(stagedBuilder = true)
@JsonDeserialize(as = ImmutablePartyContact.class)
@JsonSerialize(as = ImmutablePartyContact.class)
public interface PartyContact {

    String party();

    String contact();

    String email();
}
