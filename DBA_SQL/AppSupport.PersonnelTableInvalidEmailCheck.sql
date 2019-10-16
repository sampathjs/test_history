USE [DBA]
GO
/****** Object:  StoredProcedure [AppSupport].[PersonnelTableInvalidEmailCheck]    Script Date: 25/07/2018 10:28:22 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER PROC [AppSupport].[PersonnelTableInvalidEmailCheck] (@debug TINYINT = 0, @email_address VARCHAR(1000) ='charles.badcock@matthey.com')
-------------------------------------------------------
-- APPSupport.PersonnelTableInvalidEmailCheck
-- Description: Personnel Table Invalid Email Check checks personnel table rows contain valid email address
-- Email:  Support Team
-- Frequency: Mon-Fri, 1pm and 4 pm
-- Jira/Ivanti: 	Author   	Date:  		Version:  	Reason: 
-- XXXX 			C Badcock 	Sept 2019 	00			Added Header and formatting
-------------------------------------------------------

AS BEGIN

	SET NOCOUNT ON

	DECLARE @rowcount INT

	IF OBJECT_ID('tempdb..##PersonnelTableInvalidEmailCheck') IS NOT NULL
	DROP TABLE ##PersonnelTableInvalidEmailCheck

	-- Dynamic SQL to pick up the correct database name for cross-server deployment
	-- Works only if there is a database beginning with "OLEM" on the DB Server
	-- If no such database is found, the query runs for the prod database "OLEME00P"

	DECLARE @db_name varchar(20)
	declare @sql_stmt nvarchar(4000)

	-- Top 1 as test/dev DB servers have multiple databases
	-- If no such databases found, use the PROD DB name

	SELECT TOP 1 @db_name = ISNULL(name,'OLEME00P')
	FROM sys.databases
	WHERE name like 'olem%'


	SET @sql_stmt = 'SELECT [name], first_name + last_name AS [User], email
					INTO ##PersonnelTableInvalidEmailCheck
					FROM ' + @db_name + '.dbo.personnel
					WHERE LTRIM(RTRIM(email)) LIKE ''% %''
					OR ( email NOT LIKE ''%_@__%.__%'' AND email <> '' '' )
					ORDER BY 2'

	EXEC sp_executesql @sql_stmt

	SELECT @rowcount = @@ROWCOUNT


	IF @debug = 1 PRINT @rowcount

	IF @rowcount > 0 BEGIN

		DECLARE @email_subject NVARCHAR(100)
		DECLARE @email_query   NVARCHAR(2000)
		DECLARE @profile_name SYSNAME

		SET @email_subject = 'Warning - Invalid email address found in personnel table'
		  
		SET @email_query = 'SELECT * from ##PersonnelTableInvalidEmailCheck'

		IF @debug = 1 PRINT @email_query

		SELECT  @profile_name = name FROM msdb.dbo.sysmail_profile WHERE profile_id = 1

		IF @debug = 0 BEGIN
			EXEC msdb.dbo.sp_send_dbmail  @profile_name = @profile_name,@recipients = @email_address,@subject = @email_subject,@query = @email_query,@importance = 'HIGH',@attach_query_result_as_file = 1
			IF OBJECT_ID('tempdb..##PersonnelTableInvalidEmailCheck') IS NOT NULL DROP TABLE ##PersonnelTableInvalidEmailCheck
			RAISERROR ('PersonnelTableInvalidEmailCheck',16,1) 
			RETURN(1)
		END

	END

	IF OBJECT_ID('tempdb..##PersonnelTableInvalidEmailCheck') IS NOT NULL DROP TABLE ##PersonnelTableInvalidEmailCheck

END



 