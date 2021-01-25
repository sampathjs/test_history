/* Released with version 05-Feb-2020_V17_0_126 of APM */

package standard.apm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import standard.apm.ads.ADSException;
import standard.apm.ads.ADSInterface;
import standard.apm.ads.Factory;
import standard.include.APM_Utils;

import com.olf.openjvs.OException;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;

public class ADS_DataStoreOps
{
	private APM_Utils m_APMUtils;

	/// The ads lock will attempt to obtain a lock for an entity multiple times, the total time spent acquiring this lock is here set in milliseconds.
	private int m_lockThreadTotalSleepTime = 60000;

	/// The ads lock will sleep for this length of time in milliseconds between lock attempts.
	private int m_lockThreadSleepTime = 500;

	/// If true the pruner mechanism will use a lock to protect the prune, if false the pruner will carry out its operation without the lock.
	private boolean m_usePrunerLock = true;
	
	private static ADS_DataStoreOps self;
	
	private ADS_DataStoreOps() {
		m_APMUtils = new APM_Utils();
	}

	public static ADS_DataStoreOps instance() throws OException {
		if (self == null) {
			self = new ADS_DataStoreOps();
		}

		return self;
	}
	
	public int Update(int iUpdateMode, int iMode, int entityGroupId, String sPackageName, int datasetType, int serviceID, boolean datasetKeyInAnotherService, String sJobName, Table tMainArgt, Table tPackageDataTables, Table argt) throws OException 
	{
		int iRetVal = 1;
		int iOldEntityGroupId = 0;
		int foundRow = 0; 
		boolean bIgnoreEntityUpdate = false;

		GetEnvironmentVariables(argt);
		
		if (iUpdateMode == m_APMUtils.APM_APPLY || iUpdateMode == m_APMUtils.APM_BACKOUT)
		{
			// if no previous entity group specified then we need to work it out from the dataset control table
			iOldEntityGroupId = argt.getInt("Previous Entity Group Id", 1);
			if ( iOldEntityGroupId <= 0 ) // not set as it could not be found
			{
				// set the old entity group to the new one
				iOldEntityGroupId = entityGroupId;
				m_APMUtils.APM_PrintMessage (argt, "Could not find previous entity group. Setting it to the current entity group !");
			}
		}

		try {
			ADSInterface ads = Factory.getADSImplementation();

			// get args
			String cacheName = tPackageDataTables.getTable(1,1).getTableName();
			Table dataTable = tPackageDataTables.getTable(1,1);
			Table tEntityInfo = argt.getTable("Filtered Entity Info", 1);

			if (iUpdateMode == m_APMUtils.APM_BACKOUT) {						   
				// backout - this will not occur on a block so don't need a entityset
				// can take from the current primary entity num

				int primaryEntityNum = argt.getInt("Current Primary Entity Num", 1);

				// if we are in BACKOUT mode then we need to check whether the dataset key is being monitored by
				// another service.  If it is then we can skip the backout as the other service will do a backout and apply.
				// if its moving completely out of the monitored set then carry on and do the backout
				if ( datasetKeyInAnotherService == true)
				{
					m_APMUtils.APM_PrintMessage(argt, "Entity moving service. Skipping BACKOUT for entity: " + primaryEntityNum);
					bIgnoreEntityUpdate = true;
				}

				if ( bIgnoreEntityUpdate == false)
				{
					int secondaryEntityNum = argt.getInt("Current Secondary Entity Num", 1);
					int entityVersion = argt.getInt("Current Entity Version", 1);

					EntityPruningHandler pruningHandler = new EntityPruningHandler( iMode, argt, m_APMUtils );								 	
					boolean started = false;
					boolean locked = true;
					boolean complete = false;

					// Add an entity to backout...
					pruningHandler.AddEntity(primaryEntityNum, secondaryEntityNum, entityVersion, serviceID);

					// While we haven't locked and pruned the entities, or until the total elapsed time
					// is greater than our environment variable then attempt to lock and prune.

					int numAttempts = 1;
					while (!complete && pruningHandler.Elapsed() <= m_lockThreadTotalSleepTime) {								
						try {				
							// Attempt to create a transaction and lock the entity.											
							if (m_usePrunerLock) {
								started = pruningHandler.Begin();
								if (started == false )
								{
									iRetVal = 0;
									break;
								}
								locked = pruningHandler.Lock(datasetType, cacheName, 0);														
							}

							if (locked) {
								if ( numAttempts > 1 )							
									m_APMUtils.APM_PrintMessage(argt, "Lock succeeded ! Attempt: " + numAttempts);

								// We have a lock on the required entity, so prune it.  If
								// the entity hasn't been pruned then back it out.

								pruningHandler.Prune();

								ArrayList<PrunerEntityKey> entities = pruningHandler.GetUsedEntities();
								if ( entities.size() > 0 ) {
									iRetVal = ads.backoutDatasetGridCache(cacheName, sPackageName, primaryEntityNum, datasetType,
											m_APMUtils.GetCurrentEntityType(argt).getPrimaryEntityFilter(),
											m_APMUtils.GetCurrentEntityType(argt).getPrimaryEntity());
									APM_EntityJobOps.instance().APM_PrintAllEntityInfo(argt, tMainArgt, tEntityInfo, "Removing entity from ADS: ");								  

									// insert old style incremental stats
									m_APMUtils.InsertIncrementalOldStyleStatistic(argt, secondaryEntityNum, entityVersion, sPackageName);

								} else {
									APM_EntityJobOps.instance().APM_PrintAllEntityInfo(argt, tMainArgt, tEntityInfo, "Entity pruned, not removing from ADS: ");
								}

								complete = true;
							}
							else
							{
								// probably not locked because of a deadlock - wait and try again in next loop
								m_APMUtils.APM_PrintMessage(argt, "Failed to lock...sleeping and then retrying.  Attempt: " + numAttempts);
								pruningHandler.Quit();
								try 
								{
									Thread.sleep(numAttempts * m_lockThreadSleepTime);
								} 
								catch (InterruptedException e) 
								{
									e.printStackTrace();
								}						
								numAttempts = numAttempts + 1;
							}
						}
						finally {
							// Commit the transaction.	
							if (m_usePrunerLock && started && locked) {
								pruningHandler.End();
							}
						}
					}

					if (!complete) {
						iRetVal = 0;
						m_APMUtils.APM_PrintAndLogErrorMessage(iMode, argt, "Failed to get lock for a backout, entity has not been inserted");
					}
				}
			} else if (iUpdateMode == m_APMUtils.APM_BATCH_WRITE) {
				// write the data into ADS
				iRetVal = ads.putAllPendingDatasetGridCache(dataTable, 
						sPackageName, 
						entityGroupId,
						datasetType);
			} 
			else if (iUpdateMode == m_APMUtils.APM_APPLY) {

				EntityPruningHandler pruningHandler = new EntityPruningHandler( iMode, argt, m_APMUtils );								 	
				boolean started = false;
				boolean locked = true;
				boolean complete = false;

				// Populate the pruning handler with the entities...

				Table entityInfo = null;

				if (argt.getColNum("Filtered Entity Info") > 0) {
					// Get the info for the current job.	Will now always match query (job) ID
					entityInfo = argt.getTable("Filtered Entity Info", 1); // use the filtered entity info							
				} else {
					// Otherwise we'll have to get this from the entity info, which is invalid...
					m_APMUtils.APM_PrintAndLogErrorMessage(iMode, argt, "Cannot find \"Filtered Entity Info\" in data, ADS update will fail.\n");				
					return ( 0 );
				}

				// we only care about blocks as single backouts are dealt with above
				for (int i = 1; i <= entityInfo.getNumRows(); i++) 
				{
					int primaryEntityNum = entityInfo.getInt("primary_entity_num", i);
					int secondaryEntityNum = entityInfo.getInt("secondary_entity_num", i);
					int entityVersion = entityInfo.getInt("entity_version", i);

					bIgnoreEntityUpdate = false;
					if ( entityInfo.getColNum("update_mode") > 0 )
					{
						int entityMode = entityInfo.getInt("update_mode", i);

						// check for every entity whether its a backout and if so whether its actually should be done
						// we need to check whether the entity dataset key is being monitored by
						// another service.  If it is then we can skip the backout as the other service will do a backout and apply.
						// if its moving completely out of the monitored set then carry on and do the backout
						if ( entityMode == m_APMUtils.cModeBackout && datasetKeyInAnotherService == true)
						{
							m_APMUtils.APM_PrintMessage(argt, "Entity moving service. Skipping BACKOUT for entity: " + primaryEntityNum + "\n");
							bIgnoreEntityUpdate = true;
						}
					}
					if ( bIgnoreEntityUpdate == false )
						pruningHandler.AddEntity(primaryEntityNum, secondaryEntityNum, entityVersion, serviceID);
				}

				// While we haven't locked and pruned all the entities, or until the total elapsed time
				// is greater than our environment variable then attempt to lock and prune.

				int numAttempts = 1;
				boolean lockSucceeded = false;
				while (!complete && pruningHandler.Elapsed() <= m_lockThreadTotalSleepTime) {								
					try {										
						// Attempt to create a transaction and lock the entities.					
						if (m_usePrunerLock) {
							started = pruningHandler.Begin();
							if (started == false )
							{
								iRetVal = 0;
								break;
							}
							locked = pruningHandler.Lock(datasetType, cacheName, 0);														
						}

						if (locked) {						
							lockSucceeded = true;
							// We have a lock on the required entities, so prune them.
							if ( numAttempts > 1 )							
								m_APMUtils.APM_PrintMessage(argt, "Lock succeeded ! Attempt: " + numAttempts);

							pruningHandler.Prune();

							ArrayList<PrunerEntityKey> used = pruningHandler.GetUsedEntities();
							ArrayList<PrunerEntityKey> pruned = pruningHandler.GetPrunedEntities();

							if ( used.size() > 0 ) {

								// We have some entities to insert, so insert them.
								// Remove any pruned entitiess and apply the block update.

								if ( pruned.size() > 0 ) {
									for (int i = 0; i < pruned.size(); i++ ){
										int primaryEntityNum = pruned.get(i).GetPrimaryEntityNum();
										int secondaryEntityNum = pruned.get(i).GetSecondaryEntityNum();
										int entityVersion = pruned.get(i).GetEntityVersion();

										dataTable.deleteWhereValue(m_APMUtils.GetCurrentEntityType(argt).getPrimaryEntity(), primaryEntityNum);
										m_APMUtils.APM_PrintMessage(argt, m_APMUtils.GetCurrentEntityType(argt).getPrimaryEntity() + " " + primaryEntityNum + " " + m_APMUtils.GetCurrentEntityType(argt).getSecondaryEntity() + " " + secondaryEntityNum + " version " + entityVersion + " not processed into ADS.  Been pruned.");	

										// log info on pruned entity
										foundRow = tEntityInfo.unsortedFindInt("primary_entity_num", primaryEntityNum);
										if (foundRow > 0)
										   APM_EntityJobOps.instance().APM_PrintEntityInfoRow(argt, tMainArgt, tEntityInfo, "Entity pruned, not processed into ADS: ", foundRow);
									}															
								} else {
									// nothing has been pruned so all are being inserted
									APM_EntityJobOps.instance().APM_PrintAllEntityInfo(argt, tMainArgt, tEntityInfo, "Inserting entity into ADS: ");
								}

								// build a list of entities that are going to be inserted
								Set<Integer> entities = new HashSet<Integer>();
								for (int i = 0; i < used.size(); i++ ){
									int primaryEntityNum = used.get(i).GetPrimaryEntityNum();
									entities.add(primaryEntityNum);

									// if anything was pruned that means we'll need to log each individual row info for insertion not all as a block
									if (pruned.size() > 0) {
										foundRow = tEntityInfo.unsortedFindInt("primary_entity_num", primaryEntityNum);
										if (foundRow > 0)
										   APM_EntityJobOps.instance().APM_PrintEntityInfoRow(argt, tMainArgt, tEntityInfo, "Inserting entity into ADS: ", foundRow);
									}
								}

								iRetVal = ads.applyBlockUpdate(dataTable,sPackageName,entities,entityGroupId,datasetType,
										m_APMUtils.GetCurrentEntityType(argt).getPrimaryEntityFilter(),
										m_APMUtils.GetCurrentEntityType(argt).getPrimaryEntity());

								// add the old style statistics for this insertion
								for (int i = 0; i < used.size(); i++ ){
									int secondaryEntityNum = used.get(i).GetSecondaryEntityNum();
									int entityVersion = used.get(i).GetEntityVersion();
									
									m_APMUtils.InsertIncrementalOldStyleStatistic(argt, secondaryEntityNum, entityVersion, sPackageName);
								}

							} else {														
								// everything pruned - print prune message
								APM_EntityJobOps.instance().APM_PrintAllEntityInfo(argt, tMainArgt, tEntityInfo, "Entity pruned, not processed into ADS: ");					
							}

							complete = true;
						}
						else
						{
							// probably not locked because of a deadlock - wait and try again in next loop
							m_APMUtils.APM_PrintMessage(argt, "Failed to lock...sleeping and then retrying.  Attempt: " + numAttempts);
							pruningHandler.Quit();
							try 
							{
								Thread.sleep(numAttempts * m_lockThreadSleepTime);
							} 
							catch (InterruptedException e) 
							{
								e.printStackTrace();
							}						
							numAttempts = numAttempts + 1;
						}
					} finally {
						// Commit the transaction.					
						if (m_usePrunerLock && started && locked) {
							pruningHandler.End();
						}
					}
				}
				if (lockSucceeded == false) {
					// we never got the lock so log an error
					m_APMUtils.APM_PrintAndLogErrorMessage(iMode, argt, "Lock() failed");
					iRetVal = 0;
				}
			}

		} catch (InstantiationException e) {
			m_APMUtils.APM_PrintErrorMessage(argt, "Unable to instantiate ADS implementation" + e);
			iRetVal =0;
			String message = m_APMUtils.getStackTrace(e);
			m_APMUtils.APM_PrintErrorMessage(argt, message+ "\n");
		} catch (IllegalAccessException e) {
			m_APMUtils.APM_PrintErrorMessage(argt, "Illegal Access: Unable to instantiate ADS implementation" + e);
			iRetVal =0;						
			String message = m_APMUtils.getStackTrace(e);
			m_APMUtils.APM_PrintErrorMessage(argt, message+ "\n");
		} catch (ClassNotFoundException e) {
			m_APMUtils.APM_PrintErrorMessage(argt, "ClassNotFound: Unable to instantiate ADS implementation" + e);
			iRetVal =0;						
			String message = m_APMUtils.getStackTrace(e);
			m_APMUtils.APM_PrintErrorMessage(argt, message+ "\n");
		} catch (ADSException e) {
			m_APMUtils.APM_PrintErrorMessage(argt, "Exception while calling ADS: " + e);
			iRetVal =0;						
			String message = m_APMUtils.getStackTrace(e);
			m_APMUtils.APM_PrintErrorMessage(argt, message+ "\n");
		} catch (Exception t) {
			iRetVal =0;						
			m_APMUtils.APM_PrintErrorMessage(argt, "Unknown Exception when calling ADS Update: " + t);
			String message = m_APMUtils.getStackTrace(t);
			m_APMUtils.APM_PrintErrorMessage(argt, message+ "\n");
		}

		return iRetVal;	
	}
	
	/// <summary>
	/// Get the environment variables that are needed for this run... 
	/// </summary>
	private void GetEnvironmentVariables( Table argt ) throws OException
	{
		if ( Str.isEmpty(Util.getEnv("AB_APM_UPDATE_LOCK_THREAD_TOTAL_SLEEP_TIME")) != 1 )
		{
			String lockThreadTotalSleepTime = Util.getEnv("AB_APM_UPDATE_LOCK_THREAD_TOTAL_SLEEP_TIME");

			try
			{
				m_lockThreadTotalSleepTime = Integer.parseInt(lockThreadTotalSleepTime.trim());
				m_lockThreadTotalSleepTime = m_lockThreadTotalSleepTime < 0 ? 0 : m_lockThreadTotalSleepTime;				
			}
			catch (NumberFormatException nfe)
			{
				String warningMessage = 
					"AB_APM_UPDATE_LOCK_THREAD_TOTAL_SLEEP_TIME environment variable has bad format =" +
					lockThreadTotalSleepTime;								
				m_APMUtils.APM_PrintMessage(argt, warningMessage);	
			}			
		}

		if ( Str.isEmpty(Util.getEnv("AB_APM_UPDATE_LOCK_THREAD_SLEEP_TIME")) != 1 )
		{
			String lockThreadSleepTime = Util.getEnv("AB_APM_UPDATE_LOCK_THREAD_SLEEP_TIME");

			try
			{
				m_lockThreadSleepTime = Integer.parseInt(lockThreadSleepTime.trim());
				m_lockThreadSleepTime = m_lockThreadSleepTime < 0 ? 0 : m_lockThreadSleepTime;
			}
			catch (NumberFormatException nfe)
			{
				String warningMessage = 
					"AB_APM_UPDATE_LOCK_THREAD_SLEEP_TIME environment variable has bad format =" +
					lockThreadSleepTime;								
				m_APMUtils.APM_PrintMessage(argt, warningMessage);	
			}	
		}

		if ( Str.isEmpty(Util.getEnv("AB_APM_USE_PRUNER_LOCK")) != 1 )
		{
			String usePrunerLock = Util.getEnv("AB_APM_USE_PRUNER_LOCK");

			try
			{
				m_usePrunerLock = Boolean.parseBoolean(usePrunerLock.trim());
			}
			catch (Exception e)
			{
				String warningMessage = 
					"AB_APM_USE_PRUNER_LOCK environment variable has bad format =" +
					usePrunerLock;								
				m_APMUtils.APM_PrintMessage(argt, warningMessage);	
			}	
		}		
	}	
}
