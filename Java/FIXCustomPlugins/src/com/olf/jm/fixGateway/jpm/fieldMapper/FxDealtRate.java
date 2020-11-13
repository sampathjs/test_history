package com.olf.jm.fixGateway.jpm.fieldMapper;

import com.olf.jm.fixGateway.fieldMapper.FieldMapperBase;
import com.olf.jm.fixGateway.fieldMapper.fields.EnumExecutionReport;
import com.olf.openjvs.enums.TRANF_FIELD;

/*
 * History:
 * 2020-05-11 - V0.1 - jwaechter - Initial Version
 */


/**
 * Class responsible for mapping the Price.
 */
public class FxDealtRate extends FieldMapperBase {

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldName()
	 */
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_FX_DEALT_RATE;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTagFieldName()
	 */
	@Override
	public String getTagFieldName() {
		return EnumExecutionReport.LAST_PX.getTagName();
	}
	
	@Override
	public boolean isInfoField() {
		return false;
	}
}
