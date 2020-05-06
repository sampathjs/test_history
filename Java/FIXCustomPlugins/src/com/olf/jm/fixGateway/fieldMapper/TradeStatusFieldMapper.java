package com.olf.jm.fixGateway.fieldMapper;

import com.olf.openjvs.Table;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;

/*
 * History:
 * 2017-10-10 - V0.1 - scurran - Initial Version
 */


/**
 * Class responsible for setting the Trade Status. Value is hard coded to Validated.
 */
public class TradeStatusFieldMapper implements FieldMapper {

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapper#getTranFieldName()
	 */
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_TRAN_STATUS;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapper#getSide()
	 */
	@Override
	public int getSide() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapper#getTranFieldValue(com.olf.openjvs.Table)
	 */
	@Override
	public String getTranFieldValue(Table message) {
		return TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toString();
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapper#isInfoField()
	 */
	@Override
	public boolean isInfoField() {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapper#infoFieldName()
	 */
	@Override
	public String infoFieldName() throws FieldMapperException {
		throw new FieldMapperException("Not an info field.");
	}

}
