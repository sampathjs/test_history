IF OBJECT_ID('dbo.USER_jm_metals_email_alert_rule' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_metals_email_alert_rule] 

GO

CREATE TABLE [dbo].[USER_jm_metals_email_alert_rule](
	[task_name] [varchar](255) NULL,
	[rule_description] [varchar](255) NULL,
	[monitoring_window] [int] NULL,
	[grid_point] [varchar](10) NULL,
	[threshold_lower_limit] [int] NULL,
	[threshold_upper_limit] [int] NULL,
	[customer_balance_reporting] [varchar](2) NULL,
	[active] [char](2) NULL
) ON [PRIMARY]
GO

grant insert, select, update, delete on [dbo].[USER_jm_metals_email_alert_rule] to olf_user
grant select on [dbo].[USER_jm_metals_email_alert_rule] to olf_readonly
GO




