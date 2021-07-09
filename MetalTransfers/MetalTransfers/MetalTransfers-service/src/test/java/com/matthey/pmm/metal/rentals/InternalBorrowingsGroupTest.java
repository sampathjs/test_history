package com.matthey.pmm.metal.transfers;

import org.junit.jupiter.api.Test;

import com.matthey.pmm.metal.transfers.data.InternalBorrowingsGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class InternalBorrowingsGroupTest {

    @Test
    public void when_group_is_for_group_company_borrowings() {
        var sut = InternalBorrowingsGroup.from("JM REFINING UK@PMM UK-ROY/ING");
        assertThat(sut).isEmpty();
    }

    @Test
    public void when_group_is_for_internal_borrowings() {
        var sut = InternalBorrowingsGroup.from("UK-US");
        assertThat(sut).isNotEmpty();
        assertThat(sut.get().holderRegion()).isEqualTo("UK");
        assertThat(sut.get().ownerRegion()).isEqualTo("US");
    }

    @Test
    public void when_group_name_is_invalid_for_internal_borrowings() {
        assertThatIllegalArgumentException().isThrownBy(() -> InternalBorrowingsGroup.from("UK"))
                .withMessage("invalid group name for internal borrowings: UK");
        assertThatIllegalArgumentException().isThrownBy(() -> InternalBorrowingsGroup.from("UK-US-HK"))
                .withMessage("invalid group name for internal borrowings: UK-US-HK");
    }
}