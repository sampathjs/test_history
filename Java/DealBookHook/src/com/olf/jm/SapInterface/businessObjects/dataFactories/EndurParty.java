package com.olf.jm.SapInterface.businessObjects.dataFactories;


/**
 * The Class EndurParty. Represents Part information from Endur needed to book a trade.
 */
public class EndurParty implements IEndurParty {

	/** The bu short name. */
	private String bu;
	
	/** The le short name . */
	private String le;
	
	/** The portfolio name. */
	private String portfolio;
	
	/** The sap id used to look up the data. */
	private String sapId;
	
	/** The party type indicating if the party is internal or external. */
	private EnumPartyType partyType;

	/** the account loco. */
	private String loco;
	
	/** The settlement instruction id. */
	private int settlementId;
	
	/** Flag to set if the short list should be used. */
	private String useAutoSiShortList;
	
	/** the accounts form. */
	private String form;
	
	
	/**
	 * Instantiates a new endur party.
	 *
	 * @param endurBu the bu
	 * @param endurLe the le
	 * @param endurPortfolio the portfolio
	 * @param inputSapId the sap id
	 * @param endurPartyType the party type
	 * @param accountLoco the account loco
	 * @param endurSettlementId the endur settlement instruction id
	 * @param acctUseAutoSiShortList the use auto si short list
	 * @param accountForm the accounts form
	 */
	public EndurParty(final String endurBu, final String endurLe, final String endurPortfolio, final String inputSapId,
			final EnumPartyType endurPartyType, final String accountLoco, final int endurSettlementId, final String acctUseAutoSiShortList,
			final String accountForm) {

		this.bu = endurBu;
		this.le = endurLe;
		this.portfolio = endurPortfolio;
		this.sapId = inputSapId;
		this.partyType = endurPartyType;
		this.loco = accountLoco;
		settlementId = endurSettlementId;
		this.useAutoSiShortList = acctUseAutoSiShortList;
		this.form = accountForm;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.IEndurParty#getBusinessUnit()
	 */
	@Override
	public final String getBusinessUnit() {
		return bu;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.IEndurParty#getLegalEntity()
	 */
	@Override
	public final String getLegalEntity() {
		return le;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.IEndurParty#getProtfolio()
	 */
	@Override
	public final String getPortfolio() {
		return portfolio;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.IEndurParty#getInputSapId()
	 */
	@Override
	public final String getInputSapId() {
		return sapId;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.IEndurParty#getPartyType()
	 */
	@Override
	public final EnumPartyType getPartyType() {
		return partyType;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.IEndurParty#getSettlementId()
	 */
	@Override
	public final int getSettlementId() {
		return settlementId;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.IEndurParty#getUseAutoSIShortList()
	 */
	@Override
	public final String getUseAutoSIShortList() {		
		return useAutoSiShortList;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.IEndurParty#getAccountLoco()
	 */
	@Override
	public final String getAccountLoco() {
		return loco;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.IEndurParty#getAccountForm()
	 */
	@Override
	public final String getAccountForm() {
		return form;
	}

}
