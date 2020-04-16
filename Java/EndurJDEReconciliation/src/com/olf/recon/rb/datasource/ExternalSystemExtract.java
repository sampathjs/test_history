package com.olf.recon.rb.datasource;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.recon.exception.ReconciliationRuntimeException;
import com.olf.recon.rb.datasource.jdewrapper.JDEDealExtractARExtended;
import com.olf.recon.rb.datasource.sap.SAPExtract;
import com.openlink.util.logging.PluginLog;

public class ExternalSystemExtract implements IScript
{

	private final String REGION_CHINA = "China";
	
	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();
		
		/* Get custom report params where provided */
		ReportParameter rp = new ReportParameter(argt);
		String region = rp.getRegion();
		if (region == null || "".equalsIgnoreCase(region))
		{
			throw new ReconciliationRuntimeException("No region parameter specified!");
		}
		PluginLog.info("Running Extract for  : " + region);
		
		if (region.equalsIgnoreCase(REGION_CHINA)) {
			new SAPExtract ().execute (context);
			//context.getReturnTable().viewTable();
		} else {
			new JDEDealExtractARExtended ().execute(context);
			//context.getReturnTable().viewTable();
		}
		
		PluginLog.info("Completed");
		
	}
}
