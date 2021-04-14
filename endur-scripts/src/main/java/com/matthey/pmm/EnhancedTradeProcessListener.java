package com.matthey.pmm;

import ch.qos.logback.classic.Logger;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;

@ScriptCategory({EnumScriptCategory.TradeInput})
public abstract class EnhancedTradeProcessListener extends AbstractTradeProcessListener {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(EnhancedTradeProcessListener.class);
    
    @Override
    public PreProcessResult preProcess(Context context,
                                       EnumTranStatus targetStatus,
                                       PreProcessingInfo<EnumTranStatus>[] infoArray,
                                       Table clientData) {
        return EndurLoggerFactory.runScriptWithLogging("trade pre-processing",
                                                       this.getClass(),
                                                       logger,
                                                       () -> check(context, targetStatus, infoArray, clientData));
    }
    
    @Override
    public void postProcess(Session session, DealInfo<EnumTranStatus> dealInfo, boolean succeed, Table clientData) {
        EndurLoggerFactory.runScriptWithLogging("trade post-processing",
                                                this.getClass(),
                                                logger,
                                                () -> process(session, dealInfo, succeed, clientData));
    }
    
    protected abstract void process(Session session,
                                    DealInfo<EnumTranStatus> dealInfo,
                                    boolean succeed,
                                    Table clientData);
    
    protected abstract PreProcessResult check(Context context,
                                              EnumTranStatus targetStatus,
                                              PreProcessingInfo<EnumTranStatus>[] infoArray,
                                              Table clientData);
}