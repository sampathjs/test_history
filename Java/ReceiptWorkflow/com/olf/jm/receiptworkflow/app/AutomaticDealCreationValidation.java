package com.olf.jm.receiptworkflow.app;

import java.util.Date;

import com.olf.embedded.scheduling.AbstractNominationFieldListener;
import com.olf.embedded.scheduling.NominationFieldListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.jm.receiptworkflow.model.ConfigurationItem;
import com.olf.jm.receiptworkflow.model.FieldValidationException;
import com.olf.jm.receiptworkflow.model.RelNomField;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.EnumNominationFieldId;
import com.olf.openrisk.scheduling.FieldDescription;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.scheduling.Field;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;

/*
 * History: 
 * 2015-10-13	V1.0	jwaechter	- Initial Version
 * 2015-10-23	V1.1	jwaechter	- Enhanced error message
 */

/**
 * This library contains a nomfield notification script that is checking if the nom info 
 * field {@value #NOM_INFO_COUNTERPARTY} is being modified and if yes, it applies the following logic:
 * <ol>
 *   <li>
 *   	Retrieve the values of the fields "Receipt Deal", "Receipt Date", "Purity", "Form", "Net Weight", "Unit" 
 *   </li>
 *   <li>
 *   	Check if those values are designating untouched values or not applicable/readable fields:
 *   	<ul> 
 *   	  <li> 0.0 for doubles </li>
 *   	  <li> null, empty string or "None" for strings </li>
 *   	  <li> negative or 0 values  for integers  </li>
 *      </ul>
 *   </li>
 *   <li>
 *     If above is the case, generate an error message to the user showing which fields are not filled
 *     correctly and abort pre process showing this error message to the user..
 *   </li>
 *   <li>
 *     If the field "Warehouse Deal" is not set (negative or 0 value), block setting of field and show
 *     error message to user stating that the currently attached warehouse deal has to be unlinked before
 *     a new can be set.
 *   </li>
 *   <li> Otherwise succeed and continue </li>
 * </ol>
 * @author jwaechter
 * @version 1.1
 */
public class AutomaticDealCreationValidation  {
	public void process(Batch batch) {
		String missingFields = retrieveMissingFieldsList(batch);
		if (missingFields.length() > 0) {
			throw new FieldValidationException ("The following fields are not set correctly on nominaton #" 
					+ batch.getBatchId() + ":\n" + missingFields);
		}
	}

	private String retrieveMissingFieldsList(Batch  batch) {
		int warehouseDealNum=RelNomField.WAREHOUSE_DEAL.guardedGetInt(batch);
		Date receiptDate = RelNomField.RECEIPT_DATE.guardedGetDate(batch);
		String purity = RelNomField.PURITY.guardedGetString(batch);
		String form = RelNomField.FORM.guardedGetString(batch);
		double volume = RelNomField.VOLUME_FIELD.guardedGetDouble(batch);
		String unit = RelNomField.UNIT.guardedGetString(batch);
		String counterparty = RelNomField.COUNTERPARTY.guardedGetString(batch);
		
		StringBuilder missingFieldList = new StringBuilder ();
		boolean first=true;
		
		if ( warehouseDealNum <= 1) {
			missingFieldList.append("The Warehouse Deal Num must be set before a receipt can be created and linked by this batch");
			first = false;
		}
		if (receiptDate.equals(new Date (0))) {
			if (!first) {
				missingFieldList.append("\n");
			}
			first = false;
			missingFieldList.append("The Receipt Date must be set before a receipt can be created and linked by this batch");
		}
		if ( purity == null || purity.equals("") || purity.equalsIgnoreCase("None")) {
			if (!first) {
				missingFieldList.append("\n");
			}
			first = false;
			missingFieldList.append("The Purity must be set before a receipt can be created and linked by this batch");			
		}
		if (form == null || form.equals("") || form.equalsIgnoreCase("None")) {
			if (!first) {
				missingFieldList.append("\n");
			}
			first = false;
			missingFieldList.append("The Form must be set before a receipt can be created and linked by this batch");
		}
		if (volume == 0d) {
			if (!first) {
				missingFieldList.append("\n");
			}
			first = false;
			missingFieldList.append("The Volume must be set before a receipt can be created and linked by this batch");
		}
		if (unit == null || unit.equals("") || unit.equalsIgnoreCase("None")) {
			if (!first) {
				missingFieldList.append("\n");
			}
			missingFieldList.append("The Unit must be set before a receipt can be created and linked by this batch");			
		}
		
		if (counterparty == null || counterparty.trim().equals("")) {
			if (!first) {
				missingFieldList.append("\n");
			}
			missingFieldList.append("The Counterparty must be set before a receipt can be created and linked by this batch");			
		}
		
		return missingFieldList.toString();
	}
}
