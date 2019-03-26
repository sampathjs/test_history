package com.jm.shanghai.accounting.udsr.model.mapping;

import com.jm.shanghai.accounting.udsr.model.fixed.ConfigurationItem;

/*
 * History:
 * 2018-11-21		V1.0	 jwaechter		- Intial Version
 */

/**
 * Different types a col in the {@link ConfigurationItem#MAPPING_CONFIG_TABLE_NAME} can have.
 * @author WaetcJ01
 *
 */
public enum MappingConfigurationColType {
	OUTPUT, MAPPING_LOGIC, UNKNOWN
}
