Select * from USER_jm_tax_sub_type_trades

-- This statement will add a new column called "internal_bunit" to USER_jm_tax_sub_type_trades position 3
-- This is used for SA Legal Ent removal

IF COL_LENGTH('USER_jm_tax_sub_type_trades','internal_bunit') IS NULL
 BEGIN 
	
	
	-- add a new column
	ALTER TABLE USER_jm_tax_sub_type_trades ADD internal_bunit VARCHAR(255);

	-- create copies of the existing columns you want to move
	ALTER TABLE USER_jm_tax_sub_type_trades ADD buy_sell_new VARCHAR(255) 
	ALTER TABLE USER_jm_tax_sub_type_trades ADD ext_le_region_new VARCHAR(255)
	ALTER TABLE USER_jm_tax_sub_type_trades ADD metal_new VARCHAR(255) 
	ALTER TABLE USER_jm_tax_sub_type_trades ADD lbma_new VARCHAR(255)
	ALTER TABLE USER_jm_tax_sub_type_trades ADD lppm_new VARCHAR(255) 
	ALTER TABLE USER_jm_tax_sub_type_trades ADD cash_flow_type_new VARCHAR(255)
	ALTER TABLE USER_jm_tax_sub_type_trades ADD tax_subtype_new VARCHAR(255)
	

	-- transfer data to the new column
	DECLARE @SQL NVARCHAR(4000)
	SET @SQL='UPDATE USER_jm_tax_sub_type_trades SET buy_sell_new = buy_sell, ext_le_region_new = ext_le_region, metal_new = metal, lbma_new = lbma , lppm_new = lppm, cash_flow_type_new = cash_flow_type , tax_subtype_new = tax_subtype'
	EXEC(@SQL)
	
	SET @SQL='UPDATE USER_jm_tax_sub_type_trades SET internal_bunit = '''' '
	EXEC(@SQL)

	

	-- remove the originals
	ALTER TABLE USER_jm_tax_sub_type_trades DROP COLUMN buy_sell 
	ALTER TABLE USER_jm_tax_sub_type_trades DROP COLUMN ext_le_region
	ALTER TABLE USER_jm_tax_sub_type_trades DROP COLUMN metal
	ALTER TABLE USER_jm_tax_sub_type_trades DROP COLUMN lbma
	ALTER TABLE USER_jm_tax_sub_type_trades DROP COLUMN lppm 
	ALTER TABLE USER_jm_tax_sub_type_trades DROP COLUMN cash_flow_type
	ALTER TABLE USER_jm_tax_sub_type_trades DROP COLUMN tax_subtype 

	-- rename the new columns
	EXEC SP_RENAME 'USER_jm_tax_sub_type_trades.buy_sell_new', 'buy_sell'; 
	EXEC SP_RENAME 'USER_jm_tax_sub_type_trades.ext_le_region_new', 'ext_le_region';
	EXEC SP_RENAME 'USER_jm_tax_sub_type_trades.metal_new', 'metal'; 
	EXEC SP_RENAME 'USER_jm_tax_sub_type_trades.lbma_new', 'lbma';
	EXEC SP_RENAME 'USER_jm_tax_sub_type_trades.lppm_new', 'lppm'; 
	EXEC SP_RENAME 'USER_jm_tax_sub_type_trades.cash_flow_type_new', 'cash_flow_type';
	EXEC SP_RENAME 'USER_jm_tax_sub_type_trades.tax_subtype_new', 'tax_subtype'; 


 END
 
 
 Select * from USER_jm_tax_sub_type_trades

grant select, insert, update, delete on USER_jm_tax_sub_type_trades to olf_user
GO
grant select on USER_jm_tax_sub_type_trades to olf_readonly 
GO


	-- ALTER TABLE USER_jm_tax_sub_type_trades DROP COLUMN internal_bunit 
