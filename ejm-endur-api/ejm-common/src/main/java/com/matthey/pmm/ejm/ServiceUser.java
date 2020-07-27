package com.matthey.pmm.ejm;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;
import org.immutables.value.Value.Style;

@Immutable
@Style(stagedBuilder = true)
@JsonDeserialize(as = ImmutableServiceUser.class)
@JsonSerialize(as = ImmutableServiceUser.class)
public abstract class ServiceUser {

    @Parameter
    abstract public String username();

    @Parameter
    abstract public String encryptedPassword();
}
