package com.olf.jm.reportbuilder;

import java.util.HashMap;

import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableColumn;
import com.olf.openrisk.table.TableFormatter;

/**
 * 
 * Maps report builder output data column names and column titles.
 * 
 * @author G. Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 05-Oct-2015 |               | G. Moore        | Initial version.                                                                |
 * | 002 | 10-Nov-2015 |               | G. Moore        | OpenComponent conversion.                                                       |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
public class ReportColumns {

    /** Maps column names to column titles with spaces removed. */
    private HashMap<String, String> colNameTitle = new HashMap<>();
    /** Maps column titles to column names. */
    private HashMap<String, String> colTitleName = new HashMap<>();

    private final boolean throwIfKeyNotFound;
    /**
     * Constructor maps report column names and titles from report parameter.
     * 
     * @param report Report builder output
     */
    public ReportColumns(Table report, Boolean throwIfFieldNotFound) {
    	throwIfKeyNotFound =  throwIfFieldNotFound;
        fillColNameToColTitleMap(report);
    }

    /**
     * Fills the instance hash map colNameTitle with mapping from column name to column title. The column title has any spaces removed as it
     * will be used as an xml tag.
     * 
     * @param data Data table
     */
    private void fillColNameToColTitleMap(Table data) {
        TableFormatter formatter = data.getFormatter();
        for (TableColumn col : data.getColumns()) {
            String name = col.getName();
            String title = formatter.getColumnTitle(col);
            colNameTitle.put(name, title.replaceAll(" ", ""));
            colTitleName.put(title, name);
        }
    }

    /**
     * Get the column title for the column name.
     * 
     * @param colName Column name
     * @return column title
     */
    public String getColTitle(String colName) {
    	if (colNameTitle.containsKey(colName))
    		return colNameTitle.get(colName);
    	
    	if (!throwIfKeyNotFound)
    		return null;
    	
    	throw new OpenRiskException(String.format("%s can't find column named:%s",this.getClass().getSimpleName(), colName));
    	
    }
    
    /**
     * Get the column name for the column title.
     * 
     * @param colTitle Column title
     * @return column name
     */
    public String getColName(String colTitle) {
    	if (colTitleName.containsKey(colTitle))
    		return colTitleName.get(colTitle);
    	
    	if (!throwIfKeyNotFound)
    		return null;
    	
    	throw new OpenRiskException(String.format("%s can't find column title:%s",this.getClass().getSimpleName(), colTitle));
    }
}
