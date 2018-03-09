package com.olf.jm.metalutilisationstatement.tpm;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.tpm.AbstractProcessStep;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OException;
import com.olf.openjvs.Tpm;
import com.olf.openrisk.backoffice.SettlementAccount;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.staticdata.BusinessUnit;
import com.olf.openrisk.staticdata.Currency;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.Token;
import com.olf.openrisk.tpm.Variable;
import com.olf.openrisk.tpm.Variables;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.staticdata.Field;
import com.olf.openrisk.trading.Instrument;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import com.openlink.esp.migration.persistence.log.Logger;

/**
 * 
 * TPM plugin that takes the output from the Metals Utilisation Statement Report Builder report and books cash deals for the interest
 * calculated by the report.
 * 
 * @author Gary Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 30-Nov-2015 |               | G. Moore        | Initial version.                                                                |
 * | 002 | 05-Apr-2016 |               | J. Waechter     | No longer throwing exception in case of already existing cash interest deals    |
 * | 003 | 09-Apr-2016 |               | J. Waechter     | Settle Date changed to 15th of month following metal utilisation statement date |
 * | 004 | 01-Jul-2016 |               | J. Waechter     | Now saving metal utilisation statement date to info field                       |
 * | 005 | 18-Oct-2016 |               | J. Waechter     | Added setting of guard variables to allow rerun of plugin                       |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.TpmStep })
public class CashInterestDealBooking extends AbstractProcessStep {

    private static final String TRANF_INFO_STATEMENT_DATE = "Util Statement Date";
	private static final SimpleDateFormat SDF = new SimpleDateFormat("dd-MMM-yyyy");

    @Override
    public Table execute(Context context, Process process, Token token, Person submitter, boolean transferItemLocks, Variables variables) {
        Logging.init(context, this.getClass(), "Metals Utilisation Statement", "Cash Interest Deal Booking");
        try {
        	long wflowId = Tpm.getWorkflowId();
    		Tpm.setVariable(wflowId, "PluginStart", "Yes");

            // Get the report output table which will be used to book the interest deals
            // Get the tran num table which will be populated with the transaction numbers of the booked cash deals
            try (Variable var1 = process.getVariable("ReportOutput");
                 ConstTable output1 = var1.getValueAsTable();
                 Table details = output1.asTable();
                 Variable var2 = process.getVariable("TranNums");
                 ConstTable output2 = var2.getValueAsTable();
                 Table tranNums = output2.asTable()) {
                try {
                    process(context, details, tranNums);
            		Tpm.setVariable(wflowId, "PluginEnd", "Yes");
                    return null;
                }
                catch (RuntimeException e) {
                    Logging.error("Process failed", e);
                    throw e;
                }
                finally {
                    // Set the details table back to the ReportOutput variable so as user can see any errors
                    var1.setValue(details);
                    process.setVariable(var1);
                    // Set the tranNums table back to the TranNums variable so as TPM workflow can generate invoices
                    var2.setValue(tranNums);
                    process.setVariable(var2);
                }
            }
            catch (Exception e) {
                Logging.error("Process failed", e);
                throw new RuntimeException(e);
            }
            finally {
                Logging.close();
            }        	
        } catch (OException ex) {
            Logging.error("Could not update TPM guard variables", ex);
            throw new RuntimeException (ex);
        }
    }

    /**
     * Main processing method to book interest cash deals.
     * 
     * @param context Session context
     * @param details Table of interest charges to book
     * @param tranNums Table will be populated with transaction numbers of deals booked
     * @throws ParseException thrown for invalid date
     */
    private void process(Context context, Table details, Table tranNums) throws ParseException {
        
        if (!tranNums.isValidColumn("tran_num")) {
            tranNums.addColumn("tran_num", EnumColType.Int);
        }
        else {
            tranNums.clearData();
        }

        StringBuilder errMsg = new StringBuilder();
        
        TradingFactory tradeFactory = context.getTradingFactory();
        StaticDataFactory staticFactory = context.getStaticDataFactory();
        
        // Add error message column to details table
        try {
            details.addColumns("String[error_message]");        	
            details.getFormatter().setColumnTitle("error_message", "Error Message");
        } catch (OpenRiskException ex) {
        	Logging.info("Could not add col 'error_message' to tablle");
        }

        for (TableRow row : details.getRows()) {
            try {
                Date date = SDF.parse(row.getString("statement_date"));
                Date settleDate = getSettleDate(date);
                String accountName = row.getString("account_name");
                String currency = row.getString("preferred_currency");
                Currency metal = staticFactory.getReferenceObject(Currency.class, row.getString("metal"));
                double interest = row.getDouble("value");
                SettlementAccount account = staticFactory.getReferenceObject(SettlementAccount.class, accountName);
                BusinessUnit intBUnit = account.getBusinessUnit();
                BusinessUnit extBUnit = staticFactory.getReferenceObject(BusinessUnit.class, row.getInt("party_id"));

                Logging.info("Processing interest for business unit %1$s, party %2$s, metal %3$s, currency %4$s, interest %5$-#4.2f and date %6$s",
                        intBUnit.getName(), extBUnit.getName(), metal.getName(), currency, interest, SDF.format(date));

                // Generate reference for interest deal
                String reference = generateReference(date, account, metal);
                Logging.info("Generated interest deal reference is " + reference);
                
                // Only book deal if the interest value is non zero
                // only look at 2 decimal places 

                if(round(interest, 2) == 0.0) {
                	Logging.info("Skipping booking deal with reference " + reference + " as interest amount is 0.");
                	continue;
                }

                // Only book deal if a cash deal with the same reference does not already exist
                if (!tradeFactory.isValidTransactionReference(reference, EnumTranStatus.Validated)) {

                    Logging.info("Booking interest deal using reference " + reference);

                    int tranNum = bookDeal(context, intBUnit, extBUnit, metal, currency, reference, interest, settleDate, date);
                    Logging.info("Successfully booked cash interest deal " + tranNum + " with reference " + reference);

                    // Add cash deals transaction number to the list of booked deals
                    TableRow newTranRow = tranNums.addRow();
                    newTranRow.setValues(new Object[] {tranNum});
                }
                else {
                    String message = String.format(
                            "An interest deal is already booked for date %1$tb-%1$tY, business unit %2$s, party %3$s and metal %4$s",
                            date, intBUnit.getName(), extBUnit.getName(), metal.getName());
                    Logging.info(message);
                    details.setValue("error_message", row.getNumber(), message);
                }
            }
            catch (Exception e) {
                Logging.error("An exception occured while processing interest bookings", e);
                errMsg.append(e.getLocalizedMessage());
                details.setValue("error_message", row.getNumber(), e.getLocalizedMessage());
            }
        }
        
        if (errMsg.length() > 0) {
            throw new RuntimeException("Interest deal booking issues:\n" + errMsg);
        }
    }

    /**
     * Retrieve the portfolio id of the fee portfolio for the business unit. The portfolio name is expected to end with 'Fees'.
     * 
     * @param context Current session context
     * @param bUnit Business unit
     * @return portfolio id
     */
    private int retrieveFeePortfolioId(Context context, BusinessUnit bUnit) {

        try (Table portfolio = context.getIOFactory().runSQL(
                "\n SELECT id_number" +
                "\n   FROM portfolio p" +
                "\n   JOIN party_portfolio pp ON (pp.portfolio_id = p.id_number)" +
                "\n  WHERE p.name LIKE '% Fees'" +
                "\n    AND pp.party_id = " + bUnit.getId())) {
            if (portfolio.getRowCount() > 0) {
                    return portfolio.getInt(0, 0);
            }
        }
        throw new RuntimeException("No fees portfolio found for business unit " + bUnit.getName());
    }

    /**
     * Generate reference based on parameters.
     * 
     * @param date Statement date
     * @param account Account
     * @param metal Metal
     * @return reference string
     */
    private String generateReference(Date date, SettlementAccount account, Currency metal) {
        return String.format("%1$tY%1$tm%1$td_%2$-1d_%3$-1d", date, account.getId(), metal.getId());
    }

    /**
     * Book the interest cash deal.
     * 
     * @param context Current session context
     * @param intBUnit Internal business unit
     * @param extBUnit External business unit
     * @param metal Metal
     * @param currency Cash currency
     * @param reference Deal reference
     * @param interest Interest amount
     * @param settleDate 
     * @param date 
     * @return transaction number
     */
    private int bookDeal(Context context, BusinessUnit intBUnit, BusinessUnit extBUnit, Currency metal, String currency, String reference,
            double interest, Date settleDate, Date metalUtilisationDate) {
        TradingFactory tradeFactory = context.getTradingFactory();
        try (Instrument ins = tradeFactory.retrieveInstrumentByTicker(EnumInsType.CashInstrument, currency);
                Transaction cash = tradeFactory.createTransaction(ins)) {
               cash.setValue(EnumTransactionFieldId.CashflowType, "Rentals Interest - " + metal.getDescription().toUpperCase());
               cash.setValue(EnumTransactionFieldId.InternalBusinessUnit, intBUnit.getId());
               cash.setValue(EnumTransactionFieldId.ExternalBusinessUnit, extBUnit.getId());
               cash.setValue(EnumTransactionFieldId.InternalPortfolio, retrieveFeePortfolioId(context, intBUnit));
               cash.setValue(EnumTransactionFieldId.Position, interest);
               cash.setValue(EnumTransactionFieldId.ReferenceString, reference);
               cash.setValue(EnumTransactionFieldId.SettleDate, settleDate);
               Field metalUtilisationInfoField = cash.getField(TRANF_INFO_STATEMENT_DATE);
               if (metalUtilisationInfoField != null && metalUtilisationInfoField.isApplicable()
            		   && metalUtilisationInfoField.isWritable()) {
            	   String formattedDate = SDF.format(metalUtilisationDate);
            	   metalUtilisationInfoField.setValue(formattedDate);  
               } else {
            	   String errorMessage = 
            			   "Can't write on the transaction info field '" + TRANF_INFO_STATEMENT_DATE + "'."
            			+  "\nPlease check if the transaction info field is set up correctly"
            			+  "in the instrument builder for the right instrument types"
            			;
            	   throw new RuntimeException (errorMessage);
               }
               cash.process(EnumTranStatus.Validated);
               return cash.getDealTrackingId();
           }
    }

    private Date getSettleDate(Date date) { 
    	Calendar today = Calendar.getInstance(); 
    	today.setTime(date); today.add(Calendar.MONTH, 1);
    	today.set(Calendar.DAY_OF_MONTH, 15); 
    	return today.getTime(); 
    }
    
	public double round(double valueToRound, int numberOfDecimalPlaces)
	{
	    double multipicationFactor = Math.pow(10, numberOfDecimalPlaces);
	    double interestedInZeroDPs = valueToRound * multipicationFactor;
	    return Math.round(interestedInZeroDPs) / multipicationFactor;
	}
}
