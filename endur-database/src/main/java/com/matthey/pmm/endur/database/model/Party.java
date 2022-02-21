package com.matthey.pmm.endur.database.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Data
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "party")
public class Party {

    @Id
    private Integer partyId;
    private Integer intExt;
    private Integer partyClass;
    private Integer partyStatus;
    private String shortName;
    private String longName;
    private Integer inputterId;
    private Integer authoriserId;
    private LocalDateTime lastUpdate;
    private Integer partyVersion;
    private Integer defaultPortfolioId;
    private Integer agencyActivities;
    private Integer linkedPartyId;
}
