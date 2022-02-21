package com.matthey.pmm.endur.database.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Data
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "tran_info_types")
public class TranInfoType {

    @Id
    private Integer typeId;
    private String typeName;
    private Integer dataType;
    private Integer personnelId;
    private LocalDateTime lastUpdate;
    private Short insOrTran;
    private Short requiredFlag;
    private String defaultValue;
    private Integer pickListId;
    private Integer offsetInfoId;
    private Integer secGroupId;
    private Integer auditFlag;
    private Integer displayOrder;
    private Short readOnly;
    private Short saveToDb;
    private Short multiSelectFlag;
    private String userTableName;
    private String tranNumColName;
    private String userIdColName;
    private String tranDbVersionColName;
    private String dateModifiedColName;
    private Integer udwDefinitionId;
    private String userTableValueColName;
    private String insNumColName;
    private String paramSeqNumColName;
    private String userTableValueFormatStr;

}
