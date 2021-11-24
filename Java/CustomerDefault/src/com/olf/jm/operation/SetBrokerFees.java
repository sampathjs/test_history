package com.olf.jm.operation;

import com.olf.openjvs.*;
import com.olf.openjvs.Math;
import com.olf.openjvs.enums.*;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.jm.logging.Logging;

/**
 * 
 * When a deal is booked this script will set the Broker Fee Amt
 * 
 * Revision History:
 * Version        Updated By            Date        Ticket#            Description
 * -----------------------------------------------------------------------------------
 *     01            Ivan Fernandes        30-Apr-2021                    Initial version
 */


@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class SetBrokerFees extends AbstractTradeProcessListener {
    

    /** The const repository used to initialise the logging classes. */
    private ConstRepository constRep;
    
    /** The Constant CONTEXT used to identify entries in the const repository. */
    public static final String CONTEXT = "Broker Fee";
    
    /** The Constant SUBCONTEXT used to identify entries in the const repository.. */
    public static final String SUBCONTEXT = "Trading";

    @Override
    public void postProcess(Session session, DealInfo<EnumTranStatus> deals, boolean succeeded, Table clientData) {
    
        try {

            Logging.init(this.getClass(), CONTEXT, SUBCONTEXT);
            Logging.info("Start SetBrokerFees ");

            for (int intTranNum : deals.getTransactionIds()) {

                Logging.info("Processing tran " + intTranNum);

                com.olf.openjvs.Transaction tranPtr = com.olf.openjvs.Transaction.retrieve(intTranNum);

                // retrieve broker, base ccy, efp_dealnum
                int intBrokerId = tranPtr.getFieldInt(TRANF_FIELD.TRANF_BROKER_ID, 0);
                String strBroker = tranPtr.getField(TRANF_FIELD.TRANF_BROKER_ID, 0);

                if (intBrokerId > 0) {

                    Logging.info("Broker " + strBroker + " was selected on the deal. ");

                    String strBaseCcy = "";
                    String strCflowType = "";
                    int intInsType = tranPtr.getInsType();
                    if (intInsType == Ref.getValue(SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE, "PREC-EXCH-FUT")) {

                        strBaseCcy = tranPtr.getField(TRANF_FIELD.TRANF_TRAN_CURRENCY);

                        String strIndex = tranPtr.getField(TRANF_FIELD.TRANF_PROJ_INDEX);
                        int intIdxSubGroup = Index.getSubgroupByName(strIndex);

                        String strSQL = "select code from idx_subgroup where id_number = " + intIdxSubGroup;
                        com.olf.openjvs.Table tblCode = com.olf.openjvs.Table.tableNew();
                        DBaseTable.execISql(tblCode, strSQL);

                        strBaseCcy = tblCode.getString("code", 1);

                        tblCode.destroy();
                    } else {
                        strBaseCcy = tranPtr.getField(TRANF_FIELD.TRANF_TRAN_CURRENCY);
                        strCflowType = tranPtr.getField(TRANF_FIELD.TRANF_CFLOW_TYPE);
                    }

                    String strIsEFP = tranPtr.getField(TRANF_FIELD.TRANF_TRAN_INFO, 0, "IsEFP");

                    if (strIsEFP != null && !strIsEFP.isEmpty() && strIsEFP.equals("Yes")
                            && intInsType != Ref.getValue(SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE, "PREC-EXCH-FUT")) {
                        Logging.info("Found a EFP FX deal skipping");
                        continue;
                    }

                    if (strIsEFP != null && !strIsEFP.isEmpty() && strIsEFP.equals("Yes")
                            && intInsType == Ref.getValue(SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE, "PREC-EXCH-FUT")) {
                        strCflowType = "EFP";
                    }

                    String strSettleDate = tranPtr.getField(TRANF_FIELD.TRANF_SETTLE_DATE);
                    String strTradeDate = tranPtr.getField(TRANF_FIELD.TRANF_TRADE_DATE);

                    int intSettleDate = OCalendar.parseString(strSettleDate);
                    int intTradeDate = OCalendar.parseString(strTradeDate);

                    int intDurationDays = Math.abs(intSettleDate - intTradeDate);
                    int intDurationMonths = intDurationDays / 12;

                    String strSql;
                    strSql = "SELECT * ";
                    strSql += "FROM ";
                    strSql += "USER_broker_rates ";
                    strSql += "WHERE ";
                    strSql += "external_bunit = '" + strBroker + "' ";
                    strSql += "AND metal = '" + strBaseCcy + "' ";
                    strSql += "AND trade_type = '" + strCflowType + "' ";
                    strSql += "AND " + intDurationMonths + ">= tenor_start_month ";
                    strSql += "AND " + intDurationMonths + " < tenor_end_month  ";
                    Logging.info("Retrieving rule with sql " + strSql);

                    com.olf.openjvs.Table tblUserBrokerInfo = com.olf.openjvs.Table.tableNew();
                    DBaseTable.execISql(tblUserBrokerInfo, strSql);

                    if (tblUserBrokerInfo.getNumRows() == 1) {

                        double dblRate = tblUserBrokerInfo.getDouble("rate", 1);
                        String strUnit = tblUserBrokerInfo.getString("unit", 1);
                        
                        // If unit = ‘bps’, then fee amount = trade_price*quantity*rate/10000
                        // If unit = ‘lot’, then fee amount = quantity in lots*rate
                        // If unit = ‘Toz’ then fee amount = quantity in Toz & rate

                        double dblFeeAmt = 0.0;
                        double dblTradePrice = 0.0;
                        double dblQty = 0.0;
                        String strBoughtCcy = "";

                        strBoughtCcy = tranPtr.getField(TRANF_FIELD.TRANF_BOUGHT_CURRENCY);

                        if (strUnit.equals("notional")) {

                            dblQty = tranPtr.getFieldDouble(TRANF_FIELD.TRANF_FX_D_AMT.toInt(), 0);
                            dblTradePrice = tranPtr.getFieldDouble(TRANF_FIELD.TRANF_FX_DEALT_RATE.toInt(), 0);
                            if (strBoughtCcy != null && !strBoughtCcy.isEmpty() && !strBoughtCcy.equals("USD")) {

                                int intDealNum = tranPtr.getFieldInt(TRANF_FIELD.TRANF_DEAL_TRACKING_NUM);
                                double dblFxRate = getFXRate(session, intDealNum, strBoughtCcy);
                                dblTradePrice = dblFxRate * dblTradePrice;
                                Logging.info("dblTradePrice (" + strBoughtCcy + "/" + strBaseCcy + ")" + dblTradePrice
                                        + "= fx_rate ( USD/" + strBoughtCcy + ") " + dblFxRate + " * dblTradePrice "
                                        + dblTradePrice / dblFxRate + " (" + strBoughtCcy + "/" + strBaseCcy + ")");
                            }

                            dblFeeAmt = dblTradePrice * dblQty * dblRate;
                            Logging.info("Notional : Fee Amt = TradePrice * Qty * Rate : " + dblFeeAmt + " = "
                                    + dblTradePrice + " * " + dblQty + " * " + dblRate);

                        } else if (strUnit.equals("lot")) {

                            dblQty = tranPtr.getFieldInt(TRANF_FIELD.TRANF_POSITION.toInt(), 0);
                            dblFeeAmt = dblQty * dblRate;
                            Logging.info("Lot: Fee Amt = Qty * Rate : " + dblFeeAmt + " = " + dblQty + " * " + dblRate);

                        } else if (strUnit.equals("ounce")) {

                            dblQty = tranPtr.getFieldDouble(TRANF_FIELD.TRANF_FX_D_AMT.toInt(), 0);
                            dblFeeAmt = dblQty * dblRate;
                            Logging.info("Ounce: Fee Amt = Qty * Rate : " + dblFeeAmt + " = " + dblQty + " * " + dblRate);
                        }
                        
                        com.olf.openjvs.Table tblBrokerFee = tranPtr.getBrokerFeeTable();
                        for (int i = 1; i <= tblBrokerFee.getNumRows(); i++) {

                            if (intInsType == Ref.getValue(SHM_USR_TABLES_ENUM.INSTRUMENTS_TABLE, "PREC-EXCH-FUT")) {

                                tblBrokerFee.setDouble("allocated_amt", i, dblFeeAmt);
                                tblBrokerFee.setDouble("reserved_amt", i, 0.0d);

                                int intProvType = tblBrokerFee.getInt("prov_type", i);
                                if (intProvType == Ref.getValue(SHM_USR_TABLES_ENUM.PROV_TYPE_TABLE, "Execution")) {
                                    tranPtr.setField(TRANF_FIELD.TRANF_BROKER_FEE.toInt(), 0, "", "" + dblFeeAmt, intBrokerId);
                                    saveBrokerFeesTable(tblBrokerFee, tranPtr);
                                }
                            } else {

                                tblBrokerFee.setDouble("allocated_amt", i, dblFeeAmt);
                                tblBrokerFee.setDouble("reserved_amt", i, 0.0d);

                                int intProvType = tblBrokerFee.getInt("prov_type", i);
                                if (intProvType == Ref.getValue(SHM_USR_TABLES_ENUM.PROV_TYPE_TABLE, "OTC")) {
                                    tranPtr.setField(TRANF_FIELD.TRANF_BROKER_FEE.toInt(), 0, "", "" + dblFeeAmt, intBrokerId);
                                    saveBrokerFeesTable(tblBrokerFee, tranPtr);
                                }
                            }
                        }

                    } else {
                        Logging.info("No rows found in the user table for tran " + intTranNum);
                    }

                    tblUserBrokerInfo.destroy();
                } else {
                    Logging.info("No broker was selected on the tran " + intTranNum);
                }
            }

            Logging.info("End SetBrokerFees");

        } catch (Exception e) {

            Logging.info(e.toString());
        }

    }
    
    public double getFXRate(Session session, int intDealNum, String strBoughtCcy) throws OException {

        String sql = "select spot_rate from USER_jm_pnl_market_data where deal_num = " + intDealNum
                + " and metal_ccy = " + Ref.getValue(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, strBoughtCcy);

        double dblFXRate = 1;
        try (Table result = session.getIOFactory().runSQL(sql);) {
            dblFXRate = result.getDouble(0, 0);
        }

        Logging.debug("fx rate from the data base " + dblFXRate);
        return dblFXRate;

    }
    

    public double getUnitConversionRate(Session session, String strSrcUnit, String strDestUnit) throws OException {

    	String sql = "SELECT conv.factor \n"  + 
				 " FROM   unit_conversion conv  \n" +
				 " WHERE  conv.src_unit_id = (\n" + 
				 "   SELECT unit_id FROM idx_unit WHERE unit_label = '" + strSrcUnit + " '\n) " + 
				 " AND conv.dest_unit_id = (\n" + 
				 "   SELECT unit_id FROM idx_unit WHERE unit_label = '" + strDestUnit + "') ";
        double conversionRate = 1;
        try (Table result = session.getIOFactory().runSQL(sql);) {
            conversionRate = result.getDouble(0, 0);
        }

        Logging.debug("Conversion rate from the data base " + conversionRate);
        return conversionRate;
    }
    
    private boolean saveBrokerFeesTable(com.olf.openjvs.Table tblBrokerFee, com.olf.openjvs.Transaction tranPtr)
            throws OException {

        int intRetVal = 0;

        try {

            intRetVal = tranPtr.saveProvisional(tblBrokerFee);
            if (intRetVal == OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
                Logging.info("Success applying broker fee");
            } else {
                Logging.info("Error applying broker fee");
                return false;
            }

        } catch (OException e) {
            Logging.info(e.toString());
        }
        return true;
    }

}
