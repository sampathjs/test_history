package com.matthey.pmm.apdp.scripts;

import ch.qos.logback.classic.Logger;
import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.EnhancedFieldListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Transaction;

@ScriptCategory(EnumScriptCategory.OpsSvcTranfield)
public class TradePriceUpdater extends EnhancedFieldListener {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(TradePriceUpdater.class);
    
    @Override
    protected PreProcessResult check(Context context, Field field, String oldValue, String newValue, Table clientData) {
        return PreProcessResult.succeeded();
    }
    
    @Override
    protected void process(Session session, Field field, String oldValue, String newValue, Table clientData) {
        Transaction transaction = field.getTransaction();
        double tradePrice = transaction.getField("AP DP Trade Price").getValueAsDouble();
        double premium = transaction.getField("Metal Price Spread").getValueAsDouble();
        double tradePriceWithPremium = tradePrice + premium;
        transaction.getField("Trade Price").setValue(tradePriceWithPremium);
        logger.info("AP DP Trade Price: {}; Premium: {}%; Trade Price: {}", tradePrice, premium, tradePriceWithPremium);
    }
}
