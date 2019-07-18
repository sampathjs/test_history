create table dbo.USER_jm_sap_autopop_hist_prices

(
	base_curve					varchar(255),
	base_reference_source		varchar(255),
	fx_curve					varchar(255),
	fx_grid_point				varchar(255),	
	closing_dataset				varchar(255),
	target_price_curve			varchar(255),
	target_reference_source		varchar(255)

)
go

grant update, insert, delete, select on dbo.USER_jm_sap_autopop_hist_prices to olf_user
grant update, insert, delete, select on dbo.USER_jm_sap_autopop_hist_prices to olf_readonly
grant update, insert, delete, select on dbo.USER_jm_sap_autopop_hist_prices to olf_user_manual
go


