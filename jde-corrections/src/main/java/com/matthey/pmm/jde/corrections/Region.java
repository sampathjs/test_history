package com.matthey.pmm.jde.corrections;

import com.olf.openrisk.io.InvalidArgumentException;

import java.util.Arrays;

public enum Region {
    UK("United Kingdom"), US("United States"), HK("Hong Kong"), CN("China");
    
    public final String fullName;
    
    Region(String fullName) {
        this.fullName = fullName;
    }
    
    public static Region of(String fullName) {
        return Arrays.stream(values())
                .filter(value -> value.fullName.equals(fullName))
                .findFirst()
                .orElseThrow(() -> new InvalidArgumentException("invalid full name for the region: " + fullName));
    }
}
