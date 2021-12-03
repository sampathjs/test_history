package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * A file on the database.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableDatabaseFileTo.class)
@JsonDeserialize(as = ImmutableDatabaseFileTo.class)
@JsonRootName (value = "file")
public abstract class DatabaseFileTo {
	/**
	 * TOMS side ID.
	 * @return
	 */
    public abstract long id();
   
    @Auxiliary
    public abstract String name();
    
    @Auxiliary
    public abstract String path();
    
    @Auxiliary
    public abstract long idFileType();
    
    @Auxiliary
    public abstract long idLifecycle();
    
    @Auxiliary
    public abstract String fileContent();
    
    @Auxiliary
    public abstract String createdAt();
	
    @Auxiliary
    public abstract long idCreatedByUser();
	
    @Auxiliary
    public abstract String lastUpdate();
	
    @Auxiliary
    public abstract long idUpdatedByUser();
}