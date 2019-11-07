USE [DBA]
GO
/****** Object:  StoredProcedure [AppSupport].[LongRunningEOD]    Script Date: 25/07/2018 10:01:30 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER PROC [AppSupport].[LongRunningEOD] (@threshold INT = 60, @debug TINYINT = 0, @email_address VARCHAR(1000) ='charles.badcock@matthey.com')
-------------------------------------------------------
-- APPSupport.LongRunningEOD
-- Description: Long Running EOD Check checks long running EOD - default 60 mins
-- Email:  Support Team
-- Frequency:  Mon-Fri, 10pm 
-- Jira/Ivanti: 	Author   	Date:  		Version:  	Reason: 
-- XXXX 			C Badcock 	Sept 2019 	00			Added Header and formatting
-------------------------------------------------------

AS BEGIN

	SET NOCOUNT ON


	DECLARE @timediff INT

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


	SET @sql_stmt = 'SELECT @timediff =  DATEDIFF(MINUTE,run.start_time,GETDATE() )
					FROM ' + @db_name + '.dbo.bpm_definition def
					INNER JOIN ' + @db_name + '.dbo.bpm_running run
					ON run.bpm_definition_id = def.id_number
					WHERE bpm_name = ''Global EoD'''

	EXEC sp_executesql @sql_stmt, N'@timediff int out', @timediff out

	IF @debug = 1 
	BEGIN
		IF @timediff IS NULL PRINT 'Not currently running' ELSE PRINT @timediff
		SET @sql_stmt = 'SELECT * FROM  ' + @db_name + '.dbo.bpm_definition WHERE bpm_name = ''Global EoD'''
		EXEC sp_executesql @sql_stmt  
	END

	IF @timediff >= @threshold BEGIN

		DECLARE @email_subject NVARCHAR(100)
		DECLARE @profile_name SYSNAME

		DECLARE @email_db_name varchar(20)
		IF @db_name = 'OLEME00P' 
			SET @email_db_name = 'Production - '
		ELSE 
			SET @email_db_name = 'UAT - '

		SET @email_subject = 'Endur Alert : Priority = 4 :' + @email_db_name + ' DBA Warning - Long running End Of Day Check - FAILED - Delay = '+CONVERT(VARCHAR(10),@timediff) + ' minutes'

		SELECT  @profile_name =    name FROM msdb.dbo.sysmail_profile WHERE profile_id = 1

		IF @debug = 0 BEGIN
			EXEC msdb.dbo.sp_send_dbmail  @profile_name = @profile_name,@recipients = @email_address,@subject = @email_subject,@importance = 'HIGH'
			RAISERROR ('LongRunningEOD',16,1) 
			RETURN(1)
		END

	END

END

