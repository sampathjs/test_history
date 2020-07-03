package com.matthey.pmm.metal.rentals;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.StringUtils;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Style;
import org.jetbrains.annotations.Nullable;

import static org.immutables.value.Value.Immutable;

@Immutable
@Style(stagedBuilder = true)
@JsonDeserialize(as = ImmutableAccount.class)
@JsonSerialize(as = ImmutableAccount.class)
public abstract class Account {

    public abstract String name();

    public abstract String type();

    public abstract String reportingUnit();

    public abstract String preferredCurrency();

    public abstract String owner();

    public abstract String holder();

    @Nullable
    public abstract String internalBorrowings();

    @Derived
    public String group() {
        return StringUtils.isEmpty(internalBorrowings()) ? name().replaceAll("/.+", "") : internalBorrowings();
    }
}
