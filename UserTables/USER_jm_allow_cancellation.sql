------------------------------------------------------------------------------
-- jm Allow Cancellation
------------------------------------------------------------------------------


IF OBJECT_ID('dbo.USER_jm_allow_cancellation' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_allow_cancellation] 

GO

CREATE TABLE dbo.USER_jm_allow_cancellation  (
       deal_number int not null primary key,
       ticket_ref varchar(25)not null,
       cancellation_comment nvarchar(2000)not null,
       requested_by varchar(25)not null,
       authorized_by varchar(25)not null,
       updated_by varchar(25) not null,
       last_updated datetime not null
)

GRANT SELECT,INSERT,UPDATE,DELETE ON [dbo].USER_jm_allow_cancellation TO [olf_readonly] AS [dbo]
GO
GRANT SELECT,INSERT,UPDATE,DELETE ON [dbo].USER_jm_allow_cancellation TO [olf_user] AS [dbo]
GO
GRANT SELECT,INSERT,UPDATE,DELETE ON [dbo].USER_jm_allow_cancellation TO [olf_user_manual] AS [dbo]
GO


SELECT * from USER_jm_allow_cancellation
 
SELECT COUNT(*) from USER_jm_allow_cancellation
