BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_jm_implied_efp' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_implied_efp] 

GO

create Table [dbo].USER_jm_implied_efp
( 
deal_num int primary key,
implied_efp float not null,
last_update datetime default getdate()
); 


grant select, insert, update, delete on [dbo].[USER_jm_implied_efp] to olf_user, olf_user_manual

grant select on [dbo].[USER_jm_implied_efp] to olf_readonly

 GO
 
COMMIT; 