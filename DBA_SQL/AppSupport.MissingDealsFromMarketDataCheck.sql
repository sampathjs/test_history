USE [DBA]
GO
/****** Object:  StoredProcedure [AppSupport].[MissingDealsFromMarketDataCheck]    Script Date: 25/07/2018 10:27:22 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
ALTER PROC [AppSupport].[MissingDealsFromMarketDataCheck] (@debug TINYINT = 0,@email_address  varchar(1000) = 'charles.badcock@matthey.com')
-------------------------------------------------------
-- APPSupport.MissingDealsFromMarketDataCheck
-- Description: Missing Deals From Market Data Check - checks ab_tran against USER_jm_pnl_market_data for trades entered today
-- Email:  Support Team
-- Frequency: Mon-Fri, 8am-5pm, hourly
-- Jira/Ivanti: 	Author   	Date:  		Version:  	Reason: 
-- XXXX 			C Badcock 	Sept 2019 	00			Added Header and formatting
-- Jira959          C Badcock     Dec 2019     02            Added envionment agnostic
-- Jira989          C Badcock     Dec 2019     03            Compatible with email tables
-- Jira145			C Badcock	Aug 2020	04			Made compatible for v17 Production Database
-------------------------------------------------------

AS BEGIN
	SET NOCOUNT ON

	DECLARE @rowcount INT

	IF OBJECT_ID('tempdb..##MissingDealsFromMarketDataCheck') IS NOT NULL
	DROP TABLE ##MissingDealsFromMarketDataCheck
	 
	-- Dynamic SQL to pick up the correct database name for cross-server deployment
	-- Works only if there is a database beginning with "OLEM" on the DB Server
	-- If no such database is found, the query runs for the prod database "END_V17PROD"

	DECLARE @db_name varchar(20)
	declare @sql_stmt nvarchar(4000)

	-- Top 1 as test/dev DB servers have multiple databases
	-- If no such databases found, use the PROD DB name

	SELECT TOP 1 @db_name = ISNULL(name,'END_V17PROD')
	FROM sys.databases
	WHERE name like 'olem%'

	SET @sql_stmt = 'SELECT  DISTINCT deal_tracking_num
					INTO ##MissingDealsFromMarketDataCheck
					FROM ' + @db_name + '.dbo.ab_tran ab
					JOIN ' + @db_name + '.dbo.ab_tran_history abh 
						ON abh.tran_num = ab.deal_tracking_num and abh.version_number = 1
					WHERE input_date = DATEADD(d,DATEDIFF(d,0,GETDATE()),0)
					AND ab.ins_type IN ( 26001,32007)
					AND ab.tran_status in (3) 
					AND ab.tran_type  =  0 
					AND datediff(MINUTE,row_creation,getdate()) > 5
					AND ab.deal_tracking_num NOT IN 
					(SELECT DISTINCT deal_num FROM ' + @db_name + '.dbo.USER_jm_pnl_market_data UNION  SELECT DISTINCT deal_num FROM ' + @db_name + '.dbo.USER_jm_pnl_market_data_cn ) --  WHERE entry_date = CONVERT(int,DATEADD(d,DATEDIFF(d,0,GETDATE()),0)) )
					ORDER BY deal_tracking_num'
					
	EXEC sp_executesql @sql_stmt


	SELECT @rowcount = @@ROWCOUNT

	IF @debug = 1 PRINT @rowcount

	IF @rowcount > 0 BEGIN
	 
		DECLARE @email_subject NVARCHAR(100)
		DECLARE @email_query   NVARCHAR(2000)
		DECLARE @profile_name SYSNAME
		DECLARE @email_db_name varchar(20)
		DECLARE @email_count int
		DECLARE @email_address_new VARCHAR(1000)
		DECLARE @proc_name VARCHAR(500)
	
		IF @db_name = 'END_V17PROD' 
			SET @email_db_name = 'Production v17- '
		ELSE 
			SET @email_db_name = 'UAT v17- '

		SET @email_subject = 'Endur Alert : Priority = 4 :' + @email_db_name + ' DBA Warning - Missing Deals From Market Data Hourly Check - FAILED'
		  
		SET @email_query = 'SELECT * from ##MissingDealsFromMarketDataCheck ORDER BY deal_tracking_num'


		IF @debug = 1 PRINT @email_query
		
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

		SELECT  @profile_name =    name FROM msdb.dbo.sysmail_profile WHERE profile_id = 1

		IF @debug = 0 BEGIN

			EXEC msdb.dbo.sp_send_dbmail @profile_name = @profile_name, @recipients = @email_address_new, @subject = @email_subject, @query = @email_query, @importance = 'HIGH', @attach_query_result_as_file = 1

			DROP TABLE ##MissingDealsFromMarketDataCheck
			RAISERROR ('MissingDealsfromMarketDataCheck failure',16,1) 
			RETURN(1)
		END

	END

	DROP TABLE ##MissingDealsFromMarketDataCheck

END
