package com.matthey.openlink.bo.opsvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.matthey.openlink.utilities.Notification;
import com.matthey.openlink.utilities.Repository;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.backoffice.SettlementInstruction;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;
import com.olf.openrisk.trading.EnumBuySell;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;


/**
 * <p>D422 (4.1.5) PreProcess Dispatch collateral status
 * </p>Obtain collateral balance 
 * <br>If balance > zero process Order to New 
 * <br>Called by OpSvc {@link ValidateCollateralBalance.evaluateCollateral}
 * <br>Call from TPM {@link com.matthey.openlink.utilites.tpm.RetrieveCollateral}
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
 *	<td><font color="blue"><b>{@value #COLLATERAL_ACCOUNT}</b></font></td>
 *	<td>{@value #COLLATERAL_ACCOUNT_TYPE}</td>
 *	<td>The account class name, which identifies a collateral account
 *	</td>
 *	</tr>
 *	<tr>
 *	<td><b>{@value #METAL_ACCOUNT}</b></td>
 *	<td>{@value #METAL_ACCOUNT_TYPE} </td>
 *	<td>the account class for a metal account</td>
 *	</tr>
 *	<tr>
 *	<td><b>{@value #SETTLEMENT_INSTRUCTIONS}</b></td>
 *	<td>{@value #COLLATERAL_SETTLEMENT_INFO}</td>
 *	<td>The <i>ParamInfo</i> which holds the SI account for the given leg
 *  </td>
 *	</tr>
 *  <td><b>{@value #DISPATCH_TRAN_STATUS}</b>
 *  </td>
 *  <td>{@value #DISPATCH_TRAN_STATUS_VALUE}
 *  </td>
 *  <td>which is the effective transaction status value
 *  </td>
 *  <tr>
 *  <td><b>{@value #DISPATCH_OPSVC_TABLE}</b>
 *  </td>
 *  <td>{@value #OPSVC_TABLE}
 *  </td>
 *  <td>is the name of the table column to hold the custom table
 *  </td>
 *	</tbody>
 *	</table>
 * </p>
 *
 * @version $Revision: $
 * 
 */
@ScriptCategory({ EnumScriptCategory.TradeInput })
public class ValidateCollateralBalance extends AbstractTradeProcessListener {

	private static final String CONST_REPO_CONTEXT = "Dispatch";
	private static final String CONST_REPO_SUBCONTEXT = "Collateral";
	
	private static final String CLIENT_DATA_TABLE_NAME = "ClientData Table";
	private static final String CLIENTDATA_SUCCEEDED = "succeeded";
	private static final String CLIENTDATA_STATUS = "status";
	private static final String CLIENTDATA_TRAN_NUM = "tran_num";
	
	public static final String SETTLEMENT_INSTRUCTIONS = "Settlement Instructions";
	private static final String COLLATERAL_SETTLEMENT_INFO = "SI-Phys";
	private static final String COLLATERAL_ACCOUNT = "Collateral";
	private static final String COLLATERAL_ACCOUNT_TYPE = "Collateral Account";
	private static final String METAL_ACCOUNT = "Metal";
	private static final String METAL_ACCOUNT_TYPE = "Metal Account";
	private static final String DISPATCH_TRAN_STATUS = "Status";
	private static final String DISPATCH_TRAN_STATUS_VALUE = "Order";
	private static final String DISPATCH_OPSVC_TABLE =  "OpSvc Table";
	private static final String OPSVC_TABLE = "Dispatch_Collateral";
	
		
	private static final Map<String, String> configuration;
	    static
	    {
	    	configuration = new HashMap<String, String>();
	    	configuration.put(COLLATERAL_ACCOUNT,COLLATERAL_ACCOUNT_TYPE);
	    	configuration.put(METAL_ACCOUNT, METAL_ACCOUNT_TYPE);
	    	configuration.put(SETTLEMENT_INSTRUCTIONS, COLLATERAL_SETTLEMENT_INFO);
	    	configuration.put(DISPATCH_TRAN_STATUS, DISPATCH_TRAN_STATUS_VALUE);
	    	configuration.put(DISPATCH_OPSVC_TABLE, OPSVC_TABLE);
	    }
		public static Properties properties;
	
	/**
	 * refresh runtime values based on active configuration data
	 */
	private static void populateRuntimeValues() {
		ValidateCollateralBalance.properties = Repository.getConfiguration(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT, configuration);
	}
		
	@Override
	public PreProcessResult preProcess(Context context, EnumTranStatus targetStatus,
			PreProcessingInfo<EnumTranStatus>[] infoArray, Table clientData) {

		Logging.init(context, this.getClass(), "", "");
		populateRuntimeValues();
		
		// create table of data to be passed from pre- to pots-processing
        Table collateralData = createInstanceData(context);
		
		Boolean collateralResult = false;
		for (PreProcessingInfo<?> activeItem : infoArray) {

			Transaction transaction = null;
			EnumTranStatus collateralStatus=EnumTranStatus.New;
			try {
				transaction = activeItem.getTransaction();
				if (EnumBuySell.Buy.toString() == transaction.getField(EnumTransactionFieldId.BuySell).getValueAsString())
					continue;
				
				if (!(collateralResult = evaluateCollateral(context, transaction, targetStatus))) {
					collateralStatus = EnumTranStatus.Deleted;  // assume deal will be failed
					// Enable user to continue processing despite collateral failure 
					if ("Yes".equalsIgnoreCase(com.matthey.openlink.utilities.legacy.Ask
							.yesOrNo("This deal has not passed the collateral check. Do you want to process the deal to New status?")))
						collateralStatus = EnumTranStatus.New;
					/*FPa request suppression 02-Dec-2015 
					 * else
						com.matthey.openlink.utilities.legacy.Ask
								.ok("Please remove batches and crates from Dispatch deal and process deal to the Deleted status");*/
				}
				int row = collateralData.addRows(1);
				collateralData.setInt(CLIENTDATA_TRAN_NUM, row, transaction.getTransactionId());
				collateralData.setInt(CLIENTDATA_STATUS, row, collateralStatus.getValue());
				collateralData.setString(CLIENTDATA_SUCCEEDED, row, String.valueOf(collateralResult));

				
			} catch (DispatchCollateralException dispatch) {
				String reason = String.format("PreProcess>Tran#%d FAILED %s CAUSE:%s",
						null != transaction ? transaction.getTransactionId()
								: ValidateDispatchInstructions.DISPATCH_BOOKING_ERROR, this.getClass()
								.getSimpleName(), dispatch.getLocalizedMessage());
				Logging.error(reason, dispatch);
				return PreProcessResult.failed(dispatch.getLocalizedMessage());
				
			} catch (Exception e) {
				String reason = String.format("PreProcess>Tran#%d FAILED %s CAUSE:%s",
						null != transaction ? transaction.getTransactionId()
								: ValidateDispatchInstructions.DISPATCH_BOOKING_ERROR, 
								this.getClass().getSimpleName(), 
								e.getLocalizedMessage());
				Logging.error(reason, e);
				e.printStackTrace();
				return PreProcessResult.failed(reason);

			}
		}
		Logging.close();
		return commitInstanceData(clientData.getTable(CLIENT_DATA_TABLE_NAME,0), collateralData);

	}
	
	private Table createInstanceData(Context context) {
		TableFactory tf = context.getTableFactory();
        Table dispatchInfo = tf.createTable("DISPATCH COLLATERAL");
        dispatchInfo.addColumn(CLIENTDATA_TRAN_NUM, EnumColType.Int);
        dispatchInfo.addColumn(CLIENTDATA_STATUS, EnumColType.Int);
        dispatchInfo.addColumn(CLIENTDATA_SUCCEEDED, EnumColType.String);
		return dispatchInfo;
	}
	
	private Table getInstanceData(Table clientData) {
		int column = -1;
		try {
			column = clientData.getColumnId(properties.getProperty(DISPATCH_OPSVC_TABLE)) + 1;
		} catch (OpenRiskException oe) {
			if (oe.getLocalizedMessage().contains(
					String.format("Invalid column name '%s'", properties.getProperty(DISPATCH_OPSVC_TABLE))))
				column = -1;
			else
				throw oe;
		}
		if (column < 0) {
			Logging.info(
					String.format("%s Skipped(%d)",properties.getProperty(DISPATCH_OPSVC_TABLE), clientData.getRowCount()));
		}
		if (column < 1 ) {
			return null;
		}
		return clientData.getTable(properties.getProperty(DISPATCH_OPSVC_TABLE), 0);
	}
	
	
	private PreProcessResult commitInstanceData(Table clientData, Table dispatchInfo) {
		int column = 1;
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
	
	/**
	 * The dispatch processing assumes a single dispatch transaction for each cycle
	 * This means once the pre-processing has happened only a single entry should be present in the @param clientData
	 * If this is not true we will fail processing!
	 */
	@Override
	public void postProcess(Session session, DealInfo<EnumTranStatus> deals, boolean succeeded, Table clientData) {

		Logging.init(session, this.getClass(), "", "");
		populateRuntimeValues();

		boolean result=false;
			
			for(PostProcessingInfo<EnumTranStatus> deal: deals.getPostProcessingInfo()) {
				
				if (EnumTranStatus.Proposed != deal.getTargetStatus()) {
				Logging.info(
							String.format("Skipping#%d as target status[%s]", deal.getDealTrackingId(), deal.getTargetStatus().toString()));
					continue;
				}
			
			Transaction transaction = null;
			try {
				if (clientData.getRowCount()>0) {
					int collateralId;
//					if ((collateralId =clientData.getColumnId(CLIENT_DATA_TABLE_NAME))>0) {
						Table collateralUpdate = getInstanceData(clientData.getTable(CLIENT_DATA_TABLE_NAME,0));
						if (null == collateralUpdate )
							return;
						
						if (collateralUpdate.getRowCount()>1) {
							String reason = String.format("Unexpected multiple entries in %s",properties.getProperty(DISPATCH_OPSVC_TABLE));
						Logging.info(
									reason);
							Notification.raiseAlert("PostProcessing Dataset", DispatchCollateral.ERR_COLLATERALPOST, reason);
							return;
						}
						//session.getDebug().viewTable(collateralUpdate);
						int transactionNumber = collateralUpdate.getInt(CLIENTDATA_TRAN_NUM, 0);
						if (transactionNumber == deal.getTransactionId() 
								|| 0 == transactionNumber) {
							//deals
							transaction = session.getTradingFactory().retrieveTransactionById(deal.getTransactionId());
							EnumTranStatus tran_status = EnumTranStatus.retrieve(collateralUpdate.getInt(CLIENTDATA_STATUS, 0));
							transaction.process(tran_status);
						}
//					}
				}


			} catch (DispatchCollateralException dispatch) {
				String reason = String.format("PreProcess>Tran#%d FAILED %s CAUSE:%s",
						null != transaction ? transaction.getTransactionId()
								: ValidateDispatchInstructions.DISPATCH_BOOKING_ERROR, this.getClass()
								.getSimpleName(), dispatch.getLocalizedMessage());
				Logging.error(reason, dispatch);
				return;
				
						
			} catch (Exception e) {
				String reason = String.format("PreProcess>Tran#%d FAILED %s CAUSE:%s",
						null != transaction ? transaction.getTransactionId()
								: ValidateDispatchInstructions.DISPATCH_BOOKING_ERROR, this.getClass()
								.getSimpleName(), e.getLocalizedMessage());
				Logging.error(reason, e);
				e.printStackTrace();
				return;

			}
		}
		Logging.close();
		return;
	}



	
	
	
	/**
	 * Determine if current Dispatch deal (transaction) collateral balance is
	 * acceptable <br>
	 * This is based on the agreed formula, see implementation for details of
	 * this calculation If the result is a positive value greater than zero then
	 * indicate success and continues evaluation of other deals within the
	 * processing session. 
	 * <br>return <b>true</b> if value is > 0, otherwise <b>false</b>
	 */
	private boolean evaluateCollateral(Session session, Transaction transaction,
			EnumTranStatus targetStatus) {
		double collateral = calculateCollateral(session, transaction, targetStatus);

		Logging.info(
				String.format("Tran# %d processed for status %s has collateral of %f",
						transaction.getTransactionId(), targetStatus.toString(), collateral));
		if (collateral > DispatchCollateral.ZERO) {
			return true;
		}
		return false;
	}

	static public double calculateCollateral(Session session, Transaction transaction, EnumTranStatus targetStatus) {
		double collateral = Double.NaN;
		Logging.info(
				String.format("Tran# %d has status>%s<",
						transaction.getTransactionId(), targetStatus.getName()));
		
		populateRuntimeValues();

		try {
			//FIX - This is redundant since ParamInfo drive processing - get Info SI
			SettlementInstruction[] sis = session.getBackOfficeFactory().retrieveSettlementInstructions();
			List<SettlementInstruction> si = Arrays.asList(sis);
			List<SettlementInstruction> activeSettlementInstructions = new ArrayList<>(0);
			
			for (Leg currentLeg :transaction.getLegs()) {
				Field SIField = currentLeg.getField(properties.getProperty(SETTLEMENT_INSTRUCTIONS));
				if (null == SIField 
						|| SIField.getDisplayString().trim().length()<1
						|| SIField.getValueAsInt()<1) {
					Logging.info(
							String.format("Tran#%d has missing/invalid SI on Leg#%d", 
								transaction.getTransactionId(),
								currentLeg.getLegNumber()));
					continue;
				}
				SettlementInstruction settlementInstruction = session.getBackOfficeFactory().retrieveSettlementInstruction(SIField.getValueAsInt());
				if (si.contains(settlementInstruction))
					activeSettlementInstructions.add(settlementInstruction);
			}

				collateral = DispatchCollateral.evaluate(
						session,
						transaction,
						activeSettlementInstructions);
		
		} catch (DispatchCollateralException dispatch) {
			if (DispatchCollateral.ERR_NOCOLLATERAL != dispatch.getId())
				throw dispatch;

			Logging.error(dispatch.getLocalizedMessage(), dispatch);
			collateral = DispatchCollateral.ZERO;
			
					
		}
		return collateral;
	}

}
