package com.matthey.pmm.jde.corrections.connectors;

import com.openlink.util.constrepository.ConstRepository;

import java.time.LocalDate;

public class ConfigurationRetriever {
    
    public static LocalDate getStartDate() {
        try {
            return LocalDate.parse(new ConstRepository("JDE Corrections", "").getStringValue("Start Date"));
        } catch (Exception e) {
            throw new RuntimeException("cannot retrieve trade date from configuration", e);
        }
    }
}
