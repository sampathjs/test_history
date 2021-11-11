package com.matthey.pmm.jde.corrections.connectors;

import com.olf.embedded.application.Context;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;
import com.olf.openrisk.table.TableRow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiConsumer;

import static com.google.common.base.Preconditions.checkState;

public class UserTableUpdater<T> {
    
    private final TableFactory tableFactory;
    private final String tableName;
    private final String groupByCol;
    private final Set<String> excludedCols;
    private final BiConsumer<T, TableRow> rowGenerator;
    
    public UserTableUpdater(@NotNull Context context,
                            @NotNull String tableName,
                            @Nullable String groupByCol,
                            @NotNull Set<String> excludedCols,
                            @NotNull BiConsumer<T, TableRow> rowGenerator) {
        this.tableFactory = context.getTableFactory();
        this.tableName = tableName;
        this.groupByCol = groupByCol;
        this.excludedCols = excludedCols;
        this.rowGenerator = rowGenerator;
    }
    
    public void insertRows(Collection<T> records) {
        try (Table newRows = getUserTableStructure()) {
            excludedCols.forEach(newRows::removeColumn);
            for (T record : records) {
                TableRow row = newRows.addRow();
                rowGenerator.accept(record, row);
            }
            insertRowsToUserTable(newRows);
        }
    }
    
    private Table getUserTableStructure() {
        try {
            com.olf.openjvs.Table table = com.olf.openjvs.Table.tableNew(tableName);
            DBUserTable.structure(table);
            return tableFactory.fromOpenJvs(table);
        } catch (OException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void insertRowsToUserTable(Table rows) {
        try {
            com.olf.openjvs.Table table = tableFactory.toOpenJvs(rows);
            if (groupByCol != null) {
                table.addGroupBy(groupByCol);
                table.groupBy();
            }
            int retVal = DBUserTable.insert(table);
            checkState(retVal == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt(),
                       "failed to insert into user table: " + tableName);
        } catch (OException e) {
            throw new RuntimeException(e);
        }
    }
}
