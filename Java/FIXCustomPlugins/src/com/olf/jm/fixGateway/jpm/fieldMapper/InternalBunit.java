package com.olf.jm.fixGateway.jpm.fieldMapper;

import com.olf.jm.fixGateway.fieldMapper.FieldMapperBase;
import com.olf.jm.fixGateway.fieldMapper.FieldMapperException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.TRANF_FIELD;

/*
 * History:
 * 2020-05-13 - V0.1 - jwaechter - Initial Version
 * 2020-11-18 - V0.2 - jwaechter - Internal BU is now JM PMM UK always.
 */

/**
 *  Class responsible for mapping the Internal Bunit.
 */
public class InternalBunit extends FieldMapperBase {
	private static final String JM_PMM_UK = "JM PMM UK";
		
	@Override
	public String getTagFieldName() {
		return null; // complex logic
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldName()
	 */
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_INTERNAL_BUNIT;
	}

	@Override
	public String getTranFieldValue(Table message) throws FieldMapperException {
		return JM_PMM_UK;
	}
}