BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_jm_ap_sell_deals' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_ap_sell_deals] 

GO

create Table [dbo].USER_jm_ap_sell_deals 
( 
deal_num int,
volume_in_toz float,
match_status varchar(255),
volume_left_in_toz float,
match_date datetime,
customer_id int,
metal_type int
); 
GO

grant select, insert, update, delete on [dbo].[USER_jm_ap_sell_deals] to olf_user, olf_user_manual

grant select on [dbo].[USER_jm_ap_sell_deals] to olf_readonly
GO
 CREATE CLUSTERED INDEX [USER_jm_ap_sell_deals] ON [dbo].[USER_jm_ap_sell_deals]
(
       [deal_num], [metal_type] ASC
)
GO

COMMIT; 

