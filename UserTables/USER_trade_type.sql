
BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_trade_type' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_trade_type] 

GO


create table [dbo].[USER_trade_type]
(
	tradetype varchar(255)
)

grant select, insert, update, delete on [dbo].[USER_trade_type] to olf_user, olf_user_manual
grant select on [dbo].[USER_trade_type] to olf_readonly



 
COMMIT;  



SELECT * FROM USER_trade_type

select COUNT(*) from USER_trade_type

go

insert into dbo.USER_trade_type([tradetype]) values ('Hedged')
insert into dbo.USER_trade_type([tradetype]) values ('Unhedged')
insert into dbo.USER_trade_type([tradetype]) values ('Financial')

SELECT * FROM USER_trade_type

select COUNT(*) from USER_trade_type

go

