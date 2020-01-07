package com.matthey.openlink.trading.opsvc;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.matthey.openlink.utilities.DataAccess;
import com.matthey.openlink.utilities.Notification;
import com.matthey.openlink.utilities.Repository;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Application;
import com.olf.openrisk.application.EnumDebugLevel;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.calendar.DateTime;
import com.olf.openrisk.calendar.SymbolicDate;
import com.olf.openrisk.market.EnumBmo;
import com.olf.openrisk.market.EnumElementType;
import com.olf.openrisk.market.EnumGptCategory;
import com.olf.openrisk.market.EnumGptField;
import com.olf.openrisk.market.GridPoint;
import com.olf.openrisk.market.GridPoints;
import com.olf.openrisk.market.PriceLookup;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.staticdata.ReferenceChoice;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTranfField;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Fields;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
import com.olf.jm.logging.Logging;

/*
 * Version History
 * 1.0 - initial 
 * 1.1 - Change deal template to 'Spot_B2B' instead of 'Forward_B2B', set Fx Date to instrument Expiration Date.
 */

/** D439, 441
 * Book back to back transactions when a future trade is booked for a different region 
 * <BR>
 * Regional check is decided using <b>trader</b>(personnel) <i>country</i> against
 *  <b>Portfolio</b> info field(<i>{@value #DESK_LOCATION}</i>)
 * <br>Use a differential curve({@value #PRICEINDEX}) for pricing the back to back trade...
 * <p>If a generated back to back is changed subsequent changes to the originating deal will be blocked?!</p>
 * 
 * <p>Throws {@link Back2BackFowardException} which are notified to AlertBroker:-
 * 
 * <ul><li>Configuration related issues are report under code {@value #B2B_CONFIG}
 * <li>Portfolio issues use {@value #MISSING_PORTFOLIO_COUNTRY}
 * <li>Trader problems are {@value #MISSING_TRADER_COUNTRY}
 * <li>Tran Info issue use {@value #B2B_TRANINFO_PROBLEM}
 * <li>Synchronisation issues between the future and forward trades {@value #B2B_OUT_OF_SYNCH}
 * <li>Miscellaneous problems are reported with {@value #MISC_ERROR}
 * </ul> 
 * 
 * @version $Revision: 76 $
 */
@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class Back2BackForwards extends AbstractTradeProcessListener {
	
	private static final int B2B_CONFIG = 4390;
	private static final int MISC_ERROR = 4399;
	private static final int B2B_TRANINFO_PROBLEM = 4393;
	private static final int B2B_OUT_OF_SYNCH = 4398;
	private static final int MISSING_PORTFOLIO_COUNTRY = 4392;
	private static final int MISSING_TRADER_COUNTRY = 4391;
	
	private static final String CONST_REPO_CONTEXT="Back2Back";
	private static final String CONST_REPO_SUBCONTEXT="Configuration";
	
	private static final String TRANINFO_LINK="LinkTranInfo";
	static final String LINKED_INFO =  "Linked Deal";
	private static final String PRICEINDEX =  "JM_EFP";
	private static final String FX_TEMPLATE = "SpotTemplate";
	private static final String TEMPLATE_SPOT = "Forward_B2B"; //"Spot_B2B"; 
	private static final String BACK2BACK_LOCATION = "Loco"; 
	private static final String BACK2BACK_OFFSET_LOCATION = "Offset Loco";
	private static final String BACK2BACK_TRADE_PRICE = "Trade Price";

	private static final String USER_TABLE="UserTable";
	private static final String FUTURE_2_BACK2BACK_MAPPING = "USER_JM_auto_back_to_back";
	private static final String FUTURES_PROJECTION_INDEX = "futures_proj_index";
	private static final String BACK2BACK_MAPPED_LOCATION = "loco";
	static final String BACK2BACK_TICKER = "spot_ticker";
	private String symbPymtDate = null;

	static final char RECORD_SEPARATOR = 0x1B;
	
	private Session session = null;
	 private static final Map<String, String> configuration;
	 private ConstRepository constRep;
	 
	
	 private static Properties properties;
	    static
	    {
	    	configuration = new HashMap<String, String>();
	    	configuration.put(FX_TEMPLATE, TEMPLATE_SPOT);
	    	configuration.put(TRANINFO_LINK, LINKED_INFO);
	    	configuration.put(USER_TABLE, FUTURE_2_BACK2BACK_MAPPING);
	    	properties = Repository.getConfiguration(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT, configuration);
	    }
	/**
	 * OpService entry for handling qualifying Transactions
	 */
	@Override
	public void postProcess(Session session, DealInfo<EnumTranStatus> deals, boolean succeeded, Table clientData) {
		
		try {
			init();
			this.session = session;
			
			TradingFactory tf = session.getTradingFactory();
			PostProcessingInfo<EnumTranStatus>[] postprocessingitems = deals.getPostProcessingInfo();
			for (PostProcessingInfo<?> postprocessinginfo : postprocessingitems) {
				int dealNum = postprocessinginfo.getDealTrackingId();
				int tranNum = postprocessinginfo.getTransactionId();
				Logging.init(session, this.getClass(), "Back2BackForwards", "");
				Logging.info(String.format("Checking Tran#%d", tranNum));
				
				Transaction transaction = tf.retrieveTransactionById(tranNum);
				
				if (isFutureTraderFromDifferentBusinessUnit(transaction)) {
					updateBack2Back(transaction);
				}
			}
		} catch (Back2BackForwardException err) {
			Logging.error(err.getLocalizedMessage(), err);
			Notification.raiseAlert(err.getReason(), err.getId(), err.getLocalizedMessage());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			Logging.info("COMPLETED");
			Logging.close();
		}
		
		
			
	}


	/**
	 * Is supplied transaction trader defaulted to a different BU 
	 * 
	 */
	private boolean isFutureTraderFromDifferentBusinessUnit(Transaction transaction) {
		
		String traderCountry = getTraderCountry(transaction);
		return !("JM PMM UK".equalsIgnoreCase(traderCountry));
	}


	/**
	 * Get the default business name associated with the trader of the supplied transaction
	 */
	private String getTraderCountry(Transaction transaction)
	{
		String traderBusinessUnit = "";
		StaticDataFactory sdf = session.getStaticDataFactory();
		int traderId = transaction.getValueAsInt(EnumTransactionFieldId.InternalContact);
		Person trader = (Person)sdf.getReferenceObject(EnumReferenceObject.Person, traderId );

		Table defaultPersonnel = DataAccess.getDataFromTable(session,
				String.format("SELECT pp.personnel_id, pp.default_flag " +
				"\n  ,p.short_name, p.party_id " +
				"\n		FROM party_personnel pp " + 
				"\n		JOIN party p ON pp.party_id=p.party_id " +
				"\n WHERE pp.default_flag>0 AND pp.personnel_id=%d", traderId));
		
		if (null == defaultPersonnel || defaultPersonnel.getRowCount() != 1) {
			defaultPersonnel.dispose();
			throw new Back2BackForwardException("Configuration data", B2B_CONFIG,
					String.format("Unable to determine default Business Unit for trader(%s) on Tran#%d",
							trader.getName(), transaction.getTransactionId()));
		}
		traderBusinessUnit = defaultPersonnel.getString("short_name", 0);
		defaultPersonnel.dispose();
		return traderBusinessUnit;
	}
	
	private void setForwardPartyFromTrader(final Person trader, final EnumTransactionFieldId buField, final EnumTransactionFieldId leField, Transaction forward) {
		String businsessUnit = "business_unit";
		String legalEntity = "legal_entity";
		Table traderPartyInformation = DataAccess.getDataFromTable(session,
				String.format("SELECT pp.personnel_id, pp.default_flag " + 
				"\n	,p.short_name %s, p.party_id " + 
				"\n   ,pr.legal_entity_id, le.short_name %s " + 
				"\nFROM party_personnel pp " + 
				"\n	JOIN party p ON pp.party_id=p.party_id " + 
				"\n   JOIN party_relationship pr ON p.party_id=pr.business_unit_id " + 
				"\n   JOIN party le ON pr.legal_entity_id=le.party_id " + 
				"\nWHERE pp.default_flag>0 AND pp.personnel_id=%d",
				businsessUnit, 
				legalEntity, trader.getId()));
		
		if (null == traderPartyInformation || traderPartyInformation.getRowCount() != 1) {
			traderPartyInformation.dispose();
			throw new Back2BackForwardException("Configuration data", B2B_CONFIG,
					String.format("Unable to determine default Business Unit & Entity for trader(%s) ",
							trader.getName() ));
		}
		forward.setValue(buField,traderPartyInformation.getString(businsessUnit, 0));
		forward.setValue(leField, traderPartyInformation.getString(legalEntity, 0));

		traderPartyInformation.dispose();
		
	}
	
	/**
	 * Determine if we are creating new back2back transaction or amending and existing deal... 
	 */
	private void updateBack2Back(Transaction transaction) {
		
		int back2BackTran = Back2BackForwards.back2backTransaction(transaction.getField(properties.getProperty(TRANINFO_LINK)));
			Transaction back2Back;
			if (back2BackTran<1) {
				back2Back=createBack2Back(transaction);
				
			} else {
				back2Back=amendBack2Back(transaction);
			}
					
			if (Application.getInstance().getCurrentSession()
					.getDebug().atLeast(EnumDebugLevel.Low)) {
				Logging.info(String.format("Tran#%d updated from Tran#%d", back2Back.getTransactionId(),
						transaction.getTransactionId()));
			}
			//session.getDebug().viewTable(back2Back.asTable());
	
			try {
				for(Field field:back2Back.getFields()){
					if (field.isApplicable() && !field.isReadOnly()) {
						System.out.println(String.format("%s(%d) of %s", field.getName(), field.getId(),field.getDataType().getName()));
					}
				}
				back2Back.getField(EnumTransactionFieldId.Book).setValue(this.getClass().getSimpleName());
				System.out.println("FORM:" +back2Back.getField("Form").getDisplayString());
				System.out.println("LOCO:" +back2Back.getField("Loco").getDisplayString());
				
				back2Back.process(transaction.getTransactionStatus());
				
			} catch (Exception e) {
				e.printStackTrace();
				throw new Back2BackForwardException("Unable to process Future B2B:" + e.getMessage(), e);
			}
			//session.getDebug().viewTable(back2Back.asTable());
			String offsetDetails="";
		
			transaction.getField(properties.getProperty(TRANINFO_LINK)).setValue( back2Back.getDealTrackingId());

			transaction.saveInfoFields();
	}
	
	
	/**
	 * update existing <i>back to back</i> deal using data from supplied <i>future</i> deal
	 * <p>If the <i>back to back</i> is out of synch with the supplied <i>future</i> deal the update will <b>fail</b></p>
	 * @return 
	 */
	private Transaction amendBack2Back(Transaction future) {
		
		Field back2BackTransactionId = future.getField(properties.getProperty(TRANINFO_LINK));
		Transaction back2Back = session.getTradingFactory()
				.retrieveTransactionByDeal(Back2BackForwards.back2backTransaction(back2BackTransactionId));

		Table matchFutureForwards = getFutureForwardsMapping(future);
		
		populateBack2BackFromFuture(matchFutureForwards, 
				future, 
				(PriceLookup) future.getPricingDetails()
					.getMarket().getElement(EnumElementType.PriceLookup, PRICEINDEX), 
				back2Back);
		return back2Back;
	}
	
	public enum BACK2BACK_REFERENCE {
		TransactionId(0),
		Version(1), 
		OffsetTransactionId(2),
		OffsetVersion(3);
		
		
		private final int ordinal;
		private BACK2BACK_REFERENCE(int position) {
			ordinal = position;
		}
		
		public int getPosition() {
			return ordinal;
		}
	}
	
	/**
	 * Decipher {@value #LINKED_INFO} field 
	 */
	static int back2backTransaction(final Field back2BackTransactionId) {
		
		if (!back2BackTransactionId.isApplicable() 
				|| back2BackTransactionId.getDataType() != com.olf.openrisk.staticdata.EnumFieldType.Int) {
			throw new Back2BackForwardException("Data ", 
					B2B_TRANINFO_PROBLEM,
					String.format("TranInfo problem check configuration of %s", 
							back2BackTransactionId.getName()));
		}
		
		return back2BackTransactionId.getValueAsInt();
	}
	



	private Transaction createBack2Back(Transaction future) {
		
		Table matchFutureForwards = getFutureForwardsMapping(future);
		
		Transaction forward = getBack2BackTransaction();
		// Capture FX template TranInfo fields
		Field fld = forward.getField(EnumTransactionFieldId.TransactionInfoTable); 
		ConstTable tranInfoData = fld.getValueAsTable();
		
		populateBack2BackFromFuture(matchFutureForwards, 
				future, 
				(PriceLookup) future.getPricingDetails()
					.getMarket().getElement(EnumElementType.PriceLookup, PRICEINDEX), 
				forward);
		
		// Apply template TranInfo values to populated FX
		for(int tranInfoFields=0; tranInfoFields<tranInfoData.getRowCount(); tranInfoFields++) {
			
			    Field tranInfo = forward.getField(tranInfoData.getString("Type", tranInfoFields));
			    String currentValue = tranInfoData.getString("Value", tranInfoFields).trim();
			    if (0!=tranInfo.getName().compareToIgnoreCase(BACK2BACK_LOCATION) 
			    		&& 0!=tranInfo.getName().compareToIgnoreCase(BACK2BACK_OFFSET_LOCATION))
				switch (tranInfo.getDataType()) {
				
				case Int:
					if (currentValue.length()>0)
						forward.getField(tranInfo.getName()).setValue(Integer.parseInt(currentValue));
					break;
					
//				case Date: //TODO
//					toSet.getField(infoField.getName()).setValue(TODO);
//					break;
					
				case Double:
					if (currentValue.length()>0)
						forward.getField(tranInfo.getName()).setValue(Double.parseDouble(currentValue));
					break;
					
				case Long:
					if (currentValue.length()>0)
						forward.getField(tranInfo.getName()).setValue(Long.parseLong(currentValue));
					break;
					
				case String:
					if (currentValue.length()>0) {
						forward.getField(tranInfo.getName()).setValue(currentValue);
						Logging.info(String.format(" STR = %s", currentValue));
					}
					break;
					
				case Reference:
					if (currentValue.length()>0) {
						forward.getField(tranInfo.getName()).setValue(currentValue);;
						break;
					}
				default:
					Logging.info(String.format("Tran Field(%s):%s NOT copied(%s) from template! still %s",
							tranInfo.getDataType().getName(), tranInfo.getName(), currentValue,
							forward.getField(tranInfo.getName()).toString()));
					
				}
				
		}
		//session.getDebug().viewTable(forward.asTable());
		tranInfoData.dispose();
		fld.dispose();
		
		return forward;
	}

	/**
	 * Create transaction based on template with reference <b>{@value #TEMPLATE_SPOT}</b>
	 */
	private Transaction getBack2BackTransaction() {
		Table back2BackTemplate = DataAccess.getDataFromTable(session,
				String.format("SELECT * FROM %s WHERE %s=%d AND %s=%d AND %s='%s'", 
						"ab_tran",
						"toolset",
						EnumToolset.Fx.getValue(),
						"tran_status", 
						EnumTranStatus.Template.getValue(),
						"reference",
						properties.getProperty(FX_TEMPLATE)));

		if (null == back2BackTemplate || back2BackTemplate.getRowCount() != 1) {
			back2BackTemplate.dispose();
			throw new Back2BackForwardException("Configuration data", B2B_CONFIG,"Incorrect template for Back2Back");
		}
				
		return session.getTradingFactory().cloneTransaction(back2BackTemplate.getInt("tran_num", 0));
	}

	
	/**
	 * Update <i>back2Back</i> based on the supplied <i>future</i> where the <i>{@value #FUTURES_PROJECTION_INDEX}</i>
	 * in the user table({@value #FUTURE_2_BACK2BACK_MAPPING}) matches the future.
	 * Traverse the back to back deal applying appropriate data from <i>future</i> deal
	 * <br> get price differential via custom curve ({@value #PRICEINDEX})
	 * @see #getFutureForwardsMapping(Transaction)
	 */
	private void populateBack2BackFromFuture(final Table matchFutureForwards, 
			final Transaction future, 
			PriceLookup differentialCurve,
			Transaction forward) {
		
		if (null == differentialCurve )
			throw new Back2BackForwardException(
					"Configuration data",
					B2B_CONFIG,
					String.format("Unable to locate Price Differential curve(%s) on Tran#%d",
							PRICEINDEX, future.getTransactionId()));

		String targetCurrency = matchFutureForwards.getString(BACK2BACK_TICKER, 0);
		String ticker = targetCurrency.replaceAll("\\s", ""); //EPMM-1930
		ReferenceChoice ccy = forward.getField(EnumTransactionFieldId.Ticker).getChoices().findChoice(ticker);
		//ReferenceChoice ccy = forward.getField(EnumTransactionFieldId.CurrencyPair).getChoices().findChoice(targetCurrency);
		if (null == ccy 
			|| ccy.getName().compareToIgnoreCase(ticker)!=0) {			
			throw new Back2BackForwardException(
					"Configuration data",
					B2B_CONFIG,
					String.format("Tran#%d unable to create back to back no existing Ticker(%s)",
							future.getTransactionId(), targetCurrency));
		}


		Fields fields = forward.getFields();
		for (Field field : fields) { // walk the field entry applying values from future to forward
			int transactionField = field.getCoreId();

			if (transactionField >= 0 && !field.isUserDefined()
					&& field.getTranfId() != EnumTranfField.Action) {
				Logging.info(String.format(">>FIELD:%s", field.getTranfId()));
				switch (field.getTranfId()) {

				case BuySell:
//					field.setValue(future
//							.getValueAsInt(EnumTransactionFieldId.BuySell) == EnumBuySell.Buy
//							.getValue() ? EnumBuySell.Buy.getValue()
//							: EnumBuySell.Sell.getValue());
					field.setValue(future.getValueAsInt(EnumTransactionFieldId.BuySell));
					break;

				case FxDAmt: // determine value from Future contract lots
					
					field.setValue(/*(forward.getValueAsInt(EnumTransactionFieldId.BuySell) == EnumBuySell.Sell.getValue() ? -1 : 1) **/
							(future.getValueAsDouble(EnumTransactionFieldId.Position)
							* future.getInstrument().getLeg(0).getValueAsDouble(EnumLegFieldId.Notional)));
					break;

				case FxDate:
					// TODO lookup Curve SPOT...
//					field.setValue(String.format("%dd", differentialCurve
//							.getDefinitionField(EnumIndexFieldId.DaysDelayed)
//							.getValueAsInt()));
					field.setValue(future.getInstrument().getLeg(0).getValueAsDate(EnumLegFieldId.ExpirationDate));

					break;

// v1.3(02Jul15) populate price from Future trade in TranInfo
//				case FxSpotRate:
//				case Price: // use price adjustment from curve
//					break;

				case Position:
					// place holder to skip default processing
					break;

//				case InternalPortfolio:
//					field.setValue(future.getDisplayString(EnumTransactionFieldId.InternalPortfolio));
//
//					break;
					

				case ExternalBunit:
//					setForwardPartyFromTrader(
//							(Person) session.getStaticDataFactory().
//							getReferenceObject(EnumReferenceObject.Person,
//									future.getValueAsInt(EnumTransactionFieldId.InternalContact)),
//									EnumTransactionFieldId.ExternalBusinessUnit,
//									EnumTransactionFieldId.ExternalLegalEntity,
//									forward);
					field.setValue(future.getDisplayString(EnumTransactionFieldId.InternalBusinessUnit));
					forward.setValue(EnumTransactionFieldId.ExternalLegalEntity, future.getDisplayString(EnumTransactionFieldId.InternalLegalEntity));
					break;

				case CurrencyPair:
					if (0!=field.getValueAsString().compareToIgnoreCase(targetCurrency)) {
						if (0!=field.getValueAsString().compareToIgnoreCase("GBP/USD"))
							Logging.info(String.format("underlying metal changed from %s to %s",
									field.getValueAsString(), targetCurrency));
						forward.getField(EnumTransactionFieldId.Ticker).setValue(ccy.getName()); // EPMM-1930 ensure correct ticker active
						field.setValue(targetCurrency);
					}
					break;

					
				case InternalBunit:
					//field.setValue(future.getDisplayString(EnumTransactionFieldId.InternalBusinessUnit));
					setForwardPartyFromTrader(
							(Person) session.getStaticDataFactory().
							getReferenceObject(EnumReferenceObject.Person,
									future.getValueAsInt(EnumTransactionFieldId.InternalContact)),
									EnumTransactionFieldId.InternalBusinessUnit,
									EnumTransactionFieldId.InternalLegalEntity,
									forward);
					break;
					
				case FxTermSettleDate:
					int fxDate = forward.getField(EnumTransactionFieldId.FxDate).getValueAsInt();
					 try {
						int jdConvertDate = OCalendar.parseStringWithHolId(symbPymtDate,0,fxDate);
						String FxSettleDate = OCalendar.formatJd(jdConvertDate);
						field.setValue(FxSettleDate);
					} catch (OException e) {
							PluginLog.error("Unable to set USD settle Date");
					}
					 								
					break;
				

//				case InternalLentity:
//					field.setValue(future.getDisplayString(EnumTransactionFieldId.InternalLegalEntity));
//					break;

				default: // handle all the other fields
					Field originalField = future.getField(field.getTranfId()
							.toString());
					Logging.info(String.format("COPY Field:%s Type:%s %s", field.getName(), field.getDataType(),
							null != originalField ? "from " + originalField.getName() : "SKIP"));
					if (null != originalField && originalField.isApplicable()
							&& !originalField.isReadOnly())
						switch (field.getDataType()) {

						case String:
							field.setValue(originalField.getValueAsString());
							break;

						case Double:
							field.setValue(originalField.getValueAsDouble());
							break;

						case Date:
							field.setValue(originalField.getValueAsDate());
							break;

						case HolidayList:
							field.setValue(originalField
									.getValueAsHolidaySchedules());
							break;

						default:
							Logging.info(String.format("SKIPPING Field:%s Type:%s", field.getName(),
									field.getDataType().toString()));
						}
					break;
				}
			}
		}

		try {
			forward.setValue(EnumTransactionFieldId.ExternalPortfolio,
					future.getDisplayString(EnumTransactionFieldId.InternalPortfolio));
			forward.setValue(EnumTransactionFieldId.ExternalContact,
					future.getValueAsString(EnumTransactionFieldId.InternalContact));
			forward.setValue(EnumTransactionFieldId.InternalContact,
					future.getValueAsString(EnumTransactionFieldId.InternalContact));
			
			if (1 == forward.getChoices(EnumTransactionFieldId.InternalPortfolio).getCount() ) {
				// Only one option then use this one. 
				forward.getField(EnumTransactionFieldId.InternalPortfolio).setValue(forward.getChoices(EnumTransactionFieldId.InternalPortfolio).get(0).getId());
			} else {
				// If more that one option then  fine the metal option.
				
				// e.g. future trade is booked into UK Platinum  find an entry containing the 
				// word Platinum. Solution approved by Dennis, there will only be one protfolio
				// containing the metal name. 
				
				String futurePfolio = future.getValueAsString(EnumTransactionFieldId.InternalPortfolio).substring(3);
				
				boolean portfolioSet = false;
				for(ReferenceChoice portfolio:  forward.getChoices(EnumTransactionFieldId.InternalPortfolio) ) {
					if(portfolio.getName().contains(futurePfolio)) { 
						forward.getField(EnumTransactionFieldId.InternalPortfolio).setValue(portfolio.getName());
						portfolioSet = true;
						break;
					}
				}
				
				if(!portfolioSet)  {
					throw new Back2BackForwardException("Configuration", B2B_CONFIG, 
						String.format("Invalid(%d) portfolios in Group on Tran#%d",
						forward.getChoices(EnumTransactionFieldId.InternalPortfolio).getCount(),
						future.getTransactionId()));
				}
			}
			
			
			// up local tranInfo changes
			Field offsetLocation = validateTranInfoField(future, BACK2BACK_OFFSET_LOCATION,
				forward.getField(BACK2BACK_OFFSET_LOCATION));		
			offsetLocation.setValue(matchFutureForwards.getString(BACK2BACK_MAPPED_LOCATION, 0));
			Logging.info("\n\t OffsetLOC " + offsetLocation.getValueAsString());
			Field location = validateTranInfoField(future, BACK2BACK_LOCATION, 
					forward.getField(BACK2BACK_LOCATION));
			location.setValue(matchFutureForwards.getString(BACK2BACK_MAPPED_LOCATION, 0));
			Logging.info("\n\t LOCATION " + location.getValueAsString());
			

			Field form = validateTranInfoField(future, "Form", 
					forward.getField("Form"));
			form.setValue(matchFutureForwards.getString("form" , 0));

			Field originalDeal = validateTranInfoField(forward, properties.getProperty(TRANINFO_LINK), 
					forward.getField(properties.getProperty(TRANINFO_LINK)));
			originalDeal.setValue(future.getDealTrackingId());

			// v1.3(02Jul15) populate Future trade price into FX tran info...
			Field tradedPrice = validateTranInfoField(future, BACK2BACK_TRADE_PRICE, 
					forward.getField(BACK2BACK_TRADE_PRICE));
			tradedPrice.setValue(calculateFuturePriceDifferential(future, differentialCurve));

			//session.getDebug().viewTable(forward.asTable());
		} catch (Exception e) {
			throw new Back2BackForwardException("Error during create of B2B result:" + e.getMessage(), e);
		}
		
					
	}


	/**
	 * determine the trade price to apply to the forward(spot) deals based on the original future deal
	 * using the price differential curve({@value #PRICEINDEX})
	 * 
	 * @param future the original deal
	 * @param differentialCurve the custom curve {@value #PRICEINDEX}
	 * @return the new price
	 */
	private double calculateFuturePriceDifferential(final Transaction future, PriceLookup differentialCurve) {
		String contractCode = future.getInstrument().getLeg(0)
				.getField(EnumLegFieldId.ContractCode)
				.getValueAsString();
		GridPoints gridPoints = differentialCurve.getGridPoints();
		GridPoint differential = gridPoints.getGridPoint(contractCode);
		if (null == differential
				|| !differential.isActive()
				|| EnumGptCategory.Spot != differential.getCategory())
			throw new Back2BackForwardException(
					"Configuration data",
					B2B_CONFIG,
					String.format(
							"Unable to locate Price Differential curve(%s) with valid Grid Point for %s from Tran#%d",
							PRICEINDEX, contractCode,
							future.getTransactionId()));
		
		Logging.info(String.format("Original Price %f, differential price %f",
				future.getField(EnumTransactionFieldId.Price).getValueAsDouble(),
				differential.getValue(EnumGptField.EffInput, EnumBmo.Mid)));


//		// following is workaround for DTS128813
//		forward.setValue(
//				EnumTransactionFieldId.FxSpotRate,
//				Double.toString(future.getField(EnumTransactionFieldId.Price).getValueAsDouble()
//						- differential.getValue(EnumGptField.EffInput, EnumBmo.Mid)));
		//TODO investigate need to round based on target field...
		return future.getField(EnumTransactionFieldId.Price).getValueAsDouble()
				- differential.getValue(EnumGptField.EffInput, EnumBmo.Mid);
		 
	}


	private Field validateTranInfoField(final Transaction future,final String tranInfoField, Field location ) {
		if (null == location) {
			throw new Back2BackForwardException(
					"Configuration data",
					B2B_CONFIG,
					String.format("Tran#%d unable to create back to back MISSING (%s)",
							future.getTransactionId(), tranInfoField));
		}
		return location;
	}

	
	/**
	 * Obtain the mapping data matching the future deal 
	 * <br><i>The criteria is to lookup the user table {@value #FUTURE_2_BACK2BACK_MAPPING}
	 *  checking the {@value #FUTURES_PROJECTION_INDEX} 
	 * with the suppled transaction.</i> 
	 * <br>In the event of no match or more than a single match report a failure!
	 */
	private Table getFutureForwardsMapping(Transaction transaction) {
		
		String portfolio = transaction.getValueAsString(EnumTransactionFieldId.InternalPortfolio);
		Table mapping = DataAccess.getDataFromTable(session, 
				String.format("SELECT ut.* from %s ut " +
						" WHERE %s='%s' ",
						properties.getProperty(USER_TABLE),
						FUTURES_PROJECTION_INDEX, 
						transaction.getLeg(0).
							getDisplayString(EnumLegFieldId.ProjectionIndexFilter.getValue())
						));
		
		
		if (null == mapping || mapping.getRowCount() != 1) {
			mapping.dispose();
			throw new Back2BackForwardException("Configuration data", B2B_CONFIG,
					String.format("Incorrect mapping for portfolio(%s) on Tran#%d",
							portfolio, transaction.getTransactionId()));
		}
		
		return mapping;
	}
	private void init() throws Exception {
		constRep = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
		symbPymtDate = constRep.getStringValue("SymbolicPymtDate", "1wed > 1sun");
	}


}