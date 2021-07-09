package com.matthey.pmm.metal.transfers.service;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.matthey.pmm.metal.transfers.WebsiteUser;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(stagedBuilder = true)
@JsonDeserialize(as = ImmutableSimplifiedWebsiteUser.class)
@JsonSerialize(as = ImmutableSimplifiedWebsiteUser.class)
public interface SimplifiedWebsiteUser {

    static SimplifiedWebsiteUser from(WebsiteUser user) {
        return ImmutableSimplifiedWebsiteUser.builder()
                .username(user.userFullName())
                .needPassword(user.needPassword())
                .build();
    }

    String username();

    boolean needPassword();
}
