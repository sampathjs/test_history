package com.olf.jm.fixGateway.jpm.fieldMapper;

import com.olf.jm.fixGateway.fieldMapper.FieldMapperBaseForUserTable;
import com.olf.jm.fixGateway.fieldMapper.FieldMapperException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.TRANF_FIELD;

/*
 * History:
 * 2020-05-12 - V0.1 - jwachter - Initial Version
 */


/**
 * Class responsible for mapping the Buy / Sell flag. 
 */
public class BuySell extends FieldMapperBaseForUserTable {

	public BuySell() {
		super("JPM Execute", "Buy / Sell");
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
		return "Side"; 
	}

	protected String getComplexTagValue(Table message)
	throws FieldMapperException {
		return null;
	}	
//	@Override
//	protected String getComplexTagValue(Table message)
//			throws FieldMapperException {
//		try {
//			String buySell = message.getString(EnumExecutionReport.SIDE.getTagName(), 1);
//			Ticker tickerLogic = new FxBaseCurrency();
//			if (!tickerLogic.switchCurrencies(message)) {
//				return buySell;
//			} else {
//				switch (buySell) {
//				case "2": // BBG value
//				case "SELL": // JPM value
//					return "1";
//				case "1": // BBG value
//				case "BUY": // JPM value
//					return "2";
//				default: 
//					throw new RuntimeException ("Unknown value in FIX message for FIX tag '" + EnumExecutionReport.SIDE.getTagName() + "'");
//				}
//			}
//		} catch (OException ex) {
//			throw new RuntimeException ("Error retrieving data for FIX field '" + EnumExecutionReport.SIDE.getTagName() + "'");
//		}
//	}
}
