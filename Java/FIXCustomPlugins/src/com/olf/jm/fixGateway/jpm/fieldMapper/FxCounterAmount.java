package com.olf.jm.fixGateway.jpm.fieldMapper;

import com.olf.jm.fixGateway.fieldMapper.FieldMapperBase;
import com.olf.jm.fixGateway.fieldMapper.FieldMapperException;
import com.olf.jm.fixGateway.fieldMapper.fields.EnumExecutionReport;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.TRANF_FIELD;

/*
 * History:
 * 2020-05-12 - V0.1 - jwaechter - Initial Version
 */


/**
 *  Class responsible for providing the amount of the dealt currency
 *  
 */
public class FxCounterAmount extends FieldMapperBase {
	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldName()
	 */
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_FX_C_AMT;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTagFieldName()
	 */
	@Override
	public String getTagFieldName() {
		return null; // special calculation logic
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldValue(com.olf.openjvs.Table)
	 */
	@Override
	public String getTranFieldValue(Table message) throws FieldMapperException {
		try {
			String amountDealtCurrency = message.getString (EnumExecutionReport.LAST_QTY.getTagName(), 1);
			FxBaseCurrency baseCurrencyMapper = new FxBaseCurrency();
			if (baseCurrencyMapper.switchCurrencies(message)) {
				return "1";
			} else {
				return amountDealtCurrency;
			}			
		} catch (OException ex) {
			throw new RuntimeException ("Error accessing FIX field '" + EnumExecutionReport.LAST_QTY.getTagName() + "'");
		}
	}
}