package com.matthey.openlink.utilities;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.OpService;
import com.olf.openjvs.Tpm;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.TRANF_FIELD;

public class DispatchStatus implements IScript {

	@Override
	public void execute(IContainerContext context) throws OException {


		try {
			for (int i = 1; i <= OpService.retrieveNumTrans(); i++)
			{
				Transaction tran = OpService.retrieveTran(i);
				
				int tranNum = tran.getTranNum();
				Transaction prevTran = OpService.retrieveOriginalTran(i);
				
				String newInfoValue = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.jvsValue(), 0, DISPATCH_STATUS);
				String prevInfoValue = prevTran.getField(TRANF_FIELD.TRANF_TRAN_INFO.jvsValue(), 0, DISPATCH_STATUS);
				if (0==newInfoValue.compareToIgnoreCase("Awaiting Shipping") && 0!=prevInfoValue.compareToIgnoreCase(newInfoValue)) {
					OConsole.message(String.format("Tran %d set READY TO DISPATCH!!!", tran.getTranNum()));
					Tpm.startWorkflow(DISPATCHER);
				}
			}
			
		} catch (OException e) {			
	 			OConsole.message("Unexpected OException encountered when performing execute:\n\t" + e.getLocalizedMessage());
				throw e;
	    	}		
    		
    		
	}

	static 	String  DISPATCH_STATUS ="Dispatch Stauts";
	static String DISPATCHER="Dispatch-Safe";
	
}
