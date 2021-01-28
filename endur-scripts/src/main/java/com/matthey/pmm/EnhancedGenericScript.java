package com.matthey.pmm;

import ch.qos.logback.classic.Logger;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;

import static com.olf.embedded.application.EnumScriptCategory.Generic;

@ScriptCategory(Generic)
public abstract class EnhancedGenericScript extends AbstractGenericScript {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(EnhancedGenericScript.class);
    
    @Override
    public Table execute(Context context, ConstTable constTable) {
        return EndurLoggerFactory.runScriptWithLogging("generic script", this.getClass(), logger, () -> {
            run(context, constTable);
            return null;
        });
    }
    
    protected abstract void run(Context context, ConstTable constTable);
}
