package com.matthey.openlink.utilities.stub;

import java.util.concurrent.atomic.AtomicReference;

import com.olf.embedded.application.Context;
import com.olf.embedded.tpm.AbstractProcessStep;
import com.olf.openjvs.Tpm;
import com.olf.openrisk.application.EnumOlfDebugType;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.staticdata.EnumFieldType;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.Token;
import com.olf.openrisk.tpm.Variable;
import com.olf.openrisk.tpm.Variables;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.matthey.openlink.utilities.tpm.TpmVariables;
 
@ScriptCategory({EnumScriptCategory.TpmStep})
public class RetreiveLGD extends AbstractProcessStep {
	
    @Override
    public Table execute(Context context, Process process, Token token,
            Person submitter, boolean transferItemLocks, Variables variables) {
 

    	TpmVariables tpmVariables = new TpmVariables(context, process, variables);

    	
    	Variable lgd = null;
//    	Variables tpmVars = process.getVariables();
		if (tpmVariables.contains("JM_LGD")) {
			lgd = tpmVariables.getVariable("JM_LGD");
		} else if (tpmVariables.contains("UK_LGD")) {
			lgd = tpmVariables.getVariable("UK_LGD");
		} else if (tpmVariables.contains("US_LGD")) {
			lgd = tpmVariables.getVariable("US_LGD");
		} if (lgd == null && tpmVariables.contains("Our_LGD"))  {
			lgd=tpmVariables.getVariable("Our_LGD");
			long lgdId = psuedoLGB();
			System.out.println(String.format("LGD:%d",lgdId));
			lgd.setValue(lgdId);
			process.setVariable(lgd);
			
		} else
			throw new OpenRiskException("Variable not defined before use [JM|UK|US]_LGD");
				
        return null;
    }
    

	private static AtomicReference<Long> lastTime= new AtomicReference<Long>(System.currentTimeMillis());
    private long psuedoLGB() {
    	long nextValue = System.currentTimeMillis();
    	long last;
    	do {
    		last = lastTime.get();
    		nextValue = nextValue > last ?  nextValue : last +1;
    	} while (lastTime.compareAndSet(last, nextValue));

    	return nextValue;
    }
}