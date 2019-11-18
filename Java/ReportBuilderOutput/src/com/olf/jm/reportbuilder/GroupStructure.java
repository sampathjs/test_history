package com.olf.jm.reportbuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.olf.jm.logging.Logging;
import com.olf.jm.reportbuilder.computefields.ComputeFieldFactory;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableColumn;
import com.olf.openrisk.table.TableFormatter;
import com.olf.openrisk.table.TableRow;

/**
 * 
 * Maps the grouping structure based on the report parameters that define the grouping levels for the report.
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
 * | 003 | 24-Nov-2015 |               | G. Moore        | Compute field calculations are now in separate classes.                         |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
public class GroupStructure {

    /** Contains the title for each group level. Used as an xml element for the group. */
    private HashMap<Integer, String> groupTitle = new HashMap<>();
    /** Maps the group level number for columns that are part of a group. */ 
    private ArrayList<String> groupColumns = new ArrayList<>();
    /** For each group gives the list of columns. */
    private TreeMap<Integer, ArrayList<String>> groupLevelCols = new TreeMap<>();
    /** For each group stores the names of sum columns */
    private HashMap<Integer, ArrayList<String>> groupLevelSumCols = new HashMap<>();
    /** For each group stores the names of computed columns and their expressions */
    private HashMap<Integer, HashMap<String, String>> groupLevelComputedCols = new HashMap<>();
    /** Columns used at the data level group for sorting */
    private ArrayList<String> dataLevelSortColumns = new ArrayList<>();
    /** Stored the parent group level for a group where the groups document nodes will be created */
    private HashMap<Integer, Integer> groupParentGroup = new HashMap<>();

    /** Specifies if item number throughout the grouping is a running number or is reset at every group level */
    private boolean groupLineNumbersRunning = false;

    /*** Stores the name of columns  ***/
    private String colParamName; 
    private String colParamValue;
    
    /**
     * Maps the group structure based on the report parameters that define the grouping levels for the report.
     * 
     * @param session Ensur session
     * @param parameters Report builder parameters
     * @param data Report output data
     * @param reportCols Report output columns
     */
    public GroupStructure(Session session, Table parameters, Table data, ReportColumns reportCols) {
    	
        /*** In v17, column names have changed. Get the correct column names***/
        colParamName = Utils.getColParamName(parameters);
        colParamValue = Utils.getColParamValue(parameters);
    	
    	// Create the structure
        create(session, parameters, data, reportCols);
        		
        // Get the option for running item numbers
        int row = parameters.find(parameters.getColumnId(colParamName), "RunningGroupLineNo", 0);
        if (row >= 0) {
            String value = parameters.getString(colParamValue, row);
            groupLineNumbersRunning = "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
        }
    }

    /**
     * Work through the report parameters finding the parameters that define the report grouping. Fill various hash maps that hold the
     * grouping definition.
     * 
     * @param parameters Report parameters
     * @param data Report data
     * @throws OException
     */
    private void create(Session session, Table parameters, Table data, ReportColumns reportCols) {

        TableFormatter formatter = data.getFormatter();
        
		// In the report parameters look for any parameter names that begin with 'Group_'. These parameters provide the grouped columns and
        // their grouping level
			for (TableRow paramRow : parameters.getRows()) {
				try {
				
			    String paramName = paramRow.getString(colParamName);
			    // Is parameter a group definition? Group definition parameter names take the format 'Group_<group num>_Col_<col num>'.
			    if (paramName.startsWith("Group_")) {
			        // Get the group level number of the column
			        int grpNum = Integer.valueOf(paramName.substring(6, paramName.indexOf('_', 6)));
			        // Is this a group title? The group title is used as an xml tag for the group.
			        if (paramName.endsWith("Title")) {
			            groupTitle.put(grpNum, paramRow.getString(colParamValue));
			        }
			        // Does parameter define upper parent group? The group level element for the group will be created as a child of this parent 
			        else if (paramName.endsWith("Parent_Group")) {
			            int parentGroup = Integer.valueOf(paramRow.getString(colParamValue));
			            groupParentGroup.put(grpNum, parentGroup);
			        }
			        // Does parameter define a computed column?
			        else if (paramName.contains("_Compute_")) {
			            saveComputedColumn(paramRow, grpNum, reportCols);
			        }
			        else {
			            // Column name of column to be grouped is the parameter value.
			            String colName = paramRow.getString(colParamValue);
			            // Find the column in the data table, try to match on column title or column name
			            for (TableColumn col : data.getColumns()) {
			                String dataColTitle = formatter.getColumnTitle(col);
			                String dataColName = col.getName();
			                if (dataColTitle.equals(colName) || dataColName.equals(colName)) {
			                    // Column found, add the column name (not title) to the group columns hash map with its group level. 
			                    colName = dataColName;
			                    
			                    if (paramName.contains("_Col_")){
			                        // Add column to list of group columns 
			                        groupColumns.add(colName);
			
			                        // Add the column to the list of columns for the group level
			                        if (!groupLevelCols.containsKey(grpNum)) {
			                            groupLevelCols.put(grpNum, new ArrayList<String>());
			                        }
			                        groupLevelCols.get(grpNum).add(colName);
			                        break;
			                    }
			                    // Check if column is summed
			                    else if (paramName.contains("_Sum_")) {
			                        // Create an entry in the group sum columns map for the column
			                        if (!groupLevelSumCols.containsKey(grpNum)) {
			                            groupLevelSumCols.put(grpNum, new ArrayList<String>());
			                        }
			                        groupLevelSumCols.get(grpNum).add(colName);
			                        break;
			                    }
			                }
			            }
			        }
			    }
			    // Not a group column, is it the data title? The data title is used as an xml tag for the lowest data elements that have no
			    // grouping defined.
			    else if (paramName.equals("Data_Title")) {
			        groupTitle.put(0, paramRow.getString(colParamValue));
			    }
			    // Is defining that a data group should be placed into an upper parent group
			    else if (paramName.equals("Data_Parent_Group")) {
			        int parentGroup = Integer.valueOf(paramRow.getString(colParamValue));
			        groupParentGroup.put(0, parentGroup);
			    }
			    // Defines a sorting columns
			    else if (paramName.startsWith("Data_Sort")) {
			        dataLevelSortColumns.add(reportCols.getColName(paramRow.getString(colParamValue)));
			    }
			    // Defines a data level compute column
			    else if (paramName.contains("Data_Compute_")) {
			        saveComputedColumn(paramRow, 0, reportCols);
			    }
			} catch (NumberFormatException nfe) {
				Logging.error (String.format("Param(%s) NFE!%s", paramRow.getString(colParamName), nfe.getLocalizedMessage()), nfe);
				nfe.printStackTrace();
			} catch (Exception e) {
				Logging.error (String.format("Param(%s) Unexpetected!%s", paramRow.getString(colParamName), e.getLocalizedMessage()), e);
				e.printStackTrace();
			}

			}
    }

    /**
     * Get the computed column details from the parameter row and save in a hash map.
     * 
     * @param paramRow
     * @param groupNum
     */
    private void saveComputedColumn(TableRow paramRow, int groupNum, ReportColumns reportCols) {
        String computeString = paramRow.getString(colParamValue);
        String colName = ComputeFieldFactory.create(computeString, reportCols).getComputeColumnName();
        HashMap<String, String> computedCols = groupLevelComputedCols.get(groupNum);
        if (computedCols == null) {
            computedCols = new HashMap<>();
            groupLevelComputedCols.put(groupNum, computedCols);
        }
        computedCols.put(colName, computeString);
    }

    /**
     * Get the count of group levels.
     * 
     * @return group count
     */
    public int getGroupCount() {
        return groupLevelCols.size();
    }

    /**
     * Get the group title for the group.
     * 
     * @param groupNum Group level number
     * @return group title
     * @see #getGroupElementName(int)
     */
    public String getGroupTitle(int groupNum) {
        String title = null;
        if (groupNum == getGroupCount() + 1) {
            title = groupTitle.get(0);
        }
        else {
            title = groupTitle.get(groupNum);
        }
        if (title == null) {
            title = "Group" + groupNum;
        }
        return title;
    }

    /**
     * Get a list of columns that make up the grouping.
     * 
     * @param groupNum Group level number
     * @return column list
     */
    public List<String> getGroupColumns(int groupNum) {
        return groupLevelCols.get(groupNum);
    }

    /**
     * Is the column defined as a group column at the group level.
     * 
     * @param colName Column name
     * @param groupNum Group level number
     * @return true/false
     */
    public boolean isColumnInGroup(String colName, int groupNum) {
        return groupLevelCols.get(groupNum).contains(colName);
    }

    /**
     * Is the column defined as a group column at any group level.
     * 
     * @param colName Column name
     * @return true/false
     */
    public boolean isGroupColumn(String colName) {
        return groupColumns.contains(colName);
    }

    /**
     * Has the group level any group sum columns.
     * 
     * @param groupNum Group level number
     * @return true/false
     */
    public boolean hasGroupSumColumns(int groupNum) {
        if (groupLevelSumCols.get(groupNum) == null) {
            return false;
        }
        return groupLevelSumCols.get(groupNum).size() > 0;
    }

    /**
     * Get the group level sum columns.
     * 
     * @param groupNum Group level number
     * @return true/false
     */
    public List<String> getGroupSumColumns(int groupNum) {
        return groupLevelSumCols.get(groupNum);
    }

    /**
     * Is the column a computed column at the group level.
     * 
     * @param groupNum Group level number
     * @param colName Column name
     * @return true/false
     */
    public boolean isComputedColumn(int groupNum, String colName) {
        HashMap<String, String> cols = groupLevelComputedCols.get(groupNum);
        if (cols != null) {
            return cols.containsKey(colName);
        }
        return false;
    }

    /**
     * Get the computed columns for the group level. The hash map returned is keyed by the column name and the values represent the compute
     * expression.
     *  
     * @param groupNum Group level number
     * @return compute columns map
     */
    public Map<String, String> getGroupComputedColumns(int groupNum) {
        return groupLevelComputedCols.get(groupNum); 
    }

    /**
     * Are the group line numbers a running number or reset at each group level.
     * 
     * @return true if they are to be a running number
     */
    boolean isGroupLineNosRunning() {
        return groupLineNumbersRunning;
    }
    
    /**
     * Get the group element name as defined by the report parameters.
     * 
     * @param groupLevel Group level to get name for
     * @return Element name or, if report does not define the group title, 'GroupX' where X is the group level number
     */
    public String getGroupElementName(int groupLevel) {
        return getGroupTitle(groupLevel);
    }

    /**
     * Determines of the supplied group title is actually the data element title.
     * 
     * @param groupTitle Group title to check
     * @return true/false
     */
    public boolean isGroupNameDataGroup(String groupTitle) {
        return getGroupTitle(0).equals(groupTitle);
    }

    /**
     * Retrieve a comma separated list of data level sort columns in the correct sort order.
     * 
     * @return sort columns
     */
    public String getDataLevelSortColumns() {
        StringBuilder sort = new StringBuilder();
        for (String col : dataLevelSortColumns) {
            sort.append(',').append(col);
        }
        if (sort.length() > 0) {
            sort.deleteCharAt(0);
        }
        return sort.toString();
    }

    /**
     * Get the parent group level if configured for the supplied group level. This indicates that the group should create a node in an
     * upper parent group rather than it's direct parent group.
     * 
     * @param groupLevel Group level to get parent group
     * @return parent group level number or zero if none configured
     */
    public int getGroupParentGroup(int groupLevel) {
        if (groupParentGroup.containsKey(groupLevel)) {
            return groupParentGroup.get(groupLevel);
        }
        return 0;
    }
}
