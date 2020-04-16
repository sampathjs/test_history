package com.olf.recon.enums;

import com.olf.openjvs.enums.COL_TYPE_ENUM;

/***
 * 
 * @author joshig01
 * 
 * This enum will have all the fields for SAP Recon.
 * 
 */

public enum SAPReconOutputFieldsEnum 
{
	ACCOUNT_NUMBER("account", COL_TYPE_ENUM.COL_STRING),
	AMOUNT("amount", COL_TYPE_ENUM.COL_DOUBLE),
	BATCH_NUM("batch_num", COL_TYPE_ENUM.COL_STRING),
	CURRENCY("currency", COL_TYPE_ENUM.COL_STRING),
	CREDITDEBIT("debit_credit", COL_TYPE_ENUM.COL_STRING),
	ENDUR_DOC_NUM("document_num", COL_TYPE_ENUM.COL_INT),
	EXTRACT_DATETIME("extract_datetime", COL_TYPE_ENUM.COL_DATE_TIME),
	GL_DATE("gl_date", COL_TYPE_ENUM.COL_INT),
	GL_DATE_PLUGIN("gl_date_plugin", COL_TYPE_ENUM.COL_DATE_TIME),
	ID("id", COL_TYPE_ENUM.COL_INT),
	KEY("key", COL_TYPE_ENUM.COL_STRING),
	NOTE("note", COL_TYPE_ENUM.COL_STRING),
	ORIG_REPORTING_DATE("orig_reporting_date", COL_TYPE_ENUM.COL_INT),
	QUANTITY("qty_toz", COL_TYPE_ENUM.COL_DOUBLE),
	REPORTING_DATE("reporting_date", COL_TYPE_ENUM.COL_STRING),
	DEAL_NUM("tran_num", COL_TYPE_ENUM.COL_INT),
	TYPE("type",COL_TYPE_ENUM.COL_STRING),
	VALUEDATE("value_date", COL_TYPE_ENUM.COL_INT),
	VALUE_DATE_PLUGIN("value_date_plugin", COL_TYPE_ENUM.COL_DATE_TIME),
	TAX("tax_amount", COL_TYPE_ENUM.COL_DOUBLE)
;
	
	private final String fieldNname;
	private final COL_TYPE_ENUM columnType;

    private SAPReconOutputFieldsEnum(final String text, final COL_TYPE_ENUM colType) 
    {
        this.fieldNname = text;
        this.columnType = colType;
    }

    @Override
    public String toString() 
    {
        return fieldNname;
    }
    
    public COL_TYPE_ENUM colType () {
    	return columnType;
    }
}
