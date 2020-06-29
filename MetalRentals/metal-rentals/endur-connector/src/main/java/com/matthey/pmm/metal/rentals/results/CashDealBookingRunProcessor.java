package com.matthey.pmm.metal.rentals.results;

import com.matthey.pmm.metal.rentals.CashDealBookingRun;
import com.matthey.pmm.metal.rentals.ImmutableCashDeal;
import com.matthey.pmm.metal.rentals.ImmutableCashDealBookingRun;
import com.matthey.pmm.metal.rentals.RunResult;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.TableRow;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import static com.matthey.pmm.metal.rentals.data.CashDealColumns.CFLOW_TYPE_COL;
import static com.matthey.pmm.metal.rentals.data.CashDealColumns.EXT_BU_COL;
import static com.matthey.pmm.metal.rentals.data.CashDealColumns.EXT_PFOLIO_COL;
import static com.matthey.pmm.metal.rentals.data.CashDealColumns.FX_RATE_COL;
import static com.matthey.pmm.metal.rentals.data.CashDealColumns.INT_BU_COL;
import static com.matthey.pmm.metal.rentals.data.CashDealColumns.INT_PFOLIO_COL;
import static com.matthey.pmm.metal.rentals.data.CashDealColumns.POS_COL;
import static com.matthey.pmm.metal.rentals.data.CashDealColumns.SETTLE_DATE_COL;
import static com.matthey.pmm.metal.rentals.data.CashDealColumns.TRAN_NUM_COL;

public class CashDealBookingRunProcessor extends RunProcessor<CashDealBookingRun> {

    public CashDealBookingRunProcessor(Session session) {
        super(session);
    }

    @Override
    String tableName() {
        return "USER_metal_rentals_booked_deal";
    }

    @Override
    void toTableRow(TableRow row, CashDealBookingRun result) {
        row.getCell(USER_COL).setString(result.user());
        row.getCell(RUN_TIME_COL).setString(result.runTime());
        row.getCell(STATEMENT_MONTH_COL).setString(result.statementMonth());
        row.getCell(RESULT_COL).setString(result.result().name());
        row.getCell(CFLOW_TYPE_COL).setString(result.deal().cashflowType());
        row.getCell(INT_BU_COL).setString(result.deal().internalBU());
        row.getCell(EXT_BU_COL).setString(result.deal().externalBU());
        row.getCell(INT_PFOLIO_COL).setString(result.deal().internalPortfolio());
        row.getCell(EXT_PFOLIO_COL).setString(ObjectUtils.defaultIfNull(result.deal().internalPortfolio(), ""));
        row.getCell(SETTLE_DATE_COL).setString(result.deal().settleDate());
        row.getCell(POS_COL).setDouble(result.deal().position());
        row.getCell(FX_RATE_COL).setDouble(ObjectUtils.defaultIfNull(result.deal().fxRate(), 0d));
        row.getCell(TRAN_NUM_COL).setString(result.deal().tranNum());
    }

    @Override
    CashDealBookingRun fromTableRow(TableRow row) {
        return ImmutableCashDealBookingRun.builder()
                .user(row.getString(USER_COL))
                .runTime(row.getString(RUN_TIME_COL))
                .statementMonth(row.getString(STATEMENT_MONTH_COL))
                .result(RunResult.valueOf(row.getString(RESULT_COL)))
                .deal(ImmutableCashDeal.builder()
                              .currency("")
                              .cashflowType(row.getString(CFLOW_TYPE_COL))
                              .internalBU(row.getString(INT_BU_COL))
                              .internalPortfolio(row.getString(INT_PFOLIO_COL))
                              .externalBU(row.getString(EXT_BU_COL))
                              .settleDate(row.getString(SETTLE_DATE_COL))
                              .position(row.getDouble(POS_COL))
                              .externalPortfolio(StringUtils.defaultIfBlank(row.getString(EXT_PFOLIO_COL), null))
                              .fxRate(row.getDouble(FX_RATE_COL))
                              .tranNum(row.getString(TRAN_NUM_COL))
                              .build())
                .build();
    }
}
