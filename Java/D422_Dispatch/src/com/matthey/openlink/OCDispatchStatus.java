package com.matthey.openlink;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import com.matthey.openlink.utilities.DataAccess;
import com.matthey.openlink.utilities.Repository;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.staticdata.EnumFieldType;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.ProcessDefinition;
import com.olf.openrisk.tpm.Task;
import com.olf.openrisk.tpm.Variable;
import com.olf.openrisk.tpm.Variables;
import com.olf.openrisk.trading.EnumResetDefinitionFieldId;
import com.olf.openrisk.trading.EnumTranStatusInternalProcessing;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.ResetDefinition;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.logging.PluginLog;


/**
 * D425 - If the tranInfo {@value #DISPATCH_STATUS_COLUMN} is set to {@value #DISPATCH_TRIGGER_STATUS_VALUE} 
 * and was previously something else then determine the TPM workflow to run. This can only be done from the Post-Processing step so we need to pass through the details of all affected transaction which need to be processed.
 * <br>The TPM workflow is assumed to be {@value #TPM_DISPATCH_DEFINITION}
 * <br>
 * <br>
 *  <p><table border=0 style="width:15%;">
 *  <caption style="background-color:#0070d0;color:white"><b>ConstRepository</caption>
 *	<th><b>context</b></th>
 *	<th><b>subcontext</b></th>
 *	</tr><tbody>
 *	<tr>
 *	<td align="center">{@value #CONST_REPO_CONTEXT}</td>
 *	<td align="center">{@value #CONST_REPO_SUBCONTEXT}</td>
 *  </tbody></table></p>
 *	<p>
 *	<table border=2 bordercolor=black>
 *	<tbody>
 *	<tr>
 *	<th><b>Variable</b></th>
 *	<th><b>Default</b></th>
 *	<th><b>Description</b></th>
 *	</tr>
 *	<tr>
 *	<td><font color="blue"><b>{@value #TPM_DISPATCH}</b></font></td>
 *	<td>{@value #TPM_DISPATCH_DEFINITION}</td>
 *	<td>The TPM workflow to run for each transaction
 *	</td>
 *	</tr>
 *	<tr>
 *	<td><b>{@value #DISPATCH_STATUS}</b></td>
 *	<td>{@value #DISPATCH_STATUS_COLUMN} </td>
 *	<td><i>InfoField</i> to watch, as noted above</td>
 *	</tr>
 *	<tr>
 *	<td><b>{@value #DISPATCH_STATUS_VALUE}</b></td>
 *	<td>{@value #DISPATCH_TRIGGER_STATUS_VALUE}</td>
 *	<td>The value to check for a match. Can be a list of comma separated values.
 *  </td>
 *	</tr>
 *	<tr>
 *	<td><b>{@value #DISPATCH_PREV_STATUS_VALUE}</b></td>
 *	<td>{@value #DISPATCH_TRIGGER_PREV_STATUS_VALUE}</td>
 *	<td>The mandatory previous value of the info field to trigger </td> 
 *  </td>
 *	</tr>
 *  <td><b>{@value #DISPATCH_OPSVC_TABLE}</b>
 *  </td>
 *  <td>{@value #TRANSIENT_PROCESSING_COLUMN}
 *  </td>
 *  <td>used to pass data from Pre to Post processing
 *  </td>
 *	</tbody>
 *	</table>
 * </p>
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class OCDispatchStatus extends AbstractTradeProcessListener {


	private static final String CONST_REPO_CONTEXT = "Dispatch";
	private static final String CONST_REPO_SUBCONTEXT = "Status Trigger";
	private static final String DISPATCH_STATUS = "Status Column";
	private static final String DISPATCH_STATUS_COLUMN = "Dispatch Status";
	private static final String DISPATCH_STATUS_VALUE = "Status Value";
	private static final String DISPATCH_PREV_STATUS_VALUE = "Prev Status Value";
	private static final String DISPATCH_TRIGGER_STATUS_VALUE = "Awaiting Shipping,Left Site";
	private static final String DISPATCH_TRIGGER_PREV_STATUS_VALUE = "None";
	private static final String TPM_DISPATCH = "TPM Dispatch";
	private static final String TPM_DISPATCH_DEFINITION = "Dispatch";
	private static final String DISPATCH_OPSVC_TABLE =  "OpSvc Table";
	private static final String TRANSIENT_PROCESSING_COLUMN = "TPM_Trigger";

	private static final Map<String, String> configuration;
	static
	{
		configuration = new HashMap<String, String>();
		configuration.put(TPM_DISPATCH,TPM_DISPATCH_DEFINITION);
		configuration.put(DISPATCH_STATUS, DISPATCH_STATUS_COLUMN);
		configuration.put(DISPATCH_STATUS_VALUE, DISPATCH_TRIGGER_STATUS_VALUE);
		configuration.put(DISPATCH_PREV_STATUS_VALUE, DISPATCH_TRIGGER_PREV_STATUS_VALUE);
		configuration.put(DISPATCH_OPSVC_TABLE,TRANSIENT_PROCESSING_COLUMN);
		configuration.put("logLevel", null);
		configuration.put("logDir", null);
	}
	
	public Properties properties;
	private Table dispatchInfo;


	@Override
	public PreProcessResult preProcessInternalTarget(Context context,
			EnumTranStatusInternalProcessing targetStatus,
			PreProcessingInfo<EnumTranStatusInternalProcessing>[] infoArray, Table clientData) {
		//07 Dec - FPa advsied User will only be able to SaveTranInfo!
		return handleDispatchUpdate(context, infoArray, clientData);
	}

	/**
	 * 
	 * @param context
	 * @param transaction
	 * @return
	 */
	private int dispatchTPMSubmitterForTran(Context context, Transaction transaction, String newVal, String prevVal) {
		int submitter = 0;
		// Get all current instances of the TPM
		ProcessDefinition tpmWorkflow = context.getTpmFactory().getProcessDefinition(this.properties.getProperty(TPM_DISPATCH));
		Process[] processes = tpmWorkflow.getProcesses();
			
		for (Process process : processes) {
			// Get the tran number value from process variable
			Variable tranNum = process.getVariable("TranNum");
			if (tranNum.getValueAsInt() == transaction.getTransactionId()) {
				int taskCount = process.getTasks().length;
				if (taskCount > 0) {
					for (Task task : process.getTasks()) {
						if ("Left Site".equalsIgnoreCase(newVal) && "Awaiting Shipping".equalsIgnoreCase(prevVal)
								&& "Management Approval Group".equals(task.getAssignedGroup().getName())
								&& task.getName() != null && task.getName().indexOf("Approval") > -1) {
							submitter = -1;
							break;
						} else {
							submitter = Integer.parseInt(process.getVariable("Submitter").getValueAsString());
						}
					}
				} else {
					submitter = Integer.parseInt(process.getVariable("Submitter").getValueAsString());
				}
				break;
			}
		}
		
		return submitter;
	}

	private PreProcessResult commitInstanceData(Table clientData) {
		int column = /*clientData.getColumnCount()+*/1;
		try {
			if (clientData.getColumnId(properties.getProperty(DISPATCH_OPSVC_TABLE))<0) {
				column = clientData.addColumns(properties.getProperty(DISPATCH_OPSVC_TABLE), EnumColType.Table);
			}
		} catch (OpenRiskException oe) {
			// assume failed to find column!
			if (oe.getLocalizedMessage().contains(String.format("Invalid column name '%s'",properties.getProperty(DISPATCH_OPSVC_TABLE))))
				column = clientData.addColumns(properties.getProperty(DISPATCH_OPSVC_TABLE), EnumColType.Table);
			else 
				throw oe;
		}
		if (clientData.getRowCount()<1)
			clientData.addRows(1);
		clientData.setTable(column, 0, dispatchInfo);
		return PreProcessResult.succeeded();
	}

	private Table getInstanceData(Table clientData) {
		int column = /*clientData.getColumnCount()+*/-11;
		try {
			column = clientData.getColumnId(properties.getProperty(DISPATCH_OPSVC_TABLE))+1;
		} catch (OpenRiskException oe) {
			if (oe.getLocalizedMessage().contains(
					String.format("Invalid column name '%s'", properties.getProperty(DISPATCH_OPSVC_TABLE))))
				column = -1;
			else
				throw oe;
		}
		if (column < 0) {
			// throw new
			// OpenRiskException("Expected embedded table MISSING!!!");
			Logging.info(
					String.format("TPM Trigger Skipped(%d)",clientData.getRowCount()));
		}
		if (column < 1 ) {
			return null;
		}
		return clientData.getTable(properties.getProperty(DISPATCH_OPSVC_TABLE), 0);
	}


	private void updateInstanceData(int dealTrackingId, int transactionId, int activeVersion, String value) {
		int row = dispatchInfo.addRows(1);
		dispatchInfo.setInt("Deal", row, dealTrackingId);
		dispatchInfo.setInt("Tran", row, transactionId);
		dispatchInfo.setInt("Version", row, activeVersion);
		dispatchInfo.setString("Value", row, value);
	}

	/*
	 * History:
	 * 2017-05-02	V1.1 	jwaechter	- Added handling  of comma separated statuses
	 */
	private PreProcessResult handleDispatchUpdate(Context context,
			PreProcessingInfo<EnumTranStatusInternalProcessing>[] infoArray, Table clientData) {
		String message;
		try {
			this.properties = Repository.getConfiguration(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT, configuration);
			init();
			
			// create table of data to be passed from pre- to post-processing
			this.dispatchInfo = createInstanceData(context);
			
			for (PreProcessingInfo<?> currentDeal : infoArray) {
				Transaction deal = currentDeal.getTransaction();
				int activeVersion = deal.getVersionNumber();
				PluginLog.info(String.format("Starts - PreProcessing deal #%d, version #%d", deal.getDealTrackingId(), activeVersion));
				
				String value = deal.getField(properties.getProperty(DISPATCH_STATUS)).getDisplayString();
				String previousValue = getPreviousInstanceValue(context, deal);
				PluginLog.info(String.format("Field - Dispatch Status, Current Value - %s, Old Value - %s", value, previousValue));

				int submitterTPM = dispatchTPMSubmitterForTran(context, deal, value, previousValue);
				if (submitterTPM > 0) {
					String submitterName = context.getStaticDataFactory().getName(EnumReferenceTable.Personnel, submitterTPM);
					message = String.format("Can not save trade details, as TPM workflow - %s (started by user - %s) is already running or is in Assignment for trade #%d", this.properties.getProperty(TPM_DISPATCH), submitterName, deal.getDealTrackingId());
					PluginLog.info(message);
					return PreProcessResult.failed(message);
				}
				
				Set<String> dispatchStatusValuesLC = new TreeSet<>();
				Set<String> dispatchStatusValues = new TreeSet<>();
				
				for (String possValue : properties.getProperty(DISPATCH_STATUS_VALUE).split(",")) {
					dispatchStatusValuesLC.add(possValue.trim().toLowerCase());
					dispatchStatusValues.add(possValue.trim());
				}
				
				if (!value.equalsIgnoreCase(previousValue) 
						&& dispatchStatusValuesLC.contains(value.toLowerCase())
						&& previousValue.equalsIgnoreCase(properties.getProperty(DISPATCH_PREV_STATUS_VALUE))) {
					PluginLog.info("Current value & old value of Dispatch Status field matches criteria, saving data in ClientData table for post-process");
					
					for (String newValue : dispatchStatusValues) {
						if (value.equalsIgnoreCase(newValue)) {
							value = newValue;
						}
					}
				} else {
					PluginLog.info("Current value & old value of Dispatch Status field doesn't matches criteria to trigger TPM workflow");
					value = "N/A";
				}
				
				updateInstanceData(deal.getDealTrackingId(), deal.getTransactionId(), activeVersion, value);
				PluginLog.info(String.format("Ends - PreProcessing deal #%d, version #%d", deal.getDealTrackingId(), activeVersion));
			}
			
		} catch (Exception e) {
			message = String.format("Error processing pre-process method, Message-%s", e.getMessage());
			PluginLog.error(message);
			return PreProcessResult.failed(message);
		}
		
		return commitInstanceData(clientData.getTable("ClientData Table", 0));
	}

/**
 * 
 * 2015-12-16	V1.1	jwaechter	- added stamping of Payment date offset.
 * 2015-12-17	V1.2	jwaechter	- format changed.
 * 2015-12-17	V1.3	jwaechter	- switch to post processing.
 * 2015-12-18	V1.4	jwaechter	- now processing all legs instead of non physical
 */
	private void updatePymtDateOffset (Session session, Transaction deal) {
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMMyyyy");
		for (Leg leg : deal.getLegs()) {
			ResetDefinition rd = leg.getResetDefinition();
			if (rd != null) {
				Field field = leg.getResetDefinition().getField(EnumResetDefinitionFieldId.PaymentDateOffset);
				if (field != null && field.isApplicable() && field.isWritable()) {
					Date tradingDate = session.getTradingDate();
					String date = sdf.format(tradingDate).toString();
					field.setValue(date);
				}				
			}
		}
	}

	/**
	 * Retrieve value from db
	 * <br> return "" if no previous value otherwise value from most recent instance 
	 */
	private String getPreviousInstanceValue(Session session, Transaction deal) {

		Table infoData = DataAccess
				.getDataFromTable(
						session,
						String.format(
								"SELECT ativ.* "
										+ "\n FROM ab_tran_info_view ativ"
										+ "\n INNER JOIN ab_tran ab ON ativ.tran_num=ab.tran_num "
										+ "\n AND ab.current_flag=1 AND ab.deal_tracking_num=%d "
										+ "\n WHERE ativ.type_name='%s' ",
										deal.getDealTrackingId(),
										properties.getProperty(DISPATCH_STATUS)));
		String result="FAILED TO GET FROM DB";
		if (null == infoData || infoData.getRowCount() != 1) {
			result = "";
		} else {
			result = infoData.getString("value", 0);
		}
		return result;
	}

	private Table createInstanceData(Context context) {
		TableFactory tf = context.getTableFactory();
		Table dispatchInfo = tf.createTable("D425Dispatch");
		dispatchInfo.addColumn("Deal", EnumColType.Int);
		dispatchInfo.addColumn("Tran", EnumColType.Int);
		dispatchInfo.addColumn("Version", EnumColType.Int);
		dispatchInfo.addColumn("Value", EnumColType.String);
		return dispatchInfo;
	}


	@Override
	public void postProcessInternalTarget(Session session, DealInfo<EnumTranStatusInternalProcessing> deals,
			boolean succeeded, Table clientData) {
		
		try {
			Logging.init(session, this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			this.properties = Repository.getConfiguration(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT, configuration);
			init();
			
			for (int tranId : deals.getTransactionIds()) {
				Transaction deal = session.getTradingFactory().retrieveTransactionById(tranId);
				String currLogFile = PluginLog.getLogFile().substring(0, PluginLog.getLogFile().lastIndexOf("."));
				if (!this.getClass().getSimpleName().equals(currLogFile)) {
					init();
				}
				
				PluginLog.info(String.format("Starts - PostProcessing tranNum #%d, version #%d", tranId, deal.getVersionNumber()));

				updatePymtDateOffset (session, deal);
				PluginLog.info(String.format("PaymentDateOffset updated for tranNum #%d, version #%d", tranId, deal.getVersionNumber()));
				int versionNumber = deal.getVersionNumber();
				
				deal.saveIncremental();
				currLogFile = PluginLog.getLogFile().substring(0, PluginLog.getLogFile().lastIndexOf("."));
				if (!this.getClass().getSimpleName().equals(currLogFile)) {
					init();
				}
				
				PluginLog.info(String.format("Incremental save done, new version #%d", deal.getVersionNumber()));
				
				Table dispatchData = getInstanceData(clientData.getTable("ClientData Table", 0));
				if (null == dispatchData) {
					PluginLog.info("No dispatchData saved in pre-process, existing");
					return;
				}
				
				int match = -1;
				if ((match = dispatchData.find(0,deal.getDealTrackingId(), 0))<0) {
					PluginLog.info(String.format("No dispatchData found for tran #%d, moving to next transaction", tranId));
					dispatchData.clear();
					continue;
				}
				
				if (versionNumber >=  dispatchData.getInt("Version", match) && deal.getTransactionId() == dispatchData.getInt("Tran",match)) {
					String thisValue = deal.getField(properties.getProperty(DISPATCH_STATUS)).getDisplayString();
					
					if (thisValue.equalsIgnoreCase(dispatchData.getString("Value", match))) {
						PluginLog.info(String.format("MATCHED TPM Trigger for Tran#%d", tranId));
						ProcessDefinition tpmWorkflow;
						
						try {
							PluginLog.info(String.format("Run TPM! %s", properties.getProperty(TPM_DISPATCH)));
							tpmWorkflow = session.getTpmFactory().getProcessDefinition(properties.getProperty(TPM_DISPATCH));

						} catch (OpenRiskException olf) {
							PluginLog.info(String.format("Tran#%d, Dispatch workflow failure: %s", tranId, olf.getMessage()));
							throw olf;
						}

						Variable tranNum = session.getTpmFactory().createVariable("TranNum", EnumFieldType.Int, String.valueOf(deal.getTransactionId()));
						Variables vars = session.getTpmFactory().createVariables();
						vars.add(tranNum);
						Process process = tpmWorkflow.start(vars);
						PluginLog.info(String.format("%s(%d) STARTED", process.getState(), process.getId()));

					} else {
						PluginLog.info(String.format("MIS-MATCH skip TPM Trigger for Tran#%d, TranVersion(in postprocess)-%d, TranVersion(in preprocess)-%d", deal.getTransactionId(), versionNumber, dispatchData.getInt("Version", match)));
					}
				}
				
				dispatchData.clear();
				PluginLog.info(String.format("Ends - PostProcessing tranNum #%d, version #%d", tranId, deal.getVersionNumber()));
			}
			
		} catch (Exception e) {
			PluginLog.error("Error processing post process method. " + e.getMessage());
		}
		finally {
			Logging.close();
		}
	}
	
	/**
	 * Initialise logging 
	 * @throws Exception 
	 * 
	 * @throws OException
	 */
	private void init() throws Exception {
		String logLevel = "INFO";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = this.properties.getProperty("logLevel", logLevel);
			logDir = this.properties.getProperty("logDir", logDir);

			if (logDir == null) {
				PluginLog.init(logLevel);
			} else {
				PluginLog.init(logLevel, logDir, logFile);
			}
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}
	}
}
