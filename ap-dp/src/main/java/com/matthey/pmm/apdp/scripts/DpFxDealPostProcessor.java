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
            try (Transaction tran = session.getTradingFactory().retrieveTransactionById(tranNum)) {
                if (!"DP".equals(tran.getField("Pricing Type").getDisplayString())) {
                    continue;
                }
                
                int dealNum = tran.getDealTrackingId();
                String extBU = tran.getField(EnumTransactionFieldId.ExternalBusinessUnit).getDisplayString();
                String metal = tran.getField(EnumTransactionFieldId.FxBaseCurrency)
                        .getDisplayString()
                        .replaceAll(" \\[.+]", "");
                double volume = tran.getField(EnumTransactionFieldId.FxDealtAmount).getValueAsDouble();
                String form = tran.getField("Form").getValueAsString();
                try (Transaction accountMovementDeal = getAccountMovementDeal(session, dealNum)) {
                    accountMovementDeal.setValue(EnumTransactionFieldId.ReferenceString, "Account Movement " + dealNum);
                    accountMovementDeal.setValue(EnumTransactionFieldId.ExternalBusinessUnit, extBU);
                    Leg leg = accountMovementDeal.getLeg(1);
                    leg.setValue(EnumLegFieldId.CommoditySubGroup, METAL_NAMES.inverse().get(metal));
                    leg.setValue(EnumLegFieldId.DailyVolume, volume);
                    leg.setValue(EnumLegFieldId.CommodityForm, form);
                    accountMovementDeal.process(EnumTranStatus.New);
                }
            }
        }
    }
    
    private Transaction getAccountMovementDeal(Session session, int dealNum) {
        //language=TSQL
        String sqlTemplate = "SELECT tran_num\n" +
                             "    FROM ab_tran\n" +
                             "    WHERE current_flag = 1\n" +
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
        String
                sql
                = "SELECT tran_num FROM ab_tran WHERE current_flag = 1 AND tran_status = 15 AND reference = 'HK Account Movement IN'";
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
}
