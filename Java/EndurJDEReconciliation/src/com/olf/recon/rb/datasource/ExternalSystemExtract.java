package com.olf.recon.rb.datasource;


import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.recon.enums.SAPReconOutputFieldsEnum;
import com.olf.recon.exception.ReconciliationRuntimeException;
import com.olf.recon.rb.datasource.jdewrapper.JDEDealExtractARExtended;
import com.olf.recon.rb.datasource.sap.SAPExtract;
import com.olf.jm.logging.Logging;

public class ExternalSystemExtract implements IScript
{

	private final String REGION_CHINA = "China";
	

	@Override
	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();
		Table returnt = context.getReturnTable();
		try{
		Logging.init(this.getClass(), "Reconciliation","EndurJDEReconciliation");
		int mode = argt.getInt("ModeFlag", 1);
		
		/* Meta data collection */
		if (mode == 0) { 
			setOutputFormat(returnt); 
			
			setStandardOutputFieldsFormat(returnt);
			
			return;
		}
		
		/* Get custom report params where provided */
		ReportParameter rp = new ReportParameter(argt);
		String region = rp.getRegion();
		if (region == null || "".equalsIgnoreCase(region))
		{
			throw new ReconciliationRuntimeException("No region parameter specified!");
		}
		Logging.info("Running Extract for  : " + region);
		
		if (region.equalsIgnoreCase(REGION_CHINA)) {
			new SAPExtract ().execute (context);
			//context.getReturnTable().viewTable();
		} else {
			new JDEDealExtractARExtended ().execute(context);
			//context.getReturnTable().viewTable();
		}
		}catch(Exception ex){
			Logging.error(ex.toString());
			throw new RuntimeException(ex);
		}finally{
		
		Logging.info("Completed");
		Logging.close();
		}
	}
	
	public void setOutputFormat(Table output) throws OException 
	{

		output.addCol(SAPReconOutputFieldsEnum.ID.toString(), SAPReconOutputFieldsEnum.ID.colType());
		output.addCol(SAPReconOutputFieldsEnum.NOTE.toString(), SAPReconOutputFieldsEnum.NOTE.colType());
		output.addCol(SAPReconOutputFieldsEnum.GL_DATE.toString(), SAPReconOutputFieldsEnum.GL_DATE.colType());
		output.addCol(SAPReconOutputFieldsEnum.GL_DATE_PLUGIN.toString(), SAPReconOutputFieldsEnum.GL_DATE_PLUGIN.colType());		
		output.addCol(SAPReconOutputFieldsEnum.ACCOUNT_NUMBER.toString(), SAPReconOutputFieldsEnum.ACCOUNT_NUMBER.colType());
		output.addCol(SAPReconOutputFieldsEnum.TYPE.toString(), SAPReconOutputFieldsEnum.TYPE.colType()); 
		output.addCol(SAPReconOutputFieldsEnum.ENDUR_DOC_NUM.toString(), SAPReconOutputFieldsEnum.ENDUR_DOC_NUM.colType());
		output.addCol(SAPReconOutputFieldsEnum.VALUEDATE.toString(),SAPReconOutputFieldsEnum.VALUEDATE.colType());
		output.addCol(SAPReconOutputFieldsEnum.VALUE_DATE_PLUGIN.toString(), SAPReconOutputFieldsEnum.VALUE_DATE_PLUGIN.colType());
		output.addCol(SAPReconOutputFieldsEnum.QUANTITY.toString(), SAPReconOutputFieldsEnum.QUANTITY.colType());
		output.addCol(SAPReconOutputFieldsEnum.AMOUNT.toString(), SAPReconOutputFieldsEnum.AMOUNT.colType());
		output.addCol(SAPReconOutputFieldsEnum.CURRENCY.toString(), SAPReconOutputFieldsEnum.CURRENCY.colType());
		output.addCol(SAPReconOutputFieldsEnum.BATCH_NUM.toString(), SAPReconOutputFieldsEnum.BATCH_NUM.colType());
		output.addCol(SAPReconOutputFieldsEnum.DEAL_NUM.toString(), SAPReconOutputFieldsEnum.DEAL_NUM.colType());
		output.addCol(SAPReconOutputFieldsEnum.KEY.toString(), SAPReconOutputFieldsEnum.KEY.colType());
		output.addCol(SAPReconOutputFieldsEnum.CREDITDEBIT.toString(), SAPReconOutputFieldsEnum.CREDITDEBIT.colType());
		output.addCol(SAPReconOutputFieldsEnum.TAX.toString(), SAPReconOutputFieldsEnum.TAX.colType());
	}

	/**
	 * Add any standard output fields to the output. All reports will have these.
	 * 
	 * @param output
	 * @throws OException
	 */
	protected void setStandardOutputFieldsFormat(Table output) throws OException
	{   
		output.addCol("reporting_date", COL_TYPE_ENUM.COL_INT);
		output.addCol("extract_datetime", COL_TYPE_ENUM.COL_DATE_TIME);
	}
}
