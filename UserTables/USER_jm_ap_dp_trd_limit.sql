BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_jm_ap_dp_trd_limit' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_ap_dp_trd_limit] 

GO

create Table [dbo].USER_jm_ap_dp_trd_limit 
( 
metal varchar(255),
pricing_type varchar(255),
trade_limit_toz float

); 


grant select, insert, update, delete on [dbo].[USER_jm_ap_dp_trd_limit] to olf_user, olf_user_manual

grant select on [dbo].[USER_jm_ap_dp_trd_limit] to olf_readonly

 GO

CREATE CLUSTERED INDEX [USER_jm_ap_dp_trd_limit] ON [dbo].[USER_jm_ap_dp_trd_limit]
(
       [metal], [pricing_type] ASC
)
GO
 
 
 
COMMIT; 

