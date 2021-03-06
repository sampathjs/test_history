USE [DBA]
GO
/****** Object:  StoredProcedure [AppSupport].[FxSettleDateCheck]    Script Date: 25/07/2018 09:59:41 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER  PROC [AppSupport].[FxSettleDateCheck] (@debug TINYINT = 0, @email_address VARCHAR(1000) ='charles.badcock@matthey.com')
-------------------------------------------------------
-- APPSupport.FxSettleDateCheck
-- Description: Swap settle dates do not match - Please check and amend Deal(s)
-- Email:  Support Team
-- Frequency: Mon-Fri, Mon-Fri, 1am-22pm, 3 hours 
-- Jira/Ivanti: 	Author   	Date:  		Version:  	Reason: 
-- XXXX 			C Badcock 	Sept 2019 	00			Added Header and formatting
-- Jira959          C Badcock     Dec 2019     02            Added envionment agnostic
-- Jira989          C Badcock     Dec 2019     03            Compatible with email tables
-- Jira1305			I Fernandes	  June 2020    04		Update to exclude deals booked before fix for fx swap pass thru deals
-- Jira145			C Badcock	Aug 2020	04			Made compatible for v17 Production Database
-------------------------------------------------------

AS BEGIN

	SET NOCOUNT ON

	DECLARE @rowcount INT

	IF OBJECT_ID('tempdb..##FxSettleDateCheck') IS NOT NULL
	DROP TABLE ##FxSettleDateCheck

	-- Dynamic SQL to pick up the correct database name for cross-server deployment
	-- Works only if there is a database beginning with "OLEM" on the DB Server
	-- If no such database is found, the query runs for the prod database "END_V17PROD"

	DECLARE @db_name varchar(20)
	declare @sql_stmt nvarchar(4000)

	-- Top 1 as test/dev DB servers have multiple databases
	-- If no such databases found, use the PROD DB name

	SELECT TOP 1 @db_name = ISNULL(MIN(name),'END_V17PROD')     FROM sys.databases     WHERE name like '%PROD'

	SET @sql_stmt = 'SELECT ab.deal_tracking_num, ab.tran_status, ab.ins_type, ab.currency AS ab_Currency, ate.currency AS ate_currency, ab.tran_num, ate.para_position, ab.trade_date, ate.event_date, ab.settle_date, ates.accounting_date, ates.nostro_date, ate.para_date  
					INTO ##FxSettleDateCheck
					FROM ' + @db_name + '.dbo.ab_tran ab
					INNER JOIN ' + @db_name + '.dbo.ab_tran_event ate ON ate.tran_num = ab.tran_num
					INNER JOIN ' + @db_name + '.dbo.ab_tran_event_settle ates ON ates.event_num = ate.event_num
					INNER JOIN ' + @db_name + '.dbo.system_dates sd ON 1 = 1
					WHERE ab.settle_date <> ate.event_date
					AND ab.tran_status in (3,4)
					AND ate.event_type = 14
					AND ab.ins_type not in (47006, 48010, 12101, 12100, 30201)
					AND ate.currency in (53, 54, 55, 56, 58, 6, 62, 63)
					AND ab.settle_date >= sd.business_date
					AND ab.input_date > ''11-Jul-20'''

	EXEC sp_executesql @sql_stmt
	SELECT @rowcount = @@ROWCOUNT

	IF @debug = 1 PRINT @rowcount

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

		SET @email_subject = 'Endur Alert : Priority = 4 :' + @email_db_name + ' DBA Warning ??? Swap settle dates do not match - Please check and amend Deal(s)'
		  
		SET @email_query = 'SELECT * from ##FxSettleDateCheck ORDER BY deal_tracking_num'

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
			DROP TABLE ##FxSettleDateCheck
			RAISERROR ('FxSettleDateCheck',16,1) 
			RETURN(1)
		END

	END

	DROP TABLE ##FxSettleDateCheck

END