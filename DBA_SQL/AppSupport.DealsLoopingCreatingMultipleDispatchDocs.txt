USE [DBA]
GO
/****** Object:  StoredProcedure [AppSupport].[DealsLoopingCreatingMultipleDispatchDocs]    Script Date: 25/11/2018 10:29:22 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER PROC [AppSupport].[DealsLoopingCreatingMultipleDispatchDocs] (@debug TINYINT = 0, @email_address VARCHAR(1000) ='charles.badcock@matthey.com')
-------------------------------------------------------
-- APPSupport.DealsLoopingCreatingMultipleDispatchDocs
-- Description: Warning - Dispatch Deal looping alert
-- Email:  Support Team
-- Frequency: 1am- 9pm Every 30mins
-- Jira/Ivanti: 	Author   	Date:  		Version:  	Reason: 
-- XXXX 			C Badcock 	Sept 2019 	00			Added Header and formatting
-------------------------------------------------------

AS BEGIN

	SET NOCOUNT ON

	DECLARE @rowcount INT

	IF OBJECT_ID('tempdb..##DealsLoopingCreatingMultipleDispatchDocs') IS NOT NULL
	DROP TABLE ##DealsLoopingCreatingMultipleDispatchDocs

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


	SET @sql_stmt = 'SELECT ab.deal_tracking_num , ab.tran_num, ab.reference, ab.version_number, abh.row_creation, abh.update_type, p.name, fabh.times 
					INTO ##DealsLoopingCreatingMultipleDispatchDocs
					FROM ' + @db_name + '.dbo.ab_tran ab
					JOIN ' + @db_name + '.dbo.ab_tran_history abh ON (abh.tran_num = ab.tran_num AND abh.version_number =ab.version_number)
					JOIN ' + @db_name + '.dbo.personnel p ON (p.id_number=abh.personnel_id)
					JOIN (
						  SELECT athi.tran_num, COUNT(athi.update_type) times , MAX(athi.version_number) version_number FROM ' + @db_name + '.dbo.ab_tran_history  athi
						  WHERE  athi.row_creation  >DATEADD(minute, -30, SYSDATETIME()) 
						  AND athi.update_type = 55
						  GROUP BY athi.tran_num
						) fabh ON ( fabh.tran_num = ab.tran_num AND fabh.version_number  = ab.version_number )
					WHERE
					ab.ins_type= 48010
					AND ab.internal_portfolio IN (20045,20033)
					AND abh.row_creation  > DATEADD(minute, -30, SYSDATETIME()) 
					AND ab.trade_date  > DATEADD(month, -1, SYSDATETIME()) 
					AND fabh.times > 3'

	EXEC sp_executesql @sql_stmt

	SELECT @rowcount = @@ROWCOUNT

	IF @debug = 1 PRINT @rowcount

	IF @rowcount > 0 BEGIN

		DECLARE @email_subject NVARCHAR(100)
		DECLARE @email_query   NVARCHAR(2000)
		DECLARE @profile_name SYSNAME

		SET @email_subject = 'Warning - Dispatch Deal looping alert'
		SET @email_query = 'SELECT * from ##DealsLoopingCreatingMultipleDispatchDocs'

		IF @debug = 1 PRINT @email_query

		SELECT  @profile_name = name FROM msdb.dbo.sysmail_profile WHERE profile_id = 1

		IF @debug = 0 BEGIN
			EXEC msdb.dbo.sp_send_dbmail  @profile_name = @profile_name,@recipients = @email_address,@subject = @email_subject,@query = @email_query,@importance = 'HIGH',@attach_query_result_as_file = 1
			DROP TABLE ##DealsLoopingCreatingMultipleDispatchDocs
			RAISERROR ('DealsLoopingCreatingMultipleDispatchDocs',16,1) 
			RETURN(1)
		END

	END

	DROP TABLE ##DealsLoopingCreatingMultipleDispatchDocs

END