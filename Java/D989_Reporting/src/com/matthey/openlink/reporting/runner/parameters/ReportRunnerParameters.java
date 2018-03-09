package com.matthey.openlink.reporting.runner.parameters;

@SuppressWarnings("serial")
public class ReportRunnerParameters extends RuntimeException {


    private final String reason;

    @Override
    public String getLocalizedMessage() {

        return reason;
    }

    public ReportRunnerParameters(String message, Throwable cause) {

        super(message, cause);
        reason = message;
    }

    public ReportRunnerParameters(String message) {

        super(message);
        reason = message;
    }

}
