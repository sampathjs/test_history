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
internal_portfolio varchar(255),
pymt_type varchar(255),
contango_backwardation varchar(255),
buy_sell varchar(255),
o_customer_code_ccy varchar(255),
o_company_ccy varchar(255),
o_trade_subject_matter varchar(255),
o_company_code_a varchar(255),
o_comp_prem_a varchar(255),
o_core_account_a varchar(255),
o_core_prem_a varchar(255),
o_core_swap_a varchar(255),
o_subsidiary_a varchar(255),
o_sub_prem_a varchar(255),
o_sub_form_expense_a varchar(255),
o_sub_loco_expense_a varchar(255),
o_sub_repo_a varchar(255),
o_sub_form_income_a varchar(255),
o_sub_loco_income_a varchar(255),
o_sub_ledger_account_a varchar(255),
o_free_col_a varchar(255),
o_company_code_b varchar(255),
o_core_account_b varchar(255),
o_core_swap_pay_b varchar(255),
o_core_swap_rcv_b varchar(255),
o_subsidiary_b varchar(255),
o_sub_fut_b varchar(255),
o_sub_repo_b varchar(255),
o_sub_form_b varchar(255),
o_sub_loco_b varchar(255),
o_sub_ledger_account_b varchar(255),
o_free_col_b varchar(255),
o_company_code_c varchar(255),
o_core_account_c varchar(255),
o_subsidiary_c varchar(255),
o_core_acc_lease_c varchar(255),
o_subsidiary_lease_c varchar(255),
o_dr_cr_validated_c varchar(255),
o_dr_cr_cancelled_c varchar(255),
o_corr_core_c varchar(255),
o_corr_sub_c varchar(255),
o_corr_dr_cr_val_c varchar(255),
o_corr_dr_cr_can_c varchar(255),
o_sub_ledger_account_c varchar(255),
o_free_col_c varchar(255),
o_company_code_d varchar(255),
o_company_code_d_1 varchar(255),
o_core_account_d varchar(255),
o_subsidiary_d varchar(255),
o_core_acc_group_d varchar(255),
o_sub_group_d varchar(255),
o_sub_ledger_account_d varchar(255),
o_free_col_d varchar(255),
o_company_code_e varchar(255),
o_core_account_e varchar(255),
o_subsidiary_e varchar(255),
o_sub_ledger_account_e varchar(255),
o_free_col_e varchar(255),
o_company_code_f varchar(255),
o_core_account_f varchar(255),
o_subsidiary_f varchar(255),
o_sub_mat_ccy_f varchar(255),
o_sub_ledger_account_f varchar(255),
o_free_col_f varchar(255),
o_company_code_tax varchar(255),
o_core_rcv_tax varchar(255),
o_core_pay_tax varchar(255),
o_core_reverse_tax varchar(255),
o_core_other_tax varchar(255),
o_free_col_tax varchar(255)
)

grant select, insert, update, delete on USER_jm_acc_metal_mapping to olf_user
grant select on USER_jm_acc_metal_mapping to olf_readonly
grant select, insert, update, delete on USER_jm_acc_metal_mapping to olf_user_manual


select * from USER_jm_acc_metal_mapping