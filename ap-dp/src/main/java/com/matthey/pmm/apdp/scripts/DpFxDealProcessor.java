package com.matthey.pmm.apdp.scripts;

import ch.qos.logback.classic.Logger;
import com.google.common.collect.ImmutableMap;
import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.EnhancedGenericScript;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import org.apache.commons.text.StringSubstitutor;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.matthey.pmm.apdp.Metals.METAL_NAMES;

@ScriptCategory({EnumScriptCategory.Generic})
public class DpFxDealProcessor extends EnhancedGenericScript {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(DpFxDealProcessor.class);
    
    @Override
    protected void run(Context context, ConstTable constTable) {
        for (int tranNum : getDealsForProcessing(context)) {
            logger.info("processing tran {}", tranNum);
            
            try (Transaction tran = context.getTradingFactory().retrieveTransactionById(tranNum)) {
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
                try (Transaction accountMovementDeal = getAccountMovementDeal(context, dealNum)) {
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
                    
                    try (Transaction updatedDeal = context.getTradingFactory()
                            .retrieveTransactionById(accountMovementDeal.getTransactionId())) {
                        updatedDeal.getField("Auto SI Check").setValue("Yes");
                        int settleInstructionId = getSettlementInstruction(context, extBU);
                        updatedDeal.getLeg(2).getField("SI-Phys Internal").setValue(settleInstructionId);
                        updatedDeal.process(EnumTranStatus.Validated);
                        logger.info("booked account movement deal with tran num {}", updatedDeal.getTransactionId());
                    }
                }
            }
        }
    }
    
    private Set<Integer> getDealsForProcessing(Session session) {
        //language=TSQL
        String sqlTemplate = "SELECT t.tran_num\n" +
                             "    FROM ab_tran t\n" +
                             "             JOIN ab_tran_info_view i\n" +
                             "                  ON t.tran_num = i.tran_num AND i.type_name = 'Pricing Type'\n" +
                             "    WHERE t.toolset = ${fxToolset}\n" +
                             "      AND t.buy_sell = ${sell}\n" +
                             "      AND i.value = 'DP'\n" +
                             "      AND t.current_flag = 1\n" +
                             "      AND t.last_update > '${startDate}'\n" +
                             "      AND NOT EXISTS(SELECT *\n" +
                             "                         FROM ab_tran am\n" +
                             "                         WHERE reference = 'Account Movement ' + CAST(t.deal_tracking_num AS VARCHAR)\n" +
                             "                           AND am.last_update > t.last_update)";
        StaticDataFactory staticDataFactory = session.getStaticDataFactory();
        int fxToolset = staticDataFactory.getId(EnumReferenceTable.Toolsets, "FX");
        int sell = staticDataFactory.getId(EnumReferenceTable.BuySell, "Sell");
        LocalDate startDate = getStartDate();
        
        Map<String, Object> variables = ImmutableMap.of("fxToolset", fxToolset, "sell", sell, "startDate", startDate);
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        logger.info("sql for getting deals for processing:{}{}", System.lineSeparator(), sql);
        try (Table result = session.getIOFactory().runSQL(sql)) {
            return result.getRows().stream().map(row -> row.getInt(0)).collect(Collectors.toSet());
        }
    }
    
    public static LocalDate getStartDate() {
        try {
            return LocalDate.parse(new ConstRepository("DP", "").getStringValue("Start Date"));
        } catch (Exception e) {
            throw new RuntimeException("cannot retrieve trade date from configuration", e);
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
