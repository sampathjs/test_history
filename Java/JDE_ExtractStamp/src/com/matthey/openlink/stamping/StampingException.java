package com.matthey.openlink.stamping;

import com.openlink.util.logging.PluginLog;

/**
 * Plugin specific exception class.
 * 
 */
public class StampingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new stamping runtime exception.
     *
     * @param message the message
     */
    public StampingException(String message) {
        super(message);
        PluginLog.error(message);
    }

    /**
     * Instantiates a new stamping runtime exception.
     *
     * @param exception the exception
     */
    public StampingException(Exception exception) {
        super(exception);
        PluginLog.error(exception.getMessage());
    }

    /**
     * Instantiates a new stamping runtime exception.
     *
     * @param message the message
     * @param exception the exception
     */
    public StampingException(String message, Exception exception) {
        super(message, exception);
        PluginLog.error(message);
        PluginLog.error(exception.getMessage());
    }

}