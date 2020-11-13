package com.olf.jm.fixGateway.jpm.fieldMapper;

import com.olf.jm.fixGateway.fieldMapper.FieldMapperBase;
import com.olf.jm.fixGateway.fieldMapper.fields.EnumExecutionReport;
import com.olf.openjvs.enums.TRANF_FIELD;


/*
 * History:
 * 2020-05-11 - V0.1 - jwaechter - Initial Version
 * 2020-08-05 - V0.2 - jwaechter - Changed EXEC_ID to ORDER_ID
 */


/**
 *  Class responsible for mapping the Reference. Field is populated with the ExecId of the FIX message.
 */
public class Reference extends FieldMapperBase {

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldName()
	 */
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_REFERENCE;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTagFieldName()
	 */
	@Override
	public String getTagFieldName() {
		return EnumExecutionReport.ORDER_ID.getTagName();
	}
}
