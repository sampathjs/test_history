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

import java.nio.file.Path;
import java.nio.file.Paths;

@ScriptCategory({EnumScriptCategory.TradeInput})
public abstract class EnhancedTradeProcessListener extends AbstractTradeProcessListener {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(EnhancedTradeProcessListener.class);
    
    @Override
    public PreProcessResult preProcess(Context context,
                                       EnumTranStatus targetStatus,
                                       PreProcessingInfo<EnumTranStatus>[] infoArray,
                                       Table clientData) {
        try {
            EndurLoggerFactory.configureLogLocation(getRootDir(), this.getClass());
            String className = this.getClass().getName();
            logger.info("pre-processing of script {} has started", className);
            PreProcessResult result = check(context, targetStatus, infoArray, clientData);
            logger.info("pre-processing of script {} has ended", className);
            return result;
        } catch (Throwable e) {
            logger.error("error occurred: {}", e.getMessage(), e);
            return PreProcessResult.failed(e.getMessage());
        }
    }
    
    @Override
    public void postProcess(Session session, DealInfo<EnumTranStatus> dealInfo, boolean succeed, Table table) {
        try {
            EndurLoggerFactory.configureLogLocation(getRootDir(), this.getClass());
            String className = this.getClass().getName();
            logger.info("post-processing of script {} has started", className);
            process(session, dealInfo, succeed, table);
            logger.info("post-processing of script {} has ended", className);
        } catch (Throwable e) {
            logger.error("error occurred: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    protected abstract void process(Session session, DealInfo<EnumTranStatus> dealInfo, boolean succeed, Table table);
    
    protected abstract PreProcessResult check(Context context,
                                              EnumTranStatus targetStatus,
                                              PreProcessingInfo<EnumTranStatus>[] infoArray,
                                              Table clientData);
    
    private Path getRootDir() {
        String root = System.getenv("AB_OUTDIR");
        return Paths.get(root, "logs");
    }
    
}