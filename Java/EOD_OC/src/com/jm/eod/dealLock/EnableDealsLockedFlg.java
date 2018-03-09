/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * Copyright: OpenLink International Ltd. ©. London U.K.
 *
 * This script is normally run as part of the EOD - the value passed to the main script
 * sets the deal lock flag thus stopping trade amendments.
 *  
 * @author Douglas Connolly
 *
 * History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 02-Nov-2015 |               | D.Connolly      | Initial version.                                                                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */

package com.jm.eod.dealLock;

import com.jm.eod.common.*;
import com.olf.embedded.application.*;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openrisk.application.*;
import com.olf.openrisk.table.*;

@ScriptCategory({ EnumScriptCategory.Generic })
public class EnableDealsLockedFlg extends AbstractGenericScript 
{	
    @Override
    public Table execute(Session session, ConstTable args) 
    {
        Table data = session.getTableFactory().createTable();
        data.addColumn(Const.DEALS_LOCKED_COL_NAME, EnumColType.String);
        data.addRow();
        data.setString(Const.DEALS_LOCKED_COL_NAME, 0, "Y");
        return data;
     }
}