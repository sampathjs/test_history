package com.olf.jm.reportbuilder;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.OException;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.DBase;
import com.olf.openjvs.Index;
import com.olf.openjvs.Sim;
import com.olf.openjvs.Ref;
import com.olf.openjvs.enums.*;
import com.olf.jm.logging.Logging;

/* History:
 * 2020-11-11	V1.0	dnagy	- Initial Version
 * Builds a metal valuation report from various sources (Metals Balance Sheet, Combined Stock Report, various prices, etc.)
 * Part of the Management Report 3 pack
 */
	
public class ManagementReportMetalsValuation implements IScript {
	
	public static String bs_volume_table = "USER_tableau_mbs_combined";  //source table for the unhedged, hedged and leased volumes 
	public static String pnl_volume_table = "USER_tableau_pnl_global";   //source table for the open position volumes
	public static String csr_table_name = "USER_tableau_combined_stock"; //source table for NRV price calcs
	public static String unhedged_curve_name = "JM_Unhedged_Price";			 //source curve for unhedged metal prices, loads from saved closing rates for current business date
	
	//which lines of the first source we should read
	public static String [] bs_volume_lines = {"Total Unhedged Stock","Total Hedged Stock","L040 Intra Group Company Metal","Total Leases (External)"};
	public static String [] bs_volume_col_titles = {"volume_bs_unhedged","volume_bs_hedged","volume_bs_centre_leased","volume_bs_external_leased"};
	
	public boolean debug = false;
		
	public void execute(IContainerContext context) throws OException {
	
		Table vols_prices = context.getReturnTable();
		
		Table argt = context.getArgumentsTable();
		if (Table.isValidTable(argt)) {
			debug = true;
		}
		
		Table metals = Table.tableNew("metals");
		Table volume_pnl = Table.tableNew("volume_pnl");
		Table volume_bs = Table.tableNew("volume_bs");
		Table price_market = Table.tableNew("price_market");
		Table price_nrv = Table.tableNew("price_nrv");
		Table price_nrv_summed = Table.tableNew("price_nrv_summed");
		Table price_unhedged = Table.tableNew("price_unhedged");
		Table index_id_tbl = Table.tableNew("index_id");
		String actual_metal, sql, what, col_create, col, vol_string;
		Double d;
		int deal_num;
		
		try {
			Logging.init(this.getClass(), "", "");
			Logging.info("Starting " + getClass().getSimpleName());
			
			//metals volume and prices memory table creation 
			col_create = "S(metal_iso) S(metal_name) ";
			for (int k=0; k < bs_volume_col_titles.length; k++) {
				col_create +=" S(" + bs_volume_col_titles[k] + ")";
				col_create +=" F(" + bs_volume_col_titles[k] + "_dbl)";
			}
			col_create += "F(volume_pnl) F(price_unhedged) F(price_market) F(price_NRV) S(date) T(last_update)";
			vols_prices.addCols(col_create);
			vols_prices.setTableTitle("vols_prices");
			
			//filling table with list of metals
			sql = "select name as metal_iso, LOWER(description) as metal_name from currency where precious_metal=1";
			DBase.runSqlFillTable(sql, metals);
			vols_prices.select(metals, "metal_iso, metal_name", "metal_iso GT -1");
			
			//pnl_volume sourcing
			sql = "select sum(closing_volume) as volume_pnl, report_date as date, metal_ccy as metal_iso from " + pnl_volume_table + ", currency"
				+ " where metal_ccy=currency.name and precious_metal=1 group by metal_ccy, report_date ";
			DBase.runSqlFillTable(sql, volume_pnl);
			vols_prices.select(volume_pnl, "volume_pnl, date", "metal_iso EQ $metal_iso");
			
			//bs volumes sourcing
			for (int i=1; i <= metals.getNumRows(); i++) {
				actual_metal = metals.getString("metal_name", i);
				for (int j=0; j < bs_volume_lines.length; j++) {
					sql = "select " + actual_metal + "_actual as volume, '" + actual_metal + "' as metal_name from " + bs_volume_table + " where balance_desc = '" + bs_volume_lines[j] + "'";
					DBase.runSqlFillTable(sql, volume_bs);
					what = "volume(" + bs_volume_col_titles[j] + ")";
					vols_prices.select(volume_bs, what, "metal_name EQ $metal_name");
					volume_bs.clearRows();
				}
			}
			
			//yesterday market prices sourcing
			sql = "select idx_historical_prices.ref_source, SUBSTRING(index_name,1,3) as metal_iso, reset_date, price as price_market from idx_historical_prices, configuration, idx_def where reset_date=prev_business_date"
					+ " and idx_historical_prices.index_id=idx_def.index_id and db_status=1 and market=2 and currency=0 and idx_historical_prices.ref_source in (20008, 20007, 20022)";
			DBase.runSqlFillTable(sql, price_market);
            for (int i=1; i <= price_market.getNumRows(); i++) {
                d = to_GBP(price_market.getDouble("price_market", i), "USD");
                price_market.setDouble("price_market", i, d);
            }
			vols_prices.select(price_market, "price_market", "metal_iso EQ $metal_iso");

			//unhedged prices sourcing
			index_id_tbl.addCol("index_id", COL_TYPE_ENUM.COL_INT);
			index_id_tbl.addRow();
			int index_id = Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE, unhedged_curve_name);
			index_id_tbl.setInt("index_id", 1, index_id);
			//volume_pnl.getDate("date", 1); //this is the volume report's date, expected to be the previous business day, in date format 
			Sim.loadCloseIndexList(index_id_tbl, 1, Util.getBusinessDate());
			price_unhedged = Index.loadAllGpts(unhedged_curve_name);
			vols_prices.select(price_unhedged, "input.mid(price_unhedged)", "name EQ $metal_iso");
			
			//NRV prices sourcing
			sql = "select deal_tracking_num as deal_num, currency, currency1 as metal_iso, trade_date, maturity_date, metal_volume_toz, spot_equiv_value, settlement_value, DATEADD(dd, 0, DATEDIFF(dd, 0, last_update)) as last_update from USER_tableau_combined_stock";
			DBase.runSqlFillTable(sql, price_nrv);
			price_nrv.addCol("interest", COL_TYPE_ENUM.COL_DOUBLE);
			price_nrv.addCol("spot_eq_plus_interest_in_GBP", COL_TYPE_ENUM.COL_DOUBLE);
			for (int row=1; row <= price_nrv.getNumRows(); row++) {
				deal_num = price_nrv.getInt ("deal_num", row);
				if ("CNY".equals(price_nrv.getString("currency", row)) || (deal_num == 1099853) || (price_nrv.getDate("trade_date", row) ==	price_nrv.getDate("maturity_date", row)) ) {
					d = 0.0;
				} else {
					d =  ( price_nrv.getDouble("settlement_value", row) - price_nrv.getDouble("spot_equiv_value", row) ) 
						/ ( price_nrv.getDate("maturity_date", row) - price_nrv.getDate("trade_date", row) ) 
						* ( price_nrv.getDate("last_update", row) - price_nrv.getDate("trade_date", row) );
				}
				price_nrv.setDouble("interest",row,d);
				
				if ("CNY".equals(price_nrv.getString("currency", row))) { 
					d = 0.0;
				} else {
					d = to_GBP( (price_nrv.getDouble("spot_equiv_value", row) + price_nrv.getDouble("interest", row)), price_nrv.getString("currency", row) );
				}
				price_nrv.setDouble("spot_eq_plus_interest_in_GBP",row,d);
			}
			
			price_nrv_summed = metals.copyTable();
			price_nrv_summed.setTableTitle("price_nrv_summed");
			price_nrv_summed.addCol("price_NRV", COL_TYPE_ENUM.COL_DOUBLE);
			price_nrv_summed.select(price_nrv, "SUM, metal_volume_toz(sum_metal_volume_toz)", "metal_iso EQ $metal_iso");
			price_nrv_summed.select(price_nrv, "SUM, spot_eq_plus_interest_in_GBP(sum_spot_eq_plus_interest_in_GBP)", "metal_iso EQ $metal_iso");
			//price_nrv_summed.mathDivCol("sum_spot_eq_plus_interest_in_GBP", "sum_metal_volume_toz", "price_nrv");   //cannot use as can be division by zero
			for (int i=1; i<=price_nrv_summed.getNumRows(); i++) {
				if (price_nrv_summed.getDouble("sum_metal_volume_toz",i) != 0) {
					price_nrv_summed.setDouble("price_NRV", i, (price_nrv_summed.getDouble("sum_spot_eq_plus_interest_in_GBP",i) / price_nrv_summed.getDouble("sum_metal_volume_toz", i)) * (-1) );
				} else {
					price_nrv_summed.setDouble("price_NRV", i, 0);
				}
			}
			vols_prices.select(price_nrv_summed, "price_NRV", "metal_iso EQ $metal_iso");
			
			//converting string values to double
			for (int k=0; k < bs_volume_col_titles.length; k++) {
				col = bs_volume_col_titles[k];
				for (int row=1; row <= vols_prices.getNumRows(); row++) {
					vol_string = vols_prices.getString(col, row);
					vol_string = vol_string.replace("," , "");
					d = Double.parseDouble(vol_string);
					vols_prices.setDouble(col+"_dbl", row, d);
				}
			}
			
			//setting last_update info
			vols_prices.setColValDateTime("last_update", ODateTime.getServerCurrentDateTime());
			
			/*
			//calculating products from volumes and prices table
			col_create = "S(period) F(unhedged) F(open_positions) F(hedged) F(centre_leased) F(external_leased)";
			output.addCols(col_create);
			output.addRow();
			output.setString("period", 1, "Current");
			*/
			
			if (debug) {
				//output.viewTable();
				vols_prices.viewTable();
				price_nrv.viewTable();
				price_nrv_summed.viewTable();
			}
		} 
		catch (OException e) {
			String message = "Exception caught:" + e.getMessage();
			Logging.info(message);
			for (StackTraceElement ste : e.getStackTrace() )  {
				Logging.error(ste.toString(), e);
			}
		} finally {
			//output.destroy();
			vols_prices.destroy();
			metals.destroy();
			volume_pnl.destroy();
			volume_bs.destroy();
			price_nrv.destroy();
			price_nrv_summed.destroy();
			price_unhedged.destroy();
			index_id_tbl.destroy();
			Logging.info("End " + getClass().getSimpleName());
			Logging.close();
		}
		
	}
	
	public double to_GBP (double amount, String currency) {
		double retval = 0;
		try {
			Table ccytbl = Table.tableNew();
			String sql = "select fx_rate_mid from idx_historical_fx_rates, configuration where fx_rate_date=prev_business_date and currency_id=52";
			DBase.runSqlFillTable(sql, ccytbl);
			double gbpusd_yest = ccytbl.getDouble(1, 1);
			ccytbl.clearRows();
			
			if (!"USD".equals(currency)) {
				sql = "select fx_rate_mid, currency_convention from idx_historical_fx_rates, configuration, currency "
						+ "where idx_historical_fx_rates.fx_rate_date=configuration.prev_business_date and"
						+ " idx_historical_fx_rates.currency_id=currency.id_number and currency.name='" + currency + "'";
				DBase.runSqlFillTable(sql, ccytbl);
				double ccyusd_yest = ccytbl.getDouble(1, 1);
				int convention = ccytbl.getInt("currency_convention", 1);
				retval = ((convention == 1) ? amount * ccyusd_yest : amount / ccyusd_yest) / gbpusd_yest;
			} else {
				retval = amount / gbpusd_yest;
			}
			
			ccytbl.destroy();
			return retval; 
		} 
		catch (Exception e) {
			String message = "Exception caught:" + e.getMessage();
			Logging.info(message);
			for (StackTraceElement ste : e.getStackTrace() )  {
				Logging.error(ste.toString(), e);
			}
			return retval;
		}
	}
	
}

