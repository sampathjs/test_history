package com.matthey.pmm.mock;

import static org.immutables.value.Value.Immutable;

@Immutable
public interface IndexClosingDatasetSelector {

    String index();

    String refSource();

    String startDate();

    String endDate();
}
