package com.olf.jm.reportbuilder.computefields;

import com.olf.jm.reportbuilder.ReportColumns;

/**
 * 
 * Factory creates compute field objects based on the compute expression.
 * 
 * @author G. Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 24-Nov-2015 |               | G. Moore        | Initial version.       
 * | 002 | 18-Jan-2016 | 			   | J. Waechter	 | added idcountmapping                                                        |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
public class ComputeFieldFactory {

    /**
     * Create a {@link ComputeField} object based on the expression.
     * The supplied expression must be in the format...
     * <pre>
     * "operation([compute_expression]([column_name])" e.g. count(item_id(item_count))
     * </pre>
     * The compute expression will be dependent on the compute field.
     * 
     * @param expression
     * @param reportCols
     * @return compute field object
     */
    public static ComputeField create(String expression, ReportColumns reportCols) {
        // The compute operation can be found before an opening bracket
        int idx1 = expression.indexOf('(');
        String operation = expression.substring(0, idx1).trim();

        ComputeField field = null;
        
        switch (operation.split(" ")[0]) {
        case "runningtotal":
            field = new RunningTotal();
            break;

        case "count":
            field = new Count();
            break;

        case "sum":
            field = new Sum();
            break;
        case "idcountmapping": // JW: 2016-01-18
        	field = new IDBasedCountAndMapping();
        	break;
        default:
            break;
        }

        field.setComputeExpression(expression, reportCols);
        
        return field;
    }
}
	