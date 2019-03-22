
BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_support_change_audit' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_support_change_audit] 

GO


create Table [dbo].[USER_support_change_audit] 
( 
change_type varchar(255),
ol_object_type varchar(255),
ol_object_name varchar(255),
ol_object_status varchar(255),
ol_object_reference varchar(255),
ol_object_details varchar(255),
short_name varchar(255),
modified_date DATETIME,
ol_object_id bigint,
version_id int,
change_type_id int,
ol_object_type_id int,
personnel_id   int,
first_name varchar(255),
last_name varchar(255),
explanation varchar(255),
report_date DATETIME

); 


grant select, insert, update, delete on [dbo].[USER_support_change_audit] to olf_user, olf_user_manual 

grant select on [dbo].[USER_support_change_audit] to olf_readonly


CREATE INDEX idx_support_personnel_audit ON [USER_support_change_audit] (personnel_id,ol_object_id,report_date, change_type_id,ol_object_type_id); 
 
COMMIT;  


Select * from USER_support_change_audit





