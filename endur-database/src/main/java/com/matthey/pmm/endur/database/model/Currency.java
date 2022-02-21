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
    private Integer idNumber;
    private String name;
    private Integer defaultIndex;
    private Integer spotIndex;
    private Integer convention;
    private Integer holidayId;
    private Integer baseUnit;
    private Integer round;
    private String description;
    private Integer roundType;
    private Integer euro;
    private Float euroConversion;
    private Integer currencyZone;
    private Short euroScenarioFlag;
    private Integer advanceReleaseDays;
    private Integer userId;
    private LocalDateTime lastUpdate;
    private Short preciousMetal;
    private Short autoConversionFlag;
    private Integer versionNumber;
}
