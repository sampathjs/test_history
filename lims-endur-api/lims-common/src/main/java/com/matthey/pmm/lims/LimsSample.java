package com.matthey.pmm.lims;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;


import java.util.List;

@Immutable
@JsonSerialize(as = ImmutableLimsSample.class)
@JsonDeserialize(as = ImmutableLimsSample.class)
@JacksonXmlRootElement(localName = "Response")
public abstract class LimsSample {

    public abstract String jmBatchId();

    @Nullable
    public abstract String sampleNumber();

    @Nullable
    public abstract String product();
    
    @Nullable
    public abstract List<LimsResult> result();

    @Derived
    String exists() {
        return "True";
    }
}
