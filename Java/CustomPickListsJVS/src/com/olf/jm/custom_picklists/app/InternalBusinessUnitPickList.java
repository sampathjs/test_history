package com.olf.jm.custom_picklists.app;

import com.olf.openjvs.IScript;

/*
 * History:
 * 2015-11-16	V1.0	jwaechter	-	Initial Version
 */

/**
 * Class retrieving all parties that are business units and internal for a pick list.
 * Note that you can look up the execute method in it's superclass,
 * {@link AbstractPartyPickList}.
 * @author jwaechter
 * @version 1.0
 */
public class InternalBusinessUnitPickList extends AbstractPartyPickList implements IScript{
	public InternalBusinessUnitPickList () {
		super(AbstractPartyPickList.EnumPartyClass.BUSINESS_UNIT, AbstractPartyPickList.EnumInternalExternal.INTERNAL);
	}
}
