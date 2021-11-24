package com.olf.jm.fixGateway.jpm.fieldMapper;

import com.olf.jm.fixGateway.fieldMapper.FieldMapper;
import com.olf.jm.fixGateway.fieldMapper.FieldMapperException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.INS_SUB_TYPE;
import com.olf.openjvs.enums.TRANF_FIELD;


/*
 * History:
 * 2020-05-18 - V0.1 - jwaechter - Initial Version
 */


/**
 *  Class responsible for mapping the Ins Type field. Hard coded to METAL-SWAP
 */
public class InsSubTypeMetalSwap implements FieldMapper {

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapper#getTranFieldName()
	 */
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_INS_SUB_TYPE;
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
		return INS_SUB_TYPE.metal_swap_sub_type.toString();
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
