package com.olf.jm.coverage.businessObjects.dataFactories;

import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;
import com.olf.openrisk.table.Table;


/**
 * The Class SapTemplateData. holds data used to select the template to use when 
 * booking the trade in Endur.
 */
public class CoverageSapTemplateData implements ISapTemplateData {

	/** The template. */
	private String template;
	
	/** The portfolio post fix used to select the correct portfolio. */
	private String portfolioPostFix;
	
	/** The instrument type. */
	private String insType;
	
	/** The sap instrument type. */
	private String sapInsType;
	
	/** The sap company code. */
	private String sapCompanyCode;
	
	/** The sap metal. */
	private String sapMetal;
	
	/** The sap currency. */
	private String sapCurrency;
	
	/** The ticker. */
	private String ticker;
	
	/** The projection index. */
	private String projectionIndex;
	
	/** The ref source used on FX deals. */
	private String refSource;
	
	/** The cflow type used on FX deals. */
	private String cflowType;
	
	/**
	 * Instantiates a new sap template data.
	 *
	 * @param rawData the raw data
	 */
	public CoverageSapTemplateData(final Table rawData) {
		if (rawData == null || rawData.getRowCount() != 1) { 
			throw new RuntimeException("Invalid argument table, expecting table with 1 row.");
		}	
		
		template = rawData.getString("template", 0);
		portfolioPostFix = rawData.getString("portfolio_postfix", 0);
		insType = rawData.getString("ins_type", 0);
		sapInsType = rawData.getString("sap_inst_id", 0);
		sapCompanyCode = rawData.getString("sap_cmpy_code", 0);		
		sapMetal = rawData.getString("sap_metal", 0);			
		sapCurrency = rawData.getString("sap_currency", 0);		
		ticker = rawData.getString("ticker", 0);		
		projectionIndex = rawData.getString("proj_index", 0);	
		refSource =  rawData.getString("ref_source", 0);
		cflowType =  rawData.getString("cflow_type", 0);

	}
	
	/**
	 * Instantiates a new sap template data.
	 *
	 * @param inputSapInsId the sap instrument type
	 * @param inputSapMetal the sap metal code
	 * @param inputSapExtId the sap ext id
	 * @param inputSapCurrency the sap currency
	 */
	public CoverageSapTemplateData(final String inputSapInsId, final String inputSapMetal, final String inputSapExtId,
			final String inputSapCurrency) {
		this.sapInsType = inputSapInsId;  
		this.sapMetal = inputSapMetal; 
		this.sapCompanyCode = inputSapExtId;
		this.sapCurrency = inputSapCurrency;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.dataFactories.ISapTemplateData#getTemplate()
	 */
	@Override
	public final String getTemplate() {
		return template;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.dataFactories.ISapTemplateData#getPortfolioPostFix()
	 */
	@Override
	public final String getPortfolioPostFix() {
		return portfolioPostFix;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.dataFactories.ISapTemplateData#getInsType()
	 */
	@Override
	public final String getInsType() {
		return insType;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.dataFactories.ISapTemplateData#getSapInstrumentId()
	 */
	@Override
	public final String getSapInstrumentId() {
		return sapInsType;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.dataFactories.ISapTemplateData#getSapCompanyCode()
	 */
	@Override
	public final String getSapCompanyCode() {
		return sapCompanyCode;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.dataFactories.ISapTemplateData#getSapInsType()
	 */
	@Override
	public final String getSapInsType() {
		return sapInsType;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.dataFactories.ISapTemplateData#getSapMetal()
	 */
	@Override
	public final String getSapMetal() {
		return sapMetal;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.dataFactories.ISapTemplateData#getSapCurrency()
	 */
	@Override
	public final String getSapCurrency() {
		return sapCurrency;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.dataFactories.ISapTemplateData#getTicker()
	 */
	@Override
	public final String getTicker() {
		return ticker;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.businessObjects.dataFactories.ISapTemplateData#getProjectionIndex()
	 */
	@Override
	public final String getProjectionIndex() {
		return projectionIndex;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData#getRefSource()
	 */
	@Override
	public final String getRefSource() {		
		return refSource;
	}
	
	@Override
	public String getCflowType() {
		return cflowType;
	}

}
