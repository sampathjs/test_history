USE [DBA]
GO
/****** Object:  StoredProcedure [AppSupport].[MissedSentGLItems]    Script Date: 25/07/2018 10:25:57 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER PROC [AppSupport].[MissedSentGLItems] (@debug TINYINT = 0, @email_address VARCHAR(1000))
-------------------------------------------------------
-- APPSupport.MissedSentGLItems
-- Description:  Missed Sent GL Items Checks deals for GL activity
-- Email:  Support Team
-- Frequency:  Mon-Fri, 1am 
-- Jira/Ivanti: 	Author   	Date:  		Version:  	Reason: 
-- XXXX 			C Badcock 	Sept 2019 	00			Added Header and formatting
-- Jira959          C Badcock     Dec 2019     02            Added envionment agnostic
-- Jira989          C Badcock     Dec 2019     03            Compatible with email tables
-- Jira1197			A Agrawal	  May 2020	   04			 Excluding USD from the filters & Adding Base Metals Swaps (ab.idx_group != 3) to the filters
-- Jira145			C Badcock	Aug 2020	05			Made compatible for v17 Production Database
-------------------------------------------------------

AS BEGIN

	SET NOCOUNT ON

	DECLARE @rowcount INT

	IF OBJECT_ID('tempdb..##MissedSentGLItems') IS NOT NULL
	DROP TABLE ##MissedSentGLItems;

	-- Dynamic SQL to pick up the correct database name for cross-server deployment
	-- Works only if there is a database beginning with "OLEM" on the DB Server
	-- If no such database is found, the query runs for the prod database "END_V17PROD"

	DECLARE @db_name varchar(20)
	declare @sql_stmt nvarchar(4000)

	-- Top 1 as test/dev DB servers have multiple databases
	-- If no such databases found, use the PROD DB name

	SELECT TOP 1 @db_name = ISNULL(MIN(name),'END_V17PROD')     FROM sys.databases     WHERE name like '%PROD'


	SET @sql_stmt = 'SELECT ab.deal_tracking_num, ab.input_date, ati.value, t.name AS toolset, ccy.name AS currency, ts.name, ab.reference, ab.maturity_date, ab.last_update, p1.short_name as int_bu, p2.short_name as ext_bu
					INTO ##MissedSentGLItems
					FROM ' + @db_name + '.dbo.ab_tran ab
					INNER JOIN ' + @db_name + '.dbo.system_dates sd on 1=1
					INNER JOIN ' + @db_name + '.dbo.trans_status ts on ts.trans_status_id = ab.tran_status
					INNER JOIN ' + @db_name + '.dbo.toolsets t on t.id_number = ab.toolset
					INNER JOIN ' + @db_name + '.dbo.party p1 on p1.party_id = ab.internal_bunit
					INNER JOIN ' + @db_name + '.dbo.party p2 on p2.party_id = ab.external_bunit
					INNER JOIN ' + @db_name + '.dbo.currency ccy on ccy.id_number = ab.currency
					LEFT OUTER JOIN ' + @db_name + '.dbo.ab_tran_info ati on ati.tran_num = ab.tran_num and ati.[type_id] = 20024
					WHERE ab.tran_status IN (3,4,5) --Validated, Matured, Cancelled
					AND ab.toolset NOT IN (6,10, 35, 36) -- 10 Cash, 6 Loan Dep, 35 CallNot, 36 Commodity
					AND COALESCE(ati.value, '''') IN  (''Pending Sent'', ''Pending Cancelled'','''')
					AND ab.tran_type IN (0) -- Trading
					AND ab.internal_bunit NOT IN (20007, 20001) -- JM PMM HK, JM PMM US
					and ab.external_bunit not in (20006, 20008, 20001)-- JM PMM UK, JM PM UK
					and ab.internal_bunit <> ab.external_bunit AND ab.trade_date > ''01-01-2018''
					and ((ab.toolset != 15 and ab.input_date < sd.business_date) or (ab.toolset = 15 and ab.maturity_date  < sd.business_date and ab.idx_group != 3))
					ORDER BY ab.input_date'

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

		SET @email_subject = 'Endur Alert : Priority = 4 :' + @email_db_name + ' DBA Warning - Missed Sent GL Items - FAILED'
		  
		SET @email_query = 'SELECT * from ##MissedSentGLItems'

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
			EXEC msdb.dbo.sp_send_dbmail  @profile_name = @profile_name,@recipients = @email_address_new,@subject = @email_subject,@query = @email_query,@importance = 'HIGH',@attach_query_result_as_file = 1;
			IF OBJECT_ID('tempdb..##MissedSentGLItems') IS NOT NULL DROP TABLE ##MissedSentGLItems;
			RAISERROR ('MissedSentGLItems',16,1) 
			RETURN(1)
		END

	END

	DROP TABLE ##MissedSentGLItems

END