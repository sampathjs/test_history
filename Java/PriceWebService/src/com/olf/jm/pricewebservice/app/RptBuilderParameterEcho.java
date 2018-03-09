package com.olf.jm.pricewebservice.app;

import java.util.HashMap;
import java.util.Map;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.openlink.util.logging.PluginLog;

/**
 * ReportBuilder plugin to be used as DataSource.
 * It returns all arguments passed on to this datasource in a single row, such that
 * column name = argument name and column value of first row = argument value.
 * This allows arguments to be used in joins.
 * @author jwaechter
 * @version 1.0
 */
public class RptBuilderParameterEcho implements IScript{
	private Map<String, String> paramsToValues;
	private Map<String, Integer> paramsToTypes;
	
	@Override
	public void execute(IContainerContext context) throws OException {
		try {
			init (context);
			Table returnt = context.getReturnTable();
			returnt.addRow();
			for (String paramName : paramsToValues.keySet()) {
				String valueAsString=paramsToValues.get(paramName);
				int type=paramsToTypes.get(paramName);
				
				switch (type) {
				case 0:
					
					break;
				case 1:  // integer
					returnt.addCol(paramName, COL_TYPE_ENUM.COL_INT);
					returnt.setInt(paramName, 1, Integer.parseInt(valueAsString));
					break;
				case 2:
					break;
				case 3:
					break;
				case 4:
					break;
				case 5: // String
					returnt.addCol(paramName, COL_TYPE_ENUM.COL_STRING);
					returnt.setString(paramName, 1, valueAsString);					
					break;
				}
			}
			returnt.viewTable();
		} catch (Throwable ex) {
			OConsole.oprint(ex.toString());
			PluginLog.error(ex.toString());
			throw ex;
		} 
	}

	private void init(IContainerContext context) throws OException {
		Table argt = context.getArgumentsTable();
		if (argt.getNumRows() == 0 || argt.getColNum("PluginParameters") == 0) {
			throw new OException ("ARGT invalid: " + getClass().getName() + " requires to be run inside the ReportBuilder");
		}
		paramsToValues = new HashMap<> ();
		paramsToTypes = new HashMap<> ();
		Table pluginParameters = argt.getTable("PluginParameters", 1);
		for (int row=pluginParameters.getNumRows(); row >= 1; row--) {
			String name = pluginParameters.getString("parameter_name", row);
			String value = pluginParameters.getString("parameter_value", row);
			int type = pluginParameters.getInt("parameter_type", row);
						
			paramsToValues.put(name, value);
			paramsToTypes.put(name, type);
		}	
	}
}
