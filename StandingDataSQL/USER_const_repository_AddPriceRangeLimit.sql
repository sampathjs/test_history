begin tran

select count(*) from USER_const_repository

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[double_value]) values ('Interfaces','PriceWeb','price_range_limit',1,2.5)
insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[double_value]) values ('Interfaces','PriceWeb','price_range_limit',1,2.5)


go
select count(*) from USER_const_repository

go
commit tran

go

select * from USER_const_repository 


