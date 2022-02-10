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
    private int idNumber;
    private String name;
    private String title;
    private String firstName;
    private String lastName;
    private String phone;
    private String fax;
    private int personnelType;
    private int status;
    private LocalDateTime passwordDate;
    private int authoriser;
    private LocalDateTime lastUpdate;
    private int personnelVersion;
    private String employeeId;
    private String email;
    private String addr1;
    private String addr2;
    private String city;
    private int stateId;
    private int country;
    private String mailCode;
    private int tsvGroupId;
    private int priorityUser;
    private int passwordNeverExpires;
    private String shortAliasName;
    private int timeZone;
    private int photoNodeId;
    private int signatureNodeId;
    private int citizenshipCountry;
    private int residenceCountry;
    private int dpaGroupId;
}
