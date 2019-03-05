BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_Strategy_Deals' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_Strategy_Deals] 

GO
CREATE TABLE [dbo].[USER_Strategy_Deals](
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

grant select, insert, update, delete on [dbo].[USER_jm_emir_log] to olf_user, olf_user_manual

grant select on [dbo].[USER_jm_emir_log] to olf_readonly


GO

SET ANSI_PADDING OFF
GO



