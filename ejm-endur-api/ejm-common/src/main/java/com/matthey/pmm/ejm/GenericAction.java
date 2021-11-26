package com.matthey.pmm.ejm;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.util.Date;

@Immutable
@JsonSerialize(as = ImmutableGenericAction.class)
@JsonDeserialize(as = ImmutableGenericAction.class)
@JacksonXmlRootElement(localName = "GenericAction")
public abstract class GenericAction {

    @Nullable
    public abstract String actionId();

    @Nullable
    @Auxiliary
    public abstract String actionConsumer();
    
    @Nullable
    @Auxiliary
    public abstract String responseMessage();

    @Nullable
    @Auxiliary
    public abstract String createdAt();

    @Nullable
    @Auxiliary
    public abstract String expiresAt();
    
}
