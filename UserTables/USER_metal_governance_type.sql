BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_metal_governance_type' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_metal_governance_type] 

create table USER_metal_governance_type 
(
metal_governance_type varchar(255)
)

grant select, insert, update, delete on USER_metal_governance_type  to olf_user

grant select on USER_metal_governance_type  to olf_readonly

grant select, insert, update, delete on USER_metal_governance_type  to olf_user_manual
go
COMMIT; 

INSERT INTO USER_metal_governance_type  (metal_governance_type) VALUES ('Primary')
INSERT INTO USER_metal_governance_type  (metal_governance_type) VALUES ('Secondary')
INSERT INTO USER_metal_governance_type  (metal_governance_type) VALUES ('Trading')
GO

Select * from USER_metal_governance_type