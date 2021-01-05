package com.matthey.pmm.mtm.reporting.scripts;

import ch.qos.logback.classic.Logger;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MoreCollectors;
import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.EnhancedTradeProcessListener;
import com.matthey.pmm.ScriptHelper;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.simulation.EnumResultType;
import com.olf.openrisk.simulation.ResultType;
import com.olf.openrisk.simulation.Scenario;
import com.olf.openrisk.simulation.Simulation;
import com.olf.openrisk.simulation.SimulationFactory;
import com.olf.openrisk.staticdata.Currency;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.Portfolio;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumInsType;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import com.olf.openrisk.trading.Transactions;
import com.openlink.util.constrepository.ConstRepository;
import org.apache.commons.text.StringSubstitutor;

import java.time.LocalDate;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@ScriptCategory({EnumScriptCategory.OpsSvcTrade})
public class FxAutoSweep extends EnhancedTradeProcessListener {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(FxAutoSweep.class);
    
    @Override
    protected void process(Session session, DealInfo<EnumTranStatus> dealInfo, boolean succeed, Table clientData) {
        StaticDataFactory staticDataFactory = session.getStaticDataFactory();
        TradingFactory tradingFactory = session.getTradingFactory();
        
        LocalDate startTradeDate = getStartTradeDate();
        logger.info("start trade date: {}", startTradeDate);
        
        for (int tranNum : dealInfo.getTransactionIds()) {
            logger.info("processing source tran num: {}", tranNum);
            Transaction source = tradingFactory.retrieveTransactionById(tranNum);
            Optional<Transaction> existingAutoSweep = getExistingAutoSweep(session, source.getDealTrackingId());
            logger.info("existing auto sweep tran num: {}", existingAutoSweep.map(Transaction::getTransactionId));
            
            if (source.getTransactionStatus() == EnumTranStatus.Cancelled) {
                existingAutoSweep.ifPresent(transaction -> transaction.process(EnumTranStatus.Cancelled));
                logger.info("source is cancelled and no auto sweep needed any more");
                continue;
            }
            
            LocalDate tradeDate = ScriptHelper.fromDate(source.getValueAsDate(EnumTransactionFieldId.TradeDate));
            if (tradeDate.isBefore(startTradeDate)) {
                logger.info("trade date {} is before start trade date{}, no action needed", tradeDate, startTradeDate);
                continue;
            }
            
            String internalPortfolioName = source.getValueAsString(EnumTransactionFieldId.InternalPortfolio);
            Portfolio internalPortfolio = staticDataFactory.getReferenceObject(Portfolio.class, internalPortfolioName);
            int baseCcyField = internalPortfolio.getFieldId("Portfolio Base Currency");
            String baseCcy = internalPortfolio.getValueAsString(baseCcyField);
            logger.info("base currency for portfolio {}: {}", internalPortfolioName, baseCcy);
            int fxPortfolioField = internalPortfolio.getFieldId("Target FX Portfolio");
            String fxPortfolio = internalPortfolio.getValueAsString(fxPortfolioField);
            logger.info("target fx portfolio for portfolio {}: {}", internalPortfolioName, fxPortfolio);
            if (fxPortfolio.isEmpty()) {
                logger.info("no target fx portfolio specified so no action needed");
                continue;
            }
            
            String settleCcy = source.getValueAsString(EnumTransactionFieldId.FxTermCurrency);
            if (settleCcy.equals(baseCcy)) {
                logger.info("settle currency {} is the same as portfolio base currency {}, no action needed",
                            settleCcy,
                            baseCcy);
                continue;
            }
            
            double tradePrice = getTradePrice(session, settleCcy, baseCcy);
            double fxDealtAmount = getFxDealtAmount(session, source, settleCcy);
            Date fxDate = source.getValueAsDate(EnumTransactionFieldId.FxDate);
            String internalBU = source.getValueAsString(EnumTransactionFieldId.InternalBusinessUnit);
            
            Transaction transaction = existingAutoSweep.orElse(createAutoSweep(tradingFactory, settleCcy, baseCcy));
            transaction.getField(EnumTransactionFieldId.FxBaseCurrency).setValue(settleCcy);
            transaction.getField(EnumTransactionFieldId.FxTermCurrency).setValue(baseCcy);
            transaction.getField(EnumTransactionFieldId.BuySell).setValue(fxDealtAmount > 0 ? "Sell" : "Buy");
            transaction.getField(EnumTransactionFieldId.FxDealtAmount).setValue(Math.abs(fxDealtAmount));
            transaction.getField(EnumTransactionFieldId.FxDate).setValue(fxDate);
            transaction.getField("Trade Price").setValue(tradePrice);
            transaction.getField(EnumTransactionFieldId.ReferenceString).setValue("FX Sweep");
            transaction.getField(EnumTransactionFieldId.InternalBusinessUnit).setValue(internalBU);
            transaction.getField(EnumTransactionFieldId.InternalPortfolio).setValue(internalPortfolioName);
            transaction.getField(EnumTransactionFieldId.ExternalBusinessUnit).setValue(internalBU);
            transaction.getField(EnumTransactionFieldId.ExternalPortfolio).setValue(fxPortfolio);
            transaction.getField("Hedge Source").setValue(source.getDealTrackingId());
            transaction.process(EnumTranStatus.Validated);
            logger.info("new auto sweep tran num: {}", transaction.getTransactionId());
        }
    }
    
    private Transaction createAutoSweep(TradingFactory tradingFactory, String settleCurrency, String baseCurrency) {
        return tradingFactory.createTransaction(tradingFactory.retrieveInstrumentByTicker(EnumInsType.FxInstrument,
                                                                                          settleCurrency +
                                                                                          "/" +
                                                                                          baseCurrency));
    }
    
    private double getTradePrice(Session session, String settleCurrency, String baseCurrency) {
        return session.getMarketFactory()
                .getMarket()
                .getFXSpotRate(session.getStaticDataFactory().getReferenceObject(Currency.class, settleCurrency),
                               session.getStaticDataFactory().getReferenceObject(Currency.class, baseCurrency));
    }
    
    private LocalDate getStartTradeDate() {
        try {
            return LocalDate.parse(new ConstRepository("MtmReporting", "").getStringValue("StartTradeDate"));
        } catch (Exception e) {
            throw new RuntimeException("cannot retrieve start trade date from configuration", e);
        }
    }
    
    @Override
    protected PreProcessResult check(Context context,
                                     EnumTranStatus targetStatus,
                                     PreProcessingInfo<EnumTranStatus>[] infoArray,
                                     Table clientData) {
        return null;
    }
    
    private double getFxDealtAmount(Session session, Transaction transaction, String settleCurrency) {
        String name = "FX Sweep";
        SimulationFactory simulationFactory = session.getSimulationFactory();
        ResultType resultType = simulationFactory.getResultType(EnumResultType.CashflowByDay);
        Scenario scenario = simulationFactory.createScenario(name);
        scenario.getResultTypes().add(resultType);
        Simulation simulation = simulationFactory.createSimulation(name);
        simulation.addScenario(scenario);
        Transactions transactions = session.getTradingFactory().createTransactions();
        transactions.add(transaction);
        Table results = simulation.run(transactions)
                .getScenarioResults(name)
                .getResultsForType(EnumResultType.CashflowByDay)
                .asTable();
        int settleCurrencyId = session.getStaticDataFactory().getId(EnumReferenceTable.Currency, settleCurrency);
        return results.getRows()
                .stream()
                .filter(row -> row.getInt("currency") == settleCurrencyId)
                .map(row -> row.getDouble("cflow"))
                .collect(MoreCollectors.onlyElement());
    }
    
    private Optional<Transaction> getExistingAutoSweep(Session session, int sourceDealNum) {
        //language=TSQL
        String sqlTemplate = "SELECT t.tran_num\n" +
                             "    FROM ab_tran_info_view i\n" +
                             "             JOIN ab_tran t\n" +
                             "                  ON t.tran_num = i.tran_num AND t.current_flag = 1\n" +
                             "    WHERE type_name = 'Hedge Source'\n" +
                             "      AND value = '${sourceDealNum}'";
        Map<String, Object> variables = ImmutableMap.of("sourceDealNum", sourceDealNum);
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        Table result = session.getIOFactory().runSQL(sql);
        return result.getRowCount() > 0
               ? Optional.of(session.getTradingFactory()
                                     .retrieveTransactionById(result.getInt(0, 0)))
               : Optional.empty();
    }
}
