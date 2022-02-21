package com.matthey.pmm.endur.database.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Data
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "personnel")
public class Personnel {

    @Id
    private Integer idNumber;
    private String name;
    private String title;
    private String firstName;
    private String lastName;
    private String phone;
    private String fax;
    private Integer personnelType;
    private Integer status;
    private LocalDateTime passwordDate;
    private Integer authoriser;
    private LocalDateTime lastUpdate;
    private Integer personnelVersion;
    private String employeeId;
    private String email;
    private String addr1;
    private String addr2;
    private String city;
    private Integer stateId;
    private Integer country;
    private String mailCode;
    private Integer tsvGroupId;
    private Integer priorityUser;
    private Integer passwordNeverExpires;
    private String shortAliasName;
    private Integer timeZone;
    private Integer photoNodeId;
    private Integer signatureNodeId;
    private Integer citizenshipCountry;
    private Integer residenceCountry;
    private Integer dpaGroupId;
}
