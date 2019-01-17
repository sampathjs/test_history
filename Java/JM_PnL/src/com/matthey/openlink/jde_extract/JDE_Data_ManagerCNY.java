package com.matthey.openlink.jde_extract;

import com.olf.openjvs.OException;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;

/*
 *History: 
 * 201x-xx-xx	V1.0     	- initial version	
 * 2017-11-16	V1.1	lma	- succeed if there isn't any update
 * 2018-10-30   V1.2    smc - refactored to support CNY
 *
 */
public class JDE_Data_ManagerCNY extends JdeDataManagerBase  
{
	/* (non-Javadoc)
	 * @see com.matthey.openlink.jde_extract.IJdeDataManager#needToProcessTransaction(com.olf.openjvs.Transaction)
	 */
	@Override
	public boolean needToProcessTransaction(Transaction trn) throws OException
	{
		boolean needToProcess = false;
		
		int toolset = trn.getFieldInt(TRANF_FIELD.TRANF_TOOLSET_ID.toInt());
		
		if ((toolset == TOOLSET_ENUM.COM_SWAP_TOOLSET.toInt()))
		{
			needToProcess = true;
		}
		
		return needToProcess;
	}
}
