package com.olf.jm.reportbuilder;
 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import com.matthey.openlink.reporting.runner.generators.DefinitionParameter;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

public class DmsOutputParameterUtil {

    private static final String COL_NAME = "expr_param_name";
    private static final String COL_NAME_NEW = "parameter_name";
    private static final String COL_VALUE = "expr_param_value";
    private static final String COL_VALUE_NEW = "parameter_value";
    
    public class DmsOutputParameter {
        String name;
        public DmsOutputParameter(String name) {
            this.name = name;
        }
        @Override
        public boolean equals(Object arg0) {
            if (arg0 instanceof DmsOutputParameter) {
                String inName = ((DmsOutputParameter)arg0).name;
                return name.startsWith(inName);
            }
            return false;
        }
    }

    private static ArrayList<DmsOutputParameter> paramNames = new ArrayList<>();
    static {
        DmsOutputParameterUtil names = new DmsOutputParameterUtil();
        paramNames.add(names.new DmsOutputParameter("Output"));
        paramNames.add(names.new DmsOutputParameter("Group_"));
        paramNames.add(names.new DmsOutputParameter("Data_"));
        paramNames.add(names.new DmsOutputParameter("Template"));
        paramNames.add(names.new DmsOutputParameter("RunningGroupLineNo"));
        paramNames.add(names.new DmsOutputParameter("DocumentTitle"));
        paramNames.add(names.new DmsOutputParameter("SaveToDeal"));
        paramNames.add(names.new DmsOutputParameter("LinkedReportDefinition"));
        paramNames.add(names.new DmsOutputParameter("DATABASE_DIR"));
        paramNames.add(names.new DmsOutputParameter("DATABASE_FILENAME"));
        paramNames.add(names.new DmsOutputParameter("DATABASE_NODE_ID"));
        paramNames.add(names.new DmsOutputParameter("DISPLAY_REPORT_OUTPUT"));
        paramNames.add(names.new DmsOutputParameter("ERROR_REASON"));
        paramNames.add(names.new DmsOutputParameter("GEN_DATE"));
        paramNames.add(names.new DmsOutputParameter("LOGGING_LEVEL"));
        paramNames.add(names.new DmsOutputParameter("OUTPUT_DATA_TABLE"));
        paramNames.add(names.new DmsOutputParameter("OUTPUT_DIR"));
        paramNames.add(names.new DmsOutputParameter("OUTPUT_EXT"));
        paramNames.add(names.new DmsOutputParameter("OUTPUT_FILENAME"));
        paramNames.add(names.new DmsOutputParameter("OUTPUT_NAME"));
        paramNames.add(names.new DmsOutputParameter("OUTPUT_ROWS"));
        paramNames.add(names.new DmsOutputParameter("OUTPUT_TYPE"));
        paramNames.add(names.new DmsOutputParameter("OVERWRITE_OUTPUT_FILE"));
        paramNames.add(names.new DmsOutputParameter("QUERY_NAME"));
        paramNames.add(names.new DmsOutputParameter("QUERY_RESULT_ID"));
        paramNames.add(names.new DmsOutputParameter("QUERY_RESULT_TABLE"));
        paramNames.add(names.new DmsOutputParameter("QUERY_TYPE"));
        paramNames.add(names.new DmsOutputParameter("REPORT_FILENAME"));
        paramNames.add(names.new DmsOutputParameter("REPORT_ID"));
        paramNames.add(names.new DmsOutputParameter("REPORT_NAME"));
        paramNames.add(names.new DmsOutputParameter("REPORT_TITLE"));
        paramNames.add(names.new DmsOutputParameter("RUN_DATE"));
        paramNames.add(names.new DmsOutputParameter("RUN_ID"));
        paramNames.add(names.new DmsOutputParameter("RUN_STATUS"));
        paramNames.add(names.new DmsOutputParameter("RUN_TIME"));
        paramNames.add(names.new DmsOutputParameter("SAVE_TO_DATABASE"));
        paramNames.add(names.new DmsOutputParameter("BATCH"));
    }

    public static boolean isDmsOutputParamater(String paramName) {
        return paramNames.contains(new DmsOutputParameterUtil().new DmsOutputParameter(paramName));
    }

    public static void removeDmsOutputParameters(Table reportParameters) {
        for (int row = reportParameters.getRowCount() - 1; row >= 0; row--) {
            String paramName = reportParameters.getString(0, row);
            if (paramNames.contains(new DmsOutputParameterUtil().new DmsOutputParameter(paramName))) {
                reportParameters.removeRow(row);
            }
        }
    }
    
    public static HashMap<String, String> parameterTableToHashMap(Table reportParameters) {
        HashMap<String, String> map = new HashMap<>();
        String columnName = reportParameters.getColumnNames().contains(COL_NAME_NEW) ? COL_NAME_NEW : COL_NAME;
        String columnValue = reportParameters.getColumnNames().contains(COL_VALUE_NEW) ? COL_VALUE_NEW : COL_VALUE;
        for (TableRow row : reportParameters.getRows()) {
        	map.put(row.getString(columnName), row.getString(columnValue));
        }
        return map;
    }

    
    public static Table parameterSetToTable(Session session, Set<DefinitionParameter> reportParameters) {
        try (Table params = session.getTableFactory().createTable()) {
            params.addColumn("expr_param_name", EnumColType.String);
            params.addColumn("expr_param_value", EnumColType.String);
            for (DefinitionParameter param : reportParameters) {
                TableRow row = params.addRow();
                row.setValues(new Object[] {param.getDataName(), param.getDataValue()});
            }
            return params.cloneData();
        }
    }
}
