
------------------------------------------------------------------------------
-- material number mapping table
------------------------------------------------------------------------------
IF OBJECT_ID('dbo.USER_jm_acc_sap_material_number' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_acc_sap_material_number] 

GO

create table USER_jm_acc_sap_material_number
(
currency varchar(255),
form varchar(255),
uom varchar(255),
o_sap_uom varchar(255),
o_sap_material_number varchar(255)
)

grant select, insert, update, delete on USER_jm_acc_sap_material_number to olf_user,olf_user_manual

grant select on USER_jm_acc_sap_material_number to olf_readonly


select * from USER_jm_acc_sap_material_number
 
select COUNT(*) from USER_jm_acc_sap_material_number