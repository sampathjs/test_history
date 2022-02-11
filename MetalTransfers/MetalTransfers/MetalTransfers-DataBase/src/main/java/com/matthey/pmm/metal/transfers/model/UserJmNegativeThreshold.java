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
    private int loco_id;
    private String loco_name;
    private String is_transfer_loco;
    private String int_BU;
    private String is_PMM;
    private String default_form;
    private String country;
    private int is_pmm_id;

}
