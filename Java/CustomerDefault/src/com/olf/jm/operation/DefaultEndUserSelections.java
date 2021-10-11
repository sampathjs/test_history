package com.olf.jm.operation;

import com.olf.embedded.trading.AbstractFieldEventListener;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.ReferenceChoice;
import com.olf.openrisk.staticdata.ReferenceChoices;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumInsSub;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;

/*
* 2021-10-11   V1.1	BhardG01  EPI-1532  - WO0000000007327 _Location Pass through deals failed to Book in v14 as well as V17
*/

import com.olf.jm.logging.Logging;

@ScriptCategory({ EnumScriptCategory.EventNotification })
public class DefaultEndUserSelections extends AbstractFieldEventListener {

	@Override
	public ReferenceChoices getChoices(Session session, Field field, ReferenceChoices choices) {
		Table temp = null; 
		Transaction tran = field.getTransaction();
		if(!isPassThroughDeals(tran)){
		try{
		Logging.init(this.getClass(), "", "");
		String extBU = null;
		int isEndUser =1;
		if (tran.getToolset() == EnumToolset.Cash){
			String insSubGroup = tran.getValueAsString(EnumTransactionFieldId.InstrumentSubType);
			if (insSubGroup.equalsIgnoreCase(EnumInsSub.CashTransfer.getName())) {
				extBU = tran.getValueAsString(EnumTransactionFieldId.FromBusinessUnit);
			}
			else {
				extBU =  tran.getValueAsString(EnumTransactionFieldId.ExternalBusinessUnit);
			}
		} 
		
		else {
			extBU = tran.getValueAsString(EnumTransactionFieldId.ExternalBusinessUnit);
		}
		
		String sqlString = "SELECT * FROM user_jm_end_user_view WHERE is_end_user = "+isEndUser+" AND jm_group_company = '" + extBU + "'";
		
		try {
			temp = session.getIOFactory().runSQL(sqlString);
		} catch (Exception e) {
			Logging.error("user_jm_end_user_view not created. \n");
			return choices;
		}
		
		ReferenceChoices rcs = session.getStaticDataFactory().createReferenceChoices();
		if (temp.getRowCount() == 0) {
			// If extBU is not a JM Group Company  
			try {
				rcs.add(findChoiseIgnoreCase(choices, extBU));
			} catch (Exception e) {
				Logging.error(extBU + " is not in the picklist(case sensitive). \n");
			}
		} 
		else {
			for (TableRow row: temp.getRows()){
				try {
					rcs.add(findChoiseIgnoreCase(choices, row.getString(1)));
				} catch (Exception e) {
					Logging.error(row.getString(1) + " is not in the picklist(case sensitive). \n");
				}
			}
		}
		return rcs;
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}finally{
			temp.dispose();
			Logging.close();
		}
	}
		return null;
		
	}

	private boolean isPassThroughDeals(Transaction tran){		 
		return tran.getField("PassThrough dealid").isApplicable();
		
	}
	private ReferenceChoice findChoiseIgnoreCase(ReferenceChoices choices, String extBU) {
		ReferenceChoice rc = choices.findChoice(extBU);
		if (rc != null){
			return rc;
		}
		return null;
	}	

}