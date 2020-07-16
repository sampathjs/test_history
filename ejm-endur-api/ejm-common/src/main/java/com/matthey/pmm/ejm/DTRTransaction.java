package com.matthey.pmm.ejm;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(stagedBuilder = true)
@JsonSerialize(as = ImmutableDTRTransaction.class)
@JsonDeserialize(as = ImmutableDTRTransaction.class)
@JacksonXmlRootElement(localName = "DTRDetail")
public interface DTRTransaction {

    String accountNumber();

    String contraAccountName();

    String contraAccountNumber();

    String contraCustomerReference();

    String contraAuthorisationDate();

    double contraWeight();

    String contraWeightUnit();

    String metalCode();

    int tradeRef();

    String hasSpecifications();

    String weightUnit();

    String tradeType();

    int leg();

    String tradingLocation();

    double weight();

    String tradeDate();

    String valueDate();

    String customerRef();
}
