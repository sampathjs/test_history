package com.olf.jm.fixGateway.fieldMapper;

import com.olf.jm.fixGateway.fieldMapper.fields.EnumExecutionReport;
import com.olf.jm.fixGateway.fieldMapper.fields.FixField;
import com.olf.openjvs.enums.TRANF_FIELD;

/*
 * History:
 * 2017-10-10 - V0.1 - scurran - Initial Version
 */


/**
 * Class responsible for populating the tran info field holding the ExecutionId.
 */
public class ExecutionIdFieldMapper extends FieldMapperBase {

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldName()
	 */
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_TRAN_INFO;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTagFieldName()
	 */
	@Override
	public FixField getTagFieldName() {
		return EnumExecutionReport.EXEC_ID;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#isInfoField()
	 */
	@Override
	public boolean isInfoField() {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#infoFieldName()
	 */
	@Override
	public String infoFieldName() throws FieldMapperException {
		return "TradeBook Execution Id";
	}

}
