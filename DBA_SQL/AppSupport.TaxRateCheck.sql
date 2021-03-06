USE [DBA]
GO
/****** Object:  StoredProcedure [AppSupport].[TaxRateCheck]    Script Date: 25/07/2018 10:28:47 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER PROC [AppSupport].[TaxRateCheck] (@debug TINYINT = 0,@email_address  varchar(1000) = 'charles.badcock@matthey.com')
-------------------------------------------------------
-- APPSupport.TaxRateCheck
-- Description: Tax Rate Check Failed - Please one-step amend deal(s)
-- Email:  Support Team
-- Frequency: Mon-Fri, 4.30pm
-- Jira/Ivanti: 	Author   	Date:  		Version:  	Reason: 
-- XXXX 			C Badcock 	Sept 2019 	00			Added Header and formatting
-- Jira959          C Badcock     Dec 2019     02            Added envionment agnostic
-- Jira989          C Badcock     Dec 2019     03            Compatible with email tables
-- Jira145			C Badcock	Aug 2020	04			Made compatible for v17 Production Database
-------------------------------------------------------

AS BEGIN
	SET NOCOUNT ON

	DECLARE @rowcount INT

	IF OBJECT_ID('tempdb..##TaxRateCheck') IS NOT NULL
	DROP TABLE ##TaxRateCheck

	 

	IF OBJECT_ID('tempdb..##TaxRateCheck') IS NOT NULL
	DROP TABLE ##TaxRateCheck

	-- Dynamic SQL to pick up the correct database name for cross-server deployment
	-- Works only if there is a database beginning with "OLEM" on the DB Server
	-- If no such database is found, the query runs for the prod database "END_V17PROD"

	DECLARE @db_name varchar(20)
	declare @sql_stmt nvarchar(4000)

	-- Top 1 as test/dev DB servers have multiple databases
	-- If no such databases found, use the PROD DB name

	SELECT TOP 1 @db_name = ISNULL(MIN(name),'END_V17PROD')     FROM sys.databases     WHERE name like '%PROD'


	SET @sql_stmt = 'SELECT DISTINCT ab.deal_tracking_num Deal_Num,ativ.value Metal_Price_Spread,i.name Ins_Type, ts.name Tran_Status, tr.rate_name Tax_Type ,ate1.para_position Net_Amount, it.effective_rate Correct_Tax_Rate , 
					(it.effective_rate * ate1.para_position) Correct_Tax_Amount,
					   CAST(it.collection_date  as date) Tax_Payment_Date, c.name Tax_Currency, ate.para_position Actual_Tax_Amount, 
					   (CASE WHEN tr.add_subtract_id = 1 THEN ROUND( ( ate.para_position / ate1.para_position ), 5) ELSE -1 * ROUND(( ate.para_position / ate1.para_position ), 5) END) Actual_Tax_Rate 
					INTO ##TaxRateCheck
					FROM   ' + @db_name + '.dbo.ab_tran ab 
					JOIN ' + @db_name + '.dbo.ins_tax it ON (it.tran_num = ab.tran_num AND it.taxable_amount != 0 AND it.tax_status != 2) 
					JOIN ' + @db_name + '.dbo.ab_tran_event ate ON (ate.tran_num = it.tran_num AND ate.ins_para_seq_num = it.param_seq_num AND ate.currency = it.tax_currency AND ate.event_type = 98 AND ate.ins_seq_num = it.tax_seq_num) 
					JOIN ' + @db_name + '.dbo.ab_tran_event ate1 ON (ate1.tran_num = ate.tran_num AND ate1.ins_para_seq_num = ate.ins_para_seq_num AND ate1.currency = ate.currency AND ate1.event_type = 14 AND ate1.ins_seq_num = ate.ins_seq_num) 
					JOIN ' + @db_name + '.dbo.tax_rate tr ON (tr.tax_rate_id = it.tax_rate_id) 
					JOIN ' + @db_name + '.dbo.trans_status ts ON (ts.trans_status_id = ab.tran_status)
					JOIN ' + @db_name + '.dbo.currency c ON (c.id_number = it.tax_currency) 
					JOIN ' + @db_name + '.dbo.Instruments i on  ( i.id_number = ab.ins_type)
					LEFT JOIN ' + @db_name + '.dbo.ab_tran_info_view ativ on (ativ.tran_num = ab.tran_num and ativ.type_id = 20085)
					WHERE  ab.tran_status IN ( 3 ) 
					AND (CASE WHEN tr.add_subtract_id = 1 THEN ROUND( ( ate.para_position / ate1.para_position ), 6) ELSE -1 * ROUND(( ate.para_position / ate1.para_position ), 6) END) <> ROUND(it.effective_rate, 6) 
					AND it.tax_currency IN ( 52, 57 )  AND ab.ins_type = 30201
					ORDER  BY ab.deal_tracking_num DESC'
					
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
	
		IF @db_name = 'END_V17PROD' 
			SET @email_db_name = 'Production v17- '
		ELSE 
			SET @email_db_name = 'UAT v17- '

		SET @email_subject = 'Endur Alert : Priority = 4 :' + @email_db_name + ' DBA Warning - Tax Rate Check Failed - Please one-step amend deal(s)'
		  
		SET @email_query = 'SELECT * from ##TaxRateCheck ORDER BY Deal_Num'


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
			EXEC msdb.dbo.sp_send_dbmail @profile_name = @profile_name, @recipients = @email_address_new, @subject = @email_subject, @query = @email_query, @importance = 'HIGH', @attach_query_result_as_file = 1
			DROP TABLE ##TaxRateCheck
			RETURN(1)
		END

	END

	DROP TABLE ##TaxRateCheck

END
