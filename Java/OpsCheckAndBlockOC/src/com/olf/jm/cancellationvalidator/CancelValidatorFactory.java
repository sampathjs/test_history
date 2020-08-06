package com.olf.jm.cancellationvalidator;

import com.olf.embedded.application.Context;
import com.olf.openjvs.OException;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.Transaction;
import com.olf.jm.logging.Logging;

//import com.olf.openjvs.enums.TOOLSET_ENUM;

/**
 * Factory class to create an Object of ToolsetI based on the underlying
 * Transaction
 * 
 * @author
 * 
 */
public class CancelValidatorFactory {
	/**
	 * @param context
	 * @param tran
	 * @return
	 * @throws OException
	 */
	public AbstractValidator createToolset(Context context, Transaction tran) throws OException {
		EnumToolset toolsetEnum;
		AbstractValidator validator = null;

		try {

			toolsetEnum = tran.getToolset();
			Logging.info("toolsetString= " + toolsetEnum.getName() + "\n");


			switch (toolsetEnum) {
			
			case Fx:
			case ComFut:
				validator = new FXCancelValidator(tran, context);
				break;
				
			case ComSwap:
				validator = new CommSwapCancelValidator(tran, context);
				break;

			case Loandep:
				validator = new LoanDepCancelValidator(tran, context);
				break;

			case Composer:
			case Cash:
					validator = new TransferCancelValidator(tran, context);	
				break;
			default:
				String errMsg = toolsetEnum.toString() + " toolset is not supported.";
				Logging.error(errMsg);
				throw new OException(errMsg);
			}
		} catch (Exception exp) {
			Logging.error("Error in method createToolset " + exp.getMessage());
			throw new OException(exp.getMessage());
		}
		return validator;

	}

	


	
}
