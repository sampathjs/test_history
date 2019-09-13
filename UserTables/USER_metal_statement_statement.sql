
BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_metal_statement_statement' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_metal_statement_statement] 

GO


create table [dbo].[USER_metal_statement_statement]
(
	reference_status varchar(255)
)

grant select, insert, update, delete on [dbo].[USER_metal_statement_statement] to olf_user, olf_user_manual
grant select on [dbo].[USER_metal_statement_statement] to olf_readonly



 
COMMIT;  



SELECT * FROM USER_metal_statement_statement

select COUNT(*) from USER_metal_statement_statement

go

insert into dbo.USER_metal_statement_statement([reference_status]) values ('BLOCKED')
insert into dbo.USER_metal_statement_statement([reference_status]) values ('PROCESSING')
insert into dbo.USER_metal_statement_statement([reference_status]) values ('REPROCESS')

SELECT * FROM USER_metal_statement_statement

select COUNT(*) from USER_metal_statement_statement

go

