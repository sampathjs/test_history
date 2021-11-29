package com.matthey.pmm.lims;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;


@Immutable
@JsonSerialize(as = ImmutableLimsResult.class)
@JsonDeserialize(as = ImmutableLimsResult.class)
@JacksonXmlRootElement(localName = "Response")
public abstract class LimsResult {


    public abstract String sampl00001();

    @Nullable
    public abstract String analysis();

    @Nullable
    public abstract String imputityName();

    @Nullable
    public abstract String units();
    
    @Nullable
    public abstract String formattedEntry();
    
    @Nullable
    public abstract String status();
    
    @Derived
    String exists() {
        return "True";
    }
}
