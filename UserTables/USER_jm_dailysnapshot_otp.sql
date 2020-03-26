IF OBJECT_ID('dbo.USER_jm_dailysnapshot_otp' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_dailysnapshot_otp] 

GO

CREATE TABLE [dbo].[USER_jm_dailysnapshot_otp]
(
extract_id int,
extract_date int,
extract_time int,
bunit int,
metal_ccy int,
open_date int,
open_volume float,
open_price float,
open_value float,
close_date int
)


GRANT SELECT, INSERT, UPDATE, DELETE ON [dbo].[USER_jm_dailysnapshot_otp] to olf_user, olf_user_manual;

GRANT SELECT ON [dbo].[USER_jm_dailysnapshot_otp] to olf_readonly;

IF EXISTS (
    SELECT 1
    FROM sys.indexes 
    WHERE object_id = OBJECT_ID('dbo.USER_jm_dailysnapshot_otp')
    AND name='USER_jm_dailysnapshot_otp')
DROP INDEX USER_jm_dailysnapshot_otp ON USER_jm_dailysnapshot_otp
GO

CREATE CLUSTERED INDEX [USER_jm_dailysnapshot_otp] ON [dbo].[USER_jm_dailysnapshot_otp]
(
       [extract_date] ASC
)

SELECT * FROM USER_jm_dailysnapshot_otp
