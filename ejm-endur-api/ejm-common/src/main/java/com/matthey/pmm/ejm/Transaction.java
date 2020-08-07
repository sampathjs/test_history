package com.matthey.pmm.ejm;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(stagedBuilder = true)
@JsonSerialize(as = ImmutableTransaction.class)
@JsonDeserialize(as = ImmutableTransaction.class)
@JacksonXmlRootElement(localName = "Transaction")
public interface Transaction {

    String accountNumber();

    String metalCode();

    int tradeRef();

    String fromDate();

    String toDate();

    int leg();

    String tradeType();

    String customerRef();

    double weight();

    String tradeDate();

    String valueDate();
}
