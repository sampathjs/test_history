package standard.include;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;

public class ConsoleLogging {
	private static final String SECONDARY_ENTITY_NUM_COLUMN = "Current Secondary Entity Num";

	private static final String PRIMARY_ENTITY_NUM_COLUMN = "Current Primary Entity Num";

	private static final String ENTITY_VERSION_COLUMN = "Current Entity Version";

	private static final String OLD_ENTITY_GROUP_COLUMN = "Previous Entity Group Id";

	private static final String ENTITY_GROUP_COLUMN = "Current Entity Group Id";

	private static final String PACKAGE_CONTEXT = "PACKAGE";
	private static final String PACKAGE_COLUMN = "Current Package";

	private static final String SCENARIO_CONTEXT = "SCENARIO";
	private static final String SCENARIO_COLUMN = "Current Scenario";

	private static final String JOB_CONTEXT = "JOB";
	private static final String JOB_COLUMN = "Job Name";

	private static final String DATATABLE_CONTEXT = "DATATABLE";
	private static final String DATASET_TYPE_CONTEXT = "DATASET TYPE";
	private static final String RUN_MODE_CONTEXT = "RUN MODE";
	private static final String SERVICE_CONTEXT = "SERVICE";
	private static final String PID_CONTEXT = "PID";
	private static final String SCRIPT_CONTEXT = "SCRIPT";

	private static ConsoleLogging self;

	private APM_Utils m_APMUtils;
	
	private ConsoleLogging() {
		m_APMUtils = new APM_Utils();
	}

	public static ConsoleLogging instance() throws OException {
		if (self == null) {
			self = new ConsoleLogging();
		}

		return self;
	}

	private static void updateMessageContextTable(Table apmArgumentsTable, String contextName, String contextValue, boolean logContext) throws OException {
		if (logContext == true) {
			Table messageContextTable = apmArgumentsTable.getTable("Message Context", 1);

			int rowNumber = messageContextTable.unsortedFindString("ContextName", contextName, SEARCH_CASE_ENUM.CASE_SENSITIVE);

			if (rowNumber < 1) {
				rowNumber = messageContextTable.addRow();
				messageContextTable.setString("ContextName", rowNumber, contextName);
			}

			messageContextTable.setString("ContextValue", rowNumber, contextValue);
		}
	}

	private static void clearMessageContextTable(Table apmArgumentsTable, String contextName) throws OException {
		Table messageContextTable = apmArgumentsTable.getTable("Message Context", 1);

		int rowNumber = messageContextTable.unsortedFindString("ContextName", contextName, SEARCH_CASE_ENUM.CASE_SENSITIVE);

		if (rowNumber > 0) {
			messageContextTable.setString("ContextValue", rowNumber, "");
		}
	}

	static String getFullMsgContext(Table apmArgumentsTable) {
		String sMsgContext = "";

		try {
			
			if(apmArgumentsTable == null || 
			   apmArgumentsTable.getNumRows() == 0 || 
			   apmArgumentsTable.getTable("Message Context", 1) == null)
			{
				return "[APM]";
			}
			
			Table tMsgContext = apmArgumentsTable.getTable("Message Context", 1);

			for (int iContextRow = 1; iContextRow <= tMsgContext.getNumRows(); iContextRow++) {
				String sContextValue = tMsgContext.getString("ContextValue", iContextRow);

				if (Str.len(sContextValue) > 0) {
					sMsgContext = sMsgContext + "[" + tMsgContext.getString("ContextName", iContextRow) + ": " + sContextValue + "]";
				}
			}
		} catch (OException e) {
			printStackTrace("APM_GetFullMsgContext ", e);
		}

		return sMsgContext;
	}

	public static void printStackTrace(String message, Exception exception) {
		try {
			exception.printStackTrace(new PrintWriter(new StringWriter()));

			OConsole.oprint(message + ":- " + new StringWriter().toString());
		} catch (OException olfException) {
			// Eat the exception or we'll go recursive...
		}
	}

	private static void printStackTrace(Exception exception) {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

		String callerClass = stackTrace.length > 1 ? Thread.currentThread().getStackTrace()[1].getClassName() : "<unknown class>";
		String callerMethod = stackTrace.length > 1 ? Thread.currentThread().getStackTrace()[1].getMethodName() : "<unknown method>";

		printStackTrace(callerClass + "." + callerMethod, exception);
	}

	public void setSecondaryEntityNumContext(Table apmArgumentsTable, int secondaryEntityNum) {
		try {
			String contextValue = String.valueOf(secondaryEntityNum);
			String contextName = m_APMUtils.GetCurrentEntityType(apmArgumentsTable).getSecondaryEntity();
			if ( contextName.length() > 0 )
				updateMessageContextTable(apmArgumentsTable, contextName, contextValue, true);

			apmArgumentsTable.setInt(SECONDARY_ENTITY_NUM_COLUMN, 1, secondaryEntityNum);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void setSecondaryEntityNumContext(Table apmArgumentsTable, String mode) {
		try {
			String contextName = m_APMUtils.GetCurrentEntityType(apmArgumentsTable).getSecondaryEntity();
			if ( contextName.length() > 0 )
			   updateMessageContextTable(apmArgumentsTable, contextName, mode, true);

			apmArgumentsTable.setInt(SECONDARY_ENTITY_NUM_COLUMN, 1, -1);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void unSetSecondaryEntityNumContext(Table apmArgumentsTable) {
		try {
			String contextName = m_APMUtils.GetCurrentEntityType(apmArgumentsTable).getSecondaryEntity();
			if ( contextName != null && contextName.length() > 0 )
			   clearMessageContextTable(apmArgumentsTable, contextName);

			apmArgumentsTable.setInt(SECONDARY_ENTITY_NUM_COLUMN, 1, -1);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void setPrimaryEntityNumContext(Table apmArgumentsTable, int primaryEntityNum) {
		try {
			String contextValue = String.valueOf(primaryEntityNum);
			String contextName = m_APMUtils.GetCurrentEntityType(apmArgumentsTable).getPrimaryEntity();
			if ( contextName.length() > 0 )
			   updateMessageContextTable(apmArgumentsTable, contextName, contextValue, true);

			apmArgumentsTable.setInt(PRIMARY_ENTITY_NUM_COLUMN, 1, primaryEntityNum);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void unSetPrimaryEntityNumContext(Table apmArgumentsTable) {
		try {
			String contextName = m_APMUtils.GetCurrentEntityType(apmArgumentsTable).getPrimaryEntity();
			if ( contextName != null && contextName.length() > 0 )
			   clearMessageContextTable(apmArgumentsTable, contextName);

			apmArgumentsTable.setInt(PRIMARY_ENTITY_NUM_COLUMN, 1, -1);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void setEntityVersionContext(Table apmArgumentsTable, int entityVersion) {
		try {
			String contextValue = String.valueOf(entityVersion);
			String contextName = m_APMUtils.GetCurrentEntityType(apmArgumentsTable).getEntityVersion();
			if ( contextName.length() > 0 )
			   updateMessageContextTable(apmArgumentsTable, contextName, contextValue, true);

			apmArgumentsTable.setInt(ENTITY_VERSION_COLUMN, 1, entityVersion);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void unSetEntityVersionContext(Table apmArgumentsTable) {
		try {
			String contextName = m_APMUtils.GetCurrentEntityType(apmArgumentsTable).getEntityVersion();
			if ( contextName != null && contextName.length() > 0 )
			   clearMessageContextTable(apmArgumentsTable, contextName);

			apmArgumentsTable.setInt(ENTITY_VERSION_COLUMN, 1, -1);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void setPreviousEntityGroupContext(Table apmArgumentsTable, String entityGroupName, int entityGroupId) {
		try {
			String contextName = m_APMUtils.GetCurrentEntityType(apmArgumentsTable).getOldEntityGroupId();
			if ( contextName.length() > 0 )
			   updateMessageContextTable(apmArgumentsTable, contextName, entityGroupName, false);

			apmArgumentsTable.setInt(OLD_ENTITY_GROUP_COLUMN, 1, entityGroupId);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void unSetPreviousEntityGroupContext(Table apmArgumentsTable) {
		try {
			String contextName = m_APMUtils.GetCurrentEntityType(apmArgumentsTable).getOldEntityGroupId();
			if ( contextName != null && contextName.length() > 0 )
			   clearMessageContextTable(apmArgumentsTable, contextName);

			apmArgumentsTable.setInt(OLD_ENTITY_GROUP_COLUMN, 1, -1);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void setEntityGroupContext(Table apmArgumentsTable, String entityGroupName, int entityGroupId) {
		try {
			String contextName = m_APMUtils.GetCurrentEntityType(apmArgumentsTable).getEntityGroupId();
			if ( contextName.length() > 0 )
			   updateMessageContextTable(apmArgumentsTable, contextName, entityGroupName, true);

			apmArgumentsTable.setInt(ENTITY_GROUP_COLUMN, 1, entityGroupId);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void unSetEntityGroupContext(Table apmArgumentsTable) {
		try {
			String contextName = m_APMUtils.GetCurrentEntityType(apmArgumentsTable).getEntityGroupId();
			if ( contextName != null && contextName.length() > 0 )
			   clearMessageContextTable(apmArgumentsTable, contextName);

			apmArgumentsTable.setInt(ENTITY_GROUP_COLUMN, 1, -1);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void setPackageContext(Table apmArgumentsTable, String packageName) {
		try {
			updateMessageContextTable(apmArgumentsTable, PACKAGE_CONTEXT, packageName, true);

			apmArgumentsTable.setString(PACKAGE_COLUMN, 1, packageName);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void unSetPackageContext(Table apmArgumentsTable) {
		try {
			clearMessageContextTable(apmArgumentsTable, PACKAGE_CONTEXT);

			apmArgumentsTable.setString(PACKAGE_COLUMN, 1, "");
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void setJobContext(Table apmArgumentsTable, String jobName) {
		try {
			updateMessageContextTable(apmArgumentsTable, JOB_CONTEXT, jobName, true);

			apmArgumentsTable.setString(JOB_COLUMN, 1, jobName);
		} catch (OException exception) {
			printStackTrace(exception);
		}

	}

	public void unSetJobContext(Table apmArgumentsTable) {
		try {
			clearMessageContextTable(apmArgumentsTable, JOB_CONTEXT);

			apmArgumentsTable.setString(JOB_COLUMN, 1, "");
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void setScenarioContext(Table apmArgumentsTable, String scenarioName, int scenarioId) {
		try {
			updateMessageContextTable(apmArgumentsTable, SCENARIO_CONTEXT, scenarioName, true);

			apmArgumentsTable.setInt(SCENARIO_COLUMN, 1, scenarioId);
		} catch (OException exception) {
			printStackTrace(exception);
		}

	}

	public void unSetScenarioContext(Table apmArgumentsTable) {
		try {
			clearMessageContextTable(apmArgumentsTable, SCENARIO_CONTEXT);

			apmArgumentsTable.setInt(SCENARIO_COLUMN, 1, -1);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void setDatatableContext(Table apmArgumentsTable, String datatableName) {
		try {
			updateMessageContextTable(apmArgumentsTable, DATATABLE_CONTEXT, datatableName, true);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void unSetDatatableContext(Table apmArgumentsTable) {
		try {
			clearMessageContextTable(apmArgumentsTable, DATATABLE_CONTEXT);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void setScriptContext(Table apmArgumentsTable, String scriptName) {
		try {
			updateMessageContextTable(apmArgumentsTable, SCRIPT_CONTEXT, scriptName, true);

		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void unSetScriptContext(Table apmArgumentsTable) {
		try {
			clearMessageContextTable(apmArgumentsTable, SCRIPT_CONTEXT);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void setDatasetTypeContext(Table apmArgumentsTable, String datasetTypeName, int datasetId) {
		try {
			updateMessageContextTable(apmArgumentsTable, DATASET_TYPE_CONTEXT, datasetTypeName, true);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void unSetDatasetTypeContext(Table apmArgumentsTable) {
		try {
			clearMessageContextTable(apmArgumentsTable, DATASET_TYPE_CONTEXT);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void setProcessIdContext(Table apmArgumentsTable, int pid) {
		try {
			String contextValue = String.valueOf(pid);

			updateMessageContextTable(apmArgumentsTable, PID_CONTEXT, contextValue, true);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void unSetProcessIdContext(Table apmArgumentsTable) {
		try {
			clearMessageContextTable(apmArgumentsTable, PID_CONTEXT);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void setServiceContext(Table apmArgumentsTable, String serviceName) {
		try {
			updateMessageContextTable(apmArgumentsTable, SERVICE_CONTEXT, serviceName, true);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void unSetServiceContext(Table apmArgumentsTable) {
		try {
			clearMessageContextTable(apmArgumentsTable, SERVICE_CONTEXT);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void setRunModeContext(Table apmArgumentsTable, String runMode) {
		try {
			updateMessageContextTable(apmArgumentsTable, RUN_MODE_CONTEXT, runMode, true);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void unSetRunModeContext(Table apmArgumentsTable) {
		try {
			clearMessageContextTable(apmArgumentsTable, RUN_MODE_CONTEXT);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}
}
