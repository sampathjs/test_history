package com.matthey.openlink;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.text.SimpleDateFormat;

import com.matthey.openlink.utilities.DataAccess;
import com.matthey.openlink.utilities.Repository;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.embedded.trading.TradeProcessListener.PostProcessingInfo;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.staticdata.EnumFieldType;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.ProcessDefinition;
import com.olf.openrisk.tpm.Variable;
import com.olf.openrisk.tpm.Variables;
import com.olf.openrisk.trading.EnumResetDefinitionFieldId;
import com.olf.openrisk.trading.EnumTranStatusInternalProcessing;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.ResetDefinition;
import com.olf.openrisk.trading.Transaction;
import com.openlink.endur.utilities.logger.LogCategory;
import com.openlink.endur.utilities.logger.LogLevel;
import com.openlink.endur.utilities.logger.Logger;


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
			Logger.log(LogLevel.INFO, LogCategory.CargoScheduling, this, 
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
		this.properties = Repository.getConfiguration(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT, configuration);
		// create table of data to be passed from pre- to post-processing
		this.dispatchInfo = createInstanceData(context);
		//TEST  DiscoverPrinters.main(null);
		for (PreProcessingInfo<?> currentDeal : infoArray) {
			Transaction deal = currentDeal.getTransaction();
			int activeVersion = deal.getVersionNumber();

			String value = deal.getField(properties.getProperty(DISPATCH_STATUS)).getDisplayString();
			String previousValue = getPreviousInstanceValue(context, deal);
			Set<String> dispatchStatusValuesLC = new TreeSet<>();
			Set<String> dispatchStatusValues = new TreeSet<>();
			for (String possValue : properties.getProperty(DISPATCH_STATUS_VALUE).split(",")) {
				dispatchStatusValuesLC.add(possValue.trim().toLowerCase());
				dispatchStatusValues.add(possValue.trim());
			}
			if (!value.equalsIgnoreCase(previousValue) 
					&& dispatchStatusValuesLC.contains(value.toLowerCase())
					&& previousValue.equalsIgnoreCase(properties.getProperty(DISPATCH_PREV_STATUS_VALUE))) {
				for (String newValue : dispatchStatusValues) {
					if (value.equalsIgnoreCase(newValue)) {
						value = newValue;
					}
				}
			}
			else
				value="N/A";
			updateInstanceData(deal.getDealTrackingId(), deal.getTransactionId(), activeVersion, value);
		}

		return commitInstanceData(clientData.getTable("ClientData Table",0));
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
										+ "\n AND current_flag=1 AND ab.deal_tracking_num=%d "
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
		for (int tranId : deals.getTransactionIds()) {
			Transaction deal = session.getTradingFactory().retrieveTransactionById(tranId);
			updatePymtDateOffset (session, deal);
			int versionNumber = deal.getVersionNumber();
			deal.saveIncremental();
			//		}		
			//	}


			//	@Override
			//	public void postProcess(Session session, PostProcessingInfo<EnumTranStatus>[] infoArray, boolean succeeded, Table clientData) {
			this.properties = Repository.getConfiguration(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT, configuration);
			System.out.println("Starting... " +this.getClass().getSimpleName() );
			Table dispatchData = getInstanceData(clientData.getTable("ClientData Table",0));
			if (null ==dispatchData) {
				System.out.println("NOTHING to do... " );
				return;
			}
			int match=-1;
			System.out.println(String.format("Checking... Tran#%d" ,deal.getTransactionId()) );
			if ((match = dispatchData.find(0,deal.getDealTrackingId(), 0))<0)
				continue;
			// what to do...
			System.out.println(String.format("Checking client data... Tran#%d" ,deal.getTransactionId()) );
			if (versionNumber >=  dispatchData.getInt("Version", match) && deal.getTransactionId() == dispatchData.getInt("Tran",match)){
				String thisValue = deal.getField(properties.getProperty(DISPATCH_STATUS)).getDisplayString();
				if (thisValue.equalsIgnoreCase(dispatchData.getString("Value", match))) {
					Logger.log(LogLevel.DEBUG, LogCategory.CargoScheduling, this, 
							String.format("MATCHED TPM Trigger for Tran#%d", deal.getTransactionId()));

					ProcessDefinition tpmWorkflow;
					try {
						System.out.println("Run TPM!" + properties.getProperty(TPM_DISPATCH));
						tpmWorkflow = session.getTpmFactory().getProcessDefinition(properties.getProperty(TPM_DISPATCH));

					} catch (OpenRiskException olf) {
						Logger.log(LogLevel.FATAL, LogCategory.CargoScheduling, this, 
								String.format("Dispatch workflow failure: %s", olf.getMessage()),
								olf);
						throw olf;
					}

					Variable tranNum = session.getTpmFactory().createVariable("TranNum", EnumFieldType.Int, String.valueOf(deal.getTransactionId()));
					Variables vars = session.getTpmFactory().createVariables();
					vars.add(tranNum);
					Process process = tpmWorkflow.start(vars);
					Logger.log(LogLevel.INFO, LogCategory.CargoScheduling, this,
							String.format("%s(%d) STARTED",process.getState(), process.getId()));

				} else
					Logger.log(LogLevel.DEBUG, LogCategory.CargoScheduling, this, 
							String.format("MIS-MATCH skip TPM Trigger for Tran#%d", deal.getTransactionId()));
			}
			dispatchData.clear();
		}
		//System.out.println("ALL DONE! clear working data");
	}	
}
