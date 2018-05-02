package com.jm.opservice.blockcoveragevalidation;

import com.jm.exception.SapExtensionsRuntimeException;
import com.jm.utils.Constants;
import com.jm.utils.Util;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.OpService;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;

/**
 * Prevents a coverage trade moving to Validated if there are discrepencies in the coverage attributes:
 * 1. If the trade is a sap coverage trade but the sap order id is missing, block it from further processing
 * 2. If the trade is not a coverage trade but has a sap order id set, block it from further processing
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_OPS_SERVICE)
public class BlockCoverageValidation implements IScript
{
	public void execute(IContainerContext context) throws OException
	{
		Util.initialiseLog(Constants.LOG_SAP_EXTENSIONS_OPS_SERVICES);
		
		for (int i = 1; i <= OpService.retrieveNumTrans(); i++)
		{
			int tranNum = -1;
			
			try
			{
				Transaction tran = OpService.retrieveTran(i);
				tranNum = tran.getTranNum();
				
				String isCoverage = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.jvsValue(), 0, Constants.TRANINFO_IS_COVERAGE);
				String sapOrderId = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.jvsValue(), 0, Constants.TRANINFO_SAP_ORDER_ID);

				boolean sapOrderIdMissing = (sapOrderId == null) || (sapOrderId.equalsIgnoreCase("") || sapOrderId.equalsIgnoreCase(" "));

				if ("yes".equalsIgnoreCase(isCoverage) && sapOrderIdMissing)
				{
					OpService.serviceFail("Unable to process Coverage transaction as the SAP_Order_ID is missing!", 0);
				}
				else if ("no".equalsIgnoreCase(isCoverage) && !sapOrderIdMissing)
				{
					OpService.serviceFail("Unable to process Coverage transaction as SAP_Order_ID is present, but isCoverage = No!", 0);
				}

				tran = null;
	
			}
			catch(Exception e)
			{
				throw new SapExtensionsRuntimeException("Error occured during BlockCoverageValidation for tran_num: " + tranNum, e);
			}
		}
	}
}