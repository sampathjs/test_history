IF OBJECT_ID('dbo.USER_jm_confirmation_type' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_confirmation_type] 
GO
create table [dbo].[USER_jm_confirmation_type]
(
confirmation_type varchar(255)
)
grant select, insert, update, delete on [dbo].[USER_jm_confirmation_type] to olf_user
grant select on [dbo].[USER_jm_confirmation_type] to olf_readonly
GO