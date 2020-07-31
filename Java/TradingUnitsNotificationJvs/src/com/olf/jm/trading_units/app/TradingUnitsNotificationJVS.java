package com.olf.jm.trading_units.app;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.openlink.util.constrepository.ConstRepository;
import  com.olf.jm.logging.Logging;
import com.openlink.util.misc.TableUtilities;
/*
 * HISTORY
 * 1.0 - 2015-08-04 - jwaechter - initial version
 * 1.1 - 2015-10-29	- jwaechter	- switched from and to unit in retrieval of 
 * 								  unit conversion factor
 * 1.2 - 2016-01-20	- jwaechter	- added special logic for pure non metal forwards and spot deals
 * 1.3 - 2016-02-18 - jwaechter - added logic to check if field is applicable TRANF_FX_DEALT_RATE
 * 1.4 - 2016-09-13	- jwaechter	- added logic to prevent setting values in case no trade price had
 *                                been entered by the user.
 * 1.5 - 2016-09-14 - jwaechter	- changed precision of rounding
 */
/**
 * 
 * @author jwaechter
 * @version 1.5
 */
public class TradingUnitsNotificationJVS implements IScript{
	public static final String CREPO_CONTEXT = "FrontOffice";
	public static final String CREPO_SUBCONTEXT = "TradingUnitsNotification";

	
	public static final String TRADE_FX_TERM_UNIT_FIELD_NAME 	 = "Fx Term Currency Unit";
	public static final String TRADE_FX_BASE_CCY_UNIT_FIELD_NAME = "Fx Base Currency Unit";
	public static final String TRADE_FX_FAR_TERM_UNIT_FIELD_NAME = "Fx Far Term Unit";
	public static final String TRADE_FX_FAR_BASE_UNIT_FIELD_NAME = "Fx Far Base Unit";
	public static final String TRADE_PRICE_INFO_FIELD_NAME 		 = "Trade Price";
	public static final String TRADE_FX_FAR_PTS_FIELD_NAME 		 = "Fx Far Points";
	public static final String TRADE_FX_FWD_PTS_FIELD_NAME 		 = "FX Forward Points";
	public static final String TRADE_FX_DATE					 = "FX Date";
	public static final String TRADE_FX_FAR_DATE				 = "FX Far Date";
	public static final String TRADE_FX_SPOT_RATE				 = "FX Spot Rate";
	public static final String TRADE_FX_FAR_SPOT_RATE			 = "FX Far Spot Rate";
	
	public static final String UNIT_OUNCE_TROY					 = "TOz";
	public static final String QUALITY_SWAP 					 = "Quality Swap";
	
	private static final int FMT_PREC = 8;
	private static final int FMT_WIDTH = 12;

	@Override
	public void execute(IContainerContext context) throws OException {
		// TODO Auto-generated method stub
		OConsole.oprint ("\n\n\n\n\n\n\n\n " + this.getClass().getSimpleName() + " STARTED.");
		try {
			Table argt = context.getArgumentsTable();	
			initLogging();

			Transaction tran 				= argt.getTran("tran", 1);
			int changedFieldId 				= argt.getInt("field_id", 1);
			int side 						= argt.getInt("side", 1);
//			int seqNum2 					= argt.getInt("seq_num2", 1);
			String newValue 				= argt.getString("new_value", 1);			
			String oldValue 				= argt.getString("old_value", 1);
			
			if (oldValue.equals(newValue)) {
				return;
			}
			
			String cflowType = tran.getField(TRANF_FIELD.TRANF_CFLOW_TYPE.toInt());
			int baseCurrencyId = tran.getFieldInt(TRANF_FIELD.TRANF_BASE_CURRENCY.toInt());
			int bougthCurrencyId = tran.getFieldInt(TRANF_FIELD.TRANF_BOUGHT_CURRENCY.toInt());
			boolean bothNonMetalCurrencies = areBothCurrenciesNonMetal (baseCurrencyId, bougthCurrencyId);
			
			double tradePrice=0.0;
			String tradePriceAsString = "nothing";
			String tradeUnit="";
			String tradeUnitFar="";
			double tradePriceFar=0.0;
			String tradePriceFarAsString = "nothing";
			
			double fwdPts=0.0;
			
			boolean setNear=false;
			boolean setFar=false;
			boolean clearTradePrice=false;
			
			if (bothNonMetalCurrencies) {
				tradeUnit = UNIT_OUNCE_TROY;
				tradeUnitFar = UNIT_OUNCE_TROY;
				tradePrice = tran.getFieldDouble(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 0, TRADE_PRICE_INFO_FIELD_NAME);
				tradePriceAsString = tran.getField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 0, TRADE_PRICE_INFO_FIELD_NAME);
				
				if (changedFieldId == TRANF_FIELD.TRANF_CFLOW_TYPE.toInt()) {
					cflowType = Ref.getName(SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE, Integer.parseInt(newValue));
					clearTradePrice = true;
				}
				if (changedFieldId == getTradePriceInfoId ()) {
					try {
						tradePrice = Double.parseDouble(newValue);
						tradePriceAsString = newValue;
					} catch (NumberFormatException ex) {
						tradePrice = tran.getFieldDouble(TRANF_FIELD.TRANF_FX_SPOT_RATE.toInt());
					}					
				}
				if (cflowType.contains("Swap")) {
					if (changedFieldId == TRANF_FIELD.TRANF_FX_FAR_BASE_UNIT.toInt() ||
						changedFieldId == TRANF_FIELD.TRANF_FX_FAR_TERM_UNIT.toInt() ||
						changedFieldId == TRANF_FIELD.TRANF_FX_FAR_PTS.toInt() ||
						changedFieldId == TRANF_FIELD.TRANF_FX_FAR_DATE.toInt() ||
						changedFieldId == TRANF_FIELD.TRANF_FX_FAR_SPOT_RATE.toInt()) {
						tradePriceFar = tran.getFieldDouble(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1, TRADE_PRICE_INFO_FIELD_NAME);
						tradePriceFarAsString = tran.getField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1, TRADE_PRICE_INFO_FIELD_NAME);
						setFar = true;
					} else {
						if (changedFieldId == getTradePriceInfoId ()) {
							setNear = side==0;
							setFar = side==1;
							if (setFar) {
								tradePriceFar = Double.parseDouble(newValue);
								tradePriceFarAsString = newValue;
							}
						} else {
							setNear = true;						
						}
					}
				} else {
					setNear = true;
				}
			} else if (changedFieldId == TRANF_FIELD.TRANF_FX_TERM_CCY_UNIT.toInt()
				|| changedFieldId == TRANF_FIELD.TRANF_FX_BASE_CCY_UNIT.toInt()) {
				int tradeUnitId = Integer.parseInt(newValue);
				tradeUnit = Ref.getName(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, tradeUnitId);
				
				tradePrice = tran.getFieldDouble(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 0, TRADE_PRICE_INFO_FIELD_NAME);
				tradePriceAsString = tran.getField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 0, TRADE_PRICE_INFO_FIELD_NAME);
				fwdPts = tran.getFieldDouble(TRANF_FIELD.TRANF_FX_FAR_PTS.toInt());
				setNear = true;
			} else if (changedFieldId == TRANF_FIELD.TRANF_FX_FAR_BASE_UNIT.toInt()
					|| changedFieldId == TRANF_FIELD.TRANF_FX_FAR_TERM_UNIT.toInt()) {
				int tradeUnitId = Integer.parseInt(newValue);
				tradeUnitFar = Ref.getName(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, tradeUnitId);
				tradePriceFar = tran.getFieldDouble(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1, TRADE_PRICE_INFO_FIELD_NAME);
				tradePriceFarAsString = tran.getField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1, TRADE_PRICE_INFO_FIELD_NAME);
				fwdPts = tran.getFieldDouble(TRANF_FIELD.TRANF_FX_FAR_PTS.toInt());
				setFar = true;
			} else if (changedFieldId == TRANF_FIELD.TRANF_FX_FWD_PTS.toInt()) {
				fwdPts = Double.parseDouble(newValue);
				tradePrice = tran.getFieldDouble(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 0, TRADE_PRICE_INFO_FIELD_NAME);
				tradePriceAsString = tran.getField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 0, TRADE_PRICE_INFO_FIELD_NAME);
				tradeUnit = tran.getField(TRANF_FIELD.TRANF_FX_TERM_CCY_UNIT.toInt());
				if (tradeUnit.equalsIgnoreCase("Currency")) {
					tradeUnit = tran.getField(TRANF_FIELD.TRANF_FX_BASE_CCY_UNIT.toInt());
				}
				if (tradeUnit.equalsIgnoreCase("Currency")) {
					throw new OException ("Transaction# " + tran.getTranNum() + 
							"  is currency only and can't be processed by " + 
							this.getClass().getCanonicalName());
				}
				setNear = true;
			} else if (changedFieldId == TRANF_FIELD.TRANF_FX_FAR_PTS.toInt()) {
				fwdPts = Double.parseDouble(newValue);
				tradePriceFar = tran.getFieldDouble(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1, TRADE_PRICE_INFO_FIELD_NAME);
				tradePriceFarAsString = tran.getField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1, TRADE_PRICE_INFO_FIELD_NAME);
				tradeUnitFar = tran.getField(TRANF_FIELD.TRANF_FX_FAR_TERM_UNIT.toInt());
				if (tradeUnitFar.equalsIgnoreCase("Currency")) {
					tradeUnitFar = tran.getField(TRANF_FIELD.TRANF_FX_FAR_BASE_UNIT.toInt());
				}
				if (tradeUnitFar.equalsIgnoreCase("Currency")) {
					throw new OException ("Transaction# " + tran.getTranNum() + 
							"  is currency only and can't be processed by " + 
							this.getClass().getCanonicalName());
				}
				setFar = true;
			} else if (changedFieldId == getTradePriceInfoId ()) {
				setNear = side==0;
				setFar = side==1;
				if (setNear) {
					try {
						tradePrice = Double.parseDouble(newValue);
						tradePriceAsString = newValue;
					} catch (NumberFormatException ex) {
						tradePrice = tran.getFieldDouble(TRANF_FIELD.TRANF_FX_SPOT_RATE.toInt());
					}
					fwdPts = tran.getFieldDouble(TRANF_FIELD.TRANF_FX_FWD_PTS.toInt());
					tradeUnit = tran.getField(TRANF_FIELD.TRANF_FX_TERM_CCY_UNIT.toInt());
					if (tradeUnit.equalsIgnoreCase("Currency")) {
						tradeUnit = tran.getField(TRANF_FIELD.TRANF_FX_BASE_CCY_UNIT.toInt());
					}
					if (tradeUnit.equalsIgnoreCase("Currency")) {
						throw new OException ("Transaction# " + tran.getTranNum() + 
								"  is currency only and can't be processed by " + 
								this.getClass().getCanonicalName());
					}
				} else if (setFar) {
					try {
						tradePriceFar = Double.parseDouble(newValue);
						tradePriceFarAsString = newValue;
					} catch (NumberFormatException ex) {
						tradePriceFar = tran.getFieldDouble(TRANF_FIELD.TRANF_FX_FAR_SPOT_RATE.toInt());
					}
					fwdPts = tran.getFieldDouble(TRANF_FIELD.TRANF_FX_FAR_PTS.toInt());
					tradeUnitFar = tran.getField(TRANF_FIELD.TRANF_FX_FAR_TERM_UNIT.toInt());
					if (tradeUnitFar.equalsIgnoreCase("Currency")) {
						tradeUnitFar = tran.getField(TRANF_FIELD.TRANF_FX_FAR_BASE_UNIT.toInt());
					}
					if (tradeUnitFar.equalsIgnoreCase("Currency")) {
						throw new OException ("Transaction# " + tran.getTranNum() + 
								"  is currency only and can't be processed by " + 
								this.getClass().getCanonicalName());
					}
				}				
			} else if (changedFieldId == TRANF_FIELD.TRANF_FX_DATE.toInt()) {
				tradePrice = tran.getFieldDouble(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 0, TRADE_PRICE_INFO_FIELD_NAME);
				tradePriceAsString = tran.getField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 0, TRADE_PRICE_INFO_FIELD_NAME);
				if (tradePrice == 0.0d) {
					tradePrice = tran.getFieldDouble(TRANF_FIELD.TRANF_FX_SPOT_RATE.toInt());
				}
				tradeUnit = tran.getField(TRANF_FIELD.TRANF_FX_TERM_CCY_UNIT.toInt());
				if (tradeUnit.equalsIgnoreCase("Currency")) {
					tradeUnit = tran.getField(TRANF_FIELD.TRANF_FX_BASE_CCY_UNIT.toInt());
				}
				if (tradeUnit.equalsIgnoreCase("Currency")) {
					throw new OException ("Transaction# " + tran.getTranNum() + 
							"  is currency only and can't be processed by " + 
							this.getClass().getCanonicalName());
				}
				setNear = true;				
			} else if (changedFieldId == TRANF_FIELD.TRANF_FX_FAR_DATE.toInt()) {
				tradePriceFar = tran.getFieldDouble(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1, TRADE_PRICE_INFO_FIELD_NAME);
				tradePriceFarAsString = tran.getField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1, TRADE_PRICE_INFO_FIELD_NAME);
				if (tradePrice == 0.0d) {
					tradePrice = tran.getFieldDouble(TRANF_FIELD.TRANF_FX_FAR_SPOT_RATE.toInt());
				}
				tradeUnitFar = tran.getField(TRANF_FIELD.TRANF_FX_FAR_TERM_UNIT.toInt());
				if (tradeUnitFar.equalsIgnoreCase("Currency")) {
					tradeUnitFar = tran.getField(TRANF_FIELD.TRANF_FX_FAR_BASE_UNIT.toInt());
				}
				if (tradeUnitFar.equalsIgnoreCase("Currency")) {
					throw new OException ("Transaction# " + tran.getTranNum() + 
							"  is currency only and can't be processed by " + 
							this.getClass().getCanonicalName());
				}
				setFar = true;
			} else if (changedFieldId == TRANF_FIELD.TRANF_CFLOW_TYPE.toInt()) {
				cflowType = Ref.getName(SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE, Integer.parseInt(newValue));
				clearTradePrice = true;
			}
			
			double conversionFactor;
			double conversionFactorFar;
			
			setNear &= tradePriceAsString != null && !tradePriceAsString.equals("nothing") && tradePriceAsString.trim().length() > 0;
			setFar &= tradePriceFarAsString != null && !tradePriceFarAsString.equals("nothing") && tradePriceFarAsString.trim().length() > 0;
			
			switch (cflowType) {
			case "Spot":
				if (setNear) {
					conversionFactor = getConversionFactor (tradeUnit, UNIT_OUNCE_TROY);
					String valueToSet = Str.formatAsDouble(conversionFactor*tradePrice, FMT_WIDTH, FMT_PREC);					
					if (changedFieldId != TRANF_FIELD.TRANF_FX_SPOT_RATE.toInt()) {
						tran.setField(TRANF_FIELD.TRANF_FX_SPOT_RATE.toInt(), 
								0, "", valueToSet);
					}
				}
				if (clearTradePrice) {
					tran.setField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 0, TRADE_PRICE_INFO_FIELD_NAME, "");
				}
				break;
			case "Forward":
				if (setNear) {
					conversionFactor = getConversionFactor (tradeUnit, UNIT_OUNCE_TROY);
//					tran.setField(TRANF_FIELD.TRANF_FX_SPOT_RATE.toInt(), 
//							side, "", Str.formatAsDouble(conversionFactor*tradePrice, FMT_WIDTH, FMT_PREC));
					if (changedFieldId != TRANF_FIELD.TRANF_FX_DEALT_RATE.toInt() && tran.isFieldNotAppl(TRANF_FIELD.TRANF_FX_DEALT_RATE, 0, "") == 0) {
						tran.setField(TRANF_FIELD.TRANF_FX_DEALT_RATE.toInt(), 
								0, "", Str.formatAsDouble(conversionFactor*tradePrice, FMT_WIDTH, FMT_PREC));						
					}
				} 
				if (clearTradePrice) {
					tran.setField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 0, TRADE_PRICE_INFO_FIELD_NAME, "");
				}
				break;
			case "Swap":
				if (setNear) {
					conversionFactor = getConversionFactor (tradeUnit, UNIT_OUNCE_TROY);
//					tran.setField(TRANF_FIELD.TRANF_FX_SPOT_RATE.toInt(), 
//							side, "", Str.formatAsDouble(conversionFactor*tradePrice, FMT_WIDTH, FMT_PREC));
					if (changedFieldId != TRANF_FIELD.TRANF_FX_DEALT_RATE.toInt()) {
						tran.setField(TRANF_FIELD.TRANF_FX_DEALT_RATE.toInt(), 
								0, "", Str.formatAsDouble(conversionFactor*tradePrice, FMT_WIDTH, FMT_PREC));						
					}
				} 
				if (setFar) {	
					conversionFactorFar = getConversionFactor (tradeUnitFar, UNIT_OUNCE_TROY);
					if (changedFieldId != TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE.toInt()) {
						tran.setField(TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE.toInt(), 
								0, "", Str.formatAsDouble(conversionFactorFar*tradePriceFar, FMT_WIDTH, FMT_PREC));						
					}
				}
				if (clearTradePrice) {
					tran.setField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 0, TRADE_PRICE_INFO_FIELD_NAME, "");
					tran.setField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1, TRADE_PRICE_INFO_FIELD_NAME, "");
				}
				break;
			case "Location Swap":
				if (setNear) {
					conversionFactor = getConversionFactor (tradeUnit, UNIT_OUNCE_TROY);
//					tran.setField(TRANF_FIELD.TRANF_FX_SPOT_RATE.toInt(), 
//							side, "", Str.formatAsDouble(conversionFactor*tradePrice, FMT_WIDTH, FMT_PREC));
					if (changedFieldId != TRANF_FIELD.TRANF_FX_DEALT_RATE.toInt()) {
						tran.setField(TRANF_FIELD.TRANF_FX_DEALT_RATE.toInt(), 
								0, "", Str.formatAsDouble(conversionFactor*tradePrice, FMT_WIDTH, FMT_PREC));						
					}
				} 
				if (setFar) {
					conversionFactorFar = getConversionFactor (tradeUnitFar, UNIT_OUNCE_TROY);
//					tran.setField(TRANF_FIELD.TRANF_FX_FAR_SPOT_RATE.toInt(), 
//							side, "", Str.formatAsDouble(conversionFactorFar*tradePriceFar	, FMT_WIDTH, FMT_PREC));
					if (changedFieldId != TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE.toInt()) {
						tran.setField(TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE.toInt(), 
								0, "", Str.formatAsDouble(conversionFactorFar*tradePriceFar, FMT_WIDTH, FMT_PREC));						
					}
				}
				if (clearTradePrice) {
					tran.setField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 0, TRADE_PRICE_INFO_FIELD_NAME, "");
					tran.setField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1, TRADE_PRICE_INFO_FIELD_NAME, "");
				}
				break;
			case QUALITY_SWAP:
				if (setNear) {
					conversionFactor = getConversionFactor (tradeUnit, UNIT_OUNCE_TROY);
//					tran.setField(TRANF_FIELD.TRANF_FX_SPOT_RATE.toInt(), 
//							side, "", Str.formatAsDouble(conversionFactor*tradePrice, FMT_WIDTH, FMT_PREC));
					if (changedFieldId != TRANF_FIELD.TRANF_FX_DEALT_RATE.toInt()) {
						tran.setField(TRANF_FIELD.TRANF_FX_DEALT_RATE.toInt(), 
								0, "", Str.formatAsDouble(conversionFactor*tradePrice, FMT_WIDTH, FMT_PREC));						
					}
				} 
				if (setFar) {
					conversionFactorFar = getConversionFactor (tradeUnitFar, UNIT_OUNCE_TROY);
//					tran.setField(TRANF_FIELD.TRANF_FX_FAR_SPOT_RATE.toInt(), 
//							side, "", Str.formatAsDouble(conversionFactorFar*tradePriceFar	, FMT_WIDTH, FMT_PREC));
					if (changedFieldId != TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE.toInt()) {
						tran.setField(TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE.toInt(), 
								0, "", Str.formatAsDouble(conversionFactorFar*tradePriceFar, FMT_WIDTH, FMT_PREC));						
					}
				}
				if (clearTradePrice) {
					tran.setField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 0, TRADE_PRICE_INFO_FIELD_NAME, "");
					tran.setField(TRANF_FIELD.TRANF_AUX_TRAN_INFO.toInt(), 1, TRADE_PRICE_INFO_FIELD_NAME, "");
				}
				break;
			default:
				throw new IllegalArgumentException ("Plugin " + this.getClass().getCanonicalName() + " is not setup to process "
						+ " deals having Cash Flow Type :" + cflowType + " ");
			}
		} catch (OException e) {
			String errorMessage = "Failed."
					+ e.getMessage();
			Logging.error(errorMessage);
			return;
		}finally{
			Logging.info("\n" + this.getClass().getSimpleName() + " finished successfully.");
			Logging.close();
		}
		
	}
	
	private boolean areBothCurrenciesNonMetal(int baseCurrencyId,
			int bougthCurrencyId) throws OException {
		String sql =
				"SELECT id_number, name FROM currency WHERE precious_metal = 0 "
				+ " AND id_number IN (" + baseCurrencyId + "," + bougthCurrencyId + ")";
		Table sqlResult = Table.tableNew("sql_result");
		int ret = DBaseTable.execISql(sqlResult, sql);
		if (sqlResult.getNumRows() == 2) {
			return true;
		}
		return false;
	}

	private int getTradePriceInfoId() throws OException {
		String sql = 
				"\nSELECT tit.type_id"
			+ 	"\nFROM tran_info_types tit"
			+   "\nWHERE tit.type_name = '" + TRADE_PRICE_INFO_FIELD_NAME + "'"
			;
		Table titTable = null;
		try {
			titTable = Table.tableNew("Type ID of " + TRADE_PRICE_INFO_FIELD_NAME);
			int ret = DBaseTable.execISql(titTable, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				throw new OException ("Error executing SQL " + sql);
			}
			if (titTable.getNumRows() != 1) {
				throw new IllegalArgumentException ("Transaction Info Type '" + TRADE_PRICE_INFO_FIELD_NAME + "' is not defined");
			}
			return titTable.getInt("type_id", 1);
		} finally {
			titTable = TableUtilities.destroy(titTable);
		}
	}

	private double getConversionFactor(String toUnit, String fromUnit) throws OException {
		if  (fromUnit.equalsIgnoreCase(toUnit)) {
			return 1.0;
		}
		String sql = 
				"\nSELECT uc.factor"
			+ 	"\nFROM unit_conversion uc"
			+	"\nINNER JOIN idx_unit src"
			+   "\nON src.unit_label = '" + fromUnit + "'"
			+   "\n  AND src.unit_id = uc.src_unit_id"
			+   "\nINNER JOIN idx_unit dest"
			+   "\n  ON dest.unit_label = '" + toUnit + "'"
			+   "\n  AND dest.unit_id = uc.dest_unit_id"
			;
		Table factorTable = null;
		try {
			factorTable = Table.tableNew("conversion factor from " + fromUnit + " to " + toUnit);
			int ret = DBaseTable.execISql(factorTable, sql);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				throw new OException ("Error executing SQL " + sql);
			}
			if (factorTable.getNumRows() != 1) {
				throw new IllegalArgumentException ("There is no unit conversion factor defined from " 
						+ fromUnit + " to " + toUnit);
			}
			return factorTable.getDouble("factor", 1);
		} finally {
			factorTable = TableUtilities.destroy(factorTable);
		}
	}

	/**
	 * Initialise logging module.
	 * 
	 * @throws OException
	 */
	private void initLogging() throws OException {
		// Constants Repository Statics
		ConstRepository constRep = new ConstRepository(CREPO_CONTEXT,
				CREPO_SUBCONTEXT);
		String logLevel = constRep.getStringValue("logLevel", "info");
		String logFile = constRep.getStringValue("logFile", this.getClass()
				.getSimpleName()
				+ ".log");
		String logDir = constRep.getStringValue("logDir", "");

		try {
			
			Logging.init(this.getClass(), CREPO_CONTEXT, CREPO_SUBCONTEXT);
		} catch (Exception e) {
			String errMsg = this.getClass().getSimpleName()
					+ ": Failed to initialize logging module.";
			Util.exitFail(errMsg);
		}
	}
}
