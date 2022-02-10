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
    private int partyId;
    private int intExt;
    private int partyClass;
    private int partyStatus;
    private String shortName;
    private String longName;
    private int inputterId;
    private int authoriserId;
    private LocalDateTime lastUpdate;
    private int partyVersion;
    private int defaultPortfolioId;
    private int agencyActivities;
    private int linkedPartyId;
}
