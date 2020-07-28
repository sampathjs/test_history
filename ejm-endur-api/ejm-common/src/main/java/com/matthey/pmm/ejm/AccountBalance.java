package com.matthey.pmm.ejm;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(stagedBuilder = true)
@JsonSerialize(as = ImmutableAccountBalance.class)
@JsonDeserialize(as = ImmutableAccountBalance.class)
@JacksonXmlRootElement(localName = "AccountBalance")
public interface AccountBalance {

    @JacksonXmlProperty(localName = "account_number")
    String accountNumber();

    String date();

    String metal();

    double balance();

    String hasSpecifications();

    String hasTransactions();

    String weightUnit();
}
