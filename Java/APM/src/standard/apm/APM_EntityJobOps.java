/* Released with version 29-Aug-2019_V17_0_124 of APM */

package standard.apm;

import standard.include.APM_Utils;
import standard.include.APM_Utils.EntityType;
import standard.include.ConsoleLogging;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.olf.openjvs.fnd.ServicesBase;

public class APM_EntityJobOps
{
	private APM_Utils m_APMUtils;

	private static APM_EntityJobOps self;
	
	private APM_EntityJobOps() {
		m_APMUtils = new APM_Utils();
	}

	public static APM_EntityJobOps instance() throws OException {
		if (self == null) {
			self = new APM_EntityJobOps();
		}

		return self;
	}

    public QueryRequest createQueryIdFromMainArgt(int iMode, Table tAPMArgumentTable, Table argt) throws OException
    {
   	 	EntityType entityType = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable);
	   	switch (entityType)
	   	{
	   	    case DEAL:
	   	    	return APM_ExecuteDealQuery.instance().createQueryIdFromMainArgt(iMode, tAPMArgumentTable, argt);
	   	    case NOMINATION:
	   	    	return APM_ExecuteNominationQuery.instance().createQueryIdFromMainArgt(iMode, tAPMArgumentTable, argt);
	   	    case UNKNOWN:
	   	    	m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Unsupported Entity Mode: " + entityType);
	   	    	throw new OException("Unsupported Entity Mode: " + entityType);	
	   	}
	   	return null;
    }
    
    public QueryRequest createQueryIdFromMainArgt(int iMode, Table tAPMArgumentTable, Table argt, int job_group_id) throws OException
    {
   	 	EntityType entityType = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable);
   	 	switch (entityType)
   	 	{
   	 		case DEAL:
   	 			return APM_ExecuteDealQuery.instance().createQueryIdFromMainArgt(iMode, tAPMArgumentTable, argt, job_group_id);
   	 		case NOMINATION:
   	 			return APM_ExecuteNominationQuery.instance().createQueryIdFromMainArgt(iMode, tAPMArgumentTable, argt, job_group_id);
   	 		case UNKNOWN:
   	 			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Unsupported Entity Mode: " + entityType);
   	 			throw new OException("Unsupported Entity Mode: " + entityType);	
   	 	}
   	 	return null;
    }

	public int APM_FindLaunchType(Table tMainArgt, Table tAPMArgumentTable) throws OException
	{
   	 	EntityType entityType = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable);
   	 	switch (entityType)
   	 	{
   	 		case DEAL:
   	 			return APM_DealJobOps.instance().APM_FindLaunchType(tMainArgt, tAPMArgumentTable);
   	 		case NOMINATION:
   	 			return APM_NominationJobOps.instance().APM_FindLaunchType(tMainArgt, tAPMArgumentTable);
   	 		case UNKNOWN:
   	 			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Unsupported Entity Mode: " + entityType);
   	 			throw new OException("Unsupported Entity Mode: " + entityType);	
   	 	}
   	 	return m_APMUtils.cModeDoNothing;
	}

	public int APM_AdjustLaunchType(int mode, Table tAPMArgumentTable, int entityGroupId) throws OException
	{
   	 	EntityType entityType = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable);
   	 	switch (entityType)
   	 	{
   	 		case DEAL:
   	 			return APM_DealJobOps.instance().APM_AdjustLaunchType(mode, tAPMArgumentTable, entityGroupId);
   	 		case NOMINATION:
   	 			return APM_NominationJobOps.instance().APM_AdjustLaunchType(mode, tAPMArgumentTable, entityGroupId);
   	 		case UNKNOWN:
   	 			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Unsupported Entity Mode: " + entityType);
   	 			throw new OException("Unsupported Entity Mode: " + entityType);	
   	 	}
   	 	return m_APMUtils.cModeDoNothing;
	}

	public int APM_GetSelectedEntityGroups(int iMode, Table tAPMArgumentTable) throws OException {
   	 	EntityType entityType = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable);
   	 	switch (entityType)
   	 	{
   	 		case DEAL:
   	 			return APM_DealJobOps.instance().APM_GetSelectedPortfolios(iMode, tAPMArgumentTable);
   	 		case NOMINATION:
   	 			return APM_NominationJobOps.instance().APM_GetSelectedPipelines(iMode, tAPMArgumentTable);
   	 		case UNKNOWN:
   	 			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Unsupported Entity Mode: " + entityType);
   	 			throw new OException("Unsupported Entity Mode: " + entityType);	
   	 	}
   	 	return m_APMUtils.cModeDoNothing;
	}

	public int APM_CreateQueryIDForEntityGroup(int mode, Table tAPMArgumentTable, int entityGroup) throws OException {
   	 	EntityType entityType = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable);
   	 	switch (entityType)
   	 	{
   	 		case DEAL:
   	 			return APM_DealJobOps.instance().APM_CreateQueryIDForPfolio(mode, tAPMArgumentTable, entityGroup);
   	 		case NOMINATION:
   	 			return APM_NominationJobOps.instance().APM_CreateQueryIDForPipeline(mode, tAPMArgumentTable, entityGroup);
   	 		case UNKNOWN:
   	 			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Unsupported Entity Mode: " + entityType);
   	 			throw new OException("Unsupported Entity Mode: " + entityType);	
   	 	}
   	 	return 0;
	}

	public int APM_EnrichEntityTable(int iMode, Table tAPMArgumentTable, int iQueryId) throws OException {
   	 	EntityType entityType = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable);
   	 	switch (entityType)
   	 	{
   	 		case DEAL:
   	 			return APM_DealJobOps.instance().APM_EnrichDealTable(iMode, tAPMArgumentTable, iQueryId);
   	 		case NOMINATION:
   	 			return APM_NominationJobOps.instance().APM_EnrichNominationTable(iMode, tAPMArgumentTable, iQueryId);
   	 		case UNKNOWN:
   	 			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Unsupported Entity Mode: " + entityType);
   	 			throw new OException("Unsupported Entity Mode: " + entityType);	
   	 	}
   	 	return 0;
	}

	public int RemoveBackoutsFromQueryID(int mode, Table tAPMArgumentTable, int entityGroupId, int entityGroupQueryID)  throws OException {
   	 	EntityType entityType = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable);
   	 	switch (entityType)
   	 	{
   	 		case DEAL:
   	 			return APM_DealJobOps.instance().RemoveDealUpdateBackoutsFromQueryID(mode, tAPMArgumentTable, entityGroupId, entityGroupQueryID);
   	 		case NOMINATION:
   	 			return APM_NominationJobOps.instance().RemoveNominationBackoutsFromQueryID(mode, tAPMArgumentTable, entityGroupId, entityGroupQueryID);
   	 		case UNKNOWN:
   	 			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Unsupported Entity Mode: " + entityType);
   	 			throw new OException("Unsupported Entity Mode: " + entityType);	
   	 	}
   	 	return 0;
	}

	public void APM_PrintEntityInfoRow(Table tAPMArgumentTable, Table argt, Table tEntityInfo, String header, int row) throws OException {
   	 	EntityType entityType = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable);
   	 	switch (entityType)
   	 	{
   	 		case DEAL:
   	 			APM_DealJobOps.instance().APM_PrintDealInfoRow(tAPMArgumentTable, argt, tEntityInfo, header, row);
   	 			return;
   	 		case NOMINATION:
   	 			APM_NominationJobOps.instance().APM_PrintNominationInfoRow(tAPMArgumentTable, argt, tEntityInfo, header, row);
   	 			return;
   	 		case UNKNOWN:
   	 			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Unsupported Entity Mode: " + entityType);
   	 			throw new OException("Unsupported Entity Mode: " + entityType);	
   	 	}
   	 	return;
	}

	public void APM_PrintAllEntityInfo(Table tAPMArgumentTable, Table argt, Table tEntityInfo, String header) throws OException {
   	 	EntityType entityType = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable);
   	 	switch (entityType)
   	 	{
   	 		case DEAL:
   	 			APM_DealJobOps.instance().APM_PrintAllDealInfo(tAPMArgumentTable, argt, tEntityInfo, header);
   	 			return;
   	 		case NOMINATION:
   	 			APM_NominationJobOps.instance().APM_PrintAllNominationInfo(tAPMArgumentTable, argt, tEntityInfo, header);
   	 			return;
   	 		case UNKNOWN:
   	 			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Unsupported Entity Mode: " + entityType);
   	 			throw new OException("Unsupported Entity Mode: " + entityType);	
   	 	}
   	 	return;
	}
	
	public void APM_SetEntityInfoCopyFromArgt(Table tAPMArgumentTable, Table argt) throws OException {
   	 	EntityType entityType = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable);
   	 	switch (entityType)
   	 	{
   	 		case DEAL:
   	 			if (m_APMUtils.APM_CheckColumn(argt, "Deal Info", COL_TYPE_ENUM.COL_TABLE.toInt()) != 0 )
   	 			   tAPMArgumentTable.setTable("Global Filtered Entity Info", 1, APM_DealJobOps.instance().APM_RetrieveDealInfoCopyFromArgt(tAPMArgumentTable, argt));
   	 			return;
   	 		case NOMINATION:
   	 			if (m_APMUtils.APM_CheckColumn(argt, "Nom Info", COL_TYPE_ENUM.COL_TABLE.toInt()) != 0 )
   	 			   tAPMArgumentTable.setTable("Global Filtered Entity Info", 1, APM_NominationJobOps.instance().APM_RetrieveNominationInfoCopyFromArgt(tAPMArgumentTable, argt));
   	 			return;
   	 		case UNKNOWN:
   	 			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Unsupported Entity Mode: " + entityType);
   	 			throw new OException("Unsupported Entity Mode: " + entityType);	
   	 	}
   	 	return;
	}

	public int APM_FilterEntityInfoTable(Table tAPMArgumentTable, Table argt, Table tEntityInfo) throws OException {
   	 	EntityType entityType = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable);
   	 	switch (entityType)
   	 	{
   	 		case DEAL:
   	 			if (m_APMUtils.APM_CheckColumn(argt, "Deal Info", COL_TYPE_ENUM.COL_TABLE.toInt()) != 0 )
   	 			   return APM_DealJobOps.instance().APM_FilterDealInfoTable(tAPMArgumentTable, argt, tEntityInfo);
   	 			else
   	 			   return 1;

   	 		case NOMINATION:
   	 			if (m_APMUtils.APM_CheckColumn(argt, "Nom Info", COL_TYPE_ENUM.COL_TABLE.toInt()) != 0 )
   	 			   return APM_NominationJobOps.instance().APM_FilterNominationInfoTable(tAPMArgumentTable, argt, tEntityInfo);
   	 			else
   	 			   return 1;

   	 		case UNKNOWN:
   	 			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Unsupported Entity Mode: " + entityType);
   	 			throw new OException("Unsupported Entity Mode: " + entityType);	
   	 	}
   	 	return 0;
	}

	public boolean SetArgtInfoReturnStatus(Table tAPMArgumentTable, Table argt, int status) throws OException {
   	 	EntityType entityType = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable);
   	 	switch (entityType)
   	 	{
   	 		case DEAL:
   	 			return APM_DealJobOps.instance().SetDealArgtReturnStatus(argt, status);
   	 		case NOMINATION:
   	 			return APM_NominationJobOps.instance().SetNomArgtReturnStatus(argt, status);
   	 		case UNKNOWN:
   	 			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Unsupported Entity Mode: " + entityType);
   	 			throw new OException("Unsupported Entity Mode: " + entityType);	
   	 	}
   	 	return false;
	}
	
	public void SetBlockUpdateStatuses(Table tAPMArgumentTable, Table tMainArgt, Table blockFails) throws OException {
   	 	EntityType entityType = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable);
   	 	switch (entityType)
   	 	{
   	 		case DEAL:
   	 			APM_DealJobOps.instance().SetBlockUpdateStatuses(tMainArgt, blockFails);
   	 			return;
   	 		case NOMINATION:
   	 			APM_NominationJobOps.instance().SetBlockUpdateStatuses(tMainArgt, blockFails);
   	 			return;
   	 		case UNKNOWN:
   	 			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Unsupported Entity Mode: " + entityType);
   	 			throw new OException("Unsupported Entity Mode: " + entityType);	
   	 	}
   	 	return;
	}

	public Table GetDatasetKeysForService(Table tAPMArgumentTable, int serviceID, String serviceName) throws OException {
   	 	EntityType entityType = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable);
   	 	switch (entityType)
   	 	{
   	 		case DEAL:
   	 			return APM_DealJobOps.instance().GetDatasetKeysForService(tAPMArgumentTable, serviceID, serviceName);
   	 		case NOMINATION:
   	 			return APM_NominationJobOps.instance().GetDatasetKeysForService(tAPMArgumentTable, serviceID, serviceName);
   	 		case UNKNOWN:
   	 			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Unsupported Entity Mode: " + entityType);
   	 			throw new OException("Unsupported Entity Mode: " + entityType);	
   	 	}
   	 	return Util.NULL_TABLE;
	}

	public int GetUnderlyingSimResultRow(int iMode, Table tAPMArgumentTable, String sDataTableName, Table tPackageDataTableCols) throws OException {
   	 	EntityType entityType = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable);
   	 	switch (entityType)
   	 	{
   	 		case DEAL:
   	 			return APM_DealJobOps.instance().GetUnderlyingSimResultRow(iMode, tAPMArgumentTable, sDataTableName, tPackageDataTableCols);
   	 		case NOMINATION:
   	 			return APM_NominationJobOps.instance().GetUnderlyingSimResultRow(iMode, tAPMArgumentTable, sDataTableName, tPackageDataTableCols);
   	 		case UNKNOWN:
   	 			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Unsupported Entity Mode: " + entityType);
   	 			throw new OException("Unsupported Entity Mode: " + entityType);	
   	 	}
   	 	return 0;
	}

	public int CheckSimResultsValid(int iMode, Table tAPMArgumentTable, Table results) throws OException {
   	 	EntityType entityType = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable);
   	 	switch (entityType)
   	 	{
   	 		case DEAL:
   	 			return APM_DealJobOps.instance().CheckSimResultsValid(iMode, tAPMArgumentTable, results);
   	 		case NOMINATION:
   	 			return APM_NominationJobOps.instance().CheckSimResultsValid(iMode, tAPMArgumentTable, results);
   	 		case UNKNOWN:
   	 			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Unsupported Entity Mode: " + entityType);
   	 			throw new OException("Unsupported Entity Mode: " + entityType);	
   	 	}
   	 	return 0;
	}

	public Table EnrichSimulationResults(Table tAPMArgumentTable, Table results, int iType, int iResultClass, String sWhat) throws OException {
   	 	EntityType entityType = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable);
   	 	switch (entityType)
   	 	{
   	 		case DEAL:
   	 			return APM_DealJobOps.instance().EnrichSimulationResults(results, iType, iResultClass, sWhat);
   	 		case NOMINATION:
   	 			return APM_NominationJobOps.instance().EnrichSimulationResults(results, iType, iResultClass, sWhat);
   	 		case UNKNOWN:
   	 			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Unsupported Entity Mode: " + entityType);
   	 			throw new OException("Unsupported Entity Mode: " + entityType);	
   	 	}
   	 	return Util.NULL_TABLE;
	}

	public int SetupRevalParamForEntityType(int iMode, Table tAPMArgumentTable, String sJobName, int iQueryId, int entityGroupId, Table tRevalParam) throws OException {
   	 	EntityType entityType = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable);
   	 	switch (entityType)
   	 	{
   	 		case DEAL:
   	 			return APM_DealJobOps.instance().SetupRevalParamForDeals(iMode, tAPMArgumentTable, sJobName, iQueryId, entityGroupId, tRevalParam);
   	 		case NOMINATION:
   	 			return APM_NominationJobOps.instance().SetupRevalParamForNominations(iMode, tAPMArgumentTable, sJobName, iQueryId, entityGroupId, tRevalParam);
   	 		case UNKNOWN:
   	 			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Unsupported Entity Mode: " + entityType);
   	 			throw new OException("Unsupported Entity Mode: " + entityType);	
   	 	}
   	 	return 0;
	}

	public int SetUpArgumentTableForEntityType(Table tAPMArgumentTable, Table argt) throws OException {
   	 	EntityType entityType = m_APMUtils.GetCurrentEntityType(tAPMArgumentTable);
   	 	switch (entityType)
   	 	{
   	 		case DEAL:
   	 			return APM_DealJobOps.instance().SetUpArgumentTableForDeals(tAPMArgumentTable, argt);
   	 		case NOMINATION:
   	 			return APM_NominationJobOps.instance().SetUpArgumentTableForNominations(tAPMArgumentTable, argt);
   	 		case UNKNOWN:
   	 			m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "Unsupported Entity Mode: " + entityType);
   	 			throw new OException("Unsupported Entity Mode: " + entityType);	
   	 	}
   	 	return 0;
	}
	
}
