package com.matthey.pmm.endur.database.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Date;

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
    private long internalBunit;
    private long internalLentity;
    private long externalBunit;
    private long externalLentity;
    private long internalPortfolio;
    private long externalPortfolio;
    private long internalContact;
    private long externalContact;
    private String book;
    private Date tradeDate;
    private Date inputDate;
    private Date settleDate;
    private float position;
    private float price;
    private float rate;
    private long currency;
    private long cflowType;
    private long versionNumber;
    private float proceeds;
    private float mvalue;
    private long personnelId;
    private Date lastUpdate;
    private long insSubType;
    private long brokerId;
    private Date startDate;
    private Date maturityDate;
    private Date perpetualPosDate;
    private Date tradeTime;
    private long offsetTranNum;
    private long offsetTranType;
    private long tradeFlag;
    private long currentFlag;
    private long fxYldBasis1;
    private long fxYldBasis2;
    private short unit;
    private long lockUserId;
    private long lockType;
    private long idx_group;
    private long idxSubgroup;
    private long templateTran_num;
    private long otcClearing_brokerId;
    private long baseInsType;
    private long longTradingStrategy;
    private long extTradingStrategy;
    private long passThruId;
    private long regulatory_reporting;
    private long altTranStatus;
    private long marketPxIndexId;
}
