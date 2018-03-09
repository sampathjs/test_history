package com.olf.jm.metalstransfer.field.setter;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.embedded.application.Context;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Transaction;

/**
 * Utility class for setting the deal reference on a metal transfer.
 * <ol>
 * <li>Set deal reference to [UserId]_DDMMYYYY_HHMMSS.</li>
 * </ol>
 *  
 * @author Shaun Curran
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 07-Sep-2016 |               | S. Curran       | Initial version                                                                 |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */
public class DealRefFieldSetter {
	public static void setField(Context context, Transaction tran) {
        Date date = new Date();
        String strategyName = "" + context.getUser().getId() + '_' + new SimpleDateFormat("ddMMyyyy_HHmmss").format(date);
        tran.setValue(EnumTransactionFieldId.ReferenceString, strategyName);		
	}
}
