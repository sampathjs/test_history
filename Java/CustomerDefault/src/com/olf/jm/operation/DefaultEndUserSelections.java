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



import com.olf.jm.logging.Logging;

@ScriptCategory({ EnumScriptCategory.EventNotification })
public class DefaultEndUserSelections extends AbstractFieldEventListener {

	@Override
	public ReferenceChoices getChoices(Session session, Field field, ReferenceChoices choices) {
		Table temp = null; 
		try{
		Logging.init(this.getClass(), "", "");
		Transaction tran = field.getTransaction();
		String extBU = null;
		if (tran.getToolset() == EnumToolset.Cash){
			String insSubGroup = tran.getValueAsString(EnumTransactionFieldId.InstrumentSubType);
			if (insSubGroup.equalsIgnoreCase(EnumInsSub.CashTransfer.getName())) {
				extBU = tran.getValueAsString(EnumTransactionFieldId.FromBusinessUnit);
			}
			else {
				extBU = tran.getValueAsString(EnumTransactionFieldId.ExternalBusinessUnit);
			}
		} else {
			extBU = tran.getValueAsString(EnumTransactionFieldId.ExternalBusinessUnit);
		}
		
		String sqlString = "SELECT * FROM user_jm_end_user_view WHERE is_end_user = 1 AND jm_group_company = '" + extBU + "'";
		
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

	private ReferenceChoice findChoiseIgnoreCase(ReferenceChoices choices, String extBU) {
		ReferenceChoice rc = choices.findChoice(extBU);
		if (rc != null){
			return rc;
		}
		return null;
	}	

}