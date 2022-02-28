----------------------

IF OBJECT_ID('dbo.USER_jm_confirmation_processing' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_confirmation_processing] 

GO
create table [dbo].[USER_jm_confirmation_processing]
(
document_id int,
action_id_confirm varchar(255),
action_id_dispute varchar(255),
email_status_id int,
version int,
current_flag int,
inserted_at datetime,
last_update datetime
)
grant select, insert, update, delete on [dbo].[USER_jm_confirmation_processing] to olf_user
grant select on [dbo].[USER_jm_confirmation_processing] to olf_readonly
GO

IF EXISTS (
    SELECT 1
    FROM sys.indexes 
    WHERE object_id = OBJECT_ID('dbo.USER_jm_confirmation_processing')
    AND name='USER_jm_confirmation_processing_action_id')
DROP INDEX USER_jm_confirmation_processing_action_id ON USER_jm_confirmation_processing
GO

CREATE CLUSTERED INDEX [USER_jm_confirmation_processing_action_id] ON [dbo].[USER_jm_confirmation_processing]
(
	[action_id_confirm] ASC,
	[document_id] ASC,
	[action_id_dispute] ASC,
	[current_flag] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
