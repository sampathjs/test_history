package com.olf.jm.fixGateway.jpm.fieldMapper;

import com.olf.jm.fixGateway.fieldMapper.FieldMapperBaseForUserTable;
import com.olf.jm.fixGateway.fieldMapper.FieldMapperException;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2020-05-13 - V0.1 - jwaechter - Initial Version
 */

/**
 * Class responsible for mapping the Internal Contact.
 */
public class InternalContact extends FieldMapperBaseForUserTable {
	private static final String REQUIRED_PARTY_ROLE = "ORDER_ORIGINATION_TRADER";
	private static final String REPEATING_GROUP_FIELD_NAME = "RepeatingGroup_NoPartyIDs";
	private static final String PARTIES_TABLE = "Parties";

	public InternalContact () {
		super("JPM Execute", "Int Contact");
	}
	
	public InternalContact (final String mapName, final String mapType) {
		super(mapName, mapType);
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldName()
	 */
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_INTERNAL_CONTACT; 
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTagFieldName()
	 */
	@Override
	public String getTagFieldName() {
		return null; // see getComplexTagValue instead.// see getComplexTagValue instead.
	}

	@Override
	protected String getComplexTagValue(Table message) throws FieldMapperException {
		// TODO: retrieval logic also for EnumExecutionReport.DELIVER_TO_SUB_ID.getTagName();
		Table partiesTable = null;
		partiesTable = retrievePartiesTable(message);
		try {
			for (int row=partiesTable.getNumRows(); row >= 1; row--) {
				String partyRole = partiesTable.getString("PartyRole", row);
				if (partyRole.equalsIgnoreCase(REQUIRED_PARTY_ROLE)) {
					return partiesTable.getString("PartyID", row);
				}
			}					
			throw new FieldMapperException ("Party role '" + REQUIRED_PARTY_ROLE + "' not found in "
					+ " table '" + PARTIES_TABLE + "' in repeating groups table '" + REPEATING_GROUP_FIELD_NAME + "'");
		} catch (OException ex) {
			Logging.error (ex.toString());
			for (StackTraceElement ste : ex.getStackTrace()) {
				Logging.error (ste.toString());
			}
			throw new FieldMapperException ("Error retrieving ID of trader: " + ex.toString());			
		}
	}

	public Table retrievePartiesTable(Table message) throws FieldMapperException {
		Table repGroupTable = null;
		try {
			int colNum = message.getColNum(REPEATING_GROUP_FIELD_NAME);
			if (colNum > 0) {
				repGroupTable = message.getTable(colNum, 1);
			} else {
				throw new FieldMapperException ("Repeating group field '" + REPEATING_GROUP_FIELD_NAME + "' not found.");
			}
		} catch (OException e) {			
			Logging.error (e.toString());
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error (ste.toString());
			}
			throw new FieldMapperException ("Error retrieving repeating group field '" + REPEATING_GROUP_FIELD_NAME + "'");
		}
		try {
			if (repGroupTable.getNumRows() == 0) {
				throw new FieldMapperException ("Table '" + REPEATING_GROUP_FIELD_NAME + "' does not contain any rows.");
			}
			int colNum = repGroupTable.getColNum(PARTIES_TABLE);
			if (colNum > 0) {
				return (repGroupTable.getTable(colNum, 1));
			} else {
				throw new FieldMapperException ("Repeating group table'" + REPEATING_GROUP_FIELD_NAME + 
						"' does not contain column '" + PARTIES_TABLE + "'");				
			}
		} catch (OException e) {			
			Logging.error (e.toString());
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error (ste.toString());
			}
			throw new FieldMapperException ("Error retrieving data table '" + PARTIES_TABLE + "' in table " + REPEATING_GROUP_FIELD_NAME + "'");
		}
	}
}
