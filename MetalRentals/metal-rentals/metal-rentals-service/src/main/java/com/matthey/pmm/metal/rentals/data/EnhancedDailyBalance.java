package com.matthey.pmm.metal.rentals.data;

import com.matthey.pmm.metal.rentals.Account;
import org.apache.commons.lang3.StringUtils;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

@Immutable
@Style(stagedBuilder = true)
public interface EnhancedDailyBalance {

    Account account();

    String date();

    String metal();

    Double balance();

    Double balanceInTOz();

    @Derived
    default String accountGroup() {
        return account().group();
    }

    @Derived
    default String interestKey() {
        return StringUtils.isEmpty(account().internalBorrowings()) ? account().name() : "";
    }
}
