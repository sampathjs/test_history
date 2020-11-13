package com.olf.jm.fixGateway.fieldMapper;

import com.olf.jm.fixGateway.fieldMapper.fields.EnumExecutionReport;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.TRANF_FIELD;

/*
 * History:
 * 2017-10-10 - V0.1 - scurran - Initial Version
 * 2020-05-14 - V0.2 - jwaechter - FIX Tag is now a string. 
 *                               - Base class is now FieldMapperBaseForUserTable
 */


/**
 *  Class responsible for mapping the External Bunit.
 */
public class ExternalBunitFieldMapper extends FieldMapperBaseForUserTable {
	public ExternalBunitFieldMapper() {
		super("BBG Tradebook", "Ext Bunit");
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldName()
	 */
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_EXTERNAL_BUNIT;
		//return TRANF_FIELD.TRANF_EXTERNAL_BUNIT;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTagFieldName()
	 */
	@Override
	public String getTagFieldName() {
		//return EnumExecutionReport.SENDER_COMP_ID;
		return EnumExecutionReport.TAG76.getTagName(); // Execution Broker Id
	}

	@Override
	protected String getComplexTagValue(Table message) {
		return null; // no complex logic
	}
}
