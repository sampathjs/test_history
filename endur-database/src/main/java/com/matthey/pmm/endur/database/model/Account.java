package com.matthey.pmm.endur.database.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "account")
public class Account {

    @Id
    private int accountId;
    private int accountType;
    private String accountName;
    private String accountNumber;
    private int holderId;
    private String description;
    private int userId;
    private LocalDateTime lastUpdate;
    private int accountVersion;
    private int accountStatus;
    private int accountClass;
    private short onBalSheetFlag;
    private short allowMultiUnits;
    private int linkedAccountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;
    
    private int clearingFlag;
    private String accountLegalName;
    private int accountCountry;
    private int treasuryManager;
    private int businessUnitOwner;
    private LocalDateTime dateOpened;
    private LocalDateTime dateClosed;
    private String generalLedgerAccount;
    private int baseCurrency;
    private int sweepEnabledFlag;
    private int nostroMemoFlag;
    private int plinkAccountId;
    private String accountIban;
    private int balReportingTypeId;
    private int balReconcileFlag;
    private int autoBalanceFlag;
    private LocalDateTime lastStmtEndDate;
    private int nextStmtEndDateSeq;
    private int stmtHoliday;
    private int stmtDateAdjustMethod;
    private int stmtFormat;

}
