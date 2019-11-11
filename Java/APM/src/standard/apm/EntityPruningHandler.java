/* Released with version 29-Aug-2019_V17_0_124 of APM */

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

public class EntityPruningHandler 
{
	/// List of Entities we're working on.
	private ArrayList<PrunerEntityKey> m_entities;
	
	/// List of pruned Entities.
	private ArrayList<PrunerEntityKey> m_pruned;
	
	/// List of in use Entities.
	private ArrayList<PrunerEntityKey> m_used;	
	
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
	public EntityPruningHandler( int mode, Table argt, APM_Utils utils )
	{
		m_entities = new ArrayList<PrunerEntityKey>();
		m_pruned = new ArrayList<PrunerEntityKey>();
		m_used = new ArrayList<PrunerEntityKey>();
		m_utils = utils;
		m_argt = argt;
		m_mode = mode;		
	}
	
	/// Add an entity to be pruned.
	public void AddEntity( int primaryEntityNum, int secondaryEntityNum, int entityVersion, int serviceId )
	{		
		m_entities.add(new PrunerEntityKey( primaryEntityNum, secondaryEntityNum, entityVersion, serviceId ));
	}
	
	/// Get a list of pruned entities.
	public ArrayList<PrunerEntityKey> GetPrunedEntities()
	{
		return ( m_pruned );
	}
	
	/// Get a list of used entities.
	public ArrayList<PrunerEntityKey> GetUsedEntities()
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

		// Attempt to setup a transaction to allow for entity locking.
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

		// Attempt to setup a transaction to allow for entity locking.
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

		// Attempt to setup a transaction to allow for entity locking.
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
		
	/// Attempt to lock the entities added to the pruning handler.  All of the entities
	/// need to be locked for this to work.  If a deadlock occurs we return
	/// false as not all the entities can be locked.
	public boolean Lock(int datasetTypeId, String packageName, int LockThreadSleepTime) throws OException
	{	
		int retVal;
		
		// Attempt to touch a variety of entities...
		
		Table entityCheck = Table.tableNew("params");
		entityCheck.addCol("primary_entity_num", COL_TYPE_ENUM.COL_INT);
		entityCheck.addCol("dataset_type_id", COL_TYPE_ENUM.COL_INT);
		entityCheck.addCol("package_name", COL_TYPE_ENUM.COL_STRING);
		entityCheck.addRow();
		
		try {		
			for (int i = 0; i < m_entities.size(); i++ ) {
				PrunerEntityKey key = m_entities.get(i);						
				entityCheck.setInt(1, 1, key.GetPrimaryEntityNum());
				entityCheck.setInt(2, 1, datasetTypeId);			
				entityCheck.setString(3, 1, packageName);
			
				retVal = m_utils.APM_DBASE_RunProc(m_argt, "USER_apm_lock_entity", entityCheck);
				
				if (retVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt()) {
					m_utils.APM_PrintMessage(m_argt, "Lock() cannot lock entity " + key.GetPrimaryEntityNum() + " secondary num " + key.GetSecondaryEntityNum() + " version " + key.GetEntityVersion() + " service id " + key.GetServiceId() );
					entityCheck.destroy();
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
			entityCheck.destroy();
		}
		
		return ( true );
	}
	
	/// Attempt to prune the entities added to the pruning handler.  Populates a used ArrayList and a pruned ArrayList.
	public void Prune() throws OException
	{
		m_pruned.clear();
		m_used.clear();
		
		for (int i = 0; i < m_entities.size(); i++ ) {
			PrunerEntityKey key = m_entities.get(i);						
			if ( m_utils.APM_IsLaterEntityVersionProcessingInQueue(m_argt, key.GetPrimaryEntityNum(), key.GetSecondaryEntityNum(), key.GetEntityVersion(), key.GetServiceId()) )
				m_pruned.add(key);
			else
				m_used.add(key);				
		}
	}	
}
