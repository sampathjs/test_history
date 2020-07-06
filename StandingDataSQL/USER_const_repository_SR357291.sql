begin tran

go
 
-- REGIONAL LIQUIDITY
-- Update for UK regional liquidity
select * from USER_const_repository where 1=1 and context = 'Reports' and sub_context = 'Regional Liquidity Report'  and name = 'Balance Line IDs UK'
go
update USER_const_repository set string_value = '41,28,87,88,80,32,58,59,52,4,5,77,78,79' where context = 'Reports'  and sub_context = 'Regional Liquidity Report' and name = 'Balance Line IDs UK'
go
select * from USER_const_repository where 1=1 and context = 'Reports' and sub_context = 'Regional Liquidity Report'  and name = 'Balance Line IDs UK'
go
-- Update for US regional liquidity
select * from USER_const_repository where 1=1 and context = 'Reports' and sub_context = 'Regional Liquidity Report' and name = 'Balance Line IDs US'
go
update USER_const_repository set string_value = '29,83,84,85,86,87,50,41' where context = 'Reports'  and sub_context = 'Regional Liquidity Report' and name = 'Balance Line IDs US'
go
select * from USER_const_repository where 1=1 and context = 'Reports' and sub_context = 'Regional Liquidity Report' and name = 'Balance Line IDs US'
go


-- STOCK SPLIT
-- Update for new UK lines
select * from USER_const_repository where 1=1 and context = 'Reports'  and sub_context = 'Stock Split by Form'  and name = 'Balance Line IDs UK'
go
update USER_const_repository set string_value = '28,87,88,52,58,80' where context = 'Reports'  and sub_context = 'Stock Split by Form' and name = 'Balance Line IDs UK'
go 
select * from USER_const_repository where 1=1 and context = 'Reports'  and sub_context = 'Stock Split by Form'  and name = 'Balance Line IDs UK'
go
-- Update for new US lines
select * from USER_const_repository where 1=1 and context = 'Reports'  and sub_context = 'Stock Split by Form'  and name = 'Balance Line IDs US'
go
update USER_const_repository set string_value = '145,29,83,84,85,86,87' where context = 'Reports'  and sub_context = 'Stock Split by Form' and name = 'Balance Line IDs US'
go
select * from USER_const_repository where 1=1 and context = 'Reports'  and sub_context = 'Stock Split by Form'  and name = 'Balance Line IDs US'
go

commit tran