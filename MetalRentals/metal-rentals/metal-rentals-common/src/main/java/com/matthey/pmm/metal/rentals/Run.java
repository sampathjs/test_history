package com.matthey.pmm.metal.rentals;

import static com.matthey.pmm.metal.rentals.RunResult.Successful;

public abstract class Run {

    public abstract String user();

    public abstract String runTime();

    public abstract String statementMonth();

    public abstract RunResult result();

    public boolean isSuccessful() {
        return result() == Successful;
    }
}
