package com.matthey.pmm.lims;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;


import java.util.List;

@Immutable
@JsonSerialize(as = ImmutableLimsApplicationArea.class)
@JsonDeserialize(as = ImmutableLimsApplicationArea.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "ActionUpdate")
public abstract class LimsApplicationArea {

	@Nullable
    public abstract Sender sender();

	@Nullable
    public abstract Receiver receiver();
    
	@Nullable
    public abstract String messageName();
    
	@Nullable
    public abstract String messageAction();
    
	@Nullable
    public abstract String messageDateTime();

	@Nullable
    public abstract String MessageNum();
    
    @Derived
    String exists() {
        return "True";
    }
    

    public abstract class Sender {
    	public abstract String LogicalID();
    }
    
    
    public abstract class Receiver {
    	public abstract String LogicalID();	
    }
}
