package com.olf.jm.fixGateway.fieldMapper.fields;


/*
 * History:
 * 2017-10-10 - V0.1 - scurran - Initial Version
 * 2020-05-18 - V0.2 - jwaechter - Added several new fields for JPM Execute
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
	SECONDARY_EXEC_ID("SecondaryExecID"),	
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
	TAG9525("Tag9525"),
	SETTL_CURRENCY("SettlCurrency"),  // has to be verified, Tag #120
	SETTL_CURRENCY_AMT("SettlCurrAmt"),  // has to be verified, Tag #119
	FX_SETTLE_DATE("SettlDate"),  // has to be verified, Tag #64
	LAST_SPOT_RATE("LastSpotRate"), // has to be verified, Tag #194
	ORDER_QTY_2 ("OrderQty2"), // has to be verified, Tag #192
	SETTL_DATE_2 ("SettlDate2"), // has to be verified, Tag #193
	PRICE_2 ("Price2"), // has to be verified, Tag #640
	TAG_7596 ("Tag7596"), // has to be verified, counter amount
	TAG_996 ("Tag996"), // has to be verified,  unit of measure
	TAG_10070 ("Tag10070"), // has to be verified,  ref source
	TAG_1191 ("Tag1191"), // has to be verified, price unit of measure
	TAG_6203 ("Tag6203"), // has to be verified, fixing start date
	TAG_6204 ("Tag6204"), // has to be verified, fixing end date

	;	


	
	private String tagName;
	
	EnumExecutionReport(String tagName) {
		this.tagName = tagName;
	}
	
	public String getTagName() {
		return tagName;
	}

}
