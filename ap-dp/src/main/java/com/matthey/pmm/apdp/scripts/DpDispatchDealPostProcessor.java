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
import com.olf.openrisk.market.EnumElementType;
import com.olf.openrisk.market.EnumGptField;
import com.olf.openrisk.market.ForwardCurve;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumInsType;
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
public class DpDispatchDealPostProcessor extends EnhancedTradeProcessListener {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(DpDispatchDealPostProcessor.class);
    
    @Override
    protected void process(Session session, DealInfo<EnumTranStatus> dealInfo, boolean succeed, Table clientData) {
        for (int tranNum : dealInfo.getTransactionIds()) {
            logger.info("processing tran {}", tranNum);
            
            try (Transaction tran = session.getTradingFactory().retrieveTransactionById(tranNum)) {
                if (!"DP".equals(tran.getField("Pricing Type").getDisplayString())) {
                    logger.info("it is not a DP deal, no action needed");
                    continue;
                }
                if (tran.getValueAsInt(EnumTransactionFieldId.OffsetTransactionType) > 1) {
                    logger.info("it is an offset deal, no action needed");
                    // both the original and offset deals will trigger the op service
                    // and for both invoke, the original and the offset deals are passed into the op service, but with reversed order
                    // therefore uses "break" here rather than "continue", to make sure the original deal is only processed once totally
                    break;
                }
                
                String extBU = tran.getField("Consignee").getDisplayString();
                logger.info("run AP DP report for {}", extBU);
                runApDpReport(session, extBU);
                
                int dealNum = tran.getDealTrackingId();
                Leg leg = tran.getLeg(1);
                double volume = leg.getField(EnumLegFieldId.DailyVolume).getValueAsDouble();
                logger.info("volume of the FX sell deal: {}", volume);
                String metal = leg.getField(EnumLegFieldId.CommoditySubGroup).getValueAsString();
                ForwardCurve curve = (ForwardCurve) session.getMarketFactory()
                        .getMarket()
                        .getElement(EnumElementType.ForwardCurve, "PX_" + METAL_NAMES.get(metal) + ".USD");
                double spotPrice = curve.getGridPoints().getGridPoint("Spot").getValue(EnumGptField.EffInput);
                logger.info("spot price for {}: {}", metal, spotPrice);
                double position = volume * spotPrice;
                try (Transaction cash = getCashDeal(session, dealNum)) {
                    cash.setValue(EnumTransactionFieldId.CashflowType, "AP/DP Margin Payment");
                    cash.setValue(EnumTransactionFieldId.ReferenceString, "AP/DP Margin " + dealNum);
                    cash.setValue(EnumTransactionFieldId.InternalBusinessUnit, "JM PMM HK");
                    cash.setValue(EnumTransactionFieldId.InternalPortfolio, "HK Fees");
                    cash.setValue(EnumTransactionFieldId.ExternalBusinessUnit, extBU);
                    cash.setValue(EnumTransactionFieldId.Position, position);
                    cash.process(EnumTranStatus.New);
                    logger.info("booked cash deal with tran num {}", cash.getTransactionId());
                }
            }
        }
    }
    
    @Override
    protected PreProcessResult check(Context context,
                                     EnumTranStatus targetStatus,
                                     PreProcessingInfo<EnumTranStatus>[] infoArray,
                                     Table clientData) {
        return null;
    }
    
    private void runApDpReport(Session session, String extBU) {
        int extBUId = session.getStaticDataFactory().getId(EnumReferenceTable.Party, extBU);
        
        Table reportParam = session.getTableFactory().createTable();
        reportParam.addColumn("start_date", EnumColType.Date);
        reportParam.addColumn("end_date", EnumColType.Date);
        reportParam.addColumn("external_bu_list", EnumColType.Table);
        TableRow row = reportParam.addRow();
        row.getCell("start_date").setDate(session.getBusinessDate());
        row.getCell("end_date").setDate(session.getBusinessDate());
        Table buList = session.getTableFactory().createTable();
        buList.addColumn("id", EnumColType.Int);
        TableRow buListRow = buList.addRow();
        buListRow.getCell("id").setInt(extBUId);
        row.getCell("external_bu_list").setTable(buList);
        session.getControlFactory()
                .runScript(
                        "/Plugins/Site/AdvancedPricingReporting/com/olf/jm/advancedPricingReporting/AdvancedPricingReporting",
                        reportParam);
    }
    
    private Transaction getCashDeal(Session session, int dealNum) {
        //language=TSQL
        String sqlTemplate = "SELECT tran_num\n" +
                             "    FROM ab_tran\n" +
                             "    WHERE current_flag = 1\n" +
                             "      AND tran_status = 2\n" +
                             "      AND reference = 'AP/DP Margin ${dealNum}'";
        Map<String, Object> variables = ImmutableMap.of("dealNum", dealNum);
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        Table result = session.getIOFactory().runSQL(sql);
        TradingFactory tradingFactory = session.getTradingFactory();
        return result.getRowCount() > 0
               ? tradingFactory.retrieveTransactionById(result.getInt(0, 0))
               : tradingFactory.createTransaction(tradingFactory.retrieveInstrumentByTicker(EnumInsType.CashInstrument,
                                                                                            "USD"));
    }
}
