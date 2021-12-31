package com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * A file on the database.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableDatabaseFileTo.class)
@JsonDeserialize(as = ImmutableDatabaseFileTo.class)
@JsonRootName (value = "file")
@ApiModel(value = "DatabaseFile", description = "Object containing file on the database, emulating a file directory to some degree. Wraps around a BLOB. ")
public abstract class DatabaseFileTo {
	/**
	 * TOMS side ID.
	 * @return
	 */
	@ApiModelProperty(value = "The order management system internal unique ID for a single database file.",
			allowEmptyValue = false,
			required = true)
    public abstract long id();
   
    @Auxiliary
	@ApiModelProperty(value = "The name of the file on the database, as belonging to the parent directory specified in path",
		allowEmptyValue = false,
		required = true)
    public abstract String name();
    
    @Auxiliary
	@ApiModelProperty(value = "The name of the file on the database, as belonging to the parent directory specified in path",
		allowEmptyValue = false,
		required = true)
    public abstract String path();
    
    @Auxiliary
	@ApiModelProperty(value = "The file type represented as an ID of a reference having reference type #31 (File Type): 272(PDF), 273(Microsoft Word Document), 274 (Microsoft Excel Spreadsheet), 275(Plain Text File)",
		allowEmptyValue = false,
		required = true,
		allowableValues = "272, 273, 274, 275")
    public abstract long idFileType();
    
    @Auxiliary
	@ApiModelProperty(value = "The status of the file (deleted, or valid). Allowed values are of reference type #35 (Lifecycle Status): 290(Authorisation Pending), 291(Authorised and Active), 292 (Authorised and Inactive), 293(Deleted)",
		allowEmptyValue = false,
		required = true,
		allowableValues = "290, 291, 292, 293")
    public abstract long idLifecycle();
    
    @Auxiliary
	@ApiModelProperty(value = "Base64 encoded binary content of the file.",
		allowEmptyValue = false,
		required = true)
    public abstract String fileContent();
    
    @Auxiliary
	@ApiModelProperty(value = "Timestamp of the creation time of the file, assigned by the backend.",
		allowEmptyValue = false,
		required = true)
    public abstract String createdAt();
	
    @Auxiliary
	@ApiModelProperty(value = "The ID of the user who has created the file, assigned by the backend.",
		allowEmptyValue = false,
		required = true)    
    public abstract long idCreatedByUser();
	
    @Auxiliary
	@ApiModelProperty(value = "Timestamp of the last update of the file, assigned by the backend.",
		allowEmptyValue = false,
		required = true)
    public abstract String lastUpdate();
	
    @Auxiliary
	@ApiModelProperty(value = "The ID of the user who has updated the file, assigned by the backend.",
		allowEmptyValue = false,
		required = true)       
    public abstract long idUpdatedByUser();
}