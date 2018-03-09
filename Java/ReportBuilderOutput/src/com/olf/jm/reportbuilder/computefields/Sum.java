package com.olf.jm.reportbuilder.computefields;

import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;

/**
 * 
 * Sum computed field. The expression for the sum must be compatible with the {@link Table#select(Table, String, String)} method. The
 * sum is taken from the result of the select.
 * <p>
 * Example sum expression...
 * <pre>
 * sum(SUM item_label(item_count))
 * </pre>
 * where item_label is a column in the data table, and item_count is a column appended to the data table containing the row count.
 * 
 * @author G. Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 24-Nov-2015 |               | G. Moore        | Initial version.                                                                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
public class Sum extends AbstractComputeField {

    public void compute(Table groupData, Table allData) throws OException {

        Table work = groupData;
        // If allData table supplied use that table for computing the value.
        // The computed value will be added to the group data table.
        if (allData != null) {
            work = allData;
        }

        Table result = new Table();
        try {
            work.addCol("tmp_select", COL_TYPE_ENUM.COL_INT);
            int retval = result.select(work, computeExpression, "tmp_select EQ 0");
            if (retval == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
                groupData.addCol(computeColName, COL_TYPE_ENUM.COL_DOUBLE, computeColName);
                groupData.setColValDouble(computeColName, result.getDouble(1, 1));
            }
            else {
                throw new RuntimeException(
                        DBUserTable.dbRetrieveErrorInfo(retval, "Compute field named '" + computeColName + "' " +
                                "with expression " + computeExpression + " failed"));
            }
        }
        finally {
            result.destroy();
            work.delCol("tmp_select");
        }
    }
}
