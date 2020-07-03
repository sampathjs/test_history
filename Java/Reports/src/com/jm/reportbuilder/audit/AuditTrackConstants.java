package com.jm.reportbuilder.audit;

public class AuditTrackConstants {

	
	public static final String USER_AUDIT_TRACK_DETAILS="USER_audit_track_details";
	
	public static final String CONST_REPO_CONTEXT = "Support_AuditTrack";


	public static final String COL_Activity_Type = "activity_type";
	public static final String COL_Role_Requested = "role_requested";	
	public static final String COL_Activity_Description = "activity_description";
	public static final String COL_Ivanti_Identifier = "ivanti_identifer";
	public static final String COL_Expected_Duration = "expected_duration";

	public static final String COL_For_Personnel_ID = "for_personnel_id";
	public static final String COL_For_Short_Name = "for_short_name";

	// these three columns are set automatically internally into the user table
	public static final String COL_Last_Modified = "last_modified";
	public static final String COL_Personnel_ID = "personnel_id";
	public static final String COL_Short_Name = "short_name";
	
	public static final String COL_start_time = "start_time";
	public static final String COL_end_time = "end_time";
	
	

	public static final String ACTIVITY_PERSONNEL_CHANGE = "Personnel Change";
	public static final String ACTIVITY_PERSONNEL_CHANGE_END = "Personnel Change End";
	public static final String ACTIVITY_ELEVATED_RIGHTS = "Elevated Rights Requested";
	public static final String ACTIVITY_ELEVATED_RIGHTS_REMOVED = "Elevated Rights Remove";
	public static final String ACTIVITY_EMDASH = "Emdash Deployment";
	public static final String ACTIVITY_EMDASH_ENDED = "Emdash Ended";
	
	public static final String ACTIVITY_EOD_PROCESS = "EOD Process";	
	public static final String ACTIVITY_STATIC_DATA = "Static Data";	
	public static final String ACTIVITY_CMM_IMPORT = "CMM CR Deployment";
	public static final String ACTIVITY_CONFIG_DEPLOYMENT = "Config Change";
	
	public static final String ACTIVITY_BLANK1 = "";
	

	public static final String ROLE_ELEVATED = "IT Support Elavated";
	public static final String ROLE_EOD = "EOD";
	public static final String ROLE_DEPLOYMENT = "Deployment";
	public static final String ROLE_IT_SUPPORT = "IT Support";
	public static final String ROLE_AUDIT = "IT Support Audit";
	public static final String LOG_LEVEL_DEBUG = "Debug";

	public static final String DEFAULT_EMDASH_USER = "SRV_RODAS01";
	
}
