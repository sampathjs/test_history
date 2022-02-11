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
    private int idNumber;
    private String name;
    private String longName;
    private int portfolioType;
    private int restricted;
    private int authoriserId;
    private LocalDateTime lastUpdate;
    private int portfolioVersion;
    private int requiresStrategy;

}
