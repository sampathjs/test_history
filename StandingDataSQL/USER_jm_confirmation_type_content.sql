BEGIN TRANSACTION
DELETE FROM [dbo].[USER_jm_confirmation_type]
INSERT INTO [dbo].[USER_jm_confirmation_type]
           ([confirmation_type])
    VALUES ('Manual')
GO
INSERT INTO [dbo].[USER_jm_confirmation_type]
           ([confirmation_type])
    VALUES ('Email Link')
GO
INSERT INTO [dbo].[USER_jm_confirmation_type]
           ([confirmation_type])
   VALUES ('Swift')
GO
COMMIT 
SELECT * FROM [dbo].[USER_jm_confirmation_type]
