/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * Copyright: OpenLink International Ltd. ©. London U.K.
 *
 * Op. Service which forbids amendments on live deals when the deals locked flag is set  
 * within USER_jm_eod_status.
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

package com.jm.eod.opsvc;

import com.jm.eod.common.Const;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import com.openlink.endur.utilities.logger.*;

@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class OpsPreVerifyDealUpdAllowed extends AbstractTradeProcessListener 
{	
    @Override
    public PreProcessResult preProcess(Context context, EnumTranStatus targetStatus,
            PreProcessingInfo<EnumTranStatus>[] infoArray, Table clientData) 
    { 
    	try (UserTable dbData = context.getIOFactory().getUserTable(Const.EOD_STATUS_TBL_NAME))
    	{
            try (Table eodStatus = dbData.retrieveTable())
            {
            	if (eodStatus.getRowCount() < 1 || !eodStatus.isValidColumn(Const.DEALS_LOCKED_COL_NAME))
            	{
            		throw new RuntimeException("Invalid User Table: " + Const.EOD_STATUS_TBL_NAME);
            	}
            	if (eodStatus.getString(Const.DEALS_LOCKED_COL_NAME, 0).equals("Y"))
            	{
            		return PreProcessResult.failed("EOD is running, deals cannot be amended. Please re-try later");
            	}
            }
    	} 
    	catch (Exception e) 
    	{
    		String msg = String.format("Pre-process failure: %s - %s", this.getClass().getSimpleName(), e.getLocalizedMessage());
    		Logger.log(com.openlink.endur.utilities.logger.LogLevel.FATAL,
    				LogCategory.Trading, 
    				this,
    				e.getMessage());
    		return PreProcessResult.failed(msg);

    	} 

    	return PreProcessResult.succeeded();
    }
}
