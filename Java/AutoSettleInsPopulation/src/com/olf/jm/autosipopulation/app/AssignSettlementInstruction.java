package com.olf.jm.autosipopulation.app;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.autosipopulation.model.AccountInfoField;
import com.olf.jm.autosipopulation.model.DecisionData;
import com.olf.jm.autosipopulation.model.EnumClientDataCol;
import com.olf.jm.autosipopulation.model.EnumRunMode;
import com.olf.jm.autosipopulation.model.Pair;
import com.olf.jm.autosipopulation.model.ParamInfoField;
import com.olf.jm.autosipopulation.model.SavedUnsaved;
import com.olf.jm.autosipopulation.model.SettleInsAndAcctData;
import com.olf.jm.autosipopulation.model.StlInsInfo;
import com.olf.jm.autosipopulation.model.TranInfoField;
import com.olf.jm.autosipopulation.persistence.DBHelper;
import com.olf.jm.autosipopulation.persistence.Logic;
import com.olf.jm.autosipopulation.persistence.LogicResult;
import com.olf.jm.autosipopulation.persistence.LogicResultApplicator;
import com.olf.jm.autosipopulation.persistence.LogicResultApplicatorComplexCommPhys;
import com.olf.jm.autosipopulation.persistence.LogicResultApplicatorComplexCommPhysException;
import com.olf.jm.autosipopulation.persistence.LogicResultApplicatorException;
import com.olf.jm.autosipopulation.persistence.LogicResultApplicatorTranInfoField;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.backoffice.SettlementInstruction;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.EnumNomfField;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.staticdata.Currency;
import com.olf.openrisk.staticdata.DeliveryType;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.staticdata.Fields;
import com.olf.openrisk.staticdata.Party;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.DealEvent;
import com.olf.openrisk.trading.DealEvents;
import com.olf.openrisk.trading.EnumInsSub;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTranStatusInternalProcessing;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History: 
 * 2015-04-28	V1.0	jwaechter 	- initial version	
 * 2015-08-24	V1.1	jwaechter	- added calculation of isCommPhys
 * 2015-08-25	V1.2	jwaechter	- added precious metal distinction for form and loco
 * 									- added retrieval of precious metal list from currency table
 * 2015-09-03	V1.3	jwaechter	- added precious metal distinction for form phys and allocation type
 * 									- modified logic in isRelevantForPostProcess to check for 3 legs 
 *                                    instead of 2 (as there is always the "zero" leg)
 *                                  - modified retrieval logic for "Form-Phys" to get it always from leg 1 in case of 
 *                                    a two legged COMM-PHYS
 * 2015-09-15	V1.4	jwaechter	- removed SI info "For Passthrough" logic
 * 2015-09-21	V1.5	jwaechter	- now retrieving "Form-Phys" from leg zero in case there is no other leg.
 * 2015-10-06	V1.6	jwaechter	- added constant for AlertBroker message
 * 2015-11-27	V1.7	jwaechter	- added extra logic for new COMM-PHYS sell deals
 * 2016-01-05	V1.8	jwaechter	- added extra logic for COMM-PHYS deals to skip physical legs 
 *                                    (just fee legs are processed for non metal currencies)
 * 2016-01-29	V1.9	jwaechter	- added new info fields to exclude deals and accounts from processing
 *                                  - performance enhancements
 * 2016-02-03	V1.10	jwaechter	- added exclusion of dispatch deal if being saved incrementally
 * 2016-02-23	V1.11	jwaechter	- added code to take over siPhys for COMM-PHYS deals
 * 2016-03-16	V1.12	jwaechter	- added retrieval of TRAN level info field "SI Phys" analogous to
 *                                    existing leg level retrieval 
 * 2016-03-30	V1.13	jwaechter	- now processing target status Amended as well.
 * 2016-05-27	V1.14	jwaechter	- removed tran_group based selection of clientData subtable
 * 2016-06-01	V1.15	jwaechter	- added logic to differentiate between offset tran types
 * 2016-06-14	V1.16	jwaechter	- limited flip of direction to offset tran type "Generated Offset" only
 * 2018-01-08   V1.17   scurran     - add support for offset type Pass Thru Offset
 * 2017-11-14   V1.18   sma         - reverse internal SI and external SI for "Generated Offset" deal
 */

/**
 * This plugin implements the Auto SI population as described in item D616. 
 * The purpose of this plugin is to set the settlement instructions using the
 * specialized logics for ins sub type "Cash Tran" non "Cash Trans". 
 * 
 * 
 * The plugin applies the logic using the following process:
 * <ol>
 *   <li> 
 *      Initialize PluginLog from ConstantsRepository and initialize the GUI to use windows look and feel.
 *   </li>
 *   <li> 
 *      Retrieve "static" data about instruments, their assigned settlement instructions and their accounts
 *   </li>
 *   <li>
 *      For each transaction retrieve the necessary transaction data about tran level settlement instructions 
 *      including parameter level tran info fields in case of "complex" COMM-PHYS Multi Leg deals.
 *   </li>
 *   <li>
 *      Apply the logic that is a mapping between the static data retrieved in step 2 and the transaction data 
 *      retrieved in step 3.
 *   </li>
 *   <li>
 *     	Depending on tran status, instrument type and constants repository configuration the output is applied differenty.
 *      The logic result can be written either / and to one of the following areas: <br/>
 *      Tran info fields on tran level for non COMM-PHYS deals and on leg level for COMM-PHYS deals. <br/>
 *      Settlement instructions on event level for non COMM-PHYS deals. <br/>
 *      Settlement instructions on event level for COMM-PHYS deals. <br/>
 *      Instead of asking the user in case of multiple SIs, exception can be thrown. <br/>      
 *   </li>
 *   <li>
 *      For offset deals of type "Generated Offset" the existing results of the original deal are reused but
 *      the "direction" (internal / external) is flipped.
 *   </li>
 * </ol>
 * <br/>
 * Note that this module follows an object oriented approach. The input data is saved in instances of
 * {@link SettleInsAndAcctData} ("static" data about settlement instructions and accounts) and 
 * {@link DecisionData} (data retrieved from the transaction being currently processed).
 * The logic itself resides within class {@link Logic}.
 * The results of the application of the logic are stored in instances of {@link LogicResult} that are being
 * processed within the class {@link LogicResultApplicator}
 * <br/> <br/>
 * Prerequisites: the following info fields have to be setup and filled with values:
 * <ol>
 *   <li>
 *     Account Info Field "Loco"
 *   </li>
 *   <li>
 *     Account Info Field "Form"
 *   </li>
 *   <li>
 *     Account Info Field "Allocation Type"
 *   </li>
 *   <li>
 *     Tran Info Field "Form Phys" (on param level) for COMM-PHYS deals
 *   </li>
 *   <li>
 *     Tran Info Field "Loco" - tran level for non COMM-PHYS deals
 *   </li>
 *   <li>
 *     Tran Info Field "Form" - tran level
 *   </li>
 *   <li>
 *     Tran Info Field "Allocation Type" - tran level
 *   </li>
 *   <li>
 *     Tran Info Field "SI-Phys" - param level for COMM-PHYS deals
 *   </li>
 *   <li>
 *     Tran Info Field "SI-Phys-Tran" - tran level for non COMM-PHYS deals
 *   </li>
 *   <li>
 *     Tran Info Field "SI-Phys Internal" - param level for COMM-PHYS deals
 *   </li>
 *   <li>
 *     Tran Info Field "SI-Phys Internal-Tran" - tran level for non COMM-PHYS deals
 *   </li>
 *   <li>
 *     Tran Info Field "Auto SI Shortlist" - tran level
 *   </li>
 *   <li>
 *     Tran Info Field "Auto SI Check" - tran level
 *   </li>
 * </ol>
 * <br/>
 * Relevant constants are found in the following classes:
 * <ol>
 *   <li> Account Info fields: {@link AccountInfoField} </li>
 *   <li> Transaction Info fields {@link ParamInfoField} and {@link TranInfoField} </li>
 *   <li> Settlement Info Fields {@link StlInsInfo} </li>
 *   <li> Constants Repository: {@link AssignSettlementInstruction} </li>
 *   <li> Alert Broker: {@link AssignSettlementInstruction} </li>
 * </ol>
 * <br/>
 * This plugin has to be used as an OPS trading pre/post process check including internal target.
 * <br/>
 * Special notes about the different classes that are applying the output: <br/>
 * The "normal" process flow of the plugin should be that it's  being executed on a simple deal 
 * (not a complex COMM-PHYS deal) that is processed to any status. In this case the plugin retrieves
 * the "static" data about the settlement instruction configuration followed by retrieval of the transaction related data.
 * Based on the logic result the right settlement instructions are written to the transaction level settlement instructions. 
 * In this case Endurs core is expected to push the settlement instructions from tran level to event level if the transaction
 * is processed to validated or another transaction state generating events. The class applying this logic: {@link LogicResultApplicator}<br/>
 * In case the deal being processed is a COMM-PHYS deal having events afterwards it is not possible to write the settlement instructions 
 * on transaction level as the custom logic demands that the event level settlement instructions are depending
 * on parameter level info fields. As events are not present in pre process mode and no GUI is present in post process mode,
 * the plugin has to ask the user in pre process mode and saves them to the clieantData table that is shared with post process.
 * During execution of the post process method, the content of the clientData table is read and the settlement instructions are
 * written on event level. The class applying this logic: {@link LogicResultApplicatorComplexCommPhys} <br/>
 * In case the deal being processed is a COMM-PHYS deal and the status is not having events afterwards to avoid asking the users twice
 * the settlement instructions are saved as IDs in param level info fields that are later on picked up by the next run of AutoSi again.
 * To keep a consistency in the general processing there are also tran info fields on transaction level that follow the same principle.
 * Both the transaction level and the param level info fields containing settlement instructions also allow other plugins to easily 
 * bypass the Auto SI functionality and to preenter the settlement instruction to be used. 
 * The class saving settlement instructions to the tran info fields: {@link LogicResultApplicatorTranInfoField} <br/>
 * If deals are being processed or booked automatically there might not be access to the GUI or it might not
 * be expected to have multiple settement instructions to choose from for certain deal types. In this case the constants repository
 * variable "{@value #CONST_REPO_CONTEXT}\ {@value #CONST_REPO_SUBCONTEXT}\ useExceptionInsteadOfDialog"
 * can be set to true which will result in exceptions being thrown in cases the user would normally have to enter data.
 * The classes applying those logic are either {@link LogicResultApplicatorException} (replacing @link LogicResultApplicator}) 
 * or {@link LogicResultApplicatorComplexCommPhysException} (replacing 
 * {@link LogicResultApplicatorComplexCommPhys}) depending on the deal type being processed. <br/> <br/>
 * Please be aware there is a complex logic deciding for which tran status / instrument type / deal the 
 * settlement instruction logic is executed as the criteria defined in the OPS definition are not sufficient.
 * Due to historical growth the logic deciding whether to run or not is distributed among the preProcess, postProcess, 
 * preProcessInternalTarget, postProcessInternalTarget and the process methods.
 * @author jwaechter
 * @version 1.17
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class AssignSettlementInstruction extends AbstractTradeProcessListener {
	public static final String CONST_REPO_CONTEXT = "FrontOffice"; // context of constants repository
	public static final String CONST_REPO_SUBCONTEXT = "Auto SI Population"; // sub context of constants repository

	/**
	 * Alert Broker Message ID for message indicating we don't have a GUI and
	 * can't show either the confirmation dialog or the selection dialog 
	 * to the user.
	 */
	public static final String ALERT_BROKER_NO_GUI_MSG_ID = "ASI-NO-GUI"; 

	//private static Set<SettleInsAndAcctData> settleInsAndAccountData=null;
	private Set<SettleInsAndAcctData> settleInsAndAccountData = null;
	/**
 	 * If set to true it will retrieve static data only once per script engine.
	 */
	//private boolean useCache = false;

    /**
     * if set to true it will use result applicators throwing exceptions instead of  questioning the user
     */	
	private boolean useException = false;  
	                                      
	/**
	 * Contains 
	 */
	private List<Integer> preciousMetalList;
	
	private List<LogicResultApplicator> logicResultApplicators=null;

	@Override
	public PreProcessResult preProcess(final Context context, final EnumTranStatus targetStatus,
			final PreProcessingInfo<EnumTranStatus>[] infoArray, final Table clientData) {
		boolean succeed=true;
		boolean runPostProcess=false;
		EnumRunMode rmode = EnumRunMode.PRE;
		init (context);	
		try {
			PluginLog.info(this.getClass().getName() + " started in pre process run\n"); 
			//gatherSettleInsAndAcctData (context); // gather static data
			preciousMetalList = DBHelper.retrievePreciousMetalList (context);
			
			for (PreProcessingInfo<EnumTranStatus> ppi : infoArray) {
				logicResultApplicators = new ArrayList<>();
				Transaction tran = ppi.getTransaction();
				Transaction offset = ppi.getOffsetTransaction();
				
				int insType = tran.getInstrumentTypeObject().getId();
				Set<Integer> partyIds = new HashSet<>();
				addPartyIdsForTran(tran, partyIds);
				addPartyIdsForTran(offset, partyIds);
				
				gatherSettleInsAndAcctData (context, partyIds, insType);
				
				if (ppi.getTargetStatus() == EnumTranStatus.New || ppi.getTargetStatus() == EnumTranStatus.Proposed) {
					if (tran.getInstrumentTypeObject().getInstrumentTypeEnum() == EnumInsType.CommPhysical
						|| tran.getInstrumentTypeObject().getInstrumentTypeEnum() == EnumInsType.CommPhysBatch) {
						succeed = gatherLegDataAndAskUser(tran, context, rmode, null);
						if (offset != null) {
							Field offsetTranTypeField = offset.getField(EnumTransactionFieldId.OffsetTransactionType);
							String offsetTranType = (offsetTranTypeField != null && offsetTranTypeField.isApplicable())?
									offsetTranTypeField.getValueAsString():"";
							for (LogicResultApplicator ra : logicResultApplicators) {
								if (offsetTranType.equals("Generated Offset") || offsetTranType.equals("Pass Thru Offset")) {
									ra.flipResults(offset.getTransactionId(), offsetTranType, offset);
								}
								ra.applyLogic();
							}
						}
						runPostProcess = false;
					} else {
						// do nothing for other deals than comm phys if processing to status new or proposed
					}
				} else if (ppi.getTargetStatus() == EnumTranStatus.Validated || ppi.getTargetStatus() == EnumTranStatus.Amended){
					if (!isRelevantForPostProcess(tran)) {
						succeed = gatherTranDataAndApplyLogic(tran, context, rmode); // gather deal data and apply logic
						if (offset != null) {
							Field offsetTranTypeField = offset.getField(EnumTransactionFieldId.OffsetTransactionType);
							String offsetTranType = (offsetTranTypeField != null && offsetTranTypeField.isApplicable())?
									offsetTranTypeField.getValueAsString():"";		
							for (LogicResultApplicator ra : logicResultApplicators) {
								if (offsetTranType.equals("Generated Offset") || offsetTranType.equals("Pass Thru Offset")) {
									ra.flipResults(offset.getTransactionId(), offsetTranType, offset);								
								}
								ra.applyLogic();
							}
						}
					} else {
						Table clDataTranGroup = null;
						for (TableRow row : clientData.getRows()) {
							clDataTranGroup = row.getTable("ClientData Table"); // assuming there is just one relevant tran group
						}
						succeed = gatherLegDataAndAskUser(tran, context, rmode, clDataTranGroup);
						runPostProcess = true;
					}
				}
			}
			
			PluginLog.info(this.getClass().getName() + " ended in pre process run\n");
			context.logStatus("succeed=" + succeed);
			
		} catch (RuntimeException ex) {
			PluginLog.error(ex.toString());
			for (StackTraceElement ste : ex.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
			PluginLog.error(this.getClass().getName() + " ended with status failed\n");
			context.logStatus("Failed");
			throw ex;
		}		
		// succeed = true in case the user has confirmed there is no settlement instruction or has selected a settlement instructions
		// false if the action was cancelled by the user
		if (succeed) {
			PluginLog.info("Pre process result succeeded with runPostProcess: " + runPostProcess);
			return PreProcessResult.succeeded(runPostProcess);
		} else {
			PluginLog.info("Pre process result failed (Could not set SI as the action was cancelled by user)");
			return PreProcessResult.failed("Could not set Settlement Instructions");			
		}
	}

	private void addPartyIdsForTran(Transaction tran, Set<Integer> partyIds) {
		if (tran != null) {
			int intBU = tran.getField(EnumTransactionFieldId.InternalBusinessUnit).getValueAsInt();
			int extBU = tran.getField(EnumTransactionFieldId.ExternalBusinessUnit).getValueAsInt();
			partyIds.add(intBU);
			partyIds.add(extBU);
		}
	}
	
	@Override
	public void postProcess(final Session session, final PostProcessingInfo<EnumTranStatus>[] infoArray,
			final boolean succeeded, final Table clientData) {
		boolean succeed=false;
		EnumRunMode rmode = EnumRunMode.POST;
		init (session);
		try {
			PluginLog.info(this.getClass().getName() + " started in post process run\n"); 
			preciousMetalList = DBHelper.retrievePreciousMetalList (session);
			for (PostProcessingInfo<EnumTranStatus> pi : infoArray) {
				Transaction tran = null;
				try {
					tran = session.getTradingFactory().retrieveTransactionById(pi.getTransactionId());
					if (isRelevantForPostProcess (tran)) {
						Table clDataTranGroup = null;
						for (TableRow row : clientData.getRows()) {
							clDataTranGroup = row.getTable("ClientData Table");
						}
						setSettlementInstructionsOnEvents (session, tran, rmode, clDataTranGroup);
					}
				} finally {
					if (tran != null) {
						tran.dispose();
					}
				}
			}
			
			PluginLog.info(this.getClass().getName() + " ended in post process run\n");
			session.logStatus("succeed=" + succeed);
			
		} catch (RuntimeException ex) {
			PluginLog.error(ex.toString());
			for (StackTraceElement ste : ex.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
			PluginLog.error(this.getClass().getName() + " ended with status failed\n");
			session.logStatus("Failed");
			throw ex;
		}
	}

	public PreProcessResult preProcessInternalTarget(final Context context,
			final EnumTranStatusInternalProcessing targetStatus,
			final PreProcessingInfo<EnumTranStatusInternalProcessing>[] infoArray,
			final Table clientData) {
		boolean succeed=true;
		boolean runPostProcess=false;
		EnumRunMode rmode = EnumRunMode.PRE;
		init (context);
		try {
			PluginLog.info(this.getClass().getName() + " started in pre process run\n"); 
			//gatherSettleInsAndAcctData (context); // gather static data
			preciousMetalList = DBHelper.retrievePreciousMetalList (context);
			
			for (PreProcessingInfo<EnumTranStatusInternalProcessing> ppi : infoArray) {
				logicResultApplicators = new ArrayList<>();
				Transaction tran = ppi.getTransaction();
				Field dispatchStatusField = tran.getField(TranInfoField.DISPATCH_STATUS.getName());
				//String tranStatus = tran.getDisplayString(EnumTransactionFieldId.TransactionStatus);
				
				if (tran.getInstrumentTypeObject().getInstrumentTypeEnum() == EnumInsType.CommPhysical
					) {
					int dealTrackingNum = tran.getDealTrackingId();
					boolean isDispatchDeal = DBHelper.isDispatchDeal (context, dealTrackingNum);
					isDispatchDeal |= isDispatchDeal (tran);
					if (isDispatchDeal) {
						continue;	
					}
				}

				if (targetStatus == EnumTranStatusInternalProcessing.SaveTranInfo &&
						dispatchStatusField != null && dispatchStatusField.isApplicable() 
						&& dispatchStatusField.isReadable()) {
					String dispatchStatus = dispatchStatusField.getValueAsString();
					
					int dealTrackingNum = tran.getDealTrackingId();
					if (dealTrackingNum != 0) {
						String oldDispatchStatus = DBHelper.retrieveDispatchStatus (context, dealTrackingNum);
						if ((   oldDispatchStatus.equals("Awaiting Shipping") 
							&& dispatchStatus.equals("Left Site")) 
						|| (   oldDispatchStatus.equals("Left Site") 
								&& dispatchStatus.equals("Awaiting Shipping"))
						|| (   oldDispatchStatus.equals("None") 
								&& dispatchStatus.equals("Awaiting Shipping"))
						|| (   oldDispatchStatus.equals("None") 
								&& dispatchStatus.equals("Left Site"))								
						|| (   oldDispatchStatus.equals("Left Site") 
								&& dispatchStatus.equals("None"))								
						|| (   oldDispatchStatus.equals("Awaiting Shipping") 
								&& dispatchStatus.equals("None"))								
								) {
							continue;
						}
					}
				}
				
				int insType = tran.getInstrumentTypeObject().getId();
				Set<Integer> partyIds = new HashSet<>();
				addPartyIdsForTran(tran, partyIds);

				gatherSettleInsAndAcctData (context, partyIds, insType);
				
				if (!isRelevantForPostProcess(tran)) {
					succeed = gatherTranDataAndApplyLogic(tran, context, rmode); // gather deal data and apply logic
				} else {
					Table clDataTranGroup = null;
					for (TableRow row : clientData.getRows()) {
						clDataTranGroup = row.getTable("ClientData Table");
					}
					succeed = gatherLegDataAndAskUser(tran, context, rmode, clDataTranGroup);
					runPostProcess = true;
				}
			}
			PluginLog.info(this.getClass().getName() + " ended in pre process run\n");
			context.logStatus("succeed=" + succeed);
			
		} catch (RuntimeException ex) {
			PluginLog.error(ex.toString());
			for (StackTraceElement ste : ex.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
			PluginLog.error(this.getClass().getName() + " ended with status failed\n");
			context.logStatus("Failed");
			throw ex;
		}
		
		// succeed = true in case the user has confirmed there is no settlement instruction or has selected a settlement instructions
		// false if the action was cancelled by the user
		if (succeed) {
			PluginLog.info("Pre process result succeeded with runPostProcess: " + runPostProcess);
			return PreProcessResult.succeeded(runPostProcess);
		} else {
			PluginLog.info("Pre process result failed (Could not set SI as the action was cancelled by user)");
			return PreProcessResult.failed("Could not set Settlement Instructions");			
		}
	}

	private boolean isDispatchDeal(Transaction tran) {
		Nominations noms = tran.getNominations();
		boolean isDispatch = false;
		for (int num = noms.getCount()-1; num >=0; num--) {
			Nomination nom = noms.get(num);
			if (nom instanceof Batch) {
				Batch batch = (Batch)nom;
				String activity = batch.retrieveField(EnumNomfField.NomCmotionCsdActivityId, 0).getValueAsString();
				isDispatch |= "Warehouse Dispatch".equals(activity);
			}
		}
		return isDispatch;
	}

	public void postProcessInternalTarget(final Session session,
			final PostProcessingInfo<EnumTranStatusInternalProcessing>[] infoArray,
			final boolean succeeded, final Table clientData) {
		boolean succeed=true;
		EnumRunMode rmode = EnumRunMode.POST; 
		init (session);
		try {
			PluginLog.info(this.getClass().getName() + " started in post process run\n"); 
			preciousMetalList = DBHelper.retrievePreciousMetalList (session);
			for (PostProcessingInfo<EnumTranStatusInternalProcessing> pi : infoArray) {
				Transaction tran = null;
				try {
					tran = session.getTradingFactory().retrieveTransactionById(pi.getTransactionId());
					if (isRelevantForPostProcess (tran)) {
						Table clDataTranGroup = null;
						for (TableRow row : clientData.getRows()) {
								clDataTranGroup = row.getTable("ClientData Table");
						}
						setSettlementInstructionsOnEvents (session, tran, rmode, clDataTranGroup);
					}
				} finally {
					if (tran != null) {
						tran.dispose();
					}
				}
			}
			
			PluginLog.info(this.getClass().getName() + " ended in post process run\n");
			session.logStatus("succeed=" + succeed);
			
		} catch (RuntimeException ex) {
			PluginLog.error(ex.toString());
			for (StackTraceElement ste : ex.getStackTrace()) {
				PluginLog.error(ste.toString());
			}
			PluginLog.error(this.getClass().getName() + " ended with status failed\n");
			session.logStatus("Failed");
			throw ex;
		}
	}

	/**
	 * Checks whether the transaction should be processed by post process or not.
	 * A transaction is relevant for post process in case it is
	 * <ol>
	 *   <li> a COMM-PHYS deal </li>
	 *   <li> it has more than two legs </li>
	 *   <li> the number of currencies is greater than 1 </li>
	 * </ol>
	 * @param tran
	 * @return
	 */
	private boolean isRelevantForPostProcess(Transaction tran) {
		//Set<String> distinctCurrencies = new HashSet<> ();
		//Map<String, Set<String>> distinctFormPhysPerCurrency= new HashMap<> ();

		if (tran.getInstrumentTypeObject().getInstrumentTypeEnum() == EnumInsType.CommPhysical ||
				tran.getInstrumentTypeObject().getInstrumentTypeEnum() == EnumInsType.CommPhysBatch) {
			return true;
		}
		return false;
	}

	private void setSettlementInstructionsOnEvents(final Session session,
			final Transaction tran, final EnumRunMode rmode, final Table clientData) {
		// check table structure
		for (EnumClientDataCol col : EnumClientDataCol.values()) {
			if (!clientData.isValidColumn(col.getColName()) || 
					clientData.getColumnType(clientData.getColumnId(col.getColName())) != col.getColType()) {
				return;
			}
		}
		Field offsetTranTypeField = tran.getField(EnumTransactionFieldId.OffsetTransactionType);
		String offsetTranType = (offsetTranTypeField != null && offsetTranTypeField.isApplicable())?
				offsetTranTypeField.getValueAsString():"";		
		for (DealEvent event : tran.getDealEvents() ) {
			setSettlementInstructionsAccordingToClientData(session, event, clientData, offsetTranType);
		}
	}

	private void setSettlementInstructionsAccordingToClientData(final Session session, 
			final DealEvent event, final Table clientData, String offsetTranTypeTransaction) {
		int legNumE = event.getField("Para Seq Num").getValueAsInt();
		int ccyIdE = event.getField("Settle CCY").getValueAsInt();
		int extBusinessUnitE = event.getField("External Business Unit").getValueAsInt();

		for (TableRow row : clientData.getRows()) {
			String offsetTranType = row.getString(EnumClientDataCol.OFFSET_TRAN_TYPE.getColName());
			if (!offsetTranType.equals(offsetTranTypeTransaction)) {
				//V1.18 reverse internal SI and external SI for offset deal
				int intExtR = row.getInt(EnumClientDataCol.INT_EXT.getColName());
				int settleIdR = row.getInt(EnumClientDataCol.SETTLE_ID.getColName());
				
				if (intExtR == LogicResultApplicator.InternalExternal.INTERNAL.getId()) {
					PluginLog.info("Saving Ext SI #" + settleIdR + " on deal event #" + event.getId());
					int settleFieldId = event.getFieldId("Ext Settle Id");
					event.setValue(settleFieldId, settleIdR);
				} else {
					PluginLog.info("Saving Int SI #" + settleIdR + " on deal event #" + event.getId());
					int settleFieldId = event.getFieldId("Int Settle Id");
					event.setValue(settleFieldId, settleIdR);
				}
				session.getBackOfficeFactory().saveSettlementInstructions(event);
				continue;
			}
			
			int legNumR = row.getInt(EnumClientDataCol.LEG_NUM.getColName());
			int ccyIdR = row.getInt(EnumClientDataCol.CCY_ID.getColName());
			int intExtR = row.getInt(EnumClientDataCol.INT_EXT.getColName());
			int settleIdR = row.getInt(EnumClientDataCol.SETTLE_ID.getColName());
			int extBusinessUnitR = row.getInt(EnumClientDataCol.BUSINESS_UNIT_ID.getColName());
			
			if (legNumE == legNumR && ccyIdE == ccyIdR && extBusinessUnitE == extBusinessUnitR) {
				if (intExtR == LogicResultApplicator.InternalExternal.INTERNAL.getId()) {
					PluginLog.info("Saving Int SI #" + settleIdR + " on deal event #" + event.getId());
					int settleFieldId = event.getFieldId("Int Settle Id");
					event.setValue(settleFieldId, settleIdR);
				} else {
					PluginLog.info("Saving Ext SI #" + settleIdR + " on deal event #" + event.getId());
					int settleFieldId = event.getFieldId("Ext Settle Id");
					event.setValue(settleFieldId, settleIdR);
				}
				session.getBackOfficeFactory().saveSettlementInstructions(event);
			}
		}
	}

	private Set<Leg> getLegsForMetal(Transaction tran, String metal) {
		Set<Leg> legsHavingMetal= new HashSet<> ();
		for (Leg leg : tran.getLegs()) {
			if (leg.getLegNumber() == 0) {
				continue;
			}
			String currency = leg.getField(EnumLegFieldId.Currency).getDisplayString();
			if (currency.equals(metal)) {
				legsHavingMetal.add(leg);
			}
		}
		return legsHavingMetal;
	}
	
	private Set<Leg> getFeeLegsForCurrency(Transaction tran, String ccy) {
		Set<Leg> legsHavingCurrency= new HashSet<> ();
		for (Leg leg : tran.getLegs()) {
			if (leg.getLegNumber() == 0) {
				continue;
			}
			if (leg.getValueAsString(EnumLegFieldId.LegLabel).contains("Physical")) {
				continue;
			}
			String currency = leg.getField(EnumLegFieldId.Currency).getDisplayString();
			if (currency.equals(ccy)) {
				legsHavingCurrency.add(leg);
			}
		}
		return legsHavingCurrency;
	}

	private void gatherSettleInsAndAcctData(Session session, Set<Integer> partyIds, int insType) {
		settleInsAndAccountData = new HashSet<> ();
		Table acctStlTable = null;
		Table stlDeliveryTable = null;
		PluginLog.info("Retrieving SIs static data for criteria- partyIds:" + partyIds + ", insType:" + insType);
		try {
			// gather static accounting data
			StringBuilder sbPartyIds = new StringBuilder();
			for (int partyId : partyIds) {
				sbPartyIds.append(partyId).append(",");
			}
			if (sbPartyIds.length() > 0) {
				sbPartyIds.setLength(sbPartyIds.length() - 1);
			}
			acctStlTable = DBHelper.retrieveAccountData(session, sbPartyIds.toString(), insType);
			
			StringBuilder sbSettleIds = new StringBuilder();
			int settleRows = acctStlTable.getRowCount();
			for (int rowNum = settleRows - 1; rowNum >= 0;rowNum--) {
				sbSettleIds.append(acctStlTable.getInt("settle_id", rowNum)).append(",");
			}
			
			if (sbSettleIds.length() > 0) {
				sbSettleIds.setLength(sbSettleIds.length() - 1);
			}
			stlDeliveryTable = DBHelper.retrieveStlDeliveryTable (session, sbSettleIds.toString());
			Map<Integer, List<Pair<Integer, Integer>>> settleIdToCurrencyAndDeliveryTypeMap = deliveryTableToMap(session, stlDeliveryTable);

			StringBuilder sb = new StringBuilder();
			for (int rowNum = settleRows-1; rowNum >= 0;rowNum--) {
				// merge data and save in settleInsAndAccountData
				sb.append(gatherSettlementInstructionData(settleIdToCurrencyAndDeliveryTypeMap, acctStlTable, rowNum, session));
			}
			
			PluginLog.info("No. of static SI data rows: " + settleInsAndAccountData.size());
			String errors = sb.toString();
			if (errors.trim().length() > 0) {
				throw new RuntimeException (sb.toString());
			}
			
		} finally {
			if (acctStlTable != null) {
				acctStlTable.dispose();
			}
			if (stlDeliveryTable != null) {
				stlDeliveryTable.dispose();
			}
		}
		PluginLog.info("Retrieving SIs static data finished for criteria- partyIds:" + partyIds + ", insType:" + insType);
	}

	private Map<Integer, List<Pair<Integer, Integer>>> deliveryTableToMap(Session session, Table acctStlTable) {
		Map<Integer, List<Pair<Integer, Integer>>> settleIdToInsTypeMap = new TreeMap<>();
		for (int rowNum = acctStlTable.getRowCount()-1; rowNum >= 0;rowNum--) {
			int settleID = acctStlTable.getInt("settle_id", rowNum);
			int currencyId = acctStlTable.getInt("currency_id", rowNum);
			int deliveryTypeID = acctStlTable.getInt("delivery_type", rowNum);
						
			List<Pair<Integer, Integer>> acctStlData = settleIdToInsTypeMap.get(settleID);
			if (acctStlData == null) {
				acctStlData = new ArrayList<>();
				settleIdToInsTypeMap.put(settleID, acctStlData);
			}
			acctStlData.add(new Pair<>(currencyId, deliveryTypeID) );
		}
		return settleIdToInsTypeMap;
	}
	
	private StringBuilder gatherSettlementInstructionData(Map<Integer, List<Pair<Integer, Integer>>> settleIdToCurrencyAndDeliveryTypeMap,
			Table acctStlTable, int rowNum, Session session) {
		int accountId     	 = acctStlTable.getInt ("account_id", rowNum);
		int accountType   	 = acctStlTable.getInt ("account_type", rowNum);
		int internalExternal = (accountType == session.getStaticDataFactory().getId(EnumReferenceTable.AccountType, "Vostro"))?0:1;
		String loco		  	 = acctStlTable.getString("loco", rowNum);
		String form			 = acctStlTable.getString("form", rowNum);
		String aloc			 = acctStlTable.getString("aloc_type", rowNum);
		int settleId		 = acctStlTable.getInt("settle_id", rowNum);
		int partyId			 = acctStlTable.getInt("party_id", rowNum);
		String useShortList  = acctStlTable.getString("use_shortlist", rowNum);
		boolean shortList = (useShortList !=null && useShortList.trim().equals("Yes"))?true:false;

		SettleInsAndAcctData siad = new SettleInsAndAcctData(accountId, settleId);
		siad.setAllocationType(aloc);
		siad.setForm(form);
		siad.setInternalExternal(internalExternal);
		siad.setLoco(loco);
		siad.setSiPartyId(partyId);
		siad.setUseShortList(shortList);

		int insType = acctStlTable.getInt ("ins_type", rowNum);
		if  (insType <= 0) {
			return new StringBuilder ("Error: settlement instruction #" + settleId + " does not have any instruments assigned. "
					+ "Please verify the settlement instruction and try again.\n");
		}
		siad.addInstrument(insType);
		
 		List<Pair<Integer, Integer>> currenciesAndDeliveries = settleIdToCurrencyAndDeliveryTypeMap.get(settleId);
		if (currenciesAndDeliveries == null) {
			return new StringBuilder ("Error: settlement instruction #" + settleId + " does not have any currencies or delivery types assigned "
					+ "Please verify the settlement instruction and try again.\n");
		}
		
		for (Pair<Integer, Integer> curAndDel : currenciesAndDeliveries) {
			siad.addDeliveryInfo(curAndDel);
		}

		//PluginLog.info("Finished processing currency and delivery_types" );
		settleInsAndAccountData.add(siad);
		return new StringBuilder();
	}

	private boolean gatherLegDataAndAskUser(Transaction tran, Session session,
			EnumRunMode rmode, Table clientData) {
		Table dbSettle = null;
		Table memSettle = null;
		List<DecisionData> allDecisionData = new ArrayList<>();

		PluginLog.info("Retrieving transaction data (inside gatherLegDataAndAskUser method)...");
		try {
			int tranNum = tran.getTransactionId();
			boolean isCashTran=false;
			
			if (tran.retrieveField(com.olf.openrisk.trading.EnumTranfField.InsSubType, 0).isApplicable()) {
				EnumInsSub insSubType = tran.getInstrumentSubType();
				isCashTran = (insSubType == EnumInsSub.CashTransaction);
			}
			boolean isCommPhys = tran.getInstrumentTypeObject().getInstrumentTypeEnum() == EnumInsType.CommPhysical;

			Field locoField = tran.getField(TranInfoField.Loco.getName());
			String loco = (locoField != null && locoField.isApplicable())?locoField.getValueAsString():"None";
			Field formField = tran.getField(TranInfoField.Form.getName());
			String form = (formField != null && formField.isApplicable())?formField.getValueAsString():"None";
			if ((formField != null && formField.isApplicable() && formField.getValueAsString().equals("")) ||
				(locoField != null && locoField.isApplicable() && locoField.getValueAsString().equals(""))) {
				return true;
			}
			
			Field useAutoSiField = tran.getField(TranInfoField.USE_AUTO_SI.getName());
			Field useAutoSiShortListField = tran.getField(TranInfoField.USE_AUTO_SI_SHORTLIST.getName());
			
			String useAutoSi = (useAutoSiField != null && useAutoSiField.isApplicable())?useAutoSiField.getValueAsString():"Yes";
			String useAutoSiShortList = (useAutoSiShortListField != null && useAutoSiShortListField.isApplicable())?useAutoSiShortListField.getValueAsString():"Yes";
			boolean didUseShortListChange = DBHelper.didUseShortListChange(session, tranNum, useAutoSiShortList);
			boolean useShortList = useAutoSiShortList.trim().equalsIgnoreCase("Yes");	
			PluginLog.info("Loco: " + loco + ", Form: " + form + ", USE_AUTO_SI: " + useAutoSi + ", USE_AUTO_SI_SHORTLIST: " + useAutoSiShortList);
			
			if (!useAutoSi.trim().equalsIgnoreCase("Yes")) {
				return true;
			}
						
			if (rmode == EnumRunMode.PRE) { 
				ConstTable settleIns = tran.getValueAsTable(EnumTransactionFieldId.SettlementTable);
				if (settleIns == null) {
					throw new RuntimeException ("Could not retrieve settlement instructions from transaction pointer");
				}
				memSettle = retrieveData((Context)session, settleIns);
				dbSettle = DBHelper.retrieveSettleDataTransaction(tranNum, session);
				memSettle.select(dbSettle, "settle_instructions->si_db_int",
						"[In.currency_id] == [Out.currency_id] AND [In.delivery_type] == [Out.delivery_type_id] AND [In.int_ext] == 0" );
				memSettle.select(dbSettle, "settle_instructions->si_db_ext",
						"[In.currency_id] == [Out.currency_id] AND [In.delivery_type] == [Out.delivery_type_id] AND [In.int_ext] == 1" );
			} 
			for (TableRow row : memSettle.getRows() ) {
				int paraSeqNum = row.getInt("para_seq_num");
				DecisionData dd = new DecisionData(tranNum, paraSeqNum);
				int ccyId = row.getInt("currency_id");
				String currencyName = (ccyId == -1)?"None":session.getStaticDataFactory().getName(EnumReferenceTable.Currency,
						ccyId);

				int deliveryTypeId = row.getInt("delivery_type_id");
				int intSettleId = row.getInt("int_settle_id");
				int extSettleId = row.getInt("ext_settle_id");					
				int intPartyId = row.getInt("int_party_id");
				int extPartyId = row.getInt("ext_party_id");
				String intSettleName = row.getString("int_settle_name");
				String extSettleName = row.getString("ext_settle_name");

				int insType = row.getInt("ins_type");
				int settleIdDbInt = row.getInt("si_db_int");
				int settleIdDbExt = row.getInt("si_db_ext");

				SavedUnsaved suInt = (intSettleId ==  settleIdDbInt && !didUseShortListChange)?SavedUnsaved.SAVED:SavedUnsaved.UNSAVED;
				SavedUnsaved suExt = (extSettleId ==  settleIdDbExt && !didUseShortListChange)?SavedUnsaved.SAVED:SavedUnsaved.UNSAVED;

				Set<Pair<Integer, String>> posCoreIntSIs = getCoreSIs (session, tran, ccyId, intPartyId, deliveryTypeId);
				Set<Pair<Integer, String>> posCoreExtSIs = getCoreSIs (session, tran, ccyId, extPartyId, deliveryTypeId);
				dd.setPosCoreIntSIs(posCoreIntSIs);
				dd.setPosCoreExtSIs(posCoreExtSIs);
				dd.setUseShortList(useShortList);

				dd.setCcyId(ccyId);
				dd.setDeliveryTypeId(deliveryTypeId);
				//					dd.setInternalExternal(intExt);
				dd.setIntPartyId(intPartyId);
				dd.setExtPartyId(extPartyId);
				dd.setIntSettleId(intSettleId);
				dd.setExtSettleId(extSettleId);
				dd.setIntSettleName(intSettleName);
				dd.setExtSettleName(extSettleName);
				dd.setSavedUnsavedInt(suInt);
				dd.setSavedUnsavedExt(suExt);
				dd.setInsType(insType);
				dd.setCommPhys(isCommPhys);

				dd.setCashTran(isCashTran);
				dd.setLoco(preciousMetalList.contains(ccyId)?loco:"None");
				dd.setForm(preciousMetalList.contains(ccyId)?form:"None");
				Field offsetTranTypeField = tran.getField(EnumTransactionFieldId.OffsetTransactionType);
				String offsetTranType = (offsetTranTypeField != null && offsetTranTypeField.isApplicable())?
						offsetTranTypeField.getValueAsString():"";

				Field allocationTypeField = tran.getField(TranInfoField.AllocationType.getName());
				String allocationType = (allocationTypeField != null && allocationTypeField.isApplicable() && preciousMetalList.contains(ccyId))?
						allocationTypeField.getValueAsString():"None";
				int formPhysLegNo = tran.getLegCount()>1?1:0;
				if (paraSeqNum > 1 && isCommPhys) {
					formPhysLegNo = paraSeqNum;
				}
				dd.setAllocationType(allocationType);
				dd.setOffsetTranType(offsetTranType);
				if (preciousMetalList.contains(ccyId)) { // duplicate settlement instruction for every 
					for (Leg leg : getLegsForMetal(tran, currencyName)) {
						Field siPhysField = leg.getField(TranInfoField.SI_PHYS.getName());						
						int siPhys = -1;
						if (siPhysField != null && siPhysField.isApplicable() && tranNum != 0) {
							siPhys = siPhysField.getValueAsInt();
						}
						dd.setSiPhys(siPhys);
						Field siPhysInternalField = leg.getField(TranInfoField.SI_PHYS_INTERNAL.getName());						
						int siPhysInternal = -1;
						if (siPhysInternalField != null && siPhysInternalField.isApplicable() && tranNum != 0) {
							siPhysInternal = siPhysInternalField.getValueAsInt();
						}
						dd.setSiPhysInternal(siPhysInternal);
						
						if (dd.getExtSettleId() <= 0) {
							dd.setExtSettleId(siPhys);
							if (siPhys > 0) {
								String siPhysName = 
										session.getStaticDataFactory().getName(EnumReferenceTable.SettleInstructions, siPhys);
								dd.setExtSettleName(siPhysName);								
							} else {
								dd.setExtSettleName("");
							}
							dd.setSavedUnsavedExt(SavedUnsaved.SAVED);
						}
						
						if (dd.getIntSettleId() <= 0) {
							dd.setIntSettleId(siPhysInternal);
							if (siPhysInternal > 0) {
								String siPhysName = 
										session.getStaticDataFactory().getName(EnumReferenceTable.SettleInstructions, siPhysInternal);
								dd.setIntSettleName(siPhysName);								
							} else {
								dd.setIntSettleName("");
							}
							dd.setSavedUnsavedExt(SavedUnsaved.SAVED);
						}
						paraSeqNum = leg.getLegNumber();
						dd = new DecisionData(tranNum, paraSeqNum, dd);
						formPhysLegNo = paraSeqNum;
						Field formPhysField = tran.getLeg(formPhysLegNo).getField(ParamInfoField.FormPhys.getName());
						String formPhys = (formPhysField != null && formPhysField.isApplicable() && preciousMetalList.contains(ccyId))?
								formPhysField.getValueAsString():"None";
						dd.setFormPhys(formPhys);
						allDecisionData.add(dd);
					}
				} else {
					Collection<Leg> financialLegsForCurrency = getFeeLegsForCurrency(tran, currencyName);
					for (Leg leg : financialLegsForCurrency) { // will be executed one time max even if there is more than one fee leg
						Field siPhysField = leg.getField(TranInfoField.SI_PHYS.getName());						
						int siPhys = -1;
						if (siPhysField != null && siPhysField.isApplicable() && tranNum != 0) {
							siPhys = siPhysField.getValueAsInt();
						}
						dd.setSiPhys(siPhys);
						Field siPhysInternalField = leg.getField(TranInfoField.SI_PHYS_INTERNAL.getName());						
						int siPhysInternal = -1;
						if (siPhysInternalField != null && siPhysInternalField.isApplicable() && tranNum != 0) {
							siPhysInternal = siPhysInternalField.getValueAsInt();
						}
						dd.setSiPhysInternal(siPhysInternal);
						
						if (dd.getExtSettleId() <= 0) {
							dd.setExtSettleId(siPhys);
							if (siPhys > 0) {
								String siPhysName = 
										session.getStaticDataFactory().getName(EnumReferenceTable.SettleInstructions, siPhys);
								dd.setExtSettleName(siPhysName);								
							} else {
								dd.setExtSettleName("");
							}
						}

						if (dd.getIntSettleId() <= 0) {
							dd.setIntSettleId(siPhysInternal);
							if (siPhysInternal > 0) {
								String siPhysName = 
										session.getStaticDataFactory().getName(EnumReferenceTable.SettleInstructions, siPhysInternal);
								dd.setIntSettleName(siPhysName);								
							} else {
								dd.setIntSettleName("");
							}
							dd.setSavedUnsavedExt(SavedUnsaved.SAVED);
						}
						
						Field formPhysField = tran.getLeg(formPhysLegNo).getField(ParamInfoField.FormPhys.getName());
						String formPhys = (formPhysField != null && formPhysField.isApplicable() && preciousMetalList.contains(ccyId))?
								formPhysField.getValueAsString():"None";
						if (formPhysField != null && formPhysField.isApplicable() && formPhysField.getValueAsString().equals("")) {
							return true;
						}
						dd.setFormPhys(formPhys);
						allDecisionData.add(dd);
						break;
					}						
				}
			}
			
			PluginLog.info("No. of rows in decision data: " + allDecisionData.size());
			Logic logic = new Logic(allDecisionData, settleInsAndAccountData, session);
			List<LogicResult> result = logic.applyLogic();
			LogicResultApplicator applicator=null;
			if (clientData != null) {
				applicator = (useException)? new LogicResultApplicatorComplexCommPhysException(tran, session, result, rmode, clientData, preciousMetalList):
					new LogicResultApplicatorComplexCommPhys(tran, session, result, rmode, clientData, preciousMetalList);
				boolean ret =  applicator.applyLogic();
				this.logicResultApplicators.add(applicator);
				applicator = new LogicResultApplicatorTranInfoField (tran, session, result, rmode, preciousMetalList);
				ret &= applicator.applyLogic();
				this.logicResultApplicators.add(applicator);
				return ret;
			} else {
				applicator = new LogicResultApplicatorTranInfoField (tran, session, result, rmode, preciousMetalList);
				this.logicResultApplicators.add(applicator);
				return applicator.applyLogic();
			}
		} finally {
			if (dbSettle != null) {
				dbSettle.dispose();
			}
			if (memSettle != null) {
				memSettle.dispose();
			}
			PluginLog.info("Finished retrieving transaction data (inside gatherLegDataAndAskUser method)...");
		}

	}

	private boolean gatherTranDataAndApplyLogic(final Transaction tran, final Session session, EnumRunMode rmode) {
		Table dbSettle = null;
		Table memSettle = null;
		List<DecisionData> allDecisionData = new ArrayList<>();
		
		if (isCashTransfer(tran)) {
			PluginLog.info ("Transaction #" + tran.getTransactionId() + " is cash transfer. Skipping.");
			return true;
		}

		PluginLog.info("Retrieving transaction data (inside gatherTranDataAndApplyLogic() method)...");
		try {
			int tranNum = tran.getTransactionId();
			boolean isCashTran=false;
			if (tran.retrieveField(com.olf.openrisk.trading.EnumTranfField.InsSubType, 0).isApplicable()) {
				EnumInsSub insSubType = tran.getInstrumentSubType();
				isCashTran = (insSubType == EnumInsSub.CashTransaction);
			}
			boolean isCommPhys = tran.getInstrumentTypeObject().getInstrumentTypeEnum() == EnumInsType.CommPhysical;

			Field locoField = tran.getField(TranInfoField.Loco.getName());
			String loco = (locoField != null && locoField.isApplicable())?locoField.getValueAsString():"None";
			Field formField = tran.getField(TranInfoField.Form.getName());
			String form = (formField != null && formField.isApplicable())?formField.getValueAsString():"None";
			if ((formField != null && formField.isApplicable() && formField.getValueAsString().equals("")) ||
					(locoField != null && locoField.isApplicable() && locoField.getValueAsString().equals(""))) {
					return true;
				}

			Field siPhysField = tran.getField(TranInfoField.SI_PHYS_TRAN.getName());						
			int siPhys = -1;
			if (siPhysField != null && siPhysField.isApplicable()) {
				siPhys = siPhysField.getValueAsInt();
			}
			Field siPhysInternalField = tran.getField(TranInfoField.SI_PHYS_INTERNAL_TRAN.getName());						
			int siPhysInternal = -1;
			if (siPhysInternalField != null && siPhysInternalField.isApplicable()) {
				siPhysInternal = siPhysInternalField.getValueAsInt();
			}
			
			Field useAutoSiField = tran.getField(TranInfoField.USE_AUTO_SI.getName());
			Field useAutoSiShortListField = tran.getField(TranInfoField.USE_AUTO_SI_SHORTLIST.getName());
			String useAutoSi = (useAutoSiField != null && useAutoSiField.isApplicable())?useAutoSiField.getValueAsString():"Yes";
			String useAutoSiShortList = (useAutoSiShortListField != null && useAutoSiShortListField.isApplicable())?useAutoSiShortListField.getValueAsString():"Yes";
			boolean useShortList = useAutoSiShortList.trim().equalsIgnoreCase("Yes");
			boolean didUseShortListChange = DBHelper.didUseShortListChange(session, tranNum, useAutoSiShortList);
			PluginLog.info("Loco: " + loco + ", Form: " + form + ", USE_AUTO_SI: " + useAutoSi + ", USE_AUTO_SI_SHORTLIST: " + useAutoSiShortList);
			
			if (!useAutoSi.trim().equalsIgnoreCase("Yes")) {
				return true;
			}
			if (rmode == EnumRunMode.PRE) { 
				ConstTable settleIns = tran.getValueAsTable(EnumTransactionFieldId.SettlementTable);
				if (settleIns == null) {
					throw new RuntimeException ("Could not retrieve settlement instructions from transaction pointer");
				}
				memSettle = retrieveData((Context)session, settleIns);
				dbSettle = DBHelper.retrieveSettleDataTransaction(tranNum, session);
				memSettle.select(dbSettle, "settle_instructions->si_db_int",
						"[In.currency_id] == [Out.currency_id] AND [In.delivery_type] == [Out.delivery_type_id] AND [In.int_ext] == 0" );
				memSettle.select(dbSettle, "settle_instructions->si_db_ext",
						"[In.currency_id] == [Out.currency_id] AND [In.delivery_type] == [Out.delivery_type_id] AND [In.int_ext] == 1" );
			} else if (rmode == EnumRunMode.POST) {
				memSettle = retrieveData(session, tran);
			}
			for (TableRow row : memSettle.getRows() ) {
				int paraSeqNum = row.getInt("para_seq_num");
				DecisionData dd = new DecisionData(tranNum, paraSeqNum);
				int ccyId = row.getInt("currency_id");
				int deliveryTypeId = row.getInt("delivery_type_id");
				int intSettleId = row.getInt("int_settle_id");
				int extSettleId = row.getInt("ext_settle_id");					
				int intPartyId = row.getInt("int_party_id");
				int extPartyId = row.getInt("ext_party_id");
				String intSettleName = row.getString("int_settle_name");
				String extSettleName = row.getString("ext_settle_name");

				int insType = row.getInt("ins_type");
				int settleIdDbInt = row.getInt("si_db_int");
				int settleIdDbExt = row.getInt("si_db_ext");

				SavedUnsaved suInt = (intSettleId ==  settleIdDbInt && !didUseShortListChange)?SavedUnsaved.SAVED:SavedUnsaved.UNSAVED;
				SavedUnsaved suExt = (extSettleId ==  settleIdDbExt && !didUseShortListChange)?SavedUnsaved.SAVED:SavedUnsaved.UNSAVED;

				Set<Pair<Integer, String>> posCoreIntSIs = getCoreSIs (session, tran, ccyId, intPartyId, deliveryTypeId);
				Set<Pair<Integer, String>> posCoreExtSIs = getCoreSIs (session, tran, ccyId, extPartyId, deliveryTypeId);
				dd.setPosCoreIntSIs(posCoreIntSIs);
				dd.setPosCoreExtSIs(posCoreExtSIs);
				dd.setUseShortList(useShortList);

				dd.setCcyId(ccyId);
				dd.setDeliveryTypeId(deliveryTypeId);
				//					dd.setInternalExternal(intExt);
				dd.setIntPartyId(intPartyId);
				dd.setExtPartyId(extPartyId);
				dd.setIntSettleId(intSettleId);
				dd.setExtSettleId(extSettleId);
				dd.setIntSettleName(intSettleName);
				dd.setExtSettleName(extSettleName);
				dd.setSavedUnsavedInt(suInt);
				dd.setSavedUnsavedExt(suExt);
				dd.setInsType(insType);
				dd.setCommPhys(isCommPhys);

				dd.setCashTran(isCashTran);
				dd.setLoco(preciousMetalList.contains(ccyId)?loco:"None");
				dd.setForm(preciousMetalList.contains(ccyId)?form:"None");
				Field offsetTranTypeField = tran.getField(EnumTransactionFieldId.OffsetTransactionType);
				String offsetTranType = (offsetTranTypeField != null && offsetTranTypeField.isApplicable())?
						offsetTranTypeField.getValueAsString():"";
				Field allocationTypeField = tran.getField(TranInfoField.AllocationType.getName());
				String allocationType = (allocationTypeField != null && allocationTypeField.isApplicable() && preciousMetalList.contains(ccyId))?
						allocationTypeField.getValueAsString():"None";
				int formPhysLegNo = tran.getLegCount()>1?findLegForCurrency(tran, ccyId):0;
				if (paraSeqNum > 1 && isCommPhys) {
						formPhysLegNo = paraSeqNum;
				}
				Field formPhysField = tran.getLeg(formPhysLegNo).getField(ParamInfoField.FormPhys.getName());
				String formPhys = (formPhysField != null && formPhysField.isApplicable() && preciousMetalList.contains(ccyId))?
						formPhysField.getValueAsString():"None";
				dd.setAllocationType(allocationType);
				dd.setFormPhys(formPhys);
				dd.setOffsetTranType(offsetTranType);
				dd.setSiPhys(siPhys);
				if (dd.getExtSettleId() <= 0) {
					dd.setExtSettleId(siPhys);
					if (siPhys > 0) {
						String siPhysName = 
								session.getStaticDataFactory().getName(EnumReferenceTable.SettleInstructions, siPhys);
						dd.setExtSettleName(siPhysName);								
					} else {
						dd.setExtSettleName("");
					}
					dd.setSavedUnsavedExt(SavedUnsaved.SAVED);					
				}
				
				dd.setSiPhysInternal(siPhysInternal);
				if (dd.getIntSettleId() <= 0) {
					dd.setIntSettleId(siPhysInternal);
					if (siPhysInternal > 0) {
						String siPhysName = 
								session.getStaticDataFactory().getName(EnumReferenceTable.SettleInstructions, siPhysInternal);
						dd.setIntSettleName(siPhysName);								
					} else {
						dd.setIntSettleName("");
					}
					dd.setSavedUnsavedExt(SavedUnsaved.SAVED);
				}
				allDecisionData.add(dd);
			}
			
			PluginLog.info("No. of rows in decision data: " + allDecisionData.size());
			Logic logic = new Logic(allDecisionData, settleInsAndAccountData, session);
			List<LogicResult> result = logic.applyLogic();
			if (tran.getInstrumentTypeObject().getInstrumentTypeEnum() == EnumInsType.CommPhysical 
				|| tran.getInstrumentTypeObject().getInstrumentTypeEnum() == EnumInsType.CommPhysBatch) {
				boolean ret;
				LogicResultApplicator applicator = (useException)? new LogicResultApplicatorException(tran, session, result, rmode):
					new LogicResultApplicator(tran, session, result, rmode);
				ret = applicator.applyLogic();
				this.logicResultApplicators.add(applicator);
				applicator = new LogicResultApplicatorTranInfoField(tran, session, result, rmode, preciousMetalList);
				ret &= applicator.applyLogic();
				this.logicResultApplicators.add(applicator);
				return ret;
			} else {
				LogicResultApplicator applicator = (useException)? new LogicResultApplicatorException(tran, session, result, rmode):
					new LogicResultApplicator(tran, session, result, rmode);
				this.logicResultApplicators.add(applicator);
				return applicator.applyLogic();
			}
		} finally {
			if (dbSettle != null) {
				dbSettle.dispose();
			}
			if (memSettle != null) {
				memSettle.dispose();
			}
			PluginLog.info("Finished retrieving transaction data (inside gatherTranDataAndApplyLogic() method)...");
		}
	}

	private int findLegForCurrency(Transaction tran, int ccyId) {
		for (Leg leg : tran.getLegs()) {
			if (leg.getLegNumber() == 0) {
				continue;
			}
			Field legCurrencyField = leg.getField(EnumLegFieldId.Currency);
			if (legCurrencyField != null && legCurrencyField.isApplicable() && legCurrencyField.isReadable()) {
				if (legCurrencyField.getValueAsInt() == ccyId) {
					return leg.getLegNumber();
				}
			}
		}
		return 0;
	}

	private Set<Pair<Integer, String>> getCoreSIs(Session session,
			Transaction tran, int ccyId, int partyId, int deliveryTypeId) {
		Set<Pair<Integer, String>> coreSIs = new HashSet<>();
		if (ccyId == -1 || partyId == -1 || deliveryTypeId == -1)
			return coreSIs;
		Currency c = (Currency)session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.Currency, ccyId);
		Party p = (Party)session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.BusinessUnit, partyId);
		DeliveryType d = (DeliveryType)session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.DeliveryType, deliveryTypeId);

		SettlementInstruction[] sis = session.getBackOfficeFactory().getPossibleSettlementInstructions(tran, c, p, d);
		for (SettlementInstruction si : sis) {
			coreSIs.add(new Pair<>(si.getId(), si.getName()));
		}
		PluginLog.info("Core Code settlement instructions for tran #" + tran.getTransactionId() + ", ccy=" + c.getName()
				+ ", party=" + p.getName() + ", delivery type=" + d.getName() + "\n" + coreSIs);
		return coreSIs;
	}

	private boolean isCashTransfer(Transaction tran) {
		int insTypeId = tran.getField(EnumTransactionFieldId.InstrumentType).getValueAsInt();
		EnumInsType insType = EnumInsType.retrieve(insTypeId);
		if (insType == EnumInsType.CashInstrument) {
			int insSubTypeId = tran.getField(EnumTransactionFieldId.InstrumentSubType).getValueAsInt();				
			EnumInsSub insSubType = EnumInsSub.retrieve(insSubTypeId);
			if (insSubType == EnumInsSub.CashTransfer) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	private Table retrieveData(final Context context,
			final ConstTable settleIns) {
		Table memSettle = setupMemSettleTable(context);

		for (int row = settleIns.getRowCount()-1; row >= 0; row--) {
			int settleId = settleIns.getInt("Settle Id", row);
			int partyId = settleIns.getInt("Party Id", row);
			int insType = settleIns.getInt("Ins Type", row);
			int ccy = settleIns.getInt("Currency", row);
			int deliveryId = settleIns.getInt("Delivery", row);
			int intExt = settleIns.getInt("Int/Ext", row);
			String settleName = settleIns.getString("Settle Name", row);

			int matchingRow = findMatchingRow (memSettle, insType, -1, deliveryId, ccy);
			int memSettleRow;
			if (matchingRow == -1) {
				memSettleRow = memSettle.addRow().getNumber();
			} else {
				memSettleRow = memSettle.getRow(matchingRow).getNumber();
			}
			memSettle.setInt("para_seq_num", memSettleRow, -1);
			memSettle.setInt("event_num", memSettleRow, -1);
			memSettle.setInt("currency_id", memSettleRow, ccy);
			memSettle.setInt("delivery_type_id", memSettleRow, deliveryId);
			memSettle.setInt("ins_type", memSettleRow, insType);
			if (intExt == 0) {
				memSettle.setInt("int_settle_id", memSettleRow, settleId);
				memSettle.setInt("int_party_id", memSettleRow, partyId);
				memSettle.setString("int_settle_name", memSettleRow, settleName);
			} else {
				memSettle.setInt("ext_settle_id", memSettleRow, settleId);
				memSettle.setInt("ext_party_id", memSettleRow, partyId);
				memSettle.setString("ext_settle_name", memSettleRow, settleName);
			}
			memSettle.setInt("si_db_int", memSettleRow, -1);
			memSettle.setInt("si_db_ext", memSettleRow, -1);
		}		
		return memSettle;
	}

	private int findMatchingRow(Table memSettle, int insType, int paraSeqNum, int deliveryId,
			int ccy) {

		for (int row = memSettle.getRowCount()-1; row>=0; row--) {
			int otherInsType = memSettle.getInt("ins_type", row);
			int otherParaSeqNum = memSettle.getInt("para_seq_num", row);
			int otherDeliveryId = memSettle.getInt("delivery_type_id", row);
			int otherCcy = memSettle.getInt("currency_id", row);
			if (otherInsType == insType && otherParaSeqNum == paraSeqNum 
					&& otherDeliveryId == deliveryId && otherCcy == ccy) {
				return row;
			}
		}
		return -1;
	}

	/**
	 * Retrieves settlement instruction data from <b>events</b> for a certain transaction.
	 * @param session
	 * @param tran
	 * @return
	 */
	private Table retrieveData(Session session, Transaction tran) {
		Table memSettle;
		memSettle = setupMemSettleTable(session);
		DealEvents events = tran.getDealEvents();

		for (DealEvent event : events) {
			TableRow row = memSettle.addRow();
			memSettle.setInt("int_party_id", row.getNumber(), 
					tran.getField(EnumTransactionFieldId.InternalBusinessUnit).getValueAsInt());
			memSettle.setInt("ext_party_id", row.getNumber(), 
					tran.getField(EnumTransactionFieldId.ExternalBusinessUnit).getValueAsInt());
			memSettle.setInt("ins_type", row.getNumber(), 
					tran.getField(EnumTransactionFieldId.InstrumentType).getValueAsInt());

			memSettle.setInt("si_db_int", row.getNumber(), -1);
			memSettle.setInt("si_db_ext", row.getNumber(), -1);	

			Fields<Field> fields = event.getFields();
			for (Field field : fields) {
				switch (field.getName()) {
				case "Para Seq Num":
					memSettle.setInt("para_seq_num", row.getNumber(), field.getValueAsInt());
					break;
				case "Settle CCY":
					memSettle.setInt("currency_id", row.getNumber(), field.getValueAsInt());
					break;
				case "Delivery Type":
					memSettle.setInt("delivery_type_id", row.getNumber(), field.getValueAsInt());
					break;
				case "Int Settle Id":
					memSettle.setInt("int_settle_id", row.getNumber(), field.getValueAsInt());
					memSettle.setString("int_settle_name", row.getNumber(), field.getValueAsString());							
					break;
				case "Ext Settle Id":
					memSettle.setInt("ext_settle_id", row.getNumber(), field.getValueAsInt());
					memSettle.setString("ext_settle_name", row.getNumber(), field.getValueAsString());
					break;
				case "Event Num":
					memSettle.setInt("event_num", row.getNumber(), field.getValueAsInt());
					break;
				}
				PluginLog.debug(field.getName() + " = " + field.getDisplayString());
			}
		}
		return memSettle;
	}

	private Table setupMemSettleTable(Session session) {
		Table memSettle;
		memSettle = session.getTableFactory().createTable("Event level settlement instructions");
		memSettle.addColumn ("para_seq_num", EnumColType.Int);
		memSettle.addColumn ("currency_id", EnumColType.Int);
		memSettle.addColumn ("delivery_type_id", EnumColType.Int);
		memSettle.addColumn ("int_settle_id", EnumColType.Int);
		memSettle.addColumn ("ext_settle_id", EnumColType.Int);
		memSettle.addColumn ("int_party_id", EnumColType.Int);
		memSettle.addColumn ("ext_party_id", EnumColType.Int);
		memSettle.addColumn ("ins_type", EnumColType.Int);
		memSettle.addColumn("si_db_int", EnumColType.Int);
		memSettle.addColumn("si_db_ext", EnumColType.Int);
		memSettle.addColumn("int_settle_name", EnumColType.String);
		memSettle.addColumn("ext_settle_name", EnumColType.String);
		memSettle.addColumn("event_num", EnumColType.Int);
		return memSettle;
	}

	/**
	 * Inits plugin log by retrieving logging settings from constants repository.
	 * @param context
	 */
	private void init(Session session) {
		try {
			String abOutdir = session.getSystemSetting("AB_OUTDIR");
			ConstRepository constRepo = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			// retrieve constants repository entry "logLevel" using default value "info" in case if it's not present:
			String logLevel = constRepo.getStringValue("logLevel", "info"); 
			String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
			String logDir = constRepo.getStringValue("logDir", abOutdir);
			
			try {
				PluginLog.init(logLevel, logDir, logFile);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
			//String useCacheConstRepo = constRepo.getStringValue("useCache", "FALSE");
			String useExceptionConstRepo = constRepo.getStringValue("useExceptionInsteadOfDialog", "FALSE");
			//useCache = Boolean.parseBoolean(useCacheConstRepo);			
			useException = Boolean.parseBoolean(useExceptionConstRepo);
			
		} catch (OException e) {
			throw new RuntimeException (e);
		}		
		PluginLog.info("\n\n********************* Start of new run ***************************");

		try {
			UIManager.setLookAndFeel( // for dialogs that are used in pre process runs
					UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException (e);
		} catch (InstantiationException e) {
			throw new RuntimeException (e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException (e);
		} catch (UnsupportedLookAndFeelException e) {
			throw new RuntimeException (e);
		}
	}
	
	/*private void gatherSettleInsAndAcctData(Session session) {
		if (settleInsAndAccountData == null || useCache == false) {
			settleInsAndAccountData = new HashSet<> ();
			Table acctStlTable = null;
			Table stlDeliveryTable = null;
			Table instrumentTable = null;

			PluginLog.info("Retrieving SIs static data...");

			try {
				// gather static accounting data 
				acctStlTable = DBHelper.retrieveAccountData(session); 
				PluginLog.info("Finished retrieving Account Data");
				
				// gather static mapping between settlement instructions and delivery			
				stlDeliveryTable = DBHelper.retrieveStlDeliveryTable (session); 
				PluginLog.info("Finished retrieving SI currency & deliveryType table");
				
				// gather static data of settlement instructions.
				instrumentTable = DBHelper.retrieveStlInsTable(session); 
				PluginLog.info("Finished retrieving SI instruments table");
				
				Map<Integer, List<Integer>> settleIdToInsTypeMap = insTypeTableToMap(instrumentTable);
				Map<Integer, List<Pair<Integer, Integer>>> settleIdToCurrencyAndDeliveryTypeMap = 
						deliveryTableToMap(stlDeliveryTable);
				StringBuilder sb = new StringBuilder();
				for (int rowNum = acctStlTable.getRowCount()-1; rowNum >= 0;rowNum--) {
					// merge data and save in settleInsAndAccountData
					sb.append(gatherSettlementInstructionData(settleIdToCurrencyAndDeliveryTypeMap,
							settleIdToInsTypeMap, acctStlTable, rowNum, session));
				}
				PluginLog.info("No. of static SI data rows: " + settleInsAndAccountData.size());
				
				String errors = sb.toString();
				if (errors.trim().length() > 0) {
					throw new RuntimeException (sb.toString());
				}
			} finally {
				if (acctStlTable != null) {
					acctStlTable.dispose();
				}
				if (stlDeliveryTable != null) {
					stlDeliveryTable.dispose();
				}
				if (instrumentTable != null) {
					instrumentTable.dispose();
				}
			}
			PluginLog.info("Retrieving Static Data finished...");
		} else {
			PluginLog.info("Retrieving Static Data skipped - using cached values");			
		}
	}
	
	private Map<Integer, List<Pair<Integer, Integer>>> deliveryTableToMap(Table acctStlTable) {
		Map<Integer, List<Pair<Integer, Integer>>> settleIdToInsTypeMap = new TreeMap<>();
		for (int rowNum = acctStlTable.getRowCount() - 1; rowNum >= 0; rowNum--) {
			int settleID = acctStlTable.getInt("settle_id", rowNum);
			int currencyId = acctStlTable.getInt("currency_id", rowNum);
			int deliveryTypeID = acctStlTable.getInt("delivery_type", rowNum);

			List<Pair<Integer, Integer>> acctStlData = settleIdToInsTypeMap.get(settleID);
			if (acctStlData == null) {
				acctStlData = new ArrayList<>();
				settleIdToInsTypeMap.put(settleID, acctStlData);
			}
			acctStlData.add(new Pair<>(currencyId, deliveryTypeID));
		}
		return settleIdToInsTypeMap;
	}

	private Map<Integer, List<Integer>> insTypeTableToMap(Table instrumentTable) {
		Map<Integer, List<Integer>> settleIdToInsTypeMap = new TreeMap<>();
		for (int rowNum = instrumentTable.getRowCount() - 1; rowNum >= 0; rowNum--) {
			int settleID = instrumentTable.getInt("settle_id", rowNum);
			int insType = instrumentTable.getInt("ins_type", rowNum);
			List<Integer> insTypes = settleIdToInsTypeMap.get(settleID);
			if (insTypes == null) {
				insTypes = new ArrayList<>();
				settleIdToInsTypeMap.put(settleID, insTypes);
			}
			insTypes.add(insType);
		}
		return settleIdToInsTypeMap;
	}

	private StringBuilder gatherSettlementInstructionData(Map<Integer, List<Pair<Integer, Integer>>> settleIdToCurrencyAndDeliveryTypeMap,
			Map<Integer, List<Integer>> settleIdToInsTypeMap, Table acctStlTable, int rowNum, Session session) {
		int accountId = acctStlTable.getInt("account_id", rowNum);
		int accountType = acctStlTable.getInt("account_type", rowNum);
		int internalExternal = (accountType == session.getStaticDataFactory().getId(EnumReferenceTable.AccountType, "Vostro")) ? 0 : 1;
		String loco = acctStlTable.getString("loco", rowNum);
		String form = acctStlTable.getString("form", rowNum);
		String aloc = acctStlTable.getString("aloc_type", rowNum);
		int settleId = acctStlTable.getInt("settle_id", rowNum);
		int partyId = acctStlTable.getInt("party_id", rowNum);
		String useShortList = acctStlTable.getString("use_shortlist", rowNum);
		boolean shortList = (useShortList != null && useShortList.trim().equals("Yes")) ? true : false;

		SettleInsAndAcctData siad = new SettleInsAndAcctData(accountId,
				settleId);
		siad.setAllocationType(aloc);
		siad.setForm(form);
		siad.setInternalExternal(internalExternal);
		siad.setLoco(loco);
		siad.setSiPartyId(partyId);
		siad.setUseShortList(shortList);

		List<Integer> insTypes = settleIdToInsTypeMap.get(settleId);
		if (insTypes == null) {
			PluginLog
					.error("Error: settlement instruction #" + settleId + " does not have any instruments assigned. "
							+ "Please verify the settlement instruction \n Deal booking would continue");

			return new StringBuilder(
					"Error: settlement instruction #" + settleId + " does not have any instruments assigned. "
							+ "Please verify the settlement instruction and try again.\n");

		} else
			for (int insType : insTypes) {
				siad.addInstrument(insType);
			}
		// PluginLog.info ("Processed all ins types");

		List<Pair<Integer, Integer>> currenciesAndDeliveries = settleIdToCurrencyAndDeliveryTypeMap
				.get(settleId);
		if (currenciesAndDeliveries == null) {
			PluginLog
					.error("Error: settlement instruction #" + settleId
							+ " does not have any currencies or delivery types assigned "
							+ "Please verify the settlement instruction \n Deal booking would continue");

			return new StringBuilder(
					"Error: settlement instruction #" + settleId
							+ " does not have any currencies or delivery types assigned "
							+ "Please verify the settlement instruction and try again.\n");
		}
		for (Pair<Integer, Integer> curAndDel : currenciesAndDeliveries) {
			siad.addDeliveryInfo(curAndDel);
		}

		// PluginLog.info ("Finished processing currency and delivery_types" );
		settleInsAndAccountData.add(siad);
		return new StringBuilder();
	}*/
}