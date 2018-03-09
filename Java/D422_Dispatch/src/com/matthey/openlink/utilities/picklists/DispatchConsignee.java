package com.matthey.openlink.utilities.picklists;

import com.matthey.openlink.utilities.DataAccess;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.embedded.generic.AbstractPickList;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.ReferenceChoice;
import com.olf.openrisk.staticdata.ReferenceChoices;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;

/**
 * D422(4.12) Dispatch workflow
 * manage the results given to the user picklist, removing all those potential entries where the name contains the value {@value #LEGAL_ENTITY}
 * <br><b>{@code assumption:}</b> is that the id is that of the core table within the system. The significance of this is in the filtering of the ConsigneeAddress is uses the id selected for this field to query valid address entries from the system.
 * 
 * @version $Revision:  $
 */
@ScriptCategory({ EnumScriptCategory.PickList })
public class DispatchConsignee extends AbstractPickList{

	private static final String LEGAL_ENTITY = "Legal";

	@Override
	public ReferenceChoices getChoices(Context context, ReferenceChoices choices) {
	     // Create a new, empty set of reference choices
			  //context.getDebug().viewTable(choices.asTable());
			Table consignees = DataAccess.getDataFromTable(context,
					String.format("SELECT pf.*, pc.name" + 
							"\nFROM party_function pf " + 
							"\nJOIN function_type ft ON pf.function_type=ft.id_number AND ft.name = '%s'" + 
							"\nJOIN party p ON pf.party_id=p.party_id " + 
							"\nJOIN party_class pc ON p.party_class=pc.id_number" , "Carrier"));
			
			
			
			
		if (null == consignees || consignees.getRowCount() < 1) {
			consignees.dispose();		
			return null;
		} 

			 
		      ReferenceChoices newChoices = context.getStaticDataFactory().createReferenceChoices();

		      for ( ReferenceChoice e : choices) {
		    	  int row;
					//if (!e.getName().contains(LEGAL_ENTITY)) {
		    		  if ( (row = consignees.find(0, e.getId(), 0)) >=0) { 
		    			  if (!consignees.getString("name",  row).contains(LEGAL_ENTITY))
		    			  newChoices.add(e.getId(), e.getName());
		    		  }
		    	  //}
		      }
		 
		      consignees.dispose();
		      // Return the set of choices
		      return newChoices;

		     }
}

	