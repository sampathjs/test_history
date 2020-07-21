
BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_audit_track_details' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_audit_track_details] 

GO


create Table [dbo].[USER_audit_track_details] 
( 
	activity_type varchar(255),
	role_requested varchar(255),
	activity_description varchar(255),
	ivanti_identifer varchar(255),
	expected_duration varchar(255),
	for_personnel_id int,
	for_short_name varchar(255),
	last_modified DATETIME,
	personnel_id int,
	short_name varchar(255),
	start_time DATETIME,
	end_time DATETIME

); 


grant select, insert, update, delete on [dbo].[USER_audit_track_details] to olf_user, olf_user_manual 

grant select on [dbo].[USER_audit_track_details] to olf_readonly


COMMIT;  


Select * from USER_audit_track_details





