USE [DBA]
GO
/****** Object:  StoredProcedure [AppSupport].[ZeroPriceCheck]    Script Date: 25/07/2018 10:29:22 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER PROC [AppSupport].[ZeroPriceCheck] (@debug TINYINT = 0, @email_address VARCHAR(1000) ='charles.badcock@matthey.com')
-------------------------------------------------------
-- APPSupport.ZeroPriceCheck
-- Description: checks USER_jm_pnl_market_data spot rate and forward rate values
-- Email:  Support Team
-- Frequency:  Mon-Fri, 1am-22pm, hourly
-- Jira/Ivanti: 	Author   	Date:  		Version:  	Reason: 
-- XXXX 			C Badcock 	Sept 2019 	00			Added Header and formatting
-------------------------------------------------------

AS 
BEGIN

	SET NOCOUNT ON

	DECLARE @rowcount INT

	IF OBJECT_ID('tempdb..##ZeroPriceCheck') IS NOT NULL
	DROP TABLE ##ZeroPriceCheck

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


	SET @sql_stmt = 'SELECT ujpmd.deal_num, ujpmd.deal_leg, ujpmd.spot_rate, ujpmd.fwd_rate
					INTO ##ZeroPriceCheck
					FROM ' + @db_name + '.dbo.USER_jm_pnl_market_data ujpmd
					INNER JOIN ' + @db_name + '.dbo.ab_tran ab
					ON ujpmd.deal_num = ab.deal_tracking_num
					WHERE    ujpmd.trade_date =  CONVERT(int,DATEADD(d,DATEDIFF(d,0,GETDATE()),0)) 
					AND ab.tran_status = 3
					AND ( ujpmd.spot_rate < 0.01 OR ujpmd.fwd_rate < 0.01) 
				    ORDER BY ujpmd.deal_num'

	EXEC sp_executesql @sql_stmt

	SELECT @rowcount = @@ROWCOUNT

	IF @debug = 1 PRINT @rowcount

	IF @rowcount > 0 BEGIN

		DECLARE @email_subject NVARCHAR(100)
		DECLARE @email_query   NVARCHAR(2000)
		DECLARE @profile_name SYSNAME

		SET @email_subject = 'Warning - Zero Price Check - FAILED'

		  
		SET @email_query = 'SELECT * from ##ZeroPriceCheck'

		IF @debug = 1 PRINT @email_query

		SELECT  @profile_name =    name FROM msdb.dbo.sysmail_profile WHERE profile_id = 1

		IF @debug = 0 BEGIN
			EXEC msdb.dbo.sp_send_dbmail  @profile_name = @profile_name,@recipients = @email_address,@subject = @email_subject,@query = @email_query,@importance = 'HIGH',@attach_query_result_as_file = 1
			DROP TABLE ##ZeroPriceCheck
			RAISERROR ('ZeroPriceCheck',16,1) 
			RETURN(1)
		END
	END

	DROP TABLE ##ZeroPriceCheck

END