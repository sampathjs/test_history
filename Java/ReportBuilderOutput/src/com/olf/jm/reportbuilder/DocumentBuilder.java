package com.olf.jm.reportbuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.olf.jm.logging.Logging;
import com.olf.jm.reportbuilder.computefields.ComputeField;
import com.olf.jm.reportbuilder.computefields.ComputeFieldFactory;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.ROW_TYPE_ENUM;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.internal.OpenRiskException;

/**
 * 
 * Builds xml document from a data table based on the grouping structure.
 * 
 * @see GroupStructure
 *  
 * @author G. Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 05-Oct-2015 |               | G. Moore        | Initial version.                                                                |
 * | 002 | 10-Nov-2015 |               | G. Moore        | OpenComponent conversion but mostly kept the same as OpenJVS version because the|
 * |     |             |               |                 | grouping API is very different between the two and too risky to change at this  |
 * |     |             |               |                 | at this point in the development. Maybe later.                                  |
 * | 003 | 24-Nov-2015 |               | G. Moore        | Compute field calculations are now in separate classes.                         |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
public class DocumentBuilder {

    private static GroupStructure groupStructure;
    private static ReportColumns reportCols;

    private DocumentBuilder () {}
    
    /**
     * Build the xml document from the report data and the grouping definition. Each group level is a sub-element within the xml document.
     * 
     * @param data Report data
     * @return
     * @throws OException 
     * @throws ParserConfigurationException 
     */
    public static Document build(Session session, com.olf.openrisk.table.Table ocData, GroupStructure groupStructure, ReportColumns reportCols, Map<String, String> colLastValues)
            throws OException, ParserConfigurationException {

        DocumentBuilder.groupStructure = groupStructure;
        DocumentBuilder.reportCols = reportCols;

        // Create xml document and root node.
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Node root = doc.createElement("root");
        doc.appendChild(root);

        Table data = session.getTableFactory().toOpenJvs(ocData);
        try {
            Table groupings = generateGroupingTable(data);
            try {
                createElements(groupings, root, colLastValues);
            }
            finally {
                groupings.destroy();
            }
        }
        finally {
            data.destroy();
        }

        return doc;
    }

    /**
     * Generate the grouping table from the data table. The data returned is a table with sub-tables. The main table represents the
     * grouping of the data at group level 1. Each sub-table (contained in the column GroupDate) represents the group at the next level
     * down.
     * 
     * @param data Data to group
     * @return grouped data table
     * @throws OException
     */
    private static Table generateGroupingTable(Table data) throws OException {
        
        // Hash map will contain a table for each group level. The table is the group sum of the main data for the group level. Group
        // columns are obtained from the grouping structure.  
        HashMap<Integer, Table> groupings = new HashMap<>();

        Table workingData = new Table();
        try {
            // Cannot copy table as original table doesn't allow group sum!! Is this a core bug?
            data.addCol("item_no", COL_TYPE_ENUM.COL_INT);
            workingData.select(data, "*", "item_no EQ 0");
            data.delCol("item_no");

            int numGroups = groupStructure.getGroupCount();
            
            // Create tables containing grouped data for each of the group levels
            for (int group = 1; group <= numGroups; group++) {
    
                Table groupTbl = workingData.copyTable();
                groupings.put(group, groupTbl);
    
                // Starting at group level 1 get all the group columns up to current working group level
                StringBuilder groupColsStr = new StringBuilder();
                for (int groupColsLevel = 1; groupColsLevel <= group; groupColsLevel++) {
                    for (String colName : groupStructure.getGroupColumns(groupColsLevel)) {
                        groupColsStr.append(colName).append(',');
                        // Make sure column is not summed when grouped (in case group column is a double type)
                        groupTbl.setColGroupSumStatus(colName, 0);
                    }
                }

                // Group sum the data
                groupColsStr.deleteCharAt(groupColsStr.length() - 1);
                String groupColName = groupStructure.getGroupColumns(group).get(groupStructure.getGroupColumns(group).size() - 1);
                groupTbl.group(groupColsStr.toString());
                groupTbl.groupSum(groupColName);
    
                groupTbl.clearDataRows();
                int numRows = groupTbl.getNumRows();
                for (int row = numRows; row >= 1; row--) {
                    groupTbl.setRowType(row, ROW_TYPE_ENUM.ROW_DATA);
                }
                groupTbl.clearGroupBy();
                groupTbl.addCol("group_no", COL_TYPE_ENUM.COL_INT);
                groupTbl.setColValInt("group_no", group);
                
                // Process any compute fields for the group level

                processComputeFields(groupTbl, group, workingData);
                
                // Each row in the group tagged with the item (row) number
                groupTbl.setColIncrementInt("item_no", 1, 1);
            }
    

            // Sort main data by defined data level sort columns
            workingData.group(groupStructure.getDataLevelSortColumns());
            workingData.setColIncrementInt("item_no", 1, 1);
            // Data level data added to groupings at number of groups + 1
            groupings.put(numGroups + 1, workingData);

            // Create inner tables based on the relationship of group levels
            buildGroupInnerTables(groupings.get(1), groupings, 1);

            return groupings.get(1).copyTable();
            
        }
        finally {
            workingData.destroy();
            for (Table t : groupings.values()) {
                if (t != null && Table.isTableValid(t) == 1) {
                    t.destroy();
                }
            }
        }
    }

    /**
     * Using the group level tables, build up the grouping by creating sub tables that relate to each group level row. The sub-tables will
     * contain the filtered lower level group data for the group data on the given row.
     * 
     * @param grpLevelTbl Table with the first level of grouping, a new column named GroupData will be added containing the lower level
     *                    grouping data 
     * @param groupings Map of group level data tables
     * @param group Initial group level
     * @throws OException
     */
    private static void buildGroupInnerTables(Table grpLevelTbl, HashMap<Integer, Table> groupings, int group) throws OException {

        grpLevelTbl.addCol("GroupData", COL_TYPE_ENUM.COL_TABLE);
        
        // Get the group data table for next group level down
        Table groupLevelLowerTbl = groupings.get(group + 1);
        if (groupLevelLowerTbl == null) {
            return;
        }
        
        // This link table will be used to store the filtered lower level group data related to the current row of the current group
        Table groupLevelLowerLinkTbl = groupLevelLowerTbl.cloneTable();
        
        try {
            int numRows = grpLevelTbl.getNumRows();
            // For each row of the current group table
            for (int row = 1; row <= numRows; row++) {
                // Set the table name of the lower level group data to that of the lower level group name
                String groupName = groupStructure.getGroupElementName(group + 1);
                groupLevelLowerLinkTbl.setTableName(groupName);
                
                // For each of the rows in the lower level group data check to see if it matches the grouping columns of the current group row
                int numRowsLower = groupLevelLowerTbl.getNumRows();
                for (int rowLower = 1; rowLower <= numRowsLower; rowLower++) {
                    boolean rowMatch = true;
                    // Check each grouping column until all grouping columns check or there is a mismatch
                    for (int groupColsLevel = 1; groupColsLevel <= group; groupColsLevel++) {
                        for (String colName : groupStructure.getGroupColumns(groupColsLevel)) {
                            String mainValue = getColValueAsString(grpLevelTbl, colName, row);
                            String lowerValue = getColValueAsString(groupLevelLowerTbl, colName, rowLower);
                            if (!lowerValue.equals(mainValue)) {
                                rowMatch = false;
                                groupColsLevel = group + 1;;
                                break;
                            }
                        }
                    }
                    // If all grouping column data matched then lower level row data matches the current row grouping and the row can be
                    // added to link table
                    if (rowMatch) {
                        groupLevelLowerTbl.copyRowAdd(rowLower, groupLevelLowerLinkTbl);
                    }
                }
                
                // Update the item number column to reset the numbering on a per group level if these are not to be a running number
                if (!groupStructure.isGroupLineNosRunning()) {
                    groupLevelLowerLinkTbl.setColIncrementInt("item_no", 1, 1);
                }
                
                // Set the lower level group data link table to the current group row
                grpLevelTbl.setTable("GroupData", row, groupLevelLowerLinkTbl.copyTable());
                
                // If not at the lowest group level execute this method again but for the next group level down 
                if (group < (groupings.size() - 1)) {
                    buildGroupInnerTables(grpLevelTbl.getTable("GroupData", row), groupings, group + 1);
                }
                else {
                    // Process compute fields on the data level
                    processComputeFields(grpLevelTbl.getTable("GroupData", row), 0, grpLevelTbl);
                }

                // Clear the lower level link table ready to work on the next current group row
                groupLevelLowerLinkTbl.clearRows();
            }
        }
        finally {
            groupLevelLowerLinkTbl.destroy();
        }
    }

    /**
     * Process group level compute field calculating there values and adding to the group table.
     * 
     * @param group Group data table
     * @param groupNum Group level
     * @param data Main data table
     * @throws OException
     */
    private static void processComputeFields(Table group, int groupNum, Table data) throws OException {

        Map<String, String> computeCols = groupStructure.getGroupComputedColumns(groupNum);
        if (computeCols != null) {
            for (String computeColName : computeCols.keySet()) {
                // Get the compute expression from the map
                String expr = computeCols.get(computeColName);
                ComputeField computeField = ComputeFieldFactory.create(expr, reportCols);
                // If compute field is to act across all data then supply the compute field with all data
                int idx = expr.indexOf('(');
                if (expr.substring(0, idx).trim().endsWith("all")) {
                    computeField.compute(group, data);
                }
                else {
                    computeField.compute(group, null);
                }
            }
        }
    }

    /**
     * Create document elements based on the grouped data.
     * 
     * @param group Table with grouped data
     * @param node Document node to create child elements
     * @param colLastValues Map of columns with there last data values
     * @throws DOMException
     * @throws OException
     */
    private static void createElements(Table group, Node node, Map<String, String> colLastValues) throws DOMException, OException {

        // Get the group number the group data is based on
        int groupNum = 0;
        if (group.getColNum("group_no") > 0) {
            groupNum = group.getInt("group_no", 1);
        }

        // Iterate over each group row
        int numRows = group.getNumRows();
        for (int row = 1; row <= numRows; row++) {
            // Get the group element name which will be the containing element for the group data
            String groupName = groupStructure.getGroupElementName(groupNum);
            // Group name requires substitution of column value giving a group name that represents the data value in the column and row
            if (groupName.startsWith("$$")) {
                String colName = groupName.substring(2, groupName.length() - 2);
                colName = reportCols.getColName(colName);
                groupName = getColValueAsString(group, colName, row);
                groupName = groupName.replace('(', '_');
                groupName = groupName.replace(')', '_');
            }

            // Create group elements 
            Node groupNode = node.getOwnerDocument().createElement(groupName);
            // Check to see if this group should create a node in an upper parent group node
            int parentGroup = groupStructure.getGroupParentGroup(groupNum);
            if (parentGroup > 0) {
                String parentGroupTitle = groupStructure.getGroupTitle(parentGroup);
                // Walk up the nodes to find the required parent node
                Node parentNode = node;
                while (!parentNode.getNodeName().equals(parentGroupTitle)) {
                    parentNode = parentNode.getParentNode();
                }
                node = parentNode;
            }
            // Add group element as child of parent node
            node.appendChild(groupNode);

            // Iterate over the group data columns
            int numCols = group.getNumCols();
            for (int colNum = 1; colNum <= numCols; colNum++) {
                String colName = group.getColName(colNum);
                // If column contains lower group level data table the ignore
                if ("GroupData".equals(colName)) {
                    continue;
                }
                else if ("item_no".equals(colName)) {
                    // Add item no child
                    Element child = null;
					try {
						child = node.getOwnerDocument().createElement(groupName + "No");
					} catch (DOMException dom) {
						if (dom.getLocalizedMessage().contains("INVALID_CHARACTER"))
							Logging.error(String.format("INVALID NAME: Group(%d): Name:%s Title:%s", groupNum, groupName,groupStructure.getGroupTitle(groupNum) ), dom);
						throw dom;
					}
                    child.setTextContent(getColValueAsString(group, "item_no", row));
                    child.setAttribute("datatype", getColDatatype(group, colName));
                    groupNode.appendChild(child);
                }
                else if (groupNum > 0 && (groupStructure.isColumnInGroup(colName, groupNum) || groupStructure.isComputedColumn(groupNum, colName))) {
                    // Group column for group or group level computed column 
                	String colTitle=null;
                	try { 							  // check compute/group field present in parameter list
                    	colTitle = reportCols.getColTitle(colName);
                	} catch (OpenRiskException ore) { // this is expected for current colName
                    	if (!ore.getLocalizedMessage().contains(colName))
                    		throw ore; 				  // if for other reason continue reporting unexpected condition
                    }
                    Element child = node.getOwnerDocument().createElement(colTitle != null ? colTitle : colName);
                    child.setTextContent(getColValueAsString(group, colName, row));
                    child.setAttribute("datatype", getColDatatype(group, colName));
                    groupNode.appendChild(child);
                }
                else if (groupNum < 1 && !groupStructure.isGroupColumn(colName)) {
                    // Data level column
                	String colTitle = reportCols.getColTitle(colName);
                    Element child = node.getOwnerDocument().createElement(colTitle != null ? colTitle : colName);
                    child.setTextContent(getColValueAsString(group, colName, row));
                    child.setAttribute("datatype", getColDatatype(group, colName));
                    groupNode.appendChild(child);
                }
                colLastValues.put(colName, getColValueAsString(group, colName, row));
            }
            
            // If a group data column exists create child elements under this group element for the lower level group
            if (group.getColNum("GroupData") > 0) {
                Table subGroup = group.getTable("GroupData", row);
                if (subGroup != null && Table.isTableValid(subGroup) == 1) {
                    createElements(subGroup, groupNode, colLastValues);
                }
            }

            // Add child elements for group sum columns
            createGroupSumNodes(group, row, groupNode, groupNum);
        }
    }

    /**
     * Create the group sum nodes with the running total.
     * 
     * @param node Node to add group sum nodes to
     * @param groupLevel Current group level
     * @throws OException
     */
    private static void createGroupSumNodes(Table groupData, int row, Node node, int groupLevel) throws OException {
        // Check not at root node as this indicates initial document creation.
        if (!"root".equals(node.getNodeName())) {

            // Are there group sum columns at this level.
            if (groupStructure.hasGroupSumColumns(groupLevel)) {
                // Get group name of this level
                String groupName = groupStructure.getGroupElementName(groupLevel);
                
                if (node.getNodeName().equals(groupName)) {

                    List<String> sumCols = groupStructure.getGroupSumColumns(groupLevel);
                    
                    // Iterate for each group sum column at this level.
                    for (String colName : sumCols) {
                        // Create and add the group sum element.
                        String sumName =  groupName + '_' + reportCols.getColTitle(colName) + "_Sum";
                        double sum = groupData.getDouble(colName, row);
                        Element sumNode = node.getOwnerDocument().createElement(sumName);
                        sumNode.setTextContent(Double.toString(sum)/*value.toString()*/);
                        sumNode.setAttribute("datatype", "dbl");
                        node.appendChild(sumNode);
    
                    }
                }
                else {
                    throw new RuntimeException("Node name " + node.getNodeName() +
                            " does not match group name " + groupName + " when creaing group sum elements");
                }
            }
        }
    }

    /**
     * Get the report data column value as a string.
     * 
     * @param data Report data
     * @param colName Column name
     * @param row Column row
     * @return string value
     * @throws OException
     */
    private static String getColValueAsString(Table data, String colName, int row) throws OException {
        COL_TYPE_ENUM type = COL_TYPE_ENUM.fromInt(data.getColType(colName));
        switch (type) {
        case COL_INT:
            return String.valueOf(data.getInt(colName, row));
        case COL_DATE:
            return OCalendar.formatJdForDbAccess(data.getInt(colName, row));
        case COL_STRING:
            return data.getString(colName, row);
        case COL_DOUBLE:
            return String.valueOf(data.getDouble(colName, row));
        case COL_DATE_TIME:
            return String.valueOf(data.getDateTime(colName, row).formatForDbAccess());
        case COL_TABLE:
            return data.getTable(colName, row).getTableName();
        default:
            throw new RuntimeException(colName + " is of unhandled type " + type);
        }
    }

    /**
     * Get the data type of the column.
     * 
     * @param data Report data
     * @param colName Column name
     * @return data type
     * @throws OException
     */
    private static String getColDatatype(Table data, String colName) throws OException {
        COL_TYPE_ENUM type = COL_TYPE_ENUM.fromInt(data.getColType(colName));
        switch (type) {
        case COL_INT:
            return "int";
        case COL_DOUBLE:
            return "dbl";
        case COL_DATE:
        case COL_DATE_TIME:
        case COL_STRING:
            return "str";
        default:
            throw new RuntimeException(colName + " is of unhandled type " + type);
        }
    }
    
}
