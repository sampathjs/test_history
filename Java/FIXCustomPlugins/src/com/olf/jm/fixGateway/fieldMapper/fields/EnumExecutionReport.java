package com.olf.jm.fixGateway.fieldMapper.fields;


/*
 * History:
 * 2017-10-10 - V0.1 - scurran - Initial Version
 */

/** Enum representing the fields in the inbound fix message
 * 
 * @author curras01
 *
 */
public enum EnumExecutionReport implements FixField {

	REFERENCE("Reference"),	
	BEGIN_STRING("BeginString"),	
	BODY_LENGTH("BodyLength"),	
	MSG_SEQ_NUM("MsgSeqNum"),	
	MSG_TYPE("MsgType"),	
	SENDER_COMP_ID("SenderCompID"),	
	SENDING_TIME("SendingTime"),	
	TARGET_COMP_ID("TargetCompID"),	
	TARGET_SUB_ID("TargetSubID"),	
	DELIVER_TO_COMP_ID("DeliverToCompID"),	
	DELIVER_TO_SUB_ID("DeliverToSubID"),	
	ACCOUNT("Account"),	
	AVG_PX("AvgPx"),	
	CUM_QTY("CumQty"),	
	CURRENCY("Currency"),	
	EXEC_ID("ExecID"),	
	TAG20("Tag20"),	
	LAST_PX("LastPx"),	
	LAST_QTY("LastQty"),	
	ORDER_ID("OrderID"),	
	ORDER_QTY_DATA("OrderQtyData"),	
	ORD_STATUS("OrdStatus"),	
	ORD_TYPE("OrdType"),	
	PRICE("Price"),	
	SIDE("Side"),	
	INSTRUMENT("Instrument"),	
	TIME_IN_FORCE("TimeInForce"),	
	TRANSACT_TIME("TransactTime"),	
	TRADE_DATE("TradeDate"),	
	TAG76("Tag76"),	
	EX_DESTINATION("ExDestination"),	
	MIN_QTY("MinQty"),	
	MAX_FLOOR("MaxFloor"),	
	EXEC_TYPE("ExecType"),	
	LEAVES_QTY("LeavesQty"),	
	TAG205("Tag205"),
	MULTI_LEG_REPORTING_TYPE("MultiLegReportingType"),	
	TAG9506("Tag9506"),	
	TAG9509("Tag9509"),	
	TAG9513("Tag9513"),	
	TAG9515("Tag9515"),
	TAG9524("Tag9524"),	
	TAG9525("Tag9525");	


	
	private String tagName;
	
	EnumExecutionReport(String tagName) {
		this.tagName = tagName;
	}
	
	public String getTagName() {
		return tagName;
	}

}
