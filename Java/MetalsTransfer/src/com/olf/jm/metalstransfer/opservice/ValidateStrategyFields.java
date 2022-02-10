package com.olf.jm.metalstransfer.opservice;

import java.math.BigDecimal;
import java.util.ArrayList;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openrisk.staticdata.EnumFieldType;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.ReferenceChoices;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;

/**
 * Pre-process ops service will check that key fields have been entered on the metals transfer strategy deal.
 *  
 * @author Gary Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author             | Description                                                                  |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 23-Nov-2015 |               | G. Moore           | Initial version.                                                             |
 * | 002 | 09-Jan-2020 | SR 316284     | Nitesh Vishwakarma | fix                                                                          |
 * | 003 | 21-Jan-2021 | EPI-1546      | Prashanth          | Fix for issues WO0000000015209 - Block if Charges = Yes & "Charges in USD" =0| 
 *                                                                           PBI000000000298 - Block if Strategy Amount precision > 4      |
 *                                                                           PBI000000000306 - Block if metal is not setup on from account |
 * | 004 | 21-Jul-2021 | EPI-1810      | Prashanth          | WO0000000004514 - Block if intermediate accounts/ settlement Instruction     |
 *                                                          | linked to account do not have access to metal                                |
 * | 002 | 13-Dec-2021 | EPI-2000      | RodriR02           | fix for WO0000000134399 - Trade getting booked with prior month              |
 *                                                                           settle date post metal statement run						   |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */

@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class ValidateStrategyFields extends AbstractTradeProcessListener {
private ConstRepository constRep;
	
	/** context of constants repository */
	private static final String CONST_REPO_CONTEXT = "MetalsTransfer";
	
	/** sub context of constants repository */
	private static final String CONST_REPO_SUBCONTEXT = "ValidateStrategyFields"; 
	
	private static final String BLOCK_BUNIT =  "block_bunit";
	
	private static String blockBunitValue;
	
    /** List of tran info fields to check */
    private static ArrayList<String> infoFields = new ArrayList<>();
    static {
        infoFields.add("From A/C Loco");
        infoFields.add("From A/C Form");
        infoFields.add("From A/C");
        infoFields.add("From A/C BU");
        infoFields.add("To A/C Loco");
        infoFields.add("To A/C Form");
        infoFields.add("To A/C");
        infoFields.add("To A/C BU");
        infoFields.add("Metal");
        infoFields.add("Unit");
        infoFields.add("Qty");
    }

    /** List of tran fields to check */
    private static ArrayList<EnumTransactionFieldId> tranFields = new ArrayList<>();
    static {
        tranFields.add(EnumTransactionFieldId.ReferenceString);
        tranFields.add(EnumTransactionFieldId.TradeDate);
        tranFields.add(EnumTransactionFieldId.SettleDate);
        tranFields.add(EnumTransactionFieldId.InternalBusinessUnit);
        tranFields.add(EnumTransactionFieldId.InternalLegalEntity);
        tranFields.add(EnumTransactionFieldId.InternalPortfolio);
    }

    @Override
    public PreProcessResult preProcess(Context context, EnumTranStatus targetStatus, PreProcessingInfo<EnumTranStatus>[] infoArray,
            Table clientData) {
        try{
        	init();
//	        Logging.init(context, this.getClass(), "MetalsTransfer", "UI");
	        for (PreProcessingInfo<EnumTranStatus> info : infoArray) {
	            Transaction tran = info.getTransaction();
	            try {
	                Logging.info("Working with transaction " + tran.getTransactionId());
	                process(context, tran);
	                Logging.info("Completed transaction " + tran.getTransactionId());
	            }
	            catch (RuntimeException e) {
	                Logging.error("Process failed for transaction " + tran.getTransactionId()+ ": ", e);
	                return PreProcessResult.failed("Error during validation. Log files may have more information.\n\n" + e.getLocalizedMessage());
	            }
	        }
	        return PreProcessResult.succeeded();
        }catch (RuntimeException e) {
            Logging.error("Process failed: " , e);
            return PreProcessResult.failed("Error during validation. Log files may have more information.\n\n" + e.getLocalizedMessage());
        }
        finally {
            Logging.close();
        }        
    }

    /**
     * Main processing method.
     * 
     * @param tran Transaction being processed
     * @throws RuntimeException if validation fails
     */
    private void process(Context context, Transaction tran) {
        StringBuilder sb = new StringBuilder();

		for (String infoField : infoFields) {
			EnumFieldType valueDataType = tran.getField(infoField).getDataType();
			//Fix SR 316284 | Transfer Warning For 0 Quantity
			if (valueDataType.equals(EnumFieldType.Double)) {
				double value = tran.getField(infoField).getValueAsDouble();
				if 	(BigDecimal.valueOf(value ).compareTo(BigDecimal.ZERO) == 0)
					{
					sb.append("Field '" + infoField+ "' must be entered and cannot be 'Zero'.\n");
				}

			}else{
			String value = tran.getField(infoField).getValueAsString();
			if (value == null || value.trim().isEmpty()|| "none".equalsIgnoreCase(value)) {
				sb.append("Field '" + infoField+ "' must be entered and cannot be 'None'.\n");
			}
			}
            
        }
        for (EnumTransactionFieldId tranField : tranFields) {
            String value = tran.getValueAsString(tranField);
            if (value == null || value.trim().isEmpty() || "none".equalsIgnoreCase(value)) {
                sb.append("Field '" + tranField.getName() + "' must be entered and cannot be 'None'.\n");
            }
        }
        
        // Validate Transfer charges
        if("Yes".equalsIgnoreCase(tran.getField("Charges").getValueAsString())
        		&& tran.getField("Charge (in USD)").getValueAsDouble() <= 0.0) {
        	sb.append("Field 'Charge (in USD)' must be entered if field 'Charges' is set to Yes.\n");
        }

        // Validate Strategy amount precision
        if(BigDecimal.valueOf(tran.getField("Qty").getValueAsDouble()).scale() > 4) {
        	sb.append("Field 'Qty' must be rounded to 4 decimal places.\n");
        }
        
        // Validate if Metal is setup for Account - When a new strategy deal is booked by copying 
        // existing deal (clear tran num) add check to block trade if metal is not setup on the 
        // account which is checked in Event Notification script
        ReferenceChoices metalRc = tran.getField("Metal").getChoices();
        String metal = tran.getField("Metal").getValueAsString();
        if(metalRc.findChoice(metal) == null){
			sb.append("Metal " + metal + " is not setup on account " + tran.getField("From A/C").getValueAsString()
					+ ". \nPlease select Metal from dropdown list.\n");
        }
        
		// Validate From Account, To Account and intermediary accounts are all configured for the selected metal
		StaticDataFactory sdf = context.getStaticDataFactory();
		String fromLoco = tran.getField("From A/C Loco").getValueAsString();
		String fromForm = tran.getField("From A/C Form").getValueAsString();
		String toLoco = tran.getField("To A/C Loco").getValueAsString();
		String toForm = tran.getField("To A/C Form").getValueAsString();
		int intBunitId = tran.getValueAsInt(EnumTransactionFieldId.InternalBusinessUnit);
		int cur = tran.getField("Metal").getValueAsInt();

		int fromAccId = tran.getField("From A/C").getValueAsInt();
		int toAccId = tran.getField("To A/C").getValueAsInt();
		int intToAccountId = retrieveCashSettleAccountId(context, intBunitId, fromLoco, fromForm);
		int intFromAccountId = retrieveCashSettleAccountId(context, intBunitId, toLoco, toForm);

		if (!isAccountConfiguredForCurrency(context, fromAccId, cur)) {
			sb.append("Metal " + metal + " is not setup for From Aaccount " + getAccName(sdf, fromAccId)
					+ " Or Settlement Instruction " + getSIName(context, sdf, fromAccId) + "\n");
		}

		if (!isAccountConfiguredForCurrency(context, toAccId, cur)) {
			sb.append("Metal " + metal + " is not setup for To Account " + getAccName(sdf, toAccId)
					+ " Or Settlement Instruction " + getSIName(context, sdf, toAccId) + "\n");
		}

		if (!isAccountConfiguredForCurrency(context, intToAccountId, cur)) {
			sb.append("Metal " + metal + " is not setup for PMM account " + getAccName(sdf, intToAccountId)
					+ " Or Settlement Instruction " + getSIName(context, sdf, intToAccountId) + "\n");
		}

		if (!isAccountConfiguredForCurrency(context, intFromAccountId, cur)) {
			sb.append("Metal " + metal + " is not setup for PMM account " + getAccName(sdf, intFromAccountId)
					+ " Or Settlement Instruction " + getSIName(context, sdf, intFromAccountId) + "\n");
		}
        if (!blockBunitValue.equalsIgnoreCase("")) {
        	validateSettleDate(context, tran.getField(EnumTransactionFieldId.SettleDate).getValueAsString(), sb);
        }
		
		if (sb.length() > 0) {
			throw new RuntimeException("Some fields have not been entered.\n\n" + sb.toString());
		}
	}
    
    
	private String getSIName(Context context, StaticDataFactory sdf, int accId) {
		
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT DISTINCT settle_name FROM si_view");
		sql.append("\nWHERE account_id = ").append(accId);

		try (Table si = context.getIOFactory().runSQL(sql.toString())) {
			if (si.getRowCount() <= 0) {
				return "NA";
			} else {
				return si.getString(0, 0);
			}
		}
	}

	private String getAccName(StaticDataFactory sdf, int accId) {

		return sdf.getReferenceObject(EnumReferenceObject.SettlementAccount, accId).getName();
	}

	private boolean isAccountConfiguredForCurrency(Context context, int accId, int cur) {

		StringBuilder sql = new StringBuilder();
		sql.append("SELECT count(account_id) count FROM si_view");
		sql.append("\nWHERE account_id = ").append(accId).append(" AND currency_id = ").append(cur);

		try (Table account = context.getIOFactory().runSQL(sql.toString())) {
			if (account.getInt(0,0) <= 0) {
				return false;
			} else {
				return true;
			}
		}
	}

    
    /**
     * Get the account id associated with the cash settlement instruction for the business unit, loco and form combination. The loco and
     * form are info fields on the account. There should only be one cash account set up. If there are more an exception is thrown.
     * 
     * @param session
     * @param bunitId
     * @param loco
     * @param form
     * @return
     */
    private int retrieveCashSettleAccountId(Context context, int bunitId, String loco, String form) {
    	
    	String accIds = getAccountsForExclusion(context);
    	Logging.info("These accounts will be excluded from the list of accounts"+accIds);
    	StringBuilder sql= new StringBuilder(
                "\n SELECT ac.account_id, ac.account_name" +
                "\n   FROM party_settle ps" +
                "\n   JOIN stl_ins si ON (si.settle_id = ps.settle_id)" +
                "\n   JOIN settle_instructions ss ON (ss.settle_id = ps.settle_id)" +
                "\n   JOIN account ac ON (ac.account_id = ss.account_id)" +
                "\n   JOIN account_info ai1 ON (ai1.account_id = ss.account_id)" +
                "\n   JOIN account_info_type ait1 ON (ait1.type_id = ai1.info_type_id AND ait1.type_name = 'Loco')" +
                "\n   JOIN account_info ai2 ON (ai2.account_id = ss.account_id)" +
                "\n   JOIN account_info_type ait2 ON (ait2.type_id = ai2.info_type_id AND ait2.type_name = 'Form')" +
                "\n  WHERE ps.party_id = " + bunitId +
                "\n    AND si.ins_type = " + EnumInsType.CashInstrument.getValue() +
                "\n    AND ai1.info_value = '" + loco + "'" +
                "\n    AND ai2.info_value = '" + form + "'");
    	if(accIds!=null && !accIds.isEmpty())
		{
			sql.append("\n   AND ac.account_id not in (" + accIds + ")");
		}
    	try(Table account = context.getIOFactory().runSQL(sql.toString()))
                {
            if (account.getRowCount() == 1) {
                return account.getInt(0, 0);
            }
            if (account.getRowCount() > 0) {
            	StringBuilder accountList = new StringBuilder();
            	boolean first = true;
            	for (int row = account.getRowCount()-1; row >= 0; row--) {
            		if (!first) {
            			accountList.append(", ");
            		}
            		accountList.append(account.getString("account_name", row));
            		accountList.append(" (").append(account.getInt("account_id", row)).append(")");            		
            		first = false;
            	}
                throw new RuntimeException("More than one cash settlement account has been found for " +
                		"business unit id " + bunitId + " loco '" + loco + "' and form '" + form + "':" + accountList.toString() );
            }
            throw new RuntimeException("No cash settlement account has been found for business unit id " + 
                    bunitId + " loco '" + loco + "' and form '" + form + "'");
        }
    }
    
	String getAccountsForExclusion(Context context) {
		Table excludeAccTable = null;
		String accountExcluded = "";
		try {
			ConstRepository _constRepo = new ConstRepository("Strategy", "NewTrade");
			excludeAccTable = context.getTableFactory().fromOpenJvs(_constRepo.getMultiStringValue("accountsToExclude"));
			if (excludeAccTable == null || excludeAccTable.getRowCount() == 0) {
				Logging.info("No Accounts were found to be excluded under constRepo for name: accountsToExclude");
				throw new RuntimeException("No Accounts were found to be excluded under constRepo for name: accountsToExclude");
			}
			int rowCount = excludeAccTable.getRowCount();
			for (int rowId = 0; rowId < rowCount; rowId++) {
				String accName = excludeAccTable.getString("value", rowId);
				int accId = context.getStaticDataFactory().getId(EnumReferenceTable.Account, accName);
				accountExcluded = accountExcluded + "," + Integer.toString(accId);
			}
			accountExcluded = accountExcluded.replaceFirst(",", "");
		}

		catch (Exception e) {
			Logging.error("Failed while executing getAccountsForExclusion" + e.getMessage(), e);
			throw new RuntimeException("Failed while executing getAccountsForExclusion");
		}
		return accountExcluded;
	}
	/**
	 * Method to retrieve the latest Month for whichi Metal transfers statement
	 * has been run.
	 * 
	 * Block Backdated SAP transfer if the metal statement is already run for Business unit present in
	 * User_const_reporsitory for context 'SAP' and subcontext 'BackDatedSAPTransfer' 
	 * 
	 * @return Table - containing the Month and Year for which latest metal
	 *         transfer statements has been run.
	 * 
	 */
	private int getLastMetalStmtRunDate(Context context) {
		Table metalStmtRun = null;
		int jdStmtRunDate = 0;
		try {
			Logging.info("\n Block Backdated SAP transfer if the metal statement is already run for Business unit : " + blockBunitValue 
					+". Please check the entry in User_const_reporsitory for context 'SAP' and subcontext 'BackDatedSAPTransfer' ");
			
			String sql =  " SELECT TOP 1 statement_period " 
						+ " FROM USER_jm_monthly_metal_statement"	
						+ " WHERE internal_bunit = " + context.getStaticDataFactory().getId(EnumReferenceTable.Party, blockBunitValue)	
						+ " ORDER BY metal_statement_production_date  DESC";
				
			Logging.debug("Running SQL \n. " + sql);
			metalStmtRun = context.getIOFactory().runSQL(sql);
			if (metalStmtRun.getRowCount() < 1) {
				String message = "\n could not retrieve latest metal statement run date from USER_jm_monthly_metal_statement";
				Logging.error(message);
			}

			String StmtRunDate = metalStmtRun.getString(0, 0);
			jdStmtRunDate = OCalendar.parseString(StmtRunDate);
			Logging.info("\n Latets Metal Statement Run date " + jdStmtRunDate);

		} catch (Exception exp) {
			String message = "Error While loading data from USER_jm_monthly_metal_statement" + exp.getMessage();
			Logging.error(message);
		}
		return jdStmtRunDate;
	}
	/**
	 * Method to check if a transfer is backdated. A transfer is backdated if
	 * valueDate is less than than Approval date and Metal transfers statement
	 * has been run for that month.
	 * 
	 * @param String
	 *            the value Date
	 * @param String
	 *            the Approval Date
	 * @param Table
	 *            table containing the latest month for which Metal statement
	 *            was run
	 * 
	 */
	private void isBackdated(String valueDate, int jdStmtRunDate, StringBuilder sb) throws OException {
		int jdValueDate = OCalendar.parseString(valueDate);
		int valueDateSOM = OCalendar.getSOM(jdValueDate);
		int stmtRunDateSOM = OCalendar.getSOM(jdStmtRunDate);
		if (valueDateSOM <= stmtRunDateSOM) {
			String message = "Settle Date " + valueDate + " will not be permitted \nsince the latest Metal statement with run date " 
					+ OCalendar.formatDateInt(jdStmtRunDate, DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH)
					+ "\nhas already runned for the month \n";
			sb.append(message);
		}

	}
	public void validateSettleDate(Context context, String firstValue, StringBuilder sb) {
		try {
			int metalStmtRun = getLastMetalStmtRunDate(context);
			if (metalStmtRun != 0) {
				isBackdated(firstValue, metalStmtRun, sb);
			}
		} catch (Exception exp) {
			String message = "Error validating backdated transfers " + exp.getMessage();
			Logging.error(message);
		}

	}
	/**
	 * Initialise logging 
	 * 
	 * @throws OException
	 */
	private void init() {
		try {
			constRep = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			Logging.init(this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			blockBunitValue = constRep.getStringValue(BLOCK_BUNIT, "").trim();
		} catch (Exception e) {
			Logging.error("Error initialising logging. " + e.getMessage());
		} 
	}

}
