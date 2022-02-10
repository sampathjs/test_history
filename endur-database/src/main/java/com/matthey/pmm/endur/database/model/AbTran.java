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
    private long tranNum;

    private long tranGroup;
    private long dealTrackingNum;
    private long tranType;
    private long tranStatus;
    private long assetType;
    private long insNum;
    private long insType;
    private long insClass;
    private long toolset;
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

    private long internalContact;
    private long externalContact;
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

    private long cflowType;
    private long versionNumber;
    private float proceeds;
    private float mvalue;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personnel_id")
    private Personnel personnel;

    private LocalDateTime lastUpdate;
    private long insSubType;
    private long brokerId;
    private LocalDateTime startDate;
    private LocalDateTime maturityDate;
    private LocalDateTime perpetualPosDate;
    private LocalDateTime tradeTime;
    private long offsetTranNum;
    private long offsetTranType;
    private long tradeFlag;
    private long currentFlag;
    private long fxYldBasis1;
    private long fxYldBasis2;
    private short unit;
    private long lockUserId;
    private long lockType;
    private long idxGroup;
    private long idxSubgroup;
    private long templateTranNum;
    private long otcClearingBrokerId;
    private long baseInsType;
    private long longTradingStrategy;
    private long extTradingStrategy;
    private long passThruId;
    private long regulatoryReporting;
    private long altTranStatus;
    private long marketPxIndexId;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "tran_num", referencedColumnName = "tran_num")
    private Set<AbTranInfo> abTranInfos = new HashSet<>();
}
