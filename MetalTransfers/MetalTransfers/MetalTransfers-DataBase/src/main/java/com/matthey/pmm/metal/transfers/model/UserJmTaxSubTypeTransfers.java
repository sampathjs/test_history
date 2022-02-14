package com.matthey.pmm.metal.transfers.model;

import com.matthey.pmm.endur.database.model.DbConstants;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "USER_jm_tax_sub_type_transfers")
public class UserJmTaxSubTypeTransfers {

    @Id
    private String active;
    private String fromAccountCountry;
    private String toAccountCountry;
    private String toAccountRegion;
    private String toBuInternal;
    private String metal;
    private String lbma;
    private String lppm;
    private String taxSubtype;

}
