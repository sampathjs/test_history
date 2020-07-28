package com.matthey.pmm.ejm;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(stagedBuilder = true)
@JsonSerialize(as = ImmutableStatement.class)
@JsonDeserialize(as = ImmutableStatement.class)
@JacksonXmlRootElement(localName = "Statement")
public interface Statement {

    String accountNumber();

    int year();

    String month();

    String type();

    String documentPath();
}
