package com.olf.jm.SapInterface.businessObjects.dataFactories;


/**
 * The Class EndurAccount.
 */
public class EndurAccount implements IEndurAccount {
	
	/** The business unit. */
	private String businessUnit;
	
	/** The account name. */
	private String accountName;
	
	/** The account number. */
	private String accountNumber;	
	
	/** The company code. */
	private String companyCode;
	
	/** The segment. */
	private String segment;
	
	/** The accounts form. */
	private String form;
	
	/** The accounts loco. */
	private String loco;
	
	/**
	 * Instantiates a new endur account.
	 *
	 * @param endurBusinessUnit the endur business unit
	 * @param endurAccountName the endur account name
	 * @param sapCompanyCode the sap company code
	 * @param sapSegment the sap segment
	 * @param endurAccountNumber the endur account number
	 * @param accountLoco the account loco
	 * @param accountForm the account form
	 */
	public EndurAccount(final String endurBusinessUnit, final String endurAccountName,
			final String sapCompanyCode, final String sapSegment, final String endurAccountNumber, 
			final String accountLoco, final String accountForm) {
		this.businessUnit = endurBusinessUnit;
		this.accountName = endurAccountName;
		this.companyCode = sapCompanyCode;
		this.segment = sapSegment;
		this.accountNumber =	endurAccountNumber;
		loco = accountLoco;
		form = accountForm;
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.IEndurAccount#getAccountBusinessUnit()
	 */
	@Override
	public final String getAccountBusinessUnit() {
		return businessUnit;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.IEndurAccount#getAccountName()
	 */
	@Override
	public final String getAccountName() {
		return accountName;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.IEndurAccount#getAccountNumber()
	 */
	@Override
	public final String getAccountNumber() {
		return accountNumber;
	}	

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.IEndurAccount#getCompanyCode()
	 */
	@Override
	public final String getCompanyCode() {
		return companyCode;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.IEndurAccount#getSegment()
	 */
	@Override
	public final String getSegment() {
		return segment;
	}


	@Override
	public String getAccountForm() {
		return form;
	}


	@Override
	public String getAccountLoco() {
		return loco;
	}

}
