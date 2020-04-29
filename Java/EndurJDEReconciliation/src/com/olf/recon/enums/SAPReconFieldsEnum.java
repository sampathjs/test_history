package com.olf.recon.enums;

/***
 * 
 * @author joshig01
 * 
 * This enum will have all the fields for SAP Recon.
 * 
 */

public enum SAPReconFieldsEnum 
{
	BASIC_AUTH_PASSWORD("basic_auth_password"),
	BASIC_AUTH_USERNAME("basic_auth_username"),
	CONNECTION_TIMEOUT("connection_timeout"),
	HOST_ADDRESS("host_address"),
	HOST_PORT("host_port"),
	HOST_NAME("host_name"),
	SERVICE_URL("service_url"),
	ENDPOINT_URL("endpoint_url"),
	QUERY_URL("query_url"),
	PARAM_CONSUMER_APP("param_consumer_app"),
	PARAM_TARGET_APP("param_target_app"),
	READ_TIMEOUT("read_timeout"),
	JSON_TAG_ENDUR_DOCUMENT_ID("json_tag_endur_document_id"),
	JSON_TAG_NOTE("json_tag_note"),
	JSON_TAG_CREDIT_DEBIT("json_tag_credit_debit"),
	JSON_TAG_GLDATE("json_tag_gldate"),
	JSON_TAG_DOCUMENT_TYPE("json_tag_document_type"),
	JSON_TAG_ACCOUNT("json_tag_account"),
	JSON_TAG_ACCOUNT_TYPE("json_tag_account_type"),
	JSON_TAG_VALUE_DATE("json_tag_value_date"),
	JSON_TAG_QUANTITY("json_tag_quantity"),
	JSON_TAG_UOM("json_tag_uom"),
	JSON_TAG_AMOUNT("json_tag_amount"),
	JSON_TAG_TAX_AMOUNT("json_tag_tax_amount"),
	JSON_TAG_CURRENCY("json_tag_currency"),
	JSON_TAG_SAP_DOCUMENT_ID("json_tag_sap_document_id"),
	JSON_TAG_SAP_DOCUMENT_DATE("json_tag_sap_document_date"),
	TEST_FLAG("test_flag"),
	TEST_FILE("test_file")
;
	
	private final String tagNname;

    private SAPReconFieldsEnum(final String text) 
    {
        this.tagNname = text;
    }

    @Override
    public String toString() 
    {
        return tagNname;
    }
}
