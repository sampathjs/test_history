package com.olf.jm.reportbuilder.computefields;

import com.olf.jm.reportbuilder.ReportColumns;
import com.olf.jm.reportbuilder.Utils;

/**
 * 
 * Abstract class acts as parent class to compute fields.
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
public abstract class AbstractComputeField implements ComputeField {

    /** The compute column name */
    protected String computeColName;
    /** The compute expression */
    protected String computeExpression;

    @Override
    public void setComputeExpression(String expression, ReportColumns reportCols) {
        // Compute expression is found contained in brackets
        int idx1 = expression.indexOf('(');
        int idx2 = expression.lastIndexOf(')');
        // Compute function may refer to report output columns, substitute those for the outout table column names
        computeExpression = Utils.substituteValues(expression.substring(idx1 + 1, idx2), true, null, reportCols);
        // Inside the compute expression will be the column name
        setComputeColumnName(expression, reportCols);
    }

    /**
     * Set the compute column name which will be found in the given expression
     * @param expression
     * @param reportCols
     * @return
     */
    private void setComputeColumnName(String expression, ReportColumns reportCols) {
        // Result column name should be between brackets within the compute expression
        int idx1 = computeExpression.indexOf('(');
        int idx2 = computeExpression.indexOf(')');
        computeColName = Utils.substituteValues(computeExpression.substring(idx1 + 1, idx2), true, null, reportCols);
    }

    @Override
    public String getComputeColumnName() {
        return computeColName;
    }
}
