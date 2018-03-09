package com.olf.jm.autosipopulation.persistence;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JRootPane;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.Display;
import com.olf.jm.autosipopulation.app.AssignSettlementInstruction;
import com.olf.jm.autosipopulation.gui.ConfirmationDialog;
import com.olf.jm.autosipopulation.gui.SelectDialog;
import com.olf.jm.autosipopulation.gui.SelectDialog.ListMode;
import com.olf.jm.autosipopulation.model.DecisionData;
import com.olf.jm.autosipopulation.model.EnumRunMode;
import com.olf.jm.autosipopulation.model.Pair;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.backoffice.SettlementInstruction;
import com.olf.openrisk.staticdata.Currency;
import com.olf.openrisk.staticdata.DeliveryType;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.Party;
import com.olf.openrisk.staticdata.ReferenceObject;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;
import com.openlink.alertbroker.AlertBroker;

/*
 * History:
 * 2015-05-12	V1.0	jwaechter	- initial version
 * 2015-09-22	V1.1	jwaechter	- now taking into account first answer from user and not asking user a second time	
 *                                    if cancel is pressed
 * 2015-09-29	V1.2	jwaechter	- changed access modifiers
 * 									- removed old event logic for post process mode
 * 2015-10-06	V1.3	jwaechter	- added special handling of case plugin is executed within
 * 									  TPM but as a pre process script: skip GUI, don't block
 *                                    but send out an alert broker message
 * 2015-12-07	V1.4	jwaechter	- now using new API method incorporating identification of
 *                                    external vs. internal settlement instructions
 * 2016-01-10	V1.5	jwaechter	- added drop out to dialogs in case not all necessary info fields are present.
 * 2016-01-30	V1.6	jwaechter	- added clearing of settlement instructions
 * 2016-02-15	V1.7	jwaechter	- defect fix in method "setInternal" to ensure proper handling of cases
 *                                    where no internal settlement instruction is found.
 * 2016-06-08	V1.8	jwaechter	- change in applyLogic to default user input to id in SI-Phys and SI-Phys internal values
 * 2017-05-31	V1.9	jwaechter	- fix> user selected SI ids of 0 are no longer processed
 */

/**
 * Class containing methods to execute logic results to transactions. 
 * The logic will be different depending whether it has access to a GUI (pre process run)
 * or not (post process run). In case of post process runs the actions of type
 * "set value" are the only ones executed. <br/> <br/>
 * 
 * If run in post process mode, the "set value" actions change events,
 * in case if run in pre process mode, they are setting settlement instructions. <br/> <br/>
 * 
 * This class is used as base class for several derived classes. The base class is used to set
 * settlement instructions on tran level and is asking the user to confirm there is no selectable 
 * settlement instruction or to select one SI from a list of necessary. <br/>
 * The derived classes are either writing the settlement instructions somewhere else or do not ask the user.
 * The following classes are derived:
 * {@link LogicResultApplicatorComplexCommPhys}, {@link LogicApplicatorException},  
 * {@link LogicResultApplicatorComplexCommPhysException} and {@link LogicResultApplicatorTranInfoField}
 * @author jwaechter
 * @version 1.9
 */
public class LogicResultApplicator {
	public static enum InternalExternal { 
		INTERNAL(0), EXTERNAL(1);

		private final int id;

		private InternalExternal (int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}
	};

	protected Transaction tran;
	protected final Session session;
	protected Collection<LogicResult> results;

	protected final EnumRunMode rmode;

	protected boolean confirmed=false;

	protected ConfirmationDialog confDialog;

	protected SelectDialog selectDialog;

	public LogicResultApplicator (final Transaction tran, final Session session, 
			final Collection<LogicResult> results, final EnumRunMode rmode) {
		this.tran = tran;
		this.session = session;
		this.results = results;
		this.rmode = rmode;
	}
	
	/**
	 * "Flips" the associated logic results. Flipping means adapting them for a use
	 * on offset deals of the original deal. Offset deals have mirrored internal/external
	 * parties compared to the original deal. Note that only the first invocation
	 * of this method are changing the results.
	 */
	public void flipResults (int tranNum, String offsetTranType, Transaction tran) {
		List<LogicResult> flippedResults = new ArrayList<>();
		for (LogicResult rs : results) {
			flippedResults.add(new LogicResult(rs, tranNum, offsetTranType));
		}
		results = flippedResults;
		this.tran = tran;
	}
	/**
	 * Applies the logic for all logic results that have been assigned to this
	 * LogicApplicator in the constructor.
	 * @return true, in case the logic has been applied correctly, false if not.
	 * False is returned in case the user has cancelled the selection 
	 * of the settlement instruction or did not confirm that there is no
	 * settlement instruction
	 */
	public boolean applyLogic () {
		boolean succeed=true;
		for (LogicResult result : results) {
			if (result.getDd().getSiPhys() != -1 && result.getDd().getSiPhys() != 0) {
				result.setUserSelectedExtStlIns(result.getDd().getSiPhys());				
			}
			if (result.getDd().getSiPhysInternal() != -1 && result.getDd().getSiPhysInternal() != 0) {
				result.setUserSelectedIntStlIns(result.getDd().getSiPhysInternal());				
			}
			if (!succeed) {
				return succeed;
			}
			switch (result.getAction()) {
			case SET_BOTH:
				setBoth(result);
				break;
			case CONFIRM_NO_EXTERNAL_SET_INTERNAL:
				setInternal(result);
				succeed &= confirmNoExternal(result);
				break;
			case CONFIRM_NO_INTERNAL_SET_EXTERNAL:
				setExternal(result);
				succeed &= confirmNoInternal(result);
				break;
			case CONFIRM_BOTH:
				succeed &= confirmBoth(result);
				break;
			case SELECT_USER_EXTERNAL_SET_INTERNAL:
				setInternal(result);
				succeed &= selectExternal(result);
				break;
			case SELECT_USER_INTERNAL_SET_EXTERNAL:
				setExternal(result);
				succeed &= selectInternal(result);
				break;
			case SELECT_USER_BOTH:
				succeed &= selectBoth(result);
				break;				
			case SELECT_USER_EXTERNAL_CONFIRM_INTERNAL:
				//				succeed = confirmNoInternal (result);
				setInternal(result, 0);
				succeed &= selectExternal(result);
				break;
			case SELECT_USER_INTERNAL_CONFIRM_EXTERNAL:
				//				succeed = confirmNoExternal (result);
				setExternal(result, 0);
				succeed &= selectInternal(result);
				break;
			}
		}
		return succeed;
	}

	protected void setInternal (LogicResult result) {
		setInternal (result, result.getIntStlIns());
	}

	/**
	 * Loops through the list of tran level settlement instructions and sets the internal SI 
	 * for those SI that is matching currency, party and delivery type as provided in result	 
	 */
	protected void setInternal (LogicResult result, int intSettleId) {
		int paraSeqNum = result.getDd().getLegNum();
		//		int extPartyId = result.getDd().getExtPartyId();
		int intPartyId = result.getDd().getIntPartyId();
		int ccyId = result.getDd().getCcyId();
		int deliveryTypeId = result.getDd().getDeliveryTypeId();
		String intSettleName = (intSettleId!=0)?session.getStaticDataFactory().getName(EnumReferenceTable.SettleInstructions, intSettleId):"";

		ConstTable sis = tran.getValueAsTable(EnumTransactionFieldId.SettlementTable);
		Table memTable = null;
		try {
			memTable = sis.asTable();
			for (TableRow row : memTable.getRows()) {
				if (settlementMatch (row, ccyId, deliveryTypeId, InternalExternal.INTERNAL)) {
					memTable.setInt("Settle Id", row.getNumber(), intSettleId);
					break;
				}
			}
			Currency c = (Currency)session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.Currency, ccyId);
			Party p = (Party)session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.BusinessUnit, intPartyId);
			DeliveryType d = (DeliveryType)session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.DeliveryType, deliveryTypeId);
			ReferenceObject[] refs = session.getStaticDataFactory().getAllReferenceObjects(EnumReferenceObject.SettlementInstruction);
			SettlementInstruction i = (intSettleId != 0)?
					session.getBackOfficeFactory().retrieveSettlementInstruction(intSettleId):null;
			if (i != null) {
				session.getBackOfficeFactory().setSettlementInstruction(tran, c, p, d, true, i);
			} else {
				session.getBackOfficeFactory().clearSettlementInstruction(tran, c, p, d, true);
			}
		} finally {
			if (memTable != null) {
				memTable.dispose();
				memTable = null;
			}
		}
	}

	protected void setExternal (LogicResult result) {
		setExternal (result, result.getExtStlIns());
	}

	/**
	 * Loops through the list of tran level settlement instructions and sets the external SI 
	 * for those SI that is matching currency, party and delivery type as provided in result	 
	 */ 
	protected void setExternal (LogicResult result, int extSettleId) {
		int paraSeqNum = result.getDd().getLegNum();
		int ccyId = result.getDd().getCcyId();
		int extPartyId = result.getDd().getExtPartyId();
		int deliveryTypeId = result.getDd().getDeliveryTypeId();
		String extSettleName = (extSettleId!=0)?session.getStaticDataFactory().getName(EnumReferenceTable.SettleInstructions, extSettleId):"";

		ConstTable sis = tran.getValueAsTable(EnumTransactionFieldId.SettlementTable);
		Table memTable = null;
		try {
			memTable = sis.asTable();
			for (TableRow row : memTable.getRows()) {
				if (settlementMatch (row, ccyId, deliveryTypeId, InternalExternal.EXTERNAL)) {
					memTable.setInt("Settle Id", row.getNumber(), extSettleId);
					break;
				}
			}
			Currency c = (Currency)session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.Currency, ccyId);
			Party p = (Party)session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.BusinessUnit, extPartyId);
			DeliveryType d = (DeliveryType)session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.DeliveryType, deliveryTypeId);
			ReferenceObject[] refs = session.getStaticDataFactory().getAllReferenceObjects(EnumReferenceObject.SettlementInstruction);
			SettlementInstruction i = (extSettleId != 0)?
					(SettlementInstruction)session.getStaticDataFactory().getReferenceObject(EnumReferenceObject.SettlementInstruction, extSettleId):null;
			if (i != null) {
				session.getBackOfficeFactory().setSettlementInstruction(tran, c, p, d, false, i);
			} else {
				session.getBackOfficeFactory().clearSettlementInstruction(tran, c, p, d, false);
			}			
		} finally {
			if (memTable != null) {
				memTable.dispose();
				memTable = null;
			}
		}
	}

	protected void setBoth (LogicResult result) {
		setInternal(result);
		setExternal(result);
	}

	protected boolean confirmNoInternal (LogicResult result) {
		if (rmode == EnumRunMode.PRE) {
			if (result.getUserSelectedIntStlIns() != -1) {
				setInternal(result, 0);
				return true;
			}
			boolean ret = displayConfirmationMessage (result);
			if (ret) {
				result.setUserSelectedIntStlIns(0);
				setInternal(result);
			}
			return ret;
		} else {
			return false; // in case of post process
		}
	}

	protected boolean confirmNoExternal (LogicResult result) {
		if (rmode == EnumRunMode.PRE) {
			if (result.getUserSelectedExtStlIns() != -1) {
				setExternal(result, 0);
				return true;
			}
			boolean ret = displayConfirmationMessage (result);
			if (ret) {
				result.setUserSelectedExtStlIns(0);
				setExternal (result);
			}
			return ret;
		} else {
			return false; // in case of post process
		}
	}

	protected boolean confirmBoth  (LogicResult result) {
		if (rmode == EnumRunMode.PRE) {
			if (result.getUserSelectedIntStlIns() != -1 && result.getUserSelectedIntStlIns() != -1) {
				setInternal(result, 0);
				setExternal(result, 0);
				return true;
			}

			boolean ret = displayConfirmationMessage (result);
			if (ret) {
				result.setUserSelectedExtStlIns(0);
				result.setUserSelectedIntStlIns(0);
			}
			return ret;
		} else {
			return false; // in case of post process
		}
	}

	protected boolean selectInternal (LogicResult result) {
		if (rmode == EnumRunMode.PRE) {
			if (result.getUserSelectedIntStlIns() != -1 ) {
				setInternal (result, result.getUserSelectedIntStlIns());
				return true;
			}
			// pair containing (SI ID, SI Name) of the user selected internal SI on the left and 
			// (SI ID, SI Name) of the user selected external SI on the right
			Pair<Pair<Integer, String>, Pair<Integer, String>> selection;
			if (session instanceof Context && ((Context)session).getDisplay() != null) {
				selection = displaySelectDialog (result);
				if (selection != null) {
					setInternal (result, selection.getLeft().getLeft());
					result.setUserSelectedIntStlIns(selection.getLeft().getLeft());
					return true;
				} else {
					return false;
				}
			} else {
				AlertBroker.sendAlert(AssignSettlementInstruction.ALERT_BROKER_NO_GUI_MSG_ID, 
						"Could not ask the user to select a settlement instruction: " + 
						result.getMessageToUser());
				return true;
			}
		} else {
			return false; // in case of post process
		}
	}

	protected boolean selectExternal (LogicResult result) {
		if (rmode == EnumRunMode.PRE) {
			if (result.getUserSelectedExtStlIns() != -1 ) {
        				setExternal (result, result.getUserSelectedExtStlIns());
				return true;
			}
			// pair containing (SI ID, SI Name) of the user selected internal SI on the left and 
			// (SI ID, SI Name) of the user selected external SI on the right
			Pair<Pair<Integer, String>, Pair<Integer, String>> selection;
			if (session instanceof Context && ((Context)session).getDisplay() != null) {
				selection = displaySelectDialog (result);
				if (selection != null) {
					setExternal (result, selection.getRight().getLeft());
					result.setUserSelectedExtStlIns(selection.getRight().getLeft());
					return true;			
				} else {
					return false;
				}
			} else {
				AlertBroker.sendAlert(AssignSettlementInstruction.ALERT_BROKER_NO_GUI_MSG_ID, 
						"Could not ask the user to select a settlement instruction: " + 
						result.getMessageToUser());
				return true;
			}
		} else {
			return false; // in case of post process
		}
	}

	protected boolean selectBoth (LogicResult result) {
		if (rmode == EnumRunMode.PRE) {
			if (result.getUserSelectedIntStlIns() != -1 && result.getUserSelectedExtStlIns() != -1) {
				setExternal (result, result.getUserSelectedExtStlIns());
				setInternal (result, result.getUserSelectedIntStlIns());
				return true;
			}
			// pair containing (SI ID, SI Name) of the user selected internal SI on the left and 
			// (SI ID, SI Name) of the user selected external SI on the right
			Pair<Pair<Integer, String>, Pair<Integer, String>> selection;
			if (session instanceof Context && ((Context)session).getDisplay() != null) {
				selection = displaySelectDialog (result);
				if (selection != null) {
					setInternal (result, selection.getLeft().getLeft());		
					setExternal (result, selection.getRight().getLeft());
					result.setUserSelectedIntStlIns(selection.getLeft().getLeft());
					result.setUserSelectedExtStlIns(selection.getRight().getLeft());
					return true;
				} else {
					return false;
				}				
			} else {
				AlertBroker.sendAlert(AssignSettlementInstruction.ALERT_BROKER_NO_GUI_MSG_ID, 
						"Could not ask the user to select a settlement instruction: " + 
						result.getMessageToUser());
				return true;
			}
		} else {
			return false;
		}
	}

	protected boolean displayConfirmationMessage (LogicResult result) {
		Context context = (Context) session;
		final Display display = context.getDisplay();
		if (display == null) {
			AlertBroker.sendAlert(AssignSettlementInstruction.ALERT_BROKER_NO_GUI_MSG_ID, 
					"Could not ask the user for the following confirmation: " + 
					result.getMessageToUser());
			return true;
		}

		confDialog = new ConfirmationDialog(result.getMessageToUser(), display);
		confDialog.setTitle("Settlement Instruction Assignment");

		confDialog.getRootPane().setWindowDecorationStyle(JRootPane.QUESTION_DIALOG);
		confDialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		Dimension frameSize = confDialog.getSize();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int top = (screenSize.height - frameSize.height) / 2;
		int left = (screenSize.width - frameSize.width) / 2;
		confDialog.setLocation(left, top);
		confDialog.setVisible(true);
		confDialog.toFront();
		// Start blocking the normal GUI display
//		if (display != null) {
//			display.block();
//		}
		try {
			while (!confDialog.isFinished()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					throw new RuntimeException (e);
				}			
			};			
		} catch (Throwable t) {
			confDialog.dispose();
			throw t;
		}
		return confDialog.isOk();
	}

	/**
	 * 
	 * @param result
	 * @return Pair of (internal si, external si) with both internal si and external si being a pair
	 * of Settlement instruction Id and Settlement Instruction name. Or null in case user cancelled action
	 */
	protected Pair<Pair<Integer, String>, Pair<Integer, String>> displaySelectDialog (LogicResult result) {
		SelectDialog.ListMode mode;
		switch (result.getAction()) {
		case SELECT_USER_BOTH:
			mode = ListMode.INTEXT;
			break;
		case SELECT_USER_EXTERNAL_SET_INTERNAL:
		case SELECT_USER_EXTERNAL_CONFIRM_INTERNAL:
			mode = ListMode.EXT;
			break;
		case SELECT_USER_INTERNAL_CONFIRM_EXTERNAL:
		case SELECT_USER_INTERNAL_SET_EXTERNAL:
			mode = ListMode.INT;
			break;
		default:
			throw new IllegalArgumentException ("Wrong action " + result.getAction() + " to display select dialog");
		}

		Context context = (Context) session;
		final Display display = context.getDisplay();

		selectDialog = new SelectDialog(result.getMessageToUser(), display, mode, 
				result.getInternalSettleIns(), result.getExternalSettleIns(), 
				new Pair<>(result.getDd().getIntSettleId(), result.getDd().getIntSettleName()),
				new Pair<>(result.getDd().getExtSettleId(), result.getDd().getExtSettleName()));
		selectDialog.setTitle("Settlement Instruction Assignment");

		selectDialog.getRootPane().setWindowDecorationStyle(JRootPane.QUESTION_DIALOG);
		selectDialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		Dimension frameSize = selectDialog.getSize();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int top = (screenSize.height - frameSize.height) / 2;
		int left = (screenSize.width - frameSize.width) / 2;
		selectDialog.setLocation(left, top);
		selectDialog.setVisible(true);
		selectDialog.toFront();

		// Start blocking the normal GUI display
//		if (display != null) {
//			display.block();			
//		}
		try {
			while (!selectDialog.isFinished()) { 
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					throw new RuntimeException (e);
				}
			}
		} catch (Throwable t) {
			selectDialog.dispose();
			throw t;
		}

		Pair<Pair<Integer, String>, Pair<Integer, String>> selection = new Pair<>(selectDialog.getSelectedIntSI(), selectDialog.getSelectedExtSI());
		if (selectDialog.isOk()) {
			return selection;			
		} else {
			return null;
		}
	}

	/**
	 * Checks if a row of the tran level settlement instruction table is matching the provided currency, delivery type and int/ext flag.
	 */
	protected boolean settlementMatch(TableRow row, int ccyId, int deliveryTypeId, InternalExternal intExt) {
		int rowCcyId = row.getInt("Currency");
		int rowDeliveryTypeId = row.getInt("Delivery");
		int rowIntExt = row.getInt("Int/Ext");

		if (ccyId == rowCcyId && deliveryTypeId == rowDeliveryTypeId && rowIntExt == intExt.getId()) {
			return true;
		}
		return false;				
	}
}
