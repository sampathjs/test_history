package com.matthey.openlink.utilities.picklists;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PickList;
import com.olf.openrisk.staticdata.ReferenceChoices;
import com.olf.openrisk.table.TableRow;

/*
 * History:
 * 2015-MM-DD	V1.0	pwallace	- initial version
 * 2016-03-30	V1.1	jwaechter	- changed super class from ConsigneeNotification
 *                                    to ConsigneeTranfieldNotification
  */

/**
 * D422(4.12) Dispatch workflow
 * dependent on the sequence of data capture to require population of the Consignee before this field
 * If the user goes directly to this field then all valid addresses will be shown... 
 *
 * @version $Revision: $
 */
@ScriptCategory({ EnumScriptCategory.PickList })
public class DispatchConsigneeAddress extends ConsigneeTranfieldNotification implements PickList {

	@Override
	public ReferenceChoices getChoices(Context context, ReferenceChoices choices) {
		
		  if (activeSelection==null) {
			  choices.clear();
			  return choices;
		  }
		  
	      ReferenceChoices newChoices = context.getStaticDataFactory().createReferenceChoices();
	      for ( TableRow  assignee: activeSelection.getRows()) {
	    		 newChoices.add(assignee.getInt(0), assignee.getString(1));
	      }
	      newChoices.remove(0);
	      if(choices.size() > newChoices.size())
	    	  choices.clear();
	      // Return the set of choices
	      return newChoices;
	}

}
