package com.jm.sc.bo.opsvc;

import com.olf.jm.logging.Logging;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.OpService;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;

/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 19-Nov-2020 |               | Giriraj Joshi   | Initial version.                                                                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_OPS_SERVICE)
public class ManualVATCheck implements IScript {

	private String cashflowTypeToCheck = "Manual VAT";
	private String tranInfo = "Customer Invoice Number";
	
	@Override
	public void execute(IContainerContext context) throws OException {
		
		try {
			initLogging();
			
			for (int i = 1; i <= OpService.retrieveNumTrans(); i++)
			{
				Transaction tran = OpService.retrieveTran(i);
				
				if (tran == null) 
				{
					throw new OException("Unable to obtain transaction details");
				}
				
				String cashflowType = tran.getField(TRANF_FIELD.TRANF_CFLOW_TYPE.toInt());
				
				Logging.info("Cashflow type on deal: " + cashflowType);
				
				if (cashflowType.equalsIgnoreCase(cashflowTypeToCheck))
				{
					String custInvoiceNum = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, tranInfo);
					
					if (custInvoiceNum == null || custInvoiceNum.isEmpty()) 
					{
						throw new OException ("Customer Invoice Number must be populated when cashflow type is: " + cashflowType);				
					}			
				}
			}
		} catch (OException oe) {
			Logging.error(oe.getMessage());
			OpService.serviceFail(oe.getMessage(), 0);
			throw oe;
			
		} finally {
			Logging.close();
		}
	}
	
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception on initialisation errors or the logger or const repo.
	 */
	private void initLogging() throws OException {
		try {	
			Logging.init(this.getClass(), "", "");
		} catch (Exception e) {
			throw new OException("Error initialising logging: " + e.getMessage());
		}
	}

}
