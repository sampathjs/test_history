/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * Copyright: OpenLink International Ltd. ©. London U.K.
 *
 * This script is normally run as part of the EOD - it sets the deal lock flag to allow/disallow
 * amendments on live trades. The setting is defined within the parameter scripts.
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

import java.security.InvalidParameterException;
import com.jm.eod.common.*;
import com.olf.embedded.application.*;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openrisk.application.*;
import com.olf.openrisk.io.*;
import com.olf.openrisk.table.*;
import com.olf.jm.logging.Logging;

@ScriptCategory({ EnumScriptCategory.Generic })
public class UpdDealsLockedFlg extends AbstractGenericScript 
{	
    @Override
    public Table execute(Session session, ConstTable args) 
    {
    	try 
    	{
    		Logging.init(session, this.getClass(), "", "");
    		if (args.getRowCount() < 1 || !args.isValidColumn(Const.DEALS_LOCKED_COL_NAME))
    		{
    			throw new InvalidParameterException(String.format("Missing parameter: %s", Const.DEALS_LOCKED_COL_NAME));
    		}
    		
    		String newValue = args.getString(Const.DEALS_LOCKED_COL_NAME, 0);
    		try (UserTable dbData = session.getIOFactory().getUserTable(Const.EOD_STATUS_TBL_NAME))
    		{	
    			try (Table eodStatus = dbData.retrieveTable()) 
    			{
    				eodStatus.setString(Const.DEALS_LOCKED_COL_NAME,  0, newValue);
    				dbData.updateRows(eodStatus, Const.ID_COL_NAME);
    			}
        	}
    		return null;
        }   
    	catch (Exception e) 
    	{
    		Logging.error(e.getLocalizedMessage(),e);
    		String msg = String.format("%s: Failed to update %s - %s", this.getClass().getSimpleName(), Const.EOD_STATUS_TBL_NAME, e.getLocalizedMessage());
    		throw new RuntimeException(msg);
    	}finally{
    		Logging.close();
    	}
        
    }
}