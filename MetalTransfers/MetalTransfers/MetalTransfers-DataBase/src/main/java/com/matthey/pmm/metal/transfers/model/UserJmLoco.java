package com.matthey.pmm.metal.transfers.model;

import com.matthey.pmm.endur.database.model.DbConstants;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "USER_jm_loco")
public class UserJmLoco {

    @Id
    private int locoId;
    private String locoName;
    private String isTransferLoco;
    private String intBu;
    private String isPmm;
    private String defaultForm;
    private String country;
    private int isPmmId;

}
