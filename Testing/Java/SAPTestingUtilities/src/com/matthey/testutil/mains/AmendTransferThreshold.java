package com.matthey.testutil.mains;

import com.matthey.testutil.BaseScript;
import com.matthey.testutil.exception.SapTestUtilRuntimeException;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.logging.PluginLog;

/**
 * Amends the transfer threshold to a custom value supplied from a TPM input variable.
 * 
 * Necessary for allowing the SAP test pack to bypass the threshold.
 * @author KailaM01
 */
public class AmendTransferThreshold extends BaseScript
{
	private static final String USERTABLE = "USER_JM_negative_threshold";
	
	@Override
	public void execute(IContainerContext context) throws OException 
	{
		setupLog();
		
		Table tblArgt = context.getArgumentsTable();
		Table tblData = null;
		
		try
		{
			double threshold = tblArgt.getDouble(1, 1);
			PluginLog.debug("Threshold detected as : " + threshold);
			
			tblData = Table.tableNew(USERTABLE);

			int ret = DBUserTable.structure(tblData);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				throw new SapTestUtilRuntimeException("Unable to load structure of " + USERTABLE);
			}
			
			tblData.addRow();
			tblData.setDouble(1, 1, threshold);
			
			/* Threshold is a single row, single value - safely clear it */
			ret = DBUserTable.clear(tblData);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				throw new SapTestUtilRuntimeException("Unable to clear data from " + USERTABLE);
			}
			
			ret = DBUserTable.insert(tblData);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				throw new SapTestUtilRuntimeException("Unable to insert data into " + USERTABLE);
			}
			
			PluginLog.debug("Negative threshold set to: " + threshold);
		}
		catch (Exception e)
		{
			throw new SapTestUtilRuntimeException("Error encountred when running AmendTransferThreshold", e);
		}
		finally
		{
			if (tblData != null)
			{
				tblData.destroy();
			}
		}
	}	
}
