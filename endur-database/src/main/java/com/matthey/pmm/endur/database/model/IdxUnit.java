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
    private int unitId;
    private String unitLabel;
    private int unitTypeId;
    private int userId;
    private LocalDateTime lastUpdate;
    private int versionNumber;
    private int massTypeId;
    private int kilogramMassUnit;
    private int m3VolumeUnit;
}
