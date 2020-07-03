package com.matthey.openlink.utilities.tpm;

import com.olf.embedded.application.Context;
import com.olf.openrisk.application.EnumOlfDebugType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.Variable;
import com.olf.openrisk.tpm.Variables;

/*
 * History:
 * 2020-03-25	V1.1	YadavP03	- memory leaks, remove console print & formatting changes
 */

public class TpmVariables  {

	private final Process parent;
	private final Context context;
	
	public TpmVariables(Context context, Process process, Variables variables) {

//		 if (variables == null)
//	        	parent = process.getVariables();
//		 else
//			 parent = variables;
		parent=process;
//		 for(Variable variable : variables) {
//			 super.add(variable);
//		 }
//		parent = variables;
		this.context=context;
	}

	public boolean contains(Object o) {
		if (o instanceof String) {
			String variable = (String)o;
			if (null!=parent.getVariable(variable))
				return true;
		}
		
		return parent.getVariables().contains(o);
	}
	
	public Table asTable() {
		return parent.getVariables().asTable();
	}
	public Table asTable(String name) {
		return parent.getVariables().asTable(name);
	}
	

	public TpmVariables listVariables()  {
		
        if (context.getDebug().isDebugTypeActive(EnumOlfDebugType.Opencomponent) 
        		|| context.getDebug().isDebugTypeActive(EnumOlfDebugType.Tpm)) {

            for (Variable tpmvar : parent.getVariables()) {
        		//System.out.print(String.format("VAR: >%s<\t>%s<\t",tpmvar.getName(), tpmvar.getDataType().toString()));
            	switch (tpmvar.getDataType()) {
            	case Boolean:
            		//System.out.println(String.format(">Value:%b:", tpmvar.getValueAsBoolean()));
            		break;
            	case Int:
            	case Long:
            		//System.out.println(String.format(">Value:%d:",tpmvar.getValueAsInt()));
            		break;
            	case Double:
            		//System.out.println(String.format(">Value:%f:", tpmvar.getValueAsDouble()));
            		break;
            	case Date:
            		//System.out.println(String.format(">Value:%s:", tpmvar.getValueAsDate().toString()));
            		break;
            	case DateTime:
            		//System.out.println(String.format(">Value:%s:",tpmvar.getValueAsDateTime().toString()));
            		break;
            	case String:
            		//System.out.println(String.format(">Value:%s:",tpmvar.getValueAsString()));
            		break;
            		default:
            			//System.out.println(">Value: OTHER!");
            	}
            }
        }
        return this;
	}
	
	public Variable getVariable(String varName) {
		return parent.getVariable(varName);
	}
	
}
