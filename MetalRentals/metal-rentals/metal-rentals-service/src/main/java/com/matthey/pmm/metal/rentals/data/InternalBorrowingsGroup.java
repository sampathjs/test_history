package com.matthey.pmm.metal.rentals.data;

import org.immutables.value.Value.Immutable;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Immutable
public interface InternalBorrowingsGroup {

    static Optional<InternalBorrowingsGroup> from(String groupName) {
        final String GROUP_COMPANY_BORROWINGS_GROUP_NAME_PATTERN = ".+@.+";

        if (groupName.matches(GROUP_COMPANY_BORROWINGS_GROUP_NAME_PATTERN)) {
            return Optional.empty();
        } else {
            String[] regions = groupName.split("-");
            checkArgument(regions.length == 2, "invalid group name for internal borrowings: " + groupName);
            return Optional.of(ImmutableInternalBorrowingsGroup.builder()
                                       .holderRegion(regions[0])
                                       .ownerRegion(regions[1])
                                       .build());
        }
    }

    String holderRegion();

    String ownerRegion();
}
