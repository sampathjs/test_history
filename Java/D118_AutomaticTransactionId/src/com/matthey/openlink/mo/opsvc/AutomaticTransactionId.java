package com.matthey.openlink.mo.opsvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

//import com.matthey.openlink.bo.opsvc.ValidateCollateralBalance;
import com.matthey.openlink.utilities.DataAccess;
import com.matthey.openlink.utilities.Repository;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.openjvs.OException;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableColumn;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.table.TableRows;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;
import com.openlink.endur.utilities.logger.LogCategory;
import com.openlink.endur.utilities.logger.LogLevel;
import com.openlink.endur.utilities.logger.Logger;

 
/** D118,120
 * Automatically generate a unique transaction identifier<br>
 * All deal types are covered<br>
 * <p>It will consist of the 2 alpha characters, which are applied to the <i>TranInfo</i> field <b>'{@value #SUFFIX_NAME}'</b>
 * The valid value are stored in a user table <code>{@value #USERTABLE}</code> which restricts the values like a <i>picklist</i>
 * </p>
 * @version $Revision: 43 $
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class AutomaticTransactionId extends AbstractTradeProcessListener {

	static final String TRADE_INFO_NAME = "JM_Transaction_Id";
	static final private String USERTABLE = "USER_jm_transaction_id";
	static final private String INSTRUMENT_NAME = "ins_name";
	static final String SUFFIX_NAME = "suffix";
	static final String TRADE_STATUS_NAME = "trade_status";
	static final String BUYSELL_NAME = "buy_sell";
	static final private String AUTOSTAMP_NAME = "auto_prefix";
	static final private String AUTOSTAMP_DEFAULT = "Y";
	static final String INSTRUMENT_SUBTYPE = "ins_subtype";
	
	private Context context = null;
	private static Map<String,String> preciousMetalCurrencies=new HashMap<String,String>(0);
	
	static private List<EnumTranStatus> permissableStatus = Arrays.asList(new EnumTranStatus[] {
			EnumTranStatus.Pending,
			EnumTranStatus.Validated });


	 enum PRECIOUS_METAL {
		Present(1),
		NotPresent(0),
		Ignore(-1);
		
		private final int value;
		private PRECIOUS_METAL(int value) {
			this.value=value;
		}
		
		public int toInt(){
			return this.value;
		}
		
		public static PRECIOUS_METAL fromInt(int value) {
			for(PRECIOUS_METAL item :PRECIOUS_METAL.values()){
				if (item.toInt()==value)
					return item;
			}
			return null;
		}
	}
	 
	
		private static final Map<String, String> configuration;
		private static final String SAVE_TRANINFO = "Save_TranInfo";
		private static final String SAVE_YES = "Yes";
	    static
	    {
	    	configuration = new HashMap<String, String>();
	    	configuration.put(SAVE_TRANINFO,SAVE_YES);
	    }
		public Properties properties;
	 
	/** 
	 * Only perform processing if one of the <i>permissableStatus</i> applies to the supplied transaction(s). 
	 * <br><b>CAUTION:</b> if an transaction is associated with an Offset we have to get that from the {@code PreProcsseingInfo} collection directly, <b>not</b> via the transaction
	 **/
	@Override
	public PreProcessResult preProcess(Context context, EnumTranStatus targetStatus, PreProcessingInfo<EnumTranStatus>[] infoArray, Table clientData) {

		Logging.init(context, this.getClass(), "AutoTransactionId", "");
		
		properties = Repository.getConfiguration("AutoTransactionId", "CONFIG", configuration);

		if (true/*permissableStatus.contains(targetStatus)*/) { //REVIEW: FPa requested 03 Dec 2015, see e-mail 2-Dec-2015
			System.out.println("STARTING " + this.getClass().getSimpleName());

			if (context.getIOFactory().getUserTables(USERTABLE).getCount()<1) {
				String reason = String.format("Configuration Error: user table(%s) missing!", USERTABLE);
				//Logger.log(LogLevel.ERROR, LogCategory.Trading, this, reason);
				Logging.info(reason);
				return PreProcessResult.failed(reason);
			}
			
			this.context = context;
			if (!areUserTableEntriesUnique()) {
				String reason = String.format("Configuration Error: user table(%s) has non-unique entries!", USERTABLE);
				//Logger.log(LogLevel.ERROR, LogCategory.Trading, this, reason);
				Logging.info(reason);
				return PreProcessResult.failed(reason);				
			}
			
			
			for (PreProcessingInfo<?> ets : infoArray) {
				
				Transaction transaction = null;
				try {
					transaction = ets.getTransaction();
					if (transaction.isLocked()) { //FIXME 25Nov - Metal xfer TPM failure workaround!
					Logging.info(/*Logger.log(LogLevel.FATAL, LogCategory.Trading, this,*/ 
							String.format("Tran#%d, Deal#%d is currently LOCKED by %s(%s)!!!",
									transaction.getTransactionId(),
									transaction.getDealTrackingId(),
									transaction.getLockOwner().getName(),
									transaction.getLockType().toString()));
						try {
							transaction.unlock();
						} catch (Exception e) {
							Logging.info("ERROR unlocking transaction!: " + e.getMessage());
							throw e;
						}
					}
					PreProcessResult result;
						if (!(result= updateTransactionSuffix(transaction, ets.getOffsetTransaction(), targetStatus)).isSuccessful()) {
							return result;
						}
						
				} catch (Exception e) {
					String reason = String.format("PreProcess>Tran#%d FAILED(2) %s CAUSE:%s", null != transaction ? transaction.getTransactionId() : -888, this.getClass().getSimpleName(), e.getLocalizedMessage());
					//Logger.log(LogLevel.FATAL, LogCategory.Trading, this, reason, e);
					Logging.error(reason, e);
					e.printStackTrace();
					return PreProcessResult.failed(reason);
					
				} 
					
			}
		}

		return PreProcessResult.succeeded();

	}

	/**
	 * Apply the appropriate <b>suffix code</b> via lookup of the defined entry in the user table <i>{@value #USERTABLE}</i>
	 * The suffix will be applied to the {@value #SUFFIX_NAME} of the supplied {@linkplain transaction} 
	 * In the event an associated offset transaction is also present we process this once the main transaction has been evaluated, so offset transactions are also stamped
	 */
	private PreProcessResult updateTransactionSuffix(final Transaction sourceTransaction, final Transaction offsetTransaction, final EnumTranStatus targetStatus) {
		int offsetTransactionsToProcess = (offsetTransaction!=null ? 1 : 0);
		Transaction transaction = sourceTransaction;
		Logging.info(/*Logger.log(LogLevel.INFO, LogCategory.Trading, this,*/ 
				String.format("Tran# %d %s>%s< with Info value>%s<", transaction.getTransactionId(), (offsetTransaction!=null ? "(HAS offsetTran also) status" : "has status"),  
						targetStatus.getName(), transaction.getField(TRADE_INFO_NAME).getValueAsString()));
		
		if (tranInfoUpdateAllowed(transaction)) {
			
			//OLF - AppSvr refactor 9-Nov-2015
/*			//TODO refactor to use configrable pairs, or fix environment cause...
			if ("Cash".equalsIgnoreCase(transaction.getInstrumentTypeObject().getInstrumentTypeEnum().getName() getValue())
					&& "Cash Transfer".equalsIgnoreCase(transaction.getInstrumentSubType().getName()) ) {
				Logger.log(LogLevel.INFO, LogCategory.Trading, this, 
						String.format("Skipping CASH Transfer on Tran# %d ", transaction.getTransactionId()));

				return PreProcessResult.succeeded();
			}*/
				
			Table userControl = DataAccess.getDataFromTable(context, String.format(
					"SELECT * from %s where %s=%d AND %s<>'%s'", 
						USERTABLE, 
						INSTRUMENT_NAME,
						transaction.getInstrumentTypeObject().getInstrumentTypeEnum().getValue(), 
						AUTOSTAMP_NAME,
						AUTOSTAMP_DEFAULT
						));
			// precious metal check

			try {
				
				do {
					Logging.info(/*Logger.log(LogLevel.DEBUG, LogCategory.Trading, this,*/ 
							String.format("Tran# Has Offset(%d)", offsetTransactionsToProcess));
					TableRows rows = userControl.getRows();
					for (TableRow currentRow : rows) {
						if (currentRow.getInt(BUYSELL_NAME) == transaction.getField(EnumTransactionFieldId.BuySell).getValueAsInt()
								&& currentRow.getString(TRADE_STATUS_NAME).equalsIgnoreCase(targetStatus.getName())
								&& (currentRow.getString(INSTRUMENT_SUBTYPE).trim().length()<1 
										|| currentRow.getString(INSTRUMENT_SUBTYPE).equalsIgnoreCase(transaction.getInstrumentSubType().getName()))) {
							
							if (PRECIOUS_METAL.fromInt(currentRow.getInt("precious_metal")) != PRECIOUS_METAL.Ignore) {
								preciousMetalRefresh();
								if (isValid(transaction, PRECIOUS_METAL.fromInt(currentRow.getInt("precious_metal")))) {
									setSuffix(transaction, currentRow);
									break;
								} else
									continue;
							} else {
	
							// objective found - apply suffix ignoring PRECIOUS_METAL criteria
							setSuffix(transaction, currentRow);
							break;
							}
						}
					}
					if (offsetTransactionsToProcess>0) {
						transaction = offsetTransaction;
					}
				} while(0<offsetTransactionsToProcess--);//no match
				
			} catch (Exception e) {
				String reason = String.format("Tran#%d FAILED(1) %s CAUSE:%s", transaction.getTransactionId(), this.getClass().getSimpleName(), e.getLocalizedMessage());
				//Logger.log(LogLevel.FATAL, LogCategory.Trading, this, reason, e);
				Logging.error(reason, e);
				e.printStackTrace();
				return PreProcessResult.failed(reason);
				
			} finally {
				if (userControl!=null)
					userControl.dispose();
			}
						
			Logging.info(/*Logger.log(LogLevel.INFO, LogCategory.Trading, this,*/ 
					String.format(
							"Tran# %d Infofield>%s<(%s) processed for status %s",
							transaction.getTransactionId(),
							TRADE_INFO_NAME,
							(offsetTransaction == null ? 
									transaction.getField(TRADE_INFO_NAME).getValueAsString()
									: sourceTransaction.getField(TRADE_INFO_NAME).getValueAsString()
											+ "/"
											+ offsetTransaction.getField(TRADE_INFO_NAME)
													.getValueAsString()),
							targetStatus.toString()));
		}
		return PreProcessResult.succeeded();
	}

	private void setSuffix(Transaction transaction, TableRow currentRow) {
		System.out.println(String.format("\nSETTING TranInfo -> %s",currentRow==null ? "Populated" : "NULL") );
		System.out.println(String.format("\nSETTING TranInfo -> Row=%d",currentRow.getNumber()) );
		Field tranInfoField = transaction.getField(TRADE_INFO_NAME);
		if (!tranInfoField.isApplicable()) {
			Logging.info(/*Logger.log(LogLevel.INFO,
					LogCategory.Trading,
					this,*/
					String.format("Tran# %d can't apply Info value>%s<", transaction.getTransactionId(),
							TRADE_INFO_NAME));
			throw new OpenRiskException("Unsupported TranInfo Field");
			
		} else {
			Logging.info(/*Logger.log(LogLevel.INFO,
				LogCategory.Trading,
				this,*/
					String.format("Tran# %d Field:%s>%s", transaction.getTransactionId(),TRADE_INFO_NAME,
							tranInfoField.toString()));
		System.out.println(String.format("\nSETTING TranInfo -> Value=%s",currentRow.getString(SUFFIX_NAME)) );
		tranInfoField.setValue(currentRow.getString(SUFFIX_NAME));
		try {
			checkMandatoryFields(transaction);
			if (transaction.isAnyInfoFieldModified(true) ) {
					if ( 0!=SAVE_YES.compareToIgnoreCase(properties.getProperty(SAVE_TRANINFO))) {
						Logging.info(/*Logger.log(LogLevel.INFO,
								LogCategory.Trading,
								this,*/
								String.format("Tran# %d SKIP Save", transaction.getTransactionId()));						
					} else
						transaction.saveInfoFields();
			}
		} catch (OpenRiskException oe) {
			Logging.info(/*Logger.log(LogLevel.INFO,
					LogCategory.Trading,
					this,*/
					String.format("Tran# %d ERR:%s", transaction.getTransactionId(),oe.getMessage()));
			Logging.info(/*Logger.log(LogLevel.INFO,
					LogCategory.Trading,
					this,*/
					String.format("TranData:-%s", transaction.asTable().asXmlString()));
			throw oe;
		}
		Field isOffset = transaction.getField(EnumTransactionFieldId.OffsetTransactionType);
		Logging.info(/*Logger.log(LogLevel.INFO,
				LogCategory.Trading,
				this,*/
				String.format("Tran# %d%s applied Info value>%s<", transaction.getTransactionId(),(isOffset.isApplicable()==true ? "(Offset:" +isOffset.getDisplayString()+")" : ""),
						transaction.getField(TRADE_INFO_NAME).getValueAsString()));
		}
		System.out.println("\n **SETTING completed\n**");
	}

	private void checkMandatoryFields(Transaction transaction) {
		
		String[] requiredInfofields =  new String[] {"Form", "Loco"};
		for (String tranInfoName : requiredInfofields) {
			try{
			Field requiredField = transaction.getField(tranInfoName);
			if (null == requiredField || requiredField.getDisplayString().trim().isEmpty()){
				Logging.info(String.format("ERR: Tran%d required field(%s) not populated!",
						transaction.getTransactionId(), tranInfoName));
				throw new OpenRiskException(String.format("Required Info field(%s) not set!", tranInfoName));
			}
			} catch (OpenRiskException ore) {
				if (ore.getMessage().contains(tranInfoName) 
						&& ore.getMessage().contains("is not applicable")) // failure due to TranInfo not valid for current Tran
					continue;
			}
		}
		
	}

	/**
	 * 
	 * Is active transaction have a leg which has the appropriate PRECIOUS metal
	 */
	private boolean isValid(Transaction transaction, PRECIOUS_METAL preciousMetal) {

		int legsWithPreciousMetal=0;
		if (transaction.getLegCount()<0) {
			Field currency = transaction.getField(EnumTransactionFieldId.Currency);
			if (currency.isApplicable() && isPreciousMetal(currency))
				legsWithPreciousMetal++;
		} else
		for(Leg leg: transaction.getLegs()) {
			Field currency = leg.getField( EnumLegFieldId.Currency);
			boolean legCurrencyIsPreciousMetal = isPreciousMetal(currency);
			if ( legCurrencyIsPreciousMetal && preciousMetal == PRECIOUS_METAL.NotPresent) {
				return false;
			}
			if (legCurrencyIsPreciousMetal ){
				legsWithPreciousMetal++;
			} 
		}
	
		if ( preciousMetal==PRECIOUS_METAL.Present && legsWithPreciousMetal<1)
			return false;

		return true;
	}

	/**
	 * determine if supplied currency code matches the class collection of precious metals
	 * @return true if it is found otherwise false
	 */
	private boolean isPreciousMetal(Field currency) {
		boolean result=false;
		if (preciousMetalCurrencies.containsKey(currency.getValueAsString()))
				result = true;
				
		return result;
	}

	/**
	 * Update the class level collection with the currently identified precious metals
	 * 
	 */
	private void preciousMetalRefresh() {

		Map<String, String> preciousMetalCurrencies = new HashMap<String, String>(
				0);
		Table userControl = DataAccess.getDataFromTable(context,
				String.format(
						"SELECT name, id_number,description, precious_metal  "
							+ "\nFROM currency"
							+ "\nWHERE precious_metal = 1", "currency"));
		if (userControl.getRowCount() > 0) {
			for (TableRow currentRow : userControl.getRows()) {
				preciousMetalCurrencies.put(
						currentRow.getString("name").trim(),
						currentRow.getString("description"));
			}
		}
		Logging.info(/*Logger.log(LogLevel.DEBUG, LogCategory.Trading, this.getClass(),*/ String
				.format("Precious Metals: %d existing %d now",
						AutomaticTransactionId.preciousMetalCurrencies.size(),
						preciousMetalCurrencies.size()));
		AutomaticTransactionId.preciousMetalCurrencies.clear();
		AutomaticTransactionId.preciousMetalCurrencies
				.putAll(preciousMetalCurrencies);
	}

	/**
	 * Determine if we should alter the existing value of the TranInfo field <i>{@value #SUFFIX_NAME}</i>
	 * <p>Configuration entry {@value #AUTOSTAMP_NAME} in <i>{@value #USERTABLE}</i> identifies valid entries populated by upstream processing
	 * where the value is not from an upstream process, overwriting is permissible. </p>
	 */
	private boolean tranInfoUpdateAllowed(Transaction transaction) {
		
		Field tradeField = transaction.getField(TRADE_INFO_NAME);
		if (!tradeField.isApplicable())
			return false;
			
		if (0 == "".compareTo(tradeField.getValueAsString())) {
			return true;
		}
		
		// if upstream process already populated TranInfo skip processing
		Table userControl = DataAccess.getDataFromTable(context, String.format(
				"SELECT * from %s where %s='%s'", 
					USERTABLE, 
					AUTOSTAMP_NAME,
					AUTOSTAMP_DEFAULT));
		if (userControl.getRowCount()>0) { 
			TableColumn autoStampValues = userControl.getColumn(SUFFIX_NAME);
			if (null != autoStampValues.find(tradeField.getValueAsString())) {
				Logging.info(/*Logger.log(LogLevel.WARNING, LogCategory.Trading, this,*/
						String.format("Tran# %d auto-suffix set by Upstream activity", transaction.getTransactionId()));
				return false;
			}
		}
		
		return true;
	}

	/**
	 * Determine if the USER Table {@value #USERTABLE} contains only unique entries based on the instrument, buy/sell and status
	 * returns true if all are unique, otherwise false
	 */
	private boolean areUserTableEntriesUnique() {
		System.out.println("Checking Uniqueness...");
		boolean uniqueEntries = false;
		Table userControl = DataAccess.getDataFromTable(context, String.format(
		"SELECT count(wrk.ins_name) as [uniqueTotal],(SELECT count(ins_name) FROM %s) as [total] " +
		"\nFROM (SELECT ins_name,ins_subtype, trade_status, buy_sell,auto_prefix,precious_metal " + 
		"\n FROM %s " + 
		"\n  GROUP BY ins_name, ins_subtype, trade_status, buy_sell, auto_prefix, precious_metal) wrk", USERTABLE,USERTABLE));
		if (userControl.getRowCount()>0) { 
			  
			if (Math.abs(userControl.getInt("uniqueTotal",0) - userControl.getInt("total",0)) > 0) {
				Logging.info(/*Logger.log(LogLevel.WARNING, LogCategory.Trading, this,*/
						String.format("auto-suffix mismatch on unique(%d) entries (%d)", userControl.getInt("unique",0), userControl.getInt("total",0)));
				uniqueEntries = false;
			} else
				uniqueEntries = true;
		}
		//userControl.dispose();
		return uniqueEntries;
	}

	
}
