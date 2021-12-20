package com.matthey.pmm.toms.service.mock.testdata;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.Order;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.transport.CreditCheckTo;
import com.matthey.pmm.toms.transport.DatabaseFileTo;
import com.matthey.pmm.toms.transport.EmailTo;
import com.matthey.pmm.toms.transport.FillTo;
import com.matthey.pmm.toms.transport.IndexTo;
import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.OrderCommentTo;
import com.matthey.pmm.toms.transport.OrderTo;
import com.matthey.pmm.toms.transport.PartyTo;
import com.matthey.pmm.toms.transport.ReferenceOrderLegTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;
import com.matthey.pmm.toms.transport.UserTo;

import liquibase.change.custom.CustomSqlChange;
import liquibase.database.Database;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.RawSqlStatement;

public class InsertTestValues implements CustomSqlChange {

	@Override
	public String getConfirmationMessage() {
		return "Test Data from Enums Stored";
	}

	@Override
	public void setUp() throws SetupException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFileOpener(ResourceAccessor resourceAccessor) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ValidationErrors validate(Database database) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public SqlStatement[] generateStatements(Database database) throws CustomChangeException {
		List<SqlStatement> allStatements = testLenitInsert (database);
		allStatements.addAll(testBunitInsert(database));
		allStatements.addAll(testCreditCheckInsert(database));
		allStatements.addAll(testUserInsert(database));
		allStatements.addAll(testDatabaseFileInsert(database));	
		allStatements.addAll(testEmailInsert(database));	
		allStatements.addAll(testFillInsert(database));	
		allStatements.addAll(testIndexInsert(database));	
		allStatements.addAll(testOrderCommentInsert(database));
		allStatements.addAll(testReferenceOrderLegInsert(database));
		allStatements.addAll(testReferenceOrderInsert(database));
		allStatements.addAll(testLimitOrderInsert(database));
		
		return allStatements.toArray(new SqlStatement[allStatements.size()]);
	}
	
	private Collection<? extends SqlStatement> testLimitOrderInsert(Database database) {
		List<SqlStatement> results = new ArrayList<> (TestLimitOrder.values().length*10);
		String insertTemplate = 
				"INSERT INTO dbo.limit_order (order_id, version, settle_date, start_date_concrete, start_date_symbolic_reference_id, limit_price, price_type_reference_id,"
											      + "part_fillable_reference_id, stop_trigger_type_reference_id, currency_cross_metal_reference_id, execution_likelihood,"
											      + "validation_type_reference_id, expiry_date) VALUES (%s, %s, %s, %s, %s, %s, %s, "
											      													+	"%s, %s, %s, %s,"
											      													+   "%s, %s)";
		for (LimitOrderTo  order : TestLimitOrder.asList()) {
			results.addAll(insertAbstractOrder (database, order));
			results.add(new RawSqlStatement(String.format(insertTemplate, order.id(), order.version(), order.settleDate() != null?database.getDateLiteral(order.settleDate()):null, order.startDateConcrete() != null?database.getDateLiteral(order.startDateConcrete()):null, order.idStartDateSymbolic(), order.limitPrice(), order.idPriceType(),
					order.idYesNoPartFillable(), order.idStopTriggerType(), order.idCurrencyCrossMetal(), order.executionLikelihood(),
					order.idValidationType(), order.expiryDate() != null?database.getDateLiteral(order.expiryDate()):null)));
		}
		return results;
	}

	private Collection<? extends SqlStatement> testReferenceOrderInsert(Database database) {
		List<SqlStatement> results = new ArrayList<> (TestReferenceOrder.values().length*10);
		String insertTemplate = 
				"INSERT INTO dbo.reference_order (order_id, version, metal_price_spread, fx_rate_spread, contango_backwardation) VALUES (%s, %s, %s, %s, %s)";
		String insertTemplateLeg = 
				"INSERT INTO dbo.reference_order_leg_map (order_id, version, leg_id) VALUES (%s, %s, %s)";
		for (ReferenceOrderTo  order: TestReferenceOrder.asList()) {
			results.addAll(insertAbstractOrder (database, order));
			results.add(new RawSqlStatement(String.format(insertTemplate, order.id(), order.version(), order.metalPriceSpread(), order.fxRateSpread(), order.contangoBackwardation())));
			for (long id : order.legIds()) {
				results.add(new RawSqlStatement(String.format(insertTemplateLeg, order.id(), order.version(), id)));				
			}
		}		
		
		return results;
	}
	
	private Collection<? extends SqlStatement> insertAbstractOrder(Database database, OrderTo order) {
		List<SqlStatement> results = new ArrayList<> ((order.fillIds()!= null?order.fillIds().size():0) 
				+ (order.orderCommentIds() != null?order.orderCommentIds().size():0) 
				+ (order.creditChecksIds() != null?order.creditChecksIds().size():0) 
				+ 1);
		String insertTemplate = 
				"INSERT INTO dbo.abstract_order (order_id, version, order_type_reference_id, internal_bunit_id, external_bunit_id, internal_legal_entity_id, external_legal_entity_id, internal_portfolio_id,"
											+   " external_portfolio_id, buy_sell_reference_id, base_currency_reference_id, base_quantity, base_quantity_unit_reference_id, term_currency_reference_id, "
											+   " order_status_id, created_at, created_by, last_update, updated_by, reference, metal_form_reference_id, metal_location_reference_id, fill_percentage, "
											+   " contract_type_reference_id, ticker_reference_id) "
											+   "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, "
											+           "%s, %s, %s, %s, %s, %s,"
											+           "%s, %s, %s, %s, %s, '%s', %s, %s, %s,"
											+ 			"%s, %s)";
		String insertTemplateToComment = 
				"INSERT INTO dbo.order_comment_map (order_id, version, order_comment_id) VALUES (%s, %s, %s)";
		String insertTemplateToFills = 
				"INSERT INTO dbo.order_fills_map (order_id, version, fill_id) VALUES (%s, %s, %s)";
		String insertTemplateToCreditChecks = 
				"INSERT INTO dbo.order_credit_check_map (order_id, version, credit_check_id) VALUES (%s, %s, %s)";
		
		results.add(new RawSqlStatement(String.format(insertTemplate, order.id(), order.version(), order.idOrderType(), order.idInternalBu(), order.idExternalBu(), order.idInternalLe(), order.idExternalLe(), order.idIntPortfolio(),
				order.idExtPortfolio(), order.idBuySell(), order.idBaseCurrency(), order.baseQuantity(), order.idBaseQuantityUnit(), order.idTermCurrency(),
				order.idOrderStatus(), database.getDateLiteral(order.createdAt()), order.idCreatedByUser(), database.getDateLiteral(order.lastUpdate()), order.idUpdatedByUser(), order.reference(), order.idMetalForm(), order.idMetalLocation(), order.fillPercentage(),
				order.idContractType(), order.idTicker())));
		if (order.orderCommentIds() != null) {
			for (Long id : order.orderCommentIds()) {
				results.add(new RawSqlStatement(String.format(insertTemplateToComment, order.id(), order.version(), id)));
			}			
		}
		if (order.fillIds() != null) {			
			for (Long id : order.fillIds()) {
				results.add(new RawSqlStatement(String.format(insertTemplateToFills, order.id(), order.version(), id)));
			}
		}
		if (order.creditChecksIds() != null) {
			for (Long id : order.creditChecksIds()) {
				results.add(new RawSqlStatement(String.format(insertTemplateToCreditChecks, order.id(), order.version(), id)));
			}			
		}
		return results;
	}

	private Collection<? extends SqlStatement> testReferenceOrderLegInsert(Database database) {
		List<SqlStatement> results = new ArrayList<> (TestReferenceOrderLeg.values().length);
		String insertTemplate = 
				"INSERT INTO dbo.reference_order_leg (leg_id, fixing_start_date, fixing_end_date, payment_offset_reference_id, notional, settle_currency_reference_id, ref_source_reference_id, fx_index_ref_source_reference_id) VALUES (%s, %s, %s, %s, %s, %s, %s, %s)";
		for (ReferenceOrderLegTo leg : TestReferenceOrderLeg.asList()) {
			results.add(new RawSqlStatement(String.format(insertTemplate, leg.id(), leg.fixingStartDate() != null?database.getDateLiteral(leg.fixingStartDate()):null,
					leg.fixingEndDate() != null?database.getDateLiteral(leg.fixingEndDate()):null,
					leg.idPaymentOffset(), leg.notional(), leg.idSettleCurrency(), leg.idRefSource(), leg.idFxIndexRefSource())));
		}
		return results;		
	}

	private Collection<? extends SqlStatement> testOrderCommentInsert(Database database) {
		List<SqlStatement> results = new ArrayList<> (TestCreditCheck.values().length);
		String insertTemplate = 
				"INSERT INTO dbo.order_comment (order_comment_id, comment_text, created_at, created_by_user_id, last_update, updated_by_user_id, lifecycle_status_reference_id) VALUES (%s, '%s', %s, %s, %s, %s, %s)";
		for (OrderCommentTo comment : TestOrderComment.asList()) {
			Timestamp createdAt = convertDateTimeStringToTimestamp (comment, comment.createdAt()); 
			Timestamp lastUpdate = convertDateTimeStringToTimestamp (comment, comment.lastUpdate()); 
			results.add(new RawSqlStatement(String.format(insertTemplate, comment.id(), comment.commentText(), 
					database.getDateTimeLiteral(createdAt), comment.idCreatedByUser(),
					database.getDateTimeLiteral(lastUpdate), comment.idCreatedByUser(),
					comment.idLifeCycle())));
		}
		return results;		
	}

	private Collection<? extends SqlStatement> testIndexInsert(Database database) {
		List<SqlStatement> results = new ArrayList<> (TestIndex.values().length);
		String insertTemplate = 
				"INSERT INTO dbo.index (index_id, reference_index_name_id, reference_currency_one_id, reference_currency_two_id, reference_lifecycle_status_id, sort_column) VALUES (%s, %s, %s, %s, %s, %s)";
		for (IndexTo index : TestIndex.asList()) {
			results.add(new RawSqlStatement(String.format(insertTemplate, index.id(), index.idIndexName(), index.idCurrencyOneName(), index.idCurrencyTwoName(),
					index.idLifecycle(), index.sortColumn())));
		}
		return results;		
	}

	private Collection<? extends SqlStatement> testFillInsert(Database database) {
		List<SqlStatement> results = new ArrayList<> (TestFill.values().length);
		String insertTemplate = 
				"INSERT INTO dbo.fill (fill_id, fill_quantity, fill_price, trade_id, trader_user_id, updated_by_user_id, last_update, fill_status_reference_id, error_message) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, '%s')";
		for (FillTo fill : TestFill.asList()) {
			Timestamp lastUpdate = convertDateTimeStringToTimestamp (fill, fill.lastUpdateDateTime()); 
			results.add(new RawSqlStatement(String.format(insertTemplate, fill.id(), fill.fillQuantity(), fill.fillPrice(), fill.idTrade(), fill.idTrader(),
					fill.idUpdatedBy(), database.getDateTimeLiteral(lastUpdate),
					fill.idFillStatus(), fill.errorMessage())));
		}
		return results;
	}

	private Collection<? extends SqlStatement> testEmailInsert(Database database) {
		List<SqlStatement> results = new ArrayList<> (TestEmail.values().length*20);
		String insertTemplate = 
				"INSERT INTO dbo.email (email_id, subject, body, send_as_user_id, email_status_reference_id, error_message, retry_count, created_at, created_by, last_update, updated_by) VALUES (%s, '%s', '%s', %s, %s, '%s', %s, %s, %s, %s, %s)";
		String insertTemplateTo = 
				"INSERT INTO dbo.email_to (email_id, to) VALUES (%s, '%s')";
		String insertTemplateCc = 
				"INSERT INTO dbo.email_cc (email_id, cc) VALUES (%s, '%s')";
		String insertTemplateBcc = 
				"INSERT INTO dbo.email_bcc (email_id, bcc) VALUES (%s, '%s')";
		String insertTemplateAttachments = 
				"INSERT INTO dbo.email_attachments (email_id, database_file_id) VALUES (%s, %s)";
		String insertTemplateOrders = 
				"INSERT INTO dbo.email_order_map (email_id, order_id) VALUES (%s, %s)";
		
		for (EmailTo email : TestEmail.asList()) {
			Timestamp createdAt = convertDateTimeStringToTimestamp (email, email.createdAt()); 
			Timestamp lastUpdated = convertDateTimeStringToTimestamp (email, email.lastUpdate()); 
			results.add(new RawSqlStatement(String.format(insertTemplate, email.id(), email.subject(), email.body(), email.idSendAs(), email.idEmailStatus(),
					email.errorMessage(), email.retryCount(), 
					database.getDateTimeLiteral(createdAt), email.idCreatedByUser(),
					database.getDateTimeLiteral(lastUpdated), email.idUpdatedByUser())));
			for (String value : email.toList()) {
				results.add(new RawSqlStatement(String.format(insertTemplateTo, email.id(), value)));
			}
			for (String value : email.ccList()) {
				results.add(new RawSqlStatement(String.format(insertTemplateCc, email.id(), value)));
			}
			for (String value : email.bccList()) {
				results.add(new RawSqlStatement(String.format(insertTemplateBcc, email.id(), value)));
			}
			for (Long id : email.attachments()) {
				results.add(new RawSqlStatement(String.format(insertTemplateAttachments, email.id(), id)));
			}
			for (Long id : email.associatedOrderIds()) {
				results.add(new RawSqlStatement(String.format(insertTemplateOrders, email.id(), id)));
			}			
		}
		return results;
	}

	private Collection<? extends SqlStatement> testDatabaseFileInsert(Database database) {
		List<SqlStatement> results = new ArrayList<> (TestDatabaseFile.values().length);
		String insertTemplate = 
				"INSERT INTO dbo.database_file (database_file_id, name, path, file_type_reference_id, reference_lifecycle_status_id, file_content, created_at, created_by, last_update, updated_by) VALUES (%s, '%s', '%s', %s, %s, %s, %s, %s, %s, %s)";
		for (DatabaseFileTo file : TestDatabaseFile.asList()) {
			Timestamp createdAt = convertDateTimeStringToTimestamp (file, file.createdAt()); 
			Timestamp lastUpdated = convertDateTimeStringToTimestamp (file, file.lastUpdate()); 
			results.add(new RawSqlStatement(String.format(insertTemplate, file.id(), file.name(), file.path(), file.idFileType(), file.idLifecycle(),
					"'" + bytesToHex(file.fileContent().getBytes()) + "'",
					database.getDateTimeLiteral(createdAt), file.idCreatedByUser(),
					database.getDateTimeLiteral(lastUpdated), file.idUpdatedByUser())));
		}
		return results;
	}

	private List<SqlStatement> testUserInsert(Database database) {
		List<SqlStatement> results = new ArrayList<> (TestUser.values().length*100);
		String insertTemplate = 
				"INSERT INTO dbo.user (user_id, email, first_name, last_name, role_id, reference_lifecycle_status_id, default_internal_bunit_id, default_internal_portfolio_reference_id) VALUES (%s, '%s', '%s', '%s', %s, %s, %s, %s)";
		String insertTemplateTradeableParties = 
				"INSERT INTO dbo.user_tradeable_parties (user_id, party_id) VALUES (%s, %s)";
		String insertTemplateTradeablePortfolios = 
				"INSERT INTO dbo.user_tradeable_portfolios (user_id, reference_portfolio_id) VALUES (%s, %s)";
		for (UserTo user : TestUser.asList()) {
			results.add(new RawSqlStatement(String.format(insertTemplate, user.id(), user.email(), user.firstName(), 
					user.lastName(), user.roleId(), user.idLifecycleStatus(),
					user.idDefaultInternalBu(), user.idDefaultInternalPortfolio())));
			for (Long id : user.tradeableCounterPartyIds()) {
				results.add(new RawSqlStatement(String.format(insertTemplateTradeableParties, user.id(), id)));
			}
			for (Long id : user.tradeableInternalPartyIds()) {
				results.add(new RawSqlStatement(String.format(insertTemplateTradeableParties, user.id(), id)));
			}
			for (Long id : user.tradeablePortfolioIds()) {
				results.add(new RawSqlStatement(String.format(insertTemplateTradeablePortfolios, user.id(), id)));
			}
		}
		return results;
	}

	private Collection<SqlStatement> testCreditCheckInsert(Database database) {
		List<SqlStatement> results = new ArrayList<> (TestCreditCheck.values().length);
		String insertTemplate = 
				"INSERT INTO dbo.credit_check (credit_check_id, party_id, credit_limit, run_date_time, reference_credit_check_run_status_id, reference_credit_check_outcome_id) VALUES (%s, %s, %s, %s, %s, %s)";
		for (CreditCheckTo cc : TestCreditCheck.asList()) {
			Timestamp runDateTime = convertDateTimeStringToTimestamp (cc, cc.runDateTime()); 
			results.add(new RawSqlStatement(String.format(insertTemplate, cc.id(), cc.idParty(),cc.creditLimit(), runDateTime!=null?database.getDateTimeLiteral(runDateTime):null, cc.idCreditCheckRunStatus(), cc.idCreditCheckOutcome())));
		}
		return results;
	}

	private List<SqlStatement> testBunitInsert(Database database) {
		return insertParties (TestBunit.asList());
	}


	private List<SqlStatement> testLenitInsert(Database database) {
		return insertParties (TestLenit.asList());
	}
	
	private List<SqlStatement> insertParties(List<PartyTo> parties) {
		List<SqlStatement> results = new ArrayList<> (TestBunit.values().length);
		String insertTemplate = 
				"INSERT INTO dbo.party (party_id, name, reference_party_type_id, legal_entity_id, reference_lifecycle_status_id, sort_column) VALUES (%s, '%s', %s, %s, %s, %s)";
		for (PartyTo party : parties) {
			results.add(new RawSqlStatement(String.format(insertTemplate, party.id(), party.name(), party.typeId(), party.idLegalEntity() != 0?party.idLegalEntity():null, party.idLifecycle(), party.sortColumn())));
		}
		return results;
	}
	
	private Timestamp convertDateTimeStringToTimestamp (Object to, String dateTimeString) {
		if (dateTimeString != null) {
			return new Timestamp (parseDateTime(to, dateTimeString).getTime());			
		}
		return null;
	}
	
	private Date parseDateTime (Object to, String dateTime) {
		if (dateTime != null) {
			SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
			try {
				return sdfDateTime.parse(dateTime);
			} catch (ParseException e) {
				throw new RuntimeException ("Error while converting entity '" 
						+ to + "', DateTime: '" + dateTime + "'. Expected Date format is "
						 + TomsService.DATE_TIME_FORMAT);
			}			
		}
		return null;
	}
	
	private Date parseDate (Object to, String date) {
		if (date != null) {
			SimpleDateFormat sdfDateTime = new SimpleDateFormat (TomsService.DATE_FORMAT);
			try {
				return sdfDateTime.parse(date);
			} catch (ParseException e) {
				throw new RuntimeException ("Error while converting entity '" 
						+ to + "', Date: '" + date + "'. Expected Date format is "
						 + TomsService.DATE_FORMAT);
			}			
		} 
		return null;
	}
	
	private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
	public static String bytesToHex(byte[] bytes) {
	    byte[] hexChars = new byte[bytes.length * 2];
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = HEX_ARRAY[v >>> 4];
	        hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars, StandardCharsets.UTF_8);
	}
}
