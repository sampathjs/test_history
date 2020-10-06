package com.matthey.pmm.jde.corrections.scripts;

import ch.qos.logback.classic.Logger;
import com.matthey.pmm.EndurLoggerFactory;
import com.matthey.pmm.EnhancedTradeProcessListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ScriptCategory({EnumScriptCategory.TradeInput})
public class TradeAmendmentListener extends EnhancedTradeProcessListener {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(TradeAmendmentListener.class);
    
    private static final Map<String, String> STAMPING_TRAN_INFO = Collections.singletonMap("CASH", "Metal Ledger");
    
    @Override
    protected void process(Session session, DealInfo<EnumTranStatus> dealInfo, boolean succeed, Table table) {
    }
    
    @Override
    public PreProcessResult check(Context context,
                                  EnumTranStatus targetStatus,
                                  PreProcessingInfo<EnumTranStatus>[] infoArray,
                                  Table clientData) {
        for (PreProcessingInfo<?> activeItem : infoArray) {
            Transaction transaction = activeItem.getTransaction();
            String instrumentType = transaction.getInstrument().getToolset().toString().toUpperCase();
            int dealNum = transaction.getDealTrackingId();
            logger.info(String.format("Inputs : Deal-%d, TargetStatus-%s", dealNum, targetStatus.getName()));
            logger.info(String.format("Processing %s deal %s for allow/block amendment check",
                                      instrumentType,
                                      transaction.getDealTrackingId()));
            
            String instrumentInfoField = STAMPING_TRAN_INFO.getOrDefault(instrumentType, "General Ledger");
            if (transaction.getField(instrumentInfoField) == null) {
                return PreProcessResult.failed("Field " + instrumentInfoField + " not available for Deal " + dealNum);
            }
            
            PreProcessResult result = assessGLStatus(context, transaction, targetStatus);
            if (!result.isSuccessful()) {
                logger.info(String.format("Blocking the transition for deal#%d from current status-%s to status-%s",
                                          dealNum,
                                          transaction.getTransactionStatus().getName(),
                                          targetStatus.getName()));
                return result;
            }
        }
        return PreProcessResult.succeeded();
    }
    
    private PreProcessResult assessGLStatus(Context context, Transaction transaction, EnumTranStatus targetStatus) {
        int dealNum = transaction.getDealTrackingId();
        
        if (targetStatus == EnumTranStatus.CancelledNew || targetStatus == EnumTranStatus.Cancelled) {
            logger.info(String.format("Allowing deal#%s for cancellation", dealNum));
            return PreProcessResult.succeeded();
        } else {
            logger.info(String.format("Processing GL deal#%s for amendment check", dealNum));
            
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
        fields.put("FX Swap Period", getField(transaction, EnumTransactionFieldId.FxSwapPeriod));
        fields.put("FX Far Period", getField(transaction, EnumTransactionFieldId.FxFarPeriod));
        fields.put("FX Date", getField(transaction, EnumTransactionFieldId.FxDate));
        fields.put("FX Far Date", getField(transaction, EnumTransactionFieldId.FxFarDate));
        for (int legNum = 0; legNum < transaction.getLegCount(); legNum++) {
            fields.put("Currency " + legNum, getCurrency(transaction.getLeg(legNum)));
            fields.put("Form " + legNum, getTranInfo(transaction, 20014, legNum));
            fields.put("Loco " + legNum, getTranInfo(transaction, 20015, legNum));
        }
        return fields;
    }
    
    private String getField(Transaction transaction, EnumTransactionFieldId fieldId) {
        Field field = transaction.getField(fieldId);
        return field.isApplicable() && field.isReadable() ? field.getValueAsString() : "";
    }
    
    private String getTranInfo(Transaction transaction, int tranInfoId, int side) {
        Field field = transaction.retrieveField(tranInfoId, side);
        return field.isApplicable() && field.isReadable() ? field.getValueAsString() : "";
    }
    
    private String getCurrency(Leg leg) {
        Field field = leg.getField("Currency");
        return (field != null && field.isApplicable() && field.isReadable()) ? field.getValueAsString() : "";
    }
    
    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
