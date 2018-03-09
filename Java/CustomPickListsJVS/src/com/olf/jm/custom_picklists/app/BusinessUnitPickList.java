package com.olf.jm.custom_picklists.app;

import com.olf.openjvs.IScript;

/*
 * History:
 * 2016-03-24	V1.0	jwaechter	-	Initial Version
 */

/**
 * Class retrieving all parties that are business units for a pick list.
 * Note that you can look up the execute method in it's superclass,
 * {@link AbstractPartyPickList}.
 * @author jwaechter
 * @version 1.0
 */
public class BusinessUnitPickList extends AbstractPartyPickList implements IScript{
	public BusinessUnitPickList () {
		super(AbstractPartyPickList.EnumPartyClass.BUSINESS_UNIT, null);
	}
}
