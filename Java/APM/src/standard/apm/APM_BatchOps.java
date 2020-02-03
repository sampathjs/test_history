/* Released with version 29-Oct-2015_V14_2_4 of APM */

package standard.apm;

import standard.include.APM_Utils;

import com.olf.openjvs.*;

public class APM_BatchOps
{
	private APM_Utils m_APMUtils;
	private APM_BatchOps_ADS m_APMBatchOpsADS;
	private APM_BatchOps_SQLITE m_APMBatchOpsSQLITE;
	
	public APM_BatchOps() {
		// we do it like this so that if ADS switched on or off while engine up there is no problem with having to reinstantiate the relevant objects
		m_APMUtils = new APM_Utils();
		m_APMBatchOpsADS = new APM_BatchOps_ADS();
		m_APMBatchOpsSQLITE = new APM_BatchOps_SQLITE();
	}

	public void runStatusScript(Table tAPMArgumentTable) throws OException
	{
   	if ( m_APMUtils.useADS(tAPMArgumentTable))
   		m_APMBatchOpsADS.runStatusScript(tAPMArgumentTable);		
	}
	
   public int initialiseDatasets(Table tAPMArgumentTable, int entityGroupId) throws OException
   {
   	if ( m_APMUtils.useADS(tAPMArgumentTable))
   		return m_APMBatchOpsADS.initialiseDatasets(tAPMArgumentTable, entityGroupId);
   	else
   		return m_APMBatchOpsSQLITE.initialiseDatasets(tAPMArgumentTable, entityGroupId);
   		
   }

   public int commitPendingDatasets(Table tAPMArgumentTable, int entityGroupId) throws OException
   {
   	if ( m_APMUtils.useADS(tAPMArgumentTable))
   		return m_APMBatchOpsADS.commitPendingDatasets(tAPMArgumentTable, entityGroupId);
   	else
   		return m_APMBatchOpsSQLITE.commitPendingDatasets(tAPMArgumentTable, entityGroupId);	
   }
   
}
