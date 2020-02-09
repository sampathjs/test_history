package com.matthey.openlink.utilities.picklists;

import com.matthey.openlink.utilities.DataAccess;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractTransactionListener;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Application;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;

/**
 * D422(4.12) Dispatch workflow 
 * Consignee({@value #CONSIGNEE}) field is being set/populated, therefore capture which addresses are valid for the associated picklist({@value #CONSIGNEE_ADDRESS})
 * The only address entries to be considered are those of type {@value #CONSIGNEE}!
 * 
 * @version $Revision: $
 * @deprecated This plugin has been converted to a tranfield OPS (see resulting class of this conversion {@link ConsigneeTranfieldNotification})
 */
@ScriptCategory( {EnumScriptCategory.FieldNotification})
public class ConsigneeNotification extends AbstractTransactionListener {

	private static final String CONSIGNEE = "Consignee";
	private static final String CONSIGNEE_ADDRESS = "Consignee Address";
	private static final int DISPATCH_CONFIG = 42201;
	
	static protected Table activeSelection = null;
	
	static protected Table none =null;
	
	static {
		int na=-1;
		none=Application.getInstance().getCurrentSession().getTableFactory().createTable();
		none.addColumns("Int["+"id"+"], String["+"address"+"]");
		none.addRow();
		none.setValue("id", 0, -1);
		none.setValue("address", 0, "None");
	}
	
	@Override
	public void notify(Context context, Transaction transaction) {
		Field tranInfo1 = transaction.getField(CONSIGNEE);
		Logging.info(
				String.format("FIELD:%s->%d\t\t%s",tranInfo1.getName(), tranInfo1.getValueAsInt(), tranInfo1.getValueAsString()));
		
		if (tranInfo1!=null && tranInfo1.getValueAsString().trim().length()>0) {
					
		//identify valid addresses
		Table consigneeAddress = DataAccess.getDataFromTable(context,
				String.format("SELECT pa.party_address_id as id, " + 
						"		pa.description as address " + 
						"\nFROM party_address pa " + 
						"\nJOIN party_address_type pat ON pa.address_type = pat.address_type_id AND pat.address_type_name in( %s )" + 
						"\nWHERE pa.party_id=%d " ,
							String.format("'%s','%s'",CONSIGNEE, "Main") // CR09NOV2015 - Client requested Main & Consignee Address
							,tranInfo1.getValueAsInt()));

		if (null == consigneeAddress || consigneeAddress.getRowCount() < 1) {
			context.getDebug().printLine("\n\tNo Address");
			if (activeSelection!=null && activeSelection!=none) {
				activeSelection.dispose();
			} 
			activeSelection = none;
			//throw new DispatchConsigneeException("Configuration data", DISPATCH_CONFIG,"No address information available");
			
		} else 
			activeSelection = consigneeAddress;
		
		Field tranInfoTarget = transaction.getField(CONSIGNEE_ADDRESS);		
		// populate consignee
		if (consigneeAddress.getRowCount()==1)
				Logging.info(
					"\n\tAddress=" + consigneeAddress.getString("address", 0));
			tranInfoTarget.setValue(consigneeAddress.getString("address", 0));			
		}
		
	}

	
}
