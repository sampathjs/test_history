package com.matthey.pmm.endur.database.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Data
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "idx_unit")
public class IdxUnit {

    @Id
    private long unitId;
    private String unitLabel;
    private long unitTypeId;
    private long userId;
    private LocalDateTime lastUpdate;
    private long version_number;
    private long massTypeId;
    private long kilogramMassUnit;
    private long m3_volumeUnit;
}
