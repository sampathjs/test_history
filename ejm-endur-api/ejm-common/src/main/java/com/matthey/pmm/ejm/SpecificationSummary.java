package com.matthey.pmm.ejm;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(stagedBuilder = true)
@JsonSerialize(as = ImmutableSpecificationSummary.class)
@JsonDeserialize(as = ImmutableSpecificationSummary.class)
@JacksonXmlRootElement(localName = "Specification")
public interface SpecificationSummary {

    String accountNumber();

    String metalCode();

    String fromDate();

    String toDate();

    String form();

    double purity();

    String batchNumber();

    String tradeType();

    String tradeDate();

    int tradeRef();

    double dispatchWeight();

    String dispatchWeightUnit();

    String dispatchDate();

    String countryOfOriginDescription();

    String countryOfOriginCode();

    String gldNumber();

    String sheetNumber();
}
