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
@JacksonXmlRootElement(localName = "EmailConfirmationAction")
public abstract class EmailConfirmationAction {

    @Auxiliary
    public abstract int documentId();

    @Nullable
    public abstract String actionIdConfirm();
    
    @Nullable
    @Auxiliary
    public abstract String actionIdDispute();


    @Nullable
    @Auxiliary
    public abstract String emailStatus();

    @Auxiliary
    public abstract int version();

    @Auxiliary
    public abstract int currentFlag();    
    
    @Nullable
    @Auxiliary
    public abstract String insertedAt();

    @Nullable
    @Auxiliary
    public abstract String lastUpdate();
}
