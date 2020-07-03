
BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_jm_sub_sector' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_sub_sector] 

GO

create Table [dbo].[USER_jm_sub_sector]
(
sub_sector varchar(255)
);

grant select, insert, update, delete on USER_jm_sub_sector to olf_user
grant select on USER_jm_sub_sector to olf_readonly
grant select,insert,update,delete on USER_jm_sub_sector to olf_user_manual

COMMIT; 