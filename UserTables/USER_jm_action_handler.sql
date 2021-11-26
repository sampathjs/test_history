IF OBJECT_ID('dbo.USER_jm_action_handler' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_action_handler] 
GO
create table [dbo].[USER_jm_action_handler]
(
action_id varchar(255),
action_consumer varchar(255),
response_message varchar(4000),
created_at datetime,
expires_at datetime
)
GO
grant select, insert, update, delete on [dbo].[USER_jm_action_handler] to olf_user
grant select on [dbo].[USER_jm_action_handler] to olf_readonly
GO

IF EXISTS (
    SELECT 1
    FROM sys.indexes 
    WHERE object_id = OBJECT_ID('dbo.USER_jm_action_handler')
    AND name='USER_jm_action_handler-actionId')
DROP INDEX USER_jm_action_handler-actionId ON USER_jm_action_handler
GO

CREATE CLUSTERED INDEX [USER_jm_action_handler-actionId] ON [dbo].[USER_jm_action_handler]
(
	[action_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
