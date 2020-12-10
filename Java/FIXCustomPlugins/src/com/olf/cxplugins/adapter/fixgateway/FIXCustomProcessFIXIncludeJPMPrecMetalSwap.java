package com.olf.cxplugins.adapter.fixgateway;

import com.olf.jm.fixGateway.fieldMapper.FieldMapperException;
import com.olf.jm.fixGateway.jpm.fieldMapper.InternalBunit;
import com.olf.jm.fixGateway.jpm.fieldMapper.InternalContact;
import com.olf.jm.fixGateway.jpm.fieldMapper.PassThruUnitInfo;
import com.olf.jm.fixGateway.jpm.messageProcessor.PrecMetalSwapMsgProcessor;
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
public class FIXCustomProcessFIXIncludeJPMPrecMetalSwap extends FIXCustomProcessFIXIncludeJPM {
	/**
	 * The expected value of column {@link #SECURITY_SUB_TYPE_COL_NAME} that has to be set 
	 * for {@link FIXCustomProcessFIXIncludeJPMPrecMetalSwap} to process the incoming message.
	 */
	private static final String EXPECTED_SECURITY_SUB_TYPE = "FXSWAP";

	public String getExpectedSecuritySubtype() {
		return EXPECTED_SECURITY_SUB_TYPE; 
	}

	public MessageProcessor getMessageProcessor() {
		return new PrecMetalSwapMsgProcessor();
	}

	@Override
	public TOOLSET_ENUM ProcessFixInc_GetToolset(Table argTbl, String message_name,
			Table incomingFixTable, XString xstring) throws OException {
		return TOOLSET_ENUM.FX_TOOLSET;	  
	}

	@Override
	protected String getTemplateReference(Table argTbl, String message_name, Table incomingFixTable,
			TOOLSET_ENUM toolset, TRAN_STATUS_ENUM tranStatus, XString xstring) {
		PassThruUnitInfo passThruUnitMapper = new PassThruUnitInfo();
		try {
			String passThruUnit = passThruUnitMapper.getTranFieldValue(incomingFixTable);
			if (passThruUnit != null && passThruUnit.trim().length() > 0) {
				return "Swap Pass Thru";
			} 
			return "";
		} catch (FieldMapperException e) {
			Logging.error("Could not determine Pass Thru Unit while retrieving template reference");
			Logging.error(e.toString());
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error(ste.toString());
			}
			throw new RuntimeException ("Could not determine Pass Thru Unit while retrieving template reference", e);
		}
	}
}
