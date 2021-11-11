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
public class FxDealtAmount extends FieldMapperBase {
	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldName()
	 */
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_FX_D_AMT;
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
			if (!baseCurrencyMapper.switchCurrencies(message)) {
				return amountDealtCurrency;
			} else {
				try {
					return message.getString (EnumExecutionReport.SETTL_CURRENCY_AMT.getTagName(), 1);					
				}  catch (OException ex) {
					throw new RuntimeException ("Error accessing FIX field '" + EnumExecutionReport.SETTL_CURRENCY_AMT.getTagName() + "'");
				}
			}			
		} catch (OException ex) {
			throw new RuntimeException ("Error accessing FIX field '" + EnumExecutionReport.LAST_QTY.getTagName() + "'");
		}
	}
}