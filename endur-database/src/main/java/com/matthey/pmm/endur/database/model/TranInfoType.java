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
    private int typeId;
    private String typeName;
    private int dataType;
    private int personnelId;
    private LocalDateTime lastUpdate;
    private short insOrTran;
    private short requiredFlag;
    private String defaultValue;
    private int pickListId;
    private int offsetInfoId;
    private int secGroupId;
    private int auditFlag;
    private int displayOrder;
    private short readOnly;
    private short saveToDb;
    private short multiSelectFlag;
    private String userTableName;
    private String tranNumColName;
    private String userIdColName;
    private String tranDbVersionColName;
    private String dateModifiedColName;
    private int udwDefinitionId;
    private String userTableValueColName;
    private String insNumColName;
    private String paramSeqNumColName;
    private String userTableValueFormatStr;

}
