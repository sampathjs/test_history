
BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_support_personnel_audit' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_support_personnel_audit] 

GO


create Table [dbo].[USER_support_personnel_audit] 
( 
latest_version int,
id_number int,
per_name varchar(255),
per_firstname varchar(255),
per_country varchar(255),
per_lastname varchar(255),
per_personnel_type varchar(255),
per_pesonnel_status varchar(255),
per_personnel_version int,
per_mod_user varchar(255),
lt_full_commodity varchar(255),
lt_full_access varchar(255),
lt_read_only varchar(255),
lt_apm varchar(255),
lt_subsidiary varchar(255),
lt_connex varchar(255),
lt_server varchar(255),
sg_security_admin varchar(255),
sg_it_support varchar(255),
sg_fo_uk varchar(255),
sg_fo_hk varchar(255),
sg_fo_us varchar(255),
sg_fo_snr varchar(255),
sg_administrator varchar(255),
sg_server_user varchar(255),
sg_migration varchar(255),
sg_market_prices varchar(255),
sg_man_approval varchar(255),
sg_safe_warehouse varchar(255),
sg_eod varchar(255),
sg_stock_take varchar(255),
sg_ro_inventory varchar(255),
sg_trade_only_view varchar(255),
sg_market_user varchar(255),
sg_credit varchar(255),
sg_credit_snr varchar(255),
sg_risk varchar(255),
sg_risk_snr varchar(255),
sg_bo varchar(255),
sg_bo_us varchar(255),
sg_bo_snr varchar(255),
per_modified_date DATETIME,
last_active_date DATETIME,
apm_last_active_date DATETIME,
login_count int,
screen_config_name varchar(255),
report_date DATETIME

); 


grant select, insert, update, delete on [dbo].[USER_support_personnel_audit] to olf_user, olf_user_manual 

grant select on [dbo].[USER_support_personnel_audit] to olf_readonly


CREATE INDEX idx_support_personnel_audit ON [USER_support_personnel_audit] (id_number,report_date); 
 
COMMIT;  


Select * from USER_support_personnel_audit





