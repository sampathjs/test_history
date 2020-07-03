package com.matthey.openlink.utilities.picklists;

import com.matthey.openlink.utilities.DataAccess;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractFieldListener;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Application;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;

/*
 * History:
 * 2016-03-30	V1.0	jwaechter	- created as conversion of ConsigneeNotification to a tran field script.
 * 2020-03-25	V1.1	YadavP03	- memory leaks & formatting changes
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcTranfield })
public class ConsigneeTranfieldNotification extends AbstractFieldListener {
	private static final String CONSIGNEE = "Consignee";
	private static final String CONSIGNEE_ADDRESS = "Consignee Address";
	
	static protected Table activeSelection = null;
	
	static protected Table none =null;
	
	static {
		none=Application.getInstance().getCurrentSession().getTableFactory().createTable();
		none.addColumns("Int["+"id"+"], String["+"address"+"]");
		none.addRow();
		none.setValue("id", 0, -1);
		none.setValue("address", 0, "None");
	}
	
    public void postProcess(final Session session, final Field field, final String oldValue, final String newValue,
            final Table clientData) {
		try {
		Logging.init(session, this.getClass(), "", "");
    	Transaction transaction = field.getTransaction();
		
		if (newValue.trim().length()>0) {
			int consigneePartyId = session.getStaticDataFactory().getId(EnumReferenceTable.Party, newValue);
					
		//identify valid addresses
		Table consigneeAddress = DataAccess.getDataFromTable(session,
				String.format("SELECT pa.party_address_id as id, " + 
						"		pa.description as address " + 
						"\nFROM party_address pa " + 
						"\nJOIN party_address_type pat ON pa.address_type = pat.address_type_id AND pat.address_type_name IN ( %s )" + 
						"\nWHERE pa.party_id=%d " ,
							String.format("'%s','%s'",CONSIGNEE, "Main") // CR09NOV2015 - Client requested Main & Consignee Address
							,consigneePartyId));

		if (null == consigneeAddress || consigneeAddress.getRowCount() < 1) {
			Logging.info("No Address for party " + newValue);
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
				Logging.info("\n\tAddress=" + consigneeAddress.getString("address", 0));
			tranInfoTarget.setValue(consigneeAddress.getString("address", 0));			
		}
		} catch (Exception e) {
			Logging.error(e.getMessage(), e);
			throw new RuntimeException("Consignee Notification failed", e);
		} finally {
		Logging.close();
		}
    }
	
	
}
