begin tran

select count(*) from USER_const_repository

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('Alerts','TransferValidation','exclude_tran',2,'499837,499830,55721,55739,55885,55920,55927,55920,55927,55920,55927,57227,57227,57911,58263,58263,59806,59836,59806,59836,59806,59836,60740,61601,61664,61664,62211,62211,296375,195768,278396')
go

select count(*) from USER_const_repository

go
commit tran

go

select * from USER_const_repository where context like '%Alerts%'


