package com.matthey.pmm.endur.database.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Data
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "currency")
public class Currency {

    @Id
    private int idNumber;
    private String name;
    private int defaultIndex;
    private int spotIndex;
    private int convention;
    private int holidayId;
    private int baseUnit;
    private int round;
    private String description;
    private int roundType;
    private int euro;
    private float euroConversion;
    private int currencyZone;
    private short euroScenarioFlag;
    private int advanceReleaseDays;
    private int userId;
    private LocalDateTime lastUpdate;
    private short preciousMetal;
    private short autoConversionFlag;
    private int versionNumber;
}
