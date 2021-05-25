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
 * Class responsible for mapping the Buy / Sell flag. 
 */
public class BuySellFieldMapper extends FieldMapperBaseForUserTable {

	public BuySellFieldMapper() {
		super("BBG Tradebook", "Buy / Sell");
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldName()
	 */
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_BUY_SELL;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTagFieldName()
	 */
	@Override
	public String getTagFieldName() {
		return EnumExecutionReport.SIDE.getTagName();
	}

	@Override
	protected String getComplexTagValue(Table message) {
		return null; // no complex logic
	}

}
