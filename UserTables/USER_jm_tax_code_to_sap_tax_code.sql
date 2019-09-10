create table USER_jm_tax_code_to_sap_tax_code
(
tax_type varchar(255),
tax_subtype varchar(255),
instrument_types varchar(255),
buy_sell varchar(255),
ext_bu varchar(255),
o_tax_code varchar(255),
int_bu varchar(255)
)

grant select, insert, update, delete on USER_jm_tax_code_to_sap_tax_code to olf_user

grant select on USER_jm_tax_code_to_sap_tax_code to olf_readonly

