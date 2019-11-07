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
-------------------------------------------------------

AS BEGIN

	SET NOCOUNT ON

	DECLARE @rowcount INT

	IF OBJECT_ID('tempdb..##FxSettleDateCheck') IS NOT NULL
	DROP TABLE ##FxSettleDateCheck

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
					AND ab.settle_date >= sd.business_date'

	EXEC sp_executesql @sql_stmt
	SELECT @rowcount = @@ROWCOUNT

	IF @debug = 1 PRINT @rowcount

	IF @rowcount > 0 BEGIN

		DECLARE @email_subject NVARCHAR(100)
		DECLARE @email_query   NVARCHAR(2000)
		DECLARE @profile_name SYSNAME

		DECLARE @email_db_name varchar(20)
		IF @db_name = 'OLEME00P' 
			SET @email_db_name = 'Production - '
		ELSE 
			SET @email_db_name = 'UAT - '

		SET @email_subject = 'Endur Alert : Priority = 4 :' + @email_db_name + ' DBA Warning – Swap settle dates do not match - Please check and amend Deal(s)'
		  
		SET @email_query = 'SELECT * from ##FxSettleDateCheck ORDER BY deal_tracking_num'

		IF @debug = 1 PRINT @email_query

		SELECT  @profile_name =    name FROM msdb.dbo.sysmail_profile WHERE profile_id = 1


		IF @debug = 0 BEGIN
			EXEC msdb.dbo.sp_send_dbmail  @profile_name = @profile_name,@recipients = @email_address,@subject = @email_subject,@query = @email_query,@importance = 'HIGH',@attach_query_result_as_file = 1
			DROP TABLE ##FxSettleDateCheck
			RAISERROR ('FxSettleDateCheck',16,1) 
			RETURN(1)
		END

	END

	DROP TABLE ##FxSettleDateCheck

END