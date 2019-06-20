
IF OBJECT_ID('dbo.USER_strategy_reportdata' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_strategy_reportdata] 

GO
CREATE TABLE [dbo].[USER_strategy_reportdata](
	[deal_num] [int] NOT NULL,
	[cash_expected] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[deal_num] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO

grant select on USER_strategy_reportdata to olf_readonly

grant select, insert, update, delete on USER_strategy_reportdata to olf_user_manual,olf_user


GO

SET ANSI_PADDING OFF
GO


