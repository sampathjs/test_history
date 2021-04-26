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
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumResetFieldId;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.matthey.pmm.ScriptHelper.fromDate;
import static com.matthey.pmm.jde.corrections.connectors.ConfigurationRetriever.getStartDate;
import static com.olf.openrisk.trading.EnumFixedFloat.FloatRate;

@ScriptCategory({EnumScriptCategory.TradeInput})
public class TradeAmendmentListener extends EnhancedTradeProcessListener {
    
    private static final Logger logger = EndurLoggerFactory.getLogger(TradeAmendmentListener.class);
    
    @Override
    protected void process(Session session, DealInfo<EnumTranStatus> dealInfo, boolean succeed, Table table) {
        for (int tranNum : dealInfo.getTransactionIds()) {
            logger.info("processing tran: {}", tranNum);
            Transaction transaction = session.getTradingFactory().retrieveTransactionById(tranNum);
            if (transaction.getTransactionStatus() == EnumTranStatus.Validated) {
                transaction.getField("General Ledger").setValue("Pending Sent");
                transaction.saveInfoFields();
                logger.info("reset general ledger flag for tran {}", tranNum);
            }
        }
    }
    
    @Override
    public PreProcessResult check(Context context,
                                  EnumTranStatus targetStatus,
                                  PreProcessingInfo<EnumTranStatus>[] infoArray,
                                  Table clientData) {
        logger.info("target status: {}", targetStatus.getName());
        for (PreProcessingInfo<?> activeItem : infoArray) {
            Transaction transaction = activeItem.getTransaction();
            String instrumentType = transaction.getInstrument().getToolset().toString().toUpperCase();
            if (instrumentType.equalsIgnoreCase("PREC-EXCH-FUT")) {
        		String jdeStatus = transaction.getField("General Ledger").getDisplayString();
        		if (jdeStatus.equalsIgnoreCase("Sent")) {
        			return PreProcessResult.failed("The deal #" + transaction.getDealTrackingId() 
        			+ " has already been sent to GL");
        		}
            }
            	
            	
            int dealNum = transaction.getDealTrackingId();
            logger.info("processing deal {}: instrument type {}", dealNum, instrumentType);
            PreProcessResult result = assessGLStatus(context, transaction, targetStatus);
            if (!result.isSuccessful()) {
                logger.info("blocking the transition for deal #{} from current status-{} to status-{}",
                            dealNum,
                            transaction.getTransactionStatus().getName(),
                            targetStatus.getName());
                return result;
            }
        }
        return PreProcessResult.succeeded();
    }
    
    private PreProcessResult assessGLStatus(Context context, Transaction transaction, EnumTranStatus targetStatus) {
        int dealNum = transaction.getDealTrackingId();
        
        if (targetStatus == EnumTranStatus.CancelledNew || targetStatus == EnumTranStatus.Cancelled) {
            logger.info("allowing deal #{} for cancellation", dealNum);
            return PreProcessResult.succeeded();
        } else {
            logger.info("processing deal #{} for amendment check", dealNum);
            
            String internalBU = transaction.getValueAsString(EnumTransactionFieldId.InternalBusinessUnit);
            if ("JM PMM HK".equals(internalBU)) {
                LocalDate tradeDate = fromDate(transaction.getValueAsDate(EnumTransactionFieldId.TradeDate));
                LocalDate startDate = getStartDate();
                if (tradeDate.isBefore(startDate)) {
                    String msg = "no HK deal booked before " + startDate + " can be amended";
                    logger.info(msg);
                    return PreProcessResult.failed(msg);
                }
            }
            
            Map<String, Object> fieldsForOldTran = getUnamendableFields(context.getTradingFactory()
                                                                                .retrieveTransactionByDeal(dealNum));
            Map<String, Object> fieldsForNewTran = getUnamendableFields(transaction);
            List<String> changedFields = fieldsForNewTran.keySet()
                    .stream()
                    .filter(key -> !fieldsForNewTran.get(key).equals(fieldsForOldTran.get(key)))
                    .collect(Collectors.toList());
            if (!changedFields.isEmpty()) {
                String msg = "The following fields cannot be amended: " + String.join(";", changedFields);
                logger.info(msg);
                return PreProcessResult.failed(msg);
            }
            
            boolean isComSwap = transaction.getToolset().equals(EnumToolset.ComSwap);
            LocalDate currentDate = toLocalDate(context.getTradingDate());
            LocalDate date = isComSwap
                             ? getMinResetDate(transaction)
                             : toLocalDate(transaction.getValueAsDate(EnumTransactionFieldId.TradeDate));
            return isDateAfterStartOfPrevMonth(currentDate, date)
                   ? PreProcessResult.succeeded()
                   : PreProcessResult.failed((isComSwap ? "first reset date" : "trade date") +
                                             " is earlier than the start of previous month");
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
    
    private boolean isDateAfterStartOfPrevMonth(LocalDate reference, LocalDate date) {
        logger.info("reference date: {}; date: {}", reference, date);
        int yearDiff = reference.getYear() - date.getYear();
        int monthDiff = reference.getMonthValue() - date.getMonthValue();
        return (yearDiff * 12 + monthDiff) <= 1;
    }
    
    private LocalDate getMinResetDate(Transaction transaction) {
        return toLocalDate(transaction.getLegs()
                                   .stream()
                                   .filter(leg -> leg.getValueAsInt(EnumLegFieldId.FixFloat) == FloatRate.getValue())
                                   .flatMap(leg -> leg.getResets().stream())
                                   .map(reset -> reset.getValueAsDate(EnumResetFieldId.Date))
                                   .min(Date::compareTo)
                                   .orElseThrow(() -> new RuntimeException("no reset exists")));
    }
}
