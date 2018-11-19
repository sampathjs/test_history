
BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_support_personnel_analysis' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_support_personnel_analysis] 

GO


create Table [dbo].[USER_support_personnel_analysis] 
( 
id_number int,
per_name varchar(255),
per_firstname varchar(255),
per_lastname varchar(255),
per_country varchar(255),
per_personnel_type varchar(255),
per_pesonnel_status varchar(255),
per_personnel_version int,
per_previous_personnel_version int,
per_mod_user varchar(255),
per_modified_date DATETIME,
licence_diff varchar(255),
security_diff varchar(255),
explanation varchar(255),
report_date DATETIME

); 


grant select, insert, update, delete on [dbo].[USER_support_personnel_analysis] to olf_user, olf_user_manual 

grant select on [dbo].[USER_support_personnel_analysis] to olf_readonly


CREATE INDEX idx_support_personnel_audit ON [USER_support_personnel_analysis] (id_number,report_date); 
 
COMMIT;  

Select * from USER_support_personnel_analysis






