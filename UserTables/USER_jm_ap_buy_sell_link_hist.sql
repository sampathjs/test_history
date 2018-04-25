BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_jm_ap_buy_sell_link_hist' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_ap_buy_sell_link_hist] 

GO

create Table [dbo].USER_jm_ap_buy_sell_link_hist 
( 
buy_deal_num int,
sell_deal_num int,
match_volume float,
match_date datetime,
metal_type int,
buy_ins_type int,
sell_price float,
settle_amount float,
sell_event_num bigint,
invoice_doc_num int,
invoice_status varchar(255),
last_update datetime,
hist_user_id int,
hist_last_update datetime,
hist_update_type smallint
); 


grant select, insert, update, delete on [dbo].[USER_jm_ap_buy_sell_link_hist] to olf_user, olf_user_manual

grant select on [dbo].[USER_jm_ap_buy_sell_link_hist] to olf_readonly

 GO
 
COMMIT; 

