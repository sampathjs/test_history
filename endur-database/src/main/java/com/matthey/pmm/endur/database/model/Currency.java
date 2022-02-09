package com.matthey.pmm.endur.database.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Data
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "currency")
public class Currency {

    @Id
    private long idNumber;
    private String name;
    private long defaultIndex;
    private long spotIndex;
    private long convention;
    private long holidayId;
    private long baseUnit;
    private long round;
    private String description;
    private long roundType;
    private long euro;
    private float euroConversion;
    private long currency_zone;
    private short euro_scenarioFlag;
    private long advanceReleaseDays;
    private long userId;
    private LocalDateTime lastUpdate;
    private short preciousMetal;
    private short autoConversionFlag;
    private long versionNumber;
}
