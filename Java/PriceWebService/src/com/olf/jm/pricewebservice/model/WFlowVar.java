package com.olf.jm.pricewebservice.model;

/*
 * History:
 * 2015-04-15	V1.0	jwaechter - initial version
 * 2016-02-16	V1.1	jwaechter - added MailService
 * 2017-11-28	V1.2	scurran   - added support for gold and silver
 * 2020-01023   V1.3    Pramod Garg- SR301244 - Added Retry count and max Retry count for alert
 */

/**
 * This class contains the names of the variables of the PriceWebService TPM workflow
 * @author jwaechter
 * @version 1.2
 */
public enum WFlowVar {
	IS_CLOSING_DATE_EQUAL_TRADING_DATE("IsClosingDateEqualTradingDate"),
	INDEX_ID("IndexId"),
	INDEX_NAME("IndexName"),
	CURRENT_DATASET_TYPE ("CurrentDatasetType"),
	CURRENT_DATE("CurrentDate"),
	CURRENT_DATE_JD("CurrentDateJD"),
	TRADING_DATE("TradingDate"),
	CURRENT_USER_FOR_TEMPLATE("CurrentUserForTemplate"),
	CURRENT_TEMPLATE("CurrentTemplate"),
	CURRENT_OUTPUT("CurrentOutput"),
	CURRENT_OUTPUT_FILE("CurrentOutputFile"),
	CURRENT_REPORT_NAME("CurrentReportName"),
	CURRENT_DELIVERY_LOGIC ("CurrentDeliveryLogic"),
	CURRENT_TEMPLATE_ID ("CurrentTemplateId"),
	SENDER_EMAIL("SenderEmail"),
	SUBJECT ("Subject"),
	REPLY_TO ("ReplyToEmail"),
	CURRENT_CHARSET ("CurrentCharset"),
	REPORT_PARAMETERS_XML ("ReportParametersXml"),
	REPORT_PARAMETERS_CSV_GENERAL ("ReportParametersCsvGeneral"),
	REPORT_PARAMETERS_CSV_GENERAL_CON ("ReportParameterCSVGeneralConv"),
	REPORT_PARAMETERS_CSV_NM ("ReportParametersCsvNM"), 
	REPORT_PARAMETERS_CSV_AUAG ("ReportParametersCsvGeneralAuAg"), 
	MAIL_SERVICE ("MailService"),
	RETRY_COUNT("RetryCount2"),
	MAX_RETRY_COUNT("MaxRetryCount"),
	;
	
	private final String name;
	
	private WFlowVar (String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
