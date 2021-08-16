package com.olf.jm.fixGateway.jpm.fieldMapper;

import com.olf.jm.fixGateway.fieldMapper.FieldMapperBase;
import com.olf.jm.fixGateway.fieldMapper.fields.EnumExecutionReport;
import com.olf.openjvs.enums.TRANF_FIELD;

/*
 * History:
 * 2020-08-06	V0.1	- jwaechter - Initial Version
 */
public class BaseCurrency extends FieldMapperBase {
	
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_BASE_CURRENCY;
	}

	@Override
	public String getTagFieldName() {
		return EnumExecutionReport.CURRENCY.getTagName();
	}
	
}
