package com.olf.jm.fixGateway.jpm.fieldMapper;

import com.olf.jm.fixGateway.fieldMapper.FieldMapperBase;
import com.olf.jm.fixGateway.fieldMapper.FieldMapperException;
import com.olf.jm.fixGateway.fieldMapper.fields.EnumExecutionReport;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.TRANF_FIELD;

/*
 * History:
 * 2020-05-11 - V0.1 - jwaechter - Initial Version
 * 2020-08-05 - V0.2 - jwaechter - Changed EXEC_ID to ORDER_ID
 */

/**
 * Class responsible for mapping the Reference. Field is populated with the
 * ExecId of the FIX message.
 */
public class Reference extends FieldMapperBase {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldName()
	 */
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_REFERENCE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTagFieldName()
	 */
	@Override
	public String getTagFieldName() {
		return EnumExecutionReport.ORDER_ID.getTagName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldValue(com.
	 * olf.openjvs.Table)
	 */
	@Override
	public String getTranFieldValue(Table message) throws FieldMapperException {
		String orderId = super.getTranFieldValue(message);
		try {
			boolean secExecIdPresent = message.getColNum(EnumExecutionReport.SECONDARY_EXEC_ID.getTagName()) != -1;
			if (secExecIdPresent) {
				// return the secExecId  only in case it is present and in case the OrderID contains 
				// the secExecId as part of itself, e.g. in case of pattern:
				// OrderId = ATS-1H4-4SWWH6W-0-0
				// SecondaryExecID = 1H4-4SWWH6W
				String secExecId = message.getString(EnumExecutionReport.SECONDARY_EXEC_ID.getTagName(), 1);
				if (secExecId != null && !secExecId.isEmpty() && orderId.contains(secExecId)) {
					return secExecId;
				}
			}
		} catch (OException ex) {
			// in case of issues retrieving the optional secExecID always return the order ID
		}
		return orderId;
	}
}
