
BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_jm_sector' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_sector] 

GO

create Table [dbo].[USER_jm_sector]
(
sector varchar(255)
);

grant select, insert, update, delete on USER_jm_sector to olf_user
grant select on USER_jm_sector to olf_readonly
grant select,insert,update,delete on USER_jm_sector to olf_user_manual

COMMIT; 