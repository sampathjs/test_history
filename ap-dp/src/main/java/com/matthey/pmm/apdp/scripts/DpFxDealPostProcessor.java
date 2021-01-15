package com.matthey.pmm.apdp.scripts;

import ch.qos.logback.classic.Logger;
import com.google.common.collect.ImmutableMap;
import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.EnhancedTradeProcessListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import org.apache.commons.text.StringSubstitutor;

import java.util.Map;

import static com.matthey.pmm.apdp.Metals.METAL_NAMES;

@ScriptCategory({EnumScriptCategory.OpsSvcTrade})
public class DpFxDealPostProcessor extends EnhancedTradeProcessListener {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(DpFxDealPostProcessor.class);
    
    @Override
    protected void process(Session session, DealInfo<EnumTranStatus> dealInfo, boolean succeed, Table clientData) {
        for (int tranNum : dealInfo.getTransactionIds()) {
            logger.info("processing tran {}", tranNum);
            
            try (Transaction tran = session.getTradingFactory().retrieveTransactionById(tranNum)) {
                int dealNum = tran.getDealTrackingId();
                if (!"DP".equals(tran.getField("Pricing Type").getDisplayString())) {
                    logger.info("it is not a DP deal, no action needed");
                    continue;
                }
                
                String extBU = tran.getField(EnumTransactionFieldId.ExternalBusinessUnit).getDisplayString();
                String metal = tran.getField(EnumTransactionFieldId.FxBaseCurrency)
                        .getDisplayString()
                        .replaceAll(" \\[.+]", "");
                double volume = tran.getField(EnumTransactionFieldId.FxDealtAmount).getValueAsDouble();
                String form = tran.getField("Form").getValueAsString();
                try (Transaction accountMovementDeal = getAccountMovementDeal(session, dealNum)) {
                    if (tran.getTransactionStatus() == EnumTranStatus.Cancelled) {
                        if (accountMovementDeal.getDealTrackingId() != 0) {
                            accountMovementDeal.process(EnumTranStatus.Cancelled);
                        }
                        continue;
                    }
                    accountMovementDeal.setValue(EnumTransactionFieldId.ReferenceString, "Account Movement " + dealNum);
                    accountMovementDeal.setValue(EnumTransactionFieldId.ExternalBusinessUnit, extBU);
                    accountMovementDeal.setValue(EnumTransactionFieldId.BuySell, "Sell");
                    Leg physicalLeg = accountMovementDeal.getLeg(1);
                    physicalLeg.setValue(EnumLegFieldId.CommoditySubGroup, METAL_NAMES.inverse().get(metal));
                    physicalLeg.setValue(EnumLegFieldId.DailyVolume, volume);
                    physicalLeg.setValue(EnumLegFieldId.CommodityForm, form);
                    accountMovementDeal.process(EnumTranStatus.Validated);
                    
                    try (Transaction updatedDeal = session.getTradingFactory()
                            .retrieveTransactionById(accountMovementDeal.getTransactionId())) {
                        updatedDeal.getField("Auto SI Check").setValue("Yes");
                        int settleInstructionId = getSettlementInstruction(session, extBU);
                        updatedDeal.getLeg(2).getField("SI-Phys Internal").setValue(settleInstructionId);
                        updatedDeal.process(EnumTranStatus.Validated);
                        logger.info("booked account movement deal with tran num {}", updatedDeal.getTransactionId());
                    }
                }
            }
        }
    }
    
    private Transaction getAccountMovementDeal(Session session, int dealNum) {
        //language=TSQL
        String sqlTemplate = "SELECT tran_num\n" +
                             "    FROM ab_tran\n" +
                             "    WHERE current_flag = 1\n" +
                             "      AND tran_status = 3\n" +
                             "      AND reference = 'Account Movement ${dealNum}'";
        Map<String, Object> variables = ImmutableMap.of("dealNum", dealNum);
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        Table result = session.getIOFactory().runSQL(sql);
        TradingFactory tradingFactory = session.getTradingFactory();
        return result.getRowCount() > 0
               ? tradingFactory.retrieveTransactionById(result.getInt(0, 0))
               : tradingFactory.createTransactionFromTemplate(getTemplate(session));
    }
    
    private int getTemplate(Session session) {
        //language=TSQL
        String sql = "SELECT tran_num\n" +
                     "    FROM ab_tran\n" +
                     "    WHERE current_flag = 1\n" +
                     "      AND tran_status = 15\n" +
                     "      AND reference = 'HK Account Movement IN'";
        try (Table table = session.getIOFactory().runSQL(sql)) {
            return table.getInt(0, 0);
        }
    }
    
    @Override
    protected PreProcessResult check(Context context,
                                     EnumTranStatus targetStatus,
                                     PreProcessingInfo<EnumTranStatus>[] infoArray,
                                     Table clientData) {
        return null;
    }
    
    private int getSettlementInstruction(Session session, String extBU) {
        //language=TSQL
        String sqlTemplate = "SELECT si.settle_id, si.settle_name\n" +
                             "    FROM settle_instructions si\n" +
                             "             JOIN account a\n" +
                             "                  ON si.account_id = a.account_id\n" +
                             "             JOIN party p\n" +
                             "                  ON a.holder_id = p.party_id\n" +
                             "    WHERE si.settle_name LIKE 'PMM HK DEFERRED%'\n" +
                             "      AND p.short_name = '${extBU}'";
        Map<String, Object> variables = ImmutableMap.of("extBU", extBU.replace("'", "''"));
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        try (Table table = session.getIOFactory().runSQL(sql)) {
            logger.info("internal SI: {}", table.getString("settle_name", 0));
            return table.getInt("settle_id", 0);
        }
    }
}
