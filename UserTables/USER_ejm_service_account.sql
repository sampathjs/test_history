DECLARE @roleName NVARCHAR(50)
DECLARE @sql NVARCHAR(MAX)
SELECT @roleName = name
    FROM sys.database_principals
    WHERE name = 'olf_user_manual'
       OR name = 'olf_user' AND type = 'R'

IF OBJECT_ID('USER_ejm_service_account', 'U') IS NOT NULL DROP TABLE USER_ejm_service_account
CREATE TABLE USER_ejm_service_account
(
    username VARCHAR(255),
    password VARCHAR(255)
)
SET @sql = 'GRANT SELECT, INSERT, UPDATE, DELETE ON USER_ejm_service_account TO ' + @roleName
EXEC (@sql)
GRANT SELECT ON USER_ejm_service_account TO olf_readonly
