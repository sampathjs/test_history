USE [DBA]
GO
/****** Object:  StoredProcedure [AppSupport].[LongRunningAutoConfirmCheck]    Script Date: 25/07/2018 10:00:13 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER PROC [AppSupport].[LongRunningAutoConfirmCheck] (@threshold INT = 10, @debug TINYINT = 0, @email_address VARCHAR(1000) ='charles.badcock@matthey.com')
-------------------------------------------------------
-- APPSupport.LongRunningAutoConfirmCheck
-- Description: Long Running Auto Confirm Check checks long running TPM - default threshold 10 mins
-- Email:  Support Team
-- Frequency: Mon-Fri, 1am-22pm, 20 mins
-- Jira/Ivanti: 	Author   	Date:  		Version:  	Reason: 
-- XXXX 			C Badcock 	Sept 2019 	00			Added Header and formatting
-- Jira959          C Badcock     Dec 2019     02            Added envionment agnostic
-- Jira989          C Badcock     Dec 2019     03            Compatible with email tables
-- Jira145			C Badcock	Aug 2020	04			Made compatible for v17 Production Database
-------------------------------------------------------

AS BEGIN

	SET NOCOUNT ON


	DECLARE @timediff INT

	-- Dynamic SQL to pick up the correct database name for cross-server deployment
	-- Works only if there is a database beginning with "OLEM" on the DB Server
	-- If no such database is found, the query runs for the prod database "END_V17PROD"

	DECLARE @db_name varchar(20)
	declare @sql_stmt nvarchar(4000)

	-- Top 1 as test/dev DB servers have multiple databases
	-- If no such databases found, use the PROD DB name

	SELECT TOP 1 @db_name = ISNULL(MIN(name),'END_V17PROD')     FROM sys.databases     WHERE name like '%PROD'


	SET @sql_stmt = 'SELECT @timediff =  DATEDIFF(MINUTE,run.start_time,GETDATE() )
					FROM ' + @db_name + '.dbo.bpm_definition def
					INNER JOIN ' + @db_name + '.dbo.bpm_running run
					ON run.bpm_definition_id = def.id_number
					WHERE bpm_name = ''Automated Confirmation Processing'''

	EXEC sp_executesql @sql_stmt, N'@timediff int out', @timediff out

	IF @debug = 1 BEGIN
		IF @timediff IS NULL PRINT 'Not currently running' ELSE PRINT @timediff
		SET @sql_stmt = 'SELECT * FROM  ' + @db_name + '.dbo.bpm_definition WHERE bpm_name = ''Automated Confirmation Processing'''
		EXEC sp_executesql @sql_stmt
	END

	IF @timediff >= @threshold BEGIN

		DECLARE @email_subject NVARCHAR(100)
		DECLARE @profile_name SYSNAME
		DECLARE @email_db_name varchar(20)
		DECLARE @email_count int
		DECLARE @email_address_new VARCHAR(1000)
		DECLARE @proc_name VARCHAR(500)

		IF @db_name = 'END_V17PROD' 
			SET @email_db_name = 'Production V17- '
		ELSE 
			SET @email_db_name = 'UAT v17- '

		SET @email_subject = 'Endur Alert : Priority = 4 :' + @email_db_name + ' DBA Warning - Long running Auto Confirm Intraday TPM Process Check - FAILED - Delay = '+CONVERT(VARCHAR(10),@timediff) + ' minutes'

		SELECT  @profile_name =    name FROM msdb.dbo.sysmail_profile WHERE profile_id = 1
		
		-- Check if there are any valid active users mapped to the alert. If not, default to Endur Support email id
		SET @proc_name = OBJECT_NAME(@@PROCID)
		SELECT @email_count = COUNT(*)
			FROM AppSupport.Uvw_AlertUserMapping
			WHERE AlertName = @proc_name AND UserActive = 1

		IF @email_count = 0
			SET @email_address_new = 'GRPEndurSupportTeam@matthey.com'
		ELSE
			SELECT @email_address_new = stuff(list,1,1,'')
				FROM(
					SELECT ';' + CAST(UserEmail AS varchar(500))
					FROM AppSupport.Uvw_AlertUserMapping
					WHERE AlertName = @proc_name AND UserActive = 1
					FOR XML PATH('')
				) Sub(list)


		IF @debug = 1 PRINT @email_address_new


		IF @debug = 0 BEGIN
			EXEC msdb.dbo.sp_send_dbmail  @profile_name = @profile_name,@recipients = @email_address_new,@subject = @email_subject,@importance = 'HIGH'
			RAISERROR ('LongRunningAutoConfirmCheck',16,1) 
			RETURN(1)
		END

	END
 
END