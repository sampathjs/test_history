package com.matthey.pmm.metal.transfers.model;

import com.matthey.pmm.endur.database.model.DbConstants;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "USER_jm_form")
public class UserJmForm {

    @Id
    private String form;
}
