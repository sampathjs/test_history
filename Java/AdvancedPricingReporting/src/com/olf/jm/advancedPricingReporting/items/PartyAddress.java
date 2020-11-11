package com.olf.jm.advancedPricingReporting.items;

import java.util.Arrays;

import com.olf.embedded.application.Context;
import com.olf.jm.advancedPricingReporting.reports.ReportParameters;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.jm.logging.Logging;


/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class PartyAddress. Defines the data that makes up a party address to be output
 * on the report. 
 */
public class PartyAddress extends ItemBase {

	/** The Constant columnTypes. */
	private static final EnumColType[] columnTypes = new EnumColType[] { EnumColType.Int,
		EnumColType.String, EnumColType.String, EnumColType.String, EnumColType.String, EnumColType.String,
		EnumColType.String, EnumColType.String, EnumColType.String,
		EnumColType.String, EnumColType.String };
	
	/** The Constant columns. */
	private static final String[] columns = new String[] { "party_id", "short_name",
		"address_line_1", "address_line_2", "address_line_3", "address_line_4", "city", "country", "phone",
		"fax", "description" };
	
	
	/** The Constant INTERNAL_PREFIX. */
	private static final String INTERNAL_PREFIX = "internal";
	
	/** The Constant EXTERNAL_PREFIX. */
	private static final String EXTERNAL_PREFIX = "external";
	

	public enum EnumPartyAddressType {
		MAIN(1),	
		CONFIRM_TO(20001),
		INVOICE_TO(20002),
		CONSIGNEE(20003),
		REGISTERED_OFFICE(20004);
		
		/** The id. */
		private final int id;
		
		/**
		 * Instantiates a new enum party address type.
		 *
		 * @param id the id
		 */
		EnumPartyAddressType(int id) {
			this.id = id;
		}
		
		/**
		 * Gets the address type.
		 *
		 * @return the address type
		 */
		public int getAddressType() {
			return id;
		}
		
	}

	public enum EnumPartyType {
		INTERNAL,
		EXTERNAL
	}
	
	/** The address type. */
	private final EnumPartyAddressType addressType;
	
	/** The party type. */
	private final EnumPartyType partyType;
	
	/**
	 * Instantiates a new party address.
	 *
	 * @param context the context
	 * @param addressType the address type
	 * @param partyType the party type
	 */
	public PartyAddress(Context context, EnumPartyAddressType addressType, EnumPartyType partyType) {
		super(context);
		this.addressType = addressType;
		
		this.partyType = partyType;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getDataTypes()
	 */
	@Override
	public EnumColType[] getDataTypes() {

		
		return columnTypes;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.SectionItem#getColumnNames()
	 */
	@Override
	public String[] getColumnNames() {
		
		String[] outputColumns = Arrays.copyOf(columns, columns.length);
		String columnPrefix = EXTERNAL_PREFIX;
		if(partyType == EnumPartyType.INTERNAL) {
			columnPrefix = INTERNAL_PREFIX;
		} 
		
		for(int i = 0; i < columns.length; i++) {
			outputColumns[i] = columnPrefix + "_" + columns[i];
		}
		
		return outputColumns;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.items.ItemBase#addData(com.olf.openrisk.table.Table, com.olf.jm.advancedPricingReporting.reports.ReportParameters)
	 */
	@Override
	public void addData(Table toPopulate, ReportParameters reportParameters) {
		super.addData(toPopulate, reportParameters);
		
		int partyId;
		
		String partyIdColumn;

		if(partyType == EnumPartyType.INTERNAL) {
			partyId = reportParameters.getInternalBu();
			partyIdColumn = INTERNAL_PREFIX + "_party_id";
		} else {
			partyId = reportParameters.getExternalBu();
			partyIdColumn = EXTERNAL_PREFIX + "_party_id";
		}
		
		toPopulate.setInt(partyIdColumn, 0, partyId);
		Table partyData = loadPartyData(partyId, addressType.getAddressType());
				
		String what = buildWhatColumnClause();
		String where = "[In.party_id] == [Out." + partyIdColumn + "]";
		Logging.debug("Table select [" + what + "] + where [" + where + "]");
		toPopulate.select(partyData, what, where );
	}
	
	/**
	 * Builds the what column clause used in the table select when copying the data to the report table.
	 *
	 * @return formatted what clause that can be used in a table select
	 */
	private String buildWhatColumnClause() {
		StringBuilder whatClause = new StringBuilder();
		
		String[] outputName = getColumnNames();
		
		for(int i = 0; i < columns.length; i++) {
			if(whatClause.length() != 0) {
				whatClause.append(", ");
			}
			whatClause.append(columns[i]).append("->").append(outputName[i]);
		}
		
		return whatClause.toString();
	}
	
	
	/**
	 * Load party data.
	 *
	 * @param partyId the party id
	 * @param addressType the address type
	 * @return the table
	 */
	private Table loadPartyData(int partyId, int addressType) {
		String sql =
				" select  p.party_id as party_id, short_name, long_name, addr1 as address_line_1, addr2 as address_line_2, " +
				" pa.addr_reference_name as address_line_3, " +
				" pa.irs_terminal_num as address_line_4, " +
				" city, name as country,  phone, fax, description " +
				" from party p " +
				" join party_address pa on p.party_id = pa.party_id  and address_type = " +
				addressType +
				" join country c on c.id_number = pa.country" +
				" where p.party_id = " +
				partyId;
		return runSQL(sql);
		
	}

}
