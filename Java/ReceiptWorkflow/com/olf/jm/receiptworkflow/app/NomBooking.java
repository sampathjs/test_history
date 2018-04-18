package com.olf.jm.receiptworkflow.app;

import com.olf.embedded.scheduling.AbstractNominationProcessListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Transactions;

@ScriptCategory({ EnumScriptCategory.OpsSvcNomBooking })
public class NomBooking extends AbstractNominationProcessListener {
    /**
     * {@inheritDoc}
     */
    public PreProcessResult preProcess(final Context context, final Nominations nominations,
                                       final Nominations originalNominations, final Transactions transactions,
                                       final Table clientData) {

        return preProcess(context, nominations, transactions, clientData);
    }
}
