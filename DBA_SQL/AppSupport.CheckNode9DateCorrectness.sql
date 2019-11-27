
USE [DBA]
GO
/****** Object:  StoredProcedure [AppSupport].[CheckNode9DateCorrectness]    Script Date: 25/07/2018 10:28:47 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

IF EXISTS (
	SELECT * FROM sysobjects WHERE name = 'CheckNode9DateCorrectness' 
)
BEGIN
	DROP proc [AppSupport].CheckNode9DateCorrectness
END
GO

CREATE PROC [AppSupport].[CheckNode9DateCorrectness] (@debug TINYINT = 0,@email_address  varchar(1000) = 'charles.badcock@matthey.com')
-------------------------------------------------------
-- APPSupport.CheckNode9DateCorrectness
-- Description: Polls the runsite datetime to determine if the date is stuck on a date other than main processing date
-- Email:  Support Team
-- Frequency: Mon-Fri, 1 am - 9pm  every 5 mins 
-- Jira/Ivanti: 	Author   	Date:  		Version:  	Reason: 
-- SR287360/J944	C Badcock 	Oct 2019 	00			First Draft
-- Jira959          C Badcock     Dec 2019     02            Added envionment agnostic
-- Jira989          C Badcock     Dec 2019     03            Compatible with email tables
-------------------------------------------------------

AS BEGIN
	SET NOCOUNT ON

	DECLARE @rowcount INT

	IF OBJECT_ID('tempdb..##CheckNode9DateCorrectness') IS NOT NULL
	DROP TABLE ##CheckNode9DateCorrectness

	 

 

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

SET @sql_stmt = 'SELECT sm.service_name, sd.processing_date sys_process_date, sm.curr_date server_date, smh_prior.prior_last_update prior_last_update, 
				sm.last_update, GETDATE(), DATEDIFF(minute, smh_prior.prior_last_update, sm.last_update) AS previous_min_diff ,  DATEDIFF(minute, sm.last_update, GETDATE()) AS current_min_diff 
				FROM  ' + @db_name + '.dbo.system_dates sd, ' + @db_name + '.dbo.service_mgr sm
				JOIN ( 
				   SELECT smh.id, smh.service_name,  MAX(smh.last_update)  prior_last_update
				   FROM ' + @db_name + '.dbo.service_mgr_h  smh 
				   JOIN (
					  SELECT smh.id, smh.service_name,  MAX(smh.last_update)  last_update
					  FROM ' + @db_name + '.dbo.service_mgr_h  smh 
					  GROUP BY smh.id, smh.service_name 
				   )  smh_max ON (smh_max.id=smh.id AND smh_max.last_update !=smh.last_update)
				GROUP BY smh.id, smh.service_name ) smh_prior ON (smh_prior.id=sm.id) 
				WHERE sm.curr_date != sd.processing_date
				AND  DATEDIFF(minute, sm.last_update, GETDATE())  > 0
				AND DATEDIFF(SECOND, CONVERT(date, sm.last_update) , sm.last_update) < DATEDIFF(second, 0,  DATEADD(Day, 0 , ''21:00:00'' )) 
				AND sm.service_name = ''GBROMEOLG09P_x64_DateChange'''

				 
					
	EXEC sp_executesql @sql_stmt	

	SELECT @rowcount = @@ROWCOUNT

	IF @debug = 1 PRINT @rowcount

	IF @rowcount > 0 
	BEGIN
	 
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
	
		SET @email_subject = 'Endur Alert : Priority = 4 :' + @email_db_name + ' DBA Warning - Node9 has a stuck date - restart runsite 9'
		  
		SET @email_query = 'SELECT * from ##CheckNode9DateCorrectness'


		IF @debug = 1 PRINT @email_query

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
			DROP TABLE ##CheckNode9DateCorrectness
			RAISERROR ('CheckNode9DateCorrectness',16,1) 
			RETURN(1)
		END

	END

	DROP TABLE ##CheckNode9DateCorrectness

END
