package com.jm.stamping.adhoc;

import com.olf.openjvs.enums.*;
import com.olf.openjvs.Ask;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

public class LedgerFieldsStampingAdhoc_P implements IScript {

	private Table tDealLegderFields = Util.NULL_TABLE;
	private Table tDocLegderFields = Util.NULL_TABLE;
	private Table tStampingValues = Util.NULL_TABLE;
	
	@Override
	public void execute(IContainerContext context) throws OException {
		int retVal = -1;
		Table tDefaultSel = Util.NULL_TABLE;
		Table tAsk = Util.NULL_TABLE;
		Table tLedgerFields = Util.NULL_TABLE;
		
		try {
			init();
			
			Table argt = context.getArgumentsTable();
			if (Table.isTableValid(argt) != 1) {
				throw new OException("Invalid argt received from context");
			}
			
			tDefaultSel = createDefaultTable();
			tLedgerFields = combineLedgerFields();
			tAsk = createAskPopup(tLedgerFields, tDefaultSel);
			
			retVal = Ask.viewTable(tAsk, "Adhoc Stamping Task", "Please enter the required details for ad-hoc stamping:");
			if (retVal == 0) {
				throw new OException("User clicked cancel while entering details");
			}
			
			String ledgerFieldName = getReturnValueFromAsk(tAsk, 1);
			String stampingValue = getReturnValueFromAsk(tAsk, 2);
			String dealNums = getReturnValueFromAsk(tAsk, 3);
			String docNums = getReturnValueFromAsk(tAsk, 4);
			
			Logging.info(String.format("Input values: Ledger_Field_Name->%s, Stamping_Value->%s, Deal_Nums->%s, Doc_Nums->%s", ledgerFieldName, stampingValue, dealNums, docNums));
			constructArgt(argt);
			argt.setString(Constants.LEDGER_FIELD_NAME_IDENTIFIER, 1, ledgerFieldName);
			argt.setString(Constants.LEDGER_FIELD_VALUE_INDETIFIER, 1, stampingValue);
			
			if (isDealLedgerFieldSelected(ledgerFieldName)) {
				if (dealNums.length() == 0) {
					throw new OException("Tran No.(s) field value found empty.");
				}
				argt.setString(Constants.DEAL_NUMS_INDETIFIER, 1, dealNums);
				argt.setString(Constants.DOC_NUMS_INDETIFIER, 1, "");
				
			} else if (isDocLedgerFieldSelected(ledgerFieldName)) {
				if (docNums.length() == 0) {
					throw new OException("Document No.(s) field value found empty.");
				}
				argt.setString(Constants.DEAL_NUMS_INDETIFIER, 1, "");
				argt.setString(Constants.DOC_NUMS_INDETIFIER, 1, docNums);
			}
			
		} catch (Exception e) {
			String message = String.format("Error occurred in param script: %s", e.getMessage());
			Logging.error(message);
			throw new OException(message);
			
		} finally {
			cleanup();
			
			if (Table.isTableValid(tAsk) == 1) {
				tAsk.destroy();
			}
			
			if (Table.isTableValid(tDefaultSel) == 1) {
				tDefaultSel.destroy();
			}
			
			if (Table.isTableValid(tLedgerFields) == 1) {
				tLedgerFields.destroy();
			}
		}
	}
	
	private void cleanup() throws OException {
		if (Table.isTableValid(this.tDealLegderFields) == 1) {
			this.tDealLegderFields.destroy();
		}
		
		if (Table.isTableValid(this.tDocLegderFields) == 1) {
			this.tDocLegderFields.destroy();
		}
		
		if (Table.isTableValid(this.tStampingValues) == 1) {
			this.tStampingValues.destroy();
		}
	}
	
	/**
	 * To determine if deal level field is selected like General Ledger / Metal Ledger etc. from the list for Ledger Field.
	 * 
	 * @param selValue
	 * @return
	 * @throws OException
	 */
	private boolean isDealLedgerFieldSelected(String selValue) throws OException {
		int findRow = this.tDealLegderFields.findString(1, selValue, SEARCH_ENUM.FIRST_IN_GROUP);
		return (findRow > -1);
	}
	
	/**
	 * To determine if document level field is selected like Sales Ledger etc. from the list for Ledger Field.
	 * 
	 * @param selValue
	 * @return
	 * @throws OException
	 */
	private boolean isDocLedgerFieldSelected(String selValue) throws OException {
		int findRow = this.tDocLegderFields.findString(1, selValue, SEARCH_ENUM.FIRST_IN_GROUP);
		return (findRow > -1);
	}
	
	private String getReturnValueFromAsk(Table tAsk, int row) throws OException {
		return tAsk.getTable("return_value", row).getString("return_value", 1);
	}
	
	private Table createDefaultTable() throws OException {
		Table tDefaultSel = this.tDealLegderFields.cloneTable();
		tDefaultSel.addRow();
		tDefaultSel.setString("value", 1, "-- Please select --");
		return tDefaultSel;
	}
	
	/**
	 * Combine dealLedgerFields & docLedgerFields in to a new table to display as part of Ledger Field.
	 * 
	 * @return
	 * @throws OException
	 */
	private Table combineLedgerFields() throws OException {
		Table tLedgerFields = this.tDealLegderFields.cloneTable();
		this.tDealLegderFields.copyRowAddAll(tLedgerFields);
		this.tDocLegderFields.copyRowAddAll(tLedgerFields);
		return tLedgerFields;
	}
	
	/**
	 * Fields that are to be displayed on Pop-up for user input.
	 * 
	 * @param tLedgerFields
	 * @param tDefaultSel
	 * @return
	 * @throws OException
	 */
	private Table createAskPopup(Table tLedgerFields, Table tDefaultSel) throws OException {
		Table tAsk = Table.tableNew();
		Ask.setAvsTable(tAsk, tLedgerFields, "Ledger Field", 1, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(), 1, tDefaultSel, "Please select required Ledger field", 1);
		Ask.setAvsTable(tAsk, this.tStampingValues, "Stamping Value", 1, ASK_SELECT_TYPES.ASK_SINGLE_SELECT.toInt(), 1, tDefaultSel, "Please select required Stamping value", 1);
		Ask.setTextEdit(tAsk, "Tran No.(s)", "", ASK_TEXT_DATA_TYPES.ASK_STRING, "Please input comma-separated list of Tran Numbers", 0);
		Ask.setTextEdit(tAsk, "Document No.(s)", "", ASK_TEXT_DATA_TYPES.ASK_STRING, "Please input comma-separated list of Document Numbers", 0);
		
		return tAsk;
	}
	
	private void constructArgt(Table argt) throws OException {
		argt.addCol(Constants.LEDGER_FIELD_NAME_IDENTIFIER, COL_TYPE_ENUM.COL_STRING);
		argt.addCol(Constants.LEDGER_FIELD_VALUE_INDETIFIER, COL_TYPE_ENUM.COL_STRING);
		argt.addCol(Constants.DEAL_NUMS_INDETIFIER, COL_TYPE_ENUM.COL_STRING);
		argt.addCol(Constants.DOC_NUMS_INDETIFIER, COL_TYPE_ENUM.COL_STRING);
		argt.addRow();
	}
	
	private boolean isInValidTable(Table inputTbl) throws OException {
		return (Table.isTableValid(inputTbl) != 1 || inputTbl.getNumRows() == 0) ? true : false;
	}
	
	protected void init() throws Exception {
		ConstRepository constRepo = new ConstRepository(Constants.CONST_REPO_CONTEXT, Constants.CONST_REPO_SUB_CONTEXT);
		initialiseLogger(constRepo);
		
		this.tDealLegderFields = constRepo.getMultiStringValue(Constants.DEAL_LEDGER_FIELD_NAME_IDENTIFIER);
		this.tDocLegderFields = constRepo.getMultiStringValue(Constants.DOC_LEDGER_FIELD_NAME_IDENTIFIER);
		this.tStampingValues = constRepo.getMultiStringValue(Constants.LEDGER_FIELD_VALUE_INDETIFIER);
		
		if (isInValidTable(this.tDealLegderFields)) {
			throw new RuntimeException("No deal level ledger fields config found in user_const_repository");
		}
		
		if (isInValidTable(this.tDocLegderFields)) {
			throw new RuntimeException("No document level ledger fields config found in user_const_repository");
		}
		
		if (isInValidTable(this.tStampingValues)) {
			throw new RuntimeException("No stamping values config found in user_const_repository");
		}
		
		this.tDealLegderFields.sortCol("value");
		this.tDocLegderFields.sortCol("value");
	}
	
	/**
     * Initialise PluginLog by retrieving log settings from ConstRepository.
     *
     * @param context the context
     */
    protected void initialiseLogger(ConstRepository constRepo) {
    	String logLevel = "INFO"; 
		String logFile  = this.getClass().getSimpleName() + ".log"; 
		String logDir   = null;
		
        try {
            String abOutdir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";
            
            logLevel = constRepo.getStringValue("logLevel", logLevel); 
            logFile = constRepo.getStringValue("logFile", logFile);
            logDir = constRepo.getStringValue("logDir", abOutdir);

            Logging.init(this.getClass(), "", "");

        } catch (Exception ex) {
        	String msg = "Failed to initialise log file: " + logDir + "\\" + logFile;
        	Logging.error(msg);
            throw new RuntimeException(msg, ex);
        }       
    }

}
