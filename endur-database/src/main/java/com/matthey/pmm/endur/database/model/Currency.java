package com.matthey.pmm.endur.database.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Data
@Entity
@Table(schema = DbConstants.SCHEMA_NAME, name = "currency")
public class Currency {

    @Id
    private long id_number;
    private String name;
    private long default_index;
    private long spot_index;
    private long convention;
    private long holiday_id;
    private long base_unit;
    private long round;
    private String description;
    private long round_type;
    private long euro;
    private float euro_conversion;
    private long currency_zone;
    private short euro_scenario_flag;
    private long advance_release_days;
    private long user_id;
    private LocalDateTime last_update;
    private short precious_metal;
    private short auto_conversion_flag;
    private long version_number;
}
