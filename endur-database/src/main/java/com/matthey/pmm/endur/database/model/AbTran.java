package com.matthey.pmm.endur.database.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "ab_tran")
public class AbTran {

    @Id
    private int tranNum;

    private int tranGroup;
    private int dealTrackingNum;
    private int tranType;
    private int tranStatus;
    private int assetType;
    private int insNum;
    private int insType;
    private int insClass;
    private int toolset;
    private short buySell;
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "internal_bunit")
    private Party internalBunit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "internal_lentity")
    private Party internalLentity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "external_bunit")
    private Party externalBunit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "external_lentity")
    private Party externalLentity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "internal_portfolio")
    private Portfolio internalPortfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "external_portfolio")
    private Portfolio externalPortfolio;

    private int internalContact;
    private int externalContact;
    private String book;
    private LocalDateTime tradeDate;
    private LocalDateTime inputDate;
    private LocalDateTime settleDate;
    private float position;
    private float price;
    private float rate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency")
    private Currency currency;

    private int cflowType;
    private int versionNumber;
    private float proceeds;
    private float mvalue;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personnel_id")
    private Personnel personnel;

    private LocalDateTime lastUpdate;
    private int insSubType;
    private int brokerId;
    private LocalDateTime startDate;
    private LocalDateTime maturityDate;
    private LocalDateTime perpetualPosDate;
    private LocalDateTime tradeTime;
    private int offsetTranNum;
    private int offsetTranType;
    private int tradeFlag;
    private int currentFlag;
    private int fxYldBasis1;
    private int fxYldBasis2;
    private short unit;
    private int lockUserId;
    private int lockType;
    private int idxGroup;
    private int idxSubgroup;
    private int templateTranNum;
    private int otcClearingBrokerId;
    private int baseInsType;
    private int longTradingStrategy;
    private int extTradingStrategy;
    private int passThruId;
    private int regulatoryReporting;
    private int altTranStatus;
    private int marketPxIndexId;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "tran_num", referencedColumnName = "tran_num")
    private Set<AbTranInfo> abTranInfos = new HashSet<>();
}
