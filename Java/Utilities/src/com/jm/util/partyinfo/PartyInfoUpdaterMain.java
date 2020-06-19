/*
 * Main script for Task - Party Info Updater.
 * 
 * This task can be used to update value of any party info field (of Picklist type like Form, GT Active etc) 
 * for multiple external business units in one go. 
 * Param fields - External BU (multi-select), Party Info Field (single select), Picklist Values (single select).								   
 * 
 * History:
 * 2020-06-16	V1.0	-	Arjit  -	Initial Version
 * 
 **/

package com.jm.util.partyinfo;

import com.olf.embedded.application.Context;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.staticdata.WritableBusinessUnit;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;

public class PartyInfoUpdaterMain extends AbstractGenericScript {
	
	@Override
	public Table execute(Context context, ConstTable table) {
		return process(context, table);
	}
	
	protected Table process(Context context, ConstTable argt) {
		try {
			Logging.init(context, this.getClass(), "", "");
			Logging.info("Started PartyInfoUpdater main script");
			int rows = argt.getRowCount();
			if (rows == 0) {
				Logging.info("No rows retrieved from argt");
				return null;
			}
			
			Logging.info(rows + " rows retrieved from argt");
			StaticDataFactory sdf = context.getStaticDataFactory();
			
			for (int row = 0; row < rows; row++) {
				String name = argt.getString("external_bunit", row);
				String infoField = argt.getString("party_info", row);
				String value = argt.getString("field_value", row);
				
				Logging.info("For row#" + row + ", Party: " + name);
				WritableBusinessUnit wbu = sdf.getWritableReferenceObject(WritableBusinessUnit.class, name);
				String oldValue = wbu.getField(infoField).getValueAsString();
				if (oldValue != null && value.equals(oldValue)) {
					Logging.info("Skipping save as old value & new value " + value + " are same for info field: " + infoField + " for party: " + name);
					continue;
				}
				
				wbu.getField(infoField).setValue(value);
				wbu.save();
				Logging.info("Successfully saved " + infoField + " info field for party: " + name + " with value: " + value);
			}
			Logging.info("Ended PartyInfoUpdater main script");
			
		} catch (Exception e) {
			Logging.error("Error in executing main script: " + e.getMessage(), e);
			throw e;
			
		} finally {
			Logging.close();
		}
		
		return null;
	}

}
