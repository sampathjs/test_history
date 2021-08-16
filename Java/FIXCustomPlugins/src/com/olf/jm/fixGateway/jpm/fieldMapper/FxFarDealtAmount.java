package com.olf.jm.fixGateway.jpm.fieldMapper;

import com.olf.jm.fixGateway.fieldMapper.FieldMapperBase;
import com.olf.jm.fixGateway.fieldMapper.fields.EnumExecutionReport;
import com.olf.openjvs.enums.TRANF_FIELD;

/*
 * History:
 * 2020-05-18 - V0.1 - jwaechter - Initial Version
 * 2020-08-05 - V0.2 - jwaechter - 
 */


/**
 *  Class responsible for providing the amount of the dealt currency
 *  
 */
public class FxFarDealtAmount extends FieldMapperBase {
	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldName()
	 */
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_FX_FAR_D_AMT;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTagFieldName()
	 */
	@Override
	public String getTagFieldName() {
		return EnumExecutionReport.ORDER_QTY_2.getTagName(); // special calculation logic
	}
}