package com.olf.jm.reportbuilder.computefields;

import com.olf.jm.reportbuilder.ReportColumns;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;

/**
 * 
 * Interface for compute fields.
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
public interface ComputeField {

    /**
     * Set the compute expression from the supplied expression. The supplied expression must be in the format...
     * <pre>
     * "operation([compute_expression]([column_name])" e.g. count(item_id(item_count))
     * </pre>
     * The compute expression will be dependent on the compute field.
     * 
     * @param expression
     * @param reportCols
     */
    void setComputeExpression(String expression, ReportColumns reportCols);

    /**
     * Get the column name to be used for the compute field.
     * 
     * @return compute field column name
     */
    String getComputeColumnName();

    /**
     * Compute the computed field and add to the data table.
     * 
     * @param groupData Group data table
     * @param allData All data table
     * @throws OException
     */
    public void compute(Table groupData, Table allData) throws OException;
}
