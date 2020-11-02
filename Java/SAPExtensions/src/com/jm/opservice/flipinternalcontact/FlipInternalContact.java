package com.jm.opservice.flipinternalcontact;

import java.util.HashSet;

import com.jm.exception.SapExtensionsRuntimeException;
import com.jm.utils.Constants;
import com.jm.utils.SapExtensionsConfig;
import com.jm.utils.Util;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.OpService;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.jm.logging.Logging;

/**
 * Blocks SAP amendments based on the following rules:
 * 1. If the trade is a sap coverage trade with a sap order id set, block it from further processing
 * 2. If the trade has a metal transfer request set, block it from further processing
 */
@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_OPS_SERVICE)
public class FlipInternalContact implements IScript
{
	public void execute(IContainerContext context) throws OException
	{
		Util.initialiseLog(Constants.LOG_SAP_EXTENSIONS_OPS_SERVICES);
		
		Table tblInfo = null;
		
		try
		{
			for (int i = 1; i <= OpService.retrieveNumTrans(); i++)
			{
				int tranNum = -1;
				
				try
				{

					Transaction tran = OpService.retrieveOriginalTran(i);
					tranNum = tran.getTranNum();
							
					String isCoverage = tran.getField(TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, Constants.TRANINFO_IS_COVERAGE);
					
					if ("yes".equalsIgnoreCase(isCoverage))
					{
						String internalContact = tran.getField(TRANF_FIELD.TRANF_INTERNAL_CONTACT.toInt());
						
						HashSet<String> technicalUsers = getTechnicalUsers();
						
						if (technicalUsers.contains(internalContact))
						{
							tblInfo = Ref.getInfo();
							int userId = tblInfo.getInt("user_id", 1);
							String personnel = Ref.getShortName(SHM_USR_TABLES_ENUM.PERSONNEL_TABLE, userId);
							
							if (internalContact.equalsIgnoreCase(personnel))
							{
								continue;
							}

							tran.setField(TRANF_FIELD.TRANF_INTERNAL_CONTACT.toInt(), 0, "", userId);
							
							Logging.debug("Internal contact flipped from: " + internalContact + " to " + personnel + " on tran_num: " + tranNum);
						}
					}
					
					tran = null;
				}
				catch (Exception e)
				{
					throw new SapExtensionsRuntimeException("Error occured during FlipInternalCOntact for tran_num: " + tranNum, e);
				}
			}
		}
		finally
		{
			Logging.close();
			if (tblInfo != null)
			{
				tblInfo.destroy();
			}
		}
	}

	/**
	 * Get a list of technical users from the constants repository
	 * 
	 * @return a list of technical users
	 */
	private HashSet<String> getTechnicalUsers() 
	{
		HashSet<String> users = new HashSet<String>();
		
		try
		{
			SapExtensionsConfig constRepoConfig = new SapExtensionsConfig(SapExtensionsConfig.CONST_REP_SUBCONTEXT_OPSERVICES);
			String technicalUsers = constRepoConfig.getValue(Constants.INTERNAL_CONTACT_TECHNICAL_USER_COMPARISON);
			
			String[] technicalUsersArray = technicalUsers.split(",");
			int numItems = technicalUsersArray.length;
			for (int item = 0; item < numItems; item++)
			{
				String user = technicalUsersArray[item];
				
				if (user != null)
				{
					users.add(user.trim());
				}	
			}	
		}
		catch (Exception e)
		{
			throw new SapExtensionsRuntimeException("Unable to fetch technical users from the database - check const repository settings", e);
		}
			
		return users;
	}
}