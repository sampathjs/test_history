package com.matthey.pmm.endur.database.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "abTran")
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
    private long internalBunit;
    private long internalLentity;
    private long externalBunit;
    private long externalLentity;
    private long internalPortfolio;
    private long externalPortfolio;
    private long internalContact;
    private long externalContact;
    private String book;
    private LocalDateTime tradeDate;
    private LocalDateTime inputDate;
    private LocalDateTime settleDate;
    private float position;
    private float price;
    private float rate;
    private long currency;
    private long cflowType;
    private long versionNumber;
    private float proceeds;
    private float mvalue;
    private long personnelId;
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
    @JoinColumn(name = "tranNum", referencedColumnName = "tranNum")
    private Set<AbTranInfo> abTranInfos = new HashSet<>();
}
