package com.matthey.pmm.metal.transfers.results;

import com.matthey.pmm.metal.transfers.ImmutableStatementGeneratingRun;
import com.matthey.pmm.metal.transfers.RunResult;
import com.matthey.pmm.metal.transfers.StatementGeneratingRun;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.TableRow;

public class StatementGeneratingRunProcessor extends RunProcessor<StatementGeneratingRun> {

    private static final String PARTY_COL = "party";
    private static final String ACCOUNT_GROUP_COL = "account_group";
    private static final String STATEMENT_PATH_COL = "statement_path";

    public StatementGeneratingRunProcessor(Session session) {
        super(session);
    }

    @Override
    public String tableName() {
        return "USER_metal_rentals_statement";
    }

    @Override
    void toTableRow(TableRow row, StatementGeneratingRun result) {
        row.getCell(USER_COL).setString(result.user());
        row.getCell(RUN_TIME_COL).setString(result.runTime());
        row.getCell(STATEMENT_MONTH_COL).setString(result.statementMonth());
        row.getCell(RESULT_COL).setString(result.result().name());
        row.getCell(PARTY_COL).setString(result.party());
        row.getCell(ACCOUNT_GROUP_COL).setString(result.accountGroup());
        row.getCell(STATEMENT_PATH_COL).setString(result.statementPath());
    }

    @Override
    StatementGeneratingRun fromTableRow(TableRow row) {
        return ImmutableStatementGeneratingRun.builder()
                .user(row.getString(USER_COL))
                .runTime(row.getString(RUN_TIME_COL))
                .statementMonth(row.getString(STATEMENT_MONTH_COL))
                .result(RunResult.valueOf(row.getString(RESULT_COL)))
                .party(row.getString(PARTY_COL))
                .accountGroup(row.getString(ACCOUNT_GROUP_COL))
                .statementPath(row.getString(STATEMENT_PATH_COL))
                .build();
    }
}