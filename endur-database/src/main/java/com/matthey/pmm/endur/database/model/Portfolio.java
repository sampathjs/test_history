package com.matthey.pmm.endur.database.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Data
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "portfolio")
public class Portfolio {

    @Id
    private Integer idNumber;
    private String name;
    private String longName;
    private Integer portfolioType;
    private Integer restricted;
    private Integer authoriserId;
    private LocalDateTime lastUpdate;
    private Integer portfolioVersion;
    private Integer requiresStrategy;

}
