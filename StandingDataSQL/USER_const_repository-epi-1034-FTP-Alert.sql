begin tran

select count(*) from USER_const_repository

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Interfaces', 'PriceWeb', 'FTP_Alert_eMail', 2, 'GRPEndurSupportTeam@matthey.com')
GO
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Interfaces', 'PriceWeb', 'FTP_Alert_JM_Base_Price_Dataset', 2, 'JM HK Closing')
GO
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Interfaces', 'PriceWeb', 'FTP_Alert_JM_Base_Price_Dataset', 2, 'JM HK Opening')
GO
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Interfaces', 'PriceWeb', 'FTP_Alert_JM_Base_Price_Dataset', 2, 'JM London Opening')
GO
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Interfaces', 'PriceWeb', 'FTP_Alert_JM_Base_Price_Dataset', 2, 'JM NY Opening')
GO

select count(*) from USER_const_repository

go
commit tran

go

select * from USER_const_repository where [context] like '%Interfaces%'


