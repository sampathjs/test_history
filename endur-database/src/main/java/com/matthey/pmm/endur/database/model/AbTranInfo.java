package com.matthey.pmm.endur.database.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "ab_tran_info")
@IdClass(AbTranInfo.AbTranInfoPrimaryKey.class)
public class AbTranInfo {

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class AbTranInfoPrimaryKey implements Serializable {
        private long tranNum;
        private TranInfoType tranInfoType;
    }

    @Id
    private long tranNum;
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id")
    private TranInfoType tranInfoType;

    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personnel_id")
    private Personnel personnel;

    private LocalDateTime lastUpdate;

}
