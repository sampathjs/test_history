IF OBJECT_ID('dbo.USER_jm_acc_metal_mapping' , 'U') IS NOT NULL
      DROP TABLE [dbo].[USER_jm_acc_metal_mapping]
GO

create table USER_jm_acc_metal_mapping
(
internal_bu varchar(255),
trade_type varchar(255),
mode varchar(255),
metal varchar(255),
currency varchar(255),
settle_currency varchar(255),
contango_backwardation varchar(255),
buy_sell varchar(255),
o_company_code_a varchar(255),
o_core_account_a varchar(255),
o_subsidiary_a varchar(255),
o_sub_ledger_account_a varchar(255),
o_free_col_a varchar(255),
o_company_code_b varchar(255),
o_core_account_b varchar(255),
o_subsidiary_b varchar(255),
o_sub_ledger_account_b varchar(255),
o_free_col_b varchar(255),
o_company_code_c varchar(255),
o_core_account_c varchar(255),
o_subsidiary_c varchar(255),
o_core_acc_lease_c varchar(255),
o_subsidiary_lease_c varchar(255),
o_dr_cr_validated_c varchar(255),
o_dr_cr_cancelled_c varchar(255),
o_sub_ledger_account_c varchar(255),
o_free_col_c varchar(255),
o_company_code_d varchar(255),
o_company_code_d_1 varchar(255),
o_core_account_d varchar(255),
o_subsidiary_d varchar(255),
o_subsidiary_group_d varchar(255),
o_sub_ledger_account_d varchar(255),
o_free_col_d varchar(255),
o_company_code_e varchar(255),
o_core_account_e varchar(255),
o_subsidiary_e varchar(255),
o_sub_ledger_account_e varchar(255),
o_free_col_e varchar(255)
)
grant select, insert, update, delete on USER_jm_acc_metal_mapping to olf_user
grant select on USER_jm_acc_metal_mapping to olf_readonly
grant select, insert, update, delete on USER_jm_acc_metal_mapping to olf_user_manual


select * from USER_jm_acc_metal_mapping