package com.matthey.pmm.metal.transfers;

import static com.matthey.pmm.metal.transfers.RunResult.Successful;

public abstract class Run {

    public abstract String user();

    public abstract String runTime();

    public abstract String statementMonth();

    public abstract RunResult result();

    public boolean isSuccessful() {
        return result() == Successful;
    }
}
