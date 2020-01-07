package standard.include;

import java.io.PrintWriter;
import java.io.StringWriter;

import standard.apm.statistics.logger.LoggingUtilities;

import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;

public class StatisticsLogging {
	private static final String TRAN_CONTEXT = "TRAN";
	private static final String TRAN_COLUMN = "Current TranNum";

	private static final String DEAL_CONTEXT = "DEAL";
	private static final String DEAL_COLUMN = "Current DealNum";

	private static final String VERSION_CONTEXT = "VERSION";
	private static final String VERSION_COLUMN = "Current DealNum Version";

	private static final String OLD_PORTFOLIO_CONTEXT = "OLD_PORTFOLIO";
	private static final String OLD_PORTFOLIO_COLUMN = "Previous Portfolio";

	private static final String PORTFOLIO_CONTEXT = "PORTFOLIO";
	private static final String PORTFOLIO_COLUMN = "Current Portfolio";

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

	private static StatisticsLogging self;

	private StatisticsLogging() {
	}

	public static StatisticsLogging instance() throws OException {
		if (self == null) {
			self = new StatisticsLogging();
		}

		return self;
	}

	public void clearLockFiles() {
		LoggingUtilities.clearLockFiles();
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

	static String getFullMsgContext(Table tAPMArgumentTable) {
		String sMsgContext = "";

		try {
			Table tMsgContext = tAPMArgumentTable.getTable("Message Context", 1);

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

	public void setTranContext(Table apmArgumentsTable, int tranNumber) {
		try {
			String contextValue = String.valueOf(tranNumber);

			updateMessageContextTable(apmArgumentsTable, TRAN_CONTEXT, contextValue, true);

			apmArgumentsTable.setInt(TRAN_COLUMN, 1, tranNumber);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void setTranContext(Table apmArgumentsTable, String mode) {
		try {
			updateMessageContextTable(apmArgumentsTable, TRAN_CONTEXT, mode, true);

			apmArgumentsTable.setInt(TRAN_COLUMN, 1, -1);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void unSetTranContext(Table apmArgumentsTable) {
		try {
			clearMessageContextTable(apmArgumentsTable, TRAN_CONTEXT);

			apmArgumentsTable.setInt(TRAN_COLUMN, 1, -1);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void setDealContext(Table apmArgumentsTable, int dealNumber) {
		try {
			String contextValue = String.valueOf(dealNumber);

			updateMessageContextTable(apmArgumentsTable, DEAL_CONTEXT, contextValue, true);

			apmArgumentsTable.setInt(DEAL_COLUMN, 1, dealNumber);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void unSetDealContext(Table apmArgumentsTable) {
		try {
			clearMessageContextTable(apmArgumentsTable, DEAL_CONTEXT);

			apmArgumentsTable.setInt(DEAL_COLUMN, 1, -1);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void setDealVersionContext(Table apmArgumentsTable, int dealVersion) {
		try {
			String contextValue = String.valueOf(dealVersion);

			updateMessageContextTable(apmArgumentsTable, VERSION_CONTEXT, contextValue, true);

			apmArgumentsTable.setInt(VERSION_COLUMN, 1, dealVersion);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void unSetDealVersionContext(Table apmArgumentsTable) {
		try {
			clearMessageContextTable(apmArgumentsTable, VERSION_CONTEXT);

			apmArgumentsTable.setInt(VERSION_COLUMN, 1, -1);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void setPreviousPortfolioContext(Table apmArgumentsTable, String portfolioName, int portfolioId) {
		try {
			updateMessageContextTable(apmArgumentsTable, OLD_PORTFOLIO_CONTEXT, portfolioName, false);

			apmArgumentsTable.setInt(OLD_PORTFOLIO_COLUMN, 1, portfolioId);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void unSetPreviousPortfolioContext(Table apmArgumentsTable) {
		try {
			clearMessageContextTable(apmArgumentsTable, OLD_PORTFOLIO_CONTEXT);

			apmArgumentsTable.setInt(OLD_PORTFOLIO_COLUMN, 1, -1);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void setPortfolioContext(Table apmArgumentsTable, String portfolioName, int portfolioId) {
		try {
			updateMessageContextTable(apmArgumentsTable, PORTFOLIO_CONTEXT, portfolioName, true);

			apmArgumentsTable.setInt(PORTFOLIO_COLUMN, 1, portfolioId);
		} catch (OException exception) {
			printStackTrace(exception);
		}
	}

	public void unSetPortfolioContext(Table apmArgumentsTable) {
		try {
			clearMessageContextTable(apmArgumentsTable, PORTFOLIO_CONTEXT);

			apmArgumentsTable.setInt(PORTFOLIO_COLUMN, 1, -1);
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
