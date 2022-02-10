package com.matthey.pmm.endur.database.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Data
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "tranNotepad")
public class TranNotepad {

    @Id
    private int tranNum;
    private int noteType;
    private int lineNum;
    private String lineText;
    private int userId;
    private int commentNum;
    private LocalDateTime commentDate;
    private LocalDateTime timeStamp;
    private int insNum;
    private LocalDateTime commentEndDate;
}
