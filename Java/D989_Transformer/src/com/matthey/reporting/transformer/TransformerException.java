package com.matthey.reporting.transformer;

@SuppressWarnings("serial")
public class TransformerException extends RuntimeException {


    private final String reason;

    @Override
    public String getLocalizedMessage() {

        return reason;
    }

    @Override
    public String getMessage() {

        return reason;
    }

    public TransformerException(String message, Throwable cause) {

        super(message, cause);
        reason = message;
    }

    public TransformerException(String message) {

        super(message);
        reason = message;
    }
}
