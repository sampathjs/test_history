
BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_jm_ref_src' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_ref_src] 

GO


create Table [dbo].[USER_jm_ref_src] 
( 
ref_src varchar(255),
metal varchar(255),
ccy varchar(255),
src_idx varchar(255),
target_idx varchar(255)
); 


grant select, insert, update, delete on [dbo].[USER_jm_ref_src] to olf_user, olf_user_manual

grant select on [dbo].[USER_jm_ref_src] to olf_readonly


--CREATE INDEX idx_lbma ON [USER_jm_lbma_log] (deal_num,last_update); 
 
COMMIT;  

exec master.dbo.AssignEndurDefaultUserTablePermissions 'OLEME00P','USER_jm_ref_src'


go




-- lme am
-- xpt
insert into dbo.USER_jm_ref_src([ref_src],[metal],[ccy],[src_idx],[target_idx]) values ('LME AM','XPT','USD','PX_XPT_LME_AM.USD','NON_JM_USD_Price')
go
insert into dbo.USER_jm_ref_src([ref_src],[metal],[ccy],[src_idx],[target_idx]) values ('LME AM','XPT','GBP','PX_XPT_LME_AM.GBP','NON_JM_GBP_Price')
go
insert into dbo.USER_jm_ref_src([ref_src],[metal],[ccy],[src_idx],[target_idx]) values ('LME AM','XPT','EUR','PX_XPT_LME_AM.EUR','NON_JM_EUR_Price')
go

-- xpd
insert into dbo.USER_jm_ref_src([ref_src],[metal],[ccy],[src_idx],[target_idx]) values ('LME AM','XPD','USD','PX_XPD_LME_AM.USD','NON_JM_USD_Price')
go
insert into dbo.USER_jm_ref_src([ref_src],[metal],[ccy],[src_idx],[target_idx]) values ('LME AM','XPD','GBP','PX_XPD_LME_AM.GBP','NON_JM_GBP_Price')
go
insert into dbo.USER_jm_ref_src([ref_src],[metal],[ccy],[src_idx],[target_idx]) values ('LME AM','XPD','EUR','PX_XPD_LME_AM.EUR','NON_JM_EUR_Price')
go

--select * from dbo.USER_jm_ref_src




-- lme pm
-- xpt
insert into dbo.USER_jm_ref_src([ref_src],[metal],[ccy],[src_idx],[target_idx]) values ('LME PM','XPT','USD','PX_XPT_LME_PM.USD','NON_JM_USD_Price')
go
insert into dbo.USER_jm_ref_src([ref_src],[metal],[ccy],[src_idx],[target_idx]) values ('LME PM','XPT','GBP','PX_XPT_LME_PM.GBP','NON_JM_GBP_Price')
go
insert into dbo.USER_jm_ref_src([ref_src],[metal],[ccy],[src_idx],[target_idx]) values ('LME PM','XPT','EUR','PX_XPT_LME_PM.EUR','NON_JM_EUR_Price')
go

-- xpd
insert into dbo.USER_jm_ref_src([ref_src],[metal],[ccy],[src_idx],[target_idx]) values ('LME PM','XPD','USD','PX_XPD_LME_PM.USD','NON_JM_USD_Price')
go
insert into dbo.USER_jm_ref_src([ref_src],[metal],[ccy],[src_idx],[target_idx]) values ('LME PM','XPD','GBP','PX_XPD_LME_PM.GBP','NON_JM_GBP_Price')
go
insert into dbo.USER_jm_ref_src([ref_src],[metal],[ccy],[src_idx],[target_idx]) values ('LME PM','XPD','EUR','PX_XPD_LME_PM.EUR','NON_JM_EUR_Price')


-- lbma am
insert into dbo.USER_jm_ref_src([ref_src],[metal],[ccy],[src_idx],[target_idx]) values ('LBMA AM','XAU','USD','PX_XAU_LBMA_AM.USD','NON_JM_USD_Price')
go
insert into dbo.USER_jm_ref_src([ref_src],[metal],[ccy],[src_idx],[target_idx]) values ('LBMA AM','XAU','EUR','PX_XAU_LBMA_AM.EUR','NON_JM_EUR_Price')
go
insert into dbo.USER_jm_ref_src([ref_src],[metal],[ccy],[src_idx],[target_idx]) values ('LBMA AM','XAU','GBP','PX_XAU_LBMA_AM.GBP','NON_JM_GBP_Price')
go

-- lbma pm
insert into dbo.USER_jm_ref_src([ref_src],[metal],[ccy],[src_idx],[target_idx]) values ('LBMA PM','XAU','USD','PX_XAU_LBMA_PM.USD','NON_JM_USD_Price')
go
insert into dbo.USER_jm_ref_src([ref_src],[metal],[ccy],[src_idx],[target_idx]) values ('LBMA PM','XAU','EUR','PX_XAU_LBMA_PM.EUR','NON_JM_EUR_Price')
go
insert into dbo.USER_jm_ref_src([ref_src],[metal],[ccy],[src_idx],[target_idx]) values ('LBMA PM','XAU','GBP','PX_XAU_LBMA_PM.GBP','NON_JM_GBP_Price')
go


-- lbma silver
insert into dbo.USER_jm_ref_src([ref_src],[metal],[ccy],[src_idx],[target_idx]) values ('LBMA Silver','XAG','USD','PX_XGU_LBMA_XAG.USD','NON_JM_USD_Price')
go
insert into dbo.USER_jm_ref_src([ref_src],[metal],[ccy],[src_idx],[target_idx]) values ('LBMA Silver','XAG','EUR','PX_XGU_LBMA_XAG.EUR','NON_JM_EUR_Price')
go
insert into dbo.USER_jm_ref_src([ref_src],[metal],[ccy],[src_idx],[target_idx]) values ('LBMA Silver','XAG','GBP','PX_XGU_LBMA_XAG.GBP','NON_JM_GBP_Price')
go


select * from USER_jm_ref_src order by ref_src,metal, ccy, target_idx

