USE [DBA]
GO
/****** Object:  StoredProcedure [AppSupport].[Missing_User_JM_trading_pnl_history]    Script Date: 25/07/2018 10:26:33 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER PROC [AppSupport].[Missing_User_JM_trading_pnl_history] (@debug TINYINT = 0,@threshold INT = 1000, @email_address  varchar(1000) = 'charles.badcock@matthey.com')
-------------------------------------------------------
-- APPSupport.Missing_User_JM_trading_pnl_history 
-- Description: Missing_User_JM_trading_pnl_history Potentially missing rows in user_jm_trading_pnl_history
-- Email:  Support Team
-- Frequency: Mon-Fri,  1am -> 9pm, 10 Mins
-- Jira/Ivanti: 	Author   	Date:  		Version:  	Reason: 
-- XXXX 			C Badcock 	Sept 2019 	00			Added Header and formatting
-- XXXX 			C Badcock 	Oct 2019 	00			Corrected the output to the rowcout variable
-- Jira959          C Badcock     Dec 2019     02            Added envionment agnostic
-- Jira989          C Badcock     Dec 2019     03            Compatible with email tables
-------------------------------------------------------

AS BEGIN
	SET NOCOUNT ON
	DECLARE @rowcnt INT

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


	SET @sql_stmt = 'select @rowcnt = COUNT(*) FROM [' + @db_name + '].[dbo].[user_jm_trading_pnl_history]'

	EXEC sp_executesql @sql_stmt, N'@rowcnt INT OUTPUT',@rowcnt OUTPUT

	PRINT @rowcnt

	IF @rowcnt < @threshold BEGIN
	 
		DECLARE @email_subject NVARCHAR(100)
		DECLARE @email_query   NVARCHAR(2000)
		DECLARE @profile_name SYSNAME
		DECLARE @email_db_name varchar(20)
		DECLARE @email_count int
		DECLARE @email_address_new VARCHAR(1000)
		DECLARE @proc_name VARCHAR(500)
	
		IF @db_name = 'OLEME00P' 
			SET @email_db_name = 'Production - '
		ELSE 
			SET @email_db_name = 'UAT - '
			
		SET @email_subject = 'Endur Alert : Priority = 3 :' + @email_db_name + ' DBA Error - Potentially missing rows in user_jm_trading_pnl_history'
		  
		SET @email_query = 'SELECT COUNT(*) AS RowsInTable FROM [' + @db_name + '].[dbo].[user_jm_trading_pnl_history]'

		SELECT  @profile_name = name FROM msdb.dbo.sysmail_profile WHERE profile_id = 1
		
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

			EXEC msdb.dbo.sp_send_dbmail @profile_name = @profile_name, @recipients = @email_address_new, @subject = @email_subject, @query = @email_query, @importance = 'HIGH', @attach_query_result_as_file = 1

			RAISERROR ('MissingData from user_jm_trading_pnl_history',16,1) 
			RETURN(1)
		END

	END


END