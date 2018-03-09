package com.olf.jm.coverage.businessObjects.dataFactories;

import com.olf.jm.SapInterface.businessObjects.dataFactories.EndurParty;
import com.olf.jm.SapInterface.businessObjects.dataFactories.EnumInterfaceType;
import com.olf.jm.SapInterface.businessObjects.dataFactories.IEndurAccount;
import com.olf.jm.SapInterface.businessObjects.dataFactories.IEndurParty;
import com.olf.jm.SapInterface.businessObjects.dataFactories.IEndurParty.EnumPartyType;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.businessObjects.enums.ITableColumn;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;


/**
 * The Class SapPartyData. contains data used to map the sap internal / external parties to Endur in / ext LE, BU and 
 * portfolio.
 */
public class CoverageSapPartyData implements ISapPartyData {

	/**
	 * The Enum EnumPartyColumns.
	 */
	private enum EnumPartyColumns implements ITableColumn {

		/** The SAP input internal Id. */
		INPUT_INT_ID("input_int_id", EnumColType.String),
		
		/** The Endur internal LE. */
		INT_LE("int_le", EnumColType.String),	
		
		/** The Endur internal BU. */
		INT_BU("int_bu", EnumColType.String),	
		
		/** The internal portfolio. */
		INT_PORTFOLIO("int_portfolio", EnumColType.String),	
		
		/** The internal sap id help in endur. */
		INT_SAP_ID("int_sap_id", EnumColType.String),	
		
		/** The input SAP external id. */
		INPUT_EXT_ID("input_ext_id", EnumColType.String),	
		
		/** The endur external le. */
		EXT_LE("ext_le", EnumColType.String),	
		
		/** The endur external bu. */
		EXT_BU("ext_bu", EnumColType.String),	
		
		/** The external sap id help in endur. */
		EXT_SAP_ID("ext_sap_id", EnumColType.String),
		
		/** The settlement instruction id. */
		SETTLE_ID("settle_id", EnumColType.String), 
	
		/** The settlement instruction name. */
		SETTLE_NAME("settle_name", EnumColType.String), 
	
		/** The account number. */
		ACCOUNT_NUMBER("account_number", EnumColType.String), 
		
		/** The account name. */
		ACCOUNT_NAME("account_name", EnumColType.String),
		
		/** Use Auto SI short list. */
		USE_AUTO_SI_SHORTLIST("use_auto_si_shortlist", EnumColType.String),
		
		/** the accounts loco */
		LOCO("loco", EnumColType.String),
		
		/** the accounts form */
		FORM("form", EnumColType.String);

		/** The column name. */
		private String columnName;

		/** The column type. */
		private EnumColType columnType;

		/**
		 * Instantiates a new sap party data columns.
		 * 
		 * @param newColumnName
		 *            the new column name
		 * @param newColumnType
		 *            the new column type
		 */
		EnumPartyColumns(final String newColumnName,
				final EnumColType newColumnType) {
			columnName = newColumnName;
			columnType = newColumnType;
		}

		/**
		 * Gets the column name.
		 * 
		 * @return the column name
		 */
		public String getColumnName() {
			return columnName;
		}

		/**
		 * Gets the column type.
		 * 
		 * @return the column type
		 */
		public EnumColType getColumnType() {
			return columnType;
		}

		/* (non-Javadoc)
		 * @see com.olf.jm.SapInterface.businessObjects.enums.ITableColumn#isRequiredField()
		 */
		@Override
		public boolean isRequiredField() {
			return true;
		}
		
		@Override
		public String getColumnName(final String nameSpace) {
			throw new RuntimeException("Error column names already contain name space.");
		}
	}
	
	/** The internal party. */
	private IEndurParty internalParty;
	
	
	/** The external party. */
	private IEndurParty externalParty;

	
	/**
	 * Instantiates a new sap party data.
	 *
	 * @param rawPartyData the raw party data
	 */
	public CoverageSapPartyData(final Table rawPartyData) {
		
		if (rawPartyData == null || rawPartyData.getRowCount() != 1) { 
			throw new RuntimeException("Invalid argument table, expecting table with 1 row.");
		}
		
		String internalBU = rawPartyData.getString(EnumPartyColumns.INT_BU.getColumnName(), 0);
		
		String internalLE = rawPartyData.getString(EnumPartyColumns.INT_LE.getColumnName(), 0);
		
		String internalPortfolio = rawPartyData.getString(EnumPartyColumns.INT_PORTFOLIO.getColumnName(), 0);
		
		String externalLE = rawPartyData.getString(EnumPartyColumns.EXT_LE.getColumnName(), 0);
		
		String externalBU = rawPartyData.getString(EnumPartyColumns.EXT_BU.getColumnName(), 0);
		
		String inputInternalId = rawPartyData.getString(EnumPartyColumns.INPUT_INT_ID.getColumnName(), 0);
		
		String inputExternalId = rawPartyData.getString(EnumPartyColumns.INPUT_EXT_ID.getColumnName(), 0);	
		
		int settlementId = rawPartyData.getInt(EnumPartyColumns.SETTLE_ID.getColumnName(), 0);
		
		String autoSiFlag = rawPartyData.getString(EnumPartyColumns.USE_AUTO_SI_SHORTLIST.getColumnName(), 0);
		
		String loco = rawPartyData.getString(EnumPartyColumns.LOCO.getColumnName(), 0);
		
		String form = rawPartyData.getString(EnumPartyColumns.FORM.getColumnName(), 0);
		
		internalParty = new EndurParty(internalBU, internalLE, internalPortfolio, inputInternalId, EnumPartyType.INTERNAL, "", 0, "", "");
		
		externalParty = new EndurParty(externalBU, externalLE, null, inputExternalId, EnumPartyType.EXTERNAL, loco, settlementId, autoSiFlag, form);

	}


	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData#getInternalParty()
	 */
	@Override
	public final IEndurParty getInternalParty() {
		return internalParty;
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData#getExternalParty()
	 */
	@Override
	public final IEndurParty getExternalParty() {
		return externalParty;
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData#getToAccount()
	 */
	@Override
	public final IEndurAccount getToAccount() {
		throw new RuntimeException("To account is not supported for coverage requests"); 
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData#getFromAccount()
	 */
	@Override
	public final IEndurAccount getFromAccount() {
		throw new RuntimeException("From account is not supported for coverage requests"); 
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData#getInterfaceType()
	 */
	@Override
	public final EnumInterfaceType getInterfaceType() {
		return EnumInterfaceType.COVERAGE;
	}
	
	
}
