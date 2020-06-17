package com.matthey.pmm.metal.rentals;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;

public class PropertyChecker {

    private static final Logger logger = LoggerFactory.getLogger(PropertyChecker.class);

    public static <T> T checkAndReturn(T returnValue, boolean expression, String propertyName) {
        checkArgument(expression, "invalid value for " + propertyName + ": " + returnValue);
        logger.info("value of {}: {}", propertyName, returnValue);
        return returnValue;
    }
}
