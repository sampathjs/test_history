USE [DBA]
GO
/****** Object:  StoredProcedure [AppSupport].[SwapTradesFixFloatVolumeCheck]    Script Date: 12/09/2019 14:23:28 ******/
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
-- Jira/Ivanti: 	Author   	Date:  		Version:  	Reason: 
-- XXXX 			C Badcock 	Sept 2019 	00			Added Header and formatting
-------------------------------------------------------

AS BEGIN

	SET NOCOUNT ON

	DECLARE @rowcount INT

	IF  OBJECT_ID('tempdb.dbo.##SwapTradesFixFloatVolumeCheck_a' ) IS NOT NULL DROP TABLE ##SwapTradesFixFloatVolumeCheck_a
	IF  OBJECT_ID('tempdb.dbo.##SwapTradesFixFloatVolumeCheck_b' ) IS NOT NULL DROP TABLE ##SwapTradesFixFloatVolumeCheck_b

	-- Dynamic SQL to pick up the correct database name for cross-server deployment
	-- Works only if there is a database beginning with "OLEM" on the DB Server
	-- If no such database is found, the query runs for the prod database "OLEME00P"

	DECLARE @db_name varchar(20)
	declare @sql_stmt nvarchar(4000)
	declare @sql_stmt1 nvarchar(4000)
	declare @sql_stmt2 nvarchar(4000)

	-- Top 1 as test/dev DB servers have multiple databases
	-- If no such databases found, use the PROD DB name

	SELECT TOP 1 @db_name = ISNULL(name,'OLEME00P')
	FROM sys.databases
	WHERE name like 'olem%'


	SET @sql_stmt = 'SELECT ab.deal_tracking_num, ab.trade_date,sum(ip.notnl) as total_float_vol 
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

	SET @sql_stmt1 = 'SELECT ab.deal_tracking_num, ab.trade_date,SUM(ip.notnl)  as total_fixed_vol 
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

	SELECT a.deal_tracking_num, cast(a.trade_date as date) Trade_date,a.total_float_vol, b.total_fixed_vol, round(abs(a.total_float_vol-b.total_fixed_vol),2) as difference 
		FROM ##SwapTradesFixFloatVolumeCheck_a a
		INNER JOIN ##SwapTradesFixFloatVolumeCheck_b b
		ON a.deal_tracking_num = b.deal_tracking_num
		WHERE a.total_float_vol <> b.total_fixed_vol 
		ORDER BY a.deal_tracking_num

	SELECT @rowcount = @@ROWCOUNT

	IF @debug = 1 BEGIN 
		PRINT @rowcount
	END

	IF @rowcount > 0 BEGIN

		DECLARE @email_subject NVARCHAR(100)
		DECLARE @email_query   NVARCHAR(2000)
		DECLARE @profile_name SYSNAME

		SET @email_subject = 'Warning - Swap Trades Fix/Float Volume Check - FAILED - please investigate further'

		  
		SET @email_query = 'SELECT a.deal_tracking_num, cast(a.trade_date as date) Trade_date,a.total_float_vol, b.total_fixed_vol, round(abs(a.total_float_vol-b.total_fixed_vol),2) as difference 
							FROM ##SwapTradesFixFloatVolumeCheck_a a
							INNER JOIN ##SwapTradesFixFloatVolumeCheck_b b
							ON a.deal_tracking_num = b.deal_tracking_num
							WHERE a.total_float_vol <> b.total_fixed_vol 
							ORDER BY a.deal_tracking_num'

		IF @debug = 1 PRINT @email_query

		SELECT  @profile_name = name FROM msdb.dbo.sysmail_profile WHERE profile_id = 1

		IF @debug = 0 BEGIN
			EXEC msdb.dbo.sp_send_dbmail  @profile_name = @profile_name,@recipients = @email_address,@subject = @email_subject,@query = @email_query,@importance = 'HIGH',@attach_query_result_as_file = 1
			DROP TABLE ##SwapTradesFixFloatVolumeCheck_a 
			DROP TABLE ##SwapTradesFixFloatVolumeCheck_b
			RAISERROR ('SwapTradesFixFloatVolumeCheck',16,1) 
			RETURN(1)
		END

	END

	DROP TABLE ##SwapTradesFixFloatVolumeCheck_a 
	DROP TABLE ##SwapTradesFixFloatVolumeCheck_b

END



