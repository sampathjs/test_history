package com.olf.jm.fixGateway.fieldMapper;

import com.olf.openjvs.Table;
import com.olf.openjvs.enums.INS_TYPE_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;


/*
 * History:
 * 2017-10-10 - V0.1 - scurran - Initial Version
 */


/**
 *  Class responsible for mapping the Ins Type field. Hard coded to PREC_EXCH_FUT
 */
public class InsTypeFieldMapper implements FieldMapper {

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapper#getTranFieldName()
	 */
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_INS_TYPE;
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
		return INS_TYPE_ENUM.prec_exch_future.toString();
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

	@Override
	public int getSeqNum2() {
		return 0;
	}

	@Override
	public int getSeqNum3() {
		return 0;
	}

	@Override
	public int getSeqNum4() {
		return 0;
	}

	@Override
	public int getSeqNum5() {
		return 0;
	}
}
