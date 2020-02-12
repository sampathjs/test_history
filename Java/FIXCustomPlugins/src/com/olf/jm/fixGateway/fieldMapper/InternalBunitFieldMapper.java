package com.olf.jm.fixGateway.fieldMapper;

import com.olf.jm.fixGateway.fieldMapper.fields.EnumExecutionReport;
import com.olf.jm.fixGateway.fieldMapper.fields.FixField;
import com.olf.openjvs.enums.TRANF_FIELD;

/*
 * History:
 * 2017-10-10 - V0.1 - scurran - Initial Version
 */


/**
 *  Class responsible for mapping the Internal Bunit.
 */
public class InternalBunitFieldMapper extends FieldMapperBase {

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldName()
	 */
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_INTERNAL_BUNIT;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTagFieldName()
	 */
	@Override
	public FixField getTagFieldName() {
		return EnumExecutionReport.TARGET_COMP_ID;
	}





}
