------------------------------------------------------------------------------
-- sap tax code mapping table
------------------------------------------------------------------------------
IF OBJECT_ID('dbo.USER_jm_tax_code_to_sap_tax_code' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_tax_code_to_sap_tax_code] 

GO

create table USER_jm_tax_code_to_sap_tax_code
(
tax_type varchar(255),
tax_subtype varchar(255),
instrument_types varchar(255),
buy_sell varchar(255),
ext_bu varchar(255),
o_tax_code varchar(255)
)

grant select, insert, update, delete on USER_jm_tax_code_to_sap_tax_code to olf_user,olf_user_manual

grant select on USER_jm_tax_code_to_sap_tax_code to olf_readonly




select * from USER_jm_tax_code_to_sap_tax_code
 
select COUNT(*) from USER_jm_tax_code_to_sap_tax_code
 


