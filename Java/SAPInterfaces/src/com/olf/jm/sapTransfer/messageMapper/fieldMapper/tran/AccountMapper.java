package com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase;
import com.olf.openrisk.trading.EnumTradingObject;


/**
 * The Class AccountMapper. Mapping logic to populate the
 * to / from account info field.
 */
public class AccountMapper extends FieldMapperBase {

	
	/** The party data. */
	private ISapPartyData partyData;
	
	/** flag to indicate if the to or from account needs to be set. */
	private EnumToFrom accountToSet;
	
	/**
	 * Instantiates a new account mapper.
	 *
	 * @param context the context the script is running in.
	 * @param currentPartyData the current party data loaded from the db
	 * @param toFromAccountToSet flag to indicate if the to or from account should be set.
	 */
	public AccountMapper(final Context context, final ISapPartyData currentPartyData, final EnumToFrom toFromAccountToSet) {
		super(context);
		
		this.partyData = currentPartyData;
		
		this.accountToSet = toFromAccountToSet;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getTradingObject()
	 */
	@Override
	protected final EnumTradingObject getTradingObject() {
		
		throw new RuntimeException("Info field.");
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#isInfoField()
	 */
	@Override
	protected final boolean isInfoField() {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getInfoFieldName()
	 */
	@Override
	protected final String getInfoFieldName() {
		String fieldName = "";
		if (accountToSet == EnumToFrom.TO) {
			fieldName = EnumStrategyInfoFields.TO_ACCT.getFieldName();
		} else {
			fieldName = EnumStrategyInfoFields.FROM_ACCT.getFieldName();
		}
		return fieldName;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageMapper.fieldMapper.FieldMapperBase#getValue()
	 */
	@Override
	public final String getValue() {
		
		String value = "";
		if (accountToSet == EnumToFrom.TO) {
			value = partyData.getToAccount().getAccountName();
		} else {
			value = partyData.getFromAccount().getAccountName();
		}
		return value;

	}
	
}
