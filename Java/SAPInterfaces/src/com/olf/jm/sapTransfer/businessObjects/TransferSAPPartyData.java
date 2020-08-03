package com.olf.jm.sapTransfer.businessObjects;

import com.olf.jm.SapInterface.businessObjects.dataFactories.EndurAccount;
import com.olf.jm.SapInterface.businessObjects.dataFactories.EndurParty;
import com.olf.jm.SapInterface.businessObjects.dataFactories.EnumInterfaceType;
import com.olf.jm.SapInterface.businessObjects.dataFactories.IEndurAccount;
import com.olf.jm.SapInterface.businessObjects.dataFactories.IEndurParty;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.businessObjects.dataFactories.IEndurParty.EnumPartyType;
import com.olf.jm.SapInterface.businessObjects.enums.ITableColumn;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.jm.logging.Logging;

/**
 * The Class TransferSAPPartyData.
 */
public class TransferSAPPartyData implements ISapPartyData {
	/**
	 * The Enum EnumAccountColumns.
	 */
	private enum EnumAccountColumns implements ITableColumn {
		
		/** The input le. */
		INPUT_LE("input_le", EnumColType.String),	
		
		/** The input bu. */
		INPUT_BU("input_bu", EnumColType.String),	
		
		/** The bu. */
		BU("bu", EnumColType.String),
		
		/** The int ext. */
		INT_EXT("int_ext", EnumColType.String),	
		
		/** The account name. */
		ACCOUNT_NAME("account_name", EnumColType.String),
		
		/** The account number. */
		ACCOUNT_NUMBER("account_number", EnumColType.String),
		
		/** The loco. */
		LOCO("loco", EnumColType.String), 
		
		/** The form. */
		FORM("form", EnumColType.String),	 
		
		/** The bu sap id. */
		BU_SAP_ID("bu_sap_id", EnumColType.String),	 
		
		/** The le sap id. */
		LE_SAP_ID("le_sap_id", EnumColType.String);
	

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
	EnumAccountColumns(final String newColumnName,
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
	
	/**
	 * The Enum EnumPartyColumns.
	 */
	private enum EnumPartyColumns implements ITableColumn {
		

	/** The input int id. */
	INPUT_INT_ID("input_int_id", EnumColType.String),
	
	/** The int le. */
	INT_LE("int_le", EnumColType.String),	
	
	/** The int bu. */
	INT_BU("int_bu", EnumColType.String),	
	
	/** The int portfolio. */
	INT_PORTFOLIO("int_portfolio", EnumColType.String),	
	
	/** The int sap id. */
	INT_SAP_ID("int_sap_id", EnumColType.String),
	
	/** The party loco. */
	LOCO("loco", EnumColType.String);
	
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

	/** The toAccount details. */
	private IEndurAccount toAccount;

	/** The fromAccount details. */
	private IEndurAccount fromAccount;

	/**
	 * Instantiates a new transfer sap party data.
	 *
	 * @param intParty table containing the internal party details loaded from the db.
	 * @param fromAccountRawData table containing from account details loaded from the db.
	 * @param toAccountRawData table contianing to account details loaded from the db.
	 */
	public TransferSAPPartyData(final Table intParty, final Table fromAccountRawData, final Table toAccountRawData) {
		setInternalParty(intParty);
		
		toAccount = setAccount(toAccountRawData);
		
		fromAccount = setAccount(fromAccountRawData);
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
		throw new RuntimeException("External party data is not valid on a metal transfer.");
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData#getToAccount()
	 */
	@Override
	public final IEndurAccount getToAccount() {
		return toAccount;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData#getFromAccount()
	 */
	@Override
	public final IEndurAccount getFromAccount() {
		return fromAccount;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData#getInterfaceType()
	 */
	@Override
	public final EnumInterfaceType getInterfaceType() {
		return EnumInterfaceType.TRANSFER;
	}

	
	/**
	 * Sets the internal party.
	 *
	 * @param intParty the new internal party
	 */
	private void setInternalParty(final Table intParty) {
		
		if (intParty == null || intParty.getRowCount() != 1) { 
			throw new RuntimeException("Invalid argument table, expecting party table with 1 row.");
		}		
		
		String internalBU = intParty.getString(EnumPartyColumns.INT_BU.getColumnName(), 0);
		
		String internalLE = intParty.getString(EnumPartyColumns.INT_LE.getColumnName(), 0);
		
		String internalPortfolio = intParty.getString(EnumPartyColumns.INT_PORTFOLIO.getColumnName(), 0);
			
		String inputInternalId = intParty.getString(EnumPartyColumns.INPUT_INT_ID.getColumnName(), 0);
		
		String inputLoco = intParty.getString(EnumPartyColumns.LOCO.getColumnName(), 0);

		internalParty = new EndurParty(internalBU, internalLE,
				internalPortfolio, inputInternalId, EnumPartyType.INTERNAL,
				inputLoco, 0, "", "");		
	}
	
	/**
	 * create a account data object from the raw data loaded from the db.
	 *
	 * @param account raw account details
	 * @return populated endur account details object.
	 */
	private IEndurAccount setAccount(final Table account) {
		
		if (account == null || account.getRowCount() == 0) { 
			throw new RuntimeException("Invalid argument table, expecting account table with 1 or more rows.");
		}	
		
		// Check to see if the internal  / external data is populated
		ConstTable internalData = account.createConstView("*", "[int_ext] == 'internal' AND [bu] != ''");
		ConstTable externalData = account.createConstView("*", "[int_ext] == 'external' AND [bu] != ''");
		
		// Only one of the aboce tables should contain data
		if (internalData.getRowCount() > 0 && externalData.getRowCount() > 0) {
			throw new RuntimeException("Invalid argument table, found matching accounts for internal and external parties.");
		}
		
		if (internalData.getRowCount() == 0 && externalData.getRowCount() == 0) {
			// No data found
			return populateAccountStructure(account);
		}
		
		if (internalData.getRowCount() > 0) {
			return populateAccountStructure(internalData);
		}
		
		return populateAccountStructure(externalData);

	}
	
	/**
	 * Populate account data structure based on the sql results.
	 *
	 * @param accountData the account data
	 * @return the  endur account details
	 */
	private IEndurAccount populateAccountStructure(final ConstTable accountData) {
		String endurBusinessUnit = accountData.getString(EnumAccountColumns.BU.getColumnName(), 0);
		String endurAccountName = accountData.getString(EnumAccountColumns.ACCOUNT_NAME.getColumnName(), 0);
		String endurAccountNumber = accountData.getString(EnumAccountColumns.ACCOUNT_NUMBER.getColumnName(), 0);
		String sapCompanyCode = accountData.getString(EnumAccountColumns.LE_SAP_ID.getColumnName(), 0);
		String sapSegment = accountData.getString(EnumAccountColumns.BU_SAP_ID.getColumnName(), 0);	
		
		String accountForm = accountData.getString(EnumAccountColumns.FORM.getColumnName(), 0);
		String accountLoco = accountData.getString(EnumAccountColumns.LOCO.getColumnName(), 0);
		
		if (accountData.getRowCount() > 1) {
			Logging.info("More than 1 row found for segment " + sapSegment
					+ " company code " + sapCompanyCode + " account "
					+ endurAccountNumber + " setting account name to blank.");
			endurAccountName = "";
		}
		
		return new EndurAccount(endurBusinessUnit, endurAccountName, sapCompanyCode, sapSegment, endurAccountNumber, 
				accountLoco, accountForm);		
	}
	

}

