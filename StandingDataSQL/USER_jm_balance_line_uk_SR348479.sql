begin tran

select * from USER_jm_balance_line_uk
go


-- ################################ BEGIN POLAND UPDATE
-- Seperate out Poland Balance Line
-- new lines for seperated accounts
insert into USER_jm_balance_line_uk (id,balance_line,description,formula,display_order,display_in_drilldown)
values (86,'102','L102 Poland ECT','-R86',191,'')
go
-- update line(s) for dependent formula(s) using seperated accounts
update USER_jm_balance_line_uk set formula = '+(R11,R12,R13,R14,R15,R16,R17,R18,R19,R20,R21,R22,R23,R24,R25,R26,R81,R83,R84,R85,R86)' where id = 27
go
-- ################################ END POLAND UPDATE


-- ############################### BEGIN UK FORM GROUPING UPDATE
-- Seperate Sponge/ING-GRA lines
-- new line for seperated account
-- renamed line for seperated account
-- update lines for dependent formulas using seperated accounts

insert into USER_jm_balance_line_uk (id,balance_line,description,formula,display_order,display_in_drilldown)
values (87,'140.02','L140.02 Stock UK - ING/GRA','-R87',271,'')
go

update USER_jm_balance_line_uk set  balance_line = '140.01' , description = 'L140.01 Stock UK - Sponge' where  id = 28
go

-- update line(s) for dependent formula(s) using seperated accounts

update USER_jm_balance_line_uk set formula = '+(R28,R87,R29,R30,R31,R32,R33,R52,R53,R54,R55,R56,R57,R58,R59,R60,R61,R62,R63,R64,R65,R66,R67,R68,R80,R82)' where  id = 34
go
update USER_jm_balance_line_uk set formula = '*(/(+(R28,R87,R80,R32,R58,R59,R52),+(R4,R5)), 100)' where  id = 41
go
--################################ END UK FORM GROUPING UPDATE


select * from USER_jm_balance_line_uk
go


commit tran




-- ENDUR config update
-- account info UK Balance Sheet Line

-- JM CA POLAND@PMM UK-ROY
-- 102
-- MISC SHIPMENTS @PMM UK-ROY
-- 140.01
-- PMM UK SAFE@PMM UK-ROY/GRA
-- 140.02
-- PMM UK SAFE@PMM UK-ROY/ING
-- 140.02