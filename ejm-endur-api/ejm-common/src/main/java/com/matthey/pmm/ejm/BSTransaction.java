package com.matthey.pmm.ejm;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(stagedBuilder = true)
@JsonSerialize(as = ImmutableBSTransaction.class)
@JsonDeserialize(as = ImmutableBSTransaction.class)
@JacksonXmlRootElement(localName = "BSDetail")
public interface BSTransaction {

    String accountNumber();

    int tradeRef();

    String tradeType();

    String tradeDate();

    int leg();

    String metalCode();

    String valueDate();

    double weight();

    String weightUnit();

    double unitPrice();

    String dealCurrency();

    String invoiceDate();

    String invoiceNumber();

    String statementDate();

    String paymentDate();

    String tradingLocation();

    double amountDue();

    String customerRef();
}
