package com.matthey.pmm.ejm;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(stagedBuilder = true)
@JsonSerialize(as = ImmutableSpecification.class)
@JsonDeserialize(as = ImmutableSpecification.class)
@JacksonXmlRootElement(localName = "Specification")
public interface Specification {

    int tradeRef();

    String documentPath();
}
