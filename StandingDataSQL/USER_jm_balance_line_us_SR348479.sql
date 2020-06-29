begin tran

select * from USER_jm_balance_line_us
go

--################# BEGIN US LOCATION/FORM GROUPING UPDATE


-- Seperate Sponge/ING-GRA lines
-- new line for seperated accounts
-- renamed line for seperated account
-- update lines for dependent formulas using seperated accounts

insert into USER_jm_balance_line_us (id,balance_line,description,formula,display_order,display_in_drilldown)
values (83,'145.04','L145.04 West Whiteland - ING/GRA','-R83',439,'')
go

insert into USER_jm_balance_line_us (id,balance_line,description,formula,display_order,display_in_drilldown)
values (84,'145.05','L145.05 West Deptford - Sponge','-R84',440,'')
go


insert into USER_jm_balance_line_us (id,balance_line,description,formula,display_order,display_in_drilldown)
values (85,'145.06','L145.06 West Deptford - ING/GRA','-R85',441,'')
go


update USER_jm_balance_line_us set  balance_line = '145.03' , description = 'L145.03 West Whiteland - Sponge', display_order = 438 where  id = 29


go

-- update line(s) for dependent formula(s) using seperated accounts
update USER_jm_balance_line_us set formula = '+(R28,R29,R30,R31,R32,R33,R52,R53,R69,R70,R71,R72,R73,R74,R76,R78,R81,R83,R84,R85)' where  id = 34

go

update USER_jm_balance_line_us set formula = '*(/(+(R29,R83,R84,R85),R50), 100)' where  id = 41


--################# END US LOCATION/FORM GROUPING UPDATE

go
select * from USER_jm_balance_line_us
go


commit tran

-- ENDUR config update
-- account info US Balance Sheet Line

--WW-WD TRANSIT@PMM US-VF/ING
--145.04
--WW-WD TRANSIT@PMM US-VF/GRA
--145.04 
--WW-WD TRANSIT@PMM US-VF
--145.03
--WD-WW TRANSIT@PMM US-VF/ING
--145.06
--WD-WW TRANSIT@PMM US-VF/GRA
--145.06
--WD-WW TRANSIT@PMM US-VF
--145.05
--PMM US SAFE WW@PMM US-VF/ING
--145.04
--PMM US SAFE WW@PMM US-VF/GRA
--145.04
--PMM US SAFE WW@PMM US-VF
--145.03
--PMM US SAFE WD@PMM US-VF/ING
--145.06
--PMM US SAFE WD@PMM US-VF/GRA
--145.06
--PMM US SAFE WD@PMM US-VF
--145.05
--MISC RECEIPTS@PMM US-VF/ING
--145.04
--MISC RECEIPTS@PMM US-VF
--145.03

