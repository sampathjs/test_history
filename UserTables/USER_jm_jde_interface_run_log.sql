IF OBJECT_ID('dbo.USER_jm_jde_interface_run_log' , 'U') IS NOT NULL
      DROP TABLE [dbo].[USER_jm_jde_interface_run_log]
GO
----

create table USER_jm_jde_interface_run_log
(
extraction_id int,
interface_mode varchar(255),
region varchar(255),
internal_bunit int,
deal_num int,
endur_doc_num int,
trade_date int,
metal_value_date int,
cur_value_date int,
account_num varchar(255),
qty_toz float,
ledger_amount float,
tax_amount float,
debit_credit varchar(255),
ledger_type varchar(255),
time_in datetime,
doc_date int
)

 
grant select, insert, update, delete on USER_jm_jde_interface_run_log to olf_user
grant select on USER_jm_jde_interface_run_log to olf_readonly
grant select, insert, update, delete on USER_jm_jde_interface_run_log to olf_user_manual

select * from USER_jm_jde_interface_run_log