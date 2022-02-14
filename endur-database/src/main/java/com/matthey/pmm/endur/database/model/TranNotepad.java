package com.matthey.pmm.endur.database.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "tran_notepad")
public class TranNotepad {

    @Id
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tran_num")
    private AbTran abTran;
    @Id
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
