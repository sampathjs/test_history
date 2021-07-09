package com.matthey.pmm.metal.transfers.document;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.matthey.pmm.metal.transfers.CashDealBookingRun;
import com.matthey.pmm.metal.transfers.Run;
import com.matthey.pmm.metal.transfers.StatementEmailingRun;
import com.matthey.pmm.metal.transfers.StatementGeneratingRun;

import org.immutables.value.Value.Default;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;

import javax.annotation.Nullable;
import java.util.List;

@Immutable
@JsonDeserialize(as = ImmutableDocumentGeneratingResult.class)
@JsonSerialize(as = ImmutableDocumentGeneratingResult.class)
public interface DocumentGeneratingResult {

    @Nullable
    List<StatementGeneratingRun> statementGeneratingRuns();

    @Nullable
    List<StatementEmailingRun> statementEmailingRuns();

    @Nullable
    List<CashDealBookingRun> cashDealBookingRuns();

    @Default
    default boolean isInvoiceGeneratingOk() {
        return false;
    }

    @Derived
    default boolean isStatementGeneratingOk() {
        return isOk(statementGeneratingRuns());
    }

    @Derived
    default boolean isStatementEmailingOk() {
        return isOk(statementEmailingRuns());
    }

    @Derived
    default boolean isCashDealBookingOk() {
        return isOk(cashDealBookingRuns());
    }

    @Derived
    default boolean isMetalRentalsWorkflowOk() {
        return isCashDealBookingOk() && isInvoiceGeneratingOk();
    }

    private boolean isOk(List<? extends Run> runs) {
        return runs != null && runs.stream().allMatch(Run::isSuccessful);
    }

    @Derived
    default boolean isEverythingOk() {
        return isStatementGeneratingOk() && isStatementEmailingOk() && isCashDealBookingOk() && isInvoiceGeneratingOk();
    }
}
