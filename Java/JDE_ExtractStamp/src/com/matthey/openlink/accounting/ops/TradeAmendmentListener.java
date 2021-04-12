package com.matthey.openlink.accounting.ops;

import com.matthey.openlink.reporting.ops.Sent2GLStamp;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ScriptCategory({EnumScriptCategory.TradeInput})
public class TradeAmendmentListener extends AbstractTradeProcessListener {

    private static final String CONST_REPO_CONTEXT = "Accounting";
    private static final String CONST_REPO_SUBCONTEXT = "JDE_Extract_Stamp";

    @Override
    public PreProcessResult preProcess(Context context,
                                       EnumTranStatus targetStatus,
                                       PreProcessingInfo<EnumTranStatus>[] infoArray,
                                       Table clientData) {
        try {
            init(context);
            for (PreProcessingInfo<?> activeItem : infoArray) {
                Transaction transaction = activeItem.getTransaction();
                String instrumentType = transaction.getInstrument().getToolset().toString().toUpperCase();
                int dealNum = transaction.getDealTrackingId();
                Logging.info(String.format("Inputs : Deal-%d, TargetStatus-%s", dealNum, targetStatus.getName()));
                Logging.info(String.format("Processing %s deal %s for allow/block amendment check",
                                           instrumentType,
                                           transaction.getDealTrackingId()));

                String instrumentInfoField = Sent2GLStamp.TranInfoInstrument.getOrDefault(instrumentType,
                                                                                          "General Ledger");
                if (transaction.getField(instrumentInfoField) == null) {
                    return PreProcessResult.failed("Field " +
                                                   instrumentInfoField +
                                                   " not available for Deal " +
                                                   dealNum);
                }

                PreProcessResult result = assessGLStatus(context, transaction, targetStatus);
                if (!result.isSuccessful()) {
                    Logging.info(String.format("Blocking the transition for deal#%d from current status-%s to status-%s",
                                               dealNum,
                                               transaction.getTransactionStatus().getName(),
                                               targetStatus.getName()));
                    return result;
                }
            }
            return PreProcessResult.succeeded();
        } catch (Exception e) {
            Logging.error(e.getMessage());
            for (StackTraceElement ste : e.getStackTrace()) {
                Logging.error(ste.toString());
            }
            return PreProcessResult.failed(e.getMessage());
        } finally {
            Logging.close();
        }
    }

    @Override
    public void postProcess(Session session, DealInfo<EnumTranStatus> deals, boolean succeeded, Table clientData) {
        final TradingFactory tradingFactory = session.getTradingFactory();
        final String
                sqlTemplate
                = "select max(tran_num) tran_num from ab_tran where tran_status = 10 and deal_tracking_num = ";
        for (int currentTranNum : deals.getTransactionIds()) {
            Transaction current = tradingFactory.retrieveTransactionById(currentTranNum);
            String instrumentType = current.getInstrument().getToolset().toString().toUpperCase();
            String instrumentInfoField = Sent2GLStamp.TranInfoInstrument.getOrDefault(instrumentType, "General Ledger");
            stampDefaultFlagValue(current, instrumentInfoField);
            int dealNum = current.getDealTrackingId();
            try (Table amendedTrade = session.getIOFactory().runSQL(sqlTemplate + dealNum)) {
                int amendedTranNum = amendedTrade.getInt("tran_num", 0);
                stampDefaultFlagValue(tradingFactory.retrieveTransactionById(amendedTranNum), instrumentInfoField);
            }
        }
    }

    private void stampDefaultFlagValue(Transaction tran, String instrumentInfoField) {
        tran.getField(instrumentInfoField).setValue("Pending Sent");
        tran.saveInfoFields(false, true);
    }

    private PreProcessResult assessGLStatus(Context context, Transaction transaction, EnumTranStatus targetStatus) {
        int dealNum = transaction.getDealTrackingId();

        if (targetStatus == EnumTranStatus.CancelledNew || targetStatus == EnumTranStatus.Cancelled) {
            Logging.info(String.format("Allowing deal#%s for cancellation", dealNum));
            return PreProcessResult.succeeded();
        } else {
            Logging.info(String.format("Processing GL deal#%s for amendment check", dealNum));

            Map<String, Object> fieldsForOldTran = getUnamendableFields(context.getTradingFactory()
                                                                                .retrieveTransactionByDeal(dealNum));
            Map<String, Object> fieldsForNewTran = getUnamendableFields(transaction);
            List<String> changedFields = fieldsForNewTran.keySet()
                    .stream()
                    .filter(key -> !fieldsForNewTran.get(key).equals(fieldsForOldTran.get(key)))
                    .collect(Collectors.toList());
            if (!changedFields.isEmpty()) {
                return PreProcessResult.failed("The following fields cannot be amended: " +
                                               String.join(";", changedFields));
            }

            LocalDate tradeDate = toLocalDate(transaction.getValueAsDate(EnumTransactionFieldId.TradeDate));
            LocalDate currentDate = toLocalDate(context.getTradingDate());
            int yearDiff = currentDate.getYear() - tradeDate.getYear();
            int monthDiff = currentDate.getMonthValue() - tradeDate.getMonthValue();
            return (yearDiff * 12 + monthDiff) <= 1
                   ? PreProcessResult.succeeded()
                   : PreProcessResult.failed("trade date is earlier than previous month");
        }
    }

    private Map<String, Object> getUnamendableFields(Transaction transaction) {
        HashMap<String, Object> fields = new HashMap<>();
        fields.put("Internal BU", getField(transaction, EnumTransactionFieldId.InternalBusinessUnit));
        fields.put("Internal LE", getField(transaction, EnumTransactionFieldId.InternalLegalEntity));
        fields.put("External BU", getField(transaction, EnumTransactionFieldId.ExternalBusinessUnit));
        fields.put("External LE", getField(transaction, EnumTransactionFieldId.ExternalLegalEntity));
        fields.put("Ticker", getField(transaction, EnumTransactionFieldId.Ticker));
        fields.put("Base Currency", getField(transaction, EnumTransactionFieldId.FxBaseCurrency));
        fields.put("Term Currency", getField(transaction, EnumTransactionFieldId.FxTermCurrency));
        for (int leg = 0; leg < transaction.getLegCount(); leg++) {
            fields.put("Currency " + leg, transaction.getLeg(leg).getValueAsString(EnumLegFieldId.Currency));
        }
        return fields;
    }

    private String getField(Transaction transaction, EnumTransactionFieldId fieldId) {
        Field field = transaction.getField(fieldId);
        return field.isApplicable() && field.isReadable() ? field.getValueAsString() : "";
    }

    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void init(Session session) {
        try {
            Logging.init(session, this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
            Logging.info("\n\n********************* Start of new run ***************************");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
