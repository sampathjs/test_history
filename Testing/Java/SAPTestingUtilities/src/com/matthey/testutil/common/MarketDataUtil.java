package com.matthey.testutil.common;

import com.olf.openjvs.Index;
import com.olf.openjvs.OException;
import com.olf.openjvs.Sim;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.logging.PluginLog;

public class MarketDataUtil 
{
	public static void loadCloseIndexList(Table tblIndexList, int date) throws OException
    {
        if (Index.refreshDbList(tblIndexList, 1) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue ())
        {
            PluginLog.info("Index.refreshDb (1) failed in loadCloseIndexList");
        }
        
        if (Sim.loadAllCloseMktd(date) < 1)
        {
        	PluginLog.error ("Load close failed");
        }
        
        if (Index.refreshShm(1) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue())
        {
        	PluginLog.info("Index.refreshShm (1) failed in loadCloseIndexList");
        }
    }
}
