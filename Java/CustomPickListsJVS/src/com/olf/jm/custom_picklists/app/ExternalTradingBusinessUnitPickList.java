package com.olf.jm.custom_picklists.app;

import com.olf.openjvs.enums.PARTY_FUNCTION_TYPE;

/*
 * History:
 * 2016-01-20	V1.0	jwaechter	- Initial Version
 */

/**
 * Picklist for external trading business units
 * @author jwachter
 * @version 1.0
 */
public class ExternalTradingBusinessUnitPickList extends AbstractPartyPickList {
	public ExternalTradingBusinessUnitPickList () {
		super(EnumPartyClass.BUSINESS_UNIT, EnumInternalExternal.EXTERNAL, PARTY_FUNCTION_TYPE.TRADING_PARTY);
	}
}
