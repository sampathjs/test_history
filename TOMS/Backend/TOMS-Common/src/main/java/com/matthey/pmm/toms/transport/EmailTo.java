package com.matthey.pmm.toms.transport;

import java.util.Set;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Email with the support for all generic email attributes plus additional
 * metadata and an email workflow support.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableEmailTo.class)
@JsonDeserialize(as = ImmutableEmailTo.class)
@JsonRootName (value = "email")
public abstract class EmailTo {  
    public abstract long id();   
    
    @Auxiliary
    public abstract long idSendAs();
    
    @Auxiliary
    public abstract String subject();
    
    @Auxiliary
    public abstract String body();
    
    @Auxiliary
    @Nullable
    public abstract Set<String> toList();
    
    @Auxiliary
    @Nullable
    public abstract Set<String> ccList();
    
    @Auxiliary
    @Nullable
    public abstract Set<String> bccList();
    
    @Auxiliary
    @Nullable
    public abstract Set<Long> attachments();
    
    @Auxiliary
    public abstract long idEmailStatus();
    
    @Auxiliary
    @Nullable    
    public abstract String errorMessage();
	
    @Auxiliary
    public abstract int retryCount();
	
    @Auxiliary
    public abstract String createdAt();
	
    @Auxiliary
    public abstract long idCreatedByUser();
	
    @Auxiliary
    public abstract String lastUpdate();
	
    @Auxiliary
    public abstract long idUpdatedByUser();
	
    @Auxiliary
    @Nullable
    public abstract Set<Long> associatedOrderIds();
}
