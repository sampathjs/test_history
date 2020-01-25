package com.olf.jm.metalstransfer.tpm;

import java.util.ArrayList;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.tpm.AbstractProcessStep;
import com.olf.jm.logging.Logging;
import com.olf.jm.metalstransfer.dealbooking.CashTransfer;
import com.olf.openjvs.OException;
import com.olf.openjvs.Tpm;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.DatabaseTable;
import com.olf.openrisk.staticdata.BusinessUnit;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.Token;
import com.olf.openrisk.tpm.Variables;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.misc.TableUtilities;

/**
 * Cash Transfer deal booking. Figures out from a metals transfer strategy deal what cash transfer deals need to be booked.
 *  
 * @author Gary Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 22-Oct-2015 |               | G. Moore        | Initial version.                                                                |
 * | 002 | 02-Dec-2015 |               | G. Moore        | No such thing as a 'Do Not Validate' deal anymore so code removed.              |
 * | 003 | 12-Apr-2016 |               | J.Waechter	     | Added revalidation of deals													   |
 * | 004 | 21-Apr-2016 |			   | J.Waechter      | Moved validation of strategy to end of CashTransferTaxBooking				   |
 * | 005 | 26-Apr-2016 |			   | J.Waechter      | Cancelling deals at the beginning									  		   |
 * | 006 | 20-May-2016 |               | J.Waechter      | Bugfixes in bookRuleBasedCashTransfer method and now only using                 |
 * |     |             |               |                 | rule based logic in case of strategies not booked with JM PMM UK                |
 * | 007 | 23-May-2016 |               | J. Waechter     | Enhanced logging                                                                |
 * | 008 | 25-Jan-2017 |               | J. Waechter     | Added reset of counter if necessary	
 * | 009 | 26-Sep-2019 |			   | Pramod Garg     | Fix for US to UK gold and silver transfer to be booked, 
 * |	
 * | 010 | 25-Jan-2020 |		        | Naveen Gupta   |Excluded Gains&Losses Accounts from account list while determining intermediate account            |									   |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.TpmStep })
public class CashTransferDealBooking extends AbstractProcessStep {

    /** Stored PMM loco's */
    private ArrayList<String> pmmLoco = new ArrayList<>();
    private String strategyRef;

    @Override
    public Table execute(Context context, Process process, Token token, Person submitter, boolean transferItemLocks, Variables variables) {
        int tranNum = process.getVariable("TranNum").getValueAsInt();
        try {
        	long wflowId = Tpm.getWorkflowId();
        	String count = getVariable(wflowId, "CheckBookMetalTransfersCount");
        	int countAsInt = Integer.parseInt(count);
        	String countTax = getVariable(wflowId, "CheckBookTaxDealCount");
        	int countTaxAsInt = Integer.parseInt(countTax);
        	String maxRetryCount = getVariable(wflowId, "MaxRetryCount");
        	int maxRetryCountAsInt = Integer.parseInt(maxRetryCount);
            Logging.init(context, this.getClass(), "MetalsTransfer", "UI");
            Logging.info("Processing transaction " + tranNum);
            if (countAsInt > maxRetryCountAsInt) {
       		    Tpm.setVariable(wflowId, "CheckBookMetalTransfersCount", "" + Integer.toString(countTaxAsInt+1));            	
            } else {
       		    Tpm.setVariable(wflowId, "CheckBookMetalTransfersCount", "" + Integer.toString(countAsInt+1));            	
            }

            Table returnt = process(context, process, tranNum);
            Logging.info("Completed transaction " + tranNum);
    		Tpm.setVariable(wflowId, "CheckBookMetalTransfersCount", "" + Integer.toString(9999999));
            return returnt;
        }
        catch (OException ex) {
            Logging.error("Process failed for transaction " + tranNum + ": ", ex);
            throw new RuntimeException (ex);
        }
        catch (RuntimeException e) {
            Logging.error("Process failed for transaction " + tranNum + ": ", e);
            throw e;
        }
        finally {
            Logging.close();
        }
    }

	private String getVariable(final long wflowId, final String toLookFor) throws OException {
		com.olf.openjvs.Table varsAsTable=null;
		try {
			varsAsTable = Tpm.getVariables(wflowId);
			com.olf.openjvs.Table varSub = varsAsTable.getTable("variable", 1);			
			for (int row=varSub.getNumRows(); row >= 1; row--) {
				String name  = varSub.getString("name", row).trim();				
				String value  = varSub.getString("value", row).trim();
				if (toLookFor.equals(name)) {
					return value;
				}
			}
		} finally {
			varsAsTable = TableUtilities.destroy(varsAsTable);
		}
		return "";
	}
    
    /**
     * Main process.
     * 
     * @param context
     * @param process
     * @param tranNum
     * @return table
     */
    private Table process(Context context, Process process, int tranNum) {

        init(context);
        
        TradingFactory factory = context.getTradingFactory();

        try (Transaction strategy = factory.retrieveTransactionById(tranNum)) {

            strategyRef = strategy.getValueAsString(EnumTransactionFieldId.ReferenceString);
            
            Logging.info("Strategy " + strategyRef + ": Booking cash transfer deals");

            // Get the from and to account fields from the strategy deal

            String fromLoco = strategy.getField("From A/C Loco").getValueAsString();
            String fromForm = strategy.getField("From A/C Form").getValueAsString();
            boolean isFromLocoPmm = pmmLoco.contains(fromLoco);

            String toLoco = strategy.getField("To A/C Loco").getValueAsString();
            String toForm = strategy.getField("To A/C Form").getValueAsString();
            String intBunit = strategy.getValueAsString(EnumTransactionFieldId.InternalBusinessUnit);
            boolean isToLocoPmm = pmmLoco.contains(toLoco);
            
            CashTransfer.cancelDeals(context, strategy);
            

            // Determine if the cash transfers will be based on a rule
            try (Table rule = getRule(context, fromLoco, toLoco)) {
                if (rule.getRowCount() == 1 && !"JM PMM UK".equals(intBunit)) {
                    // Transfer is being made between Valley Forge and non-PMM location
                	Logging.info("Transfer between Valley Forge and non-PMM location");
                    bookRuleBasedCashTransfers(context, strategy, rule, fromLoco, fromForm, toLoco, toForm);
                }
                else if (rule.getRowCount() > 1 && !"JM PMM UK".equals(intBunit)) {
                	StringBuilder sb = new StringBuilder ();
                	for (int row = rule.getRowCount()-1; row >= 0; row--) {
                		sb.append ("\n");
                		for (int col = 0; col < rule.getColumnCount(); col++) {
                			sb.append (rule.getColumnName(col)).append("=");
                			sb.append (rule.getValueAt(row, col).toString()).append(";");
                		}
                	}
                    throw new RuntimeException("Strategy " + strategyRef + ": There is more than one rule defined in the table " +
                    		"USER_jm_metal_transfers_rule for the combination of from loco '" + fromLoco + "' and to loco '" + toLoco + "'"
                    		+ ". Possible rules are: " + sb.toString());
                }
                else if (fromLoco.equals(toLoco)) {
                    // Transfer is being made in the same PMM location 
                    bookSameLocationCashTransfer(context, strategy, fromLoco, fromForm, toLoco, toForm);
                }
                else if (isFromLocoPmm || isToLocoPmm){
                    // Transfer is being made between two different PMM locations
                    bookCrossLocationCashTransfers(context, strategy, fromLoco, fromForm, toLoco, toForm);
                }
                else {
                    throw new RuntimeException("Strategy " + strategyRef + ": The transfer scenario represented by the strategy is not " +
                    		"supported.");
                }
                
                Logging.info("Strategy " + strategyRef + ": Validating all cash transfer deals for strategy");
                CashTransfer.validateDeals(context, strategy);
                try {
                	Thread.sleep(1000);
                } catch ( InterruptedException ie) {
                    Logging.error("Strategy " + strategyRef + ": Could not sleep one second", ie);                	
                }
                CashTransfer.revalidateDeals(context, strategy); // JW: 2016-04-12
                try {
                	Thread.sleep(1000);
                } catch ( InterruptedException ie) {
                    Logging.error("Strategy " + strategyRef + ": Could not sleep one second", ie);                	
                }
            }
            catch (Throwable e) {
                Logging.error("Strategy " + strategyRef + ": Failed to complete metal transfer", e);
                Logging.info("Strategy " + strategyRef + ": Error occurred, cancelling all cash transfer deals for strategy");
                CashTransfer.cancelDeals(context, strategy);
                if (e instanceof RuntimeException)
                    throw (RuntimeException) e;
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    /**
     * Initialize instance variables.
     * 
     * @param context Context session
     */
    private void init(Context context) {
        // Determine which loco's are PMM loco's
        try (DatabaseTable dbTbl = context.getIOFactory().getDatabaseTable("USER_jm_loco");
             Table loco = dbTbl.retrieveTable()) {
            for (int row = 0; row < loco.getRowCount(); row++) {
                if ("PMM".equals(loco.getString("is_PMM", row))) {
                    pmmLoco.add(loco.getString("loco_name", row));
                }
            }
        }
    }
    
    /**
     * Get the metals transfer rule for the from and to loco combination.
     * 
     * @param context
     * @param fromLoco
     * @param toLoco
     * @return
     */
    private Table getRule(Context context, String fromLoco, String toLoco) {
        return context.getIOFactory().runSQL(
                "\n SELECT usr.*, p1.party_id AS intermediate_from_bunit_id, p2.party_id AS intermediate_to_bunit_id" +
                "\n   FROM USER_jm_metal_transfer_rules usr" +
                "\n   JOIN party p1 ON (p1.short_name = usr.intermediate_from_bunit)" +
                "\n   JOIN party p2 ON (p2.short_name = usr.intermediate_to_bunit)" + "\n  WHERE from_loco = '" + fromLoco + "'" +
                "\n    AND to_loco = '" + toLoco + "'");
    }

    /**
     * Book cash transfer deals based on the rule defined in the rule table. The rule table provided to this method should contain a
     * single row which is the rule for the from and to loco combination.
     * 
     * @param context Endur session
     * @param strategy Strategy transaction
     * @param rule Rule table containing single row of rule to use
     * @param fromLoco
     * @param fromForm
     * @param toLoco
     * @param toForm
     */
    private void bookRuleBasedCashTransfers(Context context, Transaction strategy, Table rule,
            String fromLoco, String fromForm, String toLoco, String toForm) {
        Logging.info("Strategy " + strategyRef + ": Booking metal transfers based based on rule in USER_jm_metal_transfer_rule");
        
        String metal = strategy.getField("Metal").getValueAsString();
        
        int fromAccountId = getStaticId(context, strategy, EnumReferenceObject.SettlementAccount, "From A/C");
        int fromBunitId = getStaticId(context, strategy, EnumReferenceObject.BusinessUnit, "From A/C BU");

        // Use the internal business unit from the strategy, or if set in the rules, override with the rule business unit
        int toBunitId = strategy.getField(EnumTransactionFieldId.InternalBusinessUnit).getValueAsInt();
        String ruleBunit = rule.getString("initial_internal_bunit", 0);
        if (ruleBunit != null && !ruleBunit.trim().isEmpty()) {
            toBunitId = context.getStaticDataFactory().getReferenceObject(BusinessUnit.class, ruleBunit).getId();
        }
        int toAccountId = retrieveCashSettleAccountId(context, toBunitId, fromLoco, fromForm);
        int toPortfolioId = retrieveMetalPortfolioId(context, toBunitId, metal);
        
        CashTransfer cash = new CashTransfer();
        cash.setFromFields(fromAccountId, fromBunitId);
        cash.setToFields(toAccountId, toBunitId, toPortfolioId);
        int tranNum = cash.bookDeal(context, strategy);
        Logging.info("Strategy " + strategyRef + ": Booked cash transfer transaction " + tranNum);

        // ------

        // 009 - Corrected the Intermediate deal to use form(from and to) selected on trade instead of Sponge in all cases.
        
        String fromBUnitName = rule.getString("intermediate_from_bunit", 0);
        fromBunitId = context.getStaticDataFactory().getId(EnumReferenceTable.Party, fromBUnitName);
        String ruleFromLoco = rule.getString("intermediate_from_loco", 0);
        fromAccountId = retrieveCashSettleAccountId(context, fromBunitId, ruleFromLoco, fromForm);
        int fromPortfolioId = retrieveMetalPortfolioId(context, fromBunitId, metal);

        String toBUnitName = rule.getString("intermediate_to_bunit", 0);
        toBunitId = context.getStaticDataFactory().getId(EnumReferenceTable.Party, toBUnitName);
        String ruleToLoco = rule.getString("intermediate_to_loco", 0);
        toAccountId = retrieveCashSettleAccountId(context, toBunitId, ruleToLoco, toForm);
        toPortfolioId = retrieveMetalPortfolioId(context, toBunitId, metal);

        cash = new CashTransfer();
        cash.setFromFields(fromAccountId, fromBunitId, fromPortfolioId);
        cash.setToFields(toAccountId, toBunitId, toPortfolioId);
        tranNum = cash.bookDeal(context, strategy);
        Logging.info("Strategy " + strategyRef + ": Booked cash transfer transaction " + tranNum);

        // ------

        fromBunitId = toBunitId;
        fromAccountId = retrieveCashSettleAccountId(context, fromBunitId, toLoco, toForm);
        fromPortfolioId = retrieveMetalPortfolioId(context, fromBunitId, metal);

        toAccountId = getStaticId(context, strategy, EnumReferenceObject.SettlementAccount, "To A/C");
        toBunitId = getStaticId(context, strategy, EnumReferenceObject.BusinessUnit, "To A/C BU");

        cash = new CashTransfer();
        cash.setFromFields(fromAccountId, fromBunitId, fromPortfolioId);
        cash.setToFields(toAccountId, toBunitId);
        tranNum = cash.bookDeal(context, strategy);
        Logging.info("Strategy " + strategyRef + ": Booked cash transfer transaction " + tranNum);
    }

    /**
     * Book cash transfer deals for metals transfer within the same location. Books a single cash transfer deal with a pass-thru account.
     * 
     * @param session Endur session
     * @param strategy Strategy transaction
     * @param fromLoco
     * @param fromForm
     * @param toLoco
     * @param toForm
     */
    private void bookSameLocationCashTransfer(Session session, Transaction strategy, String fromLoco, String fromForm,
            String toLoco, String toForm) {

        Logging.info("Strategy " + strategyRef + ": Booking pass-thru metal transfer for transfer in same location");

        int bunitId = strategy.getField(EnumTransactionFieldId.InternalBusinessUnit).getValueAsInt();
        int passThruAccountId = retrieveCashSettleAccountId(session, bunitId, fromLoco, fromForm);

        int fromAccountId = getStaticId(session, strategy, EnumReferenceObject.SettlementAccount, "From A/C");
        int fromBunitId = getStaticId(session, strategy, EnumReferenceObject.BusinessUnit, "From A/C BU");
        int fromPortfolioId = strategy.getField(EnumTransactionFieldId.InternalPortfolio).getValueAsInt();
        int toAccountId = getStaticId(session, strategy, EnumReferenceObject.SettlementAccount, "To A/C");
        int toBunitId = getStaticId(session, strategy, EnumReferenceObject.BusinessUnit, "To A/C BU");
        int toPortfolioId = strategy.getField(EnumTransactionFieldId.InternalPortfolio).getValueAsInt();
        
        CashTransfer cash = new CashTransfer();
        cash.setFromFields(fromAccountId, fromBunitId, fromPortfolioId);
        cash.setToFields(toAccountId, toBunitId, toPortfolioId);
        cash.setPassThroughAccount(passThruAccountId);
        int tranNum = cash.bookDeal(session, strategy);
        Logging.info("Strategy " + strategyRef + ": Booked cash transfer transaction " + tranNum);
    }

    /**
     * Book cash transfer deals for metals transfer across locations where at least one of those locations is a PMM location. Books two
     * cash transfer deals to move the metal between the locations.
     * 
     * @param session Endur session
     * @param strategy Strategy transaction
     * @param fromLoco
     * @param fromForm
     * @param toLoco
     * @param toForm
     */
    private void bookCrossLocationCashTransfers(Session session, Transaction strategy, String fromLoco, String fromForm,
            String toLoco, String toForm) {

        Logging.info("Strategy " + strategyRef + ": Booking metal transfers for transfer in different locations");

        int fromAccountId = getStaticId(session, strategy, EnumReferenceObject.SettlementAccount, "From A/C");
        int fromBunitId = getStaticId(session, strategy, EnumReferenceObject.BusinessUnit, "From A/C BU");

        int toBunitId = strategy.getField(EnumTransactionFieldId.InternalBusinessUnit).getValueAsInt();
        int toAccountId = retrieveCashSettleAccountId(session, toBunitId, fromLoco, fromForm);
        int toPortfolioId = strategy.getField(EnumTransactionFieldId.InternalPortfolio).getValueAsInt();

        CashTransfer cash = new CashTransfer();
        cash.setFromFields(fromAccountId, fromBunitId);
        cash.setToFields(toAccountId, toBunitId, toPortfolioId);
        
        int tranNum = cash.bookDeal(session, strategy);
        Logging.info("Strategy " + strategyRef + ": Booked cash transfer transaction " + tranNum);

        // ------

        fromBunitId = toBunitId;
        fromAccountId = retrieveCashSettleAccountId(session, fromBunitId, toLoco, toForm);
        int fromPortfolioId = strategy.getField(EnumTransactionFieldId.InternalPortfolio).getValueAsInt();

        toAccountId = getStaticId(session, strategy, EnumReferenceObject.SettlementAccount, "To A/C");
        toBunitId = getStaticId(session, strategy, EnumReferenceObject.BusinessUnit, "To A/C BU");

        cash = new CashTransfer();
        cash.setFromFields(fromAccountId, fromBunitId, fromPortfolioId);
        cash.setToFields(toAccountId, toBunitId);
        
        tranNum = cash.bookDeal(session, strategy);
        Logging.info("Strategy " + strategyRef + ": Booked cash transfer transaction " + tranNum);
        
    }

    private int getStaticId(Session session, Transaction strategy, EnumReferenceObject ref, String name) {
        return session.getStaticDataFactory().getReferenceObject(ref, strategy.getField(name).getValueAsString()).getId();
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
    private int retrieveCashSettleAccountId(Session session, int bunitId, String loco, String form) {
    	
    	Logging.info("Fetching Account for int bunit: "+session.getStaticDataFactory().getName(EnumReferenceTable.Party, bunitId)+" Loco: "+loco+" form: "+form);
    	String accIds = getAccountsForExclusion(session);
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
    	try(Table account = session.getIOFactory().runSQL(sql.toString()))
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
                throw new RuntimeException("Strategy " + strategyRef + ": More than one cash settlement account has been found for " +
                		"business unit id " + bunitId + " loco '" + loco + "' and form '" + form + "':" + accountList.toString() );
            }
            throw new RuntimeException("Strategy " + strategyRef + ": No cash settlement account has been found for business unit id " + 
                    bunitId + " loco '" + loco + "' and form '" + form + "'");
        }
    }
    
	private String getAccountsForExclusion(Session session) {
		Table excludeAccTable=null;
		String accountExcluded="";
		try{
			ConstRepository _constRepo = new ConstRepository("Strategy", "NewTrade");
			excludeAccTable=session.getTableFactory().fromOpenJvs(_constRepo.getMultiStringValue("accountsToExclude"));
			if( excludeAccTable==null || excludeAccTable.getRowCount()==0)
			{
				Logging.info("No Accounts were found to be excluded under constRepo for name: accountsToExclude");
				throw new RuntimeException("No Accounts were found to be excluded under constRepo for name: accountsToExclude");
			}
			int rowCount=excludeAccTable.getRowCount();
			for(int rowId=0;rowId<rowCount;rowId++)
			{
				String accName=excludeAccTable.getString("value", rowId);
				int accId=session.getStaticDataFactory().getId(EnumReferenceTable.Account, accName);
				accountExcluded=accountExcluded+","+Integer.toString(accId);
			}
			accountExcluded=accountExcluded.replaceFirst(",","");
		}

		catch(Exception e)
		{
			Logging.error("Failed while executing getAccountsForExclusion"+e.getMessage(),e);
			throw new RuntimeException("Failed while executing getAccountsForExclusion");
		}
		return accountExcluded;
	}

    /**
     * Retrieve the portfolio id for the business unit and metal combination. This is found by getting the description of the metal from
     * the currency table. The description is used to search for a portfolio with a name ending with that description amongst the business
     * units portfolios.r
     * 
     * @param session
     * @param bunitId
     * @param metal
     * @return
     */
    private int retrieveMetalPortfolioId(Session session, int bunitId, String metal) {

        try (Table currency = session.getIOFactory().getDatabaseTable("currency").retrieveTable()) {

            int row = currency.find(currency.getColumnId("name"), metal, 0);
            metal = currency.getString("description", row);
            
            try (Table portfolio = session.getIOFactory().runSQL(
                    "\n SELECT id_number" +
                    "\n   FROM portfolio p" +
                    "\n   JOIN party_portfolio pp ON (pp.portfolio_id = p.id_number)" +
                    "\n  WHERE p.name LIKE '%" + metal + "'" +
                    "\n    AND pp.party_id = " + bunitId)) {
                if (portfolio.getRowCount() > 0) {
                	if (portfolio.getRowCount() > 1) {
                		StringBuilder sb = new StringBuilder();
                		boolean first = true;
                		for (int pRow=0; pRow < portfolio.getRowCount(); pRow++) {
                			if (!first) {
                				sb.append(", ");
                			}
                			sb.append("#").append (portfolio.getInt("id_number", pRow));
                			first = false;
                		}
                		Logging.info("multiple portfolios found for business unit #" + bunitId + " and metal " + metal
                				+ ". Selecting the first portfolio out of the following list: " + sb.toString());
                	}
                    return portfolio.getInt(0, 0);
                } 
            }
        }
        return 0;
    }

}
