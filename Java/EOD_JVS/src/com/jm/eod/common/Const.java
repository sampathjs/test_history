package com.jm.eod.common;
/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * Copyright: OpenLink International Ltd. ©. London U.K.
 *
 * Constants for JM EOD
 *  
 * @author Douglas Connolly
 *
 * History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 03-Nov-2015 |               | D.Connolly      | Initial version.                                                                |
 * | 002 | 03-May-2019 |               | C Badcock      | Added missing constants.                                                                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 */

public class Const 
{
	public static final String REGION_COL_NAME = "region_code";
	public static final String QUERY_COL_NAME = "query_name";

	public static final String QUERY_DATE = "report_date";
	public static final String QUERY_REPORT = "report_name";

	
	// region codes inserted for regional queries eg. "HK_"
	public static final String LOCKED_DEALS_QRY_NAME = "EOD_%sLocked_Deals";
	public static final String MISSING_VALIDATIONS_QRY_NAME = "EOD_%sMissing_Validations";
	public static final String MISSING_RESETS_QRY_NAME = "EOD_%sMissing_Resets";
	public static final String RESET_FIXINGS_QRY_NAME = "EOD_%sReset_Fixings";
	
	// Added retrospectively to make EOD_OC compile
	public static final String DEALS_LOCKED_COL_NAME = "deals_locked";
	public static final String EOD_STATUS_TBL_NAME = "USER_jm_eod_status";
	public static final String ID_COL_NAME = "id";
}
