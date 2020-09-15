package com.matthey.pmm.ejm;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(stagedBuilder = true)
@JsonSerialize(as = ImmutableDailyAccountBalance.class)
@JsonDeserialize(as = ImmutableDailyAccountBalance.class)
@JacksonXmlRootElement(localName = "AccountBalance")
public interface DailyAccountBalance {

    String accountNumber();

    String date();

    String metalCode();

    double balance();

    int numTransactions();
}
