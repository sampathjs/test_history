package com.matthey.openlink.dispatch;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.matthey.openlink.reporting.runner.generators.GenerateAndOverrideParameters;
import com.matthey.openlink.reporting.runner.generators.IRequiredParameters;
import com.matthey.openlink.reporting.runner.parameters.IReportParameters;
import com.matthey.openlink.reporting.runner.parameters.ReportParameters;
import com.matthey.openlink.reporting.runner.parameters.ReportRunnerParameters;
import com.matthey.openlink.utilities.DataAccess;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.connex.AbstractGearAssembly;
import com.olf.embedded.connex.EnumRequestData;
import com.olf.embedded.connex.EnumRequestStatus;
import com.olf.embedded.connex.Request;
import com.olf.embedded.connex.RequestData;
import com.olf.embedded.connex.RequestException;
import com.olf.jm.logging.Logging;
import com.olf.jm.sqlInjection.SqlInjectionException;
import com.olf.jm.sqlInjection.SqlInjectionFilter;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableColumn;

/**
 * Connex dispatch handler for EJM requests
 * 
 * @version $Revision: $
 */
@ScriptCategory({ EnumScriptCategory.Connex })
public class EJM_Dispatcher extends AbstractGearAssembly {
	public static final String CONST_REPO_CONTEXT = "Interfaces"; // context of constants repository
	public static final String CONST_REPO_SUBCONTEXT = "eJM"; // sub context of constants repository

	private static final String REPORT_PARAMTERS = "USER_report_parameters";
	private static SqlInjectionFilter injectionFilter = null;

	@Override
	public void process(Context context, Request request, RequestData requestData) {
		initLogging(context);
	
		Logging.info(String.format("%s...STARTING", this.getClass().getSimpleName()));
		long started = System.nanoTime();
		// context.getDebug().viewTable(requestData.getInputTable());
		try {
			Logging.info(String.format("\n\t %s CALLED!!! with %s", this.getClass(), request.getMethodName()));
			Logging.info(String.format("\n\t Request:%s\n\n", request.asTable().asXmlString()));
			
			Map<String, String> reportParameters = getReportParameters(context, request.getMethodName());
			Table returnTable = processRBprocessCustomRequest(context, requestData.getInputTable(), reportParameters);

			request.setResultTable( getWSReturnTable(context, returnTable, reportParameters.get("Response"), reportParameters.get("ResponseType"), reportParameters.get("urn")), EnumRequestData.List);

		} catch ( RuntimeException re) {
			throw new RequestException(EnumRequestStatus.FailureMethodAborted, "Error: " + re.getLocalizedMessage());
			
	    } catch ( Exception e) {
			Logging.error(String.format("ERR: during EJM WS Dispatching:%s", e.getLocalizedMessage()), e);
			throw new RequestException(EnumRequestStatus.FailureMethodAborted, "Error: " + e.getLocalizedMessage());

		} finally {
			Logging.info(String.format("EJM Dispatch... FINISHED in %dms", TimeUnit.MILLISECONDS.convert(System.nanoTime() - started, TimeUnit.NANOSECONDS)));
		}

	}

	private void initLogging(Context context) {

		try {
			Logging.init(context, this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
		} catch (Exception e) {
			throw new RuntimeException("Error initializing Logging", e);
		}			
	}

	/**
	 * retrieve all the configured parameters with a task_name matching the
	 * supplied method
	 */
	private Map<String, String> getReportParameters(Context context, String methodName) {

		Map<String, String> parameters = new HashMap<String, String>(0);
		String reportIdentifer = "task_name";
		String parameterColumn = "parameter_name";
		String valueColumn = "parameter_value";

		Table reportData = null;
		try {

			reportData = DataAccess.getDataFromTable(context, String.format("SELECT * FROM %s" + "\nWHERE %s='%s' ", "USER_jm_report_parameters", reportIdentifer, methodName));

			int numberOfRows = reportData.getRowCount();
			if (numberOfRows == 0) {
				throw new ReportRunnerParameters(String.format("%s for WS %s", "No parameter data found", methodName));
			}

			for (int row = 0; row < numberOfRows; row++) {
				String parameterName = reportData.getString(parameterColumn, row);
				String parameterValue = reportData.getString(valueColumn, row);
				parameters.put(parameterName, parameterValue);
			}

		} catch (Exception ex) {
			throw new ReportRunnerParameters("Error loading parameters from user table. " + ex.getMessage());

		} finally {
			if (reportData != null) {
				try {
					reportData.dispose();

				} catch (Exception e) {
					Logging.error (String.format("Exception thrown in finally block. " + e.getMessage()), e);
				}
			}
		}
		return parameters;
	}

	/**
	 * wrapper to submit request to ReportBuilder and return the results of that
	 * processing
	 */
	private Table processRBprocessCustomRequest(Context context, Table argumentsTable, Map<String, String> parameters) {

		Table output = null;
		Table resultData = null;
		int result = 0;
		try {
			Logging.info(String.format("Starting report %s...", parameters.get(IRequiredParameters.NAME_PARAMETER)));

			for (TableColumn inputArgument : argumentsTable.getColumns()) {
				// System.out.println(String.format("col:%s",inputArgument.getName()));
				String parameterValue = sanitiseParameter(inputArgument.getName(), argumentsTable.getString(inputArgument.getName(), 0));
				parameters.put(inputArgument.getName(), parameterValue);
			}

			String requestDate = null;
			String reportDate[] = new String[] { "date", "reporDate" };
			for (int item = 0; item < reportDate.length; item++) {
				if (parameters.containsKey(reportDate[item])){
					requestDate = parameters.remove(reportDate[item]);
				}
				break;
			}
			if (null == requestDate) {
				requestDate = new SimpleDateFormat("dd-MMM-yyyy").format(context.getBusinessDate());
			}
			Logging.info(String.format("Using ReporDate:%s", requestDate));
			parameters.put("ReportDate", requestDate);
			// System.out.println("Params..." + parameters.toString());
			IReportParameters newParameters = new ReportParameters(context, parameters);

			GenerateAndOverrideParameters balances = new GenerateAndOverrideParameters(context, newParameters);
			if (balances.generate()) {
				output = balances.getResults();
				if (null != output && output.getRowCount() > 0){
					result = 1;
				}
			}
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
		
			e.printStackTrace();
		}

		if (result < 1) {
			Logging.info(String.format("\n\t %s FAILED!!!\n", parameters.get(IRequiredParameters.NAME_PARAMETER)));
			return null;
			
		} else {
			Logging.info(String.format("\n\t %s OK!!!\n\n", parameters.get(IRequiredParameters.NAME_PARAMETER)));
			try {
				resultData = output.cloneData();
				resultData.addRow();
				Logging.info ("DEBUG: " + String.format("RESULT:" + resultData.asXmlString()));
				
			} catch (Exception e) {
				Logging.error(String.format("\n\t Dispatcher error cloning result!\n\n"), e);
				e.printStackTrace();
			}
			return output;
		}
	}

	/**
	 * Cleanse passed arguments to reduce SQL injection risk
	 * @param parameter
	 * @param value
	 * @return
	 * @throws SqlInjectionException 
	 */
	private String sanitiseParameter(final String parameter, String value) throws SqlInjectionException {
		if (null == injectionFilter){
			injectionFilter = new SqlInjectionFilter();
		}
		
		try {
			injectionFilter.doFilter(value);
			
		} catch (SqlInjectionException err) {
				value = "";
				Logging.error(String.format("Parameter(%s) failed cleansing reason: %s",parameter, err.getLocalizedMessage()), err);
				throw new RuntimeException(err.getLocalizedMessage());
			}
		return value;
	}


	/**
	 * overlay request results with effective namespace to return to consumer
	 * 
	 * @return
	 */
	Table getWSReturnTable(Context context, Table results, String element, String responseName, String argumentNamespace) {

		Table WSTable = null;
		try {
			Logging.info ("DEBUG: " + String.format("\n ELEMENT=%s\n\n", element));
			WSTable = context.getTableFactory().createTable(this.getClass().getSimpleName());

			if (null != responseName) {
				WSTable.addColumn(element, EnumColType.Table);
				WSTable.addRow();

				results.setName(responseName);
				WSTable.setTable(element, 0, results);

			} else {
				WSTable.addColumn(element, EnumColType.Table);
				WSTable.addRow();
				results.setName(element);
				WSTable.setTable(element, 0, results);
			}

			context.getConnexFactory().setXMLNamespace(WSTable, argumentNamespace);

			Logging.info ("DEBUG: " + String.format("\n\nXML:-\n" + WSTable.asXmlString() + "\n<XML\n"));
			
		} catch (Exception e) {
			Logging.error (String.format("Error setting Namespace....\n\n"), e);
			e.printStackTrace();
		}
		return WSTable;

	}

}
