package com.olf.jm.metalstransfer.tpm;

import java.text.DecimalFormat;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.tpm.AbstractProcessStep;
import com.olf.jm.logging.Logging;
import com.olf.jm.metalstransfer.dealbooking.CashTransfer;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.backoffice.SettlementAccount;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.market.Market;
import com.olf.openrisk.staticdata.BusinessUnit;
import com.olf.openrisk.staticdata.Currency;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.Token;
import com.olf.openrisk.tpm.Variable;
import com.olf.openrisk.tpm.Variables;
import com.olf.openrisk.trading.EnumBuySell;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Instrument;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;

/**
 * Book cash transaction deals for migrated strategies. Unlike the non migration version
 * the VAT amounts to set are not calculated based on the Cash Transfer deals. They are retrieved
 * from a a user table defined in the TPM process (variable "MigrInputUserTable", String type)
 * and are assumed to be in column "vatamt". The data type of the "vatamt" column is allowed to vary.
 * The correct row of the user table is selected by matching the reference of the strategy with 
 * the "deal_id" column of the user table.
 * The currency of the VAT deal is retrieved from the TPM variable "TaxCurrency" (String).
 * 
 * 
 * @author jwaechter
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 14-Apr-2016 |               | J. Waechter     | Created as copy of Rev 006 of CashTransferTaxBooking                            |
 * | 002 | 15-May-2016 |			   | J. Waechter     | Added "smart" retrieval logic to evaluate col type of vatamt column             |
 * |     |             |               |                 | Added validation of strategy													   |
 * | 003 | 17-May-2016 |               | J. Waechter     | No longer using Migr Id info field. Using Reference instead.                    |
 * | 004 | 23-May-2016 |               | J. Waechter     | Enhanced logging                                                                |
 * | 005 | 28-Jun-2016 |               | J. Waechter     | Updated method retrieveInternalBUnitOfCountry to retrieve                       |
 * |     |             |               |                 | internal business unit based on legal entity assigned to country                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@ScriptCategory({ EnumScriptCategory.TpmStep })
public class CashTransferTaxBookingMigr extends AbstractProcessStep {
	private int taxDealId;
	private String strategyRef;

	/**
	 * Plugin entry point.
	 * {@inheritDoc}
	 */
	@Override
	public Table execute(Context context, Process process, Token token, Person submitter, boolean transferItemLocks, Variables variables) {
		int tranNum = process.getVariable("TranNum").getValueAsInt();

		try {
			context.getDebug().flushCache();
			context.getTradingFactory().flushEventCache();
			Logging.init(context, this.getClass(), "MetalsTransfer", "UI");
			Logging.info("Processing transaction " + tranNum);
			Table returnt = process(context, process, tranNum);
			Logging.info("Completed transaction " + tranNum);
			return returnt;
		}
		catch (RuntimeException e) {
			Logging.error("Process failed for transaction " + tranNum + ": ", e);
			throw e;
		}
		finally {
			Logging.close();
		}
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
				Logging.info(getLoggingPrefix() + "No taxable deal found, no tax needs to be assigned");
			}
			// Assume tax details have been set on taxable deal
			Variable var = process.getVariable("IsTaxDetailsAssigned");
			var.setValue(true);
			process.setVariable(var);

			// Force refresh of universal prices to ensure latest data
			market.refresh(true, true);
			market.loadUniversal();

			Variable userTableVar = process.getVariable("MigrInputUserTable");
			String userTableName = userTableVar.getValueAsString();

			Variable taxCcyVar = process.getVariable("TaxCurrency");
			String taxCcy = taxCcyVar.getValueAsString();


			taxDealId = taxableDeal.getDealTrackingId();
			strategyRef = taxableDeal.getValueAsString(EnumTransactionFieldId.ReferenceString);

			Logging.info(getLoggingPrefix() + "processing.");

			String migrDealId = strategy.getField(EnumTransactionFieldId.ReferenceString).getValueAsString();

			int ccyId = context.getStaticDataFactory().getId(EnumReferenceTable.Currency, taxCcy);
			Currency taxCurrency = (Currency)context.getStaticDataFactory().getReferenceObject(EnumReferenceObject.Currency, ccyId);

			// Get tax rate details
			try (Table taxAmounts = retrieveTaxRateDetails(context, taxableDeal, userTableName, migrDealId, strategy.getDealTrackingId())) {
				for (TableRow row : taxAmounts.getRows()) {
					double taxAmount = 0.0d;
					int colNumAmt = taxAmounts.getColumnId("vatamt");
					EnumColType colType = taxAmounts.getColumnType(colNumAmt);
					switch (colType) {
					case Double:
						taxAmount = row.getDouble("vatamt");
						break;
					case String:
						String taxAmountAsString = row.getString("vatamt");
						try {
							taxAmount = Double.parseDouble(taxAmountAsString);
						} catch (NumberFormatException ex) {
							Logging.info ("Value in vatamt " + taxAmountAsString + " for migrDealId #" + migrDealId
									+ " is not a double. Skipping booking tax deal for this deal");
							throw ex;
						}
						break;       
					case Int:
						taxAmount = row.getInt("vatamt");
						break;
					case Long:
						taxAmount = row.getLong("vatamt");
						break;
					case UnsignedInt:
						taxAmount = row.getUnsignedInt("vatamt");
						break;
					case UnsignedLong:
						taxAmount = row.getUnsignedLong("vatamt").doubleValue();
						break;
					default:
						throw new RuntimeException ("Column type of column 'vatamt' of user table "
								+ userTableName + " for MigrDealId #" + migrDealId + " is not processable");
					}

					Logging.info(getLoggingPrefix() + "taxAmount(vatamt)=" + taxAmount);
					if (taxAmount > 0) {            
						// Book the tax deal
						int cashDealTrackingNum = bookTaxDeal(context, market, strategy, taxableDeal, taxCurrency, taxAmount);
						taxAmounts.setInt("booked_deal_num", row.getNumber(), cashDealTrackingNum);
						String message = "Tax deal booked successfully";
						taxAmounts.setString("status_msg", row.getNumber(), message);                   
					}
					else {
						String message = "Booking of tax deal skipped - vatamt is 0.0";
						taxAmounts.setString("status_msg", row.getNumber(), message);                                           	
						Logging.info(getLoggingPrefix() + "There is no tax to be paid");
					}
				}
				if (taxAmounts.getRowCount() > 0) {
					UserTable ut = context.getIOFactory().getUserTable(userTableName);
					ut.updateRows(taxAmounts, "row_id");
					ut.dispose();
				}
			}
			CashTransfer.validateStrategy(context, strategy);
		}
		catch (TaxDetailsAssignmentException e) {
			// Taxable deal has not yet had its tax type set, set the TPM workflow flag
			Variable var = process.getVariable("IsTaxDetailsAssigned");
			var.setValue(false);
			process.setVariable(var);
			process.appendError(e.getLocalizedMessage());
		}

		return null;
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
		try (Table results = session.getIOFactory().runSQL(
				"\n SELECT ab.tran_num" +
						"\n   FROM ab_tran ab" +
						"\n   JOIN ab_tran_info_view ativ ON (ativ.tran_num = ab.tran_num)" +
						"\n   JOIN party pa ON (pa.party_id = ab.external_bunit)" +
						"\n  WHERE ativ.type_name = 'Strategy Num'" +
						"\n    AND value = '" + strategyNum + "'" +
						"\n    AND ab.ins_type = " + EnumInsType.CashInstrument.getValue() +
						"\n    AND ab.buy_sell = " + EnumBuySell.Sell.getValue() +
						"\n    AND pa.short_name = '" + toBunit + "'" + 
				"\n	   AND ab.current_flag = 1")) {
			for (TableRow row : results.getRows()) {
				int tranNum = row.getInt(0);
				return session.getTradingFactory().retrieveTransactionById(tranNum);
			}
		}
		return null;
	}

	/**
	 * Retrieve the tax rate details based on the migration table "USER_migr_0_tfr"
	 * 
	 * @param session
	 * @param taxableDeal
	 * @param userTableName 
	 * @return table of tax rates or null if none defined  
	 */
	private Table retrieveTaxRateDetails(Session session, Transaction taxableDeal, String userTableName, String migrId, int stratDealNum) {
		String sql = 
				"\nSELECT u.*"
						+	"\nFROM " + userTableName + " u"
						+	"\nWHERE u.deal_id = '" + migrId + "'"
						;
		try (Table migrTaxTable = session.getIOFactory().runSQL(sql)) {
			if (migrTaxTable.getRowCount() == 0) {
				Logging.info(getLoggingPrefix() + "VAT data NOT found for cash transfer deal transaction #" + taxableDeal.getTransactionId() 
						+ " of strategy deal #" + stratDealNum + " of strategy ref " + strategyRef);
			}
			return migrTaxTable.cloneData();
		}
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
		try (Table data = session.getIOFactory().runSQL(""
				+ "\n SELECT pa.short_name, pad.country"
				+ "\n   FROM account acc"
				+ "\n   JOIN party   pa  ON (pa.party_id = acc.holder_id)"
				+ "\n   LEFT JOIN party_address pad ON (pad.party_id = pa.party_id)"
				+ "\n  WHERE acc.account_id = " + account.getId()
				+ "\n    AND pad.default_flag = 1")) {

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
	 * Get the internal business unit for a country. The internal business unit belongs
	 * to an internal legal entity having a default address such that the country of
	 * the default address is matches countryId.
	 *  
	 * @param session Current session
	 * @param countryId Country id
	 * @return internal business unit
	 */
	private BusinessUnit retrieveInternalBUnitOfCountry(Session session, int countryId) {
		String countryName = session.getStaticDataFactory().getName(EnumReferenceTable.Country, countryId);

		try (Table data = session.getIOFactory().runSQL(""
				+ "\n SELECT bu.party_id"
				+ "\n   FROM party   bu"
				+ "\n   INNER JOIN party_relationship prl ON prl.business_unit_id = bu.party_id"
				+ "\n   INNER JOIN party le ON le.party_id = prl.legal_entity_id"
				+ "\n   LEFT JOIN party_address pad ON (pad.party_id = le.party_id)"
				+ "\n  WHERE bu.party_class = 1" // Business Unit
				+ "\n    AND bu.int_ext = 0" // Internal
				+ "\n    AND pad.country = " + countryId
				+ "\n    AND pad.default_flag = 1")) {

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
	 */
	private int bookTaxDeal(Session session, Market market, Transaction strategy, Transaction taxableDeal, Currency vatCurrency, double vatUsdAmount) {

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

			// Get details from taxable deal
			int lentityId = taxableDeal.getValueAsInt(EnumTransactionFieldId.InternalLegalEntity);
			int extBunitId = taxableDeal.getValueAsInt(EnumTransactionFieldId.ExternalBusinessUnit);
			int extLentityId = taxableDeal.getValueAsInt(EnumTransactionFieldId.ExternalLegalEntity);

			// Get spot rate for currency conversion
			Currency USD = staticFactory.getReferenceObject(Currency.class, "USD");
			double spotRate = market.getFXSpotRate(USD, vatCurrency);
			Logging.info(getLoggingPrefix() + "Using spot rate of " + spotRate + " giving tax amount of " + (vatUsdAmount * spotRate));

			// Set the cash deal fields
			cash.setValue(EnumTransactionFieldId.InternalBusinessUnit, intBunit.getId());
			cash.setValue(EnumTransactionFieldId.InternalLegalEntity, lentityId);
			cash.setValue(EnumTransactionFieldId.InternalPortfolio, retrieveFeePortfolioId(session, intBunit));
			cash.setValue(EnumTransactionFieldId.ExternalBusinessUnit, extBunitId);
			cash.setValue(EnumTransactionFieldId.ExternalLegalEntity, extLentityId);
			cash.setValue(EnumTransactionFieldId.Position, vatUsdAmount * spotRate);
			cash.setValue(EnumTransactionFieldId.SettleDate, taxableDeal.getValueAsDate(EnumTransactionFieldId.TradeDate));

			// Set conversion factor used 
			Field ccyConvFxRate = cash.getField("Ccy Conv FX Rate");
			ccyConvFxRate.setValue(new DecimalFormat("#,##0.000000").format(spotRate));

			// Set the strategy number
			Field strategyNum = cash.getField("Strategy Num");
			strategyNum.setValue(taxableDeal.getField("Strategy Num").getValueAsString());

			Logging.info(getLoggingPrefix() + "Booking VAT deal");
			cash.process(EnumTranStatus.Validated);
			Logging.info(getLoggingPrefix() + "Booked VAT deal " + cash.getTransactionId());
			return cash.getDealTrackingId();
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

		try (Table portfolio = session.getIOFactory().runSQL(
				"\n SELECT id_number" +
						"\n   FROM portfolio p" +
						"\n   JOIN party_portfolio pp ON (pp.portfolio_id = p.id_number)" +
						"\n  WHERE p.name LIKE '% Fees'" +
						"\n    AND pp.party_id = " + bunit.getId())) {
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
		return "Tax Deal " + taxDealId + " (" + strategyRef + ") ";
	}
}
