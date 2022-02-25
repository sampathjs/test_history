IF OBJECT_ID('dbo.USER_jm_confirmation_status' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_confirmation_status] 

GO
create table [dbo].[USER_jm_confirmation_status] 
(
email_status_id int,
email_status_name varchar(255)
)
grant select, insert, update, delete on [dbo].[USER_jm_confirmation_status] to olf_user
grant select on [dbo].[USER_jm_confirmation_status] to olf_readonly
GO