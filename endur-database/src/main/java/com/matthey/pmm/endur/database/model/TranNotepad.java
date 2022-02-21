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
        private Integer noteType;
    }

    @Id
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tran_num")
    private AbTran abTran;
    @Id
    private Integer noteType;
    private Integer lineNum;
    private String lineText;
    private Integer userId;
    private Integer commentNum;
    private LocalDateTime commentDate;
    private LocalDateTime timeStamp;
    private Integer insNum;
    private LocalDateTime commentEndDate;

}
