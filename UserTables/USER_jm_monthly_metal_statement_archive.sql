BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_jm_monthly_metal_statement_archive' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_monthly_metal_statement_archive] 

GO

CREATE TABLE [dbo].[USER_jm_monthly_metal_statement_archive](
	[reference] [varchar](255) NULL,
	[account_id] [int] NULL,
	[nostro_vostro] [int] NULL,
	[statement_period] [varchar](255) NULL,
	[external_lentity] [int] NULL,
	[internal_lentity] [varchar](255) NULL,
	[internal_bunit] [int] NULL,
	[metal_statement_production_date] [datetime] NULL,
	[output_path] [varchar](255) NULL,
	[output_files] [varchar](255) NULL,
	[files_generated]  [int] NULL,
	[last_modified] [datetime] NULL,
	[run_detail] [varchar](255) NULL,
) ON [PRIMARY]

grant select, insert, update, delete on [dbo].[USER_jm_monthly_metal_statement_archive] to olf_user, olf_user_manual
grant select on [dbo].[USER_jm_monthly_metal_statement_archive] to olf_readonly

CREATE INDEX idx_jm_monthly_metal_statement_archive ON [USER_jm_monthly_metal_statement_archive] (account_id,external_lentity,internal_bunit); 
 
GO

SET ANSI_PADDING OFF
GO

COMMIT

SELECT * FROM USER_jm_monthly_metal_statement_archive
SELECT COUNT(*) FROM USER_jm_monthly_metal_statement_archive


