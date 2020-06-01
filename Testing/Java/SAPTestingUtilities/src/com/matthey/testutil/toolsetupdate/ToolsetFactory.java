package com.matthey.testutil.toolsetupdate;

import com.olf.openjvs.OException;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.jm.logging.Logging;

/**
 * Factory class to create an Object of ToolsetI based on the underlying Transaction
 * @author SharmV04
 *
 */
public class ToolsetFactory 
{	
	/**
	 * @param argClonedTransaction
	 * @param argDealDelta
	 * @return
	 * @throws OException
	 */
	public ToolsetI createToolset(Transaction argClonedTransaction, DealDelta argDealDelta)  throws OException
	{
		TOOLSET_ENUM toolsetEnum;
		
		int enumId = argClonedTransaction.getFieldInt(TRANF_FIELD.TRANF_TOOLSET_ID.toInt());
		
		toolsetEnum = TOOLSET_ENUM.fromInt(enumId);
		Logging.debug("toolsetString="+ toolsetEnum.toString()+"\n");

		ToolsetI toolset = null;
		switch (toolsetEnum)
		{
		case FX_TOOLSET:
			toolset = new FXToolset(argClonedTransaction, argDealDelta);
			break;
		case COMPOSER_TOOLSET:
			toolset = new ComposerToolset(argClonedTransaction, argDealDelta);
			break;
		default:
			String errMsg = toolsetEnum.toString() +" toolset is not supported.";
			Logging.error(errMsg);
			throw new OException(errMsg);
		}
		
		return toolset;

	}
}
