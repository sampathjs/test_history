package com.olf.jm.fixGateway.jpm.fieldMapper;

import com.olf.jm.fixGateway.fieldMapper.FieldMapperBase;
import com.olf.jm.fixGateway.fieldMapper.fields.EnumExecutionReport;
import com.olf.openjvs.enums.TRANF_FIELD;

/*
 * History:
 * 2020-05-19	V0.1	- jwaechter - Initial Version
 */
public class BaseMetalSpread extends FieldMapperBase {
	
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_RATE_SPD;
	}

	@Override
	public String getTagFieldName() {
		return EnumExecutionReport.PRICE.getTagName();
	}
	
}
