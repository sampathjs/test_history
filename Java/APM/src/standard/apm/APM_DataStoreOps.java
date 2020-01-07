/* Released with version 29-Oct-2015_V14_2_4 of APM */

package standard.apm;

import standard.include.APM_Utils;
import standard.include.APM_Utils.EntityType;
import standard.include.ConsoleLogging;
import standard.apm.ADS_DataStoreOps;
import standard.apm.SQLITE_DataStoreOps;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.olf.openjvs.fnd.ServicesBase;

public class APM_DataStoreOps
{
	private APM_Utils m_APMUtils;

	private static APM_DataStoreOps self;
	
	private APM_DataStoreOps() {
		m_APMUtils = new APM_Utils();
	}

	public static APM_DataStoreOps instance() throws OException {
		if (self == null) {
			self = new APM_DataStoreOps();
		}

		return self;
	}
	
	public int Update(int iUpdateMode, int iMode, int entityGroupId, String sPackageName, int datasetType, int serviceID, boolean datasetKeyInAnotherService, String sJobName, Table tMainArgt, Table tPackageDataTables, Table argt) throws OException 
	{
		int iRetVal = 1;
		boolean useADS = m_APMUtils.useADS(argt);
	   	EntityType entityType = m_APMUtils.GetCurrentEntityType(argt);
	   	
	   	switch (entityType)
	   	{
	   		case DEAL:
	   		case NOMINATION:
	   			if (useADS)
	   				return ADS_DataStoreOps.instance().Update(iUpdateMode, iMode, entityGroupId, sPackageName, datasetType, serviceID, datasetKeyInAnotherService, sJobName, tMainArgt, tPackageDataTables, argt);
	   			else
	   				return SQLITE_DataStoreOps.instance().Update(iUpdateMode, iMode, entityGroupId, sPackageName, datasetType, serviceID, datasetKeyInAnotherService, sJobName, tMainArgt, tPackageDataTables, argt);
	   		case UNKNOWN:
	   			m_APMUtils.APM_PrintMessage(argt, "Unsupported Entity Mode: " + entityType);
	   			throw new OException("Unsupported Entity Mode: " + entityType);	
	   	}
	   	return 0;
	}
}
