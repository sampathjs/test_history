BEGIN TRANSACTION
DELETE FROM USER_jm_confirmation_status
INSERT INTO [dbo].[USER_jm_confirmation_status]
           ([email_status_id] ,[email_status_name])
    VALUES ( 0,          'Open' )
GO
INSERT INTO [dbo].[USER_jm_confirmation_status]
           ([email_status_id] ,[email_status_name])
    VALUES ( 1,          'Confirmed' )
GO
INSERT INTO [dbo].[USER_jm_confirmation_status]
           ([email_status_id] ,[email_status_name])
   VALUES  ( 2,          'Disputed' )
GO
COMMIT 
SELECT * FROM USER_jm_confirmation_status