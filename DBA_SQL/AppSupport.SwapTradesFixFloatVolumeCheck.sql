USE [DBA]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
ALTER PROC [AppSupport].[SwapTradesFixFloatVolumeCheck] (@debug TINYINT = 0, @email_address VARCHAR(1000) ='charles.badcock@matthey.com')
-------------------------------------------------------
-- APPSupport.SwapTradesFixFloatVolumeCheck
-- Description:  Swap Trades Fix/Float Volumes Check fix/float volume check  for last 2 months trades
-- Email:  Support Team
-- Frequency: Mon-Fri, 4 pm
-- Jira/Ivanti:     Author       Date:          Version:      Reason: 
-- XXXX             C Badcock     Sept 2019     00            Added Header and formatting
-- Jira959          C Badcock     Dec 2019     02            Added envionment agnostic
-- Jira989          C Badcock     Dec 2019     03            Compatible with email tables
-- Jira1198         S Khanna      May 2022     04            Added rounding check for floating precision
-- Jira145			C Badcock	Aug 2020	04			Made compatible for v17 Production Database
-------------------------------------------------------




AS BEGIN

	SET NOCOUNT ON

	DECLARE @rowcount INT

	IF  OBJECT_ID('tempdb.dbo.##SwapTradesFixFloatVolumeCheck_a' ) IS NOT NULL DROP TABLE ##SwapTradesFixFloatVolumeCheck_a
	IF  OBJECT_ID('tempdb.dbo.##SwapTradesFixFloatVolumeCheck_b' ) IS NOT NULL DROP TABLE ##SwapTradesFixFloatVolumeCheck_b

	-- Dynamic SQL to pick up the correct database name for cross-server deployment
	-- Works only if there is a database beginning with "OLEM" on the DB Server
	-- If no such database is found, the query runs for the prod database "END_V17PROD"

	DECLARE @db_name varchar(20)
	declare @sql_stmt nvarchar(4000)
	declare @sql_stmt1 nvarchar(4000)
	declare @sql_stmt2 nvarchar(4000)

	-- Top 1 as test/dev DB servers have multiple databases
	-- If no such databases found, use the PROD DB name

	SELECT TOP 1 @db_name = ISNULL(name,'END_V17PROD')
		FROM sys.databases
		WHERE name like 'olem%'


	SET @sql_stmt = 'SELECT ab.deal_tracking_num, ab.trade_date, ROUND(SUM(ip.notnl), 6) AS total_float_vol 
					INTO ##SwapTradesFixFloatVolumeCheck_a
					FROM ' + @db_name + '.dbo.ins_parameter  ip 
					INNER JOIN ' + @db_name + '.dbo.ab_tran ab
					ON ab.ins_num = ip.ins_num
					WHERE   ip.fx_flt = 1 
					AND ab.toolset =  15
					AND ab.tran_status = 3 
					AND (ab.trade_date <= GETDATE()
					AND ab.trade_date > DATEADD(month,-2,GETDATE()) )
					GROUP BY ab.deal_tracking_num, ab.trade_date'
	EXEC sp_executesql @sql_stmt

	SET @sql_stmt1 = 'SELECT ab.deal_tracking_num, ab.trade_date, ROUND(SUM(ip.notnl), 6) AS total_fixed_vol 
					INTO ##SwapTradesFixFloatVolumeCheck_b
					FROM ' + @db_name + '.dbo.ins_parameter   ip
					INNER JOIN ' + @db_name + '.dbo.ab_tran ab
					ON ab.ins_num = ip.ins_num
					WHERE ip.fx_flt = 0
					AND ab.toolset =  15
					AND ab.tran_status = 3 
					AND (ab.trade_date <= GETDATE()
					AND ab.trade_date > DATEADD(month,-2,GETDATE()) )
					GROUP BY ab.deal_tracking_num, ab.trade_date'
	EXEC sp_executesql @sql_stmt1

	SELECT @rowcount = count(*)
		FROM ##SwapTradesFixFloatVolumeCheck_a a
		INNER JOIN ##SwapTradesFixFloatVolumeCheck_b b
		ON a.deal_tracking_num = b.deal_tracking_num
		WHERE a.total_float_vol <> b.total_fixed_vol 
		--ORDER BY a.deal_tracking_num

	IF @debug = 1 BEGIN 
	   PRINT @rowcount
	END

	IF @rowcount > 0 BEGIN

		DECLARE @email_subject NVARCHAR(100)
		DECLARE @email_query   NVARCHAR(2000)
		DECLARE @profile_name SYSNAME
		DECLARE @email_count int
		DECLARE @email_address_new VARCHAR(1000)
		DECLARE @proc_name VARCHAR(500)

		

		DECLARE @email_db_name varchar(20)
		IF @db_name = 'END_V17PROD' 
			SET @email_db_name = 'Production v17- '
		ELSE 
			SET @email_db_name = 'UAT v17- '

		SET @email_subject = 'Endur Alert : Priority = 4 :' + @email_db_name + ' DBA Warning - Swap Trades Fix/Float Volume Check - FAILED - please investigate further'

		  
		SET @email_query = 'SELECT a.deal_tracking_num, cast(a.trade_date as date) Trade_date, a.total_float_vol, b.total_fixed_vol, ROUND(ABS(a.total_float_vol-b.total_fixed_vol), 6) AS difference 
					FROM ##SwapTradesFixFloatVolumeCheck_a a
					INNER JOIN ##SwapTradesFixFloatVolumeCheck_b b
					ON a.deal_tracking_num = b.deal_tracking_num
					WHERE a.total_float_vol <> b.total_fixed_vol 
					ORDER BY a.deal_tracking_num'

		IF @debug = 1 PRINT @email_query

		
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
			EXEC msdb.dbo.sp_send_dbmail  @profile_name = @profile_name,@recipients = @email_address_new,@subject = @email_subject,@query = @email_query,@importance = 'HIGH',@attach_query_result_as_file = 1
			DROP TABLE ##SwapTradesFixFloatVolumeCheck_a 
			DROP TABLE ##SwapTradesFixFloatVolumeCheck_b
			RAISERROR ('SwapTradesFixFloatVolumeCheck',16,1) 
			RETURN(1)
		END

	END

	DROP TABLE ##SwapTradesFixFloatVolumeCheck_a 
	DROP TABLE ##SwapTradesFixFloatVolumeCheck_b

END
