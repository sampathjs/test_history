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
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

public class PartyInfoUpdaterMain extends AbstractGenericScript {
	
	@Override
	public Table execute(Context context, ConstTable table) {
		return process(context, table);
	}
	
	protected Table process(Context context, ConstTable argt) {
		Table log = null;
		try {
			Logging.init(context, this.getClass(), "", "");
			Logging.info("Started PartyInfoUpdater main script");
			int rows = argt.getRowCount();
			if (rows == 0) {
				Logging.info("No rows retrieved from argt");
				return null;
			}
			
			Table partyInfoTypes = retrievePartyInfoFields(context);
			log = context.getTableFactory().createTable();
			log.setName("Party Info Updater Log");
			log.addColumn("party_name", EnumColType.String);
			log.addColumn("filed_name", EnumColType.String);
			log.addColumn("field_value", EnumColType.String);
			log.addColumn("comments", EnumColType.String);
					
			for (TableRow row : argt.asTable().getRows()) {
				String name = row.getString("external_bunit");
				String infoField = row.getString("party_info");
				String value = row.getString("field_value");
				
				com.olf.openjvs.Table party = com.olf.openjvs.Ref.retrieveParty(com.olf.openjvs.Ref.getValue(SHM_USR_TABLES_ENUM.PARTY_TABLE, name));
				com.olf.openjvs.Table partyInfo = party.getTable("party_info", 1);
				
				int fieldId = partyInfoTypes.getValueAsInt("type_id", partyInfoTypes.find(partyInfoTypes.getColumnId("type_name"), infoField, 0));
				int isFieldApplicable = partyInfo.findInt("type_id", fieldId, SEARCH_ENUM.FIRST_IN_GROUP);	
				
				if(isFieldApplicable <= 0){
					int rowNum = partyInfo.addRow();
					partyInfo.setInt("type_id", rowNum, fieldId);
				}
				
				String oldValue = partyInfo.getString("value", partyInfo.unsortedFindInt("type_id", fieldId));			
				
				if (oldValue != null && value.equals(oldValue)) {
					Logging.info("Skipping save as old value & new value " + value + " are same for info field: " + infoField + " for party: " + name);
					TableRow logRow = log.addRow();
					log.setString("party_name", logRow.getNumber(), name);
					log.setString("filed_name", logRow.getNumber(), infoField);
					log.setString("field_value", logRow.getNumber(), value);
					log.setString("comments", logRow.getNumber(), "Skipping save as old value & new value " + value + " are same for info field: " + infoField);
					continue;
				}
				
				partyInfo.setString("value", partyInfo.unsortedFindInt("type_id", fieldId), value);			
				party.setTable("party_info", 1, partyInfo);
				com.olf.openjvs.Ref.updateParty(party);
				
				TableRow logRow = log.addRow();
				log.setString("party_name", logRow.getNumber(), name);
				log.setString("filed_name", logRow.getNumber(), infoField);
				log.setString("field_value", logRow.getNumber(), value);
				log.setString("comments", logRow.getNumber(), "Successfully saved info field '" + infoField + "' for party: " + name + " with value: " + value);
				
				Logging.info("Successfully saved info field '" + infoField + "' for party: " + name + " with value: " + value);
			}		
		} catch (Exception e) {
			Logging.error("Error in executing main script: " + e.getMessage(), e);		
		} finally {
			context.getDebug().viewTable(log);
			Logging.info("Ended PartyInfoUpdater main script");
			Logging.close();
		}
		
		return null;
	}
	
	private Table retrievePartyInfoFields(Context context) {
		String sqlString = "SELECT pit.type_id, pit.type_name "
				+ " FROM party_info_types pit \n" 
				+ " WHERE pit.party_class = 1 AND int_ext = 1" ;

		Table partyInfoFields = context.getIOFactory().runSQL(sqlString);
		return partyInfoFields;
	}
}
