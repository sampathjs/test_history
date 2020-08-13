package com.olf.jm.reportbuilder.filter;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;

public class AdviceNoteBoxGroupFilter implements IScript {

	@Override
	public void execute(IContainerContext context) throws OException {

		Table tblArgt = context.getArgumentsTable();

		// find totals of XPT, XPD, XIR, XOS, XRH, XRU, XAG, XAU
		double dblXPTGross=0.0;
		double dblXPTNet=0.0;
		double dblXPDGross=0.0;
		double dblXPDNet=0.0;
		double dblXIRGross=0.0;
		double dblXIRNet=0.0;
		double dblXRHGross=0.0;
		double dblXOSGross=0.0;
		double dblXOSNet=0.0;
		double dblXRHNet=0.0;
		double dblXRUGross=0.0;
		double dblXRUNet=0.0;
		double dblXAUGross=0.0;
		double dblXAUNet=0.0;
		double dblXAGGross=0.0;
		double dblXAGNet=0.0;
		
		for(int i=1;i<=tblArgt.getNumRows();i++){
			
			String strMetal = tblArgt.getString("name1", i);
			double dblGrossAmt = tblArgt.getDouble("gross_qty",i);
			double dblNetAmt = tblArgt.getDouble("delivery_ticket_volume",i);
			
			switch(strMetal){
				case "Platinum":
					dblXPTGross += dblGrossAmt;
					dblXPTNet += dblNetAmt;
					break;
				case "Palladium":
					dblXPDGross += dblGrossAmt;
					dblXPDNet += dblNetAmt;
					break;
				case "Iridium":
					dblXIRGross += dblGrossAmt;
					dblXIRNet += dblNetAmt;
					break;
				case "Osmium":
					dblXOSGross += dblGrossAmt;
					dblXOSNet += dblNetAmt;
					break;
				case "Rhodium":
					dblXRHGross += dblGrossAmt;
					dblXRHNet += dblNetAmt;
					break;
				case "Ruthenium":
					dblXRUGross += dblGrossAmt;
					dblXRUNet += dblNetAmt;
					break;
				case "Gold":
					dblXAUGross += dblGrossAmt;
					dblXAUNet += dblNetAmt;
					break;
				case "Silver":
					dblXAGGross += dblGrossAmt;
					dblXAGNet += dblNetAmt;
					break;
				default:
					break;
			}
			
		}
		
		tblArgt.setColValDouble("xpt_gross", round(dblXPTGross,3));
		tblArgt.setColValDouble("xpt_net", round(dblXPTNet,3));
		tblArgt.setColValDouble("xpd_gross", round(dblXPDGross,3));
		tblArgt.setColValDouble("xpd_net", round(dblXPDNet,3));
		tblArgt.setColValDouble("xir_gross", round(dblXIRGross,3));
		tblArgt.setColValDouble("xir_net", round(dblXIRNet,3));
		tblArgt.setColValDouble("xos_gross", round(dblXOSGross,3));
		tblArgt.setColValDouble("xos_net", round(dblXOSNet,3));
		tblArgt.setColValDouble("xrh_gross", round(dblXRHGross,3));
		tblArgt.setColValDouble("xrh_net", round(dblXRHNet,3));
		tblArgt.setColValDouble("xru_gross", round(dblXRUGross,3));
		tblArgt.setColValDouble("xru_net",round(dblXRUNet,3));
		tblArgt.setColValDouble("xau_gross",round(dblXAUGross,3));
		tblArgt.setColValDouble("xau_net",round(dblXAUNet,3));
		tblArgt.setColValDouble("xag_gross", round(dblXAGGross,3));
		tblArgt.setColValDouble("xag_net",round(dblXAGNet,3));
		
		// Find rows of distinct Crates 
		// Remove individual rows of distinct crates
		// Aggregate rows of distinct crates
		// Add Aggregated Rows back to table
		
		tblArgt.addCol("counter", COL_TYPE_ENUM.COL_DOUBLE);
		tblArgt.setColValDouble("counter", 1);
		
		Table tblCrateCount = Table.tableNew();
		tblCrateCount.select(tblArgt, "DISTINCT,crate_id", "crate_id NE ''" );

		String strWhat = "SUM,counter(counter_sum)";
		String strWhere = "crate_id EQ $crate_id " ;
		tblCrateCount.select(tblArgt, strWhat, strWhere );

		for(int i = tblCrateCount.getNumRows();i>0;i--){
			if(tblCrateCount.getDouble("counter_sum", i) < 2.0){
				tblCrateCount.delRow(i);
			}
		}
		
		// Remove detail rows from argt
		Table tblCrateDetail = tblArgt.cloneTable();
		for(int i = 1; i<=tblCrateCount.getNumRows();i++){

			String strCrateId = tblCrateCount.getString("crate_id",i);
			
			for(int j = tblArgt.getNumRows();j>0;j--){
				
				if(Str.equal(tblArgt.getString("crate_id", j),strCrateId) == 1){
					
					tblArgt.copyRowAdd(j, tblCrateDetail);
					tblArgt.delRow(j);
				}
			}
		}	
		
		// Create summary row from detail rows
		// Add summary row into argt
		for(int i = 1; i<=tblCrateCount.getNumRows();i++){

			Table tblCrateSummary = tblCrateDetail.cloneTable();
			
			String strCrateId = tblCrateCount.getString("crate_id",i);
			for(int j =1;j<=tblCrateDetail.getNumRows();j++){
				
				if(Str.equal(tblCrateDetail.getString("crate_id", j),strCrateId) == 1){
					
					tblCrateDetail.copyRowAdd(j, tblCrateSummary);
				}
			}
			
			double dblGrossBoxWeight = 0.0;
			double dblNetBoxWeight= 0.0;
			for(int j=1;j<=tblCrateSummary.getNumRows();j++){
				
				//Metal
				tblCrateSummary.setString("name1", j, "");
				//Form
				tblCrateSummary.setString("comm_form_name", j, "");
				//Purity
				tblCrateSummary.setString("upper_value", j, "");
				//gross box weight
				dblGrossBoxWeight += tblCrateSummary.getDouble("gross_qty", j);
				//container net weight
				dblNetBoxWeight += tblCrateSummary.getDouble("delivery_ticket_volume", j);
			}

			tblCrateSummary.setColValDouble("gross_qty", dblGrossBoxWeight);
			tblCrateSummary.setColValDouble("delivery_ticket_volume", dblNetBoxWeight);
			tblCrateSummary.makeTableUnique();
			
			tblCrateSummary.copyRowAddAll(tblArgt);
			tblCrateSummary.destroy();
			
		}
			
		tblCrateDetail.destroy();
		tblCrateCount.destroy();
		
		tblArgt.delCol("counter");
		tblArgt.sortCol("crate_id");
	}
	
	private static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();
	 
	    BigDecimal bd = new BigDecimal(Double.toString(value));
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
}
