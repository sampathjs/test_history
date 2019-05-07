package com.olf.jm.sapTransfer.businessObjects;

import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;


/**
 * The Class TransferSAPTemplateData. 
 */
public class TransferSAPTemplateData implements ISapTemplateData {

	
	/** The portfolio post fix. */
	private String portfolioPostFix;


	/**
	 * Instantiates a new transfer sap template data.
	 *
	 * @param currentPortfolioPostFix the current portfolio post fix
	 */
	public TransferSAPTemplateData(final String currentPortfolioPostFix) {
		super();
		this.portfolioPostFix = currentPortfolioPostFix;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData#getTemplate()
	 */
	@Override
	public final String getTemplate() {
		throw new RuntimeException("Template is not defined for metal transfers.");
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData#getPortfolioPostFix()
	 */
	@Override
	public final String getPortfolioPostFix() {
		return portfolioPostFix;
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData#getInsType()
	 */
	@Override
	public final String getInsType() {
		return "Strategy";
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData#getSapInstrumentId()
	 */
	@Override
	public final String getSapInstrumentId() {
		throw new RuntimeException("InstrumentType is not defined for metal transfers.");
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData#getSapCompanyCode()
	 */
	@Override
	public final String getSapCompanyCode() {
		throw new RuntimeException("CompantCode is not defined for metal transfers.");
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData#getProjectionIndex()
	 */
	@Override
	public final String getProjectionIndex() {
		throw new RuntimeException("ProjectionIndex is not defined for metal transfers.");
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData#getTicker()
	 */
	@Override
	public final String getTicker() {
		throw new RuntimeException("Ticker is not defined for metal transfers.");
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData#getSapCurrency()
	 */
	@Override
	public final String getSapCurrency() {
		throw new RuntimeException("SapCurrancy is not defined for metal transfers.");
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData#getSapMetal()
	 */
	@Override
	public final String getSapMetal() {
		throw new RuntimeException("SapMetal is not defined for metal transfers.");
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData#getSapInsType()
	 */
	@Override
	public final String getSapInsType() {
		throw new RuntimeException("SapInsType is not defined for metal transfers.");
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData#getRefSource()
	 */
	@Override
	public final String getRefSource() {
		throw new RuntimeException("SapInsType is not defined for metal transfers.");
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData#getCflowType()
	 */
	@Override
	public String getCflowType() {
		throw new RuntimeException("SapInsType is not defined for metal transfers.");
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData#getCflowType()
	 */
	@Override
	public void setCflowType(String value) {
		throw new RuntimeException("SapInsType is not defined for metal transfers.");
		
	}


}
