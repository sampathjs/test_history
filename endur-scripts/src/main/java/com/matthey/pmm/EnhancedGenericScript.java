package com.matthey.pmm;

import ch.qos.logback.classic.Logger;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.olf.embedded.application.EnumScriptCategory.Generic;

@ScriptCategory(Generic)
public abstract class EnhancedGenericScript extends AbstractGenericScript {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(EnhancedGenericScript.class);
    
    @Override
    public Table execute(Context context, ConstTable constTable) {
        try {
            EndurLoggerFactory.configureLogLocation(getRootDir(), this.getClass());
            String className = this.getClass().getName();
            logger.info("script {} has started", className);
            run(context, constTable);
            logger.info("script {} has ended", className);
            return null;
        } catch (Throwable e) {
            logger.error("error occurred: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    private Path getRootDir() {
        String root = System.getenv("AB_OUTDIR");
        return Paths.get(root, "logs");
    }
    
    protected abstract void run(Context context, ConstTable constTable);
}
