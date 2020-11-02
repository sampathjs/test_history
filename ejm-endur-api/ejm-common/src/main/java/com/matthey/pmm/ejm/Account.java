package com.matthey.pmm.ejm;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

@Immutable
@JsonSerialize(as = ImmutableAccount.class)
@JsonDeserialize(as = ImmutableAccount.class)
@JacksonXmlRootElement(localName = "Response")
public abstract class Account {

    @Nullable
    public abstract String gtAccountNumber();

    @Nullable
    @Auxiliary
    public abstract String partyLongName();

    @Nullable
    @Auxiliary
    public abstract String partyShortName();

    @Derived
    public String accountName() {
        return defaultIfBlank(partyLongName(), defaultIfBlank(partyShortName(), "Not Linked"));
    }

    @Derived
    String exists() {
        return "True";
    }
}
