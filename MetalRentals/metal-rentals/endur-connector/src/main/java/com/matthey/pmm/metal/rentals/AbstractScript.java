package com.matthey.pmm.metal.rentals;

import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.olf.embedded.application.EnumScriptCategory.Generic;

@ScriptCategory(Generic)
public abstract class AbstractScript extends AbstractGenericScript {
    private static final Logger logger = LogManager.getLogger(AbstractScript.class);

    @Override
    public Table execute(Session session, ConstTable constTable) {
        try {
            return run(session, constTable);
        } catch (Throwable e) {
            logger.error("error occurred: " + e.getMessage(), e);
            throw e;
        }
    }

    protected abstract Table run(Session session, ConstTable constTable);

    protected String getParameter(ConstTable argumentTable, String parameterName) {
        Table parameters = argumentTable.getTable("PluginParameters", 0);
        int row = parameters.find(parameters.getColumnId("parameter_name"), parameterName, 0);
        checkArgument(row > 0, "invalid parameter name: " + parameterName);
        return parameters.getString("parameter_value", row);
    }
}
