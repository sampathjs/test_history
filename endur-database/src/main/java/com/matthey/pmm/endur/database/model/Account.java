package com.matthey.pmm.endur.database.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "account")
public class Account {

    @Id
    private Integer accountId;
    private Integer accountType;
    private String accountName;
    private String accountNumber;
    private Integer holderId;
    private String description;
    private Integer userId;
    private LocalDateTime lastUpdate;
    private Integer accountVersion;
    private Integer accountStatus;
    private Integer accountClass;
    private Short onBalSheetFlag;
    private Short allowMultiUnits;
    private Integer linkedAccountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;
    
    private Integer clearingFlag;
    private String accountLegalName;
    private Integer accountCountry;
    private Integer treasuryManager;
    private Integer businessUnitOwner;
    private LocalDateTime dateOpened;
    private LocalDateTime dateClosed;
    private String generalLedgerAccount;
    private Integer baseCurrency;
    private Integer sweepEnabledFlag;
    private Integer nostroMemoFlag;
    private Integer plinkAccountId;
    private String accountIban;
    private Integer balReportingTypeId;
    private Integer balReconcileFlag;
    private Integer autoBalanceFlag;
    private LocalDateTime lastStmtEndDate;
    private Integer nextStmtEndDateSeq;
    private Integer stmtHoliday;
    private Integer stmtDateAdjustMethod;
    private Integer stmtFormat;

}
