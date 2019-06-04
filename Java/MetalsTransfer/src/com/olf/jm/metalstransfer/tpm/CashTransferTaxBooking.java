package com.olf.jm.metalstransfer.tpm;

import java.text.DecimalFormat;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.tpm.AbstractProcessStep;
import com.olf.jm.logging.Logging;
import com.olf.jm.metalstransfer.dealbooking.CashTransfer;
import com.olf.openjvs.OException;
import com.olf.openjvs.Tpm;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.backoffice.SettlementAccount;
import com.olf.openrisk.market.EnumBmo;
import com.olf.openrisk.market.Index;
import com.olf.openrisk.market.Market;
import com.olf.openrisk.staticdata.BusinessUnit;
import com.olf.openrisk.staticdata.Currency;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.LegalEntity;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.staticdata.Unit;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.Token;
import com.olf.openrisk.tpm.Variable;
import com.olf.openrisk.tpm.Variables;
import com.olf.openrisk.trading.Comment;
import com.olf.openrisk.trading.Comments;
import com.olf.openrisk.trading.EnumBuySell;
import com.olf.openrisk.trading.EnumCommentFieldId;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Instrument;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.misc.TableUtilities;

/**
 * Book cash transaction deals for the tax on a validated metal transfer strategy.
 *  
 * @author Gary Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 18-Nov-2015 |               | G. Moore        | Initial version.                                                                |
 * | 002 | 23-Nov-2015 |               | G. Moore        | Set the settle date on the tax deal to the trade date of the taxable deal.      |
 * | 003 | 30-Nov-2015 |               | G. Moore        | Handle cases where tax type has not been set on the taxable deal due to tax post|
 * |     |             |               |                 | not having been run yet.                                                        |
 * | 004 | 18-Dec-2015 |               | G. Moore        | Internal BU of tax deal is the internal BU which has the same country as the    |
 * |     |             |               |                 | holding bank of the to account.                                                 |
 * | 005 | 28-Jan-2016 |               | S. Curran       | Copy SAP id to all trades booked                                                |
 * | 006 | 21-Mar-2016 |               | J. Waechter	 | Fixed issue with retrieving tran num of transaction of the Cash deal			   |
 * |     |             |               |                 | that is not containing valid data                                               |
 * | 007 | 24-Mar-2016 |               | J. Waechter     | Fixed fix of Rev 006 									                       |
 * | 008 | 04-Apr-2016 |               | J. Waechter     | Fixed error handling for case of no tax type or no tax subtype 			       |
 * | 009 | 21-Apr-2016 |               | J. Waechter     | Added comment for back office invoices on tax deals                             |
 * |     |             |               |                 | The internal legal entity of the tax deal is retrieved from business unit       |
 * |     |             |               |                 | Added cancelling of deals in case if exception                                  | 
 * |     |             |               |                 | Moved validation of strategy to end of this plugin                              |
 * | 010 | 22-Apr-2016 |               | J. Waechter     | Now throwing exception in case of missing USD metal price                       |
 * | 011 | 25-Apr-2016 |			   | J. Waechter     | Now retrieving business unit based on country of legal entity instead of        |
 * |     |             |               |                 | business unit itself                                                            |
 * |     |             |               |                 | enhanced refresh                                                                |
 * | 012 | 03-May-2016 |               | J. Waechter     | Added refresh of single index in addition to market refresh                     | 
 * |     |             |               |                 | Added rounding to 5 decimals for the price in USD							   |
 * | 013 | 23-May-2016 |               | J. Waechter     | Enhanced logging                                                                |
 * | 014 | 04-Oct-2016 |               | S. Curran       | reapply changes made as part of EPI-5                                           |
 *  ----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.TpmStep })
public class CashTransferTaxBooking extends AbstractProcessStep {

	private int taxDealId;
	private String strategyRef;
	public static String US_TAX_JURISDICTION_BU = "US TAX";
	public static String US_INTERNAL_BU = "JM PMM US";

	/**
	 * Plugin entry point.
	 * {@inheritDoc}
	 */
	@Override
	public Table execute(Context context, Process process, Token token, Person submitter, boolean transferItemLocks, Variables variables) {
		int tranNum = process.getVariable("TranNum").getValueAsInt();

		try {
        	long wflowId = Tpm.getWorkflowId();
			Logging.init(context, this.getClass(), "MetalsTransfer", "UI");
			Logging.info("Processing transaction " + tranNum);
			String count = getVariable (wflowId, "CheckBookTaxDealCount");
			int countAsInt = Integer.parseInt(count);
    		Tpm.setVariable(wflowId, "CheckBookTaxDealCount", "" + Integer.toString(countAsInt+1));
			Table returnt = process(context, process, tranNum);
			Logging.info("Completed transaction " + tranNum);
    		Tpm.setVariable(wflowId, "CheckBookTaxDealCount", "" + 9999999);
			return returnt;
		}
		catch (OException ex) {
			try (Transaction strategy = context.getTradingFactory().retrieveTransactionById(tranNum)) {
				CashTransfer.cancelDeals(context, strategy);
			} catch (Throwable t) {
				Logging.error("Process failed and could not cancel booked deals for transaction  " + tranNum + ": ", t);
				throw t;
			}
			Logging.error("Process failed for transaction " + tranNum + ": ", ex);
			throw new RuntimeException (ex);
		} catch (RuntimeException e) {
			try (Transaction strategy = context.getTradingFactory().retrieveTransactionById(tranNum)) {
				CashTransfer.cancelDeals(context, strategy);
			} catch (Throwable t) {
				Logging.error("Process failed and could not cancel booked deals for transaction  " + tranNum + ": ", t);
				throw t;
			}
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

		// Retrieve the strategy and the taxable cash transfer deal associated with strategy
		try (Transaction strategy = context.getTradingFactory().retrieveTransactionById(tranNum);
				Transaction taxableDeal = retrieveTaxableCashTransferDeal(context, strategy);
				Market market = context.getMarket()) {
			if (taxableDeal == null) {
				//Logging.info(getLoggingPrefix() + "No taxable deal found, no tax needs to be assigned");
				Logging.info(String.format("No taxable deal found for strategy#%d, no tax needs to be assigned", strategy.getDealTrackingId()));
				return null;
			}
			
			this.taxDealId = taxableDeal.getDealTrackingId();
			this.strategyRef = taxableDeal.getValueAsString(EnumTransactionFieldId.ReferenceString);
			Logging.info(String.format("%s Taxable deal#%s found", getLoggingPrefix(), this.taxDealId));
			
			// Assume tax details have been set on taxable deal
			Variable var = process.getVariable("IsTaxDetailsAssigned");
			var.setValue(true);
			process.setVariable(var);

			// Force refresh of universal prices to ensure latest data
			market.refresh(false, true);
			market.loadUniversal();

			Logging.info(getLoggingPrefix() + "processing.");
			String metal = strategy.getField("Metal").getValueAsString();
			double amount = strategy.getField("Qty").getValueAsDouble();
			String unit = strategy.getField("Unit").getValueAsString();

			// Convert amount to TOz
			double amountTOz = convertToToz(context, unit, amount);

			// Get metal price and calculate value in USD
			double priceUsd = getMetalBasePriceUsd(context, market, metal);
			if (Math.abs(priceUsd) < 0.000001d) {
				throw new RuntimeException ("No metal base price for metal " + metal + " in USD available. Tax deals can't be booked");
			}            
			double amountUsd = amountTOz * priceUsd;

			// Get tax rate details
			try (Table taxRates = retrieveTaxRateDetails(context, taxableDeal)) {
				for (TableRow row : taxRates.getRows()) {
					int taxJusrisdictionId = row.getInt("party_id");
					double taxRate = row.getDouble("charge_rate");
					Logging.info(getLoggingPrefix() + "Looping through taxRate=" + taxRate + " for tax jusrisdiction=" + taxJusrisdictionId);
					
					if (taxRate > 0) {
						boolean skipBookingVATDeal = checkToBypassVATDealForPMMUS(context, strategy, taxJusrisdictionId);
						if (skipBookingVATDeal) {
							continue;
						}
						
						// Is tax a reverse charge?
						if (row.getInt("add_subtract_id") == 2) {
							taxRate *= -1;
						}

						// Calculate tax amount
						double taxAmountUsd = amountUsd * taxRate;

						// Get currency tax is to be paid in
						Currency taxCurrency = retrieveTaxCurrency(context, taxJusrisdictionId);

						Logging.info(getLoggingPrefix() + "Metal: " + metal + ", Amount: " + amount + ", Unit: " + unit);
						Logging.info(getLoggingPrefix() + "Amount (TOz): " + amountTOz + ", Price (USD): " + priceUsd + ", Amount (USD): " + amountUsd);
						Logging.info(getLoggingPrefix() + "Tax Rate: " + taxRate + ", Tax Amount (USD): " + taxAmountUsd);

						String formattedPriceUsd = new DecimalFormat("#.000").format(priceUsd);
						String comment = 
								"USD " + formattedPriceUsd + "/TOz"
										+ "\nExchange Rate %s"
										+ "\nVAT @ " + taxRate*100 + "%%"	
										;
						// Book the tax deal
						bookTaxDeal(context, market, strategy, taxableDeal, taxCurrency, taxAmountUsd, comment);                    
					} else {
						Logging.info(getLoggingPrefix() + "There is no tax to be paid");
					}
				}
			}
			
			Logging.info(String.format("%s Processing strategy#%s from %s to Validated", getLoggingPrefix(), tranNum, strategy.getTransactionStatus().getName()));
			CashTransfer.validateStrategy(context, strategy);
			Logging.info(String.format("%s Strategy#%s successfully processed to Validated", getLoggingPrefix(), tranNum));
			
		} catch (TaxDetailsAssignmentException e) {
			Logging.error(e.getLocalizedMessage(), e);
			// Taxable deal has not yet had its tax type set, set the TPM workflow flag
			Variable var = process.getVariable("IsTaxDetailsAssigned");
			var.setValue(false);
			process.setVariable(var);
			process.appendError(e.getLocalizedMessage());
		}
		return null;
	}

	/**
	 * To by-pass creation of VAT deal for PMM US - if internal BU is PMM US & Tax Jurisdiction is not "US TAX"
	 * 
	 * @param session
	 * @param strategy
	 * @param taxJusrisdictionId
	 * @return
	 */
	private boolean checkToBypassVATDealForPMMUS(Session session, Transaction strategy, int taxJusrisdictionId) {
		BusinessUnit intBU = getInternalBUnitForTax(session, strategy);
		BusinessUnit taxJurisdictionBU = session.getStaticDataFactory().getReferenceObject(BusinessUnit.class, taxJusrisdictionId);
		
		if (intBU.getName() != null && US_INTERNAL_BU.equalsIgnoreCase(intBU.getName())
				&& !US_TAX_JURISDICTION_BU.equalsIgnoreCase(taxJurisdictionBU.getName())) {
			Logging.info(getLoggingPrefix() + " Not creating a VAT deal as internal BU is " + intBU.getName()
					+ " & Tax Jurisdiction is " + taxJurisdictionBU.getName());
			return true;
		}
		return false;
	}
	
	/**
	 * Retrieve the taxable cash transfer deal associated with the strategy. This will be the sell deal with a business unit as the
	 * To A/C Bunit.
	 * 
	 * @param session
	 * @param strategy
	 * @return taxable deal
	 */
	private Transaction retrieveTaxableCashTransferDeal(Session session, Transaction strategy) {
		String toBunit = strategy.getField("To A/C BU").getValueAsString();
		int strategyNum = strategy.getDealTrackingId();
		
		String sql = "SELECT ab.tran_num" +
						"\n   FROM ab_tran ab" +
						"\n   JOIN ab_tran_info_view ativ ON (ativ.tran_num = ab.tran_num AND ativ.type_name = 'Strategy Num')" +
						"\n   JOIN party pa ON (pa.party_id = ab.external_bunit)" +
						"\n  WHERE ativ.value = '" + strategyNum + "'" +
						"\n    AND ab.ins_type = " + EnumInsType.CashInstrument.getValue() +
						"\n    AND ab.buy_sell = " + EnumBuySell.Sell.getValue() +
						"\n    AND ab.current_flag = 1" +
						"\n    AND ab.tran_status = " + EnumTranStatus.Validated.getValue() +
						"\n    AND pa.short_name = '" + toBunit.replace("'", "''") + "'";
		Logging.info(String.format("Executing SQL(to retrieve taxable CASH Transfer deal) for strategy#%d - %s", strategyNum, sql));
		
		try (Table results = session.getIOFactory().runSQL(sql)) {
			for (TableRow row : results.getRows()) {
				int tranNum = row.getInt(0);
				return session.getTradingFactory().retrieveTransactionById(tranNum);
			}
		}
		
		return null;
	}

	/**
	 * Convert the amount from the given unit to TOz.
	 * 
	 * @param session
	 * @param unit
	 * @param amount
	 * @return converted amount
	 */
	private double convertToToz(Session session , String unit, double amount) {
		try (Unit fromUnit = session.getStaticDataFactory().getReferenceObject(Unit.class, unit);
				Unit toUnit = session.getStaticDataFactory().getReferenceObject(Unit.class, "TOz")) {
			double factor = fromUnit.getConversionFactor(toUnit);
			return amount * factor;
		}
	}

	/**
	 * Retrieve the tax rate details based on the tax code settings on the taxable deal.
	 *  
	 * @param session
	 * @param taxableDeal
	 * @return table of tax rates or null if none defined  
	 */
	private Table retrieveTaxRateDetails(Session session, Transaction taxableDeal) {
		int taxTypeId = retrieveTaxTypeId (session, taxableDeal);
		int taxSubTypeId = retrieveTaxSubTypeId (session, taxableDeal);
		if (taxTypeId == -1 || taxSubTypeId == -1 ) {
			Logging.info(getLoggingPrefix(), "Could not find either Tax Type or Tax Subtype for deal#" + taxableDeal.getDealTrackingId());
			return session.getTableFactory().createTable("Empty placeholder used in case of no tax type / sub type");
		}
		
		String taxType = session.getStaticDataFactory().getName(EnumReferenceTable.TaxTranType, taxTypeId);
		String taxSubType = session.getStaticDataFactory().getName(EnumReferenceTable.TaxTranSubtype, taxSubTypeId);
		
		String sql = "SELECT tax.party_id, tax.charge_rate, add_subtract_id" +
						"\n   FROM tax_rate tax" +
						"\n   JOIN tax_tran_type_restrict ttt ON (ttt.tax_rate_id = tax.tax_rate_id)" +
						"\n   JOIN tax_tran_subtype_restrict tst ON (tst.tax_rate_id = tax.tax_rate_id)" +
						"\n  WHERE ttt.tax_tran_type_id = " + taxTypeId +
						"\n    AND tst.tax_tran_subtype_id = " + taxSubTypeId;
		Logging.info(String.format("%s Executing SQL(to retrieve TaxRate Details)->%s", getLoggingPrefix(), sql));
		
		try (Table rates = session.getIOFactory().runSQL(sql)) {
			if (rates.getRowCount() == 0) {
				Logging.info(getLoggingPrefix() + "No tax rate found for tax type " + taxType + " and sub type " + taxSubType);
				return rates.cloneData();
			}
			
			Logging.info(getLoggingPrefix() + "Charge_rate=" + rates.getDouble("charge_rate", 0) + " for tax type-" + taxType + " & sub type-" + taxSubType);
			return rates.cloneData();
		}
	}

	private int retrieveTaxSubTypeId(Session session, Transaction taxableDeal) {
		String sql = "SELECT abt.tax_tran_subtype" 
						+  "\n FROM ab_tran ab" 
						+  "\n     INNER JOIN ab_tran ab2 ON ab2.tran_group = ab.tran_group AND ab2.current_flag = 1"
						+  "\n     INNER JOIN ab_tran_tax abt ON abt.tran_num = ab2.tran_num"
						+  "\n  WHERE ab.deal_tracking_num = " + taxableDeal.getDealTrackingId() 
						+  "\n    AND abt.tax_tran_type = -1"
						+  "\n    AND ab.current_flag = 1";
		Logging.info(String.format("%s Executing SQL(to retrieve TaxSubTypeId)->%s", getLoggingPrefix(), sql));
		
		try (Table taxSubtype = session.getIOFactory().runSQL(sql)) {
			if (taxSubtype.getRowCount() == 0) {
				Logging.info(getLoggingPrefix() + "No tax sub type found for deal#" + taxableDeal.getDealTrackingId());
				return -1;
			}
			Logging.info(getLoggingPrefix() + "No. of TaxSubType rows retrieved:" + taxSubtype.getRowCount() + ", taxSubId(first row)=" + taxSubtype.getInt(0, 0));
			return taxSubtype.getInt(0, 0);
		}
	}

	private int retrieveTaxTypeId(Session session, Transaction taxableDeal) {
		String sql = "SELECT abt.tax_tran_type" 
						+  "\n FROM ab_tran ab" 
						+  "\n     INNER JOIN ab_tran ab2 ON ab2.tran_group = ab.tran_group AND ab2.current_flag = 1"
						+  "\n     INNER JOIN ab_tran_tax abt ON abt.tran_num = ab2.tran_num"
						+  "\n  WHERE ab.deal_tracking_num = " + taxableDeal.getDealTrackingId() 
						+  "\n    AND abt.tax_tran_subtype = -1"
						+  "\n    AND ab.current_flag = 1";
		Logging.info(String.format("%s Executing SQL(to retrieve TaxTypeId)->%s", getLoggingPrefix(), sql));
		
		try (Table taxType = session.getIOFactory().runSQL(sql)) {
			if (taxType.getRowCount() == 0) {
				Logging.info(getLoggingPrefix() + "No tax type found for deal#" + taxableDeal.getDealTrackingId());
				return -1;
			}
			Logging.info(getLoggingPrefix() + "No. of TaxType rows retrieved:" + taxType.getRowCount() + ", taxId(first row)=" + taxType.getInt(0, 0));
			return taxType.getInt(0, 0);
		}
	}

	/**
	 * Retrieve the currency that the tax must be paid in based on the jurisdiction.
	 * 
	 * @param session
	 * @param taxJusrisdictionId
	 * @return currency
	 */
	private Currency retrieveTaxCurrency(Session session, int taxJusrisdictionId) {
		String sql = "SELECT currency_id  FROM tax_jurisdiction WHERE party_id = " + taxJusrisdictionId;
		Logging.info(String.format("%s Executing SQL->%s", getLoggingPrefix(), sql));
		
		try (Table jurisdiction = session.getIOFactory().runSQL(sql)) {
			if (jurisdiction.getRowCount() == 1) {
				int currencyId = jurisdiction.getInt("currency_id", 0);
				return session.getStaticDataFactory().getReferenceObject(Currency.class, currencyId);
			}
			else if (jurisdiction.getRowCount() > 1) {
				StringBuilder sb = new StringBuilder ();
				boolean first = true;
				for (int row = jurisdiction.getRowCount()-1;row >= 0; row--) {
					if (!first) {
						sb.append(", ");
					}
					sb.append("#").append(jurisdiction.getInt("currency_id", row));
					first = false;
				}
				throw new RuntimeException("More than one tax jurisdiction found for party id " + taxJusrisdictionId
						+". Possible values (currency ids) are " + sb.toString());
			}
			throw new RuntimeException("No tax jurisdiction found for party id " + taxJusrisdictionId);
		}
	}

	/**
	 * Get the monetary price of the metal.
	 * 
	 * @param session
	 * @param market
	 * @param metal
	 * @return metal price
	 */
	private double getMetalBasePriceUsd(Session session, Market market, String metal) {
		Logging.info(getLoggingPrefix() + "Looking for " + metal + " metal USD price on JM_Base_Price index");
		try (Index basePriceIdx = market.getIndex("JM_Base_Price");
				) {
			basePriceIdx.loadUniversal();
			Table prices = basePriceIdx.getOutputTableByBmo(EnumBmo.Mid);
			//Logging.info(getLoggingPrefix() + "Got index and prices");
			Logging.info(getLoggingPrefix() + "Got universal prices for JM_Base_Price index");
			//            String hostName = session.getHostName();
			//            String time = "" + (new Date()).getTime();
			//        	prices.exportCsv(session.getSystemSetting("AB_OUTDIR")   + "\\Prices_" + hostName + "_" + time + ".csv", true);
			int row = prices.find(prices.getColumnId("Name"), metal, 0);
			if (row >= 0) {
				return prices.getDouble("Mid", row);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed getting base price for metal " + metal + " from index JM_Base_Price", e);
		}
		
		Logging.info(getLoggingPrefix() + "Price for " + metal + " was not found on JM_Base_Price index");
		Logging.info(getLoggingPrefix() + "Looking for " + metal + "/USD FX spot price");
		
		Currency metalCcy = session.getStaticDataFactory().getReferenceObject(Currency.class, metal);
		Currency usdCcy = session.getStaticDataFactory().getReferenceObject(Currency.class, "USD");
		return market.getFXSpotRate(metalCcy, usdCcy);
	}

	/**
	 * Get the internal business unit for the tax deal. This is the internal business unit that has the same country on it's default address
	 * as the holding bank of the 'to account'.
	 * 
	 * @param session Current session
	 * @param strategy Strategy deal
	 * @return internal business unit
	 */
	private BusinessUnit getInternalBUnitForTax(Session session, Transaction strategy) {
		String toAccName = strategy.getField("To A/C").getValueAsString();
		SettlementAccount account = session.getStaticDataFactory().getReferenceObject(SettlementAccount.class, toAccName);
		int holdingBankCountryId = retrieveCountryIdOfHoldingBank(session, account);
		return retrieveInternalBUnitOfCountry(session, holdingBankCountryId);
	}

	/**
	 * Get the country id from the default address of the holding bank of the account.
	 * 
	 * @param session Current session
	 * @param account Account
	 * @return country id
	 */
	private int retrieveCountryIdOfHoldingBank(Session session, SettlementAccount account) {
		String sql = "SELECT pa.short_name, pad.country"
						+ "\n FROM account acc"
						+ "\n JOIN party   pa  ON (pa.party_id = acc.holder_id)"
						+ "\n LEFT JOIN party_address pad ON (pad.party_id = pa.party_id)"
						+ "\n WHERE acc.account_id = " + account.getId()
						+ "\n    AND pad.default_flag = 1";
		Logging.info(String.format("%s Executing SQL(to retrieve CountryId of HoldingBank)->%s", getLoggingPrefix(), sql));
		
		try (Table data = session.getIOFactory().runSQL(sql)) {
			if (data.getRowCount() == 0) {
				throw new RuntimeException("No holding bank found for account " + account.getName());
			}
			if (data.getInt("country", 0) == 0) {
				throw new RuntimeException("No country has been selected for default address for party " + data.getString("short_name", 0));
			}
			return data.getInt("country", 0);
		}
	}

	/**
	 * Get the internal business unit that matches the country on it's default address.
	 *  
	 * @param session Current session
	 * @param countryId Country id
	 * @return internal business unit
	 */
	private BusinessUnit retrieveInternalBUnitOfCountry(Session session, int countryId) {
		String countryName = session.getStaticDataFactory().getName(EnumReferenceTable.Country, countryId);
		String sql = "SELECT bu.party_id"
						+ "\n FROM party   bu"
						+ "\n INNER JOIN party_relationship prl ON prl.business_unit_id = bu.party_id"
						+ "\n INNER JOIN party le ON le.party_id = prl.legal_entity_id"
						+ "\n LEFT JOIN party_address pad ON (pad.party_id = le.party_id)"
						+ "\n WHERE bu.party_class = 1" // Business Unit
						+ "\n  AND bu.int_ext = 0" // Internal
						+ "\n  AND pad.country = " + countryId
						+ "\n  AND pad.default_flag = 1";
		Logging.info(String.format("%s Executing SQL(to retrieve InternalBUnit of Country)->%s", getLoggingPrefix(), sql));
		
		try (Table data = session.getIOFactory().runSQL(sql)) {
			if (data.getRowCount() == 0) {
				throw new RuntimeException("No internal business unit found with country " + countryName);
			}
			
			if (data.getRowCount() > 1) {
				StringBuilder sb = new StringBuilder ();
				boolean first = true;
				for (int row = data.getRowCount()-1; row >= 0; row--) {
					if (!first) {
						sb.append(", ");
					}
					sb.append("#").append(data.getInt("party_id", row));
					first = false;
				}

				throw new RuntimeException("More than one internal business unit found with country " + countryName
						+ ". Possible values are: " + sb.toString());
			}
			if (data.getInt("party_id", 0) == 0) {
				throw new RuntimeException("No internal business unit found with default address with country " + countryName);
			}

			int bUnitId = data.getInt("party_id", 0);
			return session.getStaticDataFactory().getReferenceObject(BusinessUnit.class, bUnitId);
		}
	}

	/**
	 * Book the tax cash deal.
	 * 
	 * @param session
	 * @param market
	 * @param taxableDeal
	 * @param vatCurrency
	 * @param vatUsdAmount
	 * @param comment 
	 */
	private void bookTaxDeal(Session session, Market market, Transaction strategy, Transaction taxableDeal, Currency vatCurrency, double vatUsdAmount, String comment) {
		Logging.info(String.format("%s Retrieving fields for new TaxDeal of CashflowType=VAT", getLoggingPrefix()));
		TradingFactory tradeFactory = session.getTradingFactory();
		StaticDataFactory staticFactory = session.getStaticDataFactory();
		String reference = taxableDeal.getField(EnumTransactionFieldId.ReferenceString).getValueAsString();

		// Get the cash instrument for the vat currency and create cash transaction for the charges
		try (Instrument ins = tradeFactory.retrieveInstrumentByTicker(EnumInsType.CashInstrument, vatCurrency.getName());
				Transaction cash = tradeFactory.createTransaction(ins)) {

			// Cash deal gets same reference as taxable deal
			cash.setValue(EnumTransactionFieldId.ReferenceString, reference);
			cash.setValue(EnumTransactionFieldId.CashflowType, "VAT");

			// Business unit is internal business unit for the country of the to account
			BusinessUnit intBunit = getInternalBUnitForTax(session, strategy);
			LegalEntity[] intLentities = intBunit.getLegalEntities(true);
			if (intLentities == null || intLentities.length == 0) {
				String message = "Could not retrieve an authorized legal entity for the designated internal business unit " + intBunit.getName();
				throw new RuntimeException (message);
			}
			// Get details from taxable deal
			int lentityId = intLentities[0].getId();
			int extBunitId = taxableDeal.getValueAsInt(EnumTransactionFieldId.ExternalBusinessUnit);
			int extLentityId = taxableDeal.getValueAsInt(EnumTransactionFieldId.ExternalLegalEntity);

			// Get spot rate for currency conversion
			Currency USD = staticFactory.getReferenceObject(Currency.class, "USD");
			double spotRate = market.getFXSpotRate(USD, vatCurrency);
			spotRate = Math.round(spotRate*100000)/100000.0d;
			Logging.info(getLoggingPrefix() + "Using spot rate of " + spotRate + " giving tax amount of " + (vatUsdAmount * spotRate));

			// Set the cash deal fields
			cash.setValue(EnumTransactionFieldId.InternalBusinessUnit, intBunit.getId());
			cash.setValue(EnumTransactionFieldId.InternalLegalEntity, lentityId);
			cash.setValue(EnumTransactionFieldId.InternalPortfolio, retrieveFeePortfolioId(session, intBunit));
			cash.setValue(EnumTransactionFieldId.ExternalBusinessUnit, extBunitId);
			cash.setValue(EnumTransactionFieldId.ExternalLegalEntity, extLentityId);
			cash.setValue(EnumTransactionFieldId.Position, vatUsdAmount * spotRate);
			
			// EPI-151 reapply changes from EPI-5
			//cash.setValue(EnumTransactionFieldId.SettleDate, taxableDeal.getValueAsDate(EnumTransactionFieldId.TradeDate));
			cash.setValue(EnumTransactionFieldId.TradeDate, taxableDeal.getValueAsDate(EnumTransactionFieldId.TradeDate));
			cash.setValue(EnumTransactionFieldId.SettleDate, taxableDeal.getValueAsDate(EnumTransactionFieldId.SettleDate));
			// Set conversion factor used 
			Field ccyConvFxRate = cash.getField("Ccy Conv FX Rate");
			String spotRateFormatted = new DecimalFormat("#,##0.00000").format(spotRate);
			ccyConvFxRate.setValue(spotRateFormatted);

			// Set the strategy number
			Field strategyNum = cash.getField("Strategy Num");
			strategyNum.setValue(taxableDeal.getField("Strategy Num").getValueAsString());

			comment = String.format(comment, spotRateFormatted);
			Comments comments = cash.getComments();
			comments.addItem();
			Comment c = comments.getItem(0);
			c.setValue(EnumCommentFieldId.Type, "Invoice");
			c.setValue(EnumCommentFieldId.CommentsMultiLine, comment);

			Logging.info(String.format("%s Booking VAT deal for internalBU=%d, externalBU=%s, position=%s", getLoggingPrefix(), intBunit.getId(), extBunitId, (vatUsdAmount * spotRate)));
			cash.process(EnumTranStatus.Validated);
			Logging.info(getLoggingPrefix() + "Booked VAT deal#" + cash.getTransactionId());
		}
	}

	/**
	 * Retrieve the portfolio id of the fee portfolio for the business unit. The portfolio name is expected to end with 'Fees'.
	 * 
	 * @param factory
	 * @param bunit
	 * @return
	 */
	private int retrieveFeePortfolioId(Session session, BusinessUnit bunit) {
		String sql = "SELECT id_number" +
						"\n FROM portfolio p" +
						"\n JOIN party_portfolio pp ON (pp.portfolio_id = p.id_number)" +
						"\n WHERE p.name LIKE '% Fees'" +
						"\n AND pp.party_id = " + bunit.getId();
		Logging.info(String.format("%s Executing SQL(to retrieve Fee portfolioId)->%s", getLoggingPrefix(), sql));
		
		try (Table portfolio = session.getIOFactory().runSQL(sql)) {
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
					Logging.info("multiple portfolios found for business unit #" + bunit + ". "
							+ "Selecting the first portfolio out of the following list: " + sb.toString());
				}
				return portfolio.getInt(0, 0);

			}
		}
		throw new RuntimeException("No fees portfolio found for business unit " + bunit.getName());
	}

	private String getLoggingPrefix() {
		return "Tax Deal " + this.taxDealId + " (" + this.strategyRef + ") ";
	}
}
