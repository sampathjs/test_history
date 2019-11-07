package com.olf.jm.sapTransfer.messageValidator;

import java.util.ArrayList;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;
import com.olf.jm.SapInterface.businessObjects.enums.ITableColumn;
import com.olf.jm.SapInterface.messageValidator.ValidatorBase;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.IFieldValidator;
import com.olf.jm.SapInterface.messageValidator.fieldValidator.ITwoFieldValidator;
import com.olf.jm.sapTransfer.businessObjects.enums.EnumSapTransferRequest;
import com.olf.jm.sapTransfer.messageValidator.fieldValidator.ApprovalDateValidator;
import com.olf.jm.sapTransfer.messageValidator.fieldValidator.BackDatedTransferValidator;
import com.olf.jm.sapTransfer.messageValidator.fieldValidator.FromAccountNumberValidator;
import com.olf.jm.sapTransfer.messageValidator.fieldValidator.FromSegmentValidator;
import com.olf.jm.sapTransfer.messageValidator.fieldValidator.ToAccountNumberValidator;
import com.olf.jm.sapTransfer.messageValidator.fieldValidator.ToSegmentValidator;
import com.olf.jm.sapTransfer.messageValidator.fieldValidator.TradingDeskIdValidator;
import com.olf.jm.sapTransfer.messageValidator.fieldValidator.TransferDestinationTypeValidator;
import com.olf.jm.sapTransfer.messageValidator.fieldValidator.TransferMetalElementValidator;
import com.olf.jm.sapTransfer.messageValidator.fieldValidator.UOMValidator;
import com.olf.jm.sapTransfer.messageValidator.fieldValidator.ValueDateValidator;
import com.olf.jm.sapTransfer.messageValidator.fieldValidator.MetalTransferRequestNumberValidator;

/**
 * The Class TransferValidator. Validate a transfer request message
 */
public class TransferValidator extends ValidatorBase  {

	/**
	 * Instantiates a new transfer validator.
	 *
	 * @param context the context
	 */
	public TransferValidator(final Context context) {
		this.context = context;
	
		validators = new ArrayList<IFieldValidator>();
		twoFieldValidators = new ArrayList<ITwoFieldValidator>();
	}

	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.ValidatorBase#
	 * initValidators(com.olf.jm.coverage.businessObjects.dataFactories.ISapPartyData)
	 */
	@Override
	protected final void initValidators(final ISapPartyData currentSapPartyData, final ISapTemplateData currentTemplateData) {

		validators.add(new MetalTransferRequestNumberValidator(context));	

		validators.add(new TransferMetalElementValidator(context));	
		
		validators.add(new TradingDeskIdValidator(currentSapPartyData));
		
		validators.add(new UOMValidator(context));
		
		validators.add(new TransferDestinationTypeValidator(context));
		
		validators.add(new ToSegmentValidator(currentSapPartyData));
		
		validators.add(new ToAccountNumberValidator(currentSapPartyData));
		
		validators.add(new FromSegmentValidator(currentSapPartyData));
		
		validators.add(new FromAccountNumberValidator(currentSapPartyData));
		
		validators.add(new ApprovalDateValidator());
		
		validators.add(new ValueDateValidator());
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.SapInterface.messageValidator.ValidatorBase#
	 * initTwoFieldValidators()
	 */
	@Override
	protected final void initTwoFieldValidators() {

		twoFieldValidators.add( new BackDatedTransferValidator (context));
	}


	@Override
	protected final ITableColumn[] getColumns() {
		return EnumSapTransferRequest.values();
	}
}
