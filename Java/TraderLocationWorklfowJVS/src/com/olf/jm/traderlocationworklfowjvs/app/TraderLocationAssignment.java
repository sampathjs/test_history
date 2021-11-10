package com.olf.jm.traderlocationworklfowjvs.app;

import com.olf.jm.logging.Logging;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.misc.TableUtilities;

/*
 * History:
 * 2015-09-21	V1.0	jwaechter	- Initial Version
 */



/**
 * OPS Trading plugin to be used to set the "Loco" and "Loco Offset" tran info fields to "Royston" in case of pass through deals.
 * @author jwaechter 
 * @version 1.0
 * 2021-10-26	V1.1     BhardG01	- WO0000000007327_Location Pass through deals failed
 *
 */
public class TraderLocationAssignment implements IScript
{
	private static final String LOCO_ROYSTON = "Royston";
	private static final String CREPO_CONTEXT = "FO";
	private static final String CREPO_SUBCONTEXT = "TraderLocationWorkflow";

	/**
	 * Table name of the user table containing information about the location info field values.
	 */
	private static final String USER_JM_PTE_PTO_FORM_LOCO_TABLE = "USER_jm_pte_pto_form_loco";

	private static String origLoco="";
	private static String origForm="";

	public void execute(IContainerContext context) throws OException {
		try {
			initLogging();
			process();
		} catch (Throwable t) {
			Logging.info("Error executing " + this.getClass().getCanonicalName() + " : \n" + t.toString());
			OpService.serviceFail(t.toString(), 0);
			throw t;
		} finally {
			Logging.close();
		}   	
	}

	private void process() throws OException {
		for (int i = OpService.retrieveNumTrans(); i >= 1;i--) {
			Transaction origTran = OpService.retrieveOriginalTran(i); 
//			Transaction tran = OpService.retrieveTran(i);
			String offsetTranType = origTran.getField(TRANF_FIELD.TRANF_OFFSET_TRAN_TYPE.toInt());
			if (offsetTranType == null) {
				continue;
			}
			
			if (isPTE(offsetTranType)) {
				String form = origTran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Form");
				String loco = origTran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Loco");
				origLoco = loco;
				origForm = form;
			} else if (isPTI(offsetTranType) || isPTO (offsetTranType)) {			
				if (origLoco != null && origForm != null && !origLoco.equals("") && !origForm.equals("") ) {
					updatePtiPto(origTran, origLoco, origForm);
				} else {
					OpService.serviceFail("Could not retrieve Loco and Form from PTE deal " +
							"or Loco and Form are not set in PTE deal", 0);
				}
			}				
			else if (isPassthroughDeal(origTran) ) {
				
				
				String form = origTran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Form");
				String loco = origTran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Loco");
				origLoco = loco;
				origForm = form;
				if (origLoco != null && origForm != null && !origLoco.equals("") && !origForm.equals("") ) {
					updatePtiPto(origTran, origLoco, origForm);
				} else {
					OpService.serviceFail("Could not retrieve Loco and Form from PTE deal " +
							"or Loco and Form are not set in PTE deal", 0);
				}
			}				
		}
	}


	private boolean isPassthroughDeal(Transaction origTran) throws OException {
		String offsetTranType = origTran.getField(TRANF_FIELD.TRANF_OFFSET_TRAN_TYPE.toInt());
	 
		String passthroughBU =origTran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "PassThrough Unit") ;
		String passthroughLegal =origTran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "PassThrough Legal") ;
		String passthroughPF =origTran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "PassThrough pfolio") ; 
		if (offsetTranType.equalsIgnoreCase("No Offset") && passthroughBU != null && passthroughPF != null  && passthroughLegal != null &&  !passthroughBU.equals("") && !passthroughPF.equals("")
				 && !passthroughLegal.equals("") ){
			return true;
		}
		 
		return false;
	}

	private void updatePtiPto(Transaction origTran, String loco, String form) throws OException {
		String sql = generateLocoRetrievalSqlForLoco(loco, form);
		Table formAndLoco = null;
		
		try {
			formAndLoco = Table.tableNew("Form and Loco for " + form + " and " + loco);
			int ret = DBaseTable.execISql(formAndLoco, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				String message = DBUserTable.dbRetrieveErrorInfo(ret, "\nError executing SQL " + sql + "\n");
				throw new OException (message);
			}
			if (formAndLoco.getNumRows() == 0) {
				throw new OException ("Could not locate form and loco for '" + form + "' and '" + loco + 
						"' in user table " + USER_JM_PTE_PTO_FORM_LOCO_TABLE);
			}

			if (formAndLoco.getNumRows() > 1) {
				throw new OException ("More than one form and loco for '" + form + "' and '" + loco + 
						"' in user table " + USER_JM_PTE_PTO_FORM_LOCO_TABLE);
			}
			String newForm = formAndLoco.getString("form_on_pti_pto", 1);
			String newLoco = formAndLoco.getString("loco_on_pti_pto", 1);
			
			ret = origTran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Form", newForm);
			if (ret == 0) {
				throw new OException ("Can't populate the Form field on tran #" + origTran.getTranNum() + 
						" with value '" + newForm + "'");
			}
			ret = origTran.setField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, "Loco", newLoco);
			if (ret == 0) {
				throw new OException ("Can't populate the Loco field on tran #" + origTran.getTranNum() + 
						" with value '" + newLoco + "'");
			}			
		} finally {
			formAndLoco = TableUtilities.destroy(formAndLoco);
		}
	}

	private boolean isPTE(String offsetTranType) {
		return "Pass Thru External".equals(offsetTranType);
	}

	private boolean isPTI(String offsetTranType) {
		return "Pass Thru Internal".equals(offsetTranType);
	}

	private boolean isPTO(String offsetTranType) {
		return "Pass Thru Offset".equals(offsetTranType) || "Pass Through Party".equals(offsetTranType);
	}
	
	
	private String generateLocoRetrievalSqlForLoco(String locoName, String formName) {
		StringBuilder sb = new StringBuilder ();
		sb.append("\nSELECT ul.loco_on_pte, ul.form_on_pte, ul.form_on_pti_pto, ul.loco_on_pti_pto");
		sb.append("\nFROM ").append(USER_JM_PTE_PTO_FORM_LOCO_TABLE).append(" ul");
		sb.append("\nWHERE ul.loco_on_pte = '").append(locoName).append("' AND ul.form_on_pte = '");
		sb.append(formName).append("'");
		return sb.toString();
	}


	/**
	 * Initialise logging module.
	 * 
	 * @throws OException
	 */
	private void initLogging() throws OException {
		try {
			Logging.init(getClass(), CREPO_CONTEXT, CREPO_CONTEXT);
		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName()
					+ ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
		}
	}

}