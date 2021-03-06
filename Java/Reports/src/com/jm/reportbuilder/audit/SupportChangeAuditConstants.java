package com.jm.reportbuilder.audit;


public class SupportChangeAuditConstants  {

	protected static final String USER_SUPPORT_CHANGE_AUDIT = "USER_support_change_audit";
	/**
	 * Specifies the constants' repository context parameter.
	 */
	protected static final String REPO_CONTEXT = "Support";

	/**
	 * Specifies the constants' repository sub-context parameter.
	 */
	protected static final String REPO_SUB_CONTEXT = "UserChangeAudit";

	//Constants Repository Statics
	
	protected static final String defaultLogFile = "UserChangeAudit";
	 
	protected static final String COL_PERSONNEL_ID = "personnel_id";

	protected static final String COL_PERSONNEL_SHORTNAME = "short_name";
	protected static final String COL_PERSONNEL_FIRSTNAME = "first_name";
	protected static final String COL_PERSONNEL_LASTNAME = "last_name";

	protected static final String COL_CHANGE_TYPE = "change_type";  // Financial - Code
	protected static final String COL_CHANGE_TYPE_ID = "change_type_id";  // internal ID
	protected static final String COL_OBJECT_TYPE = "ol_object_type";	// Deal/template/query
	protected static final String COL_OBJECT_TYPE_ID = "ol_object_type_id";	// internal ID
	protected static final String COL_OBJECT_ID = "ol_object_id";
	protected static final String COL_OBJECT_NAME = "ol_object_name"; 	
	protected static final String COL_OBJECT_REFERENCE = "ol_object_reference"; 	
	protected static final String COL_OBJECT_STATUS = "ol_object_status";
	protected static final String COL_CHANGE_VERSION = "version_id";
	protected static final String COL_MODIFIED_DATE = "modified_date";
	protected static final String COL_PROJECT_NAME= "ol_object_details";
	
	protected static final String COL_EXPLANATION = "explanation";
	public static final String COL_REPORT_DATE = "report_date";
	public static final String COL_AUTO_FILLED = "auto_filled";

	

	
	 
}