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
@Table(schema = DbConstants.SCHEMA_NAME, name = "ab_tran_info")
@IdClass(AbTranInfo.AbTranInfoPrimaryKey.class)
public class AbTranInfo {

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class AbTranInfoPrimaryKey implements Serializable {
        private long tranNum;
        private long typeId;
    }

    @Id
    private long tranNum;
    @Id
    private long typeId;
    private String value;
    private long personnelId;
    private LocalDateTime lastUpdate;

}
