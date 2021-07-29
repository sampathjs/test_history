package com.matthey.openlink.trading.opsvc;

import com.olf.embedded.trading.AbstractTransactionListener; 
import com.olf.openrisk.trading.Transaction;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;

/*
 * History:
 *              V1.1                           - Initial Version
 * 2021-05-31   V1.2    Gaurav   EPI-1532      - WO0000000007327 - Location Pass through deals failed 
 * 												 to Book in v14 as well as V17  
 * 
 *
 */

@ScriptCategory({ EnumScriptCategory.FieldNotification })
public class PassThroughClearTran extends AbstractTransactionListener {
	@Override
	public void notify(Context context, Transaction tran) {  
		if(tran.getField("PassThrough dealid").isApplicable() && tran.getField("PassThrough dealid").getValueAsInt()>0){
			tran.getField("PassThrough dealid").setValue(""); 
		}
		
	}
	}
