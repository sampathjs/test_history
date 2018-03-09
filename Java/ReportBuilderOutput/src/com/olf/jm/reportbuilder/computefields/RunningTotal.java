package com.olf.jm.reportbuilder.computefields;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;

public class RunningTotal extends AbstractComputeField {

    public void compute(Table groupData, Table allData) throws OException {

        Table work = groupData;
        // If allData table supplied use that table for computing the value.
        // The computed value will be added to the group data table.
        if (allData != null) {
            work = allData;
        }

        // The function should be specified in the format...
        // usecolname1, usecolname2, usecolname2 (computecolname)
        
        // Get the column names used for the running total
        // The compute column name is stored in the function surrounded by brackets (see comment above), remove this and
        // what is left is a comma separated list of columns to be used in the running total 
        String usingColNames[] = computeExpression.replace('(' + computeColName + ')', "").trim().split(",");

        // The first column, if given, will specify the column containing the starting value of the running total
        double startingValue = 0.0;
        if (usingColNames[0].length() > 0) {
            startingValue = work.getDouble(usingColNames[0], 1);
        }

        // Initialise the running total with the starting value
        double runningTotal = startingValue;
        
        // Now compute the running total for each row
        int numRows = work.getNumRows();
        for (int row = 1; row <= numRows; row++) {
            // Add to the running total the value of all the using columns
            for (String col : usingColNames) {
                runningTotal += work.getDouble(col, row);
            }
            // The using columns will include the starting value column so subtract the starting value from the running total
            runningTotal -= startingValue;
            
            // Set the running total on the group row
            groupData.setDouble(computeColName, row, runningTotal);
        }
    }
}
