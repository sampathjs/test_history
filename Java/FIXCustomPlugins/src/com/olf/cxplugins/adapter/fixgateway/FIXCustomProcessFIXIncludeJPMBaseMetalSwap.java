package com.olf.cxplugins.adapter.fixgateway;

import com.olf.jm.fixGateway.jpm.messageProcessor.BaseMetalSwapMsgProcessor;
import com.olf.jm.fixGateway.messageProcessor.MessageProcessor;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.XString;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.olf.jm.logging.Logging;

/**
 * {@link GuardedCustomFixProcessFIXInclude} for JPM Execute messages containing 
 * precious metal FX swap messages. 
 * 
 * @author jwaechter
 * @version 1.0
 */
public class FIXCustomProcessFIXIncludeJPMBaseMetalSwap extends FIXCustomProcessFIXIncludeJPM {
	/**
	 * The expected value of column {@link #SECURITY_SUB_TYPE_COL_NAME} that has to be set 
	 * for {@link FIXCustomProcessFIXIncludeJPMBaseMetalSwap} to process the incoming message.
	 */
	private static final String EXPECTED_SECURITY_SUB_TYPE = "BULLET SWAP STRIP";

	public String getExpectedSecuritySubtype() {
		return EXPECTED_SECURITY_SUB_TYPE;
	}

	public MessageProcessor getMessageProcessor() {
		return new BaseMetalSwapMsgProcessor();
	}

	@Override
	public TOOLSET_ENUM ProcessFixInc_GetToolset(Table argTbl, String message_name,
			Table incomingFixTable, XString xstring) throws OException {
		Logging.info("ProcessFixInc_GetToolset");
		if (Boolean.parseBoolean(constRep.getStringValue("AlwaysToMessageHospitalJPM"))) {
			throw new RuntimeException ("To Message Hospital because of ConstRep Settings ('"
					+ constRep.getContext() + "\\" + constRep.getSubcontext() + "\\AlwaysToMessageHospitalJPM"
					+ "')");			
		}
		
		return TOOLSET_ENUM.COM_SWAP_TOOLSET;	   
	}

	@Override
	protected String getTemplateReference(Table argTbl, String message_name, Table incomingFixTable,
			TOOLSET_ENUM toolset, TRAN_STATUS_ENUM tranStatus, XString xstring) {
		// TODO Auto-generated method stub
		return "unimplemented";
	}
}
