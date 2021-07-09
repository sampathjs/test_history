package com.matthey.pmm.metal.transfers;

import com.matthey.pmm.metal.transfers.Account;

public class TestUtils {

    public static Account genAccount(String name, String internalBorrowings, boolean forCN) {
        return ImmutableAccount.builder()
                .name(name)
                .type("Vostro")
                .reportingUnit("TOz")
                .preferredCurrency("USD")
                .owner("JM PMM US")
                .holder(forCN ? "JM PMM CN" : "JM PMM UK")
                .internalBorrowings(internalBorrowings)
                .build();
    }

    public static Account genAccountForInternalBorrowings(String owner, String holder) {
        return ImmutableAccount.builder()
                .name(owner + "@" + holder)
                .type("Nostro")
                .reportingUnit("TOz")
                .preferredCurrency("USD")
                .owner(owner)
                .holder(holder)
                .internalBorrowings("UK-US")
                .build();
    }

    public static Account genAccount(String name) {
        return genAccount(name, "TOz", "USD");
    }

    public static Account genAccount(String name, String unit, String currency) {
        return ImmutableAccount.builder()
                .name(name)
                .type("Vostro")
                .reportingUnit(unit)
                .preferredCurrency(currency)
                .owner("owner")
                .holder("holder")
                .build();
    }
}
