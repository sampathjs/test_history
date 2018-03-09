package com.olf.jm.coverage.messageValidator;

import java.util.ArrayList;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;
import com.olf.jm.SapInterface.businessObjects.enums.ITableColumn;
import com.olf.jm.SapInterface.messageValidator.ValidatorBase;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.IFieldValidator;
import com.olf.jm.coverage.businessObjects.enums.EnumSapCoverageRequest;
import com.olf.jm.coverage.messageValidator.fieldValidator.ContractDate;
import com.olf.jm.coverage.messageValidator.fieldValidator.CoverageBusinessUnitCodeValidator;
import com.olf.jm.coverage.messageValidator.fieldValidator.CoverageCurrencyValidator;
import com.olf.jm.coverage.messageValidator.fieldValidator.CoverageDealInstructionValidator;
import com.olf.jm.coverage.messageValidator.fieldValidator.CoverageMetalElementValidator;
import com.olf.jm.coverage.messageValidator.fieldValidator.InstrumentIdValidator;
import com.olf.jm.coverage.messageValidator.fieldValidator.PriceValidator;
import com.olf.jm.coverage.messageValidator.fieldValidator.QuotationRefValidator;
import com.olf.jm.coverage.messageValidator.fieldValidator.TimeCodeValidator;
import com.olf.jm.coverage.messageValidator.fieldValidator.TradingDeskIdValidator;
import com.olf.jm.coverage.messageValidator.fieldValidator.UOMValidator;
import com.olf.jm.coverage.messageValidator.fieldValidator.ValueDate;
import com.olf.jm.coverage.messageValidator.fieldValidator.WeightValidator;


/**
 * The Class CoverageValidator. Validate a coverage request message
 */
public class CoverageValidator extends ValidatorBase  {

	/**
	 * Instantiates a new coverage validator.
	 *
	 * @param context the context
	 */
	public CoverageValidator(final Context context) {
		this.context = context;
	
		validators = new ArrayList<IFieldValidator>();
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.ValidatorBase#
	 * initValidators(com.olf.jm.coverage.businessObjects.dataFactories.ISapPartyData)
	 */
	@Override
	protected final void initValidators(final ISapPartyData currentSapPartyData, final ISapTemplateData currentTemplateData) {
		IFieldValidator coverageDealInstructionValidator = new CoverageDealInstructionValidator();
		validators.add(coverageDealInstructionValidator);

		IFieldValidator quoationRefValidator = new QuotationRefValidator(context);
		validators.add(quoationRefValidator);
		
		IFieldValidator instIdValidator = new InstrumentIdValidator(context, currentTemplateData);
		validators.add(instIdValidator);

		IFieldValidator timeCodeValidator = new TimeCodeValidator(context, currentTemplateData);
		validators.add(timeCodeValidator);		
		
		IFieldValidator weightValidator = new WeightValidator();		
		validators.add(weightValidator);
		
		IFieldValidator uomValidator = new UOMValidator(context);
		validators.add(uomValidator);
		
		IFieldValidator priceValidator = new PriceValidator();
		validators.add(priceValidator);
		
		IFieldValidator currencyValidator = new CoverageCurrencyValidator(context);
		validators.add(currencyValidator);	
		
		IFieldValidator contractDataValidator = new ContractDate();
		validators.add(contractDataValidator);

		IFieldValidator valueDataValidator = new ValueDate();
		validators.add(valueDataValidator);
		
		IFieldValidator metalElementValidator = new CoverageMetalElementValidator(context);
		validators.add(metalElementValidator);
		

		
		IFieldValidator buValidator = new CoverageBusinessUnitCodeValidator(currentSapPartyData);
		validators.add(buValidator);
		

		
		
		IFieldValidator tradingDeskIdValidator = new TradingDeskIdValidator(currentSapPartyData);
		validators.add(tradingDeskIdValidator);		
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.ValidatorBase#getColumns()
	 */
	@Override
	protected final ITableColumn[] getColumns() {
		return EnumSapCoverageRequest.values();
	}
}
