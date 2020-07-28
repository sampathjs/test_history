package com.matthey.pmm.ejm.data;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.staticdata.Unit;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import org.joda.time.LocalDate;

public abstract class AbstractRetriever {

    final Session session;
    final SqlGenerator sqlGenerator;

    AbstractRetriever(Session session) {
        this.session = session;
        sqlGenerator = new SqlGenerator(session);
    }

    Table runSql(String sqlTemplate) {
        return session.getIOFactory().runSQL(sqlGenerator.genSql(sqlTemplate));
    }

    String formatDateColumn(TableRow row, String col) {
        return LocalDate.fromDateFields(row.getDate(col)).toString("dd-MMM-yyyy");
    }

    double getConversionFactor(String unit) {
        StaticDataFactory staticDataFactory = session.getStaticDataFactory();
        Unit source = staticDataFactory.getReferenceObject(Unit.class, "TOz");
        Unit target = staticDataFactory.getReferenceObject(Unit.class, unit);
        return source.getConversionFactor(target);
    }
}
