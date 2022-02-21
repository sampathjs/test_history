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
    @Column(name = "tran_num")
    private Integer tranNum;

    private Integer tranGroup;
    private Integer dealTrackingNum;
    private Integer tranType;
    private Integer tranStatus;
    private Integer assetType;
    private Integer insNum;
    private Integer insType;
    private Integer insClass;
    private Integer toolset;
    private Short buySell;
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

    private Integer internalContact;
    private Integer externalContact;
    private String book;
    private LocalDateTime tradeDate;
    private LocalDateTime inputDate;
    private LocalDateTime settleDate;
    private Float position;
    private Float price;
    private Float rate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency")
    private Currency currency;

    private Integer cflowType;
    private Integer versionNumber;
    private Float proceeds;
    private Float mvalue;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personnel_id")
    private Personnel personnel;

    private LocalDateTime lastUpdate;
    private Integer insSubType;
    private Integer brokerId;
    private LocalDateTime startDate;
    private LocalDateTime maturityDate;
    private LocalDateTime perpetualPosDate;
    private LocalDateTime tradeTime;
    private Integer offsetTranNum;
    private Integer offsetTranType;
    private Integer tradeFlag;
    private Integer currentFlag;
    @Column(name = "fx_yld_basis_1")
    private Integer fxYldBasis1;
    @Column(name = "fx_yld_basis_2")
    private Integer fxYldBasis2;
    private Short unit;
    private Integer lockUserId;
    private Integer lockType;
    private Integer idxGroup;
    private Integer idxSubgroup;
    private Integer templateTranNum;
    private Integer otcClearingBrokerId;
    private Integer baseInsType;
    private Integer intTradingStrategy;
    private Integer extTradingStrategy;
    private Integer passThruId;
    private Integer regulatoryReporting;
    private Integer altTranStatus;
    private Integer marketPxIndexId;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "tran_num", referencedColumnName = "tran_num")
    private Set<AbTranInfo> abTranInfos = new HashSet<>();
}
