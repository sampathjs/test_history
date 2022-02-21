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
    private Integer unitId;
    private String unitLabel;
    private Integer unitTypeId;
    private Integer userId;
    private LocalDateTime lastUpdate;
    private Integer versionNumber;
    private Integer massTypeId;
    private Integer kilogramMassUnit;
    private Integer m3VolumeUnit;
}
