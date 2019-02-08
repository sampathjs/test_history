package com.jm.migration;

/*
 * History:
 * 1.0 - 09.03.2018 scurran  	- initial version based on Standard Content implementation
 */

/**
 * Creates and Inserts Vostro-Nostro and Nostro-Vostro CASH Transfers .
 * 
 * @author scurran
 * @version 1.0
 * 
 * 
 */

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openjvs.enums.TABLE_SORT_DIR_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.esp.migration.persistence.ApplicationScript;
import com.openlink.esp.migration.persistence.Migration;
import com.openlink.esp.migration.persistence.Statics;
import com.openlink.esp.migration.persistence.UserTableRepository;
import com.openlink.esp.migration.persistence.log.enums.MigrError;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
import com.olf.openjvs.enums.TRANF_FIELD;

public class Migr_VN_NV_CashTransfers extends ApplicationScript implements IScript {

	//Constants Repository Statics
	public final static String MIGRATION = "Migration";
	public final static String MIGRATIONSUBCONTEXT 	= "migr_cash_transfer"; 
	
	
	private String sourceDataUserTable;
	
	private String bookToStatus;

	@Override
	public void execute(IContainerContext arg0) throws OException {
		
		initLogging();
		
		PluginLog.info("Loading Configuration data.");
		loadConfigData();
		
		Table sourceData = null;
		
		try {
			PluginLog.info("Loading deal data.");
			sourceData = loadSourceData();
			
			process(sourceData);
			
		} catch (Exception e) {
			PluginLog.error("Error migrating deal " + e.getMessage());
			throw e;
		} finally {
			if(sourceData != null) {
				sourceData.destroy();
			}
		}	
	}
	
	
	private void process(Table sourceData) throws OException {
		for(int row = 1; row <= sourceData.getNumRows(); row++) {
			Table mappingData = null;
			Transaction transfer = null;
			Table dealToProcess = null;
			try {
				String dealGroup = sourceData.getString("deal_group", row);
				mappingData = loadMappingData(dealGroup);
				
				PluginLog.info("Loading mapping data for deal group " + dealGroup);
				validateMappingData(sourceData, mappingData);
			
				transfer = loadTemplate(sourceData.getString("template", row));
				
				if(transfer == null) {
					throw new OException("Error loading template " + sourceData.getString("template", row));
				}
				
				int rowId = sourceData.getInt("row_id", row);
				
				dealToProcess = Table.tableNew(sourceDataUserTable);
				dealToProcess.select(sourceData, "*", "row_id EQ " + rowId);
				
				applyFieldMappings(dealToProcess, mappingData, transfer);
				
				transfer.insertByStatus(Migration.getParsedToTranStatus(bookToStatus));
				
				PluginLog.info("Booked deal tran num " + transfer.getTranNum() + " deal tracking num " + transfer.getFieldInt(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt()));
				
				String statusMessage = "";
				String statusFlag = "BOOKED";	
				int rowType = 7;
				
				dealToProcess.setString("status_msg", 1, statusMessage);
				dealToProcess.setString("status_flag", 1, statusFlag);
				dealToProcess.setInt("row_type", 1, rowType);
				dealToProcess.setInt("booked_deal_num", 1, transfer.getFieldInt(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt()));
				
				sourceData.setString("status_msg", row, statusMessage);
				sourceData.setString("status_flag", row, statusFlag);
				sourceData.setInt("row_type", row, rowType);
				sourceData.setInt("booked_deal_num", row, transfer.getFieldInt(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt()));
				
				UserTableRepository.update(dealToProcess, "row_id");
				
			} catch (Exception e) {
				PluginLog.error("Error processing deal " + e.getLocalizedMessage());
				
				String statusMessage = "Error Inserting deal: " + e.getMessage();
				String statusFlag = "BOOKING_FAILED";
				
				dealToProcess.setString("status_msg", 1, statusMessage.substring(0, statusMessage.length() < 255 ? statusMessage.length() : 255 ));
				dealToProcess.setString("status_flag", 1, statusFlag);
				sourceData.setString("status_msg", row, statusMessage);
				sourceData.setString("status_flag", row, statusFlag);
				
				UserTableRepository.update(dealToProcess, "row_id");

			} finally {
				if(mappingData != null) {
					mappingData.destroy();
					mappingData = null;
				}	
				
				if(transfer != null) {
					transfer.destroy();
					transfer = null;
				}
				
				if(dealToProcess != null) {
					dealToProcess.destroy();
					dealToProcess = null;
				}
			}
		}
	}
	
	

	private void applyFieldMappings(Table sourceData, Table mappingData, Transaction transfer) throws OException {
		for(int row = 1; row <= mappingData.getNumRows(); row++) {
			int field = mappingData.getInt(Statics.TBL_OP_TRANF_TO_DEALGRP_TRANF_FIELD, row);
			String name = mappingData.getString(Statics.TBL_OP_TRANF_TO_DEALGRP_TRANF_NAME, row);
			int side = mappingData.getInt(Statics.TBL_OP_TRANF_TO_DEALGRP_SEQ_NUM, 1);
		
			int seq2 = mappingData.getInt(Statics.TBL_OP_TRANF_TO_DEALGRP_SEQ_NUM2, row);
			int seq3 = mappingData.getInt(Statics.TBL_OP_TRANF_TO_DEALGRP_SEQ_NUM3, row);
			int seq4 = mappingData.getInt(Statics.TBL_OP_TRANF_TO_DEALGRP_SEQ_NUM4, row);
			int seq5 = mappingData.getInt(Statics.TBL_OP_TRANF_TO_DEALGRP_SEQ_NUM5, row);
			
			String value = sourceData.getString(mappingData.getString(Statics.TBL_OP_TRANF_TO_DEALGRP_FIELD, row), 1);
			
			applyTranFieldToTransaction(transfer, field, side, seq2, seq3, seq4, seq5, name, value);
		}
	}
	

	private void applyTranFieldToTransaction(Transaction transfer, int field,int side,int seq2,int seq3,int seq4,int seq5,String name,String value)
			throws OException {
		int status;

		PluginLog.debug("set field " + TRANF_FIELD.fromInt(field).name() + "("+ field + ")/" + name + " side " + side + " to value " + value);
		status = transfer.setField(field, side, name, value, seq2, seq3, seq4, seq5);
	
		/* 2 means successful, but value did not change */
		if (status != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt() && status != 2) {
			String errorMessage = "Error setting field " +TRANF_FIELD.fromInt(field).name();
			PluginLog.error(errorMessage);
			throw new OException(errorMessage);
		}
	}

	private Transaction loadTemplate(String template) throws OException {
		String sql = 
				"SELECT tran_num " +
				"FROM ab_tran " +
				"WHERE reference = '" + template + "' " +
				" 	AND tran_status = 15 " +
				"	AND current_flag = 1 " +
				" ORDER BY tran_num DESC";
		
		PluginLog.debug("About to run SQL " + sql);
		
		Table templateData = Table.tableNew();
		int returnCode = DBaseTable.execISql(templateData, sql);
		
		if (returnCode != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
			templateData.destroy();
			String errorMessage = "Error executing sql statment " + sql;
			PluginLog.error(errorMessage);
			throw new OException(errorMessage);		
		}		

		if(templateData.getNumRows() < 1) {
			templateData.destroy();
			String errorMessage = "Error loading data for template " + template + " no data found";
			PluginLog.error(errorMessage);
			throw new OException(errorMessage);				
		}
		
		Transaction transfer = Transaction.retrieveCopy(templateData.getInt("tran_num", 1));
		
		return transfer;
	}


	/**
	 * Validate mapping data ensuring all columns are in the source data.
	 *
	 * @param sourceData the source data
	 * @param mappingData the mapping data
	 * @throws OException 
	 */
	private void validateMappingData(Table sourceData, Table mappingData) throws OException {
		boolean missingMappindData = false;
		StringBuffer missingFields = new StringBuffer();
		
		missingFields.append("The folling field are missing from the source data ");
		for(int row = 1; row <= mappingData.getNumRows(); row++) {
			String fieldName = mappingData.getString("field", row);
			
			int colNumber = sourceData.getColNum(fieldName);
			
			if(colNumber < 1) {
				if(!missingMappindData) {
					missingFields.append(fieldName);
				} else {
					missingFields.append(", ").append(fieldName);
				}
				
				missingMappindData = true;
				String errorMessage = "Mapping column " + fieldName + " is missing from the source data in table " + sourceDataUserTable;
				PluginLog.error(errorMessage);

			}
		}
		
		if(missingMappindData) {
			PluginLog.error(missingFields.toString());
			throw new OException(missingFields.toString());
		}
		
	}

	/**
	 * Load mapping data used to map columns in the source data to TRANF fields.
	 */
	private Table loadMappingData(String dealGroup) throws OException {
		String sql = "SELECT * FROM USER_migr_op_tranf_to_dealgrp  WHERE deal_group  IN (0, " + dealGroup + ") ORDER BY sort_order ASC";
		
		PluginLog.debug("About to run SQL " + sql);
		
		Table mappingData = Table.tableNew();
		int returnCode = DBaseTable.execISql(mappingData, sql);
		
		if (returnCode != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
			mappingData.destroy();
			String errorMessage = "Error executing sql statment " + sql;
			PluginLog.error(errorMessage);
			throw new OException(errorMessage);		
		}		
		
		
		return mappingData;
	}

	/**
	 * Load the source data used in the deal booking 
	 */
	private Table loadSourceData() throws OException {
		String sql = "SELECT * FROM " + sourceDataUserTable + " WHERE row_type = 2";
		
		PluginLog.debug("About to run SQL " + sql);
		
		Table sourceData = Table.tableNew();
		int returnCode = DBaseTable.execISql(sourceData, sql);
		
		if (returnCode != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
			sourceData.destroy();
			String errorMessage = "Error executing sql statment " + sql;
			PluginLog.error(errorMessage);
			throw new OException(errorMessage);		
		}		
		
		sourceData.sortCol("row_id", TABLE_SORT_DIR_ENUM.TABLE_SORT_DIR_ASCENDING);
		return sourceData;
	}


	private void initLogging() throws OException {
		try {
			PluginLog.init(logLevel, logDir, this.getClass().getSimpleName().replace("_Plugin", "") + ".log");
		} catch (Exception e) {
			OConsole.oprint(this.getClass().getSimpleName() + ": Failed to initialize logging module.");
		}
	}
	

	private void loadConfigData() throws OException {
		try {
			ConstRepository constRep = new ConstRepository(MIGRATION, MIGRATIONSUBCONTEXT);
			
			sourceDataUserTable = "USER_migr_deals_tf_type1";
			
			sourceDataUserTable = constRep.getStringValue("src_data_table", sourceDataUserTable);

			bookToStatus = getEnvVariable("MIGR_TRAN_STATUS_CREATE");
			
			if(bookToStatus == null || bookToStatus.length() == 0) {
				String errorMessage = "MIGR_TRAN_STATUS_CREATE is not defined or empty";
				PluginLog.error(errorMessage);
				throw new OException(errorMessage);				
			}
					
			PluginLog.debug("Using config data src_data_table[" + sourceDataUserTable + "] MIGR_TRAN_STATUS_CREATE [" + bookToStatus +"]");
			
		} catch (Exception e) {
			String errorMessage = "Error loading the configuration data. " + e.getLocalizedMessage();
			PluginLog.error(errorMessage);
			throw new OException(errorMessage);
		}
	}
	
	/**
	 * Get global variable by name.
	 * 
	 * @param var
	 *            Name of the variable. Null if the variable could not be found.
	 * @return
	 */
	public static String getEnvVariable(String var) {

		String val = null;
		Table sysConfTbl = null;
		try {
			sysConfTbl = UserTableRepository.retrieveTableByName(Statics.TBL_OP_CONFIG);

			if (sysConfTbl != null && sysConfTbl.getNumRows() > 0) {
				sysConfTbl.sortCol(Statics.TBL_OP_CONFIG_SYS_VAR);
				int varRowNum = sysConfTbl.findString(Statics.TBL_OP_CONFIG_SYS_VAR, var, SEARCH_ENUM.FIRST_IN_GROUP);
				if (varRowNum > 0) {
					val = sysConfTbl.getString(Statics.TBL_OP_CONFIG_SYS_VAL, varRowNum).trim();
				}
			}
			PluginLog.debug("Loaded variable " + var + " value " + val);
		} catch (OException e) {
			PluginLog.error("Error loading variable " + var + " from the config table");
		} finally {
			try {
				if (sysConfTbl != null) {
					sysConfTbl.destroy();
				}
			} catch (OException e) {
				PluginLog.warn("Could not destroy SysConf table.");
			}
		}
		return val;
	}

}
