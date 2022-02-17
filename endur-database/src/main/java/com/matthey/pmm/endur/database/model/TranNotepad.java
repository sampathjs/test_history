package com.matthey.pmm.endur.database.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "tran_notepad")
@IdClass(TranNotepad.TranNotepadPrimaryKey.class)
public class TranNotepad {

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class TranNotepadPrimaryKey implements Serializable {
        private AbTran abTran;
        private int noteType;
    }

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
