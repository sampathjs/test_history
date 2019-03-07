BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_strategy_deals' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_strategy_deals] 

GO
CREATE TABLE [dbo].[USER_strategy_deals](
	[Deal_num] [int] NOT NULL,
	[tran_num] [int] NOT NULL,
	[tran_status] [int] NULL,
	[Status] [varchar](255) NULL,
	[last_updated] [datetime] NULL,
	[version_number] [int] NULL,
UNIQUE NONCLUSTERED 
(
	[Deal_num] ASC,
	[tran_num] ASC,
	[tran_status] ASC,
	[version_number] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

grant select on USER_strategy_deals to olf_readonly

grant select, insert, update, delete on USER_strategy_deals to olf_user_manual


GO

SET ANSI_PADDING OFF
GO



COMMIT