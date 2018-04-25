BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_jm_ap_sell_deals_hist' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_ap_sell_deals_hist] 

GO

create Table [dbo].USER_jm_ap_sell_deals_hist 
( 
deal_num int,
volume_in_toz float,
match_status varchar(255),
volume_left_in_toz float,
match_date datetime,
customer_id int,
metal_type int,
hist_user_id int,
hist_last_update datetime,
hist_update_type smallint
); 


grant select, insert, update, delete on [dbo].[USER_jm_ap_sell_deals_hist] to olf_user, olf_user_manual

grant select on [dbo].[USER_jm_ap_sell_deals_hist] to olf_readonly

GO
 
COMMIT; 

