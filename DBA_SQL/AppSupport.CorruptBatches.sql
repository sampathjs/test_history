USE [DBA]
GO
/****** Object:  StoredProcedure [AppSupport].[CorruptBatches]    Script Date: 25/07/2018 10:27:22 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
ALTER PROC [AppSupport].[CorruptBatches] (@debug TINYINT = 0,@email_address  varchar(1000) = 'charles.badcock@matthey.com')
-------------------------------------------------------
-- APPSupport.CorruptBatches
-- Description: Identifies Corrupt Batches and alerts users
-- Email:  Support Team
-- Frequency: Mon-Fri, 10pm, Daily
-- Jira/Ivanti: 	Author   	Date:  		Version:  	Reason: 
-- XXXX 			C Badcock 	Dec 2019 	00			Initial Version
-------------------------------------------------------

AS BEGIN
	SET NOCOUNT ON

	DECLARE @rowcount INT

	IF OBJECT_ID('tempdb..##CorruptBatches') IS NOT NULL
	DROP TABLE ##CorruptBatches
	 
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



	SET @sql_stmt = 'SELECT csh.schedule_id, cb.batch_num, (tdt.ticket_id) count_container 
				FROM ' + @db_name + '.dbo.comm_schedule_header csh
					   LEFT JOIN ' + @db_name + '.dbo.tsd_delivery_ticket tdt ON (csh.schedule_id=tdt.schedule_id)
					   INNER JOIN ' + @db_name + '.dbo.comm_sched_delivery_cmotion csdc ON (csdc.delivery_id=csh.delivery_id)
					   INNER JOIN ' + @db_name + '.dbo.comm_schedule_detail csd ON (csd.schedule_id=csh.schedule_id AND csd.quantity>0.1)
					   INNER JOIN ' + @db_name + '.dbo.comm_batch cb ON (cb.batch_id=csdc.batch_id)
					   INNER JOIN ' + @db_name + '.dbo.ab_tran ab ON (ab.ins_num=csh.ins_num AND ab.tran_status IN (3, 4) AND ab.ins_type=48030)
				WHERE  
					   tdt.ticket_id is NUll
					   AND cb.batch_id NOT IN (25383, 25839, 26054)
					   AND cb.batch_id IN (       
							  SELECT distinct cb.batch_id FROM ' + @db_name + '.dbo.comm_batch cb
									 INNER JOIN ' + @db_name + '.dbo.comm_sched_delivery_cmotion csd ON (csd.batch_id=cb.batch_id)
									 LEFT JOIN ' + @db_name + '.dbo.comm_schedule_header csh2 ON (csh2.delivery_id=csd.delivery_id)
									 INNER JOIN ' + @db_name + '.dbo.ab_tran ab ON (ab.ins_num=csh2.ins_num)
							  WHERE ab.ins_type=48010
									 AND ab.tran_status IN (3,4)
									 AND ab.buy_sell=0
							  EXCEPT
									 SELECT distinct cb.batch_id FROM ' + @db_name + '.dbo.comm_batch cb
										   INNER JOIN ' + @db_name + '.dbo.comm_sched_delivery_cmotion csd ON (csd.batch_id=cb.batch_id)
										   LEFT JOIN ' + @db_name + '.dbo.comm_schedule_header csh2 ON (csh2.delivery_id=csd.delivery_id)
										   INNER JOIN ' + @db_name + '.dbo.ab_tran ab ON (ab.ins_num=csh2.ins_num)
									 WHERE ab.ins_type=48010
										   AND ab.tran_status IN (3,4)
										   AND ab.buy_sell=1
							  ) '


					
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
	
		IF @db_name = 'OLEME00P' 
			SET @email_db_name = 'Production - '
		ELSE 
			SET @email_db_name = 'UAT - '

		SET @email_subject = 'Endur Alert : Priority = 3 :' + @email_db_name + ' DBA Warning - Corrupt Batches Identified'
		  
		SET @email_query = 'SELECT * from ##CorruptBatches ORDER BY batch_num'


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

			DROP TABLE ##CorruptBatches
			RAISERROR ('CorruptBatches failure',16,1) 
			RETURN(1)
		END

	END

	DROP TABLE ##CorruptBatches

END
