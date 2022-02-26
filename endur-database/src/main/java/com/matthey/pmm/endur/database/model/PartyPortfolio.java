package com.matthey.pmm.endur.database.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "party_portfolio")
@IdClass(PartyPortfolio.PartyPortfolioPrimaryKey.class)
public class PartyPortfolio {

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class PartyPortfolioPrimaryKey implements Serializable {
        private Integer partyId;
        private Integer portfolioId;
    }

    @Id
    private Integer partyId;
    @Id
    private Integer portfolioId;
    private Integer userId;
    private Integer versionNumber;

}
