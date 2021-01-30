package com.matthey.pmm;

import ch.qos.logback.classic.Logger;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractFieldListener;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Field;

@ScriptCategory(EnumScriptCategory.OpsSvcTranfield)
public abstract class EnhancedFieldListener extends AbstractFieldListener {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(EnhancedFieldListener.class);
    
    @Override
    public PreProcessResult preProcess(Context context,
                                       Field field,
                                       String oldValue,
                                       String newValue,
                                       Table clientData) {
        return EndurLoggerFactory.runScriptWithLogging("field pre-processing",
                                                       this.getClass(),
                                                       logger,
                                                       () -> check(context, field, oldValue, newValue, clientData));
    }
    
    protected abstract PreProcessResult check(Context context,
                                              Field field,
                                              String oldValue,
                                              String newValue,
                                              Table clientData);
    
    @Override
    public void postProcess(Session session, Field field, String oldValue, String newValue, Table clientData) {
        EndurLoggerFactory.runScriptWithLogging("field post-processing",
                                                this.getClass(),
                                                logger,
                                                () -> process(session, field, oldValue, newValue, clientData));
    }
    
    protected abstract void process(Session session, Field field, String oldValue, String newValue, Table clientData);
}
