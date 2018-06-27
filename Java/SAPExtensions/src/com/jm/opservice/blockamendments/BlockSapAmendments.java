package com.jm.opservice.blockamendments;

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
 * Blocks SAP amendments based on the following rules:
 * 1. If the trade is a sap coverage trade with a sap order id set, block it from further processing
 * 2. If the trade has a metal transfer request set, block it from further processing
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_OPS_SERVICE)
public class BlockSapAmendments implements IScript
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
				
				String coverage = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.jvsValue(), 0, Constants.TRANINFO_IS_COVERAGE);
				String sapOrderId = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.jvsValue(), 0, Constants.TRANINFO_SAP_ORDER_ID);
				String metalTransferRequestNumber = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.jvsValue(), 0, Constants.TRANINFO_METAL_TRANSFER_REQUEST_NUMBER);

				boolean isCoverage = ("yes".equalsIgnoreCase(coverage));
				boolean sapOrderIdPopulated = (sapOrderId != null) && (sapOrderId.length() > 2);
				boolean metalTransferRequestNumberPopulated = (metalTransferRequestNumber != null) && (metalTransferRequestNumber.length() > 2);

				if (isCoverage && sapOrderIdPopulated)
				{
					OpService.serviceFail("This coverage transaction cannot be re-processed as both isCoverage and Sap Order Id are already set.\n"
							+ "Please book a reversal and re-book correctly", 0);	
				}
				else if (metalTransferRequestNumberPopulated)
				{
					OpService.serviceFail("This transfer transaction cannot be re-processed as the metal transfer request number is already set.\n"
							+ "Please book a reversal and re-book correctly", 0);
				}

				tran = null;		
			}
			catch(Exception e)
			{
				throw new SapExtensionsRuntimeException("Error occured during BlockSapAmendments for tran_num: " + tranNum, e);
			}
		}
	}
}