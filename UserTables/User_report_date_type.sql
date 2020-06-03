BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_report_datetype' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_report_datetype]
GO
CREATE TABLE [dbo].[USER_report_datetype](
	[Report_DateType] [varchar](255) NOT NULL,
	
	
PRIMARY KEY CLUSTERED

(

       [Report_DateType] ASC

)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]

) ON [PRIMARY]

grant select on [USER_report_datetype] to olf_readonly

grant select, insert, update, delete on [USER_report_datetype] to olf_user_manual

GO

SET ANSI_PADDING OFF
GO


COMMIT
 