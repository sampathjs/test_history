package com.olf.jm.custom_picklists.app;

import com.olf.openjvs.IScript;

/*
 * History:
 * 2015-10-13	V1.0	jwaechter	-	Initial Version
 */

/**
 * Class retrieving all parties that are business units and external for a pick list.
 * Note that you can look up the execute method in it's superclass,
 * {@link AbstractPartyPickList}.
 * @author jwaechter
 * @version 1.0
 */
public class ExternalBusinessUnitPickList extends AbstractPartyPickList implements IScript{
	public ExternalBusinessUnitPickList () {
		super(AbstractPartyPickList.EnumPartyClass.BUSINESS_UNIT, AbstractPartyPickList.EnumInternalExternal.EXTERNAL);
	}
}
