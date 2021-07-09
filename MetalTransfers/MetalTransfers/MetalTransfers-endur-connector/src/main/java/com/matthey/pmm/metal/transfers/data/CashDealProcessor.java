package com.matthey.pmm.metal.transfers.data;

import com.google.common.base.Stopwatch;
import com.matthey.pmm.metal.transfers.CashDeal;
import com.matthey.pmm.metal.transfers.ImmutableCashDeal;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Instrument;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.matthey.pmm.metal.transfers.data.CashDealColumns.CCY_COL;
import static com.matthey.pmm.metal.transfers.data.CashDealColumns.CFLOW_TYPE_COL;
import static com.matthey.pmm.metal.transfers.data.CashDealColumns.EXT_BU_COL;
import static com.matthey.pmm.metal.transfers.data.CashDealColumns.EXT_PFOLIO_COL;
import static com.matthey.pmm.metal.transfers.data.CashDealColumns.FX_RATE_COL;
import static com.matthey.pmm.metal.transfers.data.CashDealColumns.INT_BU_COL;
import static com.matthey.pmm.metal.transfers.data.CashDealColumns.INT_PFOLIO_COL;
import static com.matthey.pmm.metal.transfers.data.CashDealColumns.IS_CN_AND_HAS_VAT_COL;
import static com.matthey.pmm.metal.transfers.data.CashDealColumns.POS_COL;
import static com.matthey.pmm.metal.transfers.data.CashDealColumns.SETTLE_DATE_COL;
import static com.matthey.pmm.metal.transfers.data.CashDealColumns.STATEMENT_DATE_COL;
import static com.matthey.pmm.metal.transfers.data.CashDealColumns.TRAN_NUM_COL;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class CashDealProcessor {

    private static final String DEAL_BOOKING_WORKFLOW = "Cash Deal Booking";
    private static final String DEAL_BOOKING_TABLE = "USER_metal_rentals_deal_booking";
    private static final Logger logger = LogManager.getLogger(CashDealProcessor.class);
    private static final int BATCH_COUNT = 9;

    private final Session session;

    public CashDealProcessor(Session session) {
        this.session = session;
    }

    public List<CashDeal> getCashDeals() {
        ArrayList<CashDeal> cashDeals = new ArrayList<>();
        try (Table table = getCurrentCashDeals()) {
            for (TableRow row : table.getRows()) {
                int tranNum = row.getInt(TRAN_NUM_COL);
                try (Transaction tran = session.getTradingFactory().retrieveTransaction(tranNum)) {
                    double amountWithVat = tran.getField("Amount with VAT").getValueAsDouble();
                    double position = tran.getValueAsDouble(EnumTransactionFieldId.Position);
                    String internalBU = tran.getValueAsString(EnumTransactionFieldId.InternalBusinessUnit);

                    CashDeal cashDeal = ImmutableCashDeal.builder()
                            .currency(tran.getValueAsString(EnumTransactionFieldId.Currency))
                            .cashflowType(tran.getValueAsString(EnumTransactionFieldId.CashflowType))
                            .internalBU(internalBU)
                            .internalPortfolio(tran.getValueAsString(EnumTransactionFieldId.InternalPortfolio))
                            .externalBU(tran.getValueAsString(EnumTransactionFieldId.ExternalBusinessUnit))
                            .settleDate(LocalDate.fromDateFields(tran.getValueAsDate(EnumTransactionFieldId.SettleDate))
                                                .toString())
                            .statementDate(tran.getField("Util Statement Date").getValueAsString())
                            .position(amountWithVat == 0 ? position : amountWithVat)
                            .externalPortfolio(tran.getValueAsString(EnumTransactionFieldId.ExternalPortfolio))
                            .fxRate(tran.getField("Ccy Conv FX Rate").getValueAsDouble())
                            .tranNum(tran.getValueAsString(EnumTransactionFieldId.TransactionId))
                            .build();
                    cashDeals.add(cashDeal);
                }
            }
        }
        return cashDeals;
    }

    public void cancelAllCashDeals() {
        try (Table table = getCurrentCashDeals()) {
            for (TableRow row : table.getRows()) {
                int tranNum = row.getInt(TRAN_NUM_COL);
                try (Transaction tran = session.getTradingFactory().retrieveTransaction(tranNum)) {
                    // offset deals might already cancelled
                    if (!tran.getValueAsString(EnumTransactionFieldId.TransactionStatus).equals("Cancelled")) {
                        tran.process(EnumTranStatus.Cancelled);
                    }
                }
            }
        }
    }

    public Table getCurrentCashDeals() {
        //language=TSQL
        String sqlTemplate = "SELECT t.tran_num, p.short_name AS internal_bu\n" +
                             "    FROM ab_tran t\n" +
                             "             JOIN cflow_type ct\n" +
                             "                  ON ct.id_number = t.cflow_type\n" +
                             "             JOIN trans_status s\n" +
                             "                  ON t.tran_status = s.trans_status_id\n" +
                             "             JOIN party p\n" +
                             "                  ON p.party_id = t.internal_bunit\n" +
                             "    WHERE ct.name LIKE 'Metal Rentals - %'\n" +
                             "      AND s.name = 'Validated'\n" +
                             "      AND settle_date > '${currentDate}'\n";
        Map<String, String> variables = new HashMap<>();
        variables.put("currentDate", getCurrentDate());
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);

        return session.getIOFactory().runSQL(sql);
    }

    public String getCurrentDate() {
        return LocalDate.fromDateFields(session.getTradingDate()).toString();
    }

    public boolean isDealBookingRunning() {
        return session.getControlFactory().getWorkflow(DEAL_BOOKING_WORKFLOW).isRunning();
    }

    public void startDealBooking(List<CashDeal> deals) {
        try (UserTable dealTable = session.getIOFactory().getUserTable(DEAL_BOOKING_TABLE);
             Table changes = dealTable.retrieveTable().cloneStructure()) {
            dealTable.clearRows();
            for (CashDeal deal : deals) {
                TableRow row = changes.addRow();
                row.getCell(CCY_COL).setString(deal.currency());
                row.getCell(INT_BU_COL).setString(deal.internalBU());
                row.getCell(INT_PFOLIO_COL).setString(deal.internalPortfolio());
                row.getCell(EXT_BU_COL).setString(deal.externalBU());
                row.getCell(EXT_PFOLIO_COL).setString(ObjectUtils.defaultIfNull(deal.externalPortfolio(), ""));
                row.getCell(CFLOW_TYPE_COL).setString(deal.cashflowType());
                row.getCell(SETTLE_DATE_COL).setString(deal.settleDate());
                row.getCell(STATEMENT_DATE_COL).setString(deal.statementDate());
                row.getCell(POS_COL).setDouble(deal.position());
                row.getCell(IS_CN_AND_HAS_VAT_COL)
                        .setInt(ObjectUtils.defaultIfNull(deal.isCnAndHasVat(), false) ? 1 : 0);
                row.getCell(FX_RATE_COL).setDouble(ObjectUtils.defaultIfNull(deal.fxRate(), 0d));
            }
            dealTable.insertRows(changes);
            session.getControlFactory().getWorkflow(DEAL_BOOKING_WORKFLOW).start();
        }
    }

    public void book(int batchNum) {
        try (UserTable userTable = session.getIOFactory().getUserTable(DEAL_BOOKING_TABLE)) {
            Table table = userTable.retrieveTable();
            int rowCount = table.getRowCount();
            for (int idx = batchNum - 1; idx < rowCount; idx += BATCH_COUNT) {
                try {
                    Stopwatch stopwatch = Stopwatch.createStarted();
                    int tranNum = bookDeal(table.getRow(idx));
                    logger.info("transaction {} generated within {} ms", tranNum, stopwatch.elapsed(MILLISECONDS));
                } catch (Exception e) {
                    logger.error("failed to book deal: {}", e.getMessage(), e);
                }
            }
        }
    }

    private int bookDeal(TableRow row) {
        TradingFactory tradingFactory = session.getTradingFactory();
        StaticDataFactory staticDataFactory = session.getStaticDataFactory();
        String currency = row.getString(CCY_COL);
        try (Instrument ins = tradingFactory.retrieveInstrumentByTicker(EnumInsType.CashInstrument, currency);
             Transaction cash = tradingFactory.createTransaction(ins)) {
            String internalBU = row.getString(INT_BU_COL);
            String internalPortfolio = row.getString(INT_PFOLIO_COL);
            String externalBU = row.getString(EXT_BU_COL);
            String externalPortfolio = row.getString(EXT_PFOLIO_COL);
            String cashflowType = row.getString(CFLOW_TYPE_COL);
            String settleDate = row.getString(SETTLE_DATE_COL);
            String statementDate = row.getString(STATEMENT_DATE_COL);
            double position = row.getDouble(POS_COL);
            double fxRate = row.getDouble(FX_RATE_COL);
            boolean hasVat = row.getInt(IS_CN_AND_HAS_VAT_COL) > 0;
            logger.info("deal to book: internal BU -> {}; external BU -> {}; cashflow type -> {}; settle date -> {}",
                        internalBU,
                        externalBU,
                        cashflowType,
                        settleDate);

            cash.setValue(EnumTransactionFieldId.CashflowType, cashflowType);
            cash.setValue(EnumTransactionFieldId.InternalBusinessUnit,
                          staticDataFactory.getId(EnumReferenceTable.Party, internalBU));
            cash.setValue(EnumTransactionFieldId.ExternalBusinessUnit,
                          staticDataFactory.getId(EnumReferenceTable.Party, externalBU));
            cash.setValue(EnumTransactionFieldId.InternalPortfolio,
                          staticDataFactory.getId(EnumReferenceTable.Portfolio, internalPortfolio));
            cash.getField("Interface_Trade_Type").setValue("Metal Interest");
            cash.setValue(EnumTransactionFieldId.SettleDate, settleDate);
            cash.getField("Util Statement Date").setValue(statementDate);
            if (hasVat) {
                cash.getField("Amount with VAT").setValue(position);
            } else {
                cash.setValue(EnumTransactionFieldId.Position, position);
            }
            if (!StringUtils.isBlank(externalPortfolio)) {
                cash.setValue(EnumTransactionFieldId.ExternalPortfolio,
                              staticDataFactory.getId(EnumReferenceTable.Portfolio, externalPortfolio));
            }
            if (fxRate != 0) {
                cash.getField("Ccy Conv FX Rate").setValue(fxRate);
            }

            cash.process(EnumTranStatus.Validated);
            return cash.getValueAsInt(EnumTransactionFieldId.TransactionId);
        }
    }
}