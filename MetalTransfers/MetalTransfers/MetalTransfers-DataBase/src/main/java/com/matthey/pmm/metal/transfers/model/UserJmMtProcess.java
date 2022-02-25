package com.matthey.pmm.metal.transfers.model;

import com.matthey.pmm.endur.database.model.AbTran;
import com.matthey.pmm.endur.database.model.DbConstants;
import com.matthey.pmm.endur.database.model.Personnel;
import lombok.Data;
import org.w3c.dom.Text;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "USER_jm_mt_process")
public class UserJmMtProcess {

    @Id
    private Integer runId;
    private Integer strategyId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tran_num")
    private AbTran abTran;
    private String tranStatus;
    private Integer version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personnel_id")
    private Personnel personnel;
    private String internalStatus;
    private String message;
    private LocalDateTime lastUpdatedTime;

}
