
begin tran

select count(*) from USER_const_repository

go

update USER_const_repository set string_value = 'jyotsna.walia@kwa-analytics.com' where string_value  = 'vivek.chauhan@kwa-analytics.com' and sub_context = 'EmailTest'

go

update USER_const_repository set string_value = 'jyotsna.walia@matthey.com' where string_value  = 'vivek.chauhan@matthey.com' and sub_context = 'EmailTest'

go

select count(*) from USER_const_repository

go
commit tran

go