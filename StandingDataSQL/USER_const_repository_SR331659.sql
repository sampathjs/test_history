begin tran

go
 
-- Add entry for the new prices webpage alert

select * from USER_const_repository where 1=1 and context = 'Alerts' and sub_context = 'PriceWebpageValidation'  
go

insert into USER_const_repository values ('Alerts','PriceWebpageValidation','jm_prices_url',2,'http://www.platinum.matthey.com/prices/price-tables',0,0,null)

go
insert into USER_const_repository values ('Alerts','PriceWebpageValidation','email_recipients',2,'Endur_Support',0,0,null)

go
select * from USER_const_repository where 1=1 and context = 'Alerts' and sub_context = 'PriceWebpageValidation'  
go


commit tran