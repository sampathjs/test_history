package com.matthey.openlink.utilities.stub;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.tpm.AbstractProcessStep;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
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


/*/*
 * History:
 * 2020-03-25	V1.1	 BhardG01	- WO0000000064879 - Physical dispatch risk check - based on value & carrier 
 * 2021-10-12	V1.2     BhardG01	- PBI000000002306 - Issue entering order
 */
@ScriptCategory({ EnumScriptCategory.TpmStep })
public class DispatchCarrierLimit extends AbstractProcessStep {
	

	private static final String CONST_REPO_CONTEXT = "Support"; 
	private static final String CONST_REPO_SUBCONTEXT="DispatchCarrierLimit";

	@Override
	public Table execute(Context context, Process process, Token token,
			Person submitter, boolean transferItemLocks, Variables variables) {
		
		
		Logging.init(context, this.getClass(),  CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
		
		 
		//Collateral_Value
		try{  
		Variable dispatchFlagLimit = process.getVariable("dispatchFlagLimit"); 
		Variable dispathCarrierLimit = process.getVariable("Dispath_Carrier_Limit"); 
		Variable totalDealValue = process.getVariable("Total_Deal_Value"); 
		Variable dispatchAssignment = process.getVariable("Dispatch_Assignment"); 
		int tranNum = process.getVariable("TranNum").getValueAsInt(); 
		Transaction tranPtr = context.getTradingFactory().retrieveTransactionById(tranNum);
		
		dispatchFlagLimit.setValue(isPhysicalDispatchRiskLimitBreached( tranPtr,  context));
		String careerStr=  tranPtr.getField("Carrier").getValueAsString();

		dispathCarrierLimit.setValue(getDispatchCareerApplicableLimit(careerStr, context));

		totalDealValue.setValue(getTotalDealValue( tranPtr, context)); 

		dispatchAssignment.setValue(1);
		process.setVariable(dispatchFlagLimit);
		process.setVariable(dispathCarrierLimit);
		process.setVariable(totalDealValue);
		process.setVariable(dispatchAssignment);
		}catch(Exception ex){
			throw new RuntimeException("Retrieve Collateral failed",ex);
		} finally{
			Logging.close();
		}  
		return variables.asTable().cloneData();
	}

	private Double getTotalDealValue(Transaction tranPtr, Context context) throws OException {
			 Double dailyPrice,dailyVolume = 0.0;
			Double physicalDispathValue = 0.0; 
			int lastProjectionIndexInt ;
			for (Leg currentLeg : tranPtr.getLegs()){
				if(currentLeg.isPhysicalCommodity()){ 
				
				dailyVolume = currentLeg.getValueAsDouble( EnumLegFieldId.DailyVolume); 
				lastProjectionIndexInt = currentLeg.getField(EnumLegFieldId.ProjectionIndex).getValueAsInt();
			 	dailyPrice   = getHistoricalPrices(lastProjectionIndexInt, context);
				physicalDispathValue = physicalDispathValue + 	dailyPrice*	dailyVolume;
						 
				}
			}
	 		  
			return physicalDispathValue;
		
	}

	private boolean isPhysicalDispatchRiskLimitBreached(Transaction tranPtr, Context context) throws OException {
			Double dailyPrice,dailyVolume = 0.0;
			Double physicalDispathValue = 0.0; 
			String careerStr=  tranPtr.getField("Carrier").getValueAsString();
			int lastProjectionIndexInt ;
			 
			for (Leg currentLeg : tranPtr.getLegs()){
				if(currentLeg.isPhysicalCommodity()){ 
				
				dailyVolume = currentLeg.getValueAsDouble( EnumLegFieldId.DailyVolume); 
				lastProjectionIndexInt = currentLeg.getField(EnumLegFieldId.ProjectionIndex).getValueAsInt();
				dailyPrice   = getHistoricalPrices(lastProjectionIndexInt, context);
				 
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

	private int getRefSource(int lastProjectionIndexInt, Context context) throws OException 
	{
		
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
        	String queryStr =  "SELECT limit FROM  USER_jm_dispatch_carrier_limit   WHERE carrier= '" + carrier +"'" ;
        	queryList = context1.getIOFactory().runSQL(queryStr); 
        	Double limit = queryList.getDouble("limit", 0);
			return limit;
	}
	
}