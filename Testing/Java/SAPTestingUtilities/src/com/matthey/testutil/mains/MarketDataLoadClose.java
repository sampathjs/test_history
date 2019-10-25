package com.matthey.testutil.mains;

import com.matthey.testutil.BaseScript;
import com.matthey.testutil.exception.SapTestUtilRuntimeException;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.Index;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Query;
import com.olf.openjvs.Sim;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.logging.PluginLog;

/**
 * Loads closing prices for the current business date
 */
public class MarketDataLoadClose extends BaseScript
{
	@Override
	public void execute(IContainerContext arg0) throws OException
	{
		try
		{
			setupLog();
			
			PluginLog.info("Loading closing datasets..");
			
			Table tblIndexList = getIndexList("EOD Curves"); /** TODO pass this query via TPM */
			loadCloseIndexList(tblIndexList);
			
			PluginLog.info("Closing datasets loaded!");
		}
		catch (Exception e)
		{
			com.matthey.testutil.common.Util.printStackTrace(e);
			throw new SapTestUtilRuntimeException("Error occured during LoadClose: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Refresh market data and load closing data
	 * 
	 * @param tblIndexList
	 * @throws OException
	 */
	private void loadCloseIndexList(Table tblIndexList) throws OException
    {
        if (Index.refreshDbList(tblIndexList, 1) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue ())
        {
            PluginLog.info("Index.refreshDb (1) failed in loadCloseIndexList");
        }
        
        if (Sim.loadAllCloseMktd(OCalendar.today()) < 1)
        {
        	PluginLog.error ("Load close failed");	
        }
        
        if (Index.refreshShm(1) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue ())
        {
        	PluginLog.info("Index.refreshShm (1) failed in loadCloseIndexList");
        }
    }
	
	/**
	 * Retrieve a table of valid curves to save
	 * 
	 * @param queryName
	 * @return
	 * @throws OException
	 */
    private Table getIndexList(String queryName) throws OException
    {
    	int queryId = -1;
    	
    	Table tblIndexList = null;
        
    	try
    	{
            queryId = Query.run(queryName);
        	
            tblIndexList = Table.tableNew("Index List");
            
            String sqlQuery = 
            		"SELECT i.index_id, i.index_name"
                    + " FROM idx_def i, " + Query.getResultTableForId(queryId)  + " q"
                    + " WHERE q.unique_id = " + queryId
                    + " AND q.query_result = i.index_id"
                    + " AND i.db_status = 1";
            
            int ret = DBaseTable.execISql (tblIndexList, sqlQuery);
            
            if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
            {
            	throw new SapTestUtilRuntimeException("Unable to run query: " + sqlQuery);
            }
            
            PluginLog.info("Num retrieved curves: " + tblIndexList.getNumRows());	
    	}
    	finally
    	{
            if (queryId > 0)
            {
            	Query.clear (queryId);	
            }
    	}

        return tblIndexList;
    }
}