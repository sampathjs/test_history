package com.olf.jm.reportbuilder.computefields;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;

/**
 * Generates new IDs for a given string column starting with the ID 1 and incrementing it for each newly encountered
 * unique value of the string column.
 * For example the following mapping would be performed to the following input data 
 * <table>
 *   <tr>
 *     <th> input_col</th>
 *     <th> output_col</th>
 *   </tr>
 *   <tr>
 *     <td> one name</td>
 *     <td> 1</td>
 *   </tr>
 *   <tr>
 *     <td> another name</td>
 *     <td> 2</td>
 *   </tr>
 *   <tr>
 *     <td> yet another name</td>
 *     <td> 3</td>
 *   </tr>
 *   <tr>
 *     <td> another name</td>
 *     <td> 2</td>
 *   </tr>
 *   <tr>
 *     <td> one name</td>
 *     <td> 1</td>
 *   </tr>
 * </table>
 * <p>
 * Example count expression...
 * <pre>
 * idcountmapping(col_name)
 * </pre>
 * where item_label is a column in the data table, and item_count is a column appended to the data table containing the row count.
 * 
 * @author J. Waechter
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 18-Jan-2016 |               | J. Waechter     | Initial version.                                                                |
 * | 002 | 20-Jan-2016 |               | J. Waechter     | Added sorting by computeColumn
 * | 003 | 22-Jan-2016 |               | J. Waechter	 | Added functionality that a given set of values 
 *                                                         always the same numbers are created
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
public class IDBasedCountAndMapping extends AbstractComputeField {

    @Override
    public void compute(Table groupData, Table allData) throws OException {

        Table work = groupData;
        // If allData table supplied use that table for computing the value.
        // The computed value will be added to the group data table.
        if (allData != null) {
            work = allData;
        }
        Table total = allData;
        if (total == null) {
        	total = groupData;
        }
        
        
        int counter=1;
        Map<String, Integer> map = new HashMap<>();
        work.addCol(computeColName, COL_TYPE_ENUM.COL_INT);
        int idx1 = 0;
        int idx2 = computeExpression.indexOf(',');
        String srcCol = computeExpression.substring(idx1, idx2).trim();
        
        List<String> allIds = new ArrayList<>(total.getNumRows()); 
        for (int rowNum = work.getNumRows(); rowNum >= 1; rowNum--) {
        	String value = work.getString(srcCol, rowNum);
        	allIds.add(value);
        }
        Collections.sort(allIds);
        for (String id : allIds) {
        	if (!map.containsKey(id)) {
        		map.put(id, counter++);
        	}         	
        }

        for (int rowNum = 1; rowNum <= work.getNumRows(); rowNum++) {
            int id=counter;
        	String value = work.getString(srcCol, rowNum);
    		id = map.get(value);
    		work.setInt(computeColName, rowNum, id);
        }
        work.sortCol(computeColName);
    }
}
