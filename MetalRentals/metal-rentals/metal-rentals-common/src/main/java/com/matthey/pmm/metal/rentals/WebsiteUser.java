package com.matthey.pmm.metal.rentals;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.StringUtils;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(stagedBuilder = true)
@JsonDeserialize(as = ImmutableWebsiteUser.class)
@JsonSerialize(as = ImmutableWebsiteUser.class)
public abstract class WebsiteUser {

    abstract public Integer id();

    abstract public String userFullName();

    abstract public String email();

    abstract public String encryptedPassword();

    @Derived
    public boolean needPassword() {
        return StringUtils.isBlank(encryptedPassword());
    }
}
