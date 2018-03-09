/* Released with version 09-Jan-2014_V14_0_6 of APM */

package standard.apm;

import standard.include.APM_Utils;

import java.util.ArrayList;

import com.olf.openjvs.DBase;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DB_RETURN_CODE;
import com.olf.openjvs.enums.DBTYPE_ENUM;
import com.olf.openjvs.Str;
import com.olf.openjvs.XString;
import com.olf.openjvs.Apm;

public class DealPruningHandler 
{
	/// List of deals we're working on.
	private ArrayList<PrunerDealKey> m_deals;
	
	/// List of pruned deals.
	private ArrayList<PrunerDealKey> m_pruned;
	
	/// List of in use deals.
	private ArrayList<PrunerDealKey> m_used;	
	
	/// Argument table
	private Table m_argt;
	
	/// Utility class.
	private APM_Utils m_utils;
	
	/// True iff the Elapsed method has been called.
	private boolean m_timerStarted = false;
	
	/// The time in milliseconds of the first time the Elapsed method was called.
	private long m_time = 0;

	/// The mode the APm scripts are currently in (batch, backout, etc)
	private int m_mode = 0;
	
	/// Constructor, takes the argt to feed utility methods and a reference to a APM_Util class instance.
	public DealPruningHandler( int mode, Table argt, APM_Utils utils )
	{
		m_deals = new ArrayList<PrunerDealKey>();
		m_pruned = new ArrayList<PrunerDealKey>();
		m_used = new ArrayList<PrunerDealKey>();
		m_utils = utils;
		m_argt = argt;
		m_mode = mode;		
	}
	
	/// Add a deal to be pruned.
	public void AddDeal( int dealNum, int tranNum, int version, int serviceId )
	{		
		m_deals.add(new PrunerDealKey( dealNum, tranNum, version, serviceId ));
	}
	
	/// Get a list of pruned deals.
	public ArrayList<PrunerDealKey> GetPrunedDeals()
	{
		return ( m_pruned );
	}
	
	/// Get a list of used deals.
	public ArrayList<PrunerDealKey> GetUsedDeals()
	{		
		return ( m_used );
	}
	
	/// Gets the time since the last time this method was called.
	public long Elapsed()
	{
		if (!m_timerStarted) {
			m_time = System.currentTimeMillis();
			m_timerStarted = true;
			return ( 0 );		
		}
		
		return ( System.currentTimeMillis() - m_time );
	}
	
	/// Attempt to start a transaction to frame the pruning handler.
	public boolean Begin() throws OException
	{
		int retVal;
		Table empty;

		// Attempt to setup a transaction to allow for deal locking.
		empty = Table.tableNew("empty");		
		XString err_xstring = Str.xstringNew();
		try
		{
			retVal = Apm.performOperation(m_utils.APM_PRUNE_START, 0, empty, err_xstring);
		}
		catch (Exception t)
		{
			retVal = DB_RETURN_CODE.SYB_RETURN_APP_FAILURE.toInt();
		}
		Str.xstringDestroy(err_xstring);

		if (retVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
			m_utils.APM_PrintAndLogErrorMessage(m_mode, m_argt, "Begin() call to begin prune failed");
			return ( false );			
		}		

		return ( true );
	}
	
	/// Attempt to complete a transaction to frame the pruning handler.
	public boolean End() throws OException
	{
		int retVal;
		Table empty;

		// Attempt to setup a transaction to allow for deal locking.
		empty = Table.tableNew("empty");		
		XString err_xstring = Str.xstringNew();
		try
		{
			retVal = Apm.performOperation(m_utils.APM_PRUNE_END, 0, empty, err_xstring);
		}
		catch (Exception t)
		{
			retVal = DB_RETURN_CODE.SYB_RETURN_APP_FAILURE.toInt();
		}
		Str.xstringDestroy(err_xstring);
		
		if (retVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
			m_utils.APM_PrintAndLogErrorMessage(m_mode, m_argt, "End() call to end prune failed");
			return ( false );			
		}		
		
		return ( true );	
	}

	/// Attempt to complete a transaction to frame the pruning handler.
	public boolean Quit() throws OException
	{
		int retVal = DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
		Table empty;

		// Attempt to setup a transaction to allow for deal locking.
		empty = Table.tableNew("empty");		
		XString err_xstring = Str.xstringNew();
		try
		{
			retVal = Apm.performOperation(m_utils.APM_PRUNE_QUIT, 0, empty, err_xstring);
		}
		catch (Exception t)
		{
			retVal = DB_RETURN_CODE.SYB_RETURN_APP_FAILURE.toInt();
		}			
		Str.xstringDestroy(err_xstring);
				
		if (retVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
			m_utils.APM_PrintAndLogErrorMessage(m_mode, m_argt, "Quit() call to pruner failed");
			return ( false );			
		}		
		
		return ( true );	
	}
		
	/// Attempt to lock the deals added to the pruning handler.  All of the deals
	/// need to be locked for this to work.  If a deadlock occurs we return
	/// false as not all the deals can be locked.
	public boolean Lock(int dataset_type, String packageName, int LockThreadSleepTime) throws OException
	{	
		int retVal;
		
		// Attempt to touch a variety of deals...
		
		Table dealCheck = Table.tableNew("params");
		dealCheck.addCol("deal_num", COL_TYPE_ENUM.COL_INT);
		dealCheck.addCol("dataset_type", COL_TYPE_ENUM.COL_INT);
		dealCheck.addCol("package_name", COL_TYPE_ENUM.COL_STRING);
		dealCheck.addRow();
		
		try {		
			for (int i = 0; i < m_deals.size(); i++ ) {
				PrunerDealKey key = m_deals.get(i);						
				dealCheck.setInt(1, 1, key.GetDealNum());
				dealCheck.setInt(2, 1, dataset_type);			
				dealCheck.setString(3, 1, packageName);
			
				retVal = m_utils.APM_DBASE_RunProc(m_argt, "USER_apm_lock_deal", dealCheck);
				
				if (retVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
					m_utils.APM_PrintAndLogErrorMessage(m_mode, m_argt, "Lock() cannot lock deal " + key.GetDealNum() + " tran num " + key.GetTranNum() + " version " + key.GetVersion() + " service id " + key.GetServiceId() );
					dealCheck.destroy();
					return ( false );			
				}

				// this is for testing purposes to force a deadlock
				if ( LockThreadSleepTime > 0 )
				{
					try 
					{
						Thread.sleep(LockThreadSleepTime);
					} 
					catch (InterruptedException e) 
					{
						e.printStackTrace();
					}
				}
			}		
		} catch (OException e) {
			// Catch deadlocks, which will 
			return ( false );
		} finally {
			dealCheck.destroy();
		}
		
		return ( true );
	}
	
	/// Attempt to prune the deals added to the pruning handler.  Populates a used ArrayList and a pruned ArrayList.
	public void Prune() throws OException
	{
		m_pruned.clear();
		m_used.clear();
		
		for (int i = 0; i < m_deals.size(); i++ ) {
			PrunerDealKey key = m_deals.get(i);						
			if ( m_utils.APM_IsLaterDealVersionProcessingInQueue(m_argt, key.GetDealNum(), key.GetTranNum(), key.GetVersion(), key.GetServiceId()) )
				m_pruned.add(key);
			else
				m_used.add(key);				
		}
	}	
}
