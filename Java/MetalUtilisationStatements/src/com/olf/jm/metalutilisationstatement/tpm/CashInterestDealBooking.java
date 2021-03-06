package com.olf.jm.metalutilisationstatement.tpm;

import java.text.DecimalFormat;
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
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.staticdata.BusinessUnit;
import com.olf.openrisk.staticdata.Currency;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.staticdata.ReferenceChoices;
import com.olf.openrisk.staticdata.ReferenceObject;
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
 * | 005 | 18-Oct-2016 |               | J. Waechter     | Added setting of guard variables to allow rerun of plugin 
 * | 006 | 13-Feb-2018 |               | S.Curran        | log status to the user table USER_jm_metal_rentals_run_data 
 * | 007 | 11-Apr-2018 |			   | N.Sajja		 | Update the cashflow name from Rentals Interest to Metal Rentals                 |
 * | 008 | 20-Feb-2019 |			   | K.Babu 		 | Updates for CN                                                                  |
 *   009 | 14-May-2019 |			   | K.Babu 		 | Fix for deciding if the amount with vat should be stamped                       |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.TpmStep })
public class CashInterestDealBooking extends AbstractProcessStep {

    private static final String TRANF_INFO_STATEMENT_DATE = "Util Statement Date";
    private static final String TRANF_INFO_AMOUNT_WITH_VAT = "Amount with VAT";
    private static final String TRANF_INFO_CCY_CONV_FX_RATE = "Ccy Conv FX Rate";
    
	private static final SimpleDateFormat SDF = new SimpleDateFormat("dd-MMM-yyyy");
	private static final String TRAN_INFO_INTERFACE_TRADE_TYPE = "Interface_Trade_Type";
	private static final String TRAN_INFO_INTERFACE_TRADE_TYPE_VALUE = "Metal Interest";
	private int tranNum =0;
	private static final String CN_TPM = "Metals Utilisation_CN";
    @Override
    public Table execute(Context context, Process process, Token token, Person submitter, boolean transferItemLocks, Variables variables) {
        Logging.init(context, this.getClass(), "Metals Utilisation Statement", "Cash Interest Deal Booking");
        try {
        	long wflowId = Tpm.getWorkflowId();
    		Tpm.setVariable(wflowId, "PluginStart", "Yes");

            // Get the report output table which will be used to book the interest deals
            // Get the tran num table which will be populated with the transaction numbers of the booked cash deals
            try (Variable var1 = process.getVariable("ReportOutput");
            		Variable tpmName = process.getVariable("TPMName");
                 ConstTable output1 = var1.getValueAsTable();
                 Table details = output1.asTable();
                 Variable var2 = process.getVariable("TranNums");
                 ConstTable output2 = var2.getValueAsTable();
                 Table tranNums = output2.asTable()) {
                try {
                    process(context, details, tranNums,tpmName);
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
            	Logging.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>Exiting Cash Interest Deal booking >>>>>>>>>>>>>>");
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
    private void process(Context context, Table details, Table tranNums, Variable var3) throws ParseException {
        
    	String tpmName = var3.getValueAsString();
    	boolean isCn = false;
    	
    	if (CN_TPM.equals(tpmName))
        {
    		isCn = true;
        }
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

                double grossInterestValue = 0.0;
                double netInterestValue = 0.0;
                double avgCnyRate = 0.0;
                
                // If trigger is CN TPM get the additional fields
                if (isCn)
                {
                     grossInterestValue = row.getDouble("gross_interest_rate_new");
                     //netInterestValue = row.getDouble("net_interest_rate_new");
                     netInterestValue = row.getDouble("value"); //tweak to pick up value 
                     avgCnyRate = row.getDouble("avg_cny_rate");
                     // If Gross is bigger than the final value then amount with vat should not be set. 
                     // Additional check in deal booking where it checks if gross == 0 then do not set amount with vat
                     if((grossInterestValue - netInterestValue) > 1)
                     {
                    	 grossInterestValue = 0;
                     }
                     Logging.info("CN TPM triggered,  Gross Interest value %1$-#4.4f , net interest value %2$-#4.4f currency conversion rate %3$-#4.4f", grossInterestValue, netInterestValue,avgCnyRate);

                }

                // Generate reference for interest deal
                String reference = generateReference(date, account, metal);
                Logging.info("Generated interest deal reference is " + reference);
                
                // Only book deal if the interest value is non zero
                // only look at 2 decimal places 

                if(round(interest, 2) == 0.0) {
                	Logging.info("Skipping booking deal with reference " + reference + " as interest amount is 0.");
                	updateTable(context, row, 0, "Skipping booking deal with reference " + reference + " as interest amount is 0.", "Error Booked Deal",var3 );
                	continue;
                }

                // Only book deal if a cash deal with the same reference does not already exist
                if (!tradeFactory.isValidTransactionReference(reference, EnumTranStatus.Validated)) {

                    Logging.info("Booking interest deal using reference " + reference);
                    	
                   	tranNum = bookDeal(context, intBUnit, extBUnit, metal, currency, reference, interest, settleDate, date, grossInterestValue, netInterestValue, avgCnyRate,isCn);
                    	

                    Logging.info("Successfully booked cash interest deal " + tranNum + " with reference " + reference);
                    // Add cash deals transaction number to the list of booked deals
                    TableRow newTranRow = tranNums.addRow();
                    newTranRow.setValues(new Object[] {tranNum});
                    
                    updateTable(context, row, tranNum, "", "Deal Booked", var3 );
                }
                else {
                    String message = String.format(
                            "An interest deal is already booked for date %1$tb-%1$tY, business unit %2$s, party %3$s and metal %4$s",
                            date, intBUnit.getName(), extBUnit.getName(), metal.getName());
                    Logging.info(message);
                    details.setValue("error_message", row.getNumber(), message);
                    updateTable(context, row, 0, message, "Error Booked Deal",var3 );
                }
            }
            catch (Exception e) {
                Logging.error("An exception occured while processing interest bookings", e);
                errMsg.append(e.getLocalizedMessage());
                details.setValue("error_message", row.getNumber(), e.getLocalizedMessage());
                
                updateTable(context, row, 0, "An exception occured while processing interest bookings " + e.getLocalizedMessage(), "Error Booked Deal",var3 );
            }
        }
        
        if (errMsg.length() > 0) {
            throw new RuntimeException("Interest deal booking issues:\n" + errMsg);
        }
        Logging.info("...  Completed Cash Interest Deal Booking ...");
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
            double interest, Date settleDate, Date metalUtilisationDate, double grossRate, double netRate, double currencyConversionRate, boolean isCn) {
        TradingFactory tradeFactory = context.getTradingFactory();
        try
        (    	Instrument ins = tradeFactory.retrieveInstrumentByTicker(EnumInsType.CashInstrument, currency);
            	Transaction  cash = tradeFactory.createTransaction(ins);)
        {
               cash.setValue(EnumTransactionFieldId.CashflowType, "Metal Rentals - " + metal.getDescription().toUpperCase());
               cash.setValue(EnumTransactionFieldId.InternalBusinessUnit, intBUnit.getId());
               cash.setValue(EnumTransactionFieldId.ExternalBusinessUnit, extBUnit.getId());
               cash.setValue(EnumTransactionFieldId.InternalPortfolio, retrieveFeePortfolioId(context, intBUnit));
               if( isCn)
               {
            	   
            	   Logging.info("isCN flag is set");
            	   Logging.info("Setting position for CN "+netRate);
            	   cash.setValue(EnumTransactionFieldId.Position, netRate);
               }
               else
               {
            	   Logging.info("isCN flag is NOT set");
            	   Logging.info("Setting position for NON-CN "+netRate);
            	   
            	   cash.setValue(EnumTransactionFieldId.Position, interest);
               }
               
               cash.setValue(EnumTransactionFieldId.ReferenceString, reference);
               cash.setValue(EnumTransactionFieldId.SettleDate, settleDate);
               
               Field metalUtilisationInfoField = this.getField(TRANF_INFO_STATEMENT_DATE, cash);
               String formattedDate = SDF.format(metalUtilisationDate);
               metalUtilisationInfoField.setValue(formattedDate);  
               
               //Setting Interface trade type field to Metal Interest
               Field interfaceTradeTypeField = this.getField(TRAN_INFO_INTERFACE_TRADE_TYPE,cash);
           	   interfaceTradeTypeField.setValue(TRAN_INFO_INTERFACE_TRADE_TYPE_VALUE);

               if(isCn)
               {
            	   DecimalFormat df = new DecimalFormat(".#######");
            	   // Set the amount with vat only if  the grossRate is non zero, this is to fix the vat calculation so that the 
            	   // amount on the field is not calculated by the field notification script.
            	   if(grossRate > 0)
            	   {
            		   Field grossRateField = this.getField(TRANF_INFO_AMOUNT_WITH_VAT,cash);
            		   grossRateField.setValue(Double.valueOf(df.format(grossRate))); 
            	   }

           		   Field ccyConvFxRate = this.getField(TRANF_INFO_CCY_CONV_FX_RATE,cash);
           		   ccyConvFxRate.setValue(Double.valueOf(df.format(currencyConversionRate))); 
            	   
               }
               try {
            	   
            	   Logging.info("Processing the deal to validated status ");
            	   this.sleep(10);
            	   cash.process(EnumTranStatus.Validated);
               }
               catch(Throwable th)
               {
            	   Logging.info("Exception when creating deal"+ th.getLocalizedMessage());
            	   Logging.info(th.getMessage());
            	   //Logging.info(th.getCause().getLocalizedMessage());
            	   //Logging.info(th.getCause().getMessage());
               }
               int tranNum = this.getTranNum(context, reference);
               if(tranNum == 0)
               {
            	   throw new RuntimeException("Exception while fetching deal number !!. Deal creation failed");
               }
               return tranNum;
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
	
	private void updateTable(Context context, TableRow rowToUpdate, int dealTrackingNum, String message, String status, Variable var3 ) {
		Logging.info("Updating user table with values " + dealTrackingNum + " " + message + " " + status);
		String tpmName = var3.getValueAsString();
		UserTable userTable;
		boolean isCN = false;
		if (CN_TPM.equals(tpmName))
		{
			userTable = context.getIOFactory().getUserTable("USER_jm_metalrentals_rundata_cn");
			isCN = true;
		}
		else
		{
			
			userTable = context.getIOFactory().getUserTable("USER_jm_metal_rentals_run_data");
		}
		
		Table updateData = userTable.getTableStructure();
		updateData.addRows(1);
		
		StaticDataFactory staticFactory = context.getStaticDataFactory();
	
		updateData.setString("month_stamp", 0, rowToUpdate.getString("month_stamp"));	 	 	 
		updateData.setString("month_stamp_str", 0, rowToUpdate.getString("month_stamp_str"));	  	 	 	 	 
		updateData.setString("account_name", 0,  rowToUpdate.getString("account_name"));
		if(isCN)
		{
			updateData.setInt("account_id", 0, staticFactory.getReferenceObject(EnumReferenceObject.SettlementAccount, rowToUpdate.getString("account_id")).getId());	 	 	 	 	 
			
		}
		else
		{
			
			updateData.setString("account_id", 0, staticFactory.getReferenceObject(EnumReferenceObject.SettlementAccount, rowToUpdate.getInt("account_id")).getName());	 	 	 	 	 
		}
		updateData.setString("metal_description", 0,  rowToUpdate.getString("metal_description"));	  	 	 	 	 
		updateData.setString("status", 0,  status);	 	 	 	 	 
		updateData.setInt("deal_tracking_num", 0, dealTrackingNum);	 	 	 	 	 	 
		updateData.setString("message", 0, message);	 	 	 	 	 	 
		updateData.setDate("last_update", 0, context.getServerTime());	 	 	 	 	 	 	 
		updateData.setString("long_name", 0, rowToUpdate.getString("long_name"));	 	 	 	 	 
		updateData.setString("addr1", 0, rowToUpdate.getString("addr1"));	 	 	 	 	 	 
		updateData.setString("addr2", 0, rowToUpdate.getString("addr2"));	 	 	 	 	 	 
		updateData.setString("addr_reference_name", 0, rowToUpdate.getString("addr_reference_name"));	 	 	 	 	 	 
		updateData.setString("irs_terminal_num", 0, rowToUpdate.getString("irs_terminal_num"));	 	 	 	 	 	 
		updateData.setString("city", 0, rowToUpdate.getString("city"));	 	 	 	 	 	 
		updateData.setString("county_id", 0, rowToUpdate.getString("county_id"));	  	 	 	 	 
		updateData.setString("mail_code", 0, rowToUpdate.getString("mail_code"));	 	 	 	 	 	 
		updateData.setString("state_id", 0, rowToUpdate.getString("state_id"));	  	 	 	 
		updateData.setString("country", 0, rowToUpdate.getString("country"));	  	 	 	 	 	 
		updateData.setString("fax", 0, rowToUpdate.getString("fax"));	  	 	 	 
		updateData.setString("phone", 0, rowToUpdate.getString("phone"));	 	 	 	 	 	 
		updateData.setString("tax_id", 0, rowToUpdate.getString("tax_id"));	  	 	 	 	 
		updateData.setInt("party_id", 0, rowToUpdate.getInt("party_id"));	  	 	 	 	 
		updateData.setString("short_name", 0, rowToUpdate.getString("short_name"));	 	 	 	 	 	 
		updateData.setString("addr11", 0, rowToUpdate.getString("addr11"));	 	 	 	 	 
		updateData.setString("addr21", 0, rowToUpdate.getString("addr21"));	 	 	 	 	 	 
		updateData.setString("addr_reference_name1", 0, rowToUpdate.getString("addr_reference_name1"));	 	 	 	 	 	 
		updateData.setString("irs_terminal_num1", 0, rowToUpdate.getString("irs_terminal_num1"));	 	 	 	 	 	 
		updateData.setString("city1", 0, rowToUpdate.getString("city1"));	 	 	 	 	 	 
		updateData.setString("county_id1", 0, rowToUpdate.getString("county_id1"));	 	 	 	 	 	 
		updateData.setString("mail_code1", 0, rowToUpdate.getString("mail_code1"));	 	 	 	 	 	 
		updateData.setString("state_id1", 0, rowToUpdate.getString("state_id1"));	 	 	 	 	 	 
		updateData.setString("country1", 0, rowToUpdate.getString("country1"));	  	 	 	 	 
		updateData.setString("fax1", 0, rowToUpdate.getString("fax1"));	 	 	 	 	 
		updateData.setString("phone1", 0, rowToUpdate.getString("phone1"));	 	 	 	 	 	 
		updateData.setString("tax_id1", 0, rowToUpdate.getString("tax_id1"));	 	 	 	 	 	 
		updateData.setString("account_number", 0, rowToUpdate.getString("account_number"));	 	 	 	 	 	 
		updateData.setString("statement_date", 0, rowToUpdate.getString("statement_date"));	 	 	 	 	 	 
		updateData.setString("reporting_unit", 0, rowToUpdate.getString("reporting_unit"));	 	 	 	 	 	 
		updateData.setString("preferred_currency", 0, rowToUpdate.getString("preferred_currency"));	 	 	 	 	 	 
		updateData.setString("metal", 0, rowToUpdate.getString("metal"));	 	 	 	 	 	 
		updateData.setDouble("avg_balance", 0, rowToUpdate.getDouble("avg_balance"));	  	 	 	 	 
		updateData.setDouble("avg_price", 0, rowToUpdate.getDouble("avg_price"));	 	 	 	 	 	 
		updateData.setDouble("metal_interest_rate", 0, rowToUpdate.getDouble("metal_interest_rate"));	 	 	 	 	 	 
		updateData.setDouble("value", 0, rowToUpdate.getDouble("value"));	 	 	 	 	 	 
		updateData.setString("long_name1", 0, rowToUpdate.getString("long_name1"));	 	 	 	 	 	 
		updateData.setString("addr12", 0, rowToUpdate.getString("addr12"));	 	 	 	 	 	 
		updateData.setString("addr22", 0, rowToUpdate.getString("addr22"));	 	 	 	 	 	 
		updateData.setString("addr_reference_name2", 0, rowToUpdate.getString("addr_reference_name2"));	  	 	 
		updateData.setString("irs_terminal_num2", 0, rowToUpdate.getString("irs_terminal_num2"));	 	 	 	 	 	 
		updateData.setString("city2", 0, rowToUpdate.getString("city2"));
		
		if(isCN)
		{

			Logging.info("Setting CN specific values back to the table");
			updateData.setDouble("avg_cny_rate", 0,rowToUpdate.getDouble("avg_cny_rate"));
			updateData.setDouble("net_interest_rate_new", 0,rowToUpdate.getDouble("net_interest_rate_new"));
			updateData.setDouble("gross_interest_rate_new", 0,rowToUpdate.getDouble("gross_interest_rate_new"));

			/*
			updateData.setString("county_id2", 0, rowToUpdate.getString("county_id2"));	
			updateData.setString("mail_code2", 0, rowToUpdate.getString("mail_code2"));	 
			updateData.setString("state_id2", 0, rowToUpdate.getString("state_id2"));
			*/
			ReferenceChoices county = staticFactory.getReferenceChoices(EnumReferenceTable.PsCounty);
			int countyId2 = rowToUpdate.getInt("county_id2");
			if(countyId2 > 0) {
				updateData.setString("county_id2", 0, county.getName(countyId2));	 	 	 	 	 	 
			} else {
				updateData.setString("county_id2", 0, "");	 	
			}
			
			updateData.setString("mail_code2", 0, rowToUpdate.getString("mail_code2"));	 
			
			ReferenceChoices states = staticFactory.getReferenceChoices(EnumReferenceTable.States);
			int stateId2 = rowToUpdate.getInt("state_id2");
			if(stateId2 >= 0) {
				updateData.setString("state_id2", 0,states.getName(stateId2));
			} else {
				updateData.setString("state_id2", 0, "");
			}
		
		}
		
		else
		{
			
			ReferenceChoices county = staticFactory.getReferenceChoices(EnumReferenceTable.PsCounty);
			int countyId2 = rowToUpdate.getInt("county_id2");
			if(countyId2 > 0) {
				updateData.setString("county_id2", 0, county.getName(countyId2));	 	 	 	 	 	 
			} else {
				updateData.setString("county_id2", 0, "");	 	
			}
			
			updateData.setString("mail_code2", 0, rowToUpdate.getString("mail_code2"));	 
			
			ReferenceChoices states = staticFactory.getReferenceChoices(EnumReferenceTable.States);
			int stateId2 = rowToUpdate.getInt("state_id2");
			if(stateId2 >= 0) {
				updateData.setString("state_id2", 0,states.getName(stateId2));
			} else {
				updateData.setString("state_id2", 0, "");
			}
			
		}
		
		updateData.setString("country2", 0, rowToUpdate.getString("country2"));	 	 	 	 	 	 
		updateData.setString("fax2", 0, rowToUpdate.getString("fax2"));	 	 	 	 	 	 
		updateData.setString("phone2", 0, rowToUpdate.getString("phone2"));	 	 	 	 	 	 
		updateData.setString("description", 0, rowToUpdate.getString("description"));	  	 	 	 	 
		
		userTable.updateRows(updateData, "month_stamp, account_name, metal_description");
		
		Logging.info("Table updated");
	}
	
	private int getTranNum(Context context, String reference)
	{
		
		String sql = "select tran_num from ab_tran where reference ='"+reference+"' and current_flag = 1 and tran_status = 3 ";
		Logging.info("SQL "+sql);

		Table sqlResult = context.getIOFactory().runSQL(sql);
		int tranNum = sqlResult.getInt(0, 0);
		Logging.info("Tran num "+tranNum);

		sqlResult.dispose();
		return tranNum;
		
	}
	public Field getField(String fieldName, Transaction cash)
	{
		Field tranField = cash.getField(fieldName);
 	   if (tranField != null && tranField.isApplicable()
     		   && tranField.isWritable()) {
 		   return tranField;
        } else {
     	   String errorMessage = 
     			   "Can't write on the transaction info field '" + fieldName + "'."
     			+  "\nPlease check if the transaction info field is set up correctly"
     			+  "in the instrument builder for the right instrument types"
     			;
     	   throw new RuntimeException (errorMessage);
        }
	}
	
	public void sleep(int sec)
	{
		long stopTime = System.currentTimeMillis()+(sec*1000);
		long currentTIme = System.currentTimeMillis();
		while (currentTIme < stopTime)
		{
			currentTIme = System.currentTimeMillis();
		}
		return;
	}
}
