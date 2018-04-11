package com.olf.jm.metalstransfer.dealbooking;

import com.olf.embedded.application.Context;
import com.olf.embedded.tpm.AbstractProcessStep;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.Token;
import com.olf.openrisk.tpm.Variables;

/**
 * Book cash transaction deals for the charges on a validated metal transfer strategy.
 *  
 * @author Gary Moore
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 29-Oct-2015 |               | G. Moore        | Initial version.                                                                |
 * | 002 | 28-Apr-2016 |               | J. Waechter     | Now always booking USD charges and no longer the preferred unit of the BU       |
 * | 003 | 29-Apr-2016 |               | J. Waechter     | Now applying portfolio filter based on portfolios of submitter				   |
 * | 004 | 03-May-2016 |               | J. Waechter     | portfolio logic now takes read write flag into account                          |
 * | 005 | 17-Nov-2016 |               | J. Waechter     | transaction pointer gets reloaded in case the charge deals have to be cancelled |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
public class CashTransferChargesBooking extends AbstractProcessStep {
	@Override
   public Table execute(Context context, Process process, Token token,
			Person submitter, boolean transferItemLocks, Variables variables) {
        try {
            Logging.init(context, this.getClass(), "MetalsTransfer", "ChargesBooking");
           CashTrasferChargesBookingProcessor cashTrasferChargesBookingProcessor = new CashTrasferChargesBookingProcessor();
           cashTrasferChargesBookingProcessor.process(context, variables);
            return null;
        }
        catch (RuntimeException e) {
            Logging.error("Process failed:", e);
            throw e;
        }
        finally {
            Logging.close();
        }
    }
}
