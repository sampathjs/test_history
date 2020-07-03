
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
per_personnel_type varchar(50),
per_pesonnel_status varchar(50),
per_personnel_version int,
per_mod_user varchar(255),
lt_full_commodity varchar(10),
lt_full_access varchar(10),
lt_read_only varchar(10),
lt_apm varchar(10),
lt_subsidiary varchar(10),
lt_connex varchar(10),
lt_server varchar(10),
sg_security_admin varchar(10),
sg_it_support varchar(10),
sg_fo_uk varchar(10),
sg_fo_hk varchar(10),
sg_fo_us varchar(10),
sg_fo_snr varchar(10),
sg_administrator varchar(10),
sg_server_user varchar(10),
sg_migration varchar(10),
sg_market_prices varchar(10),
sg_man_approval varchar(10),
sg_safe_warehouse varchar(10),
sg_eod varchar(10),
sg_stock_take varchar(10),
sg_ro_inventory varchar(10),
sg_trade_only_view varchar(10),
sg_market_user varchar(10),
sg_credit varchar(10),
sg_credit_snr varchar(10),
sg_risk varchar(10),
sg_risk_snr varchar(10),
sg_bo varchar(10),
sg_bo_us varchar(10),
sg_bo_snr varchar(10),
sg_support_elv varchar(10),
sg_role_testing varchar(10),
sg_deployment varchar(10),
sg_phys_transfer varchar(10),
per_modified_date DATETIME,
last_active_date DATETIME,
apm_last_active_date DATETIME,
login_count int,
screen_config_name varchar(50),
report_date DATETIME,
sg_connex_ws_user varchar(10),
sg_purge_tables varchar(10),
sg_fo_cn varchar(10),
sg_amp_editor varchar(10),
sg_bo_cn varchar(10),
sg_safe_warehouse_cn varchar(10),
sg_it_audit varchar(10),
per_cat varchar(50),
per_email varchar(50),
fg_gen varchar(10),
fg_tra varchar(10),
fg_ops varchar(10),
fg_cre varchar(10),
fg_ope varchar(10),
fg_well varchar(10),
fg_ca varchar(10),
fg_mag varchar(10),
fg_phk varchar(10),
fg_puk varchar(10),
fg_pus varchar(10),
fg_pcn varchar(10),
fg_tcuk varchar(10),
fg_tcus varchar(10),
fg_tchk varchar(10),
fg_tccn varchar(10),
fg_inuk varchar(10),
fg_inus varchar(10),
fg_inhk varchar(10),
fg_incn varchar(10),
fg_truk varchar(10),
fg_trus varchar(10),
fg_trhk varchar(10),
fg_trcn varchar(10),
fg_ms varchar(10),
fg_log varchar(10),
fg_lrdeal varchar(10),
fg_lrlea varchar(10),
fg_lrliq varchar(10),
fg_lrsum varchar(10)
); 


grant select, insert, update, delete on [dbo].[USER_support_personnel_audit] to olf_user, olf_user_manual 

grant select on [dbo].[USER_support_personnel_audit] to olf_readonly


CREATE INDEX idx_support_personnel_audit_h ON [USER_support_personnel_audit] (id_number,report_date); 
 
COMMIT;  


Select * from USER_support_personnel_audit





