IF OBJECT_ID('dbo.USER_jm_tax_code_to_sap_tax_code' , 'U') IS NOT NULL
      DROP TABLE [dbo].[USER_jm_tax_code_to_sap_tax_code]
GO
----

create table USER_jm_tax_code_to_sap_tax_code
(
tax_type varchar(255),
tax_subtype varchar(255),
instrument_types varchar(255),
cflow_type varchar(255),
buy_sell varchar(255),
int_bu varchar(255),
ext_bu varchar(255),
cpy_is_jm_group varchar(255),
o_tax_code varchar(255)
)

grant select, insert, update, delete on USER_jm_tax_code_to_sap_tax_code to olf_user
grant select on USER_jm_tax_code_to_sap_tax_code to olf_readonly
grant select, insert, update, delete on USER_jm_tax_code_to_sap_tax_code to olf_user_manual

select * from USER_jm_tax_code_to_sap_tax_code