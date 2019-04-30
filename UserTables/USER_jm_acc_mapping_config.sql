------------------------------------------------------------------------------
-- Account Mapping Table
------------------------------------------------------------------------------
IF OBJECT_ID('dbo.USER_jm_acc_mapping_config' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_acc_mapping_config] 

GO

create table USER_jm_acc_mapping_config
(
o_rule_id int,
o_scenario_id varchar(255),
mode varchar(255),
ins_type varchar(255),
buy_sell varchar(255),
tran_status varchar(255),
trade_type varchar(255),
event_type varchar(255),
settle_currency varchar(255),
from_currency varchar(255),
to_currency varchar(255),
ext_bu_jm_group varchar(255),
free_mapping_col2 varchar(255),
free_mapping_col3 varchar(255),
free_mapping_col4 varchar(255),
o_account_type varchar(255),
o_item_type varchar(255),
o_document_type varchar(255),
o_account_number varchar(255),
o_reversal_code varchar(255),
o_cost_center varchar(255),
o_event_num_grouping varchar(255),
o_doc_ccy_amount varchar(255),
o_doc_ccy_base_amount varchar(255),
o_doc_ccy_base_currency varchar(255),
o_doc_ccy_tax_amount varchar(255),
o_doc_ccy_tax_currency varchar(255),
o_partner_code varchar(255),
o_partner_type varchar(255),
o_grouping_item int,
o_value_date varchar(255),
o_grouping_document int,
o_free_int_col2 int,
o_baseline_date varchar(255),
o_buy_sell_note varchar(255)
)

grant select, insert, update, delete on USER_jm_acc_mapping_config to olf_user,olf_user_manual

grant select on USER_jm_acc_mapping_config to olf_readonly

select * from USER_jm_acc_mapping_config
 
select COUNT(*) from USER_jm_acc_mapping_config
