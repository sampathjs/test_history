package com.matthey.openlink.utilities.stub;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.tpm.AbstractProcessStep;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openrisk.market.EnumElementType;
import com.olf.openrisk.market.ForwardCurve;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableFactory;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.Token;
import com.olf.openrisk.tpm.Variable;
import com.olf.openrisk.tpm.Variables;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;


/*
 * History:
 * 2021-10-12	V1.2   BhardG01	- PBI000000002306 - Issue entering order
 */
@ScriptCategory({ EnumScriptCategory.TpmStep })
public class DispatchCarrierLimit extends AbstractProcessStep {

	@Override 
	public Table execute(Context context, Process process, Token token,
			Person submitter, boolean transferItemLocks, Variables variables) {
		Variable dispatchLimit = process.getVariable("dispatchFlagLimit"); 
		int tranNum = process.getVariable("TranNum").getValueAsInt(); 
		Transaction tranPtr = context.getTradingFactory().retrieveTransactionById(tranNum);
		dispatchLimit.setValue(isPhysicalDispatchRiskLimitBreached( tranPtr,  context));
		process.setVariable(dispatchLimit);
		boolean isPhyDispatchLimitBreached = isPhysicalDispatchRiskLimitBreached( tranPtr,  context);

		return variables.asTable().cloneData();
	}

	private boolean isPhysicalDispatchRiskLimitBreached(Transaction tranPtr, Context context) {
			String lastProjectionIndex = ""; 
			Double dailyVolume = 0.0;
			Double dailyPrice = 0.0;
			Double physicalDispathValue = 0.0;
			ForwardCurve projIndex = null;
			 
			String careerStr=  tranPtr.getField("Carrier").getValueAsString(); ; 
			for (Leg currentLeg : tranPtr.getLegs()){
				if(currentLeg.isPhysicalCommodity()){ 
				
				dailyVolume = currentLeg.getValueAsDouble( EnumLegFieldId.DailyVolume); 
				lastProjectionIndex = currentLeg.getField(EnumLegFieldId.ProjectionIndex).getValueAsString();
				projIndex = (ForwardCurve)tranPtr.getPricingDetails().getMarket().getElement(EnumElementType.ForwardCurve, lastProjectionIndex); 
				int lastProjectionIndexInt = currentLeg.getField(EnumLegFieldId.ProjectionIndex).getValueAsInt(); 
				//dailyPrice   = projIndex.getDirectParentIndexes().get(0).getGridPoints().getGridPoint("Spot").getValue(EnumGptField.EffInput);
			 	//physicalDispathValue = physicalDispathValue + 	dailyPrice*	dailyVolume;
			 	
			 	try {
					dailyPrice   = getHistoricalPrices(lastProjectionIndexInt, context);
				} catch (OException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			 	physicalDispathValue = physicalDispathValue + 	dailyPrice*	dailyVolume;
						 
				}
			}
	 		 
			 Double appDispatchLimit = getDispatchCareerApplicableLimit(careerStr, context);
			 long totalPhysicalDispathValue = Double.valueOf(physicalDispathValue).longValue();  
			 long absAppDispatchLimit = Double.valueOf(appDispatchLimit).longValue();   
			 if(totalPhysicalDispathValue >= absAppDispatchLimit){ 
				return true;
			}
			return false;
		
	}

	private Double getHistoricalPrices(int lastProjectionIndexInt, Context context) throws OException {
 
		Table queryList = context.getTableFactory().createTable();          
    	String queryStr = "SELECT top(1) price FROM  idx_historical_prices WHERE index_id=" + lastProjectionIndexInt +" AND ref_source ="+getRefSource(lastProjectionIndexInt, context)+"  order by last_update desc"; 

    	queryList = context.getIOFactory().runSQL(queryStr);  
    	Double price = queryList.getDouble("price", 0);
		return price;
	}

	private int getRefSource(int lastProjectionIndexInt, Context context) throws OException {
		
		Table queryList = context.getTableFactory().createTable();          
    	String queryStr = "SELECT ref_source,index_name FROM  idx_def WHERE index_id=" + lastProjectionIndexInt +" AND db_status=1" ; 
    	 
    	queryList = context.getIOFactory().runSQL(queryStr); 
    	String tmpRefSource = queryList.getString("index_name", 0);
    	
    	if(tmpRefSource!= null && tmpRefSource.equalsIgnoreCase("XAU.USD")){
    		return  queryList.getInt("ref_source", 0);
    	}
    	else if(tmpRefSource!= null && tmpRefSource.equalsIgnoreCase("XAG.USD")){
    		return  queryList.getInt("ref_source", 0);
    	}
    	else{
    		return Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, "JM London Opening");
    	}
	}

	private Double getDispatchCareerApplicableLimit(String carrier, Context context1) {

		TableFactory tf = context1.getTableFactory();  
		Table queryList = tf.createTable();  
        	String queryStr =  "SELECT upperlimit FROM  USER_jm_dispatch_carrier_limit   WHERE carrier= '" + carrier +"'" ;
        	queryList = context1.getIOFactory().runSQL(queryStr); 
        	Double upperlimit = queryList.getDouble("upperlimit", 0);
			return upperlimit;
	}
	
}