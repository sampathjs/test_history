package com.matthey.pmm.metal.transfers.results;

import com.matthey.pmm.metal.transfers.ImmutablePartyContact;
import com.matthey.pmm.metal.transfers.ImmutableStatementEmailingRun;
import com.matthey.pmm.metal.transfers.RunResult;
import com.matthey.pmm.metal.transfers.StatementEmailingRun;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.TableRow;

public class StatementEmailingRunProcessor extends RunProcessor<StatementEmailingRun> {

    private static final String PARTY_COL = "party";
    private static final String CONTACT_COL = "contact";
    private static final String EMAIL_COL = "email";
    private static final String STATEMENT_PATH_COL = "statement_path";

    public StatementEmailingRunProcessor(Session session) {
        super(session);
    }

    @Override
    String tableName() {
        return "USER_metal_rentals_emailing";
    }

    @Override
    void toTableRow(TableRow row, StatementEmailingRun result) {
        row.getCell(USER_COL).setString(result.user());
        row.getCell(RUN_TIME_COL).setString(result.runTime());
        row.getCell(STATEMENT_MONTH_COL).setString(result.statementMonth());
        row.getCell(RESULT_COL).setString(result.result().name());
        row.getCell(PARTY_COL).setString(result.partyContact().party());
        row.getCell(CONTACT_COL).setString(result.partyContact().contact());
        row.getCell(EMAIL_COL).setString(result.partyContact().email());
        row.getCell(STATEMENT_PATH_COL).setString(result.statementPath());
    }

    @Override
    StatementEmailingRun fromTableRow(TableRow row) {
        return ImmutableStatementEmailingRun.builder()
                .user(row.getString(USER_COL))
                .runTime(row.getString(RUN_TIME_COL))
                .statementMonth(row.getString(STATEMENT_MONTH_COL))
                .result(RunResult.valueOf(row.getString(RESULT_COL)))
                .partyContact(ImmutablePartyContact.builder()
                                      .party(row.getString(PARTY_COL))
                                      .contact(row.getString(CONTACT_COL))
                                      .email(row.getString(EMAIL_COL))
                                      .build())
                .statementPath(row.getString(STATEMENT_PATH_COL))
                .build();
    }
}
