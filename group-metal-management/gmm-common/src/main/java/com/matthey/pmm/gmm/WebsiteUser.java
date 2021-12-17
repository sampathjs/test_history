package com.matthey.pmm.gmm;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.StringUtils;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;
import org.jetbrains.annotations.Nullable;

@Immutable
@Style(stagedBuilder = true)
@JsonDeserialize(as = ImmutableWebsiteUser.class)
@JsonSerialize(as = ImmutableWebsiteUser.class)
interface WebsiteUser {
    
    @Nullable
    Integer id();
    
    String name();
    
    @Nullable
    String email();
    
    @Derived
    default boolean needPassword() {
        return StringUtils.isBlank(encryptedPassword());
    }
    
    @Nullable
    String encryptedPassword();
}
